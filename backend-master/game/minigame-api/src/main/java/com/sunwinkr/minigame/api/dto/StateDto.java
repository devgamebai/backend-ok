package com.sunwinkr.minigame.api.dto;

import com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot;

/**
 * Wire DTO for {@code GET /api/v2/taixiu/state} and STOMP tick payloads.
 *
 * <p>JDK 8 target — uses a plain final class with public fields rather
 * than a record. Field layout matches plan §5.2.
 *
 * <p>Pre-reveal censoring contract: {@code dice1/2/3 == 0} and
 * {@code result == -1} until the engine round transitions into REVEALED.
 *
 * <p>SUN-1339 §A2 provider-contract additions:
 * <ul>
 *   <li>{@link #safeBetExpiresAt} — absolute epoch-ms bet deadline (maps to engine's
 *       {@code bettingClosesAt}). FE computes countdown via
 *       {@code safeBetExpiresAt - Date.now()}.</li>
 *   <li>{@link #roundId} — current round identifier for grouping bets on settle.</li>
 * </ul>
 * Legacy fields ({@link #referenceId}, {@link #bettingState}, {@link #remainTime})
 * are preserved for FE backwards-compatibility.
 */
public final class StateDto {

    public long referenceId;
    public short remainTime;
    public boolean bettingState;

    /**
     * Absolute epoch-ms after which the server rejects bets with errorCode
     * {@code "0007"} (BET_WINDOW_CLOSED). Provider-contract field (SUN-1339 §A2).
     * Maps to {@code TaiXiuSnapshot#bettingClosesAt}.
     */
    public long safeBetExpiresAt;

    /**
     * Current round identifier. Monotonically increasing. Provider-contract
     * field (SUN-1339 §A2). Same value as {@link #referenceId} — both emitted
     * so providers and legacy FE have unambiguous access.
     */
    public long roundId;

    public long potTai;
    public long potXiu;

    public long myBetTai;
    public long myBetXiu;

    public long jpTai;
    public long jpXiu;

    public short dice1;
    public short dice2;
    public short dice3;
    public short result;

    public short numBetTai;
    public short numBetXiu;
    public short realNumBetTai;
    public short realNumBetXiu;

    public StateDto() {
        // default; populated by builders below
    }

    public static StateDto fromSnapshot(TaiXiuSnapshot s) {
        StateDto d = new StateDto();
        if (s == null) {
            return d;
        }
        d.referenceId    = s.referenceId;
        d.roundId        = s.roundId;
        d.safeBetExpiresAt = s.bettingClosesAt;
        d.remainTime     = s.remainTime;
        d.bettingState   = s.bettingState;
        d.potTai = s.potTai;
        d.potXiu = s.potXiu;
        d.myBetTai = s.myBetTai;
        d.myBetXiu = s.myBetXiu;
        d.jpTai = s.jpTai;
        d.jpXiu = s.jpXiu;
        d.dice1 = s.dice1;
        d.dice2 = s.dice2;
        d.dice3 = s.dice3;
        d.result = s.result;
        d.numBetTai = s.numBetTai;
        d.numBetXiu = s.numBetXiu;
        d.realNumBetTai = s.realNumBetTai;
        d.realNumBetXiu = s.realNumBetXiu;
        return d;
    }
}
