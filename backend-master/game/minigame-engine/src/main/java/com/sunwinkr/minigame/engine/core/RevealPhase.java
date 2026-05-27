package com.sunwinkr.minigame.engine.core;

/**
 * Reveal phase of a TaiXiu / Sicbo round.
 *
 * <p>State machine (one-way, monotonic per round):
 * <pre>
 *   OPEN -> LOCKED -> GENERATING -> REVEALED -> SETTLED -> CLEANUP -> OPEN
 * </pre>
 *
 * <p>The split between {@link #GENERATING} and {@link #REVEALED} is the
 * reveal-hardening contract from
 * {@code docs/specs/taixiu-sicbo-anticheat-audit.md §5}: dice values are
 * generated into {@code pendingDice} during {@code GENERATING} and only
 * published into {@code revealedDice} on entry to {@code REVEALED}. The
 * snapshot-builder censors dice in every phase except
 * {@code REVEALED} / {@code SETTLED}.
 *
 * <p>Source mapping (PR-1 scope — pure phase enum; transitions land in
 * {@link RevealClock}):
 * <ul>
 *   <li>{@code OPEN}      ← TaiXiuModule.gameLoop count 0..44 (TXM:425-431)</li>
 *   <li>{@code LOCKED}    ← TaiXiuModule.gameLoop count 45..50 (TXM:436-447)</li>
 *   <li>{@code GENERATING}← TaiXiuModule.gameLoop count 51 (TXM:449-452)</li>
 *   <li>{@code REVEALED}  ← +1 tick after generation (NEW — hardening §3.2)</li>
 *   <li>{@code SETTLED}   ← TaiXiuModule.gameLoop count 56 (TXM:454-457)</li>
 *   <li>{@code CLEANUP}   ← TaiXiuModule.gameLoop count 60..67 (TXM:458-462)</li>
 * </ul>
 */
public enum RevealPhase {
    OPEN,
    LOCKED,
    GENERATING,
    REVEALED,
    SETTLED,
    CLEANUP;

    /** True when the client snapshot is allowed to expose dice/result. */
    public boolean diceVisible() {
        return this == REVEALED || this == SETTLED;
    }

    /** True when bets are accepted. */
    public boolean acceptsBets() {
        return this == OPEN;
    }

    /**
     * Verifies a legal forward transition.
     *
     * @throws IllegalStateException if the transition is not in the legal cycle
     */
    public static void requireLegalTransition(RevealPhase from, RevealPhase to) {
        boolean legal;
        switch (from) {
            case OPEN:       legal = (to == LOCKED);     break;
            case LOCKED:     legal = (to == GENERATING); break;
            case GENERATING: legal = (to == REVEALED);   break;
            case REVEALED:   legal = (to == SETTLED);    break;
            case SETTLED:    legal = (to == CLEANUP);    break;
            case CLEANUP:    legal = (to == OPEN);       break;
            default:         legal = false;
        }
        if (!legal) {
            throw new IllegalStateException(
                "Illegal RevealPhase transition: " + from + " -> " + to);
        }
    }
}
