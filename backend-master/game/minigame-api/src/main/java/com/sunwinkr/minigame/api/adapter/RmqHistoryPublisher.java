package com.sunwinkr.minigame.api.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes settled-round TaiXiu history messages to RabbitMQ
 * {@code queue_taixiu} (spec §6).
 *
 * <p>Message type codes:
 * <ul>
 *   <li>{@code 100} — TransactionTaiXiuMessage</li>
 *   <li>{@code 101} — ResultTaiXiuMessage</li>
 *   <li>{@code 102} — TransactionTaiXiuDetailMessage</li>
 * </ul>
 *
 * <p>PR-4 baseline: the publish path resolves
 * {@code com.vinplay.vbee.common.messagebus.MessageBusFactory.get(...).publish(...)}
 * via reflection so the API jar compiles on branches where the
 * factory hasn't yet been merged. Failures log WARN and return
 * (mirrors TXR:1359-1365 fire-and-forget).
 *
 * <p>Plan §4.4.
 */
public class RmqHistoryPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(RmqHistoryPublisher.class);

    private static final String QUEUE = "queue_taixiu";
    private static final int TYPE_TRANSACTION = 100;
    private static final int TYPE_RESULT = 101;
    private static final int TYPE_DETAIL = 102;

    private static final String FACTORY_CLASS =
        "com.vinplay.vbee.common.messagebus.MessageBusFactory";

    public void publishTransaction(Object msg) {
        publish(msg, TYPE_TRANSACTION, "publishTransaction");
    }

    public void publishResult(Object msg) {
        publish(msg, TYPE_RESULT, "publishResult");
    }

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
            LOG.warn("RmqHistoryPublisher.{} skipped — MessageBusFactory not on classpath", tag);
        } catch (Throwable t) {
            LOG.warn("RmqHistoryPublisher." + tag + " failed", t);
        }
    }
}
