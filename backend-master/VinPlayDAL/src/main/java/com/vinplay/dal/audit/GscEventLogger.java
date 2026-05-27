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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SUN-1121 (CQRS / GSC audit Phase 1) — durable inbound event log.
 *
 * <p>Captures every GSC seamless-wallet API call into
 * {@code vinplay.gsc_event_log} BEFORE any wallet/commission/Mongo
 * logic runs, then UPDATE the same row when processing completes. Lets
 * ops answer in one query when a bet-history complaint arrives:
 * "did GSC send us this event or not?" — and either investigate our
 * processing bug or take the evidence to GSC support.
 *
 * <p><b>Defensive contract</b>: every public method here is wrapped in
 * a catch-all so a logger failure can NEVER break the underlying GSC
 * API path. A logger exception is reduced to a WARN log and a -1
 * return value. Finance correctness lives in the wallet code path; the
 * audit trail is best-effort durable but never the gate.
 *
 * <p><b>Idempotency</b>: the {@code gsc_wager_code} column carries a
 * UNIQUE constraint. If GSC retries a withdraw because our response
 * was lost, the second {@link #tryLogRequest} call detects the
 * duplicate and returns the existing row's id so the caller knows to
 * replay rather than re-process.
 *
 * <p><b>Toggle</b>: env {@code GSC_EVENT_LOG_ENABLED=false} disables
 * all writes (returns -1). Default is enabled.
 */
public final class GscEventLogger {

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
        SENSITIVE_HEADERS = Collections.unmodifiableSet(s);
    }

    private GscEventLogger() {}

    public static boolean isEnabled() {
        String v = System.getenv("GSC_EVENT_LOG_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    /**
     * Insert a row capturing the inbound GSC request. Returns the new
     * row id, or the id of an existing row if {@code gsc_wager_code}
     * collides (GSC retry), or -1 if logging is disabled / failed.
     *
     * <p>Extracts denormalized fields from {@code rawBody} via JSON
     * parsing — does NOT depend on the WithdrawRequest class so this
     * helper can be called by other GSC endpoints (Deposit, Cancel)
     * without binding to one request shape.
     */
    public static long tryLogRequest(String endpoint, String rawBody, HttpServletRequest httpReq) {
        if (!isEnabled()) return -1L;
        if (rawBody == null) return -1L;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            // Extract identifiers — best-effort, never throws.
            ExtractedFields ext = extractFromBody(rawBody);

            // Headers (sanitized).
            String headersJson = sanitizeHeaders(httpReq);

            conn = ConnectionPool.getInstance().getConnection(DB_POOL);
            String sql =
                "INSERT INTO vinplay.gsc_event_log " +
                "  (gsc_endpoint, raw_payload, request_headers, " +
                "   gsc_wager_code, gsc_round_id, member_account, product_code, " +
                "   game_code, bet_amount, valid_bet_amount, prize_amount, " +
                "   currency, transaction_count, processing_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'RECEIVED')";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int idx = 1;
            ps.setString(idx++, endpoint);
            ps.setString(idx++, rawBody);
            if (headersJson == null) ps.setNull(idx++, java.sql.Types.LONGVARCHAR); else ps.setString(idx++, headersJson);
            setNullableString(ps, idx++, ext.gscWagerCode);
            setNullableString(ps, idx++, ext.gscRoundId);
            setNullableString(ps, idx++, ext.memberAccount);
            if (ext.productCode == null) ps.setNull(idx++, java.sql.Types.INTEGER); else ps.setInt(idx++, ext.productCode);
            setNullableString(ps, idx++, ext.gameCode);
            if (ext.betAmount == null) ps.setNull(idx++, java.sql.Types.DECIMAL); else ps.setBigDecimal(idx++, ext.betAmount);
            if (ext.validBetAmount == null) ps.setNull(idx++, java.sql.Types.DECIMAL); else ps.setBigDecimal(idx++, ext.validBetAmount);
            if (ext.prizeAmount == null) ps.setNull(idx++, java.sql.Types.DECIMAL); else ps.setBigDecimal(idx++, ext.prizeAmount);
            setNullableString(ps, idx++, ext.currency);
            if (ext.transactionCount == null) ps.setNull(idx++, java.sql.Types.INTEGER); else ps.setInt(idx++, ext.transactionCount);

            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getLong(1);
            return -1L;
        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
            // GSC retried with the same (wager_code, endpoint) pair. Look
            // up the existing row id so the caller knows we already have
            // audit for this event and can replay the cached response.
            // The composite uniqueness lets us coexist withdraw +
            // deposit + cancel rows for the same wager_code — only a
            // genuine retry of the same endpoint dedups.
            try { return findIdByWagerCodeAndEndpoint(extractFromBody(rawBody).gscWagerCode, endpoint); }
            catch (Throwable t) { return -1L; }
        } catch (Throwable t) {
            logger.warn("GscEventLogger.tryLogRequest failed (non-fatal): " + t.getMessage());
            return -1L;
        } finally {
            close(keys); close(ps); close(conn);
        }
    }

    /**
     * Update the row inserted by {@link #tryLogRequest} with the
     * response we returned to GSC. Status is one of COMPLETED / FAILED.
     * Never throws.
     */
    public static void tryLogResponse(long eventLogId, String status, String responsePayload, String lastError) {
        if (eventLogId <= 0L || !isEnabled()) return;
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(DB_POOL);
            String sql =
                "UPDATE vinplay.gsc_event_log " +
                "   SET processing_status = ?, " +
                "       processed_at = CURRENT_TIMESTAMP(3), " +
                "       response_payload = ?, " +
                "       response_at = CURRENT_TIMESTAMP(3), " +
                "       last_error = ? " +
                " WHERE id = ?";
            ps = conn.prepareStatement(sql);
            int i = 1;
            ps.setString(i++, status);
            setNullableString(ps, i++, responsePayload);
            setNullableString(ps, i++, lastError);
            ps.setLong(i, eventLogId);
            ps.executeUpdate();
        } catch (Throwable t) {
            logger.warn("GscEventLogger.tryLogResponse failed (non-fatal): " + t.getMessage());
        } finally {
            close(ps); close(conn);
        }
    }

    /**
     * SUN-1212 — used by {@link com.vinplay.dal.service.GscWagerReconciler} to
     * check whether a deposit/settle row already exists for this wager,
     * regardless of whether it ever advanced past {@code RECEIVED}.
     *
     * <p>Rationale: today's audit (2026-05-02 KST) found a wager whose
     * {@code DepositProcess} handler crashed AFTER the {@code tryLogRequest}
     * insert but BEFORE the {@code tryLogResponse} update — leaving a stuck
     * {@code processing_status='RECEIVED'} row. The reconciler then scanned
     * the still-unsettled Mongo bet and posted a second wallet credit.
     *
     * <p>For this single case the stuck deposit had not applied its wallet
     * credit (the handler crashed before that step), so the math worked out.
     * But if the crash had landed AFTER the wallet write and BEFORE the
     * response update, the same code path would have double-credited the
     * player. The reconciler must therefore treat ANY existing deposit row
     * as a "do not pay" signal, defer to a separate force-complete recovery
     * job, and merely flip the Mongo {@code settled} flag so the wager
     * leaves the stuck-bucket queue.
     *
     * <p>This helper is a defensive read — never throws. Returns {@code false}
     * on logging-disabled, missing wager code, or any DB error (fail-open
     * to "no row" so a transient DB hiccup doesn't permanently block the
     * reconcile path; the caller's Mongo grace window already gave us
     * cushion against racing the legitimate deposit).
     *
     * @param wagerCode GSC wager code (Mongo {@code log_gsc_bets.wager_code})
     * @return {@code true} iff at least one row exists in
     *         {@code vinplay.gsc_event_log} with
     *         {@code gsc_endpoint='deposit'} and
     *         {@code gsc_wager_code = wagerCode}, regardless of
     *         {@code processing_status}.
     */
    public static boolean hasDepositRowForWager(String wagerCode) {
        return findDepositRowForWager(wagerCode) != null;
    }

    /**
     * Lightweight description of an existing {@code gsc_event_log} deposit row
     * for a wager, used by the reconciler's WARN log. Returns {@code null} if
     * no such row exists (or on any DB error — fail-open per
     * {@link #hasDepositRowForWager}).
     */
    public static final class DepositRowInfo {
        public final long id;
        public final String processingStatus;
        public final java.sql.Timestamp receivedAt;
        public DepositRowInfo(long id, String processingStatus, java.sql.Timestamp receivedAt) {
            this.id = id;
            this.processingStatus = processingStatus;
            this.receivedAt = receivedAt;
        }
        @Override public String toString() {
            return "id=" + id + " status=" + processingStatus + " received_at=" + receivedAt;
        }
    }

    /**
     * Companion to {@link #hasDepositRowForWager} that returns the row's
     * status + receive time when it exists. Only the most recent row is
     * returned (multiple rows are technically possible if the unique
     * constraint is ever relaxed, but the schema today guarantees at most
     * one per (endpoint, wager_code) pair).
     */
    public static DepositRowInfo findDepositRowForWager(String wagerCode) {
        if (wagerCode == null || wagerCode.isEmpty()) return null;
        if (!isEnabled()) return null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(DB_POOL);
            ps = conn.prepareStatement(
                "SELECT id, processing_status, received_at FROM vinplay.gsc_event_log "
                + "WHERE gsc_endpoint = 'deposit' AND gsc_wager_code = ? "
                + "ORDER BY id DESC LIMIT 1");
            ps.setString(1, wagerCode);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new DepositRowInfo(rs.getLong(1), rs.getString(2), rs.getTimestamp(3));
            }
            return null;
        } catch (Throwable t) {
            logger.warn("GscEventLogger.findDepositRowForWager failed wager=" + wagerCode
                    + " (treating as no row) err=" + t.getMessage());
            return null;
        } finally {
            close(rs); close(ps); close(conn);
        }
    }

    private static long findIdByWagerCodeAndEndpoint(String wagerCode, String endpoint) {
        if (wagerCode == null || endpoint == null) return -1L;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(DB_POOL);
            ps = conn.prepareStatement(
                "SELECT id FROM vinplay.gsc_event_log "
                + "WHERE gsc_wager_code = ? AND gsc_endpoint = ? LIMIT 1");
            ps.setString(1, wagerCode);
            ps.setString(2, endpoint);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
            return -1L;
        } catch (Throwable t) {
            return -1L;
        } finally {
            close(rs); close(ps); close(conn);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Body extraction (no exception ever escapes — every parse
    //  failure leaves the field NULL and we still capture raw_payload).
    // ──────────────────────────────────────────────────────────────────

    private static final class ExtractedFields {
        String gscWagerCode, gscRoundId, memberAccount, gameCode, currency;
        Integer productCode, transactionCount;
        java.math.BigDecimal betAmount, validBetAmount, prizeAmount;
    }

    private static ExtractedFields extractFromBody(String rawBody) {
        ExtractedFields f = new ExtractedFields();
        try {
            JSONObject root = new JSONObject(rawBody);
            f.memberAccount = root.optString("member_account", null);
            String pc = root.optString("product_code", null);
            if (pc != null) {
                try { f.productCode = Integer.valueOf(pc.trim()); } catch (NumberFormatException ignored) {}
            }
            f.currency = root.optString("currency", null);

            // Resolve transactions list — top-level or first batch_request
            JSONArray txs = root.optJSONArray("transactions");
            if (txs == null || txs.length() == 0) {
                JSONArray batches = root.optJSONArray("batch_requests");
                if (batches != null && batches.length() > 0) {
                    JSONObject b0 = batches.optJSONObject(0);
                    if (b0 != null) {
                        if (f.memberAccount == null) f.memberAccount = b0.optString("member_account", null);
                        if (f.productCode == null) {
                            String bpc = b0.optString("product_code", null);
                            if (bpc != null) {
                                try { f.productCode = Integer.valueOf(bpc.trim()); } catch (NumberFormatException ignored) {}
                            }
                        }
                        txs = b0.optJSONArray("transactions");
                    }
                }
            }

            if (txs != null && txs.length() > 0) {
                f.transactionCount = txs.length();
                JSONObject t0 = txs.optJSONObject(0);
                if (t0 != null) {
                    f.gscWagerCode = t0.optString("wager_code", null);
                    if (f.gscWagerCode != null && f.gscWagerCode.isEmpty()) f.gscWagerCode = null;
                    f.gscRoundId = t0.optString("round_id", null);
                    if (f.gscRoundId != null && f.gscRoundId.isEmpty()) f.gscRoundId = null;
                    f.gameCode = t0.optString("game_code", null);
                    if (f.gameCode != null && f.gameCode.isEmpty()) f.gameCode = null;
                    f.betAmount       = optDecimal(t0, "amount");
                    f.validBetAmount  = optDecimal(t0, "valid_bet_amount");
                    f.prizeAmount     = optDecimal(t0, "prize_amount");
                }
            }
        } catch (Throwable ignored) {
            // Malformed JSON — raw_payload still captured, denormalized
            // fields stay NULL. Nothing here is allowed to throw.
        }
        return f;
    }

    private static java.math.BigDecimal optDecimal(JSONObject o, String key) {
        try {
            if (!o.has(key) || o.isNull(key)) return null;
            Object v = o.get(key);
            if (v instanceof Number) return new java.math.BigDecimal(v.toString());
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.isEmpty()) return null;
                return new java.math.BigDecimal(s);
            }
        } catch (Throwable ignored) {}
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

    private static void close(AutoCloseable c) { if (c != null) try { c.close(); } catch (Exception ignored) {} }
}
