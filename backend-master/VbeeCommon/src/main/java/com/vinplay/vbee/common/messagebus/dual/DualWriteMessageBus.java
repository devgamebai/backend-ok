package com.vinplay.vbee.common.messagebus.dual;

import com.vinplay.vbee.common.messagebus.MessageBus;
import com.vinplay.vbee.common.messagebus.MessageBusBackend;
import com.vinplay.vbee.common.messages.BaseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fan-out {@link MessageBus} that publishes to BOTH the RMQ and Redis
 * adapters. Used during the S1 dual-write soak so reconciliation tooling can
 * compare deliveries on both transports before any per-queue cutover.
 *
 * <h2>Order &amp; authority</h2>
 * RMQ is published first, Redis second. RMQ is the source of truth during the
 * dual-write window: if the Redis branch fails, downstream consumers still see
 * the message via the legacy RMQ path, so end-user-visible behavior never
 * regresses during the soak. The opposite ordering would risk a Redis success
 * masking an RMQ failure, breaking the rollback safety net.
 *
 * <h2>Failure semantics</h2>
 * Both delegate calls run inside their own {@code try/catch (Throwable)} so a
 * failure on one branch never aborts the other. The underlying adapters
 * already log-and-swallow per the {@link MessageBus#publish} contract, but
 * this class re-asserts swallow semantics in case the contract is ever
 * tightened to throw &mdash; the dual-write soak must not introduce a new
 * failure mode that producer call sites have to handle.
 *
 * <p>Note on {@link Error}: catching {@link Throwable} is intentional here.
 * The publish path is fire-and-forget at every existing call site; allowing a
 * stray {@code Error} (e.g. transient {@code NoClassDefFoundError} on a slow
 * lazy-init) to escape would crash the producer thread. The trade-off is that
 * truly fatal errors (e.g. {@code OutOfMemoryError}) are also swallowed, but
 * they will rapidly resurface on the next allocation anyway and are visible
 * in the JVM-wide {@code OnOutOfMemoryError} hook the deployment uses.
 *
 * <h2>Thread safety</h2>
 * Stateless beyond the two final delegate references; thread safety is
 * inherited from the delegates.
 *
 * <h2>Lifecycle</h2>
 * {@link #close()} closes BOTH delegates in a {@code try/finally} chain so a
 * throwing {@link MessageBus#close()} on the RMQ side cannot prevent the
 * Redis side from releasing its Lettuce connection (and vice versa).
 */
public final class DualWriteMessageBus implements MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(DualWriteMessageBus.class);

    private final MessageBus rmq;
    private final MessageBus redis;

    /**
     * @param rmq   the RabbitMQ delegate (non-null). Published first.
     * @param redis the Redis Streams delegate (non-null). Published second.
     * @throws IllegalArgumentException if either delegate is null.
     */
    public DualWriteMessageBus(MessageBus rmq, MessageBus redis) {
        if (rmq == null) {
            throw new IllegalArgumentException("rmq delegate must be non-null");
        }
        if (redis == null) {
            throw new IllegalArgumentException("redis delegate must be non-null");
        }
        this.rmq = rmq;
        this.redis = redis;
    }

    @Override
    public void publish(String queueName, BaseMessage message, int command) {
        // Argument validation is delegated to the underlying buses so error
        // semantics stay identical to a single-backend deployment. We do not
        // pre-validate here because that would fail fast on null and skip the
        // second branch — which is the opposite of "publish to both, swallow
        // either failure" that this class promises.
        try {
            rmq.publish(queueName, message, command);
        } catch (Throwable t) {
            // The underlying RmqMessageBus already swallows transport errors
            // — reaching this catch means either (a) an IllegalArgumentException
            // from null/blank arguments or (b) a future contract change. Log
            // and continue so the Redis branch still runs.
            logger.warn("DualWriteMessageBus: RMQ publish failed queue={} cmd={}: {}",
                    queueName, command, t.getMessage(), t);
        }

        try {
            redis.publish(queueName, message, command);
        } catch (Throwable t) {
            logger.warn("DualWriteMessageBus: Redis publish failed queue={} cmd={}: {}",
                    queueName, command, t.getMessage(), t);
        }
    }

    @Override
    public void publishOrThrow(String queueName, BaseMessage message, int command) throws Exception {
        // Wallet-critical variant. RMQ is the primary during dual-write soak,
        // so an RMQ failure must surface to the caller's compensate path.
        // A Redis failure is logged-and-swallowed because RMQ has already
        // accepted the message — wallet correctness is preserved by RMQ
        // delivery alone during the soak. Redis-side audit miss is detectable
        // via the O1 audit table.
        rmq.publishOrThrow(queueName, message, command);
        try {
            redis.publishOrThrow(queueName, message, command);
        } catch (Throwable t) {
            logger.warn("DualWriteMessageBus.publishOrThrow: Redis branch failed (RMQ ok) queue={} cmd={}: {}",
                    queueName, command, t.getMessage(), t);
        }
    }

    @Override
    public MessageBusBackend backend() {
        return MessageBusBackend.DUAL;
    }

    @Override
    public void close() {
        // try/finally chain guarantees redis.close() runs even if rmq.close()
        // throws. Both delegates document close() as idempotent so calling
        // them again from MessageBusFactory.shutdown's shared-singleton sweep
        // is harmless.
        try {
            rmq.close();
        } catch (RuntimeException e) {
            logger.warn("DualWriteMessageBus.close: RMQ close failed: {}", e.getMessage(), e);
        } finally {
            try {
                redis.close();
            } catch (RuntimeException e) {
                logger.warn("DualWriteMessageBus.close: Redis close failed: {}", e.getMessage(), e);
            }
        }
    }
}
