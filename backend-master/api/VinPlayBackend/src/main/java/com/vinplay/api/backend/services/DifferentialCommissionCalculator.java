package com.vinplay.api.backend.services;

import java.util.ArrayList;
import java.util.List;

/**
 * Differential Commission Calculator — Spec §5.3
 *
 * Model (example): TĐL A=1.5% → ĐL B=1% → User C
 * When C bets volume V:
 *   - B (direct) earns: V × own_rate = V × 1%
 *   - A (ancestor) earns: V × (own_rate − child_rate) = V × (1.5% − 1%) = V × 0.5%
 *   - Total paid = V × 1.5% (no overlap, matches top-level rate)
 *
 * Input chain: ordered from DIRECT agent (lowest level) → TOP ancestor (highest level in tree).
 *   Example: [DirectAgent(1%), Middle(1.2%), Top(1.5%)]
 *
 * Formula per agent (walking from direct → top):
 *   differential_pct = own_rate − max(previous rates walked so far)
 *   commission = volume × differential_pct / 100
 *
 * Rates must be monotonically non-decreasing upward (child ≤ parent).
 * If a parent has LOWER rate than child, differential is 0 (no negative payouts).
 */
public class DifferentialCommissionCalculator {

    public static class AgentRate {
        public final int agentId;
        public final double commissionRate;  // percent, e.g. 1.25 means 1.25%

        public AgentRate(int agentId, double commissionRate) {
            this.agentId = agentId;
            this.commissionRate = commissionRate;
        }
    }

    public static class Distribution {
        public final int agentId;
        public final double ownPct;
        public final double childPct;
        public final double differentialPct;
        public final long amount;

        public Distribution(int agentId, double ownPct, double childPct, double differentialPct, long amount) {
            this.agentId = agentId;
            this.ownPct = ownPct;
            this.childPct = childPct;
            this.differentialPct = differentialPct;
            this.amount = amount;
        }
    }

    /**
     * Calculate differential commission distribution for a user's bet volume.
     *
     * @param chain Ordered list: chain[0] = direct agent (lowest level), chain[last] = top ancestor
     * @param volume User's bet volume for the period
     * @return Distribution per agent (only entries with differentialPct > 0)
     */
    public static List<Distribution> calculate(List<AgentRate> chain, long volume) {
        List<Distribution> out = new ArrayList<>();
        if (chain == null || chain.isEmpty() || volume <= 0) return out;

        double prevRate = 0.0;  // Below the direct agent, nobody else earns
        for (AgentRate node : chain) {
            double diff = node.commissionRate - prevRate;
            if (diff > 0) {
                // SUN-714: Use Math.round instead of (long) Math.floor.
                // IEEE 754 subtraction produces values slightly below the expected
                // fraction (1.15 - 1.05 = 0.09999999999999987), so floor(1000 *
                // 0.0999... / 100) = 0 silently dropped middle tiers in
                // differential chains even though the master tier — whose diff is
                // computed 1.25 - 1.15 = 0.10000000000000009 (slightly OVER) —
                // rounded to 1 and credited normally. Math.round gives symmetric
                // half-up rounding which is the intended semantics.
                long amount = Math.round(volume * diff / 100.0);
                if (amount > 0) {
                    out.add(new Distribution(node.agentId, node.commissionRate, prevRate, diff, amount));
                }
                prevRate = node.commissionRate;  // Only update if this agent earned (monotonic rise)
            }
            // If diff <= 0 (parent's rate not higher), skip — no commission, don't update prevRate
        }
        return out;
    }

    /**
     * Total commission paid across all agents. Should equal volume × top_rate / 100
     * (when rates are monotonic).
     */
    public static long totalAmount(List<Distribution> distributions) {
        long sum = 0;
        for (Distribution d : distributions) sum += d.amount;
        return sum;
    }
}
