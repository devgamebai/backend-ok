package game.modules.slot.utils;

import java.util.Random;

/**
 * Standalone simulator for Phase 3 slot RTP control.
 *
 * Runs N mock spins against the fund-guard + pct-based force-lose logic,
 * measures achieved RTP and fund health over time. No Hazelcast, no DB —
 * purely mathematical model to validate the force-lose probability math.
 *
 * Baseline win model approximates BALANCED_WIN_RATE = {-1,150,80,40,15,2,8,1}:
 *   - result=0 (lose):      weight 704, payout 0
 *   - result=1 (small win): weight 150, payout 12x bet
 *   - result=2 (med win):   weight  80, payout 6x bet
 *   - result=3 (big win):   weight  40, payout 3x bet
 *   (derived from the winRate array — simplified for simulation)
 *
 * Run via:
 *   cd backend-master && gradle :game:slot:run -PmainClass=game.modules.slot.utils.SlotRtpSimulator \
 *     -Dpct=80 -Dspins=2000000 -Dfund=100000000 -Dbet=10000
 *
 * Or directly: java -cp backend-master/game/slot/build/libs/slot-1.0.jar \
 *              game.modules.slot.utils.SlotRtpSimulator 80 2000000 100000000 10000
 */
public class SlotRtpSimulator {

    // Simplified payout table tuned to ~92% raw EV (matches BALANCED_WIN_RATE RTP target).
    // Weights out of 1000; lose weight = 1000 - sum(win weights) = 680.
    //   EV check: 270*2 + 40*4 + 10*22 = 540 + 160 + 220 = 920 / 1000 = 92% ✓
    private static final int[] WIN_WEIGHTS  = {270, 40, 10};
    private static final int[] WIN_PAYOUTS  = {  2,  4, 22};   // multiplier of bet
    private static final int TOTAL_WEIGHT = 1000;

    public static void main(String[] args) {
        double pct    = argDouble(args, 0, "pct",   92.0);
        long   spins  = argLong  (args, 1, "spins", 2_000_000L);
        long   fund   = argLong  (args, 2, "fund",  100_000_000L);
        int    bet    = (int) argLong(args, 3, "bet", 10_000L);

        System.out.println("======== Slot RTP Simulation ========");
        System.out.println("configured pct: " + pct);
        System.out.println("spins:          " + spins);
        System.out.println("initial fund:   " + fund);
        System.out.println("bet per spin:   " + bet);
        System.out.println();

        Random rng = new Random(42L);
        long totalBet = 0;
        long totalPayout = 0;
        long forceLoseFundGuard = 0;
        long forceLosePctExtra  = 0;
        long forceLoseCapPrize  = 0;
        long fundMin = fund;
        long fundMax = fund;

        for (long i = 0; i < spins; i++) {
            // Simulate bet
            totalBet += bet;
            fund += bet;  // bet goes into fund before payout (simplified, ignoring fee)

            // Fund-guard check
            boolean fundGuardLose = simulateFundGuardLose(fund, bet, rng);
            boolean pctExtraLose = false;
            if (!fundGuardLose) {
                pctExtraLose = simulatePctExtraLose(pct, rng);
            }

            if (fundGuardLose) { forceLoseFundGuard++; }
            else if (pctExtraLose) { forceLosePctExtra++; }

            long payout;
            if (fundGuardLose || pctExtraLose) {
                payout = 0;
            } else {
                payout = rollPayout(bet, rng);
                // cap prize to 30% of fund
                if (payout > fund * 30L / 100L) {
                    payout = 0;
                    forceLoseCapPrize++;
                }
            }

            fund -= payout;
            totalPayout += payout;
            if (fund < fundMin) fundMin = fund;
            if (fund > fundMax) fundMax = fund;

            if ((i + 1) % 500_000L == 0) {
                System.out.printf("  progress: %d spins | fund=%d | achieved_rtp=%.2f%%%n",
                        i + 1, fund, 100.0 * totalPayout / totalBet);
            }
        }

        System.out.println();
        System.out.println("======== Results ========");
        System.out.printf("total_bet:               %d%n", totalBet);
        System.out.printf("total_payout:            %d%n", totalPayout);
        System.out.printf("house_net:               %d%n", totalBet - totalPayout);
        System.out.printf("achieved RTP:            %.2f%%%n", 100.0 * totalPayout / totalBet);
        System.out.printf("target RTP (input pct):  %.2f%%%n", pct);
        System.out.printf("delta:                   %.2f pp%n", (100.0 * totalPayout / totalBet) - pct);
        System.out.println();
        System.out.printf("force-lose (fund guard): %d (%.2f%%)%n",
                forceLoseFundGuard, 100.0 * forceLoseFundGuard / spins);
        System.out.printf("force-lose (pct extra):  %d (%.2f%%)%n",
                forceLosePctExtra, 100.0 * forceLosePctExtra / spins);
        System.out.printf("force-lose (cap prize):  %d (%.2f%%)%n",
                forceLoseCapPrize, 100.0 * forceLoseCapPrize / spins);
        System.out.println();
        System.out.printf("final fund:              %d%n", fund);
        System.out.printf("fund min during run:     %d%n", fundMin);
        System.out.printf("fund max during run:     %d%n", fundMax);
    }

    /** Simulates fund-guard force-lose (same brackets as SlotHouseEdge.shouldForceLose). */
    static boolean simulateFundGuardLose(long fund, int bet, Random rng) {
        if (bet <= 0) return false;
        long ratio = fund / bet;
        int roll = rng.nextInt(100);
        if (ratio < 5)   return roll < 90;
        if (ratio < 20)  return roll < 70;
        if (ratio < 50)  return roll < 40;
        if (ratio < 100) return roll < 20;
        return false;
    }

    /** Simulates pct-based extra force-lose (matches SlotHouseEdge.shouldForceLoseWithPct math). */
    static boolean simulatePctExtraLose(double pct, Random rng) {
        double delta = SlotHouseEdge.BASELINE_PCT - pct;
        if (delta <= 0) return false;
        int extraPct = (int) Math.min(60, Math.round(delta));
        if (extraPct <= 0) return false;
        return rng.nextInt(100) < extraPct;
    }

    /** Rolls a weighted payout from the simplified BALANCED_WIN_RATE model. */
    static long rollPayout(int bet, Random rng) {
        int winWeightSum = 0;
        for (int w : WIN_WEIGHTS) winWeightSum += w;
        int roll = rng.nextInt(TOTAL_WEIGHT);
        if (roll >= winWeightSum) return 0;  // no win
        int acc = 0;
        for (int i = 0; i < WIN_WEIGHTS.length; i++) {
            acc += WIN_WEIGHTS[i];
            if (roll < acc) return (long) bet * WIN_PAYOUTS[i];
        }
        return 0;
    }

    private static double argDouble(String[] args, int idx, String name, double def) {
        if (args.length > idx) { try { return Double.parseDouble(args[idx]); } catch (Exception ignored) {} }
        String sys = System.getProperty(name);
        if (sys != null) { try { return Double.parseDouble(sys); } catch (Exception ignored) {} }
        return def;
    }
    private static long argLong(String[] args, int idx, String name, long def) {
        if (args.length > idx) { try { return Long.parseLong(args[idx]); } catch (Exception ignored) {} }
        String sys = System.getProperty(name);
        if (sys != null) { try { return Long.parseLong(sys); } catch (Exception ignored) {} }
        return def;
    }
}
