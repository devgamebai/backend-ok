package com.sunwinkr.minigame.engine.sicbo.bet;

/**
 * Immutable record of a single accepted bet stored in {@link SicboPotState}.
 *
 * <p>Maps to the {@code TransactionTaiXiuDetail} fields used by the Sicbo
 * reward path (SBR:1170-1255). Kept minimal for PR-2 scope — prize-calc
 * fields (transactionCode, currentMoney) are populated but the payout
 * result fields land in PR-3.
 */
public final class SicboPotEntry {

    public final String nickname;
    public final int userId;
    public final long betValue;
    public final int betSideId;
    public final short inputTime;
    public final short moneyType;
    public final long perBetTxId;
    public final String transactionCode;
    /** {@code true} when this entry was placed by a bot player. */
    public final boolean isBot;
    /** Post-debit wallet balance at the time the bet was accepted. */
    public final long currentMoney;

    public SicboPotEntry(String nickname, int userId, long betValue, int betSideId,
                         short inputTime, short moneyType, long perBetTxId,
                         String transactionCode, boolean isBot, long currentMoney) {
        this.nickname        = nickname;
        this.userId          = userId;
        this.betValue        = betValue;
        this.betSideId       = betSideId;
        this.inputTime       = inputTime;
        this.moneyType       = moneyType;
        this.perBetTxId      = perBetTxId;
        this.transactionCode = transactionCode;
        this.isBot           = isBot;
        this.currentMoney    = currentMoney;
    }

    @Override
    public String toString() {
        return "SicboPotEntry{nickname='" + nickname + "', betValue=" + betValue
            + ", betSideId=" + betSideId + ", isBot=" + isBot + "}";
    }
}
