package com.vinplay.dal.rtp;

import org.apache.log4j.Logger;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RtpWebhookService {
    private static final Logger logger = Logger.getLogger("api");
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // We mock storing the webhook URL in DB/Hazelcast for simplicity.
    // In real env, it fetches from config.
    private static String currentWebhookUrl = "https://discord.com/api/webhooks/mock";

    public static boolean registerWebhook(String url) {
        currentWebhookUrl = url;
        return true;
    }

    public static void queuePayload(String payload) {
        if (currentWebhookUrl == null || currentWebhookUrl.isEmpty()) return;
        
        executor.submit(() -> {
            try {
                URL url = new URL(currentWebhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);  // Bug fix: set timeout to avoid hanging threads
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                // Bug fix: consume response body to release connection
                try (java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                    if (is != null) while (is.read() != -1) {}
                }
                if (code != 200 && code != 204) {
                    logger.warn("Webhook failed with HTTP " + code);
                }
            } catch (Exception e) {
                logger.error("Failed to send webhook", e);
            }
        });
    }

    public static void testPayload() {
        queuePayload("{\"content\": \"🔔 **TEST ALERT**\\nNgài Admin vừa kích hoạt hệ thống Webhook liên sao mặt đất!\"}");
    }
}
