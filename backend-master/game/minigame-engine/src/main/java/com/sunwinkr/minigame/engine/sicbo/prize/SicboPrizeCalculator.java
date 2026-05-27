package com.sunwinkr.minigame.engine.sicbo.prize;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.eval.SicboWinningStatusEvaluator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Top-level prize calculator for one Sicbo settlement pass.
 *
 * <p>Direct behavior-preserving port of {@code MGRoomSicbo.reward()}
 * (SBR:1064-1119) for the per-bet prize/fee math. Adapter layer wires the
 * actual {@code WalletPort.credit} calls + Mongo writes — this calculator
 * stays pure.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li><b>Exploit guard (Quochuy98 incident, 2026-05-02)</b> — if
 *       {@code dice == null} OR {@code winningStatuses.isEmpty()}, refund
 *       EVERY bet via END_TRANS (regardless of betSide). See SBR:1179-1200
 *       parallel in TaiXiu code; in Sicbo this manifests when getResult
 *       returns a malformed dice triple.</li>
 *   <li>Evaluate {@code winningStatuses = SicboWinningStatusEvaluator.evaluate(d1,d2,d3)}.</li>
 *   <li>For each bet entry:
 *       <ul>
 *         <li>Skip bots ({@code userId <= 0}) — they don't receive prize
 *             messages; bot accounting is virtual.</li>
 *         <li>If the bet side is NOT in {@code winningStatuses}, record a
 *             "none" result (prize=0, fee=0, refund=false). Suppression
 *             of TAI/XIU/CHAN/LE/DOUBLE on triples (INV-15) is handled
 *             upstream by the evaluator.</li>
 *         <li>For ONE_DICE_* (IDs 15..20, INV-9): prize is
 *             {@code bet * (occurrences+1)} (×2/×3/×4).</li>
 *         <li>Otherwise: prize is {@code bet * rotation} (INV-8).</li>
 *         <li>Fee: {@code fee = tax * prize / (200 - tax)} (SBR:1072).</li>
 *       </ul></li>
 * </ol>
 *
 * <h3>Constructor params</h3>
 * <ul>
 *   <li>{@code tax} — house tax pct (legacy {@code MGRoomSicbo.tax} field,
 *       typically 5.0f). The {@code 200 - tax} denominator matches the
 *       legacy formula exactly — do not "simplify" it.</li>
 *   <li>{@code payout} — used solely for the {@code SicboPayoutCalculator}
 *       constant offsets (ONE_DICE_*) and the {@code countOccurrences} helper.</li>
 * </ul>
 *
 * <p>Thread-safe: stateless beyond the immutable {@code tax} field.
 */
public final class SicboPrizeCalculator {

    private static final int ONE_DICE_MIN_ID = 15;
    private static final int ONE_DICE_MAX_ID = 20;
    private static final int ONE_DICE_FACE_OFFSET = 14;

    private final float tax;
    private final SicboPayoutCalculator payout;

    /**
     * @param tax     house tax pct (e.g. {@code 5.0f}); must be &lt; 200 to
     *                keep the denominator positive
     * @param payout  shared payout calculator (also used by the RTP balancer)
     */
    public SicboPrizeCalculator(float tax, SicboPayoutCalculator payout) {
        if (tax < 0f || tax >= 200f) {
            throw new IllegalArgumentException("tax must be in [0..200), got " + tax);
        }
        if (payout == null) {
            throw new NullPointerException("payout");
        }
        this.tax = tax;
        this.payout = payout;
    }

