package com.vinplay.dal.rebate;

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
 * Sends Telegram notifications for rebate events.
 * Config: TELEGRAM_REBATE_BOT_TOKEN, TELEGRAM_REBATE_CHAT_ID env vars.
 * Falls back to TELEGRAM_DEPOSIT_BOT_TOKEN / TELEGRAM_DEPOSIT_CHAT_ID if rebate-specific not set.
 */
public class TelegramRebateNotifier {

    private static final Logger logger = Logger.getLogger("dal");
    private static TelegramRebateNotifier instance;

    private final String botToken;
    private final String chatId;

    private TelegramRebateNotifier() {
        String token = System.getenv("TELEGRAM_REBATE_BOT_TOKEN");
        String chat = System.getenv("TELEGRAM_REBATE_CHAT_ID");
        // Fallback to deposit bot token
        if (token == null || token.isEmpty()) token = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
        if (chat == null || chat.isEmpty()) chat = System.getenv("TELEGRAM_DEPOSIT_CHAT_ID");
        this.botToken = token;
        this.chatId = chat;
    }

    public static synchronized TelegramRebateNotifier getInstance() {
        if (instance == null) instance = new TelegramRebateNotifier();
        return instance;
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty() && chatId != null && !chatId.isEmpty();
    }

    private String fmt(long amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount);
    }

    /**
     * Notify admin about new rebate calculation results.
     */
    public void notifyRebateCalculated(int agentCount, long totalRebate, String periodType,
                                       String periodStart, String periodEnd) {
        if (!isConfigured()) return;
        try {
            String text = "\uD83D\uDD14 HO\u00C0N C\u01AF\u1EE2C M\u1EDAI\n"
                    + "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n"
                    + "\uD83D\uDCC5 K\u1EF3: " + periodType + "\n"
                    + "\uD83D\uDCC6 T\u1EEB: " + periodStart + "\n"
                    + "\uD83D\uDCC6 \u0110\u1EBFn: " + periodEnd + "\n"
                    + "\uD83D\uDC65 S\u1ED1 \u0111\u1EA1i l\u00FD: " + agentCount + "\n"
                    + "\uD83D\uDCB0 T\u1ED5ng rebate: " + fmt(totalRebate) + " VND\n"
                    + "\u23F3 Tr\u1EA1ng th\u00E1i: PENDING (ch\u1EDD admin duy\u1EC7t)";
            sendMessage(text);
        } catch (Exception e) {
            logger.warn("TelegramRebateNotifier.notifyRebateCalculated failed", e);
        }
    }

    /**
     * Notify about a payout completion.
     */
    public void notifyPayout(String agentNickname, long amount, String triggeredBy) {
        if (!isConfigured()) return;
        try {
            String text = "\u2705 PAYOUT HO\u00C0N C\u01AF\u1EE2C\n"
                    + "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n"
                    + "\uD83D\uDC64 \u0110\u1EA1i l\u00FD: " + agentNickname + "\n"
                    + "\uD83D\uDCB0 S\u1ED1 ti\u1EC1n: " + fmt(amount) + " VND\n"
                    + "\uD83D\uDC68\u200D\uD83D\uDCBB Admin: " + triggeredBy;
            sendMessage(text);
        } catch (Exception e) {
            logger.warn("TelegramRebateNotifier.notifyPayout failed", e);
        }
    }

    /**
     * Notify about batch payout completion.
     */
    public void notifyBatchPayout(int count, long totalAmount, String triggeredBy) {
        if (!isConfigured()) return;
        try {
            String text = "\u26A1 BATCH PAYOUT HO\u00C0N C\u01AF\u1EE2C\n"
                    + "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n"
                    + "\uD83D\uDC65 S\u1ED1 \u0111\u1EA1i l\u00FD: " + count + "\n"
                    + "\uD83D\uDCB0 T\u1ED5ng payout: " + fmt(totalAmount) + " VND\n"
                    + "\uD83D\uDC68\u200D\uD83D\uDCBB Admin: " + triggeredBy;
            sendMessage(text);
        } catch (Exception e) {
            logger.warn("TelegramRebateNotifier.notifyBatchPayout failed", e);
        }
    }

    private void sendMessage(String text) throws Exception {
        String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        String body = "chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                + "&text=" + URLEncoder.encode(text, "UTF-8");

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        int httpCode = conn.getResponseCode();
        if (httpCode != 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            logger.warn("TelegramRebateNotifier HTTP " + httpCode + ": " + sb);
        }
        conn.disconnect();
    }
}
