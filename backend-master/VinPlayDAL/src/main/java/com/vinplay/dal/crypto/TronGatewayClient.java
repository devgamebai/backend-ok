package com.vinplay.dal.crypto;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * HTTP client for the TRON USDT Payment Gateway (.NET service).
 * All crypto operations (deposits, withdrawals, settings) go through this client.
 *
 * Config via env vars:
 *   TRON_GATEWAY_URL     - default: http://tron-gateway-api:8080
 *   TRON_GATEWAY_API_KEY - required, x-api-key header
 */
public class TronGatewayClient {

    private static final Logger logger = Logger.getLogger("api");
    private static TronGatewayClient instance;

    private final String baseUrl;
    private final String apiKey;
    private static final int CONNECT_TIMEOUT = 10000; // 10s
    private static final int READ_TIMEOUT = 30000;    // 30s

    private TronGatewayClient() {
        String url = System.getenv("TRON_GATEWAY_URL");
        this.baseUrl = (url != null && !url.isEmpty()) ? url : "http://tron-gateway-api:8080";
        this.apiKey = System.getenv("TRON_GATEWAY_API_KEY");

        if (this.apiKey == null || this.apiKey.isEmpty()) {
            logger.warn("TronGatewayClient: TRON_GATEWAY_API_KEY not set — crypto features will fail");
        }
        logger.info("TronGatewayClient initialized: baseUrl=" + this.baseUrl);
    }

    public static synchronized TronGatewayClient getInstance() {
        if (instance == null) {
            instance = new TronGatewayClient();
        }
        return instance;
    }

    // ========== Address APIs ==========

    /** Generate or get deposit address for a user */
    public JSONObject getDepositAddress(long userId) {
        return doGetWithUserId("/api/addresses", String.valueOf(userId));
    }

    /** Get address info by TRON address */
    public JSONObject getAddressInfo(String address) {
        try {
            return doGet("/api/addresses/info?address=" + URLEncoder.encode(address, "UTF-8"));
        } catch (Exception e) {
            return errorResponse("ENCODE_ERROR", e.getMessage());
        }
    }

    // ========== Wallet APIs ==========

    /** Get hot wallet balance info */
    public JSONObject getWalletInfo() {
        return doGet("/api/wallets/info");
    }

    /**
     * SUN-1171 follow-up: hot wallet on-chain USDT balance, used as a
     * pre-check before approving a withdrawal in
     * {@code CryptoWithdrawalApprovalService.approve}. Returns
     * {@code data.address}, {@code data.balance}, {@code data.symbol}
     * — a missing / 0 balance means the gateway has no hot wallet
     * configured (treat as "unknown — let the approve proceed and
     * surface the gateway's own error if it fails").
     */
    public JSONObject getHotWalletInfo() {
        return doGet("/api/wallets/hot/info");
    }

    /** Get transaction history (deposits + withdrawals) */
    public JSONObject getWalletHistories(int page, int limit) {
        return doGet("/api/wallets/histories?page=" + page + "&limit=" + limit);
    }

    /** Submit a withdrawal request. Amount is BigDecimal to preserve USDT
     *  precision (decimal(20,6) on the source row); the gateway expects an
     *  exact decimal in the request body, never a float. */
    public JSONObject createWithdrawal(String toAddress, BigDecimal amountUsdt, long userId, String memo) {
        // Get user's deposit address to use as fromAddress (required by gateway)
        JSONObject addrResp = getDepositAddress(userId);
        String fromAddress = "";
        if (addrResp.optBoolean("isSuccess", false)) {
            JSONObject addrData = addrResp.optJSONObject("data");
            if (addrData != null) {
                fromAddress = addrData.optString("address", "");
            }
        }

        JSONObject body = new JSONObject();
        body.put("fromAddress", fromAddress);
        body.put("toAddress", toAddress);
        // org.json serializes BigDecimal as a JSON number with full precision.
        body.put("amount", amountUsdt);
        body.put("memo", memo != null ? memo : "");
        return doPostWithUserId("/api/wallets/withdraw", body, String.valueOf(userId));
    }

    /** Admin approve a pending withdrawal */
    public JSONObject approveWithdrawal(String transactionId) {
        JSONObject body = new JSONObject();
        body.put("txId", transactionId);
        return doPost("/api/wallets/withdraw/approve", body);
    }

