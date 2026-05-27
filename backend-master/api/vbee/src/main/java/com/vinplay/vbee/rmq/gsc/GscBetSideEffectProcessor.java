package com.vinplay.vbee.rmq.gsc;

import com.mongodb.client.MongoCollection;
import com.vinplay.dal.dao.GscBetsDao;
import com.vinplay.dal.dao.impl.GscBetsDaoImpl;
import com.vinplay.dal.entities.gsc.GscBet;
import com.vinplay.dal.service.seamless.gsc.GscBetSideEffectPublisher;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.mongodb.MongoRetry;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.hazelcast.core.IMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 5 prep gate 5p2 — drain {@code queue_log_gsc_bets_async} and
 * execute the Mongo {@code log_gsc_bets} write + Telegram alert in a
 * dedicated thread pool, off the GSC request hot path.
 *
 * <p>Bound to RMQ command id {@link GscBetSideEffectPublisher#COMMAND_ID}
 * via {@code api/vbee/config/rabbitmq_config.xml}; mirrors the shape of
 * {@code LogMoneyUserExtraProcessor}.
 *
 * <h2>Bounded thread pool</h2>
 * 8 worker threads with a bounded 256-slot queue. When the queue is full
 * (sustained Mongo outage), {@link #execute} returns the message back to
 * the RMQ queue rather than absorbing it onto the JVM heap. The bounded
 * queue is the structural protection against unbounded memory growth
 * during a Mongo backoff chain — the same pathology 5p2 is meant to
 * prevent on the request side.
 *
 * <h2>Retry semantics</h2>
 * Mongo writes are wrapped in {@link MongoRetry#runWithRetry} (~200ms cap
 * per 5p6). On exhaustion the consumer fires the optional Telegram
 * alert (when {@link GscBetSideEffectMessage#telegramAlertSubject} is
 * non-null) and rethrows so the RMQ machinery NACKs / requeues. The
 * RMQ infrastructure already bounds requeue counts via the broker's
 * default delivery-attempt configuration; no custom dead-letter setup
 * needed at this stage.
 *
 * <h2>Why business logic lives in the publisher class</h2>
 * The publisher and consumer share the exact same Mongo-op
 * implementation by routing through
 * {@link GscBetSideEffectPublisher#executeSideEffect}. That keeps the
 * sync-fallback path (RMQ down) and the async-consumer path
 * byte-for-byte identical and unit-testable through one entry point.
 */
public class GscBetSideEffectProcessor implements BaseProcessor<byte[], Boolean> {

    private static final Logger logger = Logger.getLogger("vbee");

    /** Worker pool size — tunable via {@code GSC_SIDE_EFFECT_THREADS} env. */
    private static final int DEFAULT_THREADS = 8;
    /** Bounded work queue depth. */
    private static final int QUEUE_CAPACITY = 256;

    /**
     * Cross-store traceability follow-up #5 — flag-gated dual-write to
     * {@code vinplay.gsc_bets}. Default {@code false} so deploying this
     * code does NOT immediately start the new MySQL writes. Ops flips
     * the env var to {@code true} when ready to begin the 2-week soak,
     * and back to {@code false} if MySQL pressure shows up. Re-read on
     * every message so the flag is hot-tunable without a redeploy.
     *
     * <p>The Mongo write stays unconditional — the dual-write is purely
     * additive. A MySQL throw is caught + logged so it can NEVER cascade
     * into the Mongo path; same posture as Phase 1's
     * {@code MoneyGateway.dualWriteToLedger} guard.
     */
    private static final String DUAL_WRITE_FLAG_ENV = "GSC_BETS_MYSQL_DUAL_WRITE";

    /**
     * DAO instance — stateless aside from the connection-pool reference,
     * so a single instance is safe to share across the worker pool.
     */
    private static final GscBetsDao GSC_BETS_DAO = new GscBetsDaoImpl();

    private static final ThreadPoolExecutor EXEC = buildExecutor();

    private static ThreadPoolExecutor buildExecutor() {
        int threads = DEFAULT_THREADS;
        try {
            String v = System.getenv("GSC_SIDE_EFFECT_THREADS");
            if (v != null && !v.isEmpty()) {
                int parsed = Integer.parseInt(v.trim());
                if (parsed >= 2 && parsed <= 64) threads = parsed;
            }
        } catch (Throwable ignored) { /* keep default */ }

        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(0);
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "gsc-side-effect-" + n.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        return new ThreadPoolExecutor(
                threads, threads,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                tf,
                // CALLER_RUNS would block the RMQ consumer thread which
                // is OK degradation: it bounds memory and slows in-bound
                // delivery rather than dropping work.
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public Boolean execute(Param<byte[]> param) {
        if (param == null || param.get() == null) return Boolean.TRUE;
        final GscBetSideEffectMessage msg;
        try {
            msg = (GscBetSideEffectMessage) BaseMessage.fromBytes(param.get());
        } catch (Throwable t) {
            logger.warn("GscBetSideEffectProcessor: malformed message dropped: " + t.getMessage());
            return Boolean.TRUE;
        }
        if (msg == null || msg.op == null) {
            logger.warn("GscBetSideEffectProcessor: null/op-less message dropped");
            return Boolean.TRUE;
        }

        try {
            EXEC.execute(() -> handleOne(msg));
        } catch (RejectedExecutionException rex) {
            // Bounded queue full and CallerRunsPolicy still rejected
            // (only happens if the executor is shutting down). Run on
            // the consumer thread directly — slower, but keeps the row
            // landing during shutdown windows.
            handleOne(msg);
        }
        return Boolean.TRUE;
    }

    private static void handleOne(GscBetSideEffectMessage msg) {
        try {
            MongoCollection<Document> col = MongoDBConnectionFactory.getDB()
                    .getCollection("log_gsc_bets");
            try {
                MongoRetry.runWithRetry(
                        msg.aggregatorTag + " async " + msg.op + " wager=" + msg.wagerCode,
                        () -> GscBetSideEffectPublisher.executeSideEffect(msg, col));
            } catch (Throwable mongoErr) {
                logger.warn("GscBetSideEffectProcessor: Mongo failed (non-fatal)."
                        + " op=" + msg.op
                        + " wager=" + msg.wagerCode
                        + " member=" + msg.memberAccount
                        + " err=" + mongoErr.getMessage());
                GscBetSideEffectPublisher.fireTelegramIfRequested(msg, mongoErr.getMessage());
                // Don't rethrow — at-least-once requeue would chain-stall
                // against a sustained Mongo outage. We've alerted ops via
                // Telegram (if requested) and the reconciler picks up
                // missed log_gsc_bets rows separately.
            }

            // Cross-store traceability follow-up #5 — flag-gated MySQL
            // dual-write. Strictly additive: a MySQL throw can never
            // cascade into the Mongo path (already done above) or into
            // the wallet decision (already committed before this RMQ
            // message was published). Same best-effort posture as the
            // Mongo block above — any failure logs and continues.
            if (isMysqlDualWriteEnabled()) {
                try {
                    runMysqlDualWrite(msg);
                } catch (Throwable mysqlErr) {
                    logger.warn("GscBetSideEffectProcessor: MySQL dual-write failed (non-fatal)."
                            + " op=" + msg.op
                            + " wager=" + msg.wagerCode
                            + " member=" + msg.memberAccount
                            + " err=" + mysqlErr.getMessage());
                    // No telegram alert here — Mongo is still source of
                    // truth during the soak; a missed MySQL row is a
                    // soak-window observability gap, not an audit gap.
                    // Backfill picks it up; reader cutover is gated on
                    // soak success which catches sustained drift.
                }
            }
        } catch (Throwable t) {
            logger.error("GscBetSideEffectProcessor: unexpected error op="
                    + msg.op + " wager=" + msg.wagerCode, t);
        }
    }

    /**
     * Re-read the env var on every call so the dual-write can be turned
     * on / off without a redeploy. Cheap (~ns); the per-message overhead
     * is dwarfed by the Mongo retry above.
     */
    private static boolean isMysqlDualWriteEnabled() {
        try {
            String v = System.getenv(DUAL_WRITE_FLAG_ENV);
            return v != null && ("true".equalsIgnoreCase(v.trim())
                    || "1".equals(v.trim())
                    || "yes".equalsIgnoreCase(v.trim()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Run the MySQL mirror op for the given message. Mirrors the Mongo
     * branch of {@link GscBetSideEffectPublisher#executeSideEffect} but
     * targets {@code vinplay.gsc_bets} via the DAO. Any throw propagates
     * to the caller's try/catch above, which logs and continues.
     */
    private static void runMysqlDualWrite(GscBetSideEffectMessage msg) throws Exception {
        if (msg == null || msg.op == null) return;
        switch (msg.op) {
            case BET_INSERT: {
                GscBet bet = new GscBet();
                bet.userId = resolveUserId(msg);
                bet.userName = msg.memberAccount;
                bet.nickName = msg.memberAccount;
                bet.betValue = msg.amount;
                bet.prize = 0L;
                bet.fee = msg.fee;
                bet.productCode = msg.productCode;
                bet.gameCode = msg.gameCode;
                bet.gameKey = msg.gameKey;
                bet.gameName = msg.gameName != null ? msg.gameName : msg.gameKey;
                bet.txnId = msg.txnId;
                bet.wagerCode = msg.wagerCode;
                bet.currency = msg.currency;
                bet.betType = "BET";
                bet.settled = false;
                bet.vendorGameId = msg.vendorGameId;
                bet.eventKey = msg.eventKey;
                long created = msg.createdAtMs > 0L ? msg.createdAtMs : System.currentTimeMillis();
                bet.createTime = new Date(created);
                bet.timeLog = bet.createTime;
                // P6.1 — seed the per-bet detail object so the multi-bet
                // hand upsert path's $push details has the raw_amount /
                // action carried through. The DAO reads element [0] of
                // bet.details to populate the JSON_ARRAY initial value
                // and the JSON_ARRAY_APPEND merge value.
                if (msg.eventKey != null && !msg.eventKey.isEmpty()) {
                    java.util.Map<String, Object> detail = new java.util.LinkedHashMap<>();
                    detail.put("txn_id", msg.txnId);
                    detail.put("amount", msg.amount);
                    if (msg.rawAmount != null) detail.put("raw_amount", msg.rawAmount);
                    if (msg.action != null) detail.put("action", msg.action);
                    bet.details = java.util.Collections.singletonList(detail);
                }
                GSC_BETS_DAO.insert(bet);
                return;
            }
            case SETTLE_UPDATE:
            case FREESPIN_CHAIN_SETTLE:
                // P6.3 — pass full filter context so the DAO can apply
                // the two-tier filter precedence (linkRoundId →
                // event_key → wager_code) mirroring legacy
                // DepositProcess Mongo $or candidates.
                GSC_BETS_DAO.settle(
                        msg.memberAccount,
                        msg.productCode,
                        msg.gameCode,
                        msg.wagerCode,
                        msg.eventKey,
                        msg.linkRoundId,
                        msg.prize,
                        msg.txnId);
                return;
            case CANCEL_DELETE:
            case ROLLBACK_DELETE:
                GSC_BETS_DAO.deleteByWagerCode(msg.wagerCode);
                return;
        }
    }

    /**
     * P6.4 — prefer the resolved user_id carried in the message. Fall
     * back to Hazelcast/MySQL lookup only when the producer didn't set
     * it (backward compat with older publishers in flight at deploy
     * time). Logs a WARN on fallback so a regression where a new
     * publisher forgets to set userId is visible in ops logs.
     */
    private static long resolveUserId(GscBetSideEffectMessage msg) {
        if (msg.userId > 0L) return msg.userId;
        long fallback = lookupUserIdFor(msg.memberAccount);
        if (fallback > 0L) {
            logger.warn("GscBetSideEffectProcessor: msg.userId unset; resolved via lookup. "
                    + "member=" + msg.memberAccount + " op=" + msg.op
                    + " wager=" + msg.wagerCode + " resolved=" + fallback);
        } else {
            logger.warn("GscBetSideEffectProcessor: msg.userId unset and lookup failed — "
                    + "row will write user_id=0. member=" + msg.memberAccount
                    + " op=" + msg.op + " wager=" + msg.wagerCode);
        }
        return fallback;
    }

    /**
     * Resolve {@code users.id} for the rebate user. Hazelcast users IMap
     * first (free if cached), MySQL fallback. Returns 0 on miss — the
     * row still inserts (user_id is NOT NULL but 0 satisfies that, and
     * downstream queries on user_id=0 are detectable; wager_code UNIQUE
     * is the real identity). Mirror of the same lookup pattern in the
     * aggregators ({@code resolveUserIdForRebate}).
     */
    private static long lookupUserIdFor(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return 0L;
        try {
            IMap<String, UserCacheModel> userMap =
                    HazelcastClientFactory.getInstance().getMap("users");
            UserCacheModel u = userMap.get(memberAccount);
            if (u != null) {
                long id = u.getId();
                if (id > 0L) return id;
            }
        } catch (Throwable ignored) {
            // Hazelcast unavailable — fall through to MySQL.
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, memberAccount);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Throwable e) {
            logger.warn("GscBetSideEffectProcessor.lookupUserIdFor MySQL fallback failed for "
                    + memberAccount + ": " + e.getMessage());
        }
        return 0L;
    }
}
