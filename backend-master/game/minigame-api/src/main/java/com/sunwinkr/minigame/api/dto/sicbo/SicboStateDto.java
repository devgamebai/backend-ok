package com.sunwinkr.minigame.api.dto.sicbo;

import com.sunwinkr.minigame.engine.sicbo.snapshot.SicboSnapshot;

/**
 * Wire DTO for {@code GET /api/v2/sicbo/state} and STOMP tick payloads.
 *
 * <p>Pre-reveal censoring contract: {@code dice1/2/3 == null} until the
 * engine round transitions into REVEALED or SETTLED. Censoring is enforced
 * inside {@link SicboSnapshot#isDiceVisible()}; this DTO mirrors it.
 *
 * <p>Sicbo carries the full 52-bet-type breakdown via {@code potBySide}
 * in a follow-up wave; the PR-4 baseline exposes the binary TAI/XIU
 * aggregate (matching the legacy client wire) plus a placeholder list.
 *
 * <p>SUN-1339 §A3 provider-contract additions:
 * <ul>
 *   <li>{@link #safeBetExpiresAt} — absolute epoch-ms bet deadline (maps to
 *       engine's {@code bettingClosesAt}). FE computes countdown via
 *       {@code safeBetExpiresAt - Date.now()}.</li>
 *   <li>{@link #roundId} — current round identifier for grouping bets on settle.</li>
 * </ul>
 * Legacy fields ({@link #referenceId}, {@link #bettingState}, {@link #remainTime})
 * are preserved for FE backwards-compatibility.
 */
public final class SicboStateDto {

    public long referenceId;

    /**
     * Absolute epoch-ms after which the server rejects bets with errorCode
     * {@code "0007"} (BET_WINDOW_CLOSED). Provider-contract field (SUN-1339 §A3).
     * Maps to {@code SicboSnapshot#bettingClosesAt}.
     */
    public long safeBetExpiresAt;

    /**
     * Current round identifier. Monotonically increasing. Provider-contract
     * field (SUN-1339 §A3). Same value as {@link #referenceId} — both emitted
     * so providers and legacy FE have unambiguous access.
     */
    public long roundId;

    public int remainTime;
    public boolean bettingState;

    public long potTai;
    public long potXiu;

    public long myBetTai;
    public long myBetXiu;

    public long jpTai;
    public long jpXiu;

    /** {@code null} when phase is not REVEALED / SETTLED (censored). */
    public Short dice1;
    public Short dice2;
    public Short dice3;

    /** Reveal phase name (OPEN / LOCKED / REVEALED / SETTLED). */
    public String phase;

    public SicboStateDto() {
    }

    public static SicboStateDto fromSnapshot(SicboSnapshot s) {
        SicboStateDto d = new SicboStateDto();
        if (s == null) {
            return d;
        }
        d.referenceId      = s.referenceId;
        d.roundId          = s.roundId;
        d.safeBetExpiresAt = s.bettingClosesAt;
        d.remainTime       = s.remainTime;
        d.bettingState     = s.bettingState;
        d.potTai = s.potTai;
        d.potXiu = s.potXiu;
        d.myBetTai = s.myBetTai;
        d.myBetXiu = s.myBetXiu;
        d.jpTai = s.jpTai;
        d.jpXiu = s.jpXiu;
        d.dice1 = s.dice1;
        d.dice2 = s.dice2;
        d.dice3 = s.dice3;
        d.phase = s.phase == null ? null : s.phase.name();
        return d;
    }
}
