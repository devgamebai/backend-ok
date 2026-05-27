package com.vinplay.vbee.common.mongodb;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import org.apache.log4j.Logger;

/**
 * Retry helper for transient Mongo driver failures.
 *
 * Context: mongo-java-driver 3.12 holds a stale connection pool when the
 * MongoDB server restarts. The first N round-loop calls after the restart
 * throw MongoSocketException / MongoSocketOpenException / MongoTimeoutException
 * until the SDAM thread rebuilds the pool. Wrapping critical writes in a
 * short retry-with-backoff lets the round loop survive a restart without
 * dropping bets or freezing the player countdown (GitLab infra issue #1).
 *
 * Usage:
 *     MongoRetryHelper.run(() -> collection.deleteMany(filter), "sicbo.clearUserBet");
 *     MongoRetryHelper.call(() -> collection.find(filter).first(), "sicbo.lookupBet");
 *
 * - Retries up to 3 times (total 4 attempts).
 * - Backoff 200ms, 400ms, 800ms.
 * - Only retries MongoSocketException (covers MongoSocketOpenException,
 *   MongoSocketReadException, MongoSocketWriteException, etc.) and
 *   MongoTimeoutException. Everything else (duplicate key, write conflict,
 *   bad filter) is propagated immediately.
 * - On final failure, re-throws the last MongoException. Caller decides
 *   whether to abort the loop or proceed.
 */
public final class MongoRetryHelper {

    private static final Logger logger = Logger.getLogger("api");
    private static final int MAX_ATTEMPTS = 4;
    private static final long BASE_BACKOFF_MS = 200L;

    private MongoRetryHelper() {}

    public interface MongoOp { void run() throws Exception; }
    public interface MongoCall<T> { T call() throws Exception; }

    /** Retry a void operation. */
    public static void run(MongoOp op, String label) {
        call(() -> { op.run(); return null; }, label);
    }

    /** Retry a value-returning operation. */
    public static <T> T call(MongoCall<T> op, String label) {
        RuntimeException last = null;
        long backoff = BASE_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return op.call();
            } catch (MongoSocketException | MongoTimeoutException e) {
                last = e;
                if (attempt == MAX_ATTEMPTS) break;
                logger.warn("MongoRetry[" + label + "] attempt " + attempt + "/" + MAX_ATTEMPTS
                        + " failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — retrying in " + backoff + "ms");
                try { Thread.sleep(backoff); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("interrupted during mongo retry backoff", ie);
                }
                backoff *= 2;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        logger.error("MongoRetry[" + label + "] exhausted after " + MAX_ATTEMPTS + " attempts", last);
        throw last;
    }
}
