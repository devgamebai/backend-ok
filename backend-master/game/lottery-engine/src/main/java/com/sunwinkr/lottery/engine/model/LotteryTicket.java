package com.sunwinkr.lottery.engine.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable view of a single lottery ticket / {@code lode} row.
 *
 * <p>Snapshot columns ({@code betUnit}, {@code rateAtPurchase},
 * {@code prizeMultiplier}) capture {@link LotteryMode} state at purchase
 * time per SUN-1295 — the settle path reads these, never the live enum.
 *
 * <p>Pre-settle vs post-settle:
 * <ul>
 *   <li>{@code prize} is {@code null} until the settle loop credits the
 *       winning amount (zero for losses is still a Long).</li>
 *   <li>{@code settledAt} is {@code null} until {@code updatePrize}
 *       commits — closes audit finding L-1 (pre-settle result reveal).</li>
 *   <li>{@code ticketId} is {@code null} for new (not-yet-persisted)
 *       bets — populated after the {@code BetStore} insert.</li>
 *   <li>{@code settleStatus} defaults to {@link SettleStatus#PENDING} for
 *       tickets constructed without an explicit status (backward compat).</li>
 * </ul>
 *
 * <p>All datetime fields are in Vietnam wall clock — see {@code LotteryClock.VN}.
 */
public final class LotteryTicket {

    private final Long ticketId;
    private final long userId;
    private final String nickname;
    private final long betValue;
    private final int modeId;
    private final String ticket;
    private final Long prize;
    private final LocalDateTime createdDate;
    private final LocalDateTime settledAt;
    private final long betUnit;
    private final int rateAtPurchase;
    private final int prizeMultiplier;
    /** SUN-1339 B1: settle lifecycle state — mirrors {@code lode.settle_status} column. */
    private final SettleStatus settleStatus;

    /** Legacy 12-arg constructor — defaults {@code settleStatus} to {@link SettleStatus#PENDING}. */
    public LotteryTicket(Long ticketId,
                         long userId,
                         String nickname,
                         long betValue,
                         int modeId,
                         String ticket,
                         Long prize,
                         LocalDateTime createdDate,
                         LocalDateTime settledAt,
                         long betUnit,
                         int rateAtPurchase,
                         int prizeMultiplier) {
        this(ticketId, userId, nickname, betValue, modeId, ticket, prize,
             createdDate, settledAt, betUnit, rateAtPurchase, prizeMultiplier,
             SettleStatus.PENDING);
    }

    /** Full 13-arg constructor — use when loading from DB where settle_status is present. */
    public LotteryTicket(Long ticketId,
                         long userId,
                         String nickname,
                         long betValue,
                         int modeId,
                         String ticket,
                         Long prize,
                         LocalDateTime createdDate,
                         LocalDateTime settledAt,
                         long betUnit,
                         int rateAtPurchase,
                         int prizeMultiplier,
                         SettleStatus settleStatus) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.betValue = betValue;
        this.modeId = modeId;
        this.ticket = Objects.requireNonNull(ticket, "ticket");
        this.prize = prize;
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate");
        this.settledAt = settledAt;
        this.betUnit = betUnit;
        this.rateAtPurchase = rateAtPurchase;
        this.prizeMultiplier = prizeMultiplier;
        this.settleStatus = settleStatus != null ? settleStatus : SettleStatus.PENDING;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public long getBetValue() {
        return betValue;
    }

    public int getModeId() {
        return modeId;
    }

    public String getTicket() {
        return ticket;
    }

    /**
     * @return prize amount, or {@code null} if not yet settled. Zero is a
     *         valid settled value (player lost) — distinguished from
     *         pending by the {@code null} check.
     */
    public Long getPrize() {
        return prize;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * @return settle wall-clock timestamp (Vietnam TZ), or {@code null} if
     *         this row has not yet been processed by the settle loop.
     *         Mirrors {@code lode.settled_at} — see audit §5.2.
     */
    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    /** SUN-1295 snapshot: bet unit at purchase time. */
    public long getBetUnit() {
        return betUnit;
    }

    /** SUN-1295 snapshot: rate at purchase time (NOT live enum). */
    public int getRateAtPurchase() {
        return rateAtPurchase;
    }

    /** SUN-1295 snapshot: prize multiplier at purchase time (NOT live enum). */
    public int getPrizeMultiplier() {
        return prizeMultiplier;
    }

    /**
     * SUN-1339 B1: settle lifecycle state. Never null — defaults to
     * {@link SettleStatus#PENDING} for tickets constructed via the 12-arg ctor.
     */
    public SettleStatus getSettleStatus() {
        return settleStatus;
    }
}
