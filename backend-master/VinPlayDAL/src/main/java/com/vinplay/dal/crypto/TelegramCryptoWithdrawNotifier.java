package com.vinplay.dal.crypto;

import com.vinplay.dal.telegram.TelegramBotClient;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sends/edits Telegram messages for crypto-withdrawal requests.
 *
 * <p>Wire layer (HTTP, send/edit, button-clear, HTML escape) is shared
 * with {@code TelegramDepositNotifier} via {@link TelegramBotClient}.
 * This class only owns the crypto-withdraw-specific message templates.
 *
 * <p>Message lifecycle:
 * <ol>
 *   <li>Player creates withdraw → {@link #sendNewWithdrawal} posts a
 *       message with [✅ Duyệt] [❌ Từ chối] buttons.</li>
 *   <li>Admin clicks Duyệt (Telegram) or hits c=9631 (CMS) →
 *       {@link #editMessageApproved} replaces text + clears buttons.</li>
 *   <li>Admin clicks Từ chối (Telegram) or hits c=9632 (CMS) →
 *       {@link #editMessageRejected} replaces text + clears buttons.</li>
 *   <li>Hot wallet short of USDT before approve →
 *       {@link #sendHotWalletLowAlert} posts a separate alert; the
 *       original pending message stays so ops can retry after top-up.</li>
 * </ol>
 */
public final class TelegramCryptoWithdrawNotifier {

    private static final Logger logger = Logger.getLogger("api");

    private final TelegramBotClient client;

    public TelegramCryptoWithdrawNotifier() {
        this.client = TelegramBotClient.getInstance();
    }

    public boolean isEnabled() { return client.isConfigured(); }

    // Display only — formats USDT to 2 decimals for human-readable Telegram messages.
    // Money math is BigDecimal; double comparison/arithmetic on USDT is forbidden.
    private static String fmtUsdt(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Post a new pending-withdraw message with Approve/Reject buttons.
     *  Returns the Telegram {@code message_id} for later edits, or -1
     *  on any failure. */
    public long sendNewWithdrawal(long dbId, String nickName, String toAddress,
                                   long amountKrw, BigDecimal amountUsdt, String txCode) {
        if (!client.isConfigured()) return -1;
        String text = "💸 <b>RÚT CRYPTO - CHỜ DUYỆT</b>\n\n"
                + "👤 Player: <code>" + TelegramBotClient.escHtml(nickName) + "</code>\n"
                + "💰 Số tiền: " + String.format("%,d", amountKrw) + " KRW (" + fmtUsdt(amountUsdt) + " USDT)\n"
                + "🏦 Địa chỉ: <code>" + TelegramBotClient.escHtml(toAddress) + "</code>\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "\n⏳ Trạng thái: <b>PENDING — chờ admin duyệt</b>";

        // SUN-PROD: callback_data shape MUST match
        // DepositTelegramPoller.handleCallbackQuery → crypto_approve /
        // crypto_reject branches (wired to call CryptoWithdrawalApprovalService).
        String keyboard = "{\"inline_keyboard\":[["
                + "{\"text\":\"✅ Duyệt\",\"callback_data\":\"crypto_approve:" + dbId + "\"},"
                + "{\"text\":\"❌ Từ chối\",\"callback_data\":\"crypto_reject:" + dbId + "\"}"
                + "]]}";

        long msgId = client.sendMessage(text, keyboard, "HTML");
        if (msgId < 0) {
            logger.warn("TelegramCryptoWithdrawNotifier sendNewWithdrawal failed dbId=" + dbId);
        }
        return msgId;
    }

    /** Edit message to APPROVED state, buttons cleared. */
    public void editMessageApproved(long messageId, String txCode, long amountKrw,
                                     BigDecimal amountUsdt, String nickName, String toAddress, String adminBy) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "✅ <b>RÚT CRYPTO - ĐÃ DUYỆT</b>\n\n"
                + "👤 Player: <code>" + TelegramBotClient.escHtml(nickName) + "</code>\n"
                + "💰 Số tiền: " + String.format("%,d", amountKrw) + " KRW (" + fmtUsdt(amountUsdt) + " USDT)\n"
                + "🏦 Địa chỉ: <code>" + TelegramBotClient.escHtml(toAddress) + "</code>\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "\n👨‍💻 Duyệt bởi: <b>" + TelegramBotClient.escHtml(adminBy) + "</b>";
        client.editMessage(messageId, text, "", "HTML");
    }

    /** Edit message to REJECTED state, buttons cleared. */
    public void editMessageRejected(long messageId, String txCode, long amountKrw,
                                     BigDecimal amountUsdt, String nickName, String toAddress,
                                     String adminBy, String reason) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "❌ <b>RÚT CRYPTO - TỪ CHỐI</b>\n\n"
                + "👤 Player: <code>" + TelegramBotClient.escHtml(nickName) + "</code>\n"
                + "💰 Số tiền: " + String.format("%,d", amountKrw) + " KRW (" + fmtUsdt(amountUsdt) + " USDT)\n"
                + "🏦 Địa chỉ: <code>" + TelegramBotClient.escHtml(toAddress) + "</code>\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "\n👨‍💻 Từ chối bởi: <b>" + TelegramBotClient.escHtml(adminBy) + "</b>"
                + (reason != null && !reason.isEmpty()
                        ? "\n💬 Lý do: " + TelegramBotClient.escHtml(reason) : "");
        client.editMessage(messageId, text, "", "HTML");
    }

    /**
     * Post a NEW chat message when the gateway hot wallet is short of
     * USDT to fulfill a withdraw. The original pending message keeps
     * its [Duyệt] [Từ chối] buttons so ops can retry after topping up
     * — only this side message is new.
     */
    public void sendHotWalletLowAlert(String txCode, BigDecimal amountUsdt,
                                       BigDecimal hotBalance, String hotAddress,
                                       String operatorName) {
        if (!client.isConfigured()) return;
        BigDecimal shortfall = amountUsdt.subtract(hotBalance);
        if (shortfall.signum() < 0) shortfall = BigDecimal.ZERO;
        String text = "⚠️ <b>HOT WALLET KHÔNG ĐỦ - CHƯA RÚT ĐƯỢC</b>\n\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "💰 Cần rút: <b>" + fmtUsdt(amountUsdt) + " USDT</b>\n"
                + "🔋 Hot wallet hiện có: <b>" + fmtUsdt(hotBalance) + " USDT</b>\n"
                + "📉 Thiếu: <b>" + fmtUsdt(shortfall) + " USDT</b>\n"
                + "🏦 Địa chỉ hot: <code>"
                + TelegramBotClient.escHtml(hotAddress != null ? hotAddress : "(unknown)")
                + "</code>\n"
                + (operatorName != null && !operatorName.isEmpty()
                        ? "👨‍💻 Operator: <b>" + TelegramBotClient.escHtml(operatorName) + "</b>\n" : "")
                + "\n☕ <b>Vui lòng top up hot wallet rồi click Duyệt lại.</b>";
        client.sendMessage(text, null, "HTML");
    }
}
