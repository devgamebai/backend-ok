package com.vinplay.dal.audit;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SXB-OBS — durable inbound event log for AWC seamless wallet callbacks.
 * Mirror of {@link GscEventLogger} for the {@code /awc/callback} path
 * (c=3097 → AwcCallbackProcessor).
 *
 * <p>Captures every AWC API call into {@code vinplay.awc_event_log}
 * BEFORE wallet logic runs, then UPDATE the same row when processing
 * completes. Feeds the AWC latency Grafana dashboard with the same
 * per-action / received_at / processed_at shape the GSC dashboard
 * already uses.
 *
 * <p><b>Defensive contract</b>: every public method here is wrapped in
 * a catch-all so a logger failure can NEVER break the AWC API path. A
 * logger exception is reduced to a WARN log and a -1 return value.
 * Finance correctness lives in the wallet code path; the audit trail
 * is best-effort durable but never the gate.
 *
 * <p><b>Idempotency</b>: AWC retries are common after network blips —
 * the {@code (platform_tx_id, awc_action)} composite UNIQUE constraint
 * collapses retries to one row; the second {@link #tryLogRequest} call
 * returns the existing row's id so the caller can replay.
 *
 * <p><b>Toggle</b>: env {@code AWC_EVENT_LOG_ENABLED=false} disables
 * all writes (returns -1). Default is enabled.
 */
public final class AwcEventLogger {

    private static final Logger logger = Logger.getLogger("backend");
    private static final String DB_POOL = "mysqlpoolname";

    /** Headers that may carry secrets — stripped from request_headers before INSERT. */
    private static final Set<String> SENSITIVE_HEADERS;
    static {
        Set<String> s = new HashSet<>();
        s.add("authorization");
        s.add("cookie");
        s.add("set-cookie");
        s.add("x-api-key");
        s.add("x-secret");
        s.add("key");  // AWC callback auth cert lives in body.key — already in raw_payload, no need in headers map
        SENSITIVE_HEADERS = Collections.unmodifiableSet(s);
    }

    private AwcEventLogger() {}

    public static boolean isEnabled() {
        String v = System.getenv("AWC_EVENT_LOG_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    /**
     * Insert a row capturing the inbound AWC request. Returns the new
     * row id, or the id of an existing row if {@code (platform_tx_id,
     * awc_action)} collides (AWC retry), or -1 if logging is
     * disabled / failed.
     */
    public static long tryLogRequest(String action, String rawBody, HttpServletRequest httpReq) {
        if (!isEnabled()) return -1L;
        if (rawBody == null) return -1L;
        // try-with-resources mirrors the 7-DAO leak sweep landed on
        // staging — every JDBC handle closes on every exit path including
        // the early-return generatedKeys branch.
        String sql =
            "INSERT INTO vinplay.awc_event_log " +
            "  (awc_action, raw_payload, request_headers, " +
            "   awc_user_id, member_account, platform, game_code, game_type, " +
            "   round_id, platform_tx_id, bet_amount, win_amount, " +
            "   currency, transaction_count, processing_status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'RECEIVED')";
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ExtractedFields ext = extractFromBody(rawBody, action);
            String headersJson = sanitizeHeaders(httpReq);

            int idx = 1;
            ps.setString(idx++, action);
            ps.setString(idx++, rawBody);
            if (headersJson == null) ps.setNull(idx++, java.sql.Types.LONGVARCHAR); else ps.setString(idx++, headersJson);
            setNullableString(ps, idx++, ext.awcUserId);
            setNullableString(ps, idx++, ext.memberAccount);
            setNullableString(ps, idx++, ext.platform);
            setNullableString(ps, idx++, ext.gameCode);
            setNullableString(ps, idx++, ext.gameType);
            setNullableString(ps, idx++, ext.roundId);
            setNullableString(ps, idx++, ext.platformTxId);
            if (ext.betAmount == null) ps.setNull(idx++, java.sql.Types.DECIMAL); else ps.setBigDecimal(idx++, ext.betAmount);
            if (ext.winAmount == null) ps.setNull(idx++, java.sql.Types.DECIMAL); else ps.setBigDecimal(idx++, ext.winAmount);
            setNullableString(ps, idx++, ext.currency);
            if (ext.transactionCount == null) ps.setNull(idx++, java.sql.Types.INTEGER); else ps.setInt(idx++, ext.transactionCount);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return -1L;
        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
            // AWC retry with same (platform_tx_id, action) — look up the
            // existing row id so the caller can replay.
            try {
                ExtractedFields ext = extractFromBody(rawBody, action);
                return findIdByTxAndAction(ext.platformTxId, action);
            } catch (Throwable t) { return -1L; }
        } catch (Throwable t) {
            logger.warn("AwcEventLogger.tryLogRequest failed (non-fatal): " + t.getMessage());
            return -1L;
        }
    }

    /**
     * Update the row inserted by {@link #tryLogRequest} with the
     * response we returned to AWC. Status is one of COMPLETED / FAILED.
     * Never throws.
     */
    public static void tryLogResponse(long eventLogId, String status, String responsePayload, String lastError) {
        if (eventLogId <= 0L || !isEnabled()) return;
        String sql =
            "UPDATE vinplay.awc_event_log " +
            "   SET processing_status = ?, " +
            "       processed_at = CURRENT_TIMESTAMP(3), " +
            "       response_payload = ?, " +
            "       response_at = CURRENT_TIMESTAMP(3), " +
            "       last_error = ? " +
            " WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, status);
            setNullableString(ps, i++, responsePayload);
            setNullableString(ps, i++, lastError);
            ps.setLong(i, eventLogId);
            ps.executeUpdate();
        } catch (Throwable t) {
            logger.warn("AwcEventLogger.tryLogResponse failed (non-fatal): " + t.getMessage());
        }
    }

    private static long findIdByTxAndAction(String platformTxId, String action) {
        if (platformTxId == null || action == null) return -1L;
        String sql = "SELECT id FROM vinplay.awc_event_log "
                   + "WHERE platform_tx_id = ? AND awc_action = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, platformTxId);
            ps.setString(2, action);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
            return -1L;
        } catch (Throwable t) {
            return -1L;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Body extraction — never throws. Parse failures leave fields NULL
    //  and raw_payload still captures the original bytes.
    // ──────────────────────────────────────────────────────────────────

    private static final class ExtractedFields {
        String awcUserId, memberAccount, platform, gameCode, gameType,
               roundId, platformTxId, currency;
        Integer transactionCount;
        java.math.BigDecimal betAmount, winAmount;
    }

    /**
     * AWC payload shape (per AwcCallbackProcessor.dispatchByAction):
     *   { key: "...", message: { action, userId, txns: [{ platformTxId, betAmount, winAmount, roundId, gameCode, ... }] } }
     * For getBalance the message has top-level userId, no txns. For
     * bet/settle the txns array carries the per-tx values.
     */
    private static ExtractedFields extractFromBody(String rawBody, String fallbackAction) {
        ExtractedFields f = new ExtractedFields();
        try {
            JSONObject root = new JSONObject(rawBody);
            JSONObject msg = root.optJSONObject("message");
            if (msg == null) msg = root; // some callers send flat

            f.awcUserId = msg.optString("userId", null);
            if (f.awcUserId != null && f.awcUserId.isEmpty()) f.awcUserId = null;

            f.platform = firstNonEmpty(msg.optString("platform", null),
                                       msg.optString("platformCode", null));
            f.gameType = msg.optString("gameType", null);
            if (f.gameType != null && f.gameType.isEmpty()) f.gameType = null;
            f.currency = msg.optString("currency", null);
            if (f.currency != null && f.currency.isEmpty()) f.currency = null;

            JSONArray txs = msg.optJSONArray("txns");
            if (txs == null || txs.length() == 0) txs = msg.optJSONArray("transactions");
            if (txs != null && txs.length() > 0) {
                f.transactionCount = txs.length();
                JSONObject t0 = txs.optJSONObject(0);
                if (t0 != null) {
                    if (f.awcUserId == null) {
                        f.awcUserId = t0.optString("userId", null);
                        if (f.awcUserId != null && f.awcUserId.isEmpty()) f.awcUserId = null;
                    }
                    f.platformTxId = firstNonEmpty(t0.optString("platformTxId", null),
                                                   t0.optString("platform_tx_id", null));
                    f.roundId   = firstNonEmpty(t0.optString("roundId", null),
                                                t0.optString("round_id", null));
                    f.gameCode  = firstNonEmpty(t0.optString("gameCode", null),
                                                t0.optString("game_code", null));
                    if (f.platform == null) f.platform = firstNonEmpty(t0.optString("platform", null),
                                                                       t0.optString("platformCode", null));
                    if (f.gameType == null) f.gameType = t0.optString("gameType", null);
                    if (f.currency == null) f.currency = t0.optString("currency", null);
                    f.betAmount = optDecimal(t0, "betAmount", "bet_amount");
                    f.winAmount = optDecimal(t0, "winAmount", "win_amount");
                }
            }

            // Derive member_account from awc_user_id: strip the AWC_PREFIX.
            // Best-effort — null on env-miss is fine.
            if (f.awcUserId != null) {
                String prefix = System.getenv("AWC_PREFIX");
                if (prefix != null && !prefix.isEmpty() && f.awcUserId.startsWith(prefix)) {
                    f.memberAccount = f.awcUserId.substring(prefix.length());
                } else {
                    f.memberAccount = f.awcUserId;
                }
            }
        } catch (Throwable ignored) {
            // Malformed JSON — raw_payload still captured.
        }
        return f;
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.isEmpty()) return v;
        return null;
    }

    private static java.math.BigDecimal optDecimal(JSONObject o, String... keys) {
        for (String key : keys) {
            try {
                if (!o.has(key) || o.isNull(key)) continue;
                Object v = o.get(key);
                if (v instanceof Number) return new java.math.BigDecimal(v.toString());
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (s.isEmpty()) continue;
                    return new java.math.BigDecimal(s);
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String sanitizeHeaders(HttpServletRequest req) {
        if (req == null) return null;
        try {
            JSONObject obj = new JSONObject();
            Enumeration<String> names = req.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = names.nextElement();
                if (name == null) continue;
                if (SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                    obj.put(name, "[REDACTED]");
                    continue;
                }
                List<String> values = new ArrayList<>();
                Enumeration<String> vs = req.getHeaders(name);
                while (vs != null && vs.hasMoreElements()) values.add(vs.nextElement());
                if (values.size() == 1) obj.put(name, values.get(0));
                else obj.put(name, new JSONArray(values));
            }
            return obj.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setNullableString(PreparedStatement ps, int idx, String v) throws java.sql.SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, v);
    }

}
