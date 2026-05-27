package com.sunwinkr.minigame.engine.bet;

/**
 * Engine-side pot contributor record. Mirrors the load-bearing fields of
 * the legacy {@code com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail}
 * (TXR:411) but is a pure-Java value type — no DAL/Mongo coupling.
 *
 * <p>PR-2 scope: enough fields to support pot accumulation, cross-side
 * detection, and the wire-up to {@link BetAcceptor}. Prize/refund/jp
 * fields land in PR-3 alongside {@code PrizeCalculator}.
 *
 * <p>Plan §2.2 row B8 / spec INV-6, INV-20.
 */
public final class TransactionTaiXiuDetail {

    public final long referenceId;
    public final int userId;
    public final String username;
    public final long betValue;
    public final int betSide;   // 0 = XIU, 1 = TAI
    public final int inputTime; // seconds remaining at bet time
    public final int moneyType; // 0 = xu, 1 = vin

    /** Captured at bet time for downstream history rows. */
    public final long currentMoney;

    /** Per-bet unique id from {@link TxIdGenerator#nextBetTxId(long)}. */
    public final long perBetTxId;

    public TransactionTaiXiuDetail(long referenceId,
                                   int userId,
                                   String username,
                                   long betValue,
                                   int betSide,
                                   int inputTime,
                                   int moneyType,
                                   long currentMoney,
                                   long perBetTxId) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        this.referenceId = referenceId;
        this.userId = userId;
        this.username = username;
        this.betValue = betValue;
        this.betSide = betSide;
        this.inputTime = inputTime;
        this.moneyType = moneyType;
        this.currentMoney = currentMoney;
        this.perBetTxId = perBetTxId;
    }
}
