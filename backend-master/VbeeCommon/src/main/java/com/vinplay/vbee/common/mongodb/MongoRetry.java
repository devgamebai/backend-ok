package com.vinplay.vbee.common.mongodb;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoExecutionTimeoutException;
import org.apache.log4j.Logger;

/**
 * SUN-1108/1110 follow-up — small retry wrapper for Mongo writes that hit
 * transient connection-pool errors.
 *
 * <p>Background: the SUN-1108/1110 incident dropped 3 bet-history rows
 * because {@code WithdrawProcess.col.insertOne(...)} failed inside a
 * generic {@code catch (Throwable)} block whose only action was a WARN
 * log and continue. The driver's {@code retryWrites=true} doesn't help
 * with {@link MongoSocketException} on connection establishment — it
 * only retries WRITE operations once the connection is up. So we add an
 * application-level retry for the connection-class transients.
 *
 * <p>Retried exceptions:
 * <ul>
 *   <li>{@link MongoSocketException} — "Exception opening socket",
 *       "Prematurely reached end of stream" (the exact errors observed
 *       in the 2026-04-25 17:16-17:46 UTC failures)</li>
 *   <li>{@link MongoTimeoutException} — server selection / write timeout</li>
 * </ul>
 *
 * <p>Schema / constraint errors (duplicate key, invalid query) are NOT
 * retried — they're real bugs, not transient.
 *
 * <p>Backoff: 50ms, 150ms (Phase 5 prep gate 5p6 — was 100ms/500ms/2000ms).
 * Total worst-case latency added: ~200ms. The previous 2.6s tail risked
 * pool exhaustion at 200+ TPS bursts (single Mongo blip × 2s tail =
 * 400 in-flight handlers holding MySQL connections through the retry
 * window). 200ms cap keeps the pool drainable under burst.
 *
 * <p>Trade-off: a real Mongo outage longer than 200ms now surfaces as
 * a thrown exception faster, which the caller's outer try/catch logs
 * and continues from. The wallet credit/debit has already committed by
 * the time MongoRetry runs (afterCredit/afterDebit are best-effort
 * post-wallet hooks), so a thrown Mongo exception only loses the
 * `log_gsc_bets` row and Telegram alert — not the player's money.
 * The reconciler picks up missed log_gsc_bets rows separately.
 */
public final class MongoRetry {

    private static final Logger logger = Logger.getLogger("mongo-retry");
    private static final long[] BACKOFF_MS = {50L, 150L};
    private static final int MAX_ATTEMPTS = BACKOFF_MS.length;

    private MongoRetry() {}

    /** Functional interface for an op that may throw any Mongo-side error. */
    public interface Op {
        void run() throws Exception;
    }

    /**
     * Run {@code op} with up to {@value #MAX_ATTEMPTS} attempts on transient
     * Mongo connection errors. Returns normally on success; on permanent
     * failure rethrows the last exception. Caller decides whether to fail
     * the request, log-and-continue, or fall back to an outbox.
     *
     * @param desc short label for logs (e.g. "log_gsc_bets insertOne")
     */
    public static void runWithRetry(String desc, Op op) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                op.run();
                if (attempt > 1) {
                    logger.warn(desc + " — succeeded on attempt " + attempt);
                }
                return;
            } catch (MongoSocketException | MongoTimeoutException | MongoExecutionTimeoutException transient_) {
                last = transient_;
                if (attempt == MAX_ATTEMPTS) {
                    logger.error(desc + " — exhausted " + MAX_ATTEMPTS
                            + " attempts: " + transient_.getMessage());
                    throw transient_;
                }
                long sleep = BACKOFF_MS[attempt - 1];
                logger.warn(desc + " — attempt " + attempt + " failed ("
                        + transient_.getClass().getSimpleName() + ": "
                        + transient_.getMessage() + "), retrying in " + sleep + "ms");
                try { Thread.sleep(sleep); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw transient_;
                }
            } catch (Exception nonTransient) {
                // Permanent error — duplicate key, schema mismatch, etc. Don't retry.
                throw nonTransient;
            }
        }
        if (last != null) throw last;
    }
}
