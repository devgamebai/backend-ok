package com.vinplay.vbee.deposit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * Handles all Telegram Bot API interactions for deposit notifications.
 * Uses HttpURLConnection (JDK 8) — no external HTTP client needed.
 * All API calls are wrapped in try-catch: Telegram failures are non-fatal.
 */
public class DepositTelegramBot {

    private static final Logger logger = Logger.getLogger("api");
    private static final String API_BASE = "https://api.telegram.org/bot";

    private final String botToken;
    private final String chatId;

    public DepositTelegramBot() {
        this.botToken = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
        this.chatId = System.getenv("TELEGRAM_DEPOSIT_CHAT_ID");
    }

    /**
     * @return true if both token and chatId are configured
     */
    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty()
                && chatId != null && !chatId.isEmpty();
    }

    // -----------------------------------------------------------------------
    // Public message methods
    // -----------------------------------------------------------------------

    /**
     * Send a new deposit notification with [Pick] and [Reject] inline buttons.
     *
     * @return Telegram message_id, or -1 on failure
     */
    public long sendDepositNotification(long txId, String txCode, String nickName,
            long amount, String bankName, String bankNumber) {
        return sendDepositNotification(txId, txCode, nickName, amount, bankName, bankNumber, null);
    }

    public long sendDepositNotification(long txId, String txCode, String nickName,
            long amount, String bankName, String bankNumber, String holderName) {

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

        String text = "\ud83d\udcb0 <b>L\u1ec7nh n\u1ea1p m\u1edbi</b> #" + escapeHtml(txCode) + "\n"
                + "\ud83d\udc64 User: <code>" + escapeHtml(nickName) + "</code>\n"
                + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: <b>" + formatAmount(amount) + "</b>\n"
                + "\ud83c\udfe6 Bank: " + escapeHtml(bankName) + "\n"
                + "\ud83d\udcb3 S\u1ed1 TK: <code>" + escapeHtml(bankNumber) + "</code>\n"
                + "\ud83d\udc64 Ch\u1ee7 TK: <b>" + escapeHtml(holderName != null ? holderName : "-") + "</b>\n"
                + "\ud83d\udd50 Time: " + now;

        String keyboard = buildInlineKeyboard(new String[][] {
                { "\ud83d\udd0d Pick", "pick:" + txId },
                { "\u274c Reject", "reject:" + txId }
        });

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\","
                + "\"reply_markup\":" + keyboard
                + "}";

        String response = telegramPost("sendMessage", json);
        return parseMessageId(response);
    }

    /**
     * Edit message after an operator picks the transaction.
     * Shows [Approve] [Reject] [Release] buttons.
     */
    public void editMessagePicked(long messageId, long txId, String txCode,
            String nickName, long amount, String operatorName) {

        String text = "\ud83d\udd12 <b>\u0110ang x\u1eed l\u00fd</b> #" + escapeHtml(txCode) + "\n"
                + "\ud83d\udc64 User: <code>" + escapeHtml(nickName) + "</code> | \ud83d\udcb5 " + formatAmount(amount) + "\n"
                + "\u2699\ufe0f Operator: @" + escapeHtml(operatorName);

        String keyboard = buildInlineKeyboard(new String[][] {
                { "\u2705 Approve", "approve:" + txId },
                { "\u274c Reject", "reject:" + txId },
                { "\ud83d\udd13 Release", "release:" + txId }
        });

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\","
                + "\"reply_markup\":" + keyboard
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message after approval — no more buttons.
     */
    public void editMessageApproved(long messageId, String txCode, long amount,
            String operatorName) {
        editMessageApproved(messageId, txCode, amount, operatorName, null, null, null, null);
    }

    /**
     * Edit message after approval — keeps bank info visible per User request
     * 2026-04-22 so the operator can match the approved txn against the
     * actual bank statement after the fact.
     */
    public void editMessageApproved(long messageId, String txCode, long amount,
            String operatorName, String nickName, String bankName,
            String bankNumber, String holderName) {

        StringBuilder text = new StringBuilder();
        text.append("\u2705 <b>\u0110\u00e3 duy\u1ec7t</b> #").append(escapeHtml(txCode)).append("\n");
        if (nickName != null) text.append("\ud83d\udc64 User: <code>").append(escapeHtml(nickName)).append("</code>\n");
        text.append("\ud83d\udcb5 ").append(formatAmount(amount)).append(" \u2192 c\u1ed9ng balance\n");
        if (bankName != null)   text.append("\ud83c\udfe6 Bank: ").append(escapeHtml(bankName)).append("\n");
        if (bankNumber != null) text.append("\ud83d\udcb3 S\u1ed1 TK: <code>").append(escapeHtml(bankNumber)).append("</code>\n");
        if (holderName != null) text.append("\ud83d\udc64 Ch\u1ee7 TK: <b>").append(escapeHtml(holderName)).append("</b>\n");
        text.append("\u2714\ufe0f Approved by @").append(escapeHtml(operatorName));

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text.toString()) + ","
                + "\"parse_mode\":\"HTML\""
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message after rejection — no more buttons.
     */
    public void editMessageRejected(long messageId, String txCode, long amount,
            String operatorName, String reason) {

        String text = "\u274c <b>T\u1eeb ch\u1ed1i</b> #" + escapeHtml(txCode) + "\n"
                + "\ud83d\udcb5 " + formatAmount(amount) + "\n"
                + "\u274c Rejected by @" + escapeHtml(operatorName) + "\n"
                + "\ud83d\udcdd L\u00fd do: " + escapeHtml(reason);

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\""
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message after expiry — show [⚡ Force Approve] button.
     */
    public void editMessageExpired(long messageId, long txId, String txCode) {

        String text = "\u23f0 <b>H\u1ebft h\u1ea1n</b> #" + escapeHtml(txCode) + "\n"
                + "Qu\u00e1 30 ph\u00fat kh\u00f4ng x\u1eed l\u00fd";

        String keyboard = buildInlineKeyboard(new String[][] {
                { "\u26a1 Force Approve", "force_approve:" + txId }
        });

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\","
                + "\"reply_markup\":" + keyboard
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message after expiry — no buttons (used when operator clicks Close).
     */
    public void editMessageExpiredNoButtons(long messageId, String txCode) {

        String text = "\u23f0 <b>H\u1ebft h\u1ea1n</b> #" + escapeHtml(txCode) + "\n"
                + "Qu\u00e1 30 ph\u00fat kh\u00f4ng x\u1eed l\u00fd";

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\""
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message back to initial state with [Pick] [Reject] after release.
     */
    public void editMessageReleased(long messageId, long txId, String txCode,
            String nickName, long amount, String bankName, String bankNumber) {

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

        String text = "\ud83d\udcb0 <b>L\u1ec7nh n\u1ea1p m\u1edbi</b> #" + escapeHtml(txCode) + "\n"
                + "\ud83d\udc64 User: <code>" + escapeHtml(nickName) + "</code>\n"
                + "\ud83d\udcb5 S\u1ed1 ti\u1ec1n: <b>" + formatAmount(amount) + "</b>\n"
                + "\ud83c\udfe6 Bank: " + escapeHtml(bankName) + " / <code>" + escapeHtml(bankNumber) + "</code>\n"
                + "\ud83d\udd50 Time: " + now + "\n"
                + "\ud83d\udd04 <i>(Released \u2014 available again)</i>";

        String keyboard = buildInlineKeyboard(new String[][] {
                { "\ud83d\udd0d Pick", "pick:" + txId },
                { "\u274c Reject", "reject:" + txId }
        });

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\","
                + "\"reply_markup\":" + keyboard
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message to show Force Approve confirmation with [⚡ Confirm] [❌ Close] buttons.
     */
    public void editMessageForceApproveConfirm(long messageId, long txId, String txCode,
            String nickName, long amount) {

        String text = "\u26a0\ufe0f <b>X\u00e1c nh\u1eadn Force Approve</b> #" + escapeHtml(txCode) + "\n"
                + "\ud83d\udc64 User: <code>" + escapeHtml(nickName) + "</code>\n"
                + "\ud83d\udcb5 " + formatAmount(amount) + "\n\n"
                + "\u26a1 B\u1ea1n c\u00f3 ch\u1eafc mu\u1ed1n force approve giao d\u1ecbch n\u00e0y?";

        String keyboard = buildInlineKeyboard(new String[][] {
                { "\u26a1 Confirm", "force_approve_confirm:" + txId },
                { "\u274c Close", "force_approve_close:" + txId }
        });

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text) + ","
                + "\"parse_mode\":\"HTML\","
                + "\"reply_markup\":" + keyboard
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Edit message to show force approved final state — no buttons.
     */
    public void editMessageForceApproved(long messageId, String txCode, long amount,
            String operatorName) {
        editMessageForceApproved(messageId, txCode, amount, operatorName, null, null, null, null);
    }

    public void editMessageForceApproved(long messageId, String txCode, long amount,
            String operatorName, String nickName, String bankName,
            String bankNumber, String holderName) {

        StringBuilder text = new StringBuilder();
        text.append("\u26a1 <b>Force Approved</b> #").append(escapeHtml(txCode)).append("\n");
        if (nickName != null) text.append("\ud83d\udc64 User: <code>").append(escapeHtml(nickName)).append("</code>\n");
        text.append("\ud83d\udcb5 ").append(formatAmount(amount)).append(" \u2192 c\u1ed9ng balance\n");
        if (bankName != null)   text.append("\ud83c\udfe6 Bank: ").append(escapeHtml(bankName)).append("\n");
        if (bankNumber != null) text.append("\ud83d\udcb3 S\u1ed1 TK: <code>").append(escapeHtml(bankNumber)).append("</code>\n");
        if (holderName != null) text.append("\ud83d\udc64 Ch\u1ee7 TK: <b>").append(escapeHtml(holderName)).append("</b>\n");
        text.append("\u26a1 Force approved by @").append(escapeHtml(operatorName));

        String json = "{"
                + "\"chat_id\":" + jsonStr(chatId) + ","
                + "\"message_id\":" + messageId + ","
                + "\"text\":" + jsonStr(text.toString()) + ","
                + "\"parse_mode\":\"HTML\""
                + "}";

        telegramPost("editMessageText", json);
    }

    /**
     * Respond to a callback query. Must be called within 10 seconds.
     */
    public void answerCallback(String callbackQueryId, String text) {
        String json = "{"
                + "\"callback_query_id\":" + jsonStr(callbackQueryId) + ","
                + "\"text\":" + jsonStr(text)
                + "}";
        telegramPost("answerCallbackQuery", json);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * POST JSON to Telegram Bot API. Returns response body or null on error.
     * Never throws — Telegram failures are non-fatal.
     */
    private String telegramPost(String method, String jsonBody) {
        if (!isConfigured()) {
            logger.warn("DepositTelegramBot: bot token or chat ID not configured, skipping " + method);
            return null;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_BASE + botToken + "/" + method);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            byte[] payload = jsonBody.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(payload);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String responseBody = sb.toString();

            if (code < 200 || code >= 300) {
                logger.error("Telegram API " + method + " returned HTTP " + code + ": " + responseBody);
                return null;
            }

            logger.debug("Telegram API " + method + " OK: " + responseBody);
            return responseBody;
        } catch (Exception e) {
            logger.error("Telegram API " + method + " error: " + e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            // Rate limit: 50ms between consecutive Telegram API calls
            try { Thread.sleep(50); } catch (InterruptedException ignored) { }
        }
    }

    /**
     * Build an inline keyboard JSON. Each row has one button.
     * buttons[i] = { displayText, callbackData }
     */
    private String buildInlineKeyboard(String[][] buttons) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"inline_keyboard\":[");
        // Build as a single row with all buttons
        sb.append("[");
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"text\":").append(jsonStr(buttons[i][0]))
              .append(",\"callback_data\":").append(jsonStr(buttons[i][1]))
              .append("}");
        }
        sb.append("]");
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Parse message_id from Telegram sendMessage response JSON.
     * Minimal JSON parsing — no external library.
     */
    private long parseMessageId(String response) {
        if (response == null) return -1;
        try {
            // Look for "message_id": followed by a number
            String key = "\"message_id\":";
            int idx = response.indexOf(key);
            if (idx < 0) return -1;
            int start = idx + key.length();
            // skip whitespace
            while (start < response.length() && response.charAt(start) == ' ') start++;
            int end = start;
            while (end < response.length() && Character.isDigit(response.charAt(end))) end++;
            if (end == start) return -1;
            return Long.parseLong(response.substring(start, end));
        } catch (Exception e) {
            logger.error("Failed to parse message_id from Telegram response", e);
            return -1;
        }
    }

    /**
     * Format amount with comma separator and Won symbol.
     */
    String formatAmount(long amount) {
        return String.format("\u20a9%,d", amount);
    }

    /**
     * Escape HTML special characters for Telegram HTML parse mode.
     */
    static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Wrap a string as a JSON string value (with escaping).
     */
    private static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public String getChatId() {
        return chatId;
    }
}
