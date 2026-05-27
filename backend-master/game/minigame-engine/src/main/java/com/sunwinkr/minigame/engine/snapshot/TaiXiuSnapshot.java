package com.sunwinkr.minigame.engine.snapshot;

/**
 * Client-facing snapshot of a TaiXiu round. Built by
 * {@code TaiXiuRound#snapshotForClient(String)} once per push-tick.
 *
 * <p>Field layout matches the wire shape implied by
 * {@code docs/plans/taixiu-extraction-plan.md §3.3} + §5.2 {@code StateDto}.
 * Per-user fields ({@link #myBetTai}, {@link #myBetXiu}) are populated
 * from the supplied username.
 *
 * <p>Censoring contract: when
 * {@link com.sunwinkr.minigame.engine.core.RevealPhase#diceVisible()}
 * is false on the producing round, {@link #dice1}, {@link #dice2},
 * {@link #dice3} are zero and {@link #result} is {@code -1}. Property
 * test {@code NoDiceInSnapshotPreRevealTest} pins this invariant.
 *
 * <p>Mutability: built once per snapshot, treated as immutable by
 * consumers. Public fields chosen over getters to mirror the
 * existing BitZero {@code BaseMsg} convention so the future
 * {@code TaiXiuModuleBridge} can copy fields directly.
 */
public final class TaiXiuSnapshot {

    /** Monotonic round reference id (INV-1). */
    public long referenceId;

    /** Remaining seconds in the current phase mapping (1Hz). */
    public short remainTime;

    /** True iff round is in {@link com.sunwinkr.minigame.engine.core.RevealPhase#OPEN}. */
    public boolean bettingState;

    public long potTai;
    public long potXiu;

    public long myBetTai;
    public long myBetXiu;

    /** Jackpot pool, mirrored per side for legacy wire compat. */
    public long jpTai;
    public long jpXiu;

    /** Dice values when {@link #result} != -1; else 0. */
    public short dice1;
    public short dice2;
    public short dice3;

    /**
     * 0 = XIU, 1 = TAI, -1 = pre-reveal (dice not visible).
     */
    public short result;

    /** Bot+real bet counts (advisory; engine fills 0 in PR-1). */
    public short numBetTai;
    public short numBetXiu;

    /** Real (non-bot) bet counts (advisory; engine fills 0 in PR-1). */
    public short realNumBetTai;
    public short realNumBetXiu;

    /**
     * Absolute epoch-ms deadline after which no bets are accepted.
     * Provider-contract field (SUN-1339 §A2): FE uses this for its own
     * countdown rather than trusting the local clock. Set when the round
     * enters OPEN; cleared to 0 when betting locks.
     */
    public long bettingClosesAt;

    /**
     * Monotonic round identifier — same as {@link #referenceId} but named
     * to match the provider-contract field (SUN-1339 §A2). Both fields are
     * emitted so providers and legacy FE have unambiguous access.
     */
    public long roundId;

    public TaiXiuSnapshot() {
        // default field values
    }
}
