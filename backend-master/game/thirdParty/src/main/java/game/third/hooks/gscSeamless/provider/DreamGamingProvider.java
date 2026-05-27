package game.third.hooks.gscSeamless.provider;

/**
 * Provider strategy for Dream Gaming (GSC product_code 1052) — the
 * Asia Gaming Dreaming Studio live-casino lineup we expose as the
 * "RB" tier (Baccarat A01-D05, DragonTiger A09/D16, Roulette A11,
 * Saigon S01-S06).
 *
 * <p>Most behavior inherits {@link ProviderAdapter}'s defaults — the
 * default {@link #resolveCommissionVolume} already prefers vendor's
 * {@code valid_bet_amount} over raw {@code bet_amount}, which is
 * exactly what Dream needs to fix SUN-1205/1206 (Baccarat hedge bets
 * paid commission on full bet instead of valid_bet).
 *
 * <p>This class exists to:
 * <ol>
 *   <li>document Dream-specific quirks in one place,</li>
 *   <li>register Dream as a recognized provider in
 *       {@link ProviderRegistry} so future Dream-only fixes have a
 *       home (parity with Evolution / Pragmatic / PG Soft / etc.),</li>
 *   <li>let ops disable provider-level features per Dream without
 *       branching on product_code in process files.</li>
 * </ol>
 *
 * <p>If Dream's wager push diverges from the spec in the future
 * (different field name, additional refund semantics, etc.), override
 * the relevant default here rather than branching in WithdrawProcess
 * or DepositProcess.
 */
public final class DreamGamingProvider implements ProviderAdapter {

    public static final DreamGamingProvider INSTANCE = new DreamGamingProvider();

    private DreamGamingProvider() {}

    @Override public int productCode() { return 1052; }
    @Override public String displayName() { return "Dream Gaming"; }

    // resolveCommissionVolume → default (uses valid_bet_amount). Dream
    // sends valid_bet_amount on every withdraw push (verified against
    // gsc_event_log: 43 of 43 withdraws populate it), so the default
    // is correct without override.

    // resolveBetAmount → default (raw amount, no fish-game override).

    // isFishGame → default (catalog-driven). Dream is live-casino only;
    // this is always false.

    // isCancelLikeDeposit → default. Dream has no documented quirk here.

    /**
     * SUN-1370: Dream's wire emits a placeholder
     * {@code valid_bet_amount = bet_amount} on the BET (withdraw) push;
     * the post-hedge truth lives on the SETTLE (deposit) push starting
     * 2026-05-16. Opt in to the inline rebate-post path so commission
     * lands within the same request as the settle credit instead of
     * waiting up to 10 minutes for the {@code GscWagerReconciler}
     * scheduler. The reconciler stays as a safety net via
     * {@code drift_audit} dedup.
     */
    @Override public boolean postsCommissionAtSettle() { return true; }
}
