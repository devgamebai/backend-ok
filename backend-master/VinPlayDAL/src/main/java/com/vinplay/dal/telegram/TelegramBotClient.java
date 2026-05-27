package com.vinplay.dal.telegram;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Single shared client for the deposit/withdraw operator-chat Telegram bot.
 *
 * <p>Both {@code TelegramDepositNotifier} (bank deposits) and
 * {@code TelegramCryptoWithdrawNotifier} (crypto withdraws) used to keep
 * their own copies of the HTTP plumbing — config read, POST helper,
 * sendMessage/editMessageText calls, money formatter, HTML escape. That
 * was the asymmetry that produced SUN-1199: crypto used Markdown V1 +
 * backticks and broke on underscored nicknames; bank used plain text
 * and didn't. Centralising the wire layer here means adding a feature
 * (button cleanup, parse_mode change, retry-with-backoff) lands in one
 * place and both flows pick it up.
 *
 * <p>Configuration via env vars:
 * <ul>
 *   <li>{@code TELEGRAM_DEPOSIT_BOT_TOKEN} — bot API token</li>
 *   <li>{@code TELEGRAM_DEPOSIT_CHAT_ID}  — group chat where ops act</li>
 * </ul>
 */
public final class TelegramBotClient {

    private static final Logger logger = Logger.getLogger("api");
    private static volatile TelegramBotClient instance;

    private final String botToken;
    private final String chatId;
    private final String apiBase;

    private TelegramBotClient() {
        this.botToken = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
        this.chatId = System.getenv("TELEGRAM_DEPOSIT_CHAT_ID");
        this.apiBase = (botToken != null && !botToken.isEmpty())
                ? "https://api.telegram.org/bot" + botToken + "/"
                : null;
    }

    public static TelegramBotClient getInstance() {
        TelegramBotClient ref = instance;
        if (ref == null) {
            synchronized (TelegramBotClient.class) {
                ref = instance;
                if (ref == null) ref = instance = new TelegramBotClient();
            }
        }
        return ref;
    }

    public boolean isConfigured() {
        return apiBase != null && chatId != null && !chatId.isEmpty();
    }

    public String chatId() { return chatId; }

    // ─────────────────────────────────────────────────────────────────
    //  Send / edit
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a new message. Returns the {@code message_id} for later
     * edits, or -1 on any failure (config missing, HTTP error,
     * unparseable response).
     *
     * @param text      message body — already escaped for parseMode
     * @param keyboard  inline-keyboard JSON, or null for none
     * @param parseMode {@code "HTML"}, {@code "Markdown"}, or null for plain text
     */
    public long sendMessage(String text, String keyboard, String parseMode) {
        if (!isConfigured()) return -1;
        try {
            StringBuilder body = new StringBuilder();
            body.append("chat_id=").append(URLEncoder.encode(chatId, "UTF-8"));
            body.append("&text=").append(URLEncoder.encode(text, "UTF-8"));
            if (parseMode != null && !parseMode.isEmpty()) {
                body.append("&parse_mode=").append(URLEncoder.encode(parseMode, "UTF-8"));
            }
            if (keyboard != null && !keyboard.isEmpty()) {
                body.append("&reply_markup=").append(URLEncoder.encode(keyboard, "UTF-8"));
            }
            String resp = httpPost(apiBase + "sendMessage", body.toString());
            return parseMessageId(resp);
        } catch (Exception e) {
            logger.warn("TelegramBotClient.sendMessage failed: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Edit an existing message.
     *
     * <p><b>Important:</b> Telegram's {@code editMessageText} preserves
     * the prior {@code reply_markup} when the field is omitted. If the
     * caller wants the inline keyboard cleared (terminal state — approve
     * / reject), pass {@code keyboard = ""} (the method substitutes the
     * empty-keyboard JSON). Pass {@code keyboard = null} only when you
     * deliberately want to keep whatever buttons were there.
     */
    public boolean editMessage(long messageId, String text, String keyboard, String parseMode) {
        if (!isConfigured() || messageId <= 0) return false;
        try {
            StringBuilder body = new StringBuilder();
            body.append("chat_id=").append(URLEncoder.encode(chatId, "UTF-8"));
            body.append("&message_id=").append(messageId);
            body.append("&text=").append(URLEncoder.encode(text, "UTF-8"));
            if (parseMode != null && !parseMode.isEmpty()) {
                body.append("&parse_mode=").append(URLEncoder.encode(parseMode, "UTF-8"));
            }
            if (keyboard != null) {
                String kb = keyboard.isEmpty() ? "{\"inline_keyboard\":[]}" : keyboard;
                body.append("&reply_markup=").append(URLEncoder.encode(kb, "UTF-8"));
            }
            String resp = httpPost(apiBase + "editMessageText", body.toString());
            return resp != null;
        } catch (Exception e) {
            logger.warn("TelegramBotClient.editMessage failed for msgId=" + messageId
                    + ": " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Formatting helpers
    // ─────────────────────────────────────────────────────────────────

    /** Format {@code 1234567} as {@code "1,234,567"} (Korean locale). */
    public static String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount);
    }

    /** Escape user-supplied content for Telegram {@code parse_mode=HTML}.
     *  Only {@code <}, {@code >}, {@code &} need handling — underscore /
     *  asterisk / backtick are literal. */
    public static String escHtml(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ─────────────────────────────────────────────────────────────────
    //  Internals
    // ─────────────────────────────────────────────────────────────────

    private static long parseMessageId(String resp) {
        if (resp == null) return -1;
        int idx = resp.indexOf("\"message_id\":");
        if (idx < 0) return -1;
        idx += 13;
        int end = resp.indexOf(",", idx);
        if (end == -1) end = resp.indexOf("}", idx);
        if (end <= idx) return -1;
        try {
            return Long.parseLong(resp.substring(idx, end).trim());
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private String httpPost(String urlStr, String body) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
                os.flush();
            }

            int httpCode = conn.getResponseCode();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String respBody = sb.toString();
                if (httpCode >= 400) {
                    logger.warn("TelegramBotClient HTTP " + httpCode + ": " + respBody);
                    return null;
                }
                return respBody;
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
