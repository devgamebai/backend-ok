package com.vinplay.dal.service;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * Thin HTTP wrapper around GSC's wager-detail endpoint
 * ({@code GET /api/operators/wagers/{wager_code}}, spec section 3.3).
 * Used by {@link GscWagerReconciler} to fetch the authoritative settle
 * state for any wager whose deposit/settle event we never received.
 *
 * <p>Reads GSC creds from env vars {@code GSC_OPERATOR_URL},
 * {@code GSC_OPERATOR_CODE}, {@code GSC_SECRET_KEY} — same source the
 * launch / game-list flows use, so credentials stay in one place.
 *
 * <p>Sign formula per the spec:
 * {@code md5(request_time + secret_key + "getwager" + operator_code)}.
 *
 * <p>All methods are best-effort: HTTP/parse errors return {@code null}
 * rather than throwing so callers (the reconciler scheduler) stay
 * lean.
 */
public final class GscWagerClient {

    private static final Logger logger = Logger.getLogger("dal");

    public static final class WagerStatus {
        public final String code;            // wager_code
        public final String memberAccount;
        public final int productCode;
        public final String gameCode;
        public final long betAmount;
        public final long validBetAmount;
        public final long prizeAmount;
        public final String status;          // "SETTLED" / "BET" / "PENDING" / ...
        public final long settledAtMs;       // 0 if not settled

        WagerStatus(String code, String memberAccount, int productCode, String gameCode,
                     long betAmount, long validBetAmount, long prizeAmount,
                     String status, long settledAtMs) {
            this.code = code;
            this.memberAccount = memberAccount;
            this.productCode = productCode;
            this.gameCode = gameCode;
            this.betAmount = betAmount;
            this.validBetAmount = validBetAmount;
            this.prizeAmount = prizeAmount;
            this.status = status;
            this.settledAtMs = settledAtMs;
        }

        public boolean isSettled() { return "SETTLED".equals(status); }
        public boolean playerWon() { return prizeAmount > 0; }
    }

    private GscWagerClient() {}

    /**
     * Fetch the authoritative status of a wager by code. Returns
     * {@code null} on HTTP error, parse error, or GSC error response.
     */
    public static WagerStatus fetch(String wagerCode) {
        if (wagerCode == null || wagerCode.isEmpty()) return null;
        String operatorUrl = System.getenv("GSC_OPERATOR_URL");
        String operatorCode = System.getenv("GSC_OPERATOR_CODE");
        String secretKey = System.getenv("GSC_SECRET_KEY");
        if (operatorUrl == null || operatorCode == null || secretKey == null) {
            logger.warn("GscWagerClient.fetch: GSC_* env vars missing — cannot reconcile");
            return null;
        }
        long requestTime = System.currentTimeMillis() / 1000;
        String sign = md5(requestTime + secretKey + "getwager" + operatorCode);
        String urlStr = operatorUrl + "/api/operators/wagers/" + wagerCode
                + "?operator_code=" + operatorCode
                + "&sign=" + sign
                + "&request_time=" + requestTime;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            int code = conn.getResponseCode();
            StringBuilder body = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                String line; while ((line = br.readLine()) != null) body.append(line);
            }
            JSONObject resp = new JSONObject(body.toString());
            JSONObject w = resp.optJSONObject("wager");
            if (w == null) {
                int gscCode = resp.optInt("code", -1);
                if (gscCode != 0) {
                    logger.info("GscWagerClient.fetch: wager=" + wagerCode
                            + " GSC code=" + gscCode + " msg=" + resp.optString("message", ""));
                }
                return null;
            }
            return new WagerStatus(
                    w.optString("code", wagerCode),
                    w.optString("member_account", ""),
                    w.optInt("product_code", 0),
                    w.optString("game_code", ""),
                    w.optLong("bet_amount", 0L),
                    w.optLong("valid_bet_amount", 0L),
                    w.optLong("prized_amount", 0L),
                    w.optString("status", ""),
                    w.optLong("settled_at", 0L));
        } catch (Exception e) {
            logger.warn("GscWagerClient.fetch threw wager=" + wagerCode + " err=" + e.getMessage());
            return null;
        }
    }

    /**
     * 3.2 Wager List — pull all wagers GSC considers settled in the
     * given time window. Used by the hourly reconciler to cross-check
     * our `log_gsc_bets` against GSC's authoritative ledger.
     *
     * <p>Per spec v2.0.5 the window is capped at **5 minutes** per call
     * and the page size is capped at 5000. The hourly recon walks 90
     * minutes in 5-minute chunks (18 calls/run) — well under any rate
     * limit and cheap enough to run every hour without complaint.
     *
     * <p>Settlement-time semantics: GSC's {@code start}/{@code end}
     * filter on {@code settled_at}, NOT bet placement time. So this
     * method returns wagers settled in the window — exactly what the
     * reconciler needs to verify our SETTLE_UPDATE pipeline kept up.
     *
     * @return list of wagers, empty list on HTTP error / parse error / GSC error.
     */
    public static java.util.List<WagerStatus> listWagers(long startMs, long endMs, int offset, int size) {
        java.util.List<WagerStatus> out = new java.util.ArrayList<>();
        String operatorUrl = System.getenv("GSC_OPERATOR_URL");
        String operatorCode = System.getenv("GSC_OPERATOR_CODE");
        String secretKey = System.getenv("GSC_SECRET_KEY");
        if (operatorUrl == null || operatorCode == null || secretKey == null) {
            logger.warn("GscWagerClient.listWagers: GSC_* env vars missing — cannot reconcile");
            return out;
        }
        long requestTime = System.currentTimeMillis() / 1000;
        String sign = md5(requestTime + secretKey + "getwagers" + operatorCode);
        String urlStr = operatorUrl + "/api/operators/wagers"
                + "?operator_code=" + operatorCode
                + "&sign=" + sign
                + "&request_time=" + requestTime
                + "&start=" + startMs
                + "&end=" + endMs
                + "&offset=" + offset
                + "&size=" + size;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            StringBuilder body = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                String line; while ((line = br.readLine()) != null) body.append(line);
            }
            JSONObject resp = new JSONObject(body.toString());
            org.json.JSONArray wagers = resp.optJSONArray("wagers");
            if (wagers == null) {
                int gscCode = resp.optInt("code", -1);
                if (gscCode != 0) {
                    logger.info("GscWagerClient.listWagers: GSC code=" + gscCode
                            + " msg=" + resp.optString("message", "")
                            + " window=[" + startMs + "," + endMs + "]");
                }
                return out;
            }
            for (int i = 0; i < wagers.length(); i++) {
                JSONObject w = wagers.optJSONObject(i);
                if (w == null) continue;
                out.add(new WagerStatus(
                        w.optString("code", ""),
                        w.optString("member_account", ""),
                        w.optInt("product_code", 0),
                        w.optString("game_code", ""),
                        w.optLong("bet_amount", 0L),
                        w.optLong("valid_bet_amount", 0L),
                        w.optLong("prize_amount", 0L),
                        w.optString("status", ""),
                        w.optLong("settled_at", 0L)));
            }
            return out;
        } catch (Exception e) {
            logger.warn("GscWagerClient.listWagers threw window=[" + startMs + "," + endMs + "] err=" + e.getMessage());
            return out;
        }
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
