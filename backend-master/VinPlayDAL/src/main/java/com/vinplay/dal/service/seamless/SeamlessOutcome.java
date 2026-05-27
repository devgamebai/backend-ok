package com.vinplay.dal.service.seamless;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal, provider-agnostic result of one seamless-wallet event handled by
 * {@link SeamlessWalletAggregator}.
 *
 * <p>The subclass's {@code serializeResponse} hook projects this into the
 * provider's wire JSON shape (AWC's {@code {"errorCode":"0000","balance":…}},
 * GSC's {@code BalanceResponse.toJson()}, etc.). The base class never
 * touches provider JSON — that mapping is the only thing each subclass
 * really owns.
 *
 * <p>Immutable: all fields are {@code final}, the metadata map (if any) is
 * defensively copied + wrapped at construction. Construct via the static
 * factories below; the constructor is {@code private}.
 *
 * <h2>Status values</h2>
 * <ul>
 *   <li>{@link Status#POSTED} — wallet movement succeeded; {@code newBalance}
 *       holds the post-movement balance in sub-units.
 *   <li>{@link Status#DUPLICATE} — provider replayed an event already
 *       processed; nothing happened to the wallet, response should be 200/OK
 *       with the current balance so the provider's retry pipeline stops.
 *   <li>{@link Status#INSUFFICIENT_BALANCE} — debit was rejected by the
 *       atomic floor-check in {@code MoneyGateway.debitUser}.
 *   <li>{@link Status#USER_NOT_FOUND} — username could not be resolved.
 *   <li>{@link Status#SIGNATURE_ERROR} — {@code verifySignature} returned
 *       {@code !ok}. Short-circuits before any wallet call.
 *   <li>{@link Status#VALIDATION_ERROR} — {@code validateBusinessRules}
 *       returned {@code !ok}. Same short-circuit.
 *   <li>{@link Status#SERVER_ERROR} — uncaught exception bubbled up. The
 *       base class catches {@link Throwable} so a class-load or any other
 *       runtime failure cannot propagate as 500 to the provider; it is
 *       surfaced as this status with {@code errorMessage} populated.
 * </ul>
 */
public final class SeamlessOutcome {

    public enum Status {
        POSTED,
        DUPLICATE,
        INSUFFICIENT_BALANCE,
        USER_NOT_FOUND,
        SIGNATURE_ERROR,
        VALIDATION_ERROR,
        SERVER_ERROR,
        /**
         * SUN-1373 — the game/product is administratively disabled
         * ({@code gsc_game_catalog.active=0} / {@code awc_game_catalog.active=0}).
         * Bets MUST be rejected at the wallet-debit layer before any money moves.
         * Subclasses map this to provider-specific codes; OUR internal error code
         * is {@code "0011"} with message {@code "GAME_INACTIVE"}.
         */
        GAME_INACTIVE
    }

    public final Status status;
    /** Post-movement balance in sub-units. 0 when the operation did not move the wallet. */
    public final long newBalance;
    /** Subclass-meaningful error code (e.g. AWC "1001"). May be null. */
    public final String errorCode;
    /** Human-readable diagnostic; logged. May be null. */
    public final String errorMessage;
    /** Optional aggregator extras threaded through to the response/audit. Never null on access. */
    public final Map<String, Object> metadata;

    private SeamlessOutcome(Status status,
                            long newBalance,
                            String errorCode,
                            String errorMessage,
                            Map<String, Object> metadata) {
        this.status = status;
        this.newBalance = newBalance;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.metadata = (metadata == null)
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    // ---------------------------------------------------------------------
    // Factories
    // ---------------------------------------------------------------------

    /** Wallet movement succeeded. {@code newBalance} is the post-movement balance in sub-units. */
    public static SeamlessOutcome ok(long newBalance) {
        return new SeamlessOutcome(Status.POSTED, newBalance, null, null, null);
    }

    /**
     * POSTED outcome carrying aggregator-specific metadata. Used by
     * subclasses that need to thread per-batch results (e.g. GSC balance's
     * {@code BalanceResponseItem} list) from {@code dispatch} to
     * {@code serializeResponse} without having to stash state on the
     * aggregator instance — which would not be thread-safe given the
     * aggregator is held as a static singleton.
     *
     * <p>The metadata map is defensively copied + wrapped at
     * construction (see {@link #SeamlessOutcome(Status, long, String, String, Map)}).
     */
    public static SeamlessOutcome posted(long newBalance, Map<String, Object> metadata) {
        return new SeamlessOutcome(Status.POSTED, newBalance, null, null, metadata);
    }

    /**
     * Provider replayed an already-processed event. {@code currentBalance}
     * lets the subclass's {@code serializeResponse} echo a balance back to
     * the provider so its retry loop stops cleanly.
     */
    public static SeamlessOutcome duplicate(long currentBalance) {
        return new SeamlessOutcome(Status.DUPLICATE, currentBalance, null, null, null);
    }

    /**
     * Debit rejected by the atomic floor-check in {@code MoneyGateway.debitUser}.
     * {@code currentBalance} is best-effort (0 if not retrievable) — debate the
     * subclass-specific provider response; some providers want the current
     * balance, some don't.
     */
    public static SeamlessOutcome insufficientBalance(long currentBalance) {
        return new SeamlessOutcome(Status.INSUFFICIENT_BALANCE, currentBalance, null, null, null);
    }

    /** Username resolution failed. */
    public static SeamlessOutcome userNotFound() {
        return new SeamlessOutcome(Status.USER_NOT_FOUND, 0L, null, null, null);
    }

    /** Signature/auth check failed. {@code msg} is logged, not surfaced verbatim. */
    public static SeamlessOutcome signatureError(String msg) {
        return new SeamlessOutcome(Status.SIGNATURE_ERROR, 0L, null, msg, null);
    }

    /**
     * Business-rule validation failed.
     *
     * @param code provider-meaningful code from {@link ValidationResult}.
     * @param msg  diagnostic for logs.
     */
    public static SeamlessOutcome validationError(String code, String msg) {
        return new SeamlessOutcome(Status.VALIDATION_ERROR, 0L, code, msg, null);
    }

    /**
     * Validation-error variant that carries response metadata. Used when the
     * subclass's {@code serializeResponse} is shape-driven (it reads a
     * {@code "shape"} discriminator from metadata) but the audit/outcome
     * status still needs to reflect the actual failure mode (so
     * {@code postAudit} can correctly mark the audit row {@code FAILED}
     * instead of {@code COMPLETED}).
     */
    public static SeamlessOutcome validationError(String code, String msg,
                                                  Map<String, Object> metadata) {
        return new SeamlessOutcome(Status.VALIDATION_ERROR, 0L, code, msg, metadata);
    }

    /**
     * Unhandled exception in the aggregator pipeline. {@code t.toString()} is
     * captured as the message so the audit trail has enough to debug from.
     * {@code null} {@code t} is permitted; the message is then null.
     */
    public static SeamlessOutcome serverError(Throwable t) {
        return new SeamlessOutcome(Status.SERVER_ERROR, 0L, null,
                t != null ? t.toString() : null, null);
    }

    /**
     * Server-error variant carrying a subclass-specified error code (rarely
     * needed; the {@link Throwable} factory is the common path).
     */
    public static SeamlessOutcome serverError(String code, String msg) {
        return new SeamlessOutcome(Status.SERVER_ERROR, 0L, code, msg, null);
    }

    /**
     * SUN-1373 — the game is administratively disabled. No wallet movement
     * occurred. Subclass {@code serializeResponse} MUST map this to the
     * provider's game-inactive rejection code and ensure our internal
     * {@code errorCode} {@code "0011"} / message {@code "GAME_INACTIVE"} is
     * surfaced on OUR side so the FE can show a "table closed" popup.
     */
    public static SeamlessOutcome gameInactive() {
        return new SeamlessOutcome(Status.GAME_INACTIVE, 0L, "0011", "GAME_INACTIVE", null);
    }
}