    /**
     * Compute the settlement result for the round.
     *
     * @param bets immutable bet snapshot (may include bot entries — they're
     *             ignored in the per-bet loop)
     * @param dice three dice values [1..6] — pass {@code null} to trigger
     *             the exploit-guard refund path
     * @return non-null result; {@link SicboSettleResult#exploitGuardFired}
     *         is {@code true} when bets were refunded
     */
    public SicboSettleResult calculate(List<SicboPotEntry> bets, short[] dice) {
        if (bets == null) {
            throw new NullPointerException("bets");
        }

        // --- Exploit guard step 1: null dice → refund all (Quochuy98) ---
        if (dice == null || dice.length != 3) {
            return refundAll(bets);
        }

        // --- Evaluate winning statuses ---
        List<String> winners = SicboWinningStatusEvaluator.evaluate(dice[0], dice[1], dice[2]);

        // --- Exploit guard step 2: empty winning-statuses → refund all ---
        if (winners.isEmpty()) {
            return refundAll(bets);
        }

        Set<String> winnersSet = new HashSet<>(winners);
        int[] diceValues = new int[] { dice[0], dice[1], dice[2] };

        List<SicboPrizePerBet> perBet = new ArrayList<>(bets.size());
        long totalPayout = 0L;
        long totalFee = 0L;

        for (SicboPotEntry tx : bets) {
            // SBR:1068 — reward() iterates listUserBet but the legacy code at
            // SBR:1046 also skips userId <= 0 for the payout pre-calculation.
            // For the actual reward path, MGRoomSicbo applies updateMoney even
            // to bots; the engine's per-bet result is computed for ALL entries
            // and the adapter decides bot-skip routing.
            SicboBetType betTx;
            try {
                betTx = SicboBetType.byId(tx.betSideId);
            } catch (IllegalArgumentException e) {
                perBet.add(SicboPrizePerBet.none(tx));
                continue;
            }
            if (!winnersSet.contains(betTx.getName())) {
                perBet.add(SicboPrizePerBet.none(tx));
                continue;
            }
            long prize;
            if (tx.betSideId >= ONE_DICE_MIN_ID && tx.betSideId <= ONE_DICE_MAX_ID) {
                int face = tx.betSideId - ONE_DICE_FACE_OFFSET;
                int occurrences = SicboPayoutCalculator.countOccurrences(diceValues, face);
                if (occurrences == 2) {
                    prize = tx.betValue * 3L;
                } else if (occurrences == 3) {
                    prize = tx.betValue * 4L;
                } else {
                    prize = tx.betValue * 2L;
                }
            } else {
                prize = tx.betValue * (long) betTx.getRotation();
            }
            // SBR:1072 fee formula — DO NOT simplify; 200-tax is intentional.
            long fee = (long) (tax * (float) prize / (200.0f - tax));
            perBet.add(SicboPrizePerBet.win(tx, prize, fee));
            // Only real users contribute to the payout total for fund-decrement
            // accounting (matches sotienphaitra real-user filter at SBR:1046).
            if (tx.userId > 0) {
                totalPayout += prize;
                totalFee += fee;
            }
        }

        return new SicboSettleResult(perBet, totalPayout, totalFee, 0L, false);
    }

    /**
     * Convenience wrapper: returns just the per-round payout total via the
     * underlying {@link SicboPayoutCalculator}. Useful for tests verifying
     * the prize calc and the RTP balancer agree.
     */
    public long potentialPayout(List<SicboPotEntry> bets, short[] dice) {
        return payout.calculatePotentialPayout(bets, dice);
    }

    /**
     * Build a settle-result where every bet is flagged for full refund
     * (exploit guard). prize and fee per entry are 0 and the
     * {@code totalRefund} field captures the sum of betValue across all
     * (real-user) bets so the adapter can verify the fund delta.
     */
    private SicboSettleResult refundAll(List<SicboPotEntry> bets) {
        List<SicboPrizePerBet> perBet = new ArrayList<>(bets.size());
        long totalRefund = 0L;
        for (SicboPotEntry tx : bets) {
            perBet.add(SicboPrizePerBet.refund(tx));
            if (tx.userId > 0) {
                totalRefund += tx.betValue;
            }
        }
        return new SicboSettleResult(perBet, 0L, 0L, totalRefund, true);
    }
}
