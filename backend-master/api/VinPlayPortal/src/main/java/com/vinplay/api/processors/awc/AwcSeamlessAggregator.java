package com.vinplay.api.processors.awc;

import com.vinplay.dal.audit.AwcEventLogger;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.dal.service.seamless.SeamlessStatus;
import com.vinplay.dal.service.seamless.SeamlessWalletAggregator;
import com.vinplay.dal.service.seamless.ValidationResult;
import com.vinplay.dal.service.seamless.VerifyResult;
import com.vinplay.vbee.common.config.AwcConfig;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * SUN-1236 Phase 1: AWC seamless-wallet aggregator skeleton.
 *
 * <p>Currently routes ONLY {@code action=getBalance} through the canonical
 * {@link SeamlessWalletAggregator} flow. All other actions throw
 * {@link UnsupportedOperationException} — they should NEVER be invoked
 * because the legacy {@code AwcCallbackProcessor} stays the entry point and
 * only delegates here when the feature flag {@code AWC_USE_AGGREGATOR=1}
 * AND the action is in the migrated set.
 *
 * <p>Default flag: OFF. AWC cert work continues unchanged on legacy.
 *
 * <p>See {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md} for the contract.
 */
public class AwcSeamlessAggregator
        extends SeamlessWalletAggregator<AwcSeamlessAggregator.AwcRequest, String> {

    /**
     * Phase 4: aggregator is the single AWC entry point. Flag retained as a
     * kill-switch — set {@code AWC_USE_AGGREGATOR=0} to short-circuit
     * (returns null from {@link #handleIfEligible}) so an operator can
     * roll back if a regression slips through. Default ON.
     */
    public static boolean isEnabled() {
        String v = System.getenv("AWC_USE_AGGREGATOR");
        if (v == null) return true; // default on
        return !(v.equals("0") || v.equalsIgnoreCase("false"));
    }

    public static boolean isActionMigrated(String action) {
        if (action == null) return false;
        switch (action) {
            // SUN-1236 Phase 2: all AWC actions wired through aggregator via
            // legacy delegation (race-sensitive code stays in
            // AwcCallbackProcessor.dispatchByAction; aggregator owns
            // signature verify + audit hooks). Phase 3 will migrate
            // individual handlers onto base primitives one by one.
            case "getBalance":
            case "bet":
            case "betNSettle":
            case "settle":
            case "cancelBet":
            case "cancelBetNSettle":
            case "refund":
            case "voidBet":
            case "voidSettle":
            case "unvoidBet":
            case "unvoidSettle":
            case "unsettle":
            case "resettle":
            case "freeSpin":
            case "adjustBet":
            case "tip":
            case "cancelTip":
            case "give":
                return true;
            default:
                return false;
        }
    }

    static final class AwcRequest {
        final String rawBody;
        final String key;
        final JSONObject message;
        final String action;

        AwcRequest(String rawBody, String key, JSONObject message) {
            this.rawBody = rawBody;
            this.key = key;
            this.message = message;
            this.action = message.optString("action", "");
        }
    }

    @Override
    protected AwcRequest parseRequest(String body, HttpServletRequest http) throws Exception {
        String key;
        String messageStr = null;
        if (body != null && body.trim().startsWith("{")) {
            JSONObject outer = new JSONObject(body);
            key = outer.optString("key", null);
            Object msg = outer.opt("message");
            if (msg instanceof JSONObject) return new AwcRequest(body, key, (JSONObject) msg);
            if (msg instanceof String) messageStr = (String) msg;
        } else {
            key = http.getParameter("key");
            messageStr = http.getParameter("message");
        }
        if (messageStr == null) throw new IllegalArgumentException("Missing message");
        return new AwcRequest(body, key, new JSONObject(messageStr));
    }

    @Override
    protected VerifyResult verifySignature(AwcRequest req, HttpServletRequest http) {
        if (req.key == null) return VerifyResult.fail("Missing key");
        if (!AwcConfig.verifyCallbackKey(req.key)) return VerifyResult.fail("Invalid key");
        return VerifyResult.ok();
    }

    @Override
    protected long currencyToInternal(double providerAmount, String currency) {
        return (long) providerAmount;
    }

    @Override
    protected double currencyToExternal(long internalBalance, String currency) {
        return internalBalance;
    }

    @Override
    protected String mapActionToSource(String action) {
        if (action == null) return MoneyGateway.SOURCE_AWC_CREDIT;
        switch (action) {
            case "bet":
            case "betNSettle":
            case "tip":
            case "adjustBet":
                return MoneyGateway.SOURCE_AWC_DEBIT;
            default:
                return MoneyGateway.SOURCE_AWC_CREDIT;
        }
    }

    @Override
    protected ValidationResult validateBusinessRules(AwcRequest req) {
        if (req.action == null || req.action.isEmpty()) {
            return ValidationResult.fail("9999", "Missing action");
        }
        if (!isActionMigrated(req.action)) {
            return ValidationResult.fail("9999", "Action not migrated to aggregator: " + req.action);
        }
        return ValidationResult.ok();
    }

    @Override
    protected SeamlessOutcome dispatch(AwcRequest req) {
        // Phase 2: all migrated actions delegate to legacy AwcCallbackProcessor
        // dispatcher. Aggregator owns lifecycle (verify/audit/serialize),
        // legacy code path owns the per-action wallet/residue/audit logic.
        // Phase 3 will progressively rewrite handlers onto base primitives.
        //
        // SUN-1340 follow-up: emit per-action timing buckets so we can read
        // p99 of AwcGetBalance / AwcBet / AwcSettle independently — parity
        // with the GSC handlers (GscBalance / GscWithdraw / GscDeposit) that
        // AggregatorP99Scheduler already alerts on. Base class records ONE
        // bucket per aggregator name covering the entire handle() call;
        // this extra recordHandle isolates the legacy-dispatch slice per
        // action so a slow getBalance does not get drowned by bet volume.
        long t0 = System.nanoTime();
        try {
            String legacyJson = new AwcCallbackProcessor()
                    .dispatchByAction(req.action, req.message, req.rawBody);
            Map<String, Object> meta = new HashMap<>();
            meta.put("rawJson", legacyJson);
            return SeamlessOutcome.posted(0, meta);
        } catch (Exception e) {
            logger.error("AwcSeamlessAggregator legacy delegate failed for action=" + req.action, e);
            return SeamlessOutcome.serverError(e);
        } finally {
            try {
                com.vinplay.dal.service.seamless.AggregatorMetrics.recordHandle(
                        "Awc" + actionLabel(req.action), System.nanoTime() - t0);
            } catch (Throwable ignored) { /* never let metrics kill dispatch */ }
        }
    }

    /** Title-case the action verb for a stable metrics bucket name. */
    private static String actionLabel(String action) {
        if (action == null || action.isEmpty()) return "Unknown";
        return Character.toUpperCase(action.charAt(0)) + action.substring(1);
    }

    private SeamlessOutcome handleGetBalance(AwcRequest req) {
        String awcUserId = req.message.optString("userId", "");
        if (awcUserId.isEmpty()) return SeamlessOutcome.userNotFound();

        // Resolve to (userId, vin) — same lookup as base's lookupUser but
        // we need users.id for the residue-tracker query, so duplicate the
        // SELECT here and stash userId in metadata for serializeResponse.
        long userIdLong = 0;
        long vin = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, vin FROM vinplay.users WHERE nick_name = ? OR user_name = ? LIMIT 1")) {
            ps.setString(1, awcUserId);
            ps.setString(2, awcUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    userIdLong = rs.getLong("id");
                    vin = rs.getLong("vin");
                }
            }
        } catch (Exception e) {
            logger.error("AwcSeamlessAggregator.getBalance lookup failed for " + awcUserId, e);
            return SeamlessOutcome.serverError(e);
        }
        if (userIdLong == 0) return SeamlessOutcome.userNotFound();

        Map<String, Object> meta = new HashMap<>();
        meta.put("awc_user_id", userIdLong);
        meta.put("awc_user_external", awcUserId);
        return SeamlessOutcome.posted(vin, meta);
    }

    @Override
    protected String serializeResponse(SeamlessOutcome out) {
        // Phase 2 legacy delegate: handler returned the raw wire JSON
        // already, just pass it through. Avoids reformatting (and possibly
        // dropping fields) that the legacy code computed correctly.
        Object raw = out.metadata.get("rawJson");
        if (raw instanceof String) return (String) raw;

        JSONObject json = new JSONObject();
        SeamlessStatus s;
        switch (out.status) {
            case POSTED:
            case DUPLICATE:
                s = SeamlessStatus.OK;
                break;
            case INSUFFICIENT_BALANCE:
                s = SeamlessStatus.INSUFFICIENT_BALANCE;
                break;
            case USER_NOT_FOUND:
                s = SeamlessStatus.INVALID_USER;
                break;
            case SIGNATURE_ERROR:
                s = SeamlessStatus.UNAUTHORIZED;
                break;
            case VALIDATION_ERROR:
            case SERVER_ERROR:
            default:
                s = SeamlessStatus.INTERNAL_ERROR;
                break;
        }
        json.put("status", s.wireCode());
        json.put("desc", out.errorMessage == null ? "" : out.errorMessage);
        if (out.status == SeamlessOutcome.Status.POSTED || out.status == SeamlessOutcome.Status.DUPLICATE) {
            Object userIdObj = out.metadata.get("awc_user_id");
            long userIdLong = userIdObj instanceof Long ? (Long) userIdObj : 0L;
            int residue = userIdLong > 0 ? AwcResidueTracker.getResidue(userIdLong) : 0;
            double display = out.newBalance + residue / 1000.0;
            json.put("balance", display);
            json.put("balanceTs", OffsetDateTime.now(ZoneOffset.ofHours(8))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            json.put("userId", out.metadata.getOrDefault("awc_user_external", ""));
        }
        return json.toString();
    }

    /**
     * Static helper — call from AwcCallbackProcessor when flag on + action
     * migrated. Returns wire JSON, or null if not eligible (caller falls back
     * to legacy path).
     */
    public static String handleIfEligible(HttpServletRequest http, String action) {
        if (!isEnabled() || !isActionMigrated(action)) return null;
        return new AwcSeamlessAggregator().handle(http);
    }

    // ──────────────────────────────────────────────────────────────────
    // SXB-OBS — durable inbound audit. Mirror of the GSC pattern
    // (GscDepositAggregator.preAudit / postAudit → GscEventLogger).
    // Wires every /awc/callback through AwcEventLogger so the Grafana
    // awc-latency dashboard sees per-action received_at / processed_at.
    //
    // The action verb is extracted from the body inside the logger
    // (parseRequest hasn't necessarily run by the time preAudit fires
    // in the base class lifecycle — readBody then preAudit then parse,
    // so the body string is available but the JSONObject isn't).
    // Falls back to "unknown" if parse fails — raw_payload still
    // captures the original bytes for forensic review.
    //
    // Never throws — AwcEventLogger is defensive at both call sites.
    // ──────────────────────────────────────────────────────────────────

    @Override
    protected long preAudit(String rawBody, HttpServletRequest http) {
        String action = "unknown";
        try {
            if (rawBody != null && rawBody.trim().startsWith("{")) {
                JSONObject outer = new JSONObject(rawBody);
                Object msg = outer.opt("message");
                if (msg instanceof JSONObject) {
                    action = ((JSONObject) msg).optString("action", "unknown");
                } else if (msg instanceof String) {
                    action = new JSONObject((String) msg).optString("action", "unknown");
                }
            }
        } catch (Throwable ignored) { /* leave action="unknown"; raw_payload still captured */ }
        if (action == null || action.isEmpty()) action = "unknown";
        return AwcEventLogger.tryLogRequest(action, rawBody, http);
    }

    @Override
    protected void postAudit(long auditId, SeamlessOutcome out, String responseJson) {
        if (auditId <= 0L) return;
        boolean ok = out != null
                && (out.status == SeamlessOutcome.Status.POSTED
                || out.status == SeamlessOutcome.Status.DUPLICATE);
        String lastError = (out == null || ok) ? null : out.errorMessage;
        AwcEventLogger.tryLogResponse(auditId, ok ? "COMPLETED" : "FAILED",
                responseJson, lastError);
    }
}
