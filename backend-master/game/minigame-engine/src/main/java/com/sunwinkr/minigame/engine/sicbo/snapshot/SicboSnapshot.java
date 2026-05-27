package com.sunwinkr.minigame.engine.sicbo.snapshot;

import com.sunwinkr.minigame.engine.core.RevealPhase;

/**
 * Immutable client-facing snapshot of a Sicbo round.
 *
 * <p>Mirrors {@code SicboInfoMsg} field list (SBR:991-1011).
 * Replaces {@code MGRoomSicbo.updateTaiXiuInfo(User)} for the new engine path.
 *
 * <h3>Dice censoring rule (§3, Snapshot censoring)</h3>
 * {@code dice1/dice2/dice3} are null (not set) when
 * {@code phase ∉ {REVEALED, SETTLED}}. This prevents early reveal
 * of the dice result to clients polling during the betting window.
 * All client-facing serialization flows through this snapshot.
 *
 * <p>DTO — no behavior; all fields public for fast serialization.
 */
public final class SicboSnapshot {

    /** Always present. */
    public final short gameId;
    public final short moneyType;
    public final long referenceId;

    /**
     * Absolute epoch-ms deadline after which no bets are accepted.
     * Provider-contract field (SUN-1339 §A3): FE uses this for its own
     * countdown rather than trusting the local clock. Set when the round
     * enters OPEN; cleared to 0 when betting locks.
     */
    public final long bettingClosesAt;

    /**
     * Monotonic round identifier — same as {@link #referenceId} but named
     * to match the provider-contract field (SUN-1339 §A3). Both fields are
     * emitted so providers and legacy FE have unambiguous access.
     */
    public final long roundId;

    public final int remainTime;
    public final boolean bettingState;
    public final long potTai;
    public final long potXiu;
    public final long myBetTai;
    public final long myBetXiu;
    public final long jpTai;
    public final long jpXiu;

    /**
     * Dice values 1-6, or null if phase is not REVEALED or SETTLED.
     * Null = dice not yet revealed to client.
     */
    public final Short dice1;
    public final Short dice2;
    public final Short dice3;

    /** The reveal phase at snapshot time. */
    public final RevealPhase phase;

    // -----------------------------------------------------------------------

    /**
     * Full constructor. Callers should use the builder pattern below.
     * Dice values censored automatically based on phase.
     */
    private SicboSnapshot(
            short gameId,
            short moneyType,
            long referenceId,
            long bettingClosesAt,
            long roundId,
            int remainTime,
            boolean bettingState,
            long potTai,
            long potXiu,
            long myBetTai,
            long myBetXiu,
            long jpTai,
            long jpXiu,
            Short rawDice1,
            Short rawDice2,
            Short rawDice3,
            RevealPhase phase) {
        this.gameId = gameId;
        this.moneyType = moneyType;
        this.referenceId = referenceId;
        this.bettingClosesAt = bettingClosesAt;
        this.roundId = roundId;
        this.remainTime = remainTime;
        this.bettingState = bettingState;
        this.potTai = potTai;
        this.potXiu = potXiu;
        this.myBetTai = myBetTai;
        this.myBetXiu = myBetXiu;
        this.jpTai = jpTai;
        this.jpXiu = jpXiu;
        this.phase = phase;

        // Censor dice when phase is not REVEALED or SETTLED
        boolean diceVisible = (phase == RevealPhase.REVEALED || phase == RevealPhase.SETTLED);
        this.dice1 = diceVisible ? rawDice1 : null;
        this.dice2 = diceVisible ? rawDice2 : null;
        this.dice3 = diceVisible ? rawDice3 : null;
    }

    /**
     * Creates a snapshot for a client in the given phase.
     * Pass raw dice values; this constructor censors them if phase is not REVEALED/SETTLED.
     */
    public static SicboSnapshot of(
            short gameId,
            short moneyType,
            long referenceId,
            int remainTime,
            boolean bettingState,
            long potTai,
            long potXiu,
            long myBetTai,
            long myBetXiu,
            long jpTai,
            long jpXiu,
            Short rawDice1,
            Short rawDice2,
            Short rawDice3,
            RevealPhase phase) {
        return new SicboSnapshot(
                gameId, moneyType, referenceId,
                /*bettingClosesAt*/ 0L, /*roundId*/ referenceId,
                remainTime, bettingState,
                potTai, potXiu, myBetTai, myBetXiu, jpTai, jpXiu,
                rawDice1, rawDice2, rawDice3, phase);
    }

    /**
     * Creates a snapshot including provider-contract fields (SUN-1339 §A3).
     * Preferred form for new callers.
     */
    public static SicboSnapshot of(
            short gameId,
            short moneyType,
            long referenceId,
            long bettingClosesAt,
            long roundId,
            int remainTime,
            boolean bettingState,
            long potTai,
            long potXiu,
            long myBetTai,
            long myBetXiu,
            long jpTai,
            long jpXiu,
            Short rawDice1,
            Short rawDice2,
            Short rawDice3,
            RevealPhase phase) {
        return new SicboSnapshot(
                gameId, moneyType, referenceId,
                bettingClosesAt, roundId,
                remainTime, bettingState,
                potTai, potXiu, myBetTai, myBetXiu, jpTai, jpXiu,
                rawDice1, rawDice2, rawDice3, phase);
    }

    /**
     * Returns true if dice are visible to the client (phase is REVEALED or SETTLED).
     */
    public boolean isDiceVisible() {
        return phase == RevealPhase.REVEALED || phase == RevealPhase.SETTLED;
    }
}
