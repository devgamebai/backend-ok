package com.sunwinkr.minigame.engine.sicbo.core;

import com.sunwinkr.minigame.engine.core.RevealPhase;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboDiceGenerator;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboFundProtector;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPrizeCalculator;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboSettleResult;

import java.util.List;

/**
 * State-machine shell for a Sicbo round.
 *
 * <p>PR-3 adds the dice-generation + prize pipeline. Bet acceptance + pot
 * state are wired in PR-2; the boot path / module wiring lands in PR-4.
 *
 * <p>Round timing (SBM:417-477):
 * <ul>
 *   <li>Betting open: count 0-39 (40 seconds)</li>
 *   <li>Betting locked: count 40</li>
 *   <li>Finish flag: count 43</li>
 *   <li>Dice generation: count 44</li>
 *   <li>Prize payout (sync): count 48</li>
 *   <li>Bot reschedule: count 53</li>
 *   <li>New round: count 55</li>
 * </ul>
 *
 * <p>Phase machine: OPEN → LOCKED → REVEALED → SETTLED
 * Driven by {@code SicboModule.gameLoop} via {@code tick(int count)}.
 */
public class SicboRound {

    /** Current lifecycle phase. Starts OPEN at construction. */
    private volatile RevealPhase phase;

    /** Reference ID for this round (monotonic, shared VIN+XU rooms). */
    private final long referenceId;

    /**
     * Dice values produced by {@link #generateDicesLocked} — set once at
     * dice-generation time and consumed by {@link #calculatePrize}. Kept
     * package-private + volatile so the snapshot builder reads it safely
     * after the phase advances to REVEALED.
     */
    private volatile short[] pendingDice;

    /**
     * Absolute epoch-ms after which no new bets are accepted (SUN-1339 §A3).
     * Set to {@code System.currentTimeMillis() + BETTING_WINDOW_MS} when a
     * new round opens. Cleared to 0 when {@link #lockBetting()} is called.
     * Volatile so the HTTP-thread SicboBetService sees the lock atomically.
     */
    private volatile long bettingClosesAt;

    /**
     * Betting window duration in milliseconds. Matches the Sicbo round
     * timing: betting open count 0-39 (40 seconds), locked at count 40.
     */
    static final long BETTING_WINDOW_MS = 40_000L;

    /**
     * Construct a new Sicbo round in OPEN phase.
     *
     * @param referenceId monotonic round identifier from SicboReferenceIdStore
     */
    public SicboRound(long referenceId) {
        this.referenceId = referenceId;
        this.phase = RevealPhase.OPEN;
        this.bettingClosesAt = System.currentTimeMillis() + BETTING_WINDOW_MS;
    }

    /** Returns the current reveal phase. */
    public RevealPhase getPhase() {
        return phase;
    }

    /** Returns the reference ID for this round. */
    public long getReferenceId() {
        return referenceId;
    }

    /** Dice values produced for this round (or {@code null} pre-generation). */
    public short[] getPendingDice() {
        return pendingDice;
    }

    /**
     * Start a new round. Transitions phase to OPEN.
     * Full implementation: PR-2 (reset listResult, listUserBet, bettingRound,
     * enableBetting, startTime, totalValueBetUser per SBR:217-243).
     *
     * @param refId new reference ID
     */
    public void startNewRound(long refId) {
        // TODO(SUN-PR4): implement full SBR:217-243 init sequence
        this.phase = RevealPhase.OPEN;
        this.pendingDice = null;
        this.bettingClosesAt = System.currentTimeMillis() + BETTING_WINDOW_MS;
    }

    /**
     * Lock the betting window. Transitions phase OPEN → LOCKED.
     * Full implementation: PR-4 (sets enableBetting=false, bettingRound=false,
     * writes allow_betting_<refId>=0 to Hazelcast per SBR:310-322).
     */
    public void lockBetting() {
        // TODO(SUN-PR4): implement full SBR:310-322 disable-betting sequence
        this.phase = RevealPhase.LOCKED;
        this.bettingClosesAt = 0L; // window closed; timestamp-based guard sees 0
    }

    /**
     * Finish the round after prizes are distributed. Transitions phase → SETTLED.
     * Full implementation: PR-4 (clears resultTX, bettingRound=false, removes
     * Hazelcast keys per SBR:245-268).
     */
    public void finishRound() {
        // TODO(SUN-PR4): implement full SBR:245-268 finish sequence
        this.phase = RevealPhase.SETTLED;
    }

    /**
     * Epoch-ms deadline after which bets are rejected by the timestamp guard.
     * Returns 0 after {@link #lockBetting()} is called (SUN-1339 §A3).
     */
    public long bettingClosesAt() {
        return bettingClosesAt;
    }

    /**
     * Returns true when the betting window is open.
     *
     * <p>Mirrors {@code MGRoomSicbo.isBetting()} (SBR:1013-1015) which
     * returns the {@code bettingRound} boolean. In the extracted engine the
     * equivalent is {@code phase == RevealPhase.OPEN}.
     *
     * <p>Used by {@code SicboBetService.accept()} as the pre-check guard
     * and as the race re-check after wallet debit.
     */
    public boolean isBetting() {
        return this.phase == RevealPhase.OPEN;
    }

    /**
     * Run the result pipeline (RTP balancer → fund protector) and stash the
     * chosen dice in {@link #pendingDice}.
     *
     * <p>Mirrors {@code MGRoomSicbo.getResult} (SBR:598-630). The caller is
     * responsible for invoking this AFTER {@link #lockBetting()} — pot
     * snapshot is captured at entry to mitigate the race noted in PR-3 plan.
     *
     * @param pot       per-round bet accumulator (snapshot taken at entry)
     * @param generator RTP-aware dice generator (typically
     *                  {@code SicboRtpDiceGenerator})
     * @param protector bounded fund-protection fallback
     * @param fund      current house fund balance (vin units) — passed to
     *                  the protector as {@code fundTaiXiu}
     * @return chosen dice array (length 3)
     * @throws IllegalStateException if called outside LOCKED phase
     */
    public short[] generateDicesLocked(SicboPotState pot,
                                       SicboDiceGenerator generator,
                                       SicboFundProtector protector,
                                       long fund) {
        if (this.phase != RevealPhase.LOCKED) {
            throw new IllegalStateException("generateDicesLocked requires LOCKED phase, was " + phase);
        }
        // SBR:735 — snapshot the bet list once at result-time. listUserBet
        // mutations after this point cannot influence the chosen dice.
        List<SicboPotEntry> snapshot = pot.listUserBet();
        long totalValueBetUser = pot.totalValueBetUser();

        short[] candidate = generator.generate(this, snapshot, totalValueBetUser);
        SicboFundProtector.Result protect = protector.protect(this, snapshot, totalValueBetUser, fund, candidate);
        this.pendingDice = protect.dice;
        return protect.dice;
    }

    /**
     * Compute the prize/fee/refund breakdown using {@link #pendingDice}.
     *
     * <p>Mirrors {@code MGRoomSicbo.reward()} (SBR:1064-1119). The settle
     * result is consumed by the adapter to drive {@code WalletPort.credit}
     * + Mongo writes; this method itself is side-effect free.
     *
     * @param pot              per-round bet accumulator (snapshot used)
     * @param prizeCalculator  prize calculator (typically reused across rounds)
     * @return settle result ready for the adapter
     */
    public SicboSettleResult calculatePrize(SicboPotState pot, SicboPrizeCalculator prizeCalculator) {
        return prizeCalculator.calculate(pot.listUserBet(), this.pendingDice);
    }
}
