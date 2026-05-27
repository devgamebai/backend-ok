package game.third.hooks.gscSeamless.provider;

import java.util.Map;

/**
 * Per-provider strategy for GSC seamless-wallet quirks.
 *
 * Each upstream gaming provider (Evolution / Pragmatic / PG Soft / JILI / CQ9
 * / Fachai / HashGame …) ships slightly different webhook shapes — different
 * round-chain linkage fields, different fish-game tagging, different cancel
 * patterns. Before this interface was introduced those quirks were branched
 * inline across {@code WithdrawProcess}, {@code DepositProcess},
 * {@code CancelProcess} and {@code WithdrawRequest.Transaction}, which led
 * to the SUN-1174 / SUN-1184 / SUN-1188 / SUN-1196 family of regressions —
 * each fix for one provider would shadow another's case.
 *
 * Now: one class per provider in this package, registered in
 * {@link ProviderRegistry#forProduct(int)} by GSC product_code. Process
 * files just look up the adapter and delegate. Adding a new provider =
 * one new class + one registry entry.
 *
 * Implementations MUST be stateless (singleton-safe).
 */
public interface ProviderAdapter {

    int productCode();

    String displayName();

    // ── per-bet quirks ────────────────────────────────────────────────────

    /**
     * Resolve the bet amount in our wallet sub-units.
     *
     * Fish-shooter games (CQ9 / Fachai / JILI fish) historically send
     * {@code amount} = full wallet balance via the SUN-888 quirk; the real
     * per-shot cost is in {@code valid_bet_amount} (preferred) or
     * {@code bet_amount}. Apply that substitution here so EVERY fish-game
     * provider gets it uniformly — pre-refactor the override was inline
     * in WithdrawProcess for any isCq9Fish row regardless of which
     * provider sent it.
     *
     * Live-casino + slot providers don't trip this branch (isFishGame
     * is false) — they keep using the raw amount.
     *
     * @param rawAmount         transaction.amount (signed; we abs)
     * @param validBetAmount    transaction.valid_bet_amount
     * @param betAmount         transaction.bet_amount
     * @param isFishGame        result of {@link #isFishGame(String)}
     * @param fxIn              currency exchange-rate IN multiplier
     */
    default long resolveBetAmount(double rawAmount, double validBetAmount,
                                   double betAmount, boolean isFishGame, double fxIn) {
        if (isFishGame) {
            if (validBetAmount > 0) {
                return Math.round(Math.abs(validBetAmount) * fxIn);
            }
            if (betAmount > 0) {
                return Math.round(Math.abs(betAmount) * fxIn);
            }
        }
        return Math.round(Math.abs(rawAmount) * fxIn);
    }

    /**
     * SUN-1205/1206: resolve the commission-eligible bet volume for the
     * downstream rebate pipeline. Most providers compute commission on
     * the same bet they deduct, but live-casino vendors (Dream Gaming
     * being the headline) refund the smaller side of a hedge — players
     * who bet banker+player same-amount commit money but earn no real
     * wager (valid_bet=0); banker 20k + player 30k → valid_bet=10k.
     *
     * <p>Default behavior: trust the vendor's {@code valid_bet_amount}
     * field if non-zero, else fall back to the deducted {@code amount}.
     * Providers whose wager push uses a different field name or whose
     * semantics need a hedge-aware computation can override.
     *
     * @param validBetAmount  vendor's valid_bet_amount (after refund)
     * @param betAmount       vendor's bet_amount (raw)
     * @param deductAmount    money actually taken from player's wallet
     * @param fxIn            currency exchange-rate IN multiplier
     * @return commission volume (always &ge; 0)
     */
    default long resolveCommissionVolume(double validBetAmount, double betAmount,
                                          long deductAmount, double fxIn) {
        // Vendor sent valid_bet_amount and it differs from bet → use it
        // (this is the hedge case we care about).
        if (validBetAmount >= 0d && validBetAmount != betAmount) {
            return Math.round(Math.abs(validBetAmount) * fxIn);
        }
        // Otherwise fall back to what the wallet was charged.
        return Math.abs(deductAmount);
    }

    /**
     * Round-chain id used to merge sub-bets of one logical hand:
     *   - Evolution Blackjack main + insurance share a {@code game_id}
     *   - PG Soft / Pragmatic buy-freespins share a {@code parent_round_id}
     *   - JILI same as above (but only for slots that emit it)
     *
     * Returns null when the provider has no chain concept (regular slot
     * spin) — coalesce then falls back to event_key / wager_code.
     */
    default String resolveLinkId(Object payload, String roundId) {
        return null;
    }

    // ── classification ────────────────────────────────────────────────────

    /**
     * Fish-shooter games skip the log_gsc_bets row + commission pipeline
     * (SUN-980 / SUN-1174). Default: catalog category=Fishing, with a
     * substring "fish"/"cq9" fallback for un-cataloged games (defensive
     * — fail closed: a missed catalog row treated as fish skips
     * commission rather than mis-counts as a real bet).
     */
    default boolean isFishGame(String gameCode) {
        if (gameCode == null) return false;
        try {
            String cat = com.vinplay.dal.service.GscGameNameResolver
                    .categoryOf(productCode(), gameCode);
            if ("Fishing".equalsIgnoreCase(cat)) return true;
        } catch (Throwable ignored) { /* catalog lookup is best-effort */ }
        String gc = gameCode.toLowerCase();
        return gc.contains("fish") || gc.contains("cq9");
    }

    /**
     * SUN-1182: some providers (HASH_ROULETTE / 1149) don't fire the
     * dedicated /cancel endpoint — they send a regular /deposit whose
     * action or wager_status carries the cancel intent. DepositProcess
     * uses this to decide whether to delete the log_gsc_bets row +
     * reverse rebate after refund.
     */
    default boolean isCancelLikeDeposit(String action, String wagerStatus) {
        if (action != null && action.equalsIgnoreCase("CANCEL")) return true;
        if (wagerStatus == null) return false;
        return wagerStatus.equalsIgnoreCase("CANCEL")
            || wagerStatus.equalsIgnoreCase("REFUND")
            || wagerStatus.equalsIgnoreCase("VOID");
    }

    /**
     * SUN-1370: opt-in flag for providers whose BET-time
     * {@code valid_bet_amount} is a placeholder and whose SETTLE-time
     * push carries the authoritative wager truth. The aggregator uses
     * this to decide whether to inline-post the deferred rebate from
     * {@code afterCredit} or wait for the scheduled
     * {@code GscWagerReconciler} tick.
     *
     * <p>Default: {@code false}. Override per-provider when the wire
     * contract requires it. Dream Gaming (productCode 1052) is the
     * only one that currently opts in.
     */
    default boolean postsCommissionAtSettle() {
        return false;
    }

    // ── shared helpers (stateless) ────────────────────────────────────────

    /**
     * First nested Map value within a payload root. PG Soft / Pragmatic /
     * JILI all wrap detail under a {@code <id>} key whose value is the
     * detail map; the id varies, but there is exactly one such inner map
     * per transaction.
     */
    static Map<?, ?> firstNestedMap(Object payload) {
        if (!(payload instanceof Map)) return null;
        Map<?, ?> root = (Map<?, ?>) payload;
        for (Object v : root.values()) {
            if (v instanceof Map) return (Map<?, ?>) v;
        }
        return null;
    }

    static String mapGetStr(Map<?, ?> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }
}
