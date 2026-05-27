package com.vinplay.dal.service.seamless;

/**
 * Result of {@link SeamlessWalletAggregator#validateBusinessRules}: did the
 * parsed request pass the per-aggregator business-rule checks (currency
 * whitelist, action enum membership, required-field presence, …) before any
 * wallet-moving call?
 *
 * <p>Distinct from {@link VerifyResult}: signature verification answers
 * "is the caller authentic?", validation answers "is the request well-formed
 * for our wallet rules?". The two errors map to different provider response
 * codes, so they are kept in separate types.
 *
 * <p>The {@code code} field is the provider-meaningful error string the
 * subclass wants surfaced (e.g. AWC's {@code "1006"} for missing currency,
 * GSC's {@code "INVALID_PARAM"}). It bubbles into
 * {@link SeamlessOutcome#errorCode} and is then projected into the provider's
 * JSON wire format by {@code serializeResponse}.
 */
public final class ValidationResult {

    public final boolean ok;
    public final String code;
    public final String message;

    private ValidationResult(boolean ok, String code, String message) {
        this.ok = ok;
        this.code = code;
        this.message = message;
    }

    /** Validation passed. {@code code} and {@code message} are null. */
    public static ValidationResult ok() {
        return new ValidationResult(true, null, null);
    }

    /**
     * Validation failed.
     *
     * @param code    provider-meaningful error code (e.g. "1006"). Surfaces
     *                in the response JSON via the subclass.
     * @param message human-readable diagnostic — logged, possibly surfaced.
     */
    public static ValidationResult fail(String code, String message) {
        return new ValidationResult(false, code, message);
    }
}
