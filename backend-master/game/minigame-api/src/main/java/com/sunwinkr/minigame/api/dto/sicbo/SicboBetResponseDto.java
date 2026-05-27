package com.sunwinkr.minigame.api.dto.sicbo;

/**
 * Wire DTO for {@code POST /api/v2/sicbo/bet} success/error response.
 *
 * <p>Error codes match the engine {@code SicboBetAcceptResult} encoding:
 * {@code 0}=OK, {@code 1}=wallet/race, {@code 2}=closed, {@code 3}=balance,
 * {@code 4}=below min, {@code 5}=cross-side (PRESERVED-DEAD-CODE),
 * {@code 6}=invalid bet side name (AMBIGUOUS #6).
 * Surface error codes: {@code 0401}=unauthorized, {@code 0429}=rate limit.
 */
public final class SicboBetResponseDto {

    public boolean success;
    public String errorCode;
    public long currentMoney;
    public long perBetTxId;
    public String transactionCode;
    public Integer betSideId;
    public String message;

    public SicboBetResponseDto() {
    }

    public static SicboBetResponseDto ok(long currentMoney, long perBetTxId,
                                          String transactionCode, int betSideId) {
        SicboBetResponseDto d = new SicboBetResponseDto();
        d.success = true;
        d.errorCode = "0000";
        d.currentMoney = currentMoney;
        d.perBetTxId = perBetTxId;
        d.transactionCode = transactionCode;
        d.betSideId = betSideId;
        d.message = "OK";
        return d;
    }

    public static SicboBetResponseDto error(int engineCode, long currentMoney, String message) {
        SicboBetResponseDto d = new SicboBetResponseDto();
        d.success = false;
        d.errorCode = String.format("%04d", engineCode);
        d.currentMoney = currentMoney;
        d.perBetTxId = 0L;
        d.transactionCode = null;
        d.betSideId = null;
        d.message = message;
        return d;
    }
}
