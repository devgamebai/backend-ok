package com.vinplay.dal.service.seamless;

/**
 * SUN-1236: shared seamless-wallet status codes used across AWC, GSC, and
 * future provider integrations.
 *
 * <p>Both AWC and GSC return the same wire format: a {@code "status"} field
 * with one of {@code 0000} / {@code 1001} / {@code 1002} / {@code 9999}.
 * Centralizing here removes the string-literal sprawl in each processor and
 * gives the future {@link SeamlessWalletAggregator}-based subclasses a
 * single enum to project into their provider-specific response JSON.
 *
 * <p>Wire codes follow AWC's documented contract (kept stable for backward
 * compatibility):
 * <ul>
 *   <li>{@code 0000} — success</li>
 *   <li>{@code 1000} — invalid user (bad/missing userId)</li>
 *   <li>{@code 1001} — generic error / unauthorized / insufficient balance
 *       (legacy umbrella; new code paths SHOULD prefer 1002 for the
 *       insufficient-balance signal)</li>
 *   <li>{@code 1002} — insufficient balance (explicit)</li>
 *   <li>{@code 9999} — internal server error</li>
 * </ul>
 *
 * <p>Existing string literals in {@code AwcCallbackProcessor.errResp(...)}
 * stay valid — this enum is additive. As call sites migrate to the
 * aggregator pattern they should switch to {@link #wireCode()}.
 */
public enum SeamlessStatus {
    OK("0000"),
    INVALID_USER("1000"),
    UNAUTHORIZED("1001"),
    INSUFFICIENT_BALANCE("1002"),
    INTERNAL_ERROR("9999");

    private final String wireCode;

    SeamlessStatus(String wireCode) {
        this.wireCode = wireCode;
    }

    /**
     * @return the on-the-wire code (string, 4-digit) used in the {@code status}
     *         JSON field of every seamless-wallet response.
     */
    public String wireCode() {
        return wireCode;
    }

    /**
     * Reverse lookup — useful for tests and audit-log analysis.
     */
    public static SeamlessStatus fromWireCode(String code) {
        if (code == null) return null;
        for (SeamlessStatus s : values()) {
            if (s.wireCode.equals(code)) return s;
        }
        return null;
    }
}
