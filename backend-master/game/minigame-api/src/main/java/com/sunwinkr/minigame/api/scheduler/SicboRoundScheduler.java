package com.sunwinkr.minigame.api.scheduler;

import com.sunwinkr.minigame.api.adapter.sicbo.SicboSettleService;
import com.sunwinkr.minigame.api.push.SicboTickPublisher;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboDiceGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboFundProtector;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPrizeCalculator;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboSettleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone Sicbo round scheduler (SUN-1341 E2).
 *
 * <p>Drives the Sicbo round lifecycle independently of the BitZero
 * {@code game-minigame} container. The legacy container suffers from a
 * {@code ClassNotFoundException: game.modules.XocDia.GameXocDiaController}
 * that crashes its gameLoop on startup; this scheduler makes Sicbo
 * self-contained in the Spring minigame-api process.
 *
 * <h3>Round timing (60-second cycle)</h3>
 * <pre>
 *  0s  — {@link #startNewRound()}  : OPEN, bettingClosesAt = now + 30s
 * 30s  — {@link #lockBetting()}   : LOCKED, bettingClosesAt = 0
 * 40s  — {@link #revealAndSettle()}: roll 3d6, REVEALED, settle, SETTLED
 * 60s  — next {@link #startNewRound()} fires
 * </pre>
 *
 * <h3>Feature flag</h3>
 * The scheduler is gated by {@code SICBO_STANDALONE_SCHEDULER_ENABLED=1}.
 * Default OFF — the legacy BitZero bridge remains authoritative. Set to
 * {@code 1} or {@code true} to activate the standalone scheduler.
 *
 * <p>All three {@code @Scheduled} methods share {@code fixedRate=60_000} with
 * staggered {@code initialDelay} values:
 * <ul>
 *   <li>{@code startNewRound}:  initialDelay=0,      fires at 0s, 60s, 120s…</li>
 *   <li>{@code lockBetting}:    initialDelay=30_000,  fires at 30s, 90s, 150s…</li>
 *   <li>{@code revealAndSettle}: initialDelay=40_000, fires at 40s, 100s, 160s…</li>
 * </ul>
 *
 * <p>Thread-safety: Spring task executor calls each scheduled method on its own
 * thread. The mutable collaborators ({@link SicboRound}, {@link SicboPotState})
 * are already volatile/synchronized. The {@link #txGenRef} is an
 * {@link AtomicReference} so the per-round generator is swapped atomically.
 */
@Component
public class SicboRoundScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(SicboRoundScheduler.class);

    /**
     * Env flag — set to "1" or "true" to activate the standalone scheduler.
     * Default OFF: the legacy BitZero bridge remains authoritative when unset.
     */
    public static final String FLAG_ENV = "SICBO_STANDALONE_SCHEDULER_ENABLED";

    /** Sicbo round cycle duration in milliseconds. */
    static final long ROUND_CYCLE_MS = 60_000L;

    /** Betting window duration in milliseconds (0–30s of each round). */
    static final long BETTING_WINDOW_MS = 30_000L;

    /** Delay from round start to lock (30s). */
    static final long LOCK_DELAY_MS = 30_000L;

    /** Delay from round start to reveal+settle (40s). */
    static final long REVEAL_DELAY_MS = 40_000L;

    private final SicboRound round;
    private final SicboPotState pot;
    private final SicboDiceGenerator diceGenerator;
    private final SicboFundProtector fundProtector;
    private final SicboPrizeCalculator prizeCalculator;
    private final SicboSettleService settleService;
    private final SicboTickPublisher tickPublisher;

    /**
     * Monotonically increasing reference-ID counter. Starts at 1 (matching the
     * Spring context boot value) and is incremented at every {@link #startNewRound}.
     */
    final AtomicLong refIdCounter;

    /**
     * Per-round transaction-ID generator. Replaced atomically at the start of
     * each round so all {@code SicboTxIdGenerator.next()} calls within a round
     * carry the correct {@code referenceId}.
     */
    final AtomicReference<SicboTxIdGenerator> txGenRef;

    public SicboRoundScheduler(SicboRound round,
                                SicboPotState pot,
                                @Qualifier("sicboDiceGenerator") SicboDiceGenerator diceGenerator,
                                SicboFundProtector fundProtector,
                                SicboPrizeCalculator prizeCalculator,
                                SicboSettleService settleService,
                                SicboTickPublisher tickPublisher) {
        this.round          = round;
        this.pot            = pot;
        this.diceGenerator  = diceGenerator;
        this.fundProtector  = fundProtector;
        this.prizeCalculator = prizeCalculator;
        this.settleService  = settleService;
        this.tickPublisher  = tickPublisher;
        // Bootstrap: seed with the refId already on the singleton round bean.
        long initialRefId = round.getReferenceId();
        this.refIdCounter  = new AtomicLong(initialRefId);
        this.txGenRef       = new AtomicReference<>(new SicboTxIdGenerator(initialRefId));
    }

    // -----------------------------------------------------------------------
    // Phase 1 — Round start (0s, 60s, 120s …)
    // -----------------------------------------------------------------------

    /**
     * Open a new Sicbo round.
     *
     * <ul>
     *   <li>Increment refId.</li>
     *   <li>Reset the pot.</li>
     *   <li>Replace the per-round txId generator.</li>
     *   <li>Call {@link SicboRound#startNewRound} — sets phase=OPEN,
     *       pendingDice=null, bettingClosesAt=now+{@link SicboRound#BETTING_WINDOW_MS}.</li>
     *   <li>Publish STOMP {@code round-start} event.</li>
     * </ul>
     *
     * <p>No-op when {@link #isEnabled()} is {@code false}.
     */
    @Scheduled(fixedRate = ROUND_CYCLE_MS, initialDelay = 0L)
    public void startNewRound() {
        if (!isEnabled()) {
            return;
        }
        try {
            long newRefId = refIdCounter.incrementAndGet();
            pot.reset();
            txGenRef.set(new SicboTxIdGenerator(newRefId));
            round.startNewRound(newRefId);
            LOG.info("SicboRoundScheduler.startNewRound: refId={} bettingClosesAt={}",
                     newRefId, round.bettingClosesAt());
            publishRoundStart(newRefId);
        } catch (Throwable t) {
            LOG.error("SicboRoundScheduler.startNewRound failed", t);
        }
    }

    // -----------------------------------------------------------------------
    // Phase 2 — Lock betting (30s)
    // -----------------------------------------------------------------------

    /**
     * Lock the betting window 30 seconds into the round.
     *
     * <p>Calls {@link SicboRound#lockBetting()} — transitions phase OPEN→LOCKED
     * and sets {@code bettingClosesAt=0} so the timestamp guard in
     * {@link com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService} rejects
     * further bets atomically.
     *
     * <p>No-op when {@link #isEnabled()} is {@code false}.
     */
    @Scheduled(fixedRate = ROUND_CYCLE_MS, initialDelay = LOCK_DELAY_MS)
    public void lockBetting() {
        if (!isEnabled()) {
            return;
        }
        try {
            long refId = round.getReferenceId();
            round.lockBetting();
            LOG.info("SicboRoundScheduler.lockBetting: refId={} phase={}", refId, round.getPhase());
        } catch (Throwable t) {
            LOG.error("SicboRoundScheduler.lockBetting failed", t);
        }
    }

    // -----------------------------------------------------------------------
    // Phase 3 — Reveal + settle (40s)
    // -----------------------------------------------------------------------

    /**
     * Roll the dice, reveal the result, and settle all bets.
     *
     * <p>Steps:
     * <ol>
     *   <li>Guard: phase must be LOCKED. If the phase is OPEN (e.g. lock step
     *       missed), call {@link SicboRound#lockBetting()} defensively.</li>
     *   <li>Generate dice via {@link SicboRound#generateDicesLocked} (RTP
     *       balancer + fund protector).</li>
     *   <li>Publish STOMP {@code reveal} push.</li>
     *   <li>Compute prize breakdown via {@link SicboRound#calculatePrize}.</li>
     *   <li>Call {@link SicboSettleService#settle} — credits wallets + marks
     *       DB rows SETTLED (idempotent).</li>
     *   <li>Transition phase LOCKED→SETTLED via {@link SicboRound#finishRound}.</li>
     * </ol>
     *
     * <p>Any error in dice generation or settlement is caught and logged; the
     * round is still finished so the scheduler does not get stuck.
     *
     * <p>No-op when {@link #isEnabled()} is {@code false}.
     */
    @Scheduled(fixedRate = ROUND_CYCLE_MS, initialDelay = REVEAL_DELAY_MS)
    public void revealAndSettle() {
        if (!isEnabled()) {
            return;
        }
        long refId = round.getReferenceId();
        try {
            // Defensive lock in case lockBetting() was skipped (e.g. startup skew)
            if (round.isBetting()) {
                LOG.warn("SicboRoundScheduler.revealAndSettle: round {} still OPEN at reveal — locking defensively", refId);
                round.lockBetting();
            }

            // Roll 3d6
            short[] dice;
            try {
                dice = round.generateDicesLocked(pot, diceGenerator, fundProtector, /*fund=*/0L);
            } catch (Throwable t) {
                LOG.error("SicboRoundScheduler.revealAndSettle: dice generation failed refId={}", refId, t);
                round.finishRound();
                return;
            }

            LOG.info("SicboRoundScheduler.revealAndSettle: refId={} dice=[{},{},{}] sum={}",
                     refId, dice[0], dice[1], dice[2], (int) dice[0] + dice[1] + dice[2]);

            // Publish reveal to STOMP subscribers
            publishReveal(dice);

            // Compute prize breakdown (pure, no side effects)
            SicboSettleResult result;
            try {
                result = round.calculatePrize(pot, prizeCalculator);
            } catch (Throwable t) {
                LOG.error("SicboRoundScheduler.revealAndSettle: calculatePrize failed refId={}", refId, t);
                round.finishRound();
                return;
            }

            // Settle — credits wallets + marks sicbo_bet rows SETTLED
            try {
                settleService.settle(refId, result);
            } catch (Throwable t) {
                LOG.error("SicboRoundScheduler.revealAndSettle: settle failed refId={}", refId, t);
                // settle is per-bet fault-tolerant, but catch top-level Throwable anyway
            }

            // Transition to SETTLED
            round.finishRound();
            LOG.info("SicboRoundScheduler.revealAndSettle: refId={} settled={} totalPayout={}",
                     refId, result.perBet == null ? 0 : result.perBet.size(), result.totalPayout);

        } catch (Throwable t) {
            LOG.error("SicboRoundScheduler.revealAndSettle: unexpected error refId={}", refId, t);
            try {
                round.finishRound();
            } catch (Throwable ignored) {
                // best effort; next startNewRound will reset state
            }
        }
    }

    // -----------------------------------------------------------------------
    // Feature flag
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when the standalone scheduler is enabled.
     *
     * <p>Set env var {@code SICBO_STANDALONE_SCHEDULER_ENABLED=1} (or
     * {@code true}) to activate. Default OFF — legacy BitZero bridge drives
     * the round lifecycle when unset.
     */
    public static boolean isEnabled() {
        String v = System.getenv(FLAG_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    // -----------------------------------------------------------------------
    // Push helpers (null-safe; log+swallow failures)
    // -----------------------------------------------------------------------

    private void publishRoundStart(long newRefId) {
        try {
            if (tickPublisher != null) {
                tickPublisher.publishRoundStart(newRefId);
            }
        } catch (Throwable t) {
            LOG.debug("SicboRoundScheduler: publishRoundStart failed refId={}", newRefId, t);
        }
    }

    private void publishReveal(short[] dice) {
        try {
            if (tickPublisher == null) {
                return;
            }
            // Build a minimal snapshot for the reveal push.
            com.sunwinkr.minigame.engine.sicbo.snapshot.SicboSnapshot snap =
                com.sunwinkr.minigame.engine.sicbo.snapshot.SicboSnapshot.of(
                    (short) 5, (short) 1, round.getReferenceId(),
                    0, false,
                    0L, 0L, 0L, 0L, 0L, 0L,
                    dice[0], dice[1], dice[2],
                    round.getPhase());
            tickPublisher.publishReveal(snap);
        } catch (Throwable t) {
            LOG.debug("SicboRoundScheduler: publishReveal failed", t);
        }
    }
}