    /** Admin reject a pending withdrawal */
    public JSONObject rejectWithdrawal(String transactionId, String reason) {
        JSONObject body = new JSONObject();
        body.put("txId", transactionId);
        body.put("reason", reason != null ? reason : "");
        return doPost("/api/wallets/withdraw/reject", body);
    }

    /** Cancel a pending withdrawal */
    public JSONObject cancelWithdrawal(String transactionId) {
        JSONObject body = new JSONObject();
        body.put("transactionId", transactionId);
        return doPost("/api/wallets/withdraw/cancel", body);
    }

    /** Sweep to cold wallet */
    public JSONObject sweepToColdWallet() {
        return doGet("/api/wallets/withdraw/cold-wallet");
    }

    // ========== Settings APIs ==========

    /** Get USDT contract settings (symbol, decimals, min/max, fees) */
    public JSONObject getContractSettings() {
        return doGet("/api/settings/contract");
    }

    /** Update system settings */
    public JSONObject updateSystemSettings(JSONObject settings) {
        return doPost("/api/settings/system", settings);
    }

    /** Update provider settings */
    public JSONObject updateProviderSettings(JSONObject settings) {
        return doPost("/api/settings/provider", settings);
    }

    /** Update contract settings */
    public JSONObject updateContractSettings(JSONObject settings) {
        return doPost("/api/settings/contract", settings);
    }

    // ========== Internal HTTP Methods ==========

    private JSONObject doGetWithUserId(String path, String userId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("x-user-id", userId);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            return readResponse(conn);
        } catch (Exception e) {
            logger.error("TronGatewayClient GET " + path + " (userId=" + userId + ") failed", e);
            return errorResponse("GATEWAY_ERROR", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private JSONObject doGet(String path) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            return readResponse(conn);
        } catch (Exception e) {
            logger.error("TronGatewayClient GET " + path + " failed", e);
            return errorResponse("GATEWAY_ERROR", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private JSONObject doPostWithUserId(String path, JSONObject body, String userId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("x-user-id", userId);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);

            String jsonStr = body.toString();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonStr.getBytes("UTF-8"));
                os.flush();
            }

            return readResponse(conn);
        } catch (Exception e) {
            logger.error("TronGatewayClient POST " + path + " (userId=" + userId + ") failed", e);
            return errorResponse("GATEWAY_ERROR", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private JSONObject doPost(String path, JSONObject body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);

            String jsonStr = body.toString();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonStr.getBytes("UTF-8"));
                os.flush();
            }

            return readResponse(conn);
        } catch (Exception e) {
            logger.error("TronGatewayClient POST " + path + " failed", e);
            return errorResponse("GATEWAY_ERROR", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private JSONObject readResponse(HttpURLConnection conn) throws Exception {
        int httpCode = conn.getResponseCode();
        boolean isError = httpCode >= 400;

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                isError ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        String responseBody = sb.toString();

        if (responseBody.isEmpty()) {
            if (isError) {
                return errorResponse("HTTP_" + httpCode, "Empty response");
            }
            // Empty 200/204 response
            JSONObject ok = new JSONObject();
            ok.put("isSuccess", true);
            return ok;
        }

        JSONObject json = new JSONObject(responseBody);

        // Gateway returns {isSuccess, data, error}. Normalize to our format.
        if (isError && !json.optBoolean("isSuccess", false)) {
            JSONObject err = json.optJSONObject("error");
            String code = err != null ? err.optString("code", "UNKNOWN") : "HTTP_" + httpCode;
            String msg = err != null ? err.optString("message", "") : responseBody;
            logger.warn("TronGatewayClient HTTP " + httpCode + ": " + code + " — " + msg);
        }

        return json;
    }

    private static JSONObject errorResponse(String code, String message) {
        JSONObject resp = new JSONObject();
        resp.put("isSuccess", false);
        JSONObject err = new JSONObject();
        err.put("code", code);
        err.put("message", message != null ? message : "Unknown error");
        resp.put("error", err);
        resp.put("data", JSONObject.NULL);
        return resp;
    }

    /** Check if the gateway client is configured (has API key) */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
