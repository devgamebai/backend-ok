package com.vinplay.dal.deposit;

import com.vinplay.dal.telegram.TelegramBotClient;
import org.apache.log4j.Logger;

/**
 * Sends/edits Telegram messages for deposit notifications.
 * Used by Backend API processors to sync CMS actions to Telegram.
 *
 * <p>The wire layer (HTTP plumbing, money formatting, button-clear
 * behaviour on terminal states) lives in {@link TelegramBotClient} —
 * shared with {@code TelegramCryptoWithdrawNotifier} so a fix on one
 * notifier benefits both. This class only owns the deposit-specific
 * message templates.
 */
public final class TelegramDepositNotifier {

    private static final Logger logger = Logger.getLogger("api");
    private static final TelegramDepositNotifier INSTANCE = new TelegramDepositNotifier();

    private final TelegramBotClient client;

    private TelegramDepositNotifier() {
        this.client = TelegramBotClient.getInstance();
    }

    public static TelegramDepositNotifier getInstance() { return INSTANCE; }

    public boolean isConfigured() { return client.isConfigured(); }

    /** Edit message to show "picked/processing" state with Approve/Reject/Release buttons. */
    public void editMessagePicked(long messageId, long txId, String txCode,
                                   String nickName, long amount, String operatorName) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "🔄 PROCESSING #" + txCode + "\n"
                + "━━━━━━━━━━━━━━━\n"
                + "👤 User: " + nickName + "\n"
                + "💵 Số tiền: ₩" + TelegramBotClient.formatMoney(amount) + "\n"
                + "📋 Trạng thái: Đang xử lý\n"
                + "🔒 Operator: " + operatorName + "\n"
                + "💻 Platform: CMS";
        String keyboard = "{\"inline_keyboard\":[[" +
                "{\"text\":\"✅ Approve\",\"callback_data\":\"approve:" + txId + "\"}," +
                "{\"text\":\"❌ Reject\",\"callback_data\":\"reject:" + txId + "\"}" +
                "]]}";
        client.editMessage(messageId, text, keyboard, null);
        logger.info("TelegramDepositNotifier: msgId=" + messageId + " → PICKED by " + operatorName);
    }

    /**
     * Edit message to show "approved" state — buttons cleared, bank info
     * preserved (User request 2026-04-22 — operators reconcile against
     * bank statements after the fact).
     */
    public void editMessageApproved(long messageId, String txCode, long amount,
                                     String nickName, String operatorName,
                                     String bankName, String bankNumber, String holderName) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "✅ APPROVED #" + txCode + "\n"
                + "━━━━━━━━━━━━━━━\n"
                + "👤 User: " + nickName + "\n"
                + "💵 Số tiền: ₩" + TelegramBotClient.formatMoney(amount) + "\n"
                + "🏦 Bank: " + (bankName != null ? bankName : "-") + "\n"
                + "💳 Số TK: " + (bankNumber != null ? bankNumber : "-") + "\n"
                + "👤 Chủ TK: " + (holderName != null ? holderName : "-") + "\n"
                + "📋 Trạng thái: Đã duyệt ✅\n"
                + "👨‍💻 Operator: " + operatorName + "\n"
                + "💻 Platform: CMS\n"
                + "💰 Balance: Đã cộng tiền";
        // empty keyboard string clears existing buttons (terminal state)
        client.editMessage(messageId, text, "", null);
        logger.info("TelegramDepositNotifier: msgId=" + messageId + " → APPROVED by " + operatorName);
    }

    /** Legacy overload — kept for callers that don't have bank fields. */
    public void editMessageApproved(long messageId, String txCode, long amount,
                                     String nickName, String operatorName) {
        editMessageApproved(messageId, txCode, amount, nickName, operatorName, null, null, null);
    }

    /** Edit message to show "rejected" state — no buttons. */
    public void editMessageRejected(long messageId, String txCode, long amount,
                                     String nickName, String operatorName, String reason) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "❌ REJECTED #" + txCode + "\n"
                + "━━━━━━━━━━━━━━━\n"
                + "👤 User: " + nickName + "\n"
                + "💵 Số tiền: ₩" + TelegramBotClient.formatMoney(amount) + "\n"
                + "📋 Trạng thái: Từ chối ❌\n"
                + "👨‍💻 Operator: " + operatorName + "\n"
                + "💻 Platform: CMS\n"
                + "📝 Lý do: " + (reason != null ? reason : "-");
        client.editMessage(messageId, text, "", null);
        logger.info("TelegramDepositNotifier: msgId=" + messageId + " → REJECTED by " + operatorName);
    }

    /** Edit message back to PENDING with Pick/Reject buttons (release). */
    public void editMessageReleased(long messageId, long txId, String txCode,
                                     String nickName, long amount) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "💰 PENDING #" + txCode + "\n"
                + "━━━━━━━━━━━━━━━\n"
                + "👤 User: " + nickName + "\n"
                + "💵 Số tiền: ₩" + TelegramBotClient.formatMoney(amount) + "\n"
                + "📋 Trạng thái: Chờ xử lý\n"
                + "🔄 Đã được release bởi operator";
        String keyboard = "{\"inline_keyboard\":[[" +
                "{\"text\":\"🔍 Pick\",\"callback_data\":\"pick:" + txId + "\"}," +
                "{\"text\":\"❌ Reject\",\"callback_data\":\"reject:" + txId + "\"}" +
                "]]}";
        client.editMessage(messageId, text, keyboard, null);
        logger.info("TelegramDepositNotifier: msgId=" + messageId + " → RELEASED");
    }

    /**
     * Edit message to show "force approved" state — no buttons, ⚡ flag.
     * Used by c=9615 when an operator force-approves an expired or
     * rejected transaction.
     */
    public void editMessageForceApproved(long messageId, String txCode, long amount,
                                          String nickName, String operatorName,
                                          String bankName, String bankNumber, String holderName) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "⚡ FORCE APPROVED #" + txCode + "\n"
                + "━━━━━━━━━━━━━━━\n"
                + "👤 User: " + nickName + "\n"
                + "💵 Số tiền: ₩" + TelegramBotClient.formatMoney(amount) + "\n"
                + "🏦 Bank: " + (bankName != null ? bankName : "-") + "\n"
                + "💳 Số TK: " + (bankNumber != null ? bankNumber : "-") + "\n"
                + "👤 Chủ TK: " + (holderName != null ? holderName : "-") + "\n"
                + "📋 Trạng thái: Force Approved ⚡\n"
                + "👨‍💻 Operator: " + operatorName + "\n"
                + "💰 Balance: Đã cộng tiền";
        client.editMessage(messageId, text, "", null);
        logger.info("TelegramDepositNotifier: msgId=" + messageId + " → FORCE APPROVED by " + operatorName);
    }

    /** Legacy overload (no bank fields). */
    public void editMessageForceApproved(long messageId, String txCode, long amount,
                                          String nickName, String operatorName) {
        editMessageForceApproved(messageId, txCode, amount, nickName, operatorName, null, null, null);
    }
}
