package com.sunwinkr.minigame.engine.dice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTP-aware dice generator. Direct port of
 * {@code GenerationTaiXiu.generateResultWithHouseEdge} (GTX:93-166).
 *
 * <h3>Four branches (must remain behavior-preserving)</h3>
 * <ol>
 *   <li><b>Default-92 / no config</b> — both per-user and game-default
 *       are &gt;= 92 → return {@link RandomDiceGenerator} result.</li>
 *   <li><b>targetEdge &lt;= 0</b> or {@code totalBet &lt;= 0} → random.</li>
 *   <li><b>Imbalance &lt; 5%</b> ({@link #MIN_IMBALANCE_RATIO}) → random
 *       (natural house edge from tax suffices).</li>
 *   <li><b>Both scenarios negative profit</b> → random (legacy GTX:150).</li>
 * </ol>
 *
 * Otherwise: pick the side whose forced-outcome profit is closest to the
 * target profit (GTX:155-165).
 *
 * <p>Plan §2.3 row D3 / spec INV-18.
 */
public final class HouseEdgeDiceGenerator implements DiceGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(HouseEdgeDiceGenerator.class);

    /** Default win-rate floor at which "no admin config" is inferred. */
    static final double DEFAULT_WIN_RATE_FLOOR = 92.0;
    /** GTX:54 — minimum 5% imbalance to engage forcing. */
    static final double MIN_IMBALANCE_RATIO = 0.05;
    /** GTX:51 — fallback target house edge when no config present. */
    static final double DEFAULT_HOUSE_EDGE = 0.0;

    private final RtpResolverPort rtp;
    private final DiceGenerator randomFallback;

    public HouseEdgeDiceGenerator(RtpResolverPort rtp) {
        this(rtp, new RandomDiceGenerator());
    }

    /** Test seam — inject a deterministic fallback. */
    HouseEdgeDiceGenerator(RtpResolverPort rtp, DiceGenerator randomFallback) {
        if (rtp == null) {
            throw new NullPointerException("rtp");
        }
        if (randomFallback == null) {
            throw new NullPointerException("randomFallback");
        }
        this.rtp = rtp;
        this.randomFallback = randomFallback;
    }

    @Override
    public short[] generate(RoundContext ctx) {
        if (ctx == null) {
            return randomFallback.generate(null);
        }
        double winRatePct = rtp.effectivePct(ctx.userId, ctx.gameId);
        double targetEdgePct;
        if (winRatePct >= DEFAULT_WIN_RATE_FLOOR
            && rtp.effectivePct(ctx.gameId) >= DEFAULT_WIN_RATE_FLOOR) {
            // Branch 1: unconfigured → no forcing.
            targetEdgePct = DEFAULT_HOUSE_EDGE;
        } else {
            targetEdgePct = 100.0 - winRatePct;
        }

        // Branch 2: zero/negative edge or zero pot → random.
        if (targetEdgePct <= 0.0) {
            LOG.debug("HouseEdge: no config for {}, random", ctx.gameId);
            return randomFallback.generate(null);
        }
        long totalBet = ctx.realPotTai + ctx.realPotXiu;
        if (totalBet <= 0L) {
            return randomFallback.generate(null);
        }

        // Branch 3: pots balanced within 5% → tax handles edge naturally.
        double imbalance = Math.abs(ctx.realPotTai - ctx.realPotXiu) / (double) totalBet;
        if (imbalance < MIN_IMBALANCE_RATIO) {
            LOG.debug("HouseEdge: balanced pots ({}%), random",
                imbalance * 100);
            return randomFallback.generate(null);
        }

        // Profit scenarios per GTX:135-139.
        double profitIfTai = ctx.realPotXiu - ctx.realPotTai * (1.0 - ctx.taxPct / 100.0);
        double profitIfXiu = ctx.realPotTai - ctx.realPotXiu * (1.0 - ctx.taxPct / 100.0);
        double targetProfit = totalBet * targetEdgePct / 100.0;

        // Branch 4: both scenarios negative → random (GTX:150-153).
        if (profitIfTai < 0 && profitIfXiu < 0) {
            LOG.debug("HouseEdge: both scenarios negative, random");
            return randomFallback.generate(null);
        }

        double diffTai = Math.abs(profitIfTai - targetProfit);
        double diffXiu = Math.abs(profitIfXiu - targetProfit);
        short forceSide = (diffTai <= diffXiu) ? (short) 1 : (short) 0;
        LOG.debug("HouseEdge: forcing side {} for game {}", forceSide, ctx.gameId);
        return new ForcedDiceGenerator(forceSide).generate(null);
    }
}
