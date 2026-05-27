package com.sunwinkr.lottery.engine.settle;

/**
 * Emitted by {@link LotterySettleService} when a single ticket's settle
 * step fails — the loop continues, this carries the failure context
 * out for alerting / retry telemetry. Adapter in PR-3 wires Telegram
 * + structured log.
 */
public final class SettleFailureEvent {

    private final long ticketId;
    private final String nickname;
    private final String reason;

    public SettleFailureEvent(long ticketId, String nickname, String reason) {
        this.ticketId = ticketId;
        this.nickname = nickname;
        this.reason = reason;
    }

    public long getTicketId() {
        return ticketId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getReason() {
        return reason;
    }
}
