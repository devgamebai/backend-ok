package com.vinplay.dal.withdraw;

import com.vinplay.dal.telegram.TelegramBotClient;
import org.apache.log4j.Logger;

/**
 * SUN-1220 — Telegram notifications for bank-withdrawal requests.
 *
 * <p>Mirrors {@code TelegramCryptoWithdrawNotifier} but with bank
 * (not USDT) details: bank name, bank number, customer name. Wire
 * layer (HTTP, send/edit, button-clear, HTML escape) is shared via
 * {@link TelegramBotClient} — this class only owns the bank-specific
 * message templates.
 *
 * <p>Message lifecycle:
 * <ol>
 *   <li>Player creates withdraw → {@link #sendNewWithdrawal} posts a
 *       message with [✅ Duyệt] [❌ Từ chối] buttons.</li>
 *   <li>Admin clicks Duyệt (Telegram) or hits the CMS endpoint →
 *       {@link #editMessageApproved} replaces text + clears buttons.</li>
 *   <li>Admin clicks Từ chối (Telegram) or hits the CMS endpoint →
 *       {@link #editMessageRejected} replaces text + clears buttons.</li>
 * </ol>
 *
 * <p>Callback data shape MUST match the
 * {@code DepositTelegramPoller.handleCallbackQuery} bank_approve /
 * bank_reject branches — keep the prefixes in sync.
 */
public final class TelegramBankWithdrawNotifier {

    private static final Logger logger = Logger.getLogger("api");

    private final TelegramBotClient client;

    public TelegramBankWithdrawNotifier() {
        this.client = TelegramBotClient.getInstance();
    }

    public boolean isEnabled() { return client.isConfigured(); }

    /** Post a new pending bank-withdraw message with Approve/Reject buttons.
     *  Returns the Telegram {@code message_id} for later edits, or -1
     *  on any failure. */
    public long sendNewWithdrawal(long dbId, String nickName, String bankName,
                                   String bankNumber, String customerName,
                                   long amountKrw, String txCode) {
        if (!client.isConfigured()) return -1;
        String text = "🏦 <b>RÚT BANK - CHỜ DUYỆT</b>\n\n"
                + "👤 Player: <code>" + TelegramBotClient.escHtml(nickName) + "</code>\n"
                + "💰 Số tiền: " + String.format("%,d", amountKrw) + " KRW\n"
                + "🏦 Ngân hàng: <code>" + TelegramBotClient.escHtml(bankName) + "</code>\n"
                + "💳 Số tài khoản: <code>" + TelegramBotClient.escHtml(bankNumber) + "</code>\n"
                + "👨‍💼 Tên TK: <code>"
                + TelegramBotClient.escHtml(customerName != null ? customerName : "-")
                + "</code>\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "\n⏳ Trạng thái: <b>PENDING — chờ admin duyệt</b>";

        String keyboard = "{\"inline_keyboard\":[["
                + "{\"text\":\"✅ Duyệt\",\"callback_data\":\"bank_approve:" + dbId + "\"},"
                + "{\"text\":\"❌ Từ chối\",\"callback_data\":\"bank_reject:" + dbId + "\"}"
                + "]]}";

        long msgId = client.sendMessage(text, keyboard, "HTML");
        if (msgId < 0) {
            logger.warn("TelegramBankWithdrawNotifier sendNewWithdrawal failed dbId=" + dbId);
        }
        return msgId;
    }

    /** Edit message to APPROVED state, buttons cleared. */
    public void editMessageApproved(long messageId, String txCode, long amountKrw,
                                     String nickName, String bankName, String bankNumber,
                                     String customerName, String adminBy) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "✅ <b>RÚT BANK - ĐÃ DUYỆT</b>\n\n"
                + "👤 Player: <code>" + TelegramBotClient.escHtml(nickName) + "</code>\n"
                + "💰 Số tiền: " + String.format("%,d", amountKrw) + " KRW\n"
                + "🏦 Ngân hàng: <code>" + TelegramBotClient.escHtml(bankName) + "</code>\n"
                + "💳 Số tài khoản: <code>" + TelegramBotClient.escHtml(bankNumber) + "</code>\n"
                + "👨‍💼 Tên TK: <code>"
                + TelegramBotClient.escHtml(customerName != null ? customerName : "-")
                + "</code>\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "\n👨‍💻 Duyệt bởi: <b>" + TelegramBotClient.escHtml(adminBy) + "</b>";
        client.editMessage(messageId, text, "", "HTML");
    }

    /** Edit message to REJECTED state, buttons cleared. */
    public void editMessageRejected(long messageId, String txCode, long amountKrw,
                                     String nickName, String bankName, String bankNumber,
                                     String customerName, String adminBy, String reason) {
        if (!client.isConfigured() || messageId <= 0) return;
        String text = "❌ <b>RÚT BANK - TỪ CHỐI</b>\n\n"
                + "👤 Player: <code>" + TelegramBotClient.escHtml(nickName) + "</code>\n"
                + "💰 Số tiền: " + String.format("%,d", amountKrw) + " KRW\n"
                + "🏦 Ngân hàng: <code>" + TelegramBotClient.escHtml(bankName) + "</code>\n"
                + "💳 Số tài khoản: <code>" + TelegramBotClient.escHtml(bankNumber) + "</code>\n"
                + "👨‍💼 Tên TK: <code>"
                + TelegramBotClient.escHtml(customerName != null ? customerName : "-")
                + "</code>\n"
                + "🔖 Mã GD: <code>" + TelegramBotClient.escHtml(txCode) + "</code>\n"
                + "\n👨‍💻 Từ chối bởi: <b>" + TelegramBotClient.escHtml(adminBy) + "</b>"
                + (reason != null && !reason.isEmpty()
                        ? "\n💬 Lý do: " + TelegramBotClient.escHtml(reason) : "");
        client.editMessage(messageId, text, "", "HTML");
    }
}
