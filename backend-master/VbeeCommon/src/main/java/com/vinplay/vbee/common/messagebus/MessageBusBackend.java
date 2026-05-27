package com.vinplay.vbee.common.messagebus;

/**
 * Identifies which transport a {@link MessageBus} instance is bound to.
 *
 * <p>Returned by {@link MessageBus#backend()} so tests and reconciliation
 * tooling can know which transport actually delivered a given message during
 * the staged RMQ &rarr; Redis Streams migration.
 *
 * <ul>
 *   <li>{@link #RMQ}    &mdash; legacy RabbitMQ transport (pre-migration default).</li>
 *   <li>{@link #REDIS}  &mdash; Redis Streams transport (post-cutover).</li>
 *   <li>{@link #DUAL}   &mdash; dual-write fan-out: publishes to both RMQ and
 *       Redis. Used during cutover so reconciliation can compare deliveries
 *       before fully shifting a queue's traffic.</li>
 * </ul>
 */
public enum MessageBusBackend {
    RMQ,
    REDIS,
    DUAL
}
