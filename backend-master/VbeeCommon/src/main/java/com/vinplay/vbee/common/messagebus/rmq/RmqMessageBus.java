package com.vinplay.vbee.common.messagebus.rmq;

import com.vinplay.vbee.common.messagebus.MessageBus;
import com.vinplay.vbee.common.messagebus.MessageBusBackend;
import com.vinplay.vbee.common.messagebus.QueueRouter;
import com.vinplay.vbee.common.messagebus.QueueRouter.Hop;
import com.vinplay.vbee.common.messagebus.audit.MessageBusAuditWriter;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.rmq.RMQPublishTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ-backed {@link MessageBus} adapter — strict behavioral pass-through
 * of the legacy
 * {@link com.vinplay.vbee.common.rmq.RMQApi#publishMessage(String,
 * com.vinplay.vbee.common.messages.BaseMessage, int) RMQApi.publishMessage} /
 * {@link com.vinplay.vbee.common.rmq.RMQApi#publishMessagePayment(
 * com.vinplay.vbee.common.messages.BaseMessage, int)
 * publishMessagePayment} /
 * {@link com.vinplay.vbee.common.rmq.RMQApi#publishMessageLogMoney(
 * com.vinplay.vbee.common.messages.LogMoneyUserMessage)
 * publishMessageLogMoney} entry points.
 *
 * <p>Routing decisions (cmd-keyed payment-queue selection, log_money triple
 * fan-out) are delegated to {@link QueueRouter} so F3's
 * {@code RedisStreamMessageBus} can reuse the same rules unchanged.
 *
 * <h2>Failure semantics</h2>
 * Per the {@link MessageBus#publish} contract, transport-level errors raised
 * by {@link RMQPublishTask#start()} ({@link IOException},
 * {@link TimeoutException}, {@link InterruptedException}) are caught, logged,
 * and swallowed. The legacy {@code RMQApi} declared these checked, but no
 * caller in the codebase actually handles them &mdash; they propagated up to
 * fire-and-forget producer call sites that just logged-and-moved-on, so this
 * adapter preserves that observable behavior.
 *
 * <h2>Lifecycle</h2>
 * {@code RMQPool} is a process-wide static singleton ({@code RMQPool.getInstance()}),
 * so this adapter holds no per-instance transport state. {@link #close()} is a
 * no-op.
 *
 * <h2>Thread safety</h2>
 * Stateless; safe for concurrent {@link #publish} calls from any thread.
 */
public final class RmqMessageBus implements MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(RmqMessageBus.class);

    public RmqMessageBus() {
    }

    @Override
    public void publish(String queueName, BaseMessage message, int command) {
        if (message == null) {
            throw new IllegalArgumentException("message must be non-null");
        }
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }

        List<Hop> hops = QueueRouter.route(queueName, command);
        for (Hop hop : hops) {
            boolean success = false;
            try {
                // Mirrors RMQApi.publishMessage / publishMessagePayment /
                // publishMessageLogMoney exactly: build an RMQPublishTask and
                // call start(). Each hop is published independently — a
                // failure on one fan-out target does not abort the others, to
                // match the legacy fan-out which relied on each task's own
                // try/catch boundary in the caller (or, more commonly, on
                // callers that simply did not handle the checked exceptions
                // and allowed them to propagate).
                new RMQPublishTask(message, hop.queueName, hop.command).start();
                success = true;
            } catch (IOException e) {
                logger.error("RmqMessageBus.publish IOException queue={} cmd={}: {}",
                        hop.queueName, hop.command, e.getMessage(), e);
            } catch (TimeoutException e) {
                logger.error("RmqMessageBus.publish TimeoutException queue={} cmd={}: {}",
                        hop.queueName, hop.command, e.getMessage(), e);
            } catch (InterruptedException e) {
                // Restore the interrupt flag so callers higher up the stack
                // observe the cancellation request, then continue with the
                // remaining hops the way the legacy code did (it never
                // special-cased InterruptedException either).
                Thread.currentThread().interrupt();
                logger.error("RmqMessageBus.publish InterruptedException queue={} cmd={}: {}",
                        hop.queueName, hop.command, e.getMessage(), e);
            } finally {
                // O1: record the publish outcome to the message_bus_audit
                // queue. Best-effort; the writer drops on overflow and never
                // blocks. Records the post-routing hop (queue/cmd actually
                // sent), not the original args, so reconciliation matches
                // what RedisStreamMessageBus records on the same publish.
                MessageBusAuditWriter.getInstance()
                        .record(hop.queueName, hop.command, MessageBusBackend.RMQ, success);
            }
        }
    }

    @Override
    public void publishOrThrow(String queueName, BaseMessage message, int command) throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("message must be non-null");
        }
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }
        List<Hop> hops = QueueRouter.route(queueName, command);
        for (Hop hop : hops) {
            boolean success = false;
            try {
                new RMQPublishTask(message, hop.queueName, hop.command).start();
                success = true;
            } catch(Throwable e){
                e.printStackTrace();
            } finally {
                MessageBusAuditWriter.getInstance()
                        .record(hop.queueName, hop.command, MessageBusBackend.RMQ, success);
            }
        }
    }

    @Override
    public MessageBusBackend backend() {
        return MessageBusBackend.RMQ;
    }

    @Override
    public void close() {
        // RMQPool is a process-wide static singleton; nothing to release per-instance.
    }
}
