package com.vinplay.dal.service.seamless.gsc;

import java.util.Map;

/**
 * Discriminator carried in {@link com.vinplay.dal.service.seamless.SeamlessOutcome#metadata}
 * under key {@code "shape"} so each aggregator's {@code serializeResponse}
 * can dispatch on a typed enum instead of a free-form string.
 *
 * <p><b>Why an enum.</b> Phase 3 aggregators (PushBet, Transfer, Cancel,
 * Rollback, Deposit, Withdraw) all branch in {@code serializeResponse} on
 * a metadata-tag that selects between their native wire shape, the legacy
 * {@code BalanceResponse} fall-back used on unknown-currency, and (for
 * Withdraw) a distinct {@code withdraw_error} shape. Phase 5 cleanup
 * (deferred NIT 23c) replaces the {@code String} keys with this enum so a
 * typo can't silently cascade into a wrong wire shape.
 *
 * <p><b>Wire-shape parity.</b> The enum is purely an internal discriminator;
 * no enum value or name reaches the response payload. The aggregators
 * still emit byte-identical JSON to the legacy code paths.
 *
 * <p><b>Backward-compatibility.</b> Each {@code serializeResponse} also
 * accepts the legacy lowercase {@code String} forms ({@code "transfer"},
 * {@code "balance"}, etc.) so a stray older outcome with string-typed
 * shape metadata (e.g. one read out of an audit row built before this
 * change landed) still serializes to the right shape.
 */
public enum GscResponseShape {
    /** Empty {@code BalanceResponse} fall-back used by every aggregator on unknown-currency. */
    BALANCE_FALLBACK,
    /** Native {@code PushBetResponse} shape. */
    PUSHBET,
    /** Native {@code TransferResponse} shape (also used by deposit/withdraw success). */
    TRANSFER,
    /** Native {@code CancelResponse} shape. */
    CANCEL,
    /** Native {@code RollbackResponse} shape. */
    ROLLBACK,
    /** Native {@code DepositResponse} shape. */
    DEPOSIT,
    /** Native {@code WithdrawResponse} shape. */
    WITHDRAW,
    /**
     * Withdraw error envelope — distinct from {@link #WITHDRAW} because
     * legacy {@code WithdrawProcess} returns a different JSON shape on
     * insufficient-balance / signature-error than on success.
     */
    WITHDRAW_ERROR;

    /**
     * Read the shape discriminator from outcome metadata. Accepts a
     * typed enum value (current Phase 5 form) or a lowercase legacy
     * {@code String} (pre-NIT-23c form) so a stray older outcome built
     * with the string form still serializes to the right shape during
     * a rolling deploy. Returns {@code defaultShape} when the key is
     * absent or unrecognized.
     */
    public static GscResponseShape from(Map<String, Object> meta, GscResponseShape defaultShape) {
        if (meta == null) return defaultShape;
        Object v = meta.get("shape");
        if (v instanceof GscResponseShape) return (GscResponseShape) v;
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("balance".equalsIgnoreCase(s)) return BALANCE_FALLBACK;
            if ("pushbet".equalsIgnoreCase(s)) return PUSHBET;
            if ("transfer".equalsIgnoreCase(s)) return TRANSFER;
            if ("cancel".equalsIgnoreCase(s)) return CANCEL;
            if ("rollback".equalsIgnoreCase(s)) return ROLLBACK;
            if ("deposit".equalsIgnoreCase(s)) return DEPOSIT;
            if ("withdraw".equalsIgnoreCase(s)) return WITHDRAW;
            if ("withdraw_error".equalsIgnoreCase(s)) return WITHDRAW_ERROR;
        }
        return defaultShape;
    }
}
