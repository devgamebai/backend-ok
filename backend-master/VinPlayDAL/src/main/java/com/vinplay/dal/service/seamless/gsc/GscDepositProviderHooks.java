package com.vinplay.dal.service.seamless.gsc;

/**
 * Phase 3e — minimal per-vendor strategy surface that
 * {@link GscDepositAggregator} delegates to inside its dispatch loop.
 *
 * <p><b>Why this lives in VinPlayDAL.</b> The full
 * {@code game.third.hooks.gscSeamless.provider.ProviderAdapter} (and its
 * registry) sits in the {@code thirdParty} module, which depends on
 * {@code VinPlayDAL} — not the other way round. {@link GscDepositAggregator}
 * lives in {@code VinPlayDAL} so it cannot import the third-party
 * {@code ProviderAdapter} type directly without inverting the module
 * graph. The fix is the standard
 * <a href="https://en.wikipedia.org/wiki/Dependency_inversion_principle">DIP</a>:
 * declare the surface the aggregator needs as a tiny interface in
 * {@code VinPlayDAL}, and let the {@code DepositProcess} wiring on the
 * {@code thirdParty} side hand the aggregator an implementation that
 * delegates to {@code ProviderRegistry.forProduct(...)}.
 *
 * <h2>Captured methods (all that {@code DepositProcess} legacy uses)</h2>
 * <ul>
 *   <li>{@link #isCancelLikeDeposit} — SUN-1182: Hash Game and similar
 *       providers send a regular {@code /deposit} with action=CANCEL or
 *       wager_status in {CANCEL,REFUND,VOID} instead of using the
 *       dedicated cancel endpoint. Detection drives the post-credit
 *       cleanup branch (drop log_gsc_bets row + reverse rebate; skip
 *       the settle update).</li>
 *   <li>{@link #isFishGame} — SUN-888: CQ9 fish-game webhooks carry the
 *       full wallet balance in {@code amount}; the real prize is in
 *       {@code prize_amount}. The aggregator overrides the credit
 *       amount when both flags trigger.</li>
 *   <li>{@link #resolveLinkId} — SUN-1196 freespin chain: PG Soft /
 *       Pragmatic / JILI emit {@code parent_round_id} on freespin
 *       transactions; the aggregator routes the {@code $inc prize}
 *       Mongo update to the BUY row identified by that link, not the
 *       freespin's own (non-existent) row.</li>
 * </ul>
 *
 * <p>The methods mirror {@code ProviderAdapter} 1:1 in signature so the
 * {@code thirdParty}-side bridge is a one-line lambda per method.
 * If a future provider needs additional hooks (e.g. CQ9 fish prize
 * extraction beyond the default), add the hook here and wire it through
 * the bridge — the aggregator still owns the call site.
 *
 * <p>All methods have safe defaults for the case where the bridge can't
 * be resolved (test harness, registry lookup failure) — false / null —
 * so the aggregator can always reach a sane answer without checking for
 * null hooks.
 */
public interface GscDepositProviderHooks {

    /**
     * Default no-op implementation: every method returns the safe
     * fall-through value (false / null) so the aggregator behaves like
     * a vanilla provider when the bridge isn't wired.
     */
    GscDepositProviderHooks DEFAULT = new GscDepositProviderHooks() {};

    /** Mirrors {@code ProviderAdapter#isCancelLikeDeposit(String, String)}. */
    default boolean isCancelLikeDeposit(String action, String wagerStatus) {
        if (action != null && action.equalsIgnoreCase("CANCEL")) return true;
        if (wagerStatus == null) return false;
        return wagerStatus.equalsIgnoreCase("CANCEL")
            || wagerStatus.equalsIgnoreCase("REFUND")
            || wagerStatus.equalsIgnoreCase("VOID");
    }

    /** Mirrors {@code ProviderAdapter#isFishGame(String)}. */
    default boolean isFishGame(String gameCode) {
        return false;
    }

    /** Mirrors {@code ProviderAdapter#resolveLinkId(Object, String)}. */
    default String resolveLinkId(Object payload, String roundId) {
        return null;
    }

    /**
     * SUN-1370: opt-in flag for providers whose BET-time
     * {@code valid_bet_amount} is a placeholder and whose SETTLE-time
     * push carries the authoritative wager truth. Returns {@code true}
     * to instruct the aggregator to inline-post the deferred rebate
     * directly from {@code afterCredit} (using
     * {@link com.vinplay.dal.service.GscWagerReconciler#postFromSettlePayload})
     * instead of waiting for the 5-min reconciler scheduler tick.
     *
     * <p>Default {@code false}: every other provider keeps the
     * eager BET-time commission path. Only Dream Gaming (productCode
     * 1052) currently opts in.
     *
     * <p>Idempotency: the inline path writes a {@code drift_audit}
     * claim row before posting, so a concurrent reconciler tick on the
     * same wager short-circuits via the existing
     * {@code alreadyAudited} guard. Safe to enable per-provider
     * without coordinating the reconciler.
     */
    default boolean postsCommissionAtSettle() {
        return false;
    }

    /**
     * Resolve a {@link GscDepositProviderHooks} for a given GSC
     * {@code product_code}. The {@code DepositProcess} side wires this
     * to {@code ProviderRegistry.forProduct(...)} → method-reference
     * adapter; tests can pass a fixed/no-op resolver.
     */
    interface Resolver {
        GscDepositProviderHooks forProduct(int productCode);

        /** Always returns {@link #DEFAULT}. Useful for tests. */
        Resolver DEFAULT = new Resolver() {
            @Override public GscDepositProviderHooks forProduct(int productCode) {
                return GscDepositProviderHooks.DEFAULT;
            }
        };
    }
}
