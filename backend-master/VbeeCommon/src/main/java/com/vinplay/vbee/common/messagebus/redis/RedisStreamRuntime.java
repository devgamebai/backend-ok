package com.vinplay.vbee.common.messagebus.redis;

import com.vinplay.vbee.common.config.VBeePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bootstrap entry point for the Redis-Streams consumer side &mdash; the
 * structural analogue of {@link com.vinplay.vbee.rmq.RMQ#start()} for the new
 * transport.
 *
 * <h2>Topology source</h2>
 * Reads the same {@code config/rabbitmq_config.xml} (or, when the rename
 * planned for P1.5 lands, {@code config/message_bus_config.xml}) so RMQ and
 * Redis consumers operate from a single source of truth: queue names, thread
 * counts, prefetch overrides, and command-id-to-processor maps come from one
 * XML rather than diverging between transports.
 *
 * <h2>Per-queue routing flag</h2>
 * For each {@code <queue>} block, we resolve a backend flag from the
 * environment:
 * <ul>
 *   <li>{@code MESSAGE_BUS_<QUEUE_UPPER>} where {@code QUEUE_UPPER} is the
 *       queue name uppercased with every non-{@code [A-Z0-9_]} character
 *       replaced by {@code _} (so {@code queue_slotExtend} becomes
 *       {@code MESSAGE_BUS_QUEUE_SLOTEXTEND}).</li>
 *   <li>If unset, fall back to {@code MESSAGE_BUS_DEFAULT}.</li>
 *   <li>If both unset, defaults to {@code "rmq"}.</li>
 * </ul>
 * Only queues whose flag resolves (case-insensitive) to {@code redis} or
 * {@code dual} get a {@link RedisStreamConsumer}. Everything else is left for
 * the RMQ path.
 *
 * <h2>Multiple consumer JVMs</h2>
 * The consumer side runs in two JVMs &mdash; the {@code vbee} container (for
 * the bulk of the queues) and the {@code game-Minigame} container (for
 * {@code queue_action_minigame} / {@code queue_action_portal} etc.). Each JVM
 * walks its OWN {@code rabbitmq_config.xml}, which lists only the queues that
 * particular JVM consumes. The two configs do not overlap, so there's no
 * double-consumption. To support this, {@link #start(String)} accepts an
 * explicit config path; the no-arg {@link #start()} defers to the candidate
 * search under {@link VBeePath#basePath} for backwards compatibility with
 * vbee's existing wiring.
 *
 * <h2>Half-applied state</h2>
 * If F4 is deployed but {@code C-bootstrap} hasn't yet wired
 * {@link #start()} into {@code VBeeMain}, this class is dead code and {@code vbee}
 * keeps running RMQ consumers exactly as before.
 *
 * <h2>Failure isolation</h2>
 * The XML parse failure or a Redis-unreachable error is logged and swallowed
 * &mdash; we do not crash the JVM. The RMQ path runs independently and stays
 * up. This is by design: the migration must not turn an optional Redis path
 * into a hard dependency before C-bootstrap deems it ready.
 *
 * <h2>Lifecycle</h2>
 * {@link #shutdown()} is registered as a JVM shutdown hook the first time
 * {@link #start()} is called. It calls {@link RedisStreamConsumer#shutdown()}
 * on each consumer (which issues XGROUP DELCONSUMER) so dead pods don't pin
 * PEL entries for {@code PEL_IDLE_MS} on every redeploy.
 */
public final class RedisStreamRuntime {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamRuntime.class);

    /**
     * Candidate config-file paths, tried in order when {@link #start()} is
     * invoked without an explicit path. Today the topology lives in
     * {@code rabbitmq_config.xml}; the plan's P1.5 step will rename it to
     * {@code message_bus_config.xml}. Trying both lets F4 work either side of
     * that rename without a flag-day.
     */
    private static final String[] CONFIG_CANDIDATES = new String[] {
            "config/message_bus_config.xml",
            "config/rabbitmq_config.xml"
    };

    /** {@code MESSAGE_BUS_DEFAULT} fallback if no per-queue flag is set. */
    private static final String DEFAULT_FLAG_DEFAULT = "rmq";

    private static final List<RedisStreamConsumer> STARTED = new ArrayList<RedisStreamConsumer>();
    private static volatile boolean shutdownHookInstalled = false;

    /**
     * Read the topology XML, evaluate the per-queue routing flag, and start
     * one {@link RedisStreamConsumer} per queue whose flag resolves to
     * {@code redis} or {@code dual}.
     *
     * <p>Logs the count of started consumers (or "0 queues enabled" when the
     * default state of all-RMQ leaves no work for this runtime). Never throws
     * &mdash; failures are logged and swallowed so a misconfigured Redis
     * cannot kill the RMQ path.
     */
    public static void start() {
        start(null);
    }

    /**
     * Like {@link #start()}, but reads topology from {@code configPath}
     * instead of the default candidate search. Used by the Minigame container,
     * which has its own {@code rabbitmq_config.xml} listing only the queues
     * Minigame consumes (e.g. {@code queue_action_minigame}). When
     * {@code configPath} is {@code null} or blank, falls back to the candidate
     * search under {@link VBeePath#basePath} that the no-arg overload uses.
     *
     * @param configPath explicit XML path, or {@code null} to use the default
     *                   {@link #CONFIG_CANDIDATES} resolution.
     */
    public static void start(String configPath) {
        logger.info("STARTING REDIS STREAMS CONSUMER RUNTIME ...");
        try {
            File configFile = (configPath == null || configPath.trim().isEmpty())
                    ? resolveConfigFile()
                    : resolveExplicitConfigFile(configPath);
            if (configFile == null) {
                if (configPath == null || configPath.trim().isEmpty()) {
                    logger.warn("RedisStreamRuntime: no config file found in {} — skipping",
                            java.util.Arrays.toString(CONFIG_CANDIDATES));
                } else {
                    logger.warn("RedisStreamRuntime: config file '{}' not found — skipping", configPath);
                }
                return;
            }

            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();

            // The XML root element is <rabbitmq>. We don't bind to that name
            // semantically — we just want the <queues>/<queue> children. If
            // the rename to message_bus_config.xml also renames the root,
            // fall back to walking from the document element.
            NodeList queueList = doc.getElementsByTagName("queue");
            int started = 0;
            int enabled = 0;
            for (int i = 0; i < queueList.getLength(); i++) {
                Element queueEl = (Element) queueList.item(i);
                QueueConfig qc = parseQueue(queueEl);
                if (qc == null) continue;

                String flag = resolveBackendFlag(qc.name);
                if (!isRedisEnabled(flag)) {
                    logger.debug("RedisStreamRuntime: queue '{}' flag='{}' — skipping", qc.name, flag);
                    continue;
                }
                enabled++;
                try {
                    RedisStreamConsumer consumer = new RedisStreamConsumer(
                            qc.name, qc.threads, qc.prefetch);
                    consumer.start(qc.commands);
                    STARTED.add(consumer);
                    started++;
                    logger.info("RedisStreamRuntime: started queue='{}' threads={} prefetch={} cmds={} flag='{}'",
                            qc.name, qc.threads, qc.prefetch, qc.commands.size(), flag);
                } catch (Exception e) {
                    // One queue's failure must not abort bootstrapping the
                    // others.
                    logger.error("RedisStreamRuntime: failed to start queue '{}'", qc.name, e);
                }
            }

            if (enabled == 0) {
                logger.info("RedisStreamRuntime: 0 queues enabled (all queues defaulting to rmq) — exiting");
                return;
            }

            installShutdownHook();
            logger.info("RedisStreamRuntime: {} of {} enabled queues started", started, enabled);
        } catch (Exception e) {
            // Don't crash the JVM on a Redis or XML config error — the RMQ
            // path is the source of truth until cutover and must keep running.
            logger.error("RedisStreamRuntime: start failed (RMQ path is unaffected)", e);
        }
    }

    /**
     * Stop every consumer started by {@link #start()}. Idempotent.
     *
     * <p>Called from the JVM shutdown hook by default; safe to invoke
     * directly from tests.
     */
    public static synchronized void shutdown() {
        for (RedisStreamConsumer c : STARTED) {
            try {
                c.shutdown();
            } catch (Throwable t) {
                logger.warn("RedisStreamRuntime: consumer shutdown failed: {}", t.getMessage());
            }
        }
        STARTED.clear();
    }

    private static synchronized void installShutdownHook() {
        if (shutdownHookInstalled) return;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }, "redis-stream-runtime-shutdown"));
        shutdownHookInstalled = true;
    }

    /**
     * Try each candidate path under {@link VBeePath#basePath} and return the
     * first one that exists. Returns null if none exist.
     */
    private static File resolveConfigFile() {
        String base = VBeePath.basePath != null ? VBeePath.basePath : "";
        for (String candidate : CONFIG_CANDIDATES) {
            File f = new File(base.concat(candidate));
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /**
     * Resolve an explicit caller-provided path. Tries it as an absolute path
     * first; if that fails, prefixes {@link VBeePath#basePath} so callers can
     * pass a relative path matching the existing config-loading convention
     * (e.g. {@code "config/rabbitmq_config.xml"}).
     */
    private static File resolveExplicitConfigFile(String configPath) {
        File f = new File(configPath);
        if (f.isFile()) return f;
        String base = VBeePath.basePath != null ? VBeePath.basePath : "";
        File rel = new File(base.concat(configPath));
        if (rel.isFile()) return rel;
        return null;
    }

    /**
     * Parse a single {@code <queue>} XML element into a {@link QueueConfig}.
     * Returns null on malformed input rather than throwing &mdash; one bad
     * queue block must not abort the whole bootstrap.
     */
    private static QueueConfig parseQueue(Element queueEl) {
        try {
            String name = textOf(queueEl, "name");
            if (name == null || name.isEmpty()) return null;

            int threads = 1;
            try {
                threads = Integer.parseInt(textOf(queueEl, "threads"));
            } catch (NumberFormatException nfe) {
                logger.warn("RedisStreamRuntime: queue '{}' missing/invalid threads — defaulting to 1", name);
            }

            int prefetch = 0; // 0 → consumer uses its DEFAULT_PREFETCH
            NodeList prefetchNodes = queueEl.getElementsByTagName("prefetch");
            if (prefetchNodes.getLength() > 0) {
                try {
                    prefetch = Integer.parseInt(prefetchNodes.item(0).getTextContent().trim());
                } catch (NumberFormatException e) {
                    logger.warn("RedisStreamRuntime: queue '{}' invalid prefetch — using default", name);
                }
            }

            HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
            NodeList cmdsNodes = queueEl.getElementsByTagName("commands");
            if (cmdsNodes.getLength() > 0) {
                Element cmds = (Element) cmdsNodes.item(0);
                NodeList cmdList = cmds.getElementsByTagName("command");
                for (int j = 0; j < cmdList.getLength(); j++) {
                    Element eCmd = (Element) cmdList.item(j);
                    try {
                        Integer id = Integer.parseInt(textOf(eCmd, "id"));
                        String path = textOf(eCmd, "path");
                        if (path != null) {
                            commandsMap.put(id, path.trim());
                        }
                    } catch (Exception inner) {
                        logger.warn("RedisStreamRuntime: queue '{}' bad command entry — skipping", name);
                    }
                }
            }

            return new QueueConfig(name, threads, prefetch, commandsMap);
        } catch (Exception e) {
            logger.warn("RedisStreamRuntime: parseQueue error: {}", e.getMessage());
            return null;
        }
    }

    private static String textOf(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String s = nl.item(0).getTextContent();
        return s == null ? null : s.trim();
    }

    /**
     * Resolve the backend flag for a queue. Tries
     * {@code MESSAGE_BUS_<QUEUE_UPPER>}, then {@code MESSAGE_BUS_DEFAULT},
     * then {@link #DEFAULT_FLAG_DEFAULT}.
     *
     * <p>{@code QUEUE_UPPER} is the queue name uppercased with every
     * non-{@code [A-Z0-9_]} character replaced by {@code _}. Example:
     * {@code queue_slotExtend} &rarr; {@code MESSAGE_BUS_QUEUE_SLOTEXTEND}.
     */
    static String resolveBackendFlag(String queueName) {
        String envVar = "MESSAGE_BUS_" + sanitizeQueueForEnvVar(queueName);
        String v = System.getenv(envVar);
        if (v != null && !v.isEmpty()) {
            return v.trim().toLowerCase();
        }
        v = System.getenv("MESSAGE_BUS_DEFAULT");
        if (v != null && !v.isEmpty()) {
            return v.trim().toLowerCase();
        }
        return DEFAULT_FLAG_DEFAULT;
    }

    /**
     * Uppercase + replace any character that isn't {@code [A-Z0-9_]} with
     * {@code _}. Idempotent for already-sane queue names; tolerant of
     * hyphens, dots, or camelCase.
     */
    static String sanitizeQueueForEnvVar(String queueName) {
        String upper = queueName.toUpperCase();
        StringBuilder sb = new StringBuilder(upper.length());
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static boolean isRedisEnabled(String flag) {
        return "redis".equals(flag) || "dual".equals(flag);
    }

    private static final class QueueConfig {
        final String name;
        final int threads;
        final int prefetch;
        final Map<Integer, String> commands;

        QueueConfig(String name, int threads, int prefetch, Map<Integer, String> commands) {
            this.name = name;
            this.threads = threads;
            this.prefetch = prefetch;
            this.commands = commands;
        }
    }

    private RedisStreamRuntime() {
        // utility class — no instances
    }
}
