package com.sunwinkr.minigame.api.adapter.sicbo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes settled Sicbo round history messages to RabbitMQ
 * {@code queue_taixiu_sicbo} (legacy SBR:1359-1365).
 *
 * <p>Message type codes for Sicbo are 28100/28101/28102 — analogous to
 * TaiXiu's 100/101/102 (plan §6). The payload classes
 * ({@code TransactionSicboMessage}, {@code ResultSicboMessage},
 * {@code TransactionSicboDetailMessage}) live in {@code VbeeCommon} and
 * are accepted as opaque {@code Object} here so this adapter compiles
 * against any matching wire-type without a hard package reference.
 *
 * <p>PR-4 baseline: the publish path resolves
 * {@code com.vinplay.vbee.common.messagebus.MessageBusFactory.get(...).publish(...)}
 * via reflection, which keeps the API jar buildable on branches where
 * the {@code MessageBusFactory} class has not yet been merged. When the
 * factory is on the classpath at runtime, publish proceeds; otherwise
 * the call logs WARN and returns — matching the legacy fire-and-forget
 * semantics (TXR:1359-1365).
 *
 * <p>Plan §6 (Sicbo analog of {@code RmqHistoryPublisher}).
 */
public class RmqSicboHistoryPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(RmqSicboHistoryPublisher.class);

    private static final String QUEUE = "queue_taixiu_sicbo";

    /** Sicbo TransactionMessage type id. */
    public static final int TYPE_TRANSACTION = 28100;
    /** Sicbo ResultMessage type id. */
    public static final int TYPE_RESULT = 28101;
    /** Sicbo TransactionDetailMessage type id. */
    public static final int TYPE_DETAIL = 28102;

    private static final String FACTORY_CLASS =
        "com.vinplay.vbee.common.messagebus.MessageBusFactory";

    /** Publish the round-level transaction record. */
    public void publishTransaction(Object msg) {
        publish(msg, TYPE_TRANSACTION, "publishTransaction");
    }

    /** Publish the round result record. */
    public void publishResult(Object msg) {
        publish(msg, TYPE_RESULT, "publishResult");
    }

    /** Publish a per-bet settlement detail record. */
    public void publishTransactionDetail(Object msg) {
        publish(msg, TYPE_DETAIL, "publishTransactionDetail");
    }

    private void publish(Object msg, int type, String tag) {
        try {
            Class<?> factoryCls = Class.forName(FACTORY_CLASS);
            Object bus = factoryCls.getMethod("get", String.class).invoke(null, QUEUE);
            bus.getClass()
                .getMethod("publish", String.class, Object.class, int.class)
                .invoke(bus, QUEUE, msg, type);
        } catch (ClassNotFoundException cnfe) {
            LOG.warn("RmqSicboHistoryPublisher.{} skipped — MessageBusFactory not on classpath", tag);
        } catch (Throwable t) {
            LOG.warn("RmqSicboHistoryPublisher." + tag + " failed", t);
        }
    }
}
