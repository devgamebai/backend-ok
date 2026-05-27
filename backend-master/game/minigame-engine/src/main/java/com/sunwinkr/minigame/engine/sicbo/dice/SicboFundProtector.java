package com.sunwinkr.minigame.engine.sicbo.dice;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bounded-retry fund-protection fallback applied after the RTP balancer
 * picks a dice triple.
 *
 * <p>Mitigation of AMBIGUOUS #4 — the legacy unbounded while-loop in
 * {@code MGRoomSicbo.getResult} (SBR:614-617):
 *
 * <pre>{@code
 * if (tienloi < 0L && (fundSB = this.fundTaiXiu + tienloi) < 0L) {
 *     while (tienloi < 0L && this.fundTaiXiu + (tienloi =
 *             this.totalValueBetUser - (sotientra = this.sotienphaitra(
 *                     (result = TaiXiuUtil.genarateRandomResult())[0],
 *                     result[1], result[2]))) <= 0L) {
 *         Debug.trace(...);
 *     }
 * }
 * }</pre>
 *
 * <p>The original loop has no upper bound — under a pathologically deep-
 * negative fund it can spin indefinitely and starve the game loop. This
 * implementation:
 * <ol>
 *   <li>Caps retries at {@link #MAX_REROLL_ITERATIONS} (1000).</li>
 *   <li>Tracks the least-bad random outcome seen (smallest house loss)
 *       so even on exhaustion the returned dice is no worse than the
 *       original RTP choice.</li>
 *   <li>Emits a metric / log-line {@code sicbo.fund_protector.exhausted}
 *       so ops can alert on starvation.</li>
 *   <li>Behaviour-preserving on the non-pathological path — if any retry
 *       produces a tienloi that keeps fund non-negative, we exit early
 *       just like the legacy loop.</li>
 * </ol>
 *
 * <h3>Inputs and semantics</h3>
 * Mirrors the legacy formula:
 * {@code tienloi = totalValueBetUser - sotienphaitra(d1, d2, d3)}.
 * "tienloi > 0" means the house GAINS (totalPayout less than wagers).
 * "tienloi < 0" means the house LOSES (payout exceeds wagers).
 *
 * <h3>Thread-safety</h3>
 * Stateless apart from the (atomic) exhausted-counter accessible via
 * {@link #exhaustedCount()}. Safe for concurrent use across rooms.
 */
public final class SicboFundProtector {

    private static final Logger LOG = LoggerFactory.getLogger(SicboFundProtector.class);

    /** Upper bound on retry attempts before exhaustion (replaces inf-loop AMBIGUOUS #4). */
    public static final int MAX_REROLL_ITERATIONS = 1000;

    private final SicboRandomDiceGenerator random;
    private final SicboPayoutCalculator payout;

    /** Cumulative count of {@link #protect} calls that hit the iteration bound. */
    private volatile long exhaustedCount = 0L;

    public SicboFundProtector(SicboRandomDiceGenerator random, SicboPayoutCalculator payout) {
        if (random == null) {
            throw new NullPointerException("random");
        }
        if (payout == null) {
            throw new NullPointerException("payout");
        }
        this.random = random;
        this.payout = payout;
    }

    /**
     * Apply the fund-protection fallback to {@code candidate} dice.
     *
     * <p>If {@code candidate} keeps the post-round fund non-negative — i.e.
     * {@code totalValueBetUser - payout(candidate) >= 0} OR
     * {@code fund + (totalValueBetUser - payout(candidate)) >= 0} — return
     * {@code candidate} unchanged.
     *
     * <p>Otherwise re-roll random dice up to {@link #MAX_REROLL_ITERATIONS}
     * times, returning the first triple that keeps the fund non-negative.
     * If the bound is exhausted, return the least-bad triple seen so far
     * (maximizing tienloi) and increment the exhausted-count metric.
     *
     * @param ctx                 round (passed through to the random generator)
     * @param snapshot            immutable bet snapshot
     * @param totalValueBetUser   total real-user wagers
     * @param fundTaiXiu          current house fund balance (vin units)
     * @param candidate           dice from the upstream RTP balancer
     * @return dice array that preserves fund solvency when possible,
     *         else the least-bad triple under the iteration bound
     */
    public Result protect(SicboRound ctx,
                          List<SicboPotEntry> snapshot,
                          long totalValueBetUser,
                          long fundTaiXiu,
                          short[] candidate) {
        if (candidate == null || candidate.length != 3) {
            throw new IllegalArgumentException("candidate must be length-3 dice array");
        }

        long candPayout = payout.calculatePotentialPayout(snapshot, candidate);
        long candTienloi = totalValueBetUser - candPayout;

        // SBR:614 — only enter the protection loop when candidate would push
        // the fund negative AND tienloi itself is negative. Otherwise the
        // RTP-balancer pick is safe.
        if (!(candTienloi < 0L && fundTaiXiu + candTienloi < 0L)) {
            return new Result(candidate, candTienloi, false, 0);
        }

        // Track the least-bad triple. Start with the candidate so we never
        // return worse than what we came in with.
        short[] bestDice = candidate;
        long bestTienloi = candTienloi;

        int iterations = 0;
        while (iterations < MAX_REROLL_ITERATIONS) {
            iterations++;
            short[] tryDice = random.generate(ctx, snapshot, totalValueBetUser);
            long tryPayout = payout.calculatePotentialPayout(snapshot, tryDice);
            long tryTienloi = totalValueBetUser - tryPayout;

            if (tryTienloi > bestTienloi) {
                bestTienloi = tryTienloi;
                bestDice = tryDice;
            }

            // SBR:615 exit condition: tienloi >= 0 OR fund + tienloi > 0.
            if (!(tryTienloi < 0L && fundTaiXiu + tryTienloi <= 0L)) {
                return new Result(tryDice, tryTienloi, false, iterations);
            }
        }

        // Bound exhausted — emit metric, return least-bad.
        exhaustedCount++;
        LOG.warn("sicbo.fund_protector.exhausted refId={} totalValueBetUser={} fund={} bestTienloi={} iterations={}",
                 ctx != null ? ctx.getReferenceId() : -1L,
                 totalValueBetUser, fundTaiXiu, bestTienloi, iterations);
        return new Result(bestDice, bestTienloi, true, iterations);
    }

    /** @return number of {@link #protect} calls that hit the iteration bound. */
    public long exhaustedCount() {
        return exhaustedCount;
    }

    /** Test-only: reset the exhausted counter. */
    void resetExhaustedCount() {
        exhaustedCount = 0L;
    }

    /**
     * Outcome of a {@link #protect} call.
     *
     * <ul>
     *   <li>{@code dice} — chosen dice triple (length 3, each in [1..6])</li>
     *   <li>{@code tienloi} — house P&amp;L for this dice
     *       ({@code totalValueBetUser - payout})</li>
     *   <li>{@code exhausted} — {@code true} when the protector hit
     *       {@link #MAX_REROLL_ITERATIONS} without finding a solvent dice</li>
     *   <li>{@code iterations} — retry count consumed (0 when the candidate
     *       was already safe)</li>
     * </ul>
     */
    public static final class Result {
        public final short[] dice;
        public final long tienloi;
        public final boolean exhausted;
        public final int iterations;

        public Result(short[] dice, long tienloi, boolean exhausted, int iterations) {
            this.dice = dice;
            this.tienloi = tienloi;
            this.exhausted = exhausted;
            this.iterations = iterations;
        }
    }
}
