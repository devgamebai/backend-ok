package com.sunwinkr.lottery.engine.bet;

/**
 * Outcome of {@link BetAcceptor#accept(BetRequest)}.
 *
 * <p>Error codes — per
 * {@code docs/plans/lottery-extraction-plan.md §2.3 B1}:
 * <ul>
 *   <li>{@code 0000} — OK</li>
 *   <li>{@code 0001} — wallet rejected (generic adapter failure)</li>
 *   <li>{@code 0002} — bets locked (window closed)</li>
 *   <li>{@code 0003} — insufficient funds</li>
 *   <li>{@code 0004} — unknown / invalid mode</li>
 *   <li>{@code 0005} — invalid ticket number shape</li>
 * </ul>
 *
 * <p>Legacy {@code LotteryModule.buyTicket} silently dropped invalid
 * inputs ({@code TextUtils.isEmpty(num)} return, JLM:208-209). The
 * engine returns an explicit code — no silent drops, per audit hardening.
 */
public final class BetAcceptResult {

    private final String errorCode;
    private final long currentMoney;
    private final Long ticketId;

    private BetAcceptResult(String errorCode, long currentMoney, Long ticketId) {
        this.errorCode = errorCode;
        this.currentMoney = currentMoney;
        this.ticketId = ticketId;
    }

    public static BetAcceptResult ok(long currentMoney, long ticketId) {
        return new BetAcceptResult("0000", currentMoney, ticketId);
    }

    /** Wallet adapter rejected the debit (1001-equiv from legacy). */
    public static BetAcceptResult walletRejected() {
        return new BetAcceptResult("0001", -1L, null);
    }

    /** Bets locked — outside the 00:00 → 18:10 (Hanoi) window. */
    public static BetAcceptResult locked() {
        return new BetAcceptResult("0002", -1L, null);
    }

    /** Insufficient funds — wallet balance < {@code finalBetValue}. */
    public static BetAcceptResult insufficientFunds(long currentMoney) {
        return new BetAcceptResult("0003", currentMoney, null);
    }

    /** Unknown mode id (legacy returned null and exploded). */
    public static BetAcceptResult unknownMode() {
        return new BetAcceptResult("0004", -1L, null);
    }

    /** Invalid ticket number shape for the requested mode. */
    public static BetAcceptResult invalidNumber() {
        return new BetAcceptResult("0005", -1L, null);
    }

    /** SUN-1366 — bet exceeds the per-number cap for the requested mode. */
    public static BetAcceptResult betExceedsCap() {
        return new BetAcceptResult("0006", -1L, null);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getCurrentMoney() {
        return currentMoney;
    }

    /** @return DB id of the persisted ticket, or {@code null} on any failure */
    public Long getTicketId() {
        return ticketId;
    }

    public boolean isSuccess() {
        return "0000".equals(errorCode);
    }
}
