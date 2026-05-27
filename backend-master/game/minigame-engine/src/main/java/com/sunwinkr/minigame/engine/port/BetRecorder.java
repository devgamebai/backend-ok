package com.sunwinkr.minigame.engine.port;

/**
 * Engine-facing port for persisting per-bet history rows (Mongo
 * {@code user_bet_tai_xiu} per TXR:324, Sicbo {@code user_bet_tai_xiu_sicbo}
 * per SBR:435-446).
 *
 * <p>Two callable forms — both with default implementations that
 * delegate into the other. Concrete adapters override the form that
 * matches their persistence layer; tests may override either.
 *
 * <p>Plan §2.2 row B9 / spec INV-20.
 */
public interface BetRecorder {

    /**
     * Persist one bet-history row (TaiXiu canonical form). Default
     * forwards to {@link #record}.
     */
    default void recordBet(BetRecord r) {
        if (r == null) {
            throw new NullPointerException("BetRecord");
        }
        record(r.refId, r.nickname, r.betValue,
            (int) r.inputTime, (int) r.betSide, (int) r.moneyType);
    }

    /**
     * Sicbo PR-3 flat-args record. Default forwards to
     * {@link #recordBet}.
     */
    default void record(long referenceId, String nickname, long betValue,
                        int inputTime, int betSideId, int moneyType) {
        recordBet(new BetRecord(referenceId, nickname,
            (short) inputTime, (short) betSideId, betValue, 0L, (short) moneyType));
    }

    /**
     * Per-bet history payload. Mirrors the Mongo document fields
     * documented in rules-spec §6 (table {@code user_bet_tai_xiu}).
     */
    final class BetRecord {

        public final long refId;
        public final String nickname;
        public final short inputTime;
        public final short betSide;
        public final long betValue;
        public final long balance;
        public final short moneyType;

        public BetRecord(long refId,
                         String nickname,
                         short inputTime,
                         short betSide,
                         long betValue,
                         long balance,
                         short moneyType) {
            if (nickname == null) {
                throw new NullPointerException("nickname");
            }
            this.refId = refId;
            this.nickname = nickname;
            this.inputTime = inputTime;
            this.betSide = betSide;
            this.betValue = betValue;
            this.balance = balance;
            this.moneyType = moneyType;
        }
    }
}
