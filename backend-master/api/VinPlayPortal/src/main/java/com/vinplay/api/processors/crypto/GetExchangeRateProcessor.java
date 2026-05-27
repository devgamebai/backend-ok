package com.vinplay.api.processors.crypto;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * c=3024 — Get live USDT/KRW exchange rate from Upbit (fallback: Bithumb, CryptoCompare).
 * Called when player opens deposit/withdrawal page.
 */
public class GetExchangeRateProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final int TIMEOUT = 5000;

    // Cache rate for 30 seconds
    private static volatile double cachedRate = 0;
    private static volatile long cachedAt = 0;
    private static volatile String cachedSource = "";
    private static final long CACHE_TTL = 30000; // 30s

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            long now = System.currentTimeMillis();

            // Return cached if fresh
            if (cachedRate > 0 && (now - cachedAt) < CACHE_TTL) {
                JSONObject data = new JSONObject();
                data.put("rate", cachedRate);
                data.put("pair", "USDT/KRW");
                data.put("source", cachedSource);
                data.put("cached", true);
                response.put("success", true);
                response.put("data", data);
                return response.toString();
            }

            // Try Upbit first
            double rate = fetchUpbit();
            String source = "upbit";

            // Fallback: Bithumb
            if (rate <= 0) {
                rate = fetchBithumb();
                source = "bithumb";
            }

            // Fallback: CryptoCompare
            if (rate <= 0) {
                rate = fetchCryptoCompare();
                source = "cryptocompare";
            }

            if (rate > 0) {
                cachedRate = rate;
                cachedAt = now;
                cachedSource = source;

                JSONObject data = new JSONObject();
                data.put("rate", rate);
                data.put("pair", "USDT/KRW");
                data.put("source", source);
                data.put("cached", false);
                response.put("success", true);
                response.put("data", data);
            } else {
                response.put("success", false);
                response.put("errorCode", "5001");
                response.put("message", "Unable to fetch exchange rate");
            }

        } catch (Exception e) {
            logger.error("GetExchangeRateProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5001");
        }
        return response.toString();
    }

    private double fetchUpbit() {
        try {
            String json = httpGet("https://api.upbit.com/v1/ticker?markets=KRW-USDT");
            if (json != null && json.startsWith("[")) {
                JSONArray arr = new JSONArray(json);
                if (arr.length() > 0) {
                    return arr.getJSONObject(0).getDouble("trade_price");
                }
            }
        } catch (Exception e) {
            logger.warn("GetExchangeRateProcessor Upbit failed: " + e.getMessage());
        }
        return 0;
    }

    private double fetchBithumb() {
        try {
            String json = httpGet("https://api.bithumb.com/public/ticker/USDT_KRW");
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                if ("0000".equals(obj.optString("status"))) {
                    return Double.parseDouble(obj.getJSONObject("data").getString("closing_price"));
                }
            }
        } catch (Exception e) {
            logger.warn("GetExchangeRateProcessor Bithumb failed: " + e.getMessage());
        }
        return 0;
    }

    /**
     * SUN-1171: static helper for other processors that need a USDT/KRW rate
     * without going through the request-bound execute() path. Honors the
     * same 30s in-process cache and the same Upbit → Bithumb → CryptoCompare
     * fallback cascade used by the c=3024 endpoint. Returns 0 if every source
     * is unreachable; callers must reject the operation in that case rather
     * than guessing a rate (a wrong KRW credit is worse than a delayed one).
     */
    public static double getCurrentRate() {
        long now = System.currentTimeMillis();
        if (cachedRate > 0 && (now - cachedAt) < CACHE_TTL) {
            return cachedRate;
        }
        GetExchangeRateProcessor instance = new GetExchangeRateProcessor();
        double rate = instance.fetchUpbit();
        String src = "upbit";
        if (rate <= 0) { rate = instance.fetchBithumb(); src = "bithumb"; }
        if (rate <= 0) { rate = instance.fetchCryptoCompare(); src = "cryptocompare"; }
        if (rate > 0) {
            cachedRate = rate;
            cachedAt = now;
            cachedSource = src;
        }
        return rate;
    }

    private double fetchCryptoCompare() {
        try {
            String json = httpGet("https://min-api.cryptocompare.com/data/price?fsym=USDT&tsyms=KRW");
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                if (obj.has("KRW")) {
                    return obj.getDouble("KRW");
                }
            }
        } catch (Exception e) {
            logger.warn("GetExchangeRateProcessor CryptoCompare failed: " + e.getMessage());
        }
        return 0;
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
