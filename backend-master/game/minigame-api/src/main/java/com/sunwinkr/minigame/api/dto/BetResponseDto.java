package com.sunwinkr.minigame.api.dto;

/**
 * Wire DTO for {@code POST /api/v2/taixiu/bet} success/error response.
 *
 * <p>Error codes match the legacy {@code BetTaiXiuMsg.Error} encoding:
 * {@code 0}=OK, {@code 1}=wallet/race, {@code 2}=closed,
 * {@code 3}=balance, {@code 4}=below min, {@code 5}=cross-side.
 * Surface error codes:
 * {@code 0401}=unauthorized, {@code 0429}=rate limit.
 */
public final class BetResponseDto {

    public boolean success;
    public String errorCode;
    public long currentMoney;
    public long perBetTxId;
    public String message;

    public BetResponseDto() {
    }

    public static BetResponseDto ok(long currentMoney, long perBetTxId) {
        BetResponseDto d = new BetResponseDto();
        d.success = true;
        d.errorCode = "0000";
        d.currentMoney = currentMoney;
        d.perBetTxId = perBetTxId;
        d.message = "OK";
        return d;
    }

    public static BetResponseDto error(int engineCode, long currentMoney, String message) {
        BetResponseDto d = new BetResponseDto();
        d.success = false;
        d.errorCode = String.format("%04d", engineCode);
        d.currentMoney = currentMoney;
        d.perBetTxId = 0L;
        d.message = message;
        return d;
    }
}
