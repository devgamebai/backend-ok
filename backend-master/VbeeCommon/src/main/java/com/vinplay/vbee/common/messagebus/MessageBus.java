package com.vinplay.vbee.common.messagebus;

import com.vinplay.vbee.common.messages.BaseMessage;

/**
 * Transport-neutral durable async work-queue contract.
 *
 * <p>Abstracts the producer side of the platform's fire-and-forget messaging:
 * callers hand off a {@link BaseMessage} addressed to a logical queue, and the
 * implementation persists it to whichever transport that queue is currently
 * routed through. The message will eventually be delivered to a single
 * consumer process from the queue's consumer group (work-queue semantics, not
 * pub/sub fan-out).
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@code RmqMessageBus} — legacy RabbitMQ adapter (delivered in F2).
 *       Preserves the exact wire format of the pre-migration
 *       {@code RMQApi.publishMessage} path.</li>
 *   <li>{@code RedisStreamMessageBus} — Redis Streams adapter (delivered in
 *       F3). Uses {@code XADD} for publish and consumer groups
 *       ({@code XREADGROUP} / {@code XACK}) on the consumer side.</li>
 *   <li>{@code DualWriteMessageBus} — fan-out adapter that publishes to both
 *       RMQ and Redis during cutover so reconciliation tooling can compare
 *       deliveries before traffic is fully shifted.</li>
 * </ul>
 *
 * <h2>Selection</h2>
 * Per-queue at runtime via {@code MessageBusFactory} (delivered in F5). A
 * single JVM may hold one {@code MessageBus} per logical queue, each pointing
 * at a different backend, allowing the migration to advance one queue at a
 * time. Producer call sites only depend on this interface; no call site needs
 * to know which transport is actually being used.
 *
 * @see MessageBusBackend
 * @see BaseMessage
 */
public interface MessageBus {

    /**
     * Publish a message to the given queue for asynchronous processing by a
     * single consumer in the queue's consumer group.
     *
     * <h2>Threading</h2>
     * Implementations MUST be thread-safe. The expected deployment is one
     * {@code MessageBus} instance per logical queue, shared across all
     * producer threads in the JVM. Callers do not need to synchronize.
     *
     * <h2>Failure semantics</h2>
     * Transient transport errors (broker unreachable, connection drop,
     * publisher-confirm timeout, Redis {@code XADD} failure, etc.) are logged
     * and the message is dropped. They are NEVER thrown to the caller. This
     * preserves the existing behavior of the legacy
     * {@code RMQApi.publishMessage} path so that the producer-side migration
     * does not change error visibility on any call site.
     *
     * <p>This method throws ONLY on programmer error — specifically when an
     * argument violates the precondition (null {@code message} or null/blank
     * {@code queueName}). Those are surfaced as {@link IllegalArgumentException}
     * because they indicate a defect in the calling code, not a runtime
     * transport issue.
     *
     * <h2>Throws-clause guidance for producer migration (M-* tasks)</h2>
     * Callers may keep or drop their existing
     * {@code throws IOException, TimeoutException, InterruptedException}
     * declarations on the calling method — both compile. Producer migration
     * tasks (M-*) should NOT remove existing throws declarations even though
     * {@code publish()} cannot throw them; preserves rollback diff cleanliness.
     *
     * @param queueName the logical queue identifier (must be non-null and
     *                  non-blank); used by {@code MessageBusFactory} to pick
     *                  the backend and by the implementation as the AMQP
     *                  routing key (RMQ) or Redis stream key (Redis).
     * @param message   the payload to enqueue (must be non-null). Serialized
     *                  by the implementation using the {@code BaseMessage}
     *                  contract; callers do not need to pre-serialize.
     * @param command   legacy {@code int} dispatcher key; preserved verbatim
     *                  because processors still switch on it. Wire format:
     *                  stored as a UTF-8-encoded string of digits in the
     *                  Redis stream's {@code c} field (Redis path) or as the
     *                  legacy AMQP property (RMQ path).
     * @throws IllegalArgumentException if {@code message} is null or
     *                                  {@code queueName} is null or blank.
     */
    void publish(String queueName, BaseMessage message, int command);

    /**
     * Publish a message and surface transport errors to the caller — the
     * wallet-critical-call-site variant of {@link #publish}.
     *
     * <h2>When to use</h2>
     * Use this method when the caller has already mutated wallet state
     * (debited / credited / pre-gated) and the publish is the durable record
     * of that mutation. If the publish silently fails, the caller has no way
     * to know it needs to compensate — wallet decreases without an audit row,
     * or the downstream processor never runs and a credit never lands.
     * Examples: {@code UserServiceImpl.updateMoney}, the per-method
     * {@code payment/service/impl/*} flows, {@code MoneyInGameServiceImpl}.
     *
     * <p>Use {@link #publish} (silent variant) for fire-and-forget side
     * effects whose loss is recoverable post-hoc via the audit table — log
     * rows, rebate triggers, telegram alerts.
     *
     * <h2>Failure semantics</h2>
     * Transport-level failures (broker unreachable, connection drop, Redis
     * {@code XADD} error, etc.) are surfaced to the caller as
     * {@link Exception} so the existing compensate / rollback paths fire.
     *
     * <p>For {@code DualWriteMessageBus}: throws iff the PRIMARY backend
     * fails. Secondary failure is logged and swallowed (the message still
     * lands on the primary, so wallet correctness is preserved during
     * dual-write soak). The primary is RMQ in {@code dual} mode and Redis
     * after S2 cutover; the underlying adapter selection is handled by
     * {@code MessageBusFactory}.
     *
     * @throws IllegalArgumentException if {@code message} is null or
     *                                  {@code queueName} is null or blank.
     * @throws Exception                if the primary backend fails. Existing
     *                                  callers' {@code catch (Exception e)}
     *                                  blocks already catch this; no
     *                                  signature change is needed at the
     *                                  call site beyond the method swap.
     */
    void publishOrThrow(String queueName, BaseMessage message, int command) throws Exception;

    /**
     * Returns the active backend identifier for this {@code MessageBus}
     * instance — the transport that will actually deliver messages published
     * through {@link #publish}.
     *
     * <p>Used by tests and reconciliation tooling to know which transport
     * actually delivered a message. For example, after a per-queue cutover
     * tests can assert that {@code backend() == REDIS}; reconciliation jobs
     * use this to know whether to query RMQ management API, Redis
     * {@code XINFO}, or both ({@code DUAL}) when verifying delivery.
     *
     * @return the backend currently bound to this instance; never null.
     */
    MessageBusBackend backend();

    /**
     * Graceful shutdown — flush any in-flight publishes and release transport
     * resources (RMQ channels and connections, Lettuce {@code StatefulRedisConnection}
     * handles, executor pools, etc.).
     *
     * <p>Idempotent: safe to call twice. The second and subsequent calls are
     * no-ops.
     *
     * <p>Called by {@code MessageBusFactory.shutdown()} from the JVM shutdown
     * hook. Application code generally does not need to call this directly;
     * it exists on the interface so tests and the factory can drive lifecycle
     * deterministically.
     */
    void close();
}
