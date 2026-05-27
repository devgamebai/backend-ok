package com.vinplay.dal.service.seamless.gsc;

/**
 * Phase 3f — minimal per-vendor strategy surface that
 * {@link GscWithdrawAggregator} delegates to inside its dispatch loop.
 *
 * <p><b>Why a separate interface from {@link GscDepositProviderHooks}.</b>
 * Phase 3e introduced one for Deposit. The two endpoints share some
 * provider hooks (fish detection, link-id resolution) but each also
 * carries hooks the other does not need:
 *
 * <ul>
 *   <li>Withdraw needs {@link #resolveBetAmount} (SUN-888 fish-game amount
 *       override) and {@link #resolveCommissionVolume} (SUN-1205/1206
 *       Dream Gaming hedge bets).</li>
 *   <li>Deposit needs {@code isCancelLikeDeposit} (SUN-1182 cancel-via-
 *       deposit detection) which has no Withdraw analogue.</li>
 * </ul>
 *
 * <p>Keeping them separate means each aggregator's bridge surface
 * captures exactly what it actually consumes — no dead methods on the
 * Withdraw side, no exposure of cancel-detection logic to a path that
 * doesn't process cancels. Future per-aggregator hooks can be added to
 * the appropriate interface without bloating the other.
 *
 * <p><b>Why this lives in VinPlayDAL.</b> Same reason as
 * {@link GscDepositProviderHooks}: the full
 * {@code game.third.hooks.gscSeamless.provider.ProviderAdapter} (and its
 * registry) sits in the {@code thirdParty} module, which depends on
 * {@code VinPlayDAL} — not the other way round. {@link GscWithdrawAggregator}
 * lives in {@code VinPlayDAL} so it cannot import the third-party
 * {@code ProviderAdapter} type directly. The fix is the standard
 * <a href="https://en.wikipedia.org/wiki/Dependency_inversion_principle">DIP</a>:
 * declare the surface the aggregator needs as a tiny interface in
 * {@code VinPlayDAL}, and let the {@code WithdrawProcess} wiring on the
 * {@code thirdParty} side hand the aggregator an implementation that
 * delegates to {@code ProviderRegistry.forProduct(...)}.
 *
 * <h2>Captured methods (all that {@code WithdrawProcess} legacy uses)</h2>
 * <ul>
 *   <li>{@link #isFishGame} — SUN-888: CQ9 / Fachai / JILI fish webhooks
 *       carry the full wallet balance in {@code amount}; the real per-shot
 *       cost is in {@code valid_bet_amount} / {@code bet_amount}. Drives
 *       the per-batch fish flag and the {@link #resolveBetAmount} branch.</li>
 *   <li>{@link #resolveBetAmount} — SUN-888 amount substitution. Default
 *       behavior trusts {@code valid_bet_amount} when fish, else uses
 *       {@code abs(amount)}. Wallet sub-units after FX multiplication.</li>
 *   <li>{@link #resolveCommissionVolume} — SUN-1205/1206 Dream Gaming
 *       hedge-bet rebate volume. When vendor sends a smaller
 *       {@code valid_bet_amount} than {@code bet_amount} (banker+player
 *       same-amount → 0; banker 20k + player 30k → 10k), the rebate
 *       pipeline sees the smaller volume while the wallet is debited
 *       in full. Default trusts vendor's valid_bet_amount, falls back
 *       to deducted amount.</li>
 *   <li>{@link #resolveLinkId} — SUN-1196 freespin chain / Evolution
 *       Blackjack main+insurance: PG Soft / Pragmatic / JILI emit
 *       {@code parent_round_id}; Evolution emits a Blackjack-shared
 *       {@code game_id}. The aggregator uses this to merge sub-bets
 *       of one logical hand on the {@code log_gsc_bets} insert/upsert.</li>
 * </ul>
 *
 * <p>The methods mirror {@code ProviderAdapter} 1:1 in signature so the
 * {@code thirdParty}-side bridge is a one-line lambda per method. If a
 * future provider needs additional hooks, add them here and wire them
 * through the bridge — the aggregator still owns the call site.
 *
 * <p>All methods have safe defaults for the case where the bridge can't
 * be resolved (test harness, registry lookup failure) — false / null /
 * round-and-abs the raw amount — so the aggregator can always reach a
 * sane answer without checking for null hooks.
 */
public interface GscWithdrawProviderHooks {

    /**
     * Default no-op implementation: every method returns the safe
     * fall-through value (false / null / abs+round) so the aggregator
     * behaves like a vanilla provider when the bridge isn't wired.
     */
    GscWithdrawProviderHooks DEFAULT = new GscWithdrawProviderHooks() {};

    /** Mirrors {@code ProviderAdapter#isFishGame(String)}. */
    default boolean isFishGame(String gameCode) {
        return false;
    }

    /**
     * Mirrors {@code ProviderAdapter#resolveBetAmount(double, double, double, boolean, double)}.
     * Default: when fish, prefer {@code validBetAmount} > 0, else
     * {@code betAmount} > 0; otherwise {@code abs(rawAmount)}. All times
     * the FX multiplier so the result is in wallet sub-units.
     */
    default long resolveBetAmount(double rawAmount, double validBetAmount,
                                   double betAmount, boolean isFishGame, double fxIn) {
        if (isFishGame) {
            if (validBetAmount > 0) return Math.round(Math.abs(validBetAmount) * fxIn);
            if (betAmount > 0)      return Math.round(Math.abs(betAmount) * fxIn);
        }
        return Math.round(Math.abs(rawAmount) * fxIn);
    }

    /**
     * Mirrors {@code ProviderAdapter#resolveCommissionVolume(double, double, long, double)}.
     * Default: trust vendor's {@code validBetAmount} when it differs
     * from {@code betAmount}; otherwise return {@code abs(deductAmount)}.
     */
    default long resolveCommissionVolume(double validBetAmount, double betAmount,
                                          long deductAmount, double fxIn) {
        if (validBetAmount >= 0d && validBetAmount != betAmount) {
            return Math.round(Math.abs(validBetAmount) * fxIn);
        }
        return Math.abs(deductAmount);
    }

    /** Mirrors {@code ProviderAdapter#resolveLinkId(Object, String)}. */
    default String resolveLinkId(Object payload, String roundId) {
        return null;
    }

    /**
     * Resolve a {@link GscWithdrawProviderHooks} for a given GSC
     * {@code product_code}. The {@code WithdrawProcess} side wires this
     * to {@code ProviderRegistry.forProduct(...)} → method-reference
     * adapter; tests can pass a fixed/no-op resolver.
     */
    interface Resolver {
        GscWithdrawProviderHooks forProduct(int productCode);

        /** Always returns {@link #DEFAULT}. Useful for tests. */
        Resolver DEFAULT = new Resolver() {
            @Override public GscWithdrawProviderHooks forProduct(int productCode) {
                return GscWithdrawProviderHooks.DEFAULT;
            }
        };
    }
}
