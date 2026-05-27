package com.sunwinkr.lottery.engine.port;

/**
 * Result of a {@link WalletPort#debit} or {@link WalletPort#credit} call.
 *
 * <p>Shape mirrors the legacy {@code com.vinplay.vbee.common.response.MoneyResponse}
 * without leaking that dependency into the pure-Java engine module.
 *
 * <p>{@code currentMoney} is the wallet balance immediately after the call
 * — {@code -1} when the call failed. Caller MUST inspect {@link #isSuccess()}
 * before trusting {@code currentMoney}.
 *
 * <p>Error code semantics (preserves
 * {@code docs/plans/lottery-extraction-plan.md §2.3 B1}):
 * <ul>
 *   <li>{@code 0000} — success</li>
 *   <li>{@code 0001} — wallet rejected (generic adapter failure)</li>
 *   <li>{@code 0003} — insufficient funds</li>
 * </ul>
 */
public final class MoneyResult {

    private final boolean success;
    private final long currentMoney;
    private final String errorCode;

    private MoneyResult(boolean success, long currentMoney, String errorCode) {
        this.success = success;
        this.currentMoney = currentMoney;
        this.errorCode = errorCode;
    }

    public static MoneyResult ok(long currentMoney) {
        return new MoneyResult(true, currentMoney, "0000");
    }

    public static MoneyResult fail(String errorCode) {
        return new MoneyResult(false, -1L, errorCode);
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
}
