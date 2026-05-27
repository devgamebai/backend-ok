package com.vinplay.vbee.common.messagebus.redis;

import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.utils.CommonUtils;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.XClaimArgs;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis-Streams-backed consumer runtime &mdash; the consumer-side counterpart
 * to {@link com.vinplay.vbee.common.messagebus.redis.RedisStreamMessageBus
 * RedisStreamMessageBus} (F3).
 *
 * <h2>One consumer per logical queue</h2>
 * Instantiate one {@code RedisStreamConsumer} per queue from the bootstrap
 * (see {@link RedisStreamRuntime}). Each consumer launches {@code numWorkers}
 * worker threads that share a single Lettuce connection (Lettuce's pipelined
 * connection is thread-safe by design &mdash; opening one connection per
 * worker would just multiply Netty I/O loops with no throughput gain).
 *
 * <h2>Per-worker loop</h2>
 * On each iteration the worker:
 * <ol>
 *   <li><b>Drains its pending list first.</b>
 *       {@code XREADGROUP GROUP <grp> <self> COUNT <prefetch> STREAMS {q}.stream 0}.
 *       This recovers entries the worker had already received but not ACKed
 *       before a previous crash. For each PEL entry the worker first issues
 *       {@code XCLAIM ... JUSTID} on its own consumer name &mdash; without
 *       this, a repeated {@code XREADGROUP 0} read does <em>not</em> bump
 *       the PEL {@code delivery-count} field, and the DLQ threshold check in
 *       step 5 would never fire on a single-consumer queue.</li>
 *   <li><b>Then reads fresh.</b>
 *       {@code XREADGROUP ... BLOCK 2000 COUNT <prefetch> STREAMS {q}.stream >}.
 *       Bounded block keeps shutdown latency in the order of {@link #BLOCK_MS}
 *       rather than the unbounded {@code 0}.</li>
 *   <li>For each entry read, extract the {@code "b"} field (raw
 *       {@link BaseMessage#toBytes()} bytes) and the {@code "c"} field (UTF-8
 *       decimal command id, matching the F3 producer wire format).</li>
 *   <li>Call {@code BaseController.processCommand(cmd, Param(body))} &mdash;
 *       the same dispatch path the legacy RMQ consumer uses, so business
 *       logic is identical across transports.</li>
 *   <li>On success {@code XACK} the entry. On exception or processor returning
 *       {@code Boolean.FALSE}, run the atomic Lua DLQ-promotion script.</li>
 * </ol>
 *
 * <h2>Atomic DLQ promotion</h2>
 * The DLQ-promotion script (see {@code DLQ_LUA}) reads the current PEL
 * delivery-count for the entry, copies the body to {@code {q}.dlq} and
 * {@code XACK}s the original &mdash; all atomically &mdash; only when the
 * count has reached {@link #MAX_DELIVERIES}. Splitting this across separate
 * {@code XPENDING}/{@code XADD}/{@code XACK} calls would race with
 * {@link #PEL_REAPER} ({@code XAUTOCLAIM}) on a different consumer and could
 * produce duplicate DLQ rows.
 *
 * <h2>Stale-PEL reaper</h2>
 * One scheduled task <em>per (queue, JVM)</em> &mdash; not per worker thread
 * &mdash; runs every 30s and issues
 * {@code XAUTOCLAIM {q}.stream <grp> <self> 30000 0-0 COUNT 100} to take over
 * entries left in the PEL of a dead pod. Reaping at the queue level (not
 * worker level) avoids 30 reapers spamming XAUTOCLAIM on a 30-thread queue.
 *
 * <h2>Poison-pill protection</h2>
 * {@link BaseMessage#fromBytes(byte[])} swallows {@code IOException} and
 * returns {@code null}. Without explicit handling, a corrupt entry would loop
 * through the retry path forever and flood the log. The worker XACKs and
 * promotes-to-DLQ immediately on a {@code null} body so the entry leaves the
 * stream.
 *
 * <h2>Group bootstrap</h2>
 * On {@link #start} we issue {@code XGROUP CREATE {q}.stream <grp> $ MKSTREAM}.
 * {@code MKSTREAM} so the group can be declared before any producer has
 * written; {@code BUSYGROUP} (already exists) is caught and ignored.
 *
 * <h2>Shutdown</h2>
 * {@link #shutdown} interrupts every worker and then issues
 * {@code XGROUP DELCONSUMER {q}.stream <grp> <self>} for each consumer name.
 * Without this, the dead consumer's PEL entries would only become claimable
 * after {@link #PEL_IDLE_MS}; the explicit DELCONSUMER releases them
 * immediately to the next pod.
 *
 * <h2>Thread safety</h2>
 * Worker threads, the shared Lettuce connection, the controller, and the PEL
 * reaper are all instantiated by {@link #start} and read-only after that.
 * Lettuce's sync command interface is thread-safe.
 */
public final class RedisStreamConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamConsumer.class);

    /**
     * Per-message handle-time logger. Mirrors the {@code csvHandleMessage}
     * channel that the legacy {@code com.vinplay.vbee.logger.HandleMessageLogger}
     * writes to from the RMQ path, so historical Logback CSV appender
     * configuration (if any) keeps producing rows for both transports. Inlined
     * here rather than imported from {@code api/vbee/} because VbeeCommon
     * cannot depend on {@code api/vbee/}.
     */
    private static final Logger HANDLE_LOG = LoggerFactory.getLogger("csvHandleMessage");
    private static final String HANDLE_FMT = ",%20s,\t%5d,\t%6d,\t%10s,\t%10s";
    private static final String HANDLE_DATE_FMT = "HH:mm:ss dd-MM-yyyy";

    /** Default per-worker {@code XREADGROUP COUNT} when caller doesn't pass one. */
    private static final int DEFAULT_PREFETCH = 20;

    /** Promote-to-DLQ threshold matched against PEL {@code delivery-count}. */
    private static final int MAX_DELIVERIES = 3;

    /** Bounded {@code BLOCK} on the fresh-read XREADGROUP. Keeps shutdown
     *  latency low without going completely busy-spin. */
    private static final long BLOCK_MS = 2000L;

    /** Idle threshold for {@code XAUTOCLAIM} — entries older than this in the
     *  PEL are considered owned by a dead consumer. */
    private static final long PEL_IDLE_MS = 30_000L;

    /** Reaper schedule period. Same cadence as {@link #PEL_IDLE_MS} so each
     *  tick can claim entries idle since the previous tick. */
    private static final long REAPER_PERIOD_MS = 30_000L;

    /** Cap on entries claimed per {@code XAUTOCLAIM} — bounds the work this
     *  reaper tick does even if a peer pod left thousands of stuck entries. */
    private static final int REAPER_BATCH = 100;

    /**
     * Atomic DLQ-promotion script.
     * <p>KEYS[1] = {@code {q}.stream}, KEYS[2] = {@code {q}.dlq}.
     * <p>ARGV[1] = group, ARGV[2] = consumer, ARGV[3] = entry id,
     *    ARGV[4] = max-deliveries (string-int), ARGV[5] = serialized body bytes,
     *    ARGV[6] = command-id bytes.
     * <p>Returns:
     *    <code>0</code> if the entry has already been ACKed by another consumer
     *    (XPENDING returned empty) &mdash; nothing to do.
     *    <em>positive count</em> if the entry still has retries left &mdash;
     *    leave it in the PEL for the next claim or fresh-read pass.
     *    <code>-1</code> if the entry was promoted: copied to DLQ and ACKed.
     */
    private static final String DLQ_LUA =
            "local pending = redis.call('XPENDING', KEYS[1], ARGV[1], 'IDLE', 0, ARGV[3], ARGV[3], 1, ARGV[2])\n" +
            "if #pending == 0 then return 0 end\n" +
            "local count = tonumber(pending[1][4])\n" +
            "if count < tonumber(ARGV[4]) then return count end\n" +
            "redis.call('XADD', KEYS[2], 'MAXLEN', '~', 100000, '*', 'b', ARGV[5], 'c', ARGV[6], 'orig_id', ARGV[3], 'reason', 'max_deliveries')\n" +
            "redis.call('XACK', KEYS[1], ARGV[1], ARGV[3])\n" +
            "return -1\n";

    private static final byte[] FIELD_BODY = "b".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FIELD_COMMAND = "c".getBytes(StandardCharsets.UTF_8);

    private final String queueName;
    private final int numWorkers;
    private final int prefetch;

    /** Resolved at {@link #start}. byte[] form of the stream key, group name,
     *  consumer name, and DLQ key &mdash; pre-computed once because they are
     *  used on every loop iteration and the connection codec is byte[]/byte[]. */
    private byte[] streamKey;
    private byte[] dlqKey;
    private byte[] groupName;
    private byte[] consumerName;
    private String streamKeyStr;   // for logging
    private String groupNameStr;   // for logging

    private BaseController<byte[], Boolean> controller;
    private Thread[] workers;
    private ScheduledExecutorService reaper;
    private volatile String dlqScriptSha;
    private volatile boolean running;

    public RedisStreamConsumer(String queueName, int numWorkers) {
        this(queueName, numWorkers, DEFAULT_PREFETCH);
    }

    public RedisStreamConsumer(String queueName, int numWorkers, int prefetch) {
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }
        this.queueName = queueName;
        this.numWorkers = numWorkers > 0 ? numWorkers : 1;
        this.prefetch = prefetch > 0 ? prefetch : DEFAULT_PREFETCH;
    }

    /**
     * Start the consumer. Initializes the dispatch controller, declares the
     * consumer group (idempotent), loads the DLQ Lua script, and launches
     * {@code numWorkers} worker threads plus one PEL reaper.
     *
     * <p>Wraps the whole bootstrap in try/catch and logs on failure rather
     * than throwing, so that one misconfigured queue cannot crash a JVM that
     * runs many queues.
     */
    public void start(Map<Integer, String> commandsMap) {
        try {
            this.streamKeyStr = StreamNames.stream(this.queueName);
            this.groupNameStr = StreamNames.group(this.queueName);
            this.streamKey = streamKeyStr.getBytes(StandardCharsets.UTF_8);
            this.dlqKey = StreamNames.dlq(this.queueName).getBytes(StandardCharsets.UTF_8);
            this.groupName = groupNameStr.getBytes(StandardCharsets.UTF_8);
            this.consumerName = StreamNames.consumerName().getBytes(StandardCharsets.UTF_8);

            this.controller = new BaseController<byte[], Boolean>();
            try {
                this.controller.initCommands(commandsMap);
            } catch (Exception e) {
                logger.error("RedisStreamConsumer[{}]: initCommands failed", queueName, e);
            }

            StatefulRedisConnection<byte[], byte[]> conn = RedisConnectionHolder.get();
            RedisCommands<byte[], byte[]> sync = conn.sync();

            // XGROUP CREATE ... MKSTREAM. Idempotent: BUSYGROUP means it
            // already exists which is the expected state on every restart
            // after the first.
            try {
                XReadArgs.StreamOffset<byte[]> dollar = XReadArgs.StreamOffset.<byte[]>from(streamKey, "$");
                sync.xgroupCreate(dollar, groupName, XGroupCreateArgs.Builder.mkstream(true));
                logger.info("RedisStreamConsumer[{}]: created consumer group '{}'", queueName, groupNameStr);
            } catch (RedisBusyException busy) {
                // Group already exists — fine.
            } catch (RedisCommandExecutionException ree) {
                // Lettuce wraps server-side errors; BUSYGROUP can land here too.
                String msg = ree.getMessage();
                if (msg != null && msg.contains("BUSYGROUP")) {
                    // Already exists.
                } else {
                    throw ree;
                }
            }

            // Load DLQ Lua script once. Workers EVALSHA by the cached SHA.
            this.dlqScriptSha = sync.scriptLoad(DLQ_LUA.getBytes(StandardCharsets.UTF_8));
            logger.debug("RedisStreamConsumer[{}]: DLQ script loaded sha={}", queueName, dlqScriptSha);

            this.running = true;

            // Spin up workers — share the singleton connection.
            this.workers = new Thread[numWorkers];
            for (int i = 0; i < numWorkers; i++) {
                final int workerIndex = i;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runWorkerLoop(workerIndex);
                    }
                }, "redis-stream-" + queueName + "-" + i);
                t.setDaemon(true);
                workers[i] = t;
                t.start();
            }

            // One reaper per (queue, JVM).
            this.reaper = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                private final AtomicInteger n = new AtomicInteger();
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "redis-stream-reaper-" + queueName + "-" + n.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
            this.reaper.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    runReaperOnce();
                }
            }, REAPER_PERIOD_MS, REAPER_PERIOD_MS, TimeUnit.MILLISECONDS);

            logger.info("RedisStreamConsumer[{}]: started workers={} prefetch={} group={}",
                    queueName, numWorkers, prefetch, groupNameStr);
        } catch (Exception e) {
            logger.error("RedisStreamConsumer[{}]: start failed", queueName, e);
        }
    }

    /**
     * Stop workers and release the consumer name in Redis.
     *
     * <p>Sets {@link #running} false, interrupts each worker so the blocking
     * {@code XREADGROUP} returns promptly, then issues
     * {@code XGROUP DELCONSUMER} so this consumer's PEL entries become
     * claimable by peers immediately rather than waiting {@link #PEL_IDLE_MS}.
     *
     * <p>Does NOT close the shared Lettuce connection &mdash; that's owned by
     * {@link RedisConnectionHolder} and is shared with the producer side.
     */
    public void shutdown() {
        this.running = false;
        if (reaper != null) {
            try {
                reaper.shutdownNow();
            } catch (Throwable ignored) { }
        }
        if (workers != null) {
            for (Thread w : workers) {
                if (w != null) {
                    w.interrupt();
                }
            }
            for (Thread w : workers) {
                if (w != null) {
                    try {
                        w.join(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        try {
            StatefulRedisConnection<byte[], byte[]> conn = RedisConnectionHolder.get();
            // XGROUP DELCONSUMER releases this consumer's PEL entries.
            conn.sync().xgroupDelconsumer(streamKey,
                    Consumer.<byte[]>from(groupName, consumerName));
            logger.info("RedisStreamConsumer[{}]: shutdown — released consumer {}",
                    queueName, StreamNames.consumerName());
        } catch (Throwable t) {
            // Best-effort cleanup; reaper on peer pods will pick up entries
            // after PEL_IDLE_MS even if DELCONSUMER fails.
            logger.warn("RedisStreamConsumer[{}]: DELCONSUMER on shutdown failed: {}",
                    queueName, t.getMessage());
        }
    }

    /**
     * Per-worker main loop: drain PEL, then read fresh, then dispatch.
     * Loops until {@link #running} is false or the worker is interrupted.
     */
    private void runWorkerLoop(int workerIndex) {
        StatefulRedisConnection<byte[], byte[]> conn;
        try {
            conn = RedisConnectionHolder.get();
        } catch (Exception e) {
            logger.error("RedisStreamConsumer[{}] worker {}: cannot acquire connection — exiting",
                    queueName, workerIndex, e);
            return;
        }
        RedisCommands<byte[], byte[]> sync = conn.sync();
        Consumer<byte[]> me = Consumer.<byte[]>from(groupName, consumerName);

        XReadArgs blockArgs = XReadArgs.Builder.count(prefetch).block(BLOCK_MS);
        XReadArgs noBlockArgs = XReadArgs.Builder.count(prefetch);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 1) Drain PEL — entries previously delivered to this consumer
                //    but not yet ACKed (we crashed mid-process, or returned an
                //    exception last loop). Read at id "0".
                XReadArgs.StreamOffset<byte[]> pelOffset = XReadArgs.StreamOffset.<byte[]>from(streamKey, "0");
                java.util.List<StreamMessage<byte[], byte[]>> pelBatch =
                        sync.xreadgroup(me, noBlockArgs, pelOffset);
                if (pelBatch != null && !pelBatch.isEmpty()) {
                    for (StreamMessage<byte[], byte[]> msg : pelBatch) {
                        // Bump PEL delivery-count by re-claiming JUSTID on
                        // ourselves. Without this, the DLQ threshold would
                        // never trigger for entries stuck in our own PEL.
                        try {
                            sync.xclaim(streamKey, me, XClaimArgs.Builder.justid().minIdleTime(0L),
                                    msg.getId());
                        } catch (Exception claimErr) {
                            // Non-fatal — the entry is still in our PEL; we'll
                            // try to process it below.
                            logger.debug("RedisStreamConsumer[{}] JUSTID claim failed id={}: {}",
                                    queueName, msg.getId(), claimErr.getMessage());
                        }
                        handleMessage(sync, msg);
                    }
                }

                // 2) Read fresh — id ">". Bounded BLOCK so shutdown wakes
                //    every BLOCK_MS even with no traffic.
                XReadArgs.StreamOffset<byte[]> freshOffset = XReadArgs.StreamOffset.<byte[]>lastConsumed(streamKey);
                java.util.List<StreamMessage<byte[], byte[]>> freshBatch =
                        sync.xreadgroup(me, blockArgs, freshOffset);
                if (freshBatch != null) {
                    for (StreamMessage<byte[], byte[]> msg : freshBatch) {
                        handleMessage(sync, msg);
                    }
                }
            } catch (RedisException re) {
                logger.warn("RedisStreamConsumer[{}] worker {} Redis error: {}",
                        queueName, workerIndex, re.getMessage());
                // Brief backoff before retrying so we don't tight-loop on a
                // permanently broken connection.
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Throwable t) {
                logger.error("RedisStreamConsumer[{}] worker {} unexpected error",
                        queueName, workerIndex, t);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        logger.info("RedisStreamConsumer[{}] worker {} exiting", queueName, workerIndex);
    }

    /**
     * Process a single stream entry: decode, dispatch, ack-or-DLQ.
     *
     * <p>Mirrors the legacy RMQ consumer's message-handling shape:
     * decode the body, build a {@link Param}, call
     * {@code controller.processCommand(cmd, p)}, then ack on success / DLQ
     * promote on failure.
     */
    private void handleMessage(RedisCommands<byte[], byte[]> sync, StreamMessage<byte[], byte[]> msg) {
        long startHandleTime = System.currentTimeMillis();
        String entryId = msg.getId();
        Map<byte[], byte[]> body = msg.getBody();

        // Lettuce StreamMessage exposes the field map; lookup is O(n) over
        // entries because byte[] equality isn't built into HashMap. We have
        // exactly two fields per message (b, c) so this is fine.
        byte[] payload = lookupField(body, FIELD_BODY);
        byte[] commandBytes = lookupField(body, FIELD_COMMAND);

        if (payload == null || commandBytes == null) {
            // Malformed entry — missing required field. ACK + DLQ promote.
            logger.warn("RedisStreamConsumer[{}] entry {} missing required field — DLQ + ACK",
                    queueName, entryId);
            promoteToDlqAndAck(sync, entryId, payload != null ? payload : new byte[0],
                    commandBytes != null ? commandBytes : new byte[0]);
            return;
        }

        BaseMessage decoded = BaseMessage.fromBytes(payload);
        if (decoded == null) {
            // Poison-pill: corrupt serialization. fromBytes swallows the IOException
            // and returns null. ACK + DLQ promote so the entry leaves the stream.
            logger.warn("RedisStreamConsumer[{}] entry {} body decode returned null — DLQ + ACK",
                    queueName, entryId);
            promoteToDlqAndAck(sync, entryId, payload, commandBytes);
            return;
        }

        int command;
        try {
            command = Integer.parseInt(new String(commandBytes, StandardCharsets.UTF_8));
        } catch (NumberFormatException nfe) {
            logger.warn("RedisStreamConsumer[{}] entry {} cmd field not integer — DLQ + ACK",
                    queueName, entryId);
            promoteToDlqAndAck(sync, entryId, payload, commandBytes);
            return;
        }

        try {
            logger.debug("{} HANDLE MESSAGE: {}", queueName, command);
            Param<byte[]> p = new Param<byte[]>();
            p.set(payload);
            Boolean result = controller.processCommand(Integer.valueOf(command), p);
            if (result != null && Boolean.FALSE.equals(result)) {
                // Processor explicitly returned false — leave in PEL or
                // promote to DLQ depending on delivery count.
                tryDlqOrLeavePending(sync, entryId, payload, commandBytes);
            } else {
                sync.xack(streamKey, groupName, entryId);
            }
        } catch (NoCommandRegistered ncr) {
            logger.error("COMMAND {} NOT FOUND IN QUEUE: {}", command, queueName);
            // Unknown command — ACK to prevent infinite loop. Same behavior
            // as the legacy RMQ consumer.
            try {
                sync.xack(streamKey, groupName, entryId);
            } catch (Exception ackErr) {
                logger.warn("RedisStreamConsumer[{}] xack after NoCommand failed: {}",
                        queueName, ackErr.getMessage());
            }
        } catch (Exception ex) {
            logger.error("{} HANDLE MESSAGE: {} ERROR:", queueName, command, ex);
            tryDlqOrLeavePending(sync, entryId, payload, commandBytes);
        }
        long handleTime = System.currentTimeMillis() - startHandleTime;
        try {
            // Mirrors HandleMessageLogger.log() but inlined to keep VbeeCommon
            // free of any api/vbee/ dependency.
            if (HANDLE_LOG.isDebugEnabled()) {
                String ratio = CommonUtils.getRatioTime(handleTime);
                HANDLE_LOG.debug(String.format(HANDLE_FMT, queueName, command, handleTime, ratio,
                        new SimpleDateFormat(HANDLE_DATE_FMT).format(new Date())));
            }
        } catch (Throwable ignored) { /* metrics logging must never fail processing */ }
    }

    /**
     * Look up a field from a {@link StreamMessage} body keyed on a {@code byte[]}.
     * Handles the case where the codec hands us {@code byte[]} keys whose
     * default {@code equals} is identity-based, so we walk the entries.
     */
    private static byte[] lookupField(Map<byte[], byte[]> body, byte[] field) {
        if (body == null) return null;
        for (Map.Entry<byte[], byte[]> e : body.entrySet()) {
            if (java.util.Arrays.equals(e.getKey(), field)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Run the atomic DLQ Lua script on a failed entry. The script either
     * promotes (if delivery-count &ge; MAX_DELIVERIES) or leaves the entry in
     * the PEL for another delivery attempt.
     */
    private void tryDlqOrLeavePending(RedisCommands<byte[], byte[]> sync, String entryId,
                                      byte[] payload, byte[] commandBytes) {
        try {
            byte[][] keys = new byte[][] { streamKey, dlqKey };
            byte[][] argv = new byte[][] {
                    groupName,
                    consumerName,
                    entryId.getBytes(StandardCharsets.UTF_8),
                    Integer.toString(MAX_DELIVERIES).getBytes(StandardCharsets.UTF_8),
                    payload,
                    commandBytes
            };
            Long rc = sync.evalsha(dlqScriptSha, ScriptOutputType.INTEGER, keys, argv);
            if (rc != null && rc.longValue() == -1L) {
                logger.error("{} message PROMOTED TO DLQ after {} deliveries, entry={}",
                        queueName, MAX_DELIVERIES, entryId);
            }
        } catch (RedisCommandExecutionException ree) {
            // NOSCRIPT: server restarted and lost the cached script. Reload
            // and retry once.
            String msg = ree.getMessage();
            if (msg != null && msg.contains("NOSCRIPT")) {
                try {
                    this.dlqScriptSha = sync.scriptLoad(DLQ_LUA.getBytes(StandardCharsets.UTF_8));
                    tryDlqOrLeavePending(sync, entryId, payload, commandBytes);
                } catch (Exception reloadErr) {
                    logger.warn("RedisStreamConsumer[{}] DLQ script reload failed: {}",
                            queueName, reloadErr.getMessage());
                }
            } else {
                logger.warn("RedisStreamConsumer[{}] DLQ EVALSHA failed entry={}: {}",
                        queueName, entryId, msg);
            }
        } catch (Throwable t) {
            logger.warn("RedisStreamConsumer[{}] DLQ EVALSHA unexpected error entry={}: {}",
                    queueName, entryId, t.getMessage());
        }
    }

    /**
     * Force-promote a malformed entry to DLQ unconditionally (poison-pill or
     * missing fields). Bypasses the delivery-count check because retrying
     * would just decode-fail again forever.
     */
    private void promoteToDlqAndAck(RedisCommands<byte[], byte[]> sync, String entryId,
                                    byte[] payload, byte[] commandBytes) {
        try {
            // XADD DLQ.
            java.util.Map<byte[], byte[]> dlqEntry = new java.util.HashMap<byte[], byte[]>(8);
            dlqEntry.put(FIELD_BODY, payload);
            dlqEntry.put(FIELD_COMMAND, commandBytes);
            dlqEntry.put("orig_id".getBytes(StandardCharsets.UTF_8),
                    entryId.getBytes(StandardCharsets.UTF_8));
            dlqEntry.put("reason".getBytes(StandardCharsets.UTF_8),
                    "decode_failed".getBytes(StandardCharsets.UTF_8));
            sync.xadd(dlqKey,
                    io.lettuce.core.XAddArgs.Builder.maxlen(100_000L).approximateTrimming(),
                    dlqEntry);
            sync.xack(streamKey, groupName, entryId);
        } catch (Throwable t) {
            logger.warn("RedisStreamConsumer[{}] poison-pill DLQ failed entry={}: {}",
                    queueName, entryId, t.getMessage());
        }
    }

    /**
     * One reaper tick: claim entries whose PEL idle &gt; PEL_IDLE_MS and
     * re-deliver them to ourselves on the next worker fresh-read pass.
     *
     * <p>Uses {@code XAUTOCLAIM JUSTID} so we don't pull the bodies down on
     * the reaper thread &mdash; the next worker {@code XREADGROUP 0} will
     * pick them up via the regular PEL-drain path.
     */
    private void runReaperOnce() {
        if (!running) return;
        try {
            StatefulRedisConnection<byte[], byte[]> conn = RedisConnectionHolder.get();
            RedisCommands<byte[], byte[]> sync = conn.sync();
            Consumer<byte[]> me = Consumer.<byte[]>from(groupName, consumerName);
            XAutoClaimArgs<byte[]> args = XAutoClaimArgs.Builder
                    .<byte[]>justid(me, PEL_IDLE_MS, "0-0")
                    .count(REAPER_BATCH);
            sync.xautoclaim(streamKey, args);
            // We don't process the returned ids here — the next PEL-drain
            // pass on each worker will see them in our PEL and dispatch.
        } catch (Throwable t) {
            // Reaper is best-effort; a transient error is logged at debug
            // and the next tick will retry.
            logger.debug("RedisStreamConsumer[{}] reaper tick error: {}",
                    queueName, t.getMessage());
        }
    }
}
