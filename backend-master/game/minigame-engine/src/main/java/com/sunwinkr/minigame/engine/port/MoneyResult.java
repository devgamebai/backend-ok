package com.sunwinkr.minigame.engine.port;

/**
 * Engine-side mirror of {@code com.vinplay.usercore.service.response
 * .MoneyResponse}. Immutable value type returned by
 * {@link WalletPort#debit} / {@link WalletPort#credit}.
 *
 * <p>{@link #currentMoney} reflects the post-call wallet balance. On
 * failure it is the caller's wallet balance at the point of the
 * lookup (legacy contract — TXR:416 {@code new MoneyResponse(false,
 * "1001")}).
 *
 * <p>Plan §4.1 / spec §6.
 */
public final class MoneyResult {

    private final boolean success;
    private final long currentMoney;
    private final String errorCode;

    public MoneyResult(boolean success, long currentMoney, String errorCode) {
        this.success = success;
        this.currentMoney = currentMoney;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getCurrentMoney() {
        return currentMoney;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Numeric error-code accessor for callers that prefer int (e.g. the
     * Sicbo bet path which compares against integer codes). Returns 0 on
     * success or when {@link #errorCode} is non-numeric.
     */
    public int getErrorCodeInt() {
        if (errorCode == null) {
            return 0;
        }
        try {
            return Integer.parseInt(errorCode);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public static MoneyResult success(long currentMoney) {
        return new MoneyResult(true, currentMoney, "0");
    }

    public static MoneyResult failure(long currentMoney, String errorCode) {
        return new MoneyResult(false, currentMoney, errorCode);
    }

    // ---- Legacy aliases (Sicbo PR-3 bet path) ------------------------------

    /** Sicbo PR-3 alias for {@link #success(long)}. */
    public static MoneyResult ok(long currentMoney) {
        return success(currentMoney);
    }

    /** Sicbo PR-3 alias — numeric error code (treated as the string form). */
    public static MoneyResult fail(int errorCode) {
        return failure(0L, Integer.toString(errorCode));
    }
}
