package com.vinplay.vbee.deposit;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.DepositMessage;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * RabbitMQ processor for deposit bank notifications.
 *
 * Message is Java-serialized DepositMessage (via BaseMessage.toBytes / ObjectOutputStream).
 * Deserializes with ObjectInputStream, then sends Telegram notification.
 *
 * ACK policy:
 *   - return true  → ack (message consumed successfully or idempotent skip)
 *   - return false → nack (message will be retried / sent to DLQ)
 */
public class DepositQueueConsumer implements BaseProcessor<byte[], Boolean> {

    private static final Logger logger = Logger.getLogger("api");

    private static volatile DepositTelegramBot sharedBot;
    private static volatile DepositTransactionDao sharedDao;

    public static void init(DepositTelegramBot bot, DepositTransactionDao dao) {
        sharedBot = bot;
        sharedDao = dao;
    }

    @Override
    public Boolean execute(Param<byte[]> param) {
        try {
            byte[] body = (byte[]) param.get();

            // Deserialize Java object (BaseMessage.toBytes uses ObjectOutputStream)
            DepositMessage message;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(body);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                message = (DepositMessage) ois.readObject();
            }

            long txId = message.getTxId();
            String txCode = message.getTxCode();
            String nickName = message.getNickName();
            long amount = message.getAmount();
            String bankName = message.getBankName();
            String bankNumber = message.getBankNumber();
            String holderName = message.getHolderName();

            // Invalid message — ack to discard (no point retrying)
            if (txId <= 0 || txCode == null) {
                logger.error("DepositQueueConsumer: invalid message, txId=" + txId + " txCode=" + txCode + " — discarding");
                return true;
            }

            logger.info("DepositQueueConsumer: received deposit txId=" + txId
                    + " txCode=" + txCode + " nickName=" + nickName + " amount=" + amount);

            DepositTelegramBot bot = sharedBot;
            DepositTransactionDao dao = sharedDao;

            // Bot/DAO not initialized — nack for retry (transient startup issue)
            if (bot == null || dao == null) {
                logger.error("DepositQueueConsumer: bot or dao not initialized — will retry");
                return false;
            }

            // Idempotency check — already notified, ack to skip
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            if (tx != null && tx.telegramMsgId > 0) {
                logger.info("DepositQueueConsumer: txId=" + txId + " already notified, skipping");
                return true;
            }

            // Telegram bot not configured — ack (admin chose not to use Telegram)
            if (!bot.isConfigured()) {
                logger.warn("DepositQueueConsumer: Telegram bot not configured, skipping notification");
                return true;
            }

            // Send Telegram notification
            long msgId = bot.sendDepositNotification(txId, txCode, nickName, amount, bankName, bankNumber, holderName);

            if (msgId <= 0) {
                // Telegram send failed — nack for retry
                logger.warn("DepositQueueConsumer: failed to send Telegram for txId=" + txId + " — will retry");
                return false;
            }

            // Update DB with telegram message ID
            boolean dbUpdated = dao.setTelegramMessageId(txId, msgId, bot.getChatId());
            if (!dbUpdated) {
                // DB update failed — nack for retry (Telegram msg already sent, idempotency will catch it next time)
                logger.warn("DepositQueueConsumer: DB update failed for txId=" + txId + " msgId=" + msgId + " — will retry");
                return false;
            }

            logger.info("DepositQueueConsumer: Telegram notification sent for txId=" + txId + " msgId=" + msgId);
            return true;

        } catch (Exception e) {
            logger.error("DepositQueueConsumer error — will retry", e);
            return false;
        }
    }
}
