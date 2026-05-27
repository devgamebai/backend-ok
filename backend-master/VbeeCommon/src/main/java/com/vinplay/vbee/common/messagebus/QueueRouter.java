package com.vinplay.vbee.common.messagebus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared queue-routing rules used by both {@code RmqMessageBus} (F2) and
 * {@code RedisStreamMessageBus} (F3). Encapsulates the legacy logic from
 * {@link com.vinplay.vbee.common.rmq.RMQApi#publishMessagePayment(
 * com.vinplay.vbee.common.messages.BaseMessage, int)
 * RMQApi.publishMessagePayment} and
 * {@link com.vinplay.vbee.common.rmq.RMQApi#publishMessageLogMoney(
 * com.vinplay.vbee.common.messages.LogMoneyUserMessage)
 * RMQApi.publishMessageLogMoney} so that both transport backends route
 * messages to the same logical destinations.
 *
 * <h2>Sentinel queue names</h2>
 * Producers signal which legacy entry point they want by passing a sentinel
 * queue name to {@link MessageBus#publish}:
 * <ul>
 *   <li>{@code "queue_payment"} &mdash; equivalent to calling
 *       {@code publishMessagePayment(msg, cmd)}: the router picks the real
 *       queue (and possibly translates the cmd) based on the input cmd.</li>
 *   <li>{@code "queue_log_money"} &mdash; equivalent to calling
 *       {@code publishMessageLogMoney(msg)}: fans out to three concrete
 *       queues with hard-coded cmd ids; the input cmd is ignored.</li>
 *   <li>any other queue name &mdash; equivalent to a direct
 *       {@code publishMessage(queueName, msg, cmd)}: a single 1-hop pass-through.</li>
 * </ul>
 *
 * <h2>Source of truth</h2>
 * All rules below are taken verbatim from
 * {@code VbeeCommon/src/main/java/com/vinplay/vbee/common/rmq/RMQApi.java}.
 * If RMQApi changes, this class must follow it. Line numbers are cited next
 * to each rule for traceability.
 */
public final class QueueRouter {

    /** Sentinel queue name that triggers payment-cmd routing (RMQApi.java:21-46). */
    public static final String QUEUE_PAYMENT = "queue_payment";

    /** Sentinel queue name that triggers log_money fan-out (RMQApi.java:48-56). */
    public static final String QUEUE_LOG_MONEY = "queue_log_money";

    /** A single (queue, command) tuple after routing. Immutable. */
    public static final class Hop {
        public final String queueName;
        public final int command;

        public Hop(String queueName, int command) {
            this.queueName = queueName;
            this.command = command;
        }

        @Override
        public String toString() {
            return "Hop{queueName='" + queueName + "', command=" + command + '}';
        }
    }

    /**
     * Resolve the input {@code (queueName, command)} into one or more
     * {@link Hop}s.
     *
     * <ul>
     *   <li>{@code "queue_payment"} &rarr; 1 hop, with cmd-specific queue +
     *       cmd translation per RMQApi.java:23-43:
     *       <ul>
     *         <li>cmd 16 &rarr; {@code (queue_payment_minigame, 30)} (RMQApi.java:24-27)</li>
     *         <li>cmd 10 &rarr; {@code (queue_payment_gamebai, 40)} (RMQApi.java:29-32)</li>
     *         <li>cmd 12 &rarr; {@code (queue_payment_gamebai, 41)} (RMQApi.java:34-37)</li>
     *         <li>cmd 13 &rarr; {@code (queue_payment_gamebai, 42)} (RMQApi.java:39-42)</li>
     *         <li>any other cmd &rarr; {@code (queue_payment, cmd)} &mdash; the
     *             switch falls through and {@code queueName} stays
     *             {@code "queue_payment"} (RMQApi.java:22, default fall-through).</li>
     *       </ul>
     *   </li>
     *   <li>{@code "queue_log_money"} &rarr; 3 hops (RMQApi.java:49-55), order
     *       preserved exactly as in RMQApi:
     *       <ol>
     *         <li>{@code (queue_log_money, 601)}</li>
     *         <li>{@code (queue_log_money_extra, 1001)}</li>
     *         <li>{@code (queue_log_report_user_balance, 602)}</li>
     *       </ol>
     *       The input {@code command} is ignored &mdash; RMQApi hard-codes
     *       these three cmd ids regardless of caller input.</li>
     *   <li>any other queue name &rarr; 1 hop {@code (queueName, command)}
     *       (RMQApi.java:16-19, the plain {@code publishMessage} path).</li>
     * </ul>
     *
     * @param queueName the logical queue identifier passed by the caller; must
     *                  be non-null and non-blank.
     * @param command   the legacy dispatcher cmd id; used for payment-routing
     *                  selection and as the literal cmd for pass-through.
     *                  Ignored for {@code queue_log_money}.
     * @return immutable list of one or more hops; never null, never empty.
     * @throws IllegalArgumentException if {@code queueName} is null or blank.
     */
    public static List<Hop> route(String queueName, int command) {
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("queueName must be non-null and non-blank");
        }

        // RMQApi.java:48-56 — publishMessageLogMoney triple fan-out.
        // Input cmd is ignored; hard-coded cmd ids 601/1001/602.
        if (QUEUE_LOG_MONEY.equals(queueName)) {
            List<Hop> hops = new ArrayList<>(3);
            hops.add(new Hop("queue_log_money", 601));
            hops.add(new Hop("queue_log_money_extra", 1001));
            hops.add(new Hop("queue_log_report_user_balance", 602));
            return Collections.unmodifiableList(hops);
        }

        // RMQApi.java:21-46 — publishMessagePayment cmd-keyed switch.
        if (QUEUE_PAYMENT.equals(queueName)) {
            String effectiveQueue = QUEUE_PAYMENT;
            int effectiveCmd = command;
            switch (command) {
                case 16: // RMQApi.java:24-27
                    effectiveQueue = "queue_payment_minigame";
                    effectiveCmd = 30;
                    break;
                case 10: // RMQApi.java:29-32
                    effectiveQueue = "queue_payment_gamebai";
                    effectiveCmd = 40;
                    break;
                case 12: // RMQApi.java:34-37
                    effectiveQueue = "queue_payment_gamebai";
                    effectiveCmd = 41;
                    break;
                case 13: // RMQApi.java:39-42
                    effectiveQueue = "queue_payment_gamebai";
                    effectiveCmd = 42;
                    break;
                default:
                    // RMQApi.java:22 default — queueName stays "queue_payment",
                    // command unchanged. Note RMQApi.java:41 is missing a
                    // `break` after case 13 but it is the last labeled case
                    // before the closing brace, so behavior is identical to
                    // an explicit break — replicated here as such.
                    break;
            }
            return Collections.singletonList(new Hop(effectiveQueue, effectiveCmd));
        }

        // RMQApi.java:16-19 — plain publishMessage pass-through.
        return Collections.singletonList(new Hop(queueName, command));
    }

    private QueueRouter() {
        // utility class — no instances
    }
}
