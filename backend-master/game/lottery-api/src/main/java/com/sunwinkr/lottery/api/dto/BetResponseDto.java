package com.sunwinkr.lottery.api.dto;

/**
 * Wire DTO for the bet response. Plan §5.2.
 *
 * <p>{@code errorCode} matches the engine codes per plan §2.3 B1:
 * {@code 0000}=OK, {@code 0001}=wallet rejected, {@code 0002}=locked,
 * {@code 0003}=insufficient funds, {@code 0004}=unknown mode,
 * {@code 0005}=invalid number.
 */
public final class BetResponseDto {

    public boolean success;
    public String errorCode;
    public long currentMoney;
    public Long ticketId;
    public String message;

    public BetResponseDto() {
    }

    public static BetResponseDto ok(long currentMoney, Long ticketId) {
        BetResponseDto d = new BetResponseDto();
        d.success = true;
        d.errorCode = "0000";
        d.currentMoney = currentMoney;
        d.ticketId = ticketId;
        d.message = "OK";
        return d;
    }

    public static BetResponseDto error(String errorCode, long currentMoney, String message) {
        BetResponseDto d = new BetResponseDto();
        d.success = false;
        d.errorCode = errorCode;
        d.currentMoney = currentMoney;
        d.ticketId = null;
        d.message = message;
        return d;
    }
}
