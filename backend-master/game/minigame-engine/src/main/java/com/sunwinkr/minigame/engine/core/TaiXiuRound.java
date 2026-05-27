package com.sunwinkr.minigame.engine.core;

import com.sunwinkr.minigame.engine.port.CachePort;
import com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State machine for one logical TaiXiu round (covers both the VIN and XU
 * rooms which share a {@code referenceId}). PR-1 scope: lifecycle + reveal
 * censoring only. Bet acceptance, dice generation, prize calc, jackpot,
 * and bots land in later PRs and plug in as collaborators via the
 * {@code port/} interfaces.
 *
 * <p>Lifecycle method mapping per
 * {@code docs/plans/taixiu-extraction-plan.md §2.1 L1-L5}:
 * <ul>
 *   <li>{@link #tick(long)}            ← {@code TaiXiuModule.gameLoop}    (L1)</li>
 *   <li>{@link #startNewRound(long)}   ← {@code TaiXiuModule.startNewRoundTX} (L2)</li>
 *   <li>{@link #resetForNewRound(long)}← {@code MGRoomTaiXiu.startNewGame}    (L3)</li>
 *   <li>{@link #lockBetting()}         ← {@code MGRoomTaiXiu.disableBetting}  (L4)</li>
 *   <li>{@link #finishRound()}         ← {@code MGRoomTaiXiu.finish}          (L5)</li>
 * </ul>
 *
 * <p>Reveal hardening (plan §3): dice are written into
 * {@link #pendingDice} during {@link RevealPhase#GENERATING} but only
 * published to {@link #revealedDice} on entry to
 * {@link RevealPhase#REVEALED}. The snapshot censors pre-reveal phases.
 *
 * <h3>Thread-safety</h3>
 * <p>The {@code phase}, {@code pendingDice}, {@code revealedDice}, and
 * {@code referenceId} fields are {@code volatile} so that the snapshot
 * builder, which runs on the broadcast/STOMP thread, observes a happens-
 * before relationship with the gameLoop thread's writes. State
 * <em>transitions</em> are not synchronized — by contract, all mutations
 * happen from a single gameLoop thread, matching BitZero's
 * {@code taskScheduler.scheduleAtFixedRate(..., 1, SECONDS)} (TXM:156).
 * Concurrent {@link #tick(long)} calls are not supported.
 */
public final class TaiXiuRound {

    private static final Logger LOG = LoggerFactory.getLogger(TaiXiuRound.class);

    private final RevealClock clock;
    private final CachePort cache;

    private volatile long referenceId;
    private volatile RevealPhase phase = RevealPhase.OPEN;

    private volatile short[] pendingDice;
    private volatile short[] revealedDice;

    /**
     * Wall-clock start of the active round in ms — used by remainTime
     * calculations in later PRs. Per plan §2.1 L5, {@link #finishRound()}
     * must NOT reset this (SUN-1245).
     */
    private volatile long startTimeMs;

    /**
     * Absolute epoch-ms after which no new bets are accepted (SUN-1339 §A2).
     * Set to {@code startTimeMs + BETTING_WINDOW_MS} when a new round opens.
     * Cleared to 0 when {@link #lockBetting()} is called. Volatile so the
     * HTTP-thread BetAcceptor sees the lock atomically.
     */
    private volatile long bettingClosesAt;

    /**
     * Betting window duration in milliseconds. The RevealClock locks at
     * count=45 (45s into the round), matching the plan §3.2 LOCK_BETTING event.
     */
    static final long BETTING_WINDOW_MS = 45_000L;

    /**
     * Last tick timestamp passed to {@link #tick(long)} — fed to the
     * watchdog (plan §2.1 L9; not in PR-1 scope).
     */
    private volatile long lastTickMs;

    /**
     * @param clock reveal clock; pass {@link RevealClock.SimpleRevealClock}
     *              or a stub for tests
     * @param cache adapter to publish {@code allow_betting_*} mirror writes;
     *              may be a no-op for tests
     */
    public TaiXiuRound(RevealClock clock, CachePort cache) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        if (cache == null) {
            throw new NullPointerException("cache");
        }
        this.clock = clock;
        this.cache = cache;
        this.referenceId = 1L; // matches TaiXiuModule.loadData default
    }

    /* ------------------------------------------------------------------ *
     * L2 — startNewRound: refId++ BEFORE broadcast (INV-1).
     * ------------------------------------------------------------------ */

    /**
     * Begin a new round at the supplied reference id. Increments
     * monotonically — calling with a refId &lt; the current id throws.
     *
     * <p>Mirrors {@code TaiXiuModule.startNewRoundTX} (TXM:339-374).
     *
     * @param newRefId next reference id (must be &gt; current)
     * @throws IllegalArgumentException if {@code newRefId} is not strictly increasing
     */
    public void startNewRound(long newRefId) {
        if (newRefId <= this.referenceId) {
            throw new IllegalArgumentException(
                "refId must be strictly increasing: current=" + this.referenceId
                    + " supplied=" + newRefId);
        }
        this.referenceId = newRefId;
        resetForNewRound(newRefId);
        try {
            cache.setCurrentReference(newRefId);
            cache.setAllowBetting(newRefId, true);
        } catch (Throwable t) {
            // SUN-1xxx: per-call try/catch — cache failures must NEVER break the round
            LOG.warn("CachePort write failed on startNewRound refId=" + newRefId, t);
        }
    }

    /* ------------------------------------------------------------------ *
     * L3 — resetForNewRound: clear per-round state.
     * ------------------------------------------------------------------ */

    /**
     * Reset per-round mutable state without changing {@link #referenceId}.
     * Used both by {@link #startNewRound(long)} and by external callers
     * that need to re-seed the round in shadow-replay (later PRs).
     *
     * <p>Mirrors {@code MGRoomTaiXiu.startNewGame} (TXR:171-206).
     *
     * @param refId expected reference id — must match the current
     *              {@link #referenceId}; supplied for symmetry with the
     *              legacy signature and to catch stale callers in tests.
     */
    public void resetForNewRound(long refId) {
        if (refId != this.referenceId) {
            throw new IllegalArgumentException(
                "resetForNewRound refId mismatch: current=" + this.referenceId
                    + " supplied=" + refId);
        }
        this.phase = RevealPhase.OPEN;
        this.pendingDice = null;
        this.revealedDice = null;
        this.startTimeMs = System.currentTimeMillis();
        this.bettingClosesAt = this.startTimeMs + BETTING_WINDOW_MS;
        this.clock.resetForNewRound();
        // Pot reset (INV-16) lands in PR-2 when PotState is introduced.
    }

    /* ------------------------------------------------------------------ *
     * L4 — lockBetting: phase=LOCKED + cache mirror.
     * ------------------------------------------------------------------ */

    /**
     * Lock the bet window. Idempotent — repeated calls after LOCKED are
     * no-ops because the legal-transition check rejects LOCKED→LOCKED.
     *
     * <p>Mirrors {@code MGRoomTaiXiu.disableBetting} (TXR:251-254).
     */
    public void lockBetting() {
        if (phase == RevealPhase.LOCKED) {
            return; // idempotent
        }
        advance(RevealPhase.LOCKED);
        this.bettingClosesAt = 0L; // window closed; timestamp-based guard sees 0
        try {
            cache.setAllowBetting(referenceId, false);
        } catch (Throwable t) {
            LOG.warn("CachePort.setAllowBetting(false) failed for refId=" + referenceId, t);
        }
    }

    /* ------------------------------------------------------------------ *
     * L5 — finishRound: clean Hazelcast keys; do NOT touch startTime.
     * ------------------------------------------------------------------ */

    /**
     * Mark the round finished. Removes the advisory cache key. The
     * {@link #startTimeMs} field is intentionally untouched (SUN-1245 —
     * later remainTime calculations rely on the same anchor for the
     * SETTLED-&gt;CLEANUP window).
     *
     * <p>Mirrors {@code MGRoomTaiXiu.finish} (TXR:208-225).
     */
    public void finishRound() {
        try {
            cache.removeAllowBetting(referenceId);
        } catch (Throwable t) {
            LOG.warn("CachePort.removeAllowBetting failed for refId=" + referenceId, t);
        }
        // No phase transition here — finish() is a side-effect at count=50
        // while still in LOCKED. Later PRs (PR-2) may want to emit a dedicated
        // FINISHED phase, but for PR-1 we preserve the legacy "still LOCKED"
        // semantics from the rules spec §1.
    }

    /* ------------------------------------------------------------------ *
     * L1 — tick: drive phase transitions; defer per-event logic to PR-2+.
     * ------------------------------------------------------------------ */

    /**
     * Advance the clock by one 1Hz tick. PR-1 emits the phase transitions
     * only; bet-locking, dice generation, settlement, and bot cleanup are
     * stubbed and will be wired in later PRs.
     *
     * <p>Mirrors {@code TaiXiuModule.gameLoop} (TXM:422-468).
     *
     * @param nowMs caller-supplied wall clock — used by the watchdog and
     *              by later PR-2's bet-window calc. PR-1 stores it only.
     */
    public void tick(long nowMs) {
        this.lastTickMs = nowMs;
        RevealClock.Event ev = clock.advance();
        if (ev == null) {
            return;
        }
        switch (ev) {
            case LOCK_BETTING:
                lockBetting();
                break;
            case REFUND_CALC:
                // PR-2: invoke RefundCalculator. PR-1: phase remains LOCKED.
                break;
            case FINISH_FLAG:
                finishRound();
                break;
            case GENERATE_DICE:
                // PR-3: pull from ResultPipeline. PR-1: phase only.
                advance(RevealPhase.GENERATING);
                break;
            case REVEAL_DICE:
                // PR-3: revealDices() — copy pendingDice → revealedDice.
                advance(RevealPhase.REVEALED);
                publishPendingDice();
                break;
            case SETTLE:
                // PR-3: PrizeCalculator.calculate(). PR-1: phase only.
                advance(RevealPhase.SETTLED);
                break;
            case CLEANUP_BOTS:
                // PR-2: BotPlanner cleanup. PR-1: phase only.
                advance(RevealPhase.CLEANUP);
                break;
            case NEW_ROUND:
                // Drives the round forward. The caller (or a higher-level
                // controller in later PRs) is responsible for selecting
                // the next refId. For PR-1 we increment locally so the
                // standalone state machine is testable.
                startNewRound(this.referenceId + 1);
                break;
            default:
                throw new IllegalStateException("Unhandled clock event: " + ev);
        }
    }

    /**
     * Advance the round's {@link RevealPhase} with legal-transition check.
     * Package-private for tests; production callers go through
     * {@link #tick(long)}.
     */
    void advance(RevealPhase to) {
        RevealPhase.requireLegalTransition(this.phase, to);
        this.phase = to;
    }

    /**
     * Publish {@link #pendingDice} into {@link #revealedDice} on entry
     * to REVEALED. If no pendingDice exists (PR-1: never set), the field
     * remains null and {@link #snapshotForClient(String)} continues to
     * censor.
     *
     * <p>In PR-3 the {@code ResultPipeline.generate(...)} call inside the
     * {@code GENERATE_DICE} handler will populate {@link #pendingDice}.
     */
    private void publishPendingDice() {
        if (pendingDice != null) {
            this.revealedDice = pendingDice;
        }
    }

    /* ------------------------------------------------------------------ *
     * Snapshot — censoring contract per plan §3.3 / §3.5.
     * ------------------------------------------------------------------ */

    /**
     * Build a client-facing snapshot. Dice + result are zeroed/-1 unless
     * the round is in {@link RevealPhase#REVEALED} or
     * {@link RevealPhase#SETTLED}.
     *
     * @param username non-null username (per-user fields stubbed in PR-1)
     */
    public TaiXiuSnapshot snapshotForClient(String username) {
        if (username == null) {
            throw new NullPointerException("username");
        }
        TaiXiuSnapshot s = new TaiXiuSnapshot();
        // Snapshot reads volatile fields once — avoids torn reads across
        // a phase transition mid-build.
        RevealPhase localPhase = this.phase;
        short[] localRevealed = this.revealedDice;
        s.referenceId     = this.referenceId;
        s.roundId         = this.referenceId;          // SUN-1339: provider contract alias
        s.bettingClosesAt = this.bettingClosesAt;      // SUN-1339: safeBetExpiresAt source
        s.remainTime      = 0; // PR-2: derive from RevealClock#remainTime()
        s.bettingState    = localPhase.acceptsBets();
        s.potTai       = 0L; // PR-2: PotState.totalValue()
        s.potXiu       = 0L;
        s.myBetTai     = 0L; // PR-2: PotState.totalByUser(username)
        s.myBetXiu     = 0L;
        s.jpTai        = 0L; // PR-3: JackpotPool.value()
        s.jpXiu        = 0L;
        if (localPhase.diceVisible() && localRevealed != null && localRevealed.length >= 3) {
            s.dice1 = localRevealed[0];
            s.dice2 = localRevealed[1];
            s.dice3 = localRevealed[2];
            int sum = localRevealed[0] + localRevealed[1] + localRevealed[2];
            s.result = (short) (sum > 10 ? 1 : 0);
        } else {
            s.dice1 = 0;
            s.dice2 = 0;
            s.dice3 = 0;
            s.result = (short) -1;
        }
        s.numBetTai = 0;
        s.numBetXiu = 0;
        s.realNumBetTai = 0;
        s.realNumBetXiu = 0;
        return s;
    }

    /* ------------------------------------------------------------------ *
     * Test affordances (package-private) — keep production API minimal.
     * ------------------------------------------------------------------ */

    /** @return current phase (read-only). */
    public RevealPhase phase() {
        return phase;
    }

    /** @return current reference id (read-only). */
    public long referenceId() {
        return referenceId;
    }

    /** @return wall-clock start time of the round in ms (read-only). */
    public long startTimeMs() {
        return startTimeMs;
    }

    /**
     * Epoch-ms deadline after which bets are rejected by the timestamp guard.
     * Returns 0 after {@link #lockBetting()} is called (SUN-1339 §A2).
     */
    public long bettingClosesAt() {
        return bettingClosesAt;
    }

    /** @return last tick timestamp in ms; 0 until first tick. */
    public long lastTickMs() {
        return lastTickMs;
    }

    /**
     * Wire the round's downstream collaborators (PR-4 EngineConfig hook).
     *
     * <p>Baseline no-op — the engine's pipeline integration is driven by
     * a separate wave; this method exists so {@code EngineConfig} can
     * call it for symmetry with the future wiring contract without
     * causing a compile error during shadow rollout.
     */
    public void wireResultPipeline(com.sunwinkr.minigame.engine.dice.ResultPipeline pipeline,
                                    com.sunwinkr.minigame.engine.bet.BetLedger ledger,
                                    com.sunwinkr.minigame.engine.jackpot.JackpotPool jackpotPool,
                                    com.sunwinkr.minigame.engine.jackpot.JackpotTriggerPolicy triggerPolicy) {
        // PR-4 baseline no-op — wiring lands in a follow-up integration
        // commit. Pipeline is invoked directly by the BitZero bridge
        // until the cutover gate clears.
    }

    /** Package-private: seed {@code pendingDice} from tests. */
    void setPendingDice(short[] dice) {
        this.pendingDice = dice;
    }

    /** Package-private: seed {@code revealedDice} directly from tests. */
    void setRevealedDice(short[] dice) {
        this.revealedDice = dice;
    }
}
