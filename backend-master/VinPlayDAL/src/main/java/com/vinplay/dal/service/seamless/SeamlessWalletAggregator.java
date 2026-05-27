package com.vinplay.dal.service.seamless;

import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Template-method base class for seamless-wallet aggregators (AWC, GSC,
 * future SBO/IBC, …). Subclass per provider; the base owns the race-sensitive
 * flow (signature → validation → wallet movement → response → audit) so each
 * subclass collapses to "what is different about this provider".
 *
 * <p><b>Why this exists.</b> The legacy AWC and GSC processors each grew
 * their own dedup gate (AWC: SELECT-then-INSERT on {@code awc_transactions}
 * — audit #18; GSC: Hazelcast {@code containsKey}/{@code put} on
 * {@code gsc_tx_ids} — audit #19). Both are TOCTOU races. The single fix is
 * to route every wallet-moving call through {@link MoneyGateway#creditUser}
 * / {@link MoneyGateway#debitUser}, whose UNIQUE
 * {@code (tx_id, source, user_id)} on {@code money_gateway_log} is the
 * race-safe dedup gate (audit #17, see {@code MoneyGateway.java:633}).
 *
 * <p>By centralizing that call in {@link #doDebit}/{@link #doCredit} as
 * {@code final} primitives, a new aggregator subclass <em>cannot</em>
 * reintroduce a SELECT-then-INSERT dedup. Structural prevention.
 *
 * <h2>Lifecycle</h2>
 * <pre>
 *   handle(http)                              -- final
 *     ├─ readBody(http)                       -- final
 *     ├─ preAudit(rawBody, http)              -- overridable hook
 *     ├─ parseRequest(rawBody, http)          -- abstract
 *     ├─ verifySignature(req, http)           -- abstract; bail on !ok
 *     ├─ validateBusinessRules(req)           -- overridable; bail on !ok
 *     ├─ dispatch(req)                        -- abstract; the per-action switch
 *     │     calls doDebit / doCredit / doReadBalance (all final)
 *     ├─ serializeResponse(out)               -- abstract
 *     └─ postAudit(auditId, out, json)        -- overridable hook (best-effort)
 * </pre>
 *
 * <p>{@code handle()} catches {@link Throwable} — defensively, mirroring the
 * MoneyGateway dual-write call sites — so a class-load failure or any other
 * runtime error in a subclass cannot propagate as a 500 back to the provider.
 *
 * <h2>What lives where</h2>
 * <ul>
 *   <li><b>Per-aggregator code (small):</b> the concrete subclass. Owns the
 *       request-shape parsing, signature scheme, currency rules, action→source
 *       mapping, response JSON shape, and the dispatch switch.
 *   <li><b>Shared race-sensitive code:</b> this class. Owns body reading,
 *       audit hook timing, dispatch wrapping (try/catch), and the wallet-call
 *       primitives.
 *   <li><b>Wallet primitives:</b> {@link MoneyGateway} (unchanged). The
 *       single point where {@code users.vin} can change.
 * </ul>
 *
 * <p><b>Phase 1 status:</b> scaffolding only — no production callers. AWC
 * still routes through {@code AwcCallbackProcessor}, GSC still routes through
 * {@code gscSeamless/*Process.java}. Phases 2–4 migrate them behind feature
 * flags. See {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md}.
 *
 * @param <REQ> the parsed request type the subclass produces from the body
 * @param <RES> reserved for future use (the wire-response type) — currently
 *              unused at the base level since {@code serializeResponse}
 *              returns a String
 */
public abstract class SeamlessWalletAggregator<REQ, RES> {

    protected static final Logger logger = Logger.getLogger("backend");

    // -----------------------------------------------------------------
    // Public entry-point — final
    // -----------------------------------------------------------------

    /** Phase 5p3 — handle() WARN threshold: any single call slower than this. */
    private static final long SLOW_HANDLE_NANOS = 50L * 1_000_000L; // 50ms

    /**
     * Process one inbound provider request end-to-end, returning the
     * provider-shape JSON response. This method never throws — any
     * {@link Throwable} from a subclass hook is caught and surfaced as a
     * {@link SeamlessOutcome.Status#SERVER_ERROR} outcome.
     *
     * <p><b>Phase 5p3 timing wrapper.</b> The whole body is wrapped in
     * try/finally so {@link AggregatorMetrics#recordHandle} fires on every
     * exit path — success, exception, or {@link Error}. The wrapper itself
     * never throws and never alters the outcome. A WARN log fires when a
     * single call exceeds {@value #SLOW_HANDLE_NANOS} nanos (50ms);
     * sustained p99 breach is detected separately by
     * {@code AggregatorP99Scheduler} polling {@link AggregatorMetrics}.
     *
     * @param http the inbound servlet request — body is consumed exactly once
     * @return JSON string in the provider's wire format
     */
    public final String handle(HttpServletRequest http) {
        final long t0 = System.nanoTime();
        String json = "{}";
        SeamlessOutcome.Status outcomeStatus = null;
        try {
            String rawBody;
            try {
                rawBody = readBody(http);
            } catch (IOException e) {
                // Body unread: skip the audit pre-row entirely (we have nothing to
                // audit), serialize a server-error outcome, return.
                logger.error("SeamlessWalletAggregator readBody failed", e);
                SeamlessOutcome bodyErr = SeamlessOutcome.serverError(e);
                outcomeStatus = bodyErr.status;
                json = serializeResponseSafely(bodyErr);
                return json;
            }

            long auditId;
            try {
                auditId = preAudit(rawBody, http);
            } catch (Throwable t) {
                // preAudit must never block the request — log and proceed.
                logger.warn("SeamlessWalletAggregator preAudit failed (continuing): " + t.getMessage(), t);
                auditId = 0L;
            }

            SeamlessOutcome out;
            try {
                REQ req = parseRequest(rawBody, http);
                VerifyResult vr = verifySignature(req, http);
                if (!vr.ok) {
                    out = SeamlessOutcome.signatureError(vr.message);
                } else {
                    ValidationResult v = validateBusinessRules(req);
                    if (!v.ok) {
                        out = SeamlessOutcome.validationError(v.code, v.message);
                    } else {
                        out = dispatch(req);
                    }
                }
            } catch (Throwable t) {
                // Defense in depth: a class-load failure (NoClassDefFoundError),
                // missing JDBC driver, or any other Error cannot be allowed to
                // bubble out as 500 to the provider — providers respond to
                // anything-but-200 by retrying, possibly forever. Log + serialize
                // a server-error and let the legacy / dedup machinery on the next
                // retry sort it out.
                logger.error("SeamlessWalletAggregator unexpected error", t);
                out = SeamlessOutcome.serverError(t);
            }

            outcomeStatus = out.status;
            json = serializeResponseSafely(out);

            try {
                postAudit(auditId, out, json);
            } catch (Exception ignore) {
                // Already logged inside the impl if it cared; per-spec we never
                // fail the response on audit failure.
            }
            return json;
        } finally {
            // Timing instrumentation — must NEVER alter the response, must
            // NEVER throw out of this finally. Catch Throwable around every
            // recording call.
            try {
                long elapsedNanos = System.nanoTime() - t0;
                String mname = safeMetricsName();
                AggregatorMetrics.recordHandle(mname, elapsedNanos);
                if (elapsedNanos > SLOW_HANDLE_NANOS) {
                    long elapsedMs = elapsedNanos / 1_000_000L;
                    logger.warn("SeamlessWalletAggregator slow handle: name=" + mname
                            + " elapsedMs=" + elapsedMs
                            + " status=" + (outcomeStatus == null ? "UNKNOWN" : outcomeStatus.name())
                            + " (threshold=50ms)");
                }
            } catch (Throwable timingErr) {
                // Last-ditch defence: never let metrics break the handler.
                try {
                    logger.warn("SeamlessWalletAggregator timing wrapper error: " + timingErr.getMessage());
                } catch (Throwable ignore) { /* nothing more we can do */ }
            }
        }
    }

    /**
     * Resolve {@link #metricsName()} defensively — a buggy subclass that
     * throws or returns null cannot poison the metrics path.
     */
    private String safeMetricsName() {
        try {
            String n = metricsName();
            if (n == null || n.isEmpty()) return getClass().getSimpleName();
            return n;
        } catch (Throwable t) {
            return getClass().getSimpleName();
        }
    }

    /**
     * Wraps {@code serializeResponse} so a subclass throw doesn't bubble out
     * — the provider must always get a String back, even if it's an empty
     * {@code "{}"}. (A truly broken subclass will be caught by an alarm; the
     * goal here is graceful degradation.)
     */
    private String serializeResponseSafely(SeamlessOutcome out) {
        try {
            String s = serializeResponse(out);
            return s != null ? s : "{}";
        } catch (Throwable t) {
            logger.error("SeamlessWalletAggregator serializeResponse failed; returning {}", t);
            return "{}";
        }
    }

    /**
     * Read the request body into a String exactly once. Mirrors the pattern
     * in {@code DepositProcess.java:46-53} — calling {@code getReader()}
     * twice on a servlet request returns nothing the second time, so the
     * body must be cached here for both parse and audit use.
     */
    protected final String readBody(HttpServletRequest http) throws IOException {
        // Jetty's JettyServlet calls getParameterMap() before dispatching,
        // which consumes the input stream for form-encoded POSTs. A later
        // getReader() then throws IllegalStateException("STREAMED"). When
        // that happens we hand back an empty string — every concrete
        // parseRequest() implementation already falls back to
        // http.getParameter(...) when the body is missing or non-JSON, so
        // form-encoded callers (AWC, GSC) keep working unchanged.
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = http.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IllegalStateException streamConsumed) {
            return "";
        }
    }

    // -----------------------------------------------------------------
    // Shared flow primitives — final. Subclasses CALL these from dispatch,
    // never override. Keeping them final is the structural guarantee that
    // a new aggregator cannot reintroduce a TOCTOU dedup.
    // -----------------------------------------------------------------

    /**
     * Debit a user's wallet, race-safely. The {@code txn.externalRef} +
     * {@link #mapActionToSource} pair is THE dedup gate — backed by the
     * UNIQUE {@code (tx_id, source, user_id)} on {@code money_gateway_log}
     * (audit #17). Provider retries with the same {@code externalRef} are
     * handled here by mapping the gateway's {@code "Duplicate transaction"}
     * error to {@link SeamlessOutcome.Status#DUPLICATE} — caller does not
     * see an exception.
     *
     * @return outcome — POSTED on success, DUPLICATE on retry, INSUFFICIENT_BALANCE
     *         when wallet floor-check trips, USER_NOT_FOUND when the username
     *         resolves to nothing, SERVER_ERROR otherwise
     */
    protected final SeamlessOutcome doDebit(SeamlessTxn txn) {
        UserRef user = lookupUser(txn.getUsername());
        if (user == null) {
            logger.warn("SeamlessWalletAggregator.doDebit: user not found username=" + txn.getUsername());
            return SeamlessOutcome.userNotFound();
        }

        String description = aggregatorName() + " " + txn.getAction() + " " + txn.getExternalRef();

        MoneyGateway.CreditResult cr = MoneyGateway.debitUser(
                user.id, user.nickname, txn.getAmountSubunit(),
                mapActionToSource(txn.getAction()),
                txn.getExternalRef(),
                description);
        return mapGatewayResult(cr, /*isCredit=*/false, user);
    }

    /**
     * Credit a user's wallet, race-safely. Mirror of {@link #doDebit};
     * see that method's doc for dedup semantics.
     */
    protected final SeamlessOutcome doCredit(SeamlessTxn txn) {
        UserRef user = lookupUser(txn.getUsername());
        if (user == null) {
            logger.warn("SeamlessWalletAggregator.doCredit: user not found username=" + txn.getUsername());
            return SeamlessOutcome.userNotFound();
        }

        String description = aggregatorName() + " " + txn.getAction() + " " + txn.getExternalRef();

        MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                user.id, user.nickname, txn.getAmountSubunit(),
                mapActionToSource(txn.getAction()),
                txn.getExternalRef(),
                description);
        return mapGatewayResult(cr, /*isCredit=*/true, user);
    }

    /**
     * Read-only balance lookup. No wallet movement, no audit row, no dedup —
     * pure GET. Returns {@link SeamlessOutcome.Status#POSTED} with the current
     * balance on success, {@code USER_NOT_FOUND} if the username does not
     * resolve, {@code SERVER_ERROR} if the DB read trips.
     */
    protected final SeamlessOutcome doReadBalance(String username) {
        if (username == null || username.isEmpty()) return SeamlessOutcome.userNotFound();
        // Single-query path: combine the user-id lookup with the balance read.
        // Saves one pool acquisition + one round-trip per balance request.
        // The OR(nick_name, user_name) uses index_merge over the two UNIQUE
        // indexes — same plan as the legacy split-call lookupUser; just one
        // connection lease instead of two. Drops p99 connection contention
        // on /balance under burst load.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vin FROM vinplay.users WHERE nick_name = ? OR user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return SeamlessOutcome.ok(rs.getLong("vin"));
                }
            }
            return SeamlessOutcome.userNotFound();
        } catch (Exception e) {
            logger.error("SeamlessWalletAggregator.doReadBalance: DB error user=" + username, e);
            return SeamlessOutcome.serverError(e);
        }
    }

    /**
     * Map the gateway's loosely-typed {@code CreditResult.error} string into
     * a typed {@link SeamlessOutcome}. The strings used here are the literal
     * values returned by {@link MoneyGateway#creditUser} /
     * {@link MoneyGateway#debitUser} ({@code MoneyGateway.java:195/406/432/etc.});
     * if those strings ever change, this mapper must change with them.
     */
    private SeamlessOutcome mapGatewayResult(MoneyGateway.CreditResult cr,
                                             boolean isCredit,
                                             UserRef user) {
        if (cr.success) {
            return SeamlessOutcome.ok(cr.newBalance);
        }
        String err = cr.error == null ? "" : cr.error;
        if ("Duplicate transaction".equals(err)) {
            // Idempotent retry: the wallet was not moved this call, but we
            // want the response to look like a normal success so the provider
            // stops retrying. The current balance is best-effort fetched via
            // a separate read; if that read fails, fall back to 0.
            long currentBalance = bestEffortReadBalance(user.id);
            return SeamlessOutcome.duplicate(currentBalance);
        }
        if (err.startsWith("Insufficient")) {
            long currentBalance = bestEffortReadBalance(user.id);
            return SeamlessOutcome.insufficientBalance(currentBalance);
        }
        if (err.startsWith("User not found")) {
            return SeamlessOutcome.userNotFound();
        }
        // Anything else (DB error, validation failure inside the gateway) →
        // SERVER_ERROR with the raw message — the subclass decides how to
        // serialize it back.
        logger.warn("SeamlessWalletAggregator: gateway " + (isCredit ? "credit" : "debit")
                + " failed user=" + user.nickname + " err=" + err);
        return SeamlessOutcome.serverError(/*code=*/null, err);
    }

    private long bestEffortReadBalance(long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vin FROM vinplay.users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("vin") : 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Resolve {@code (users.id, users.nick_name)} from {@code users.user_name}
     * in one round-trip. Same pattern as
     * {@code AwcCallbackProcessor.lookupUser} (line 695-711).
     *
     * <p>Returns {@code null} when no row matches.
     */
    private UserRef lookupUser(String username) {
        if (username == null || username.isEmpty()) return null;
        // SUN-EXPLOIT-GUARD V4 (2026-05-03): GSC's `member_account` is the
        // public identity (nick_name in our schema), NOT the login user_name.
        // For accounts where user_name != nick_name (e.g. user_name=
        // "nguoinaodo", nick_name="laviai"), the legacy Hazelcast lookup
        // worked because the `users` IMap is keyed by nick_name. The new
        // aggregator path queried user_name → 0 rows → SeamlessOutcome
        // .userNotFound() → GSC /balance answered "Member not found" →
        // GSC /launch-game returned codeId=406 with empty token.
        // Try nick_name first (the GSC convention), fall back to user_name
        // for the small minority where the two are identical.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_name, nick_name FROM vinplay.users "
                             + "WHERE nick_name = ? OR user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nick = rs.getString("nick_name");
                    return new UserRef(rs.getLong("id"),
                            (nick != null && !nick.isEmpty()) ? nick : username);
                }
            }
        } catch (Exception e) {
            logger.warn("SeamlessWalletAggregator.lookupUser failed for " + username + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Aggregator-name tag used in the gateway audit description string. Default
     * is the simple class name; override only if a subclass wants a tighter
     * fixed tag (e.g. "AWC" rather than "AwcAggregator").
     */
    protected String aggregatorName() {
        return getClass().getSimpleName();
    }

    /**
     * Phase 5p3 — per-handler tag used for timing metrics buckets and
     * Telegram p99 alerts. Distinct from {@link #aggregatorName()} on
     * purpose: {@code aggregatorName} is the provider tag baked into
     * {@code money_gateway_log.description} (e.g. {@code "GSC"}, shared
     * by all 6 GSC handlers — changing it would break audit conventions),
     * whereas {@code metricsName} is the unique per-handler bucket key
     * (e.g. {@code "GscBalance"}, {@code "GscPushBet"}) that the p99
     * scheduler uses to decide which handler is slow.
     *
     * <p>Default returns {@link #aggregatorName()} for backward compatibility
     * — single-handler aggregators (future AWC migration) need no override.
     * The 6 GSC subclasses override to return the distinct tag.
     */
    protected String metricsName() {
        return aggregatorName();
    }

    // -----------------------------------------------------------------
    // Hooks subclasses MUST implement
    // -----------------------------------------------------------------

    /**
     * Parse the raw HTTP body into the subclass's request DTO. Throwing here
     * is fine — {@link #handle} catches it and turns it into a
     * {@link SeamlessOutcome.Status#SERVER_ERROR}.
     */
    protected abstract REQ parseRequest(String body, HttpServletRequest http) throws Exception;

    /**
     * Verify the inbound request authenticates against the provider's
     * signing scheme (AWC: equality with shared cert; GSC: MD5 of
     * {@code operatorCode + requestTime + endpoint + secretKey}; etc.).
     */
    protected abstract VerifyResult verifySignature(REQ req, HttpServletRequest http);

    /**
     * Convert the provider's amount + currency into wallet sub-units.
     * AWC sends VND in whole units (returns {@code (long) amount}); GSC sends
     * a display currency that needs {@code Math.round(amount * exchangeRate)}.
     * Each subclass owns its rules — no shared rate logic forced.
     */
    protected abstract long currencyToInternal(double providerAmount, String currency);

    /**
     * Convert an internal wallet balance back into the provider's display
     * currency for outbound responses. Inverse of {@link #currencyToInternal}.
     */
    protected abstract double currencyToExternal(long internalBalance, String currency);

    /**
     * Map the aggregator-specific action verb ("bet", "settle", "cancelBet", …)
     * into one of the {@code MoneyGateway.SOURCE_*} constants (e.g.
     * {@link MoneyGateway#SOURCE_AWC_DEBIT}). The returned source plus the
     * txn's {@code externalRef} together form the dedup key.
     */
    protected abstract String mapActionToSource(String aggregatorAction);

    /**
     * Project a {@link SeamlessOutcome} into the provider's wire response
     * JSON. The base class never touches provider JSON — that's the only
     * thing each subclass really owns.
     */
    protected abstract String serializeResponse(SeamlessOutcome out);

    /**
     * Per-action switch. Subclass builds a {@link SeamlessTxn} from the
     * parsed {@code REQ} and calls one of {@link #doDebit}, {@link #doCredit},
     * {@link #doReadBalance}, or returns its own {@link SeamlessOutcome} for
     * non-wallet actions.
     */
    protected abstract SeamlessOutcome dispatch(REQ req);

    // -----------------------------------------------------------------
    // Hooks with sensible defaults — subclass overrides only if needed
    // -----------------------------------------------------------------

    /**
     * Write the inbound raw payload to a per-aggregator audit table BEFORE
     * any wallet logic runs (e.g. {@code GscEventLogger.tryLogRequest},
     * AWC's {@code saveTxn}). Returns an opaque audit row id that
     * {@link #postAudit} receives back. Default: no-op, returns 0.
     */
    protected long preAudit(String rawBody, HttpServletRequest http) {
        return 0L;
    }

    /**
     * Write the outcome + serialized response back into the audit row from
     * {@link #preAudit}, e.g. {@code GscEventLogger.tryLogResponse}. Best
     * effort — exceptions are swallowed in {@link #handle}. Default: no-op.
     */
    protected void postAudit(long auditId, SeamlessOutcome out, String responseJson) {
        // default no-op
    }

    /**
     * Per-aggregator business-rule check: currency whitelist, action enum
     * membership, required-field presence, etc. Runs after signature
     * verification, before {@link #dispatch}. Default: pass.
     */
    protected ValidationResult validateBusinessRules(REQ req) {
        return ValidationResult.ok();
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static final class UserRef {
        final long id;
        final String nickname;
        UserRef(long id, String nickname) {
            this.id = id;
            this.nickname = nickname;
        }
    }
}
