package com.vinplay.dal.service.seamless.gsc;

/**
 * Per-request configuration shared by every GSC seamless-wallet aggregator
 * (balance, pushbet, withdraw, deposit, cancel, rollback). Read fresh on
 * each {@code handle} invocation so ops can change rates / secret without
 * restarting the API server.
 *
 * <p><b>Why a shared interface rather than nested per-aggregator interfaces.</b>
 * Phase 2 introduced this contract on {@link GscBalanceAggregator} as a
 * nested {@code Config} interface. Phase 3 was always going to need the
 * exact same three knobs in {@code GscPushBetAggregator},
 * {@code GscWithdrawAggregator}, etc., so the Phase 3 prep follow-up
 * (see {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md} §3) hoisted it
 * into a single shared type. Consequence: a single Process-class wiring
 * builds one anonymous {@code GscConfigProvider} reading from
 * {@code ThirdPartyLoad.getGscConfig()} and feeds it to every aggregator
 * the Process class fronts. The Phase 5 cleanup (deferred NIT 23a,
 * 2026-05) dropped the backward-compat {@code GscBalanceAggregator.Config}
 * alias once both callers (Phase 2 BalanceProcess + balance test) had
 * been migrated to {@code GscConfigProvider} directly.
 *
 * <h2>Method contract</h2>
 * <ul>
 *   <li>{@link #getSecretKey()} — operator secret used in
 *       {@code md5(operator_code + request_time + endpoint + secret)}.
 *       Different endpoints supply different "endpoint" verbs
 *       ({@code "getbalance"}, {@code "pushbet"}, {@code "pushbetdata"},
 *       …); the secret is shared.
 *   <li>{@link #getOperatorExchangeRate()} — operator-level fallback rate
 *       used when the per-currency multiplier is 1. Mirrors
 *       {@code GSCConfig.getExchangeRate()}.
 *   <li>{@link #getCurrencyExchangeRate(String)} — per-currency multiplier
 *       (e.g. {@code 1000} for KRW2/VND2/IDR2) or {@code 0} for an
 *       unknown currency code. Mirrors
 *       {@code GSC.CurrencyCode.findByName(c).getExchangeRate()}.
 *   <li>{@link #getTaxPercent()} — operator-level tax / commission cut
 *       applied to wallet-moving handlers (PushBet, Withdraw, Deposit).
 *       Returns {@code 0} when no tax applies. Mirrors
 *       {@code GSCConfig.getTax()}. Phase 3a's PushBet aggregator does
 *       not move money so this method is unused there, but keeping it on
 *       the shared interface avoids a v2 redesign when Phase 3b/3c land.
 * </ul>
 */
public interface GscConfigProvider {

    /** GSC operator secret key for the {@code md5(...)} signature. */
    String getSecretKey();

    /**
     * Operator-level fallback exchange rate (used when the currency
     * enum's per-currency factor is 1). Must mirror
     * {@code GSCConfig.getExchangeRate()}.
     */
    double getOperatorExchangeRate();

    /**
     * Per-currency multiplier: returns {@code 1000} for VND2/KRW2/IDR2/…,
     * {@code 1} for VND/KRW/IDR/CNY/…, or {@code 0} for an unknown
     * currency code. Mirrors
     * {@code GSC.CurrencyCode.findByName(c).getExchangeRate()}.
     */
    int getCurrencyExchangeRate(String currencyCode);

    /**
     * Operator-level tax / commission percent (0–100). Used by
     * wallet-moving aggregators to compute the per-bet fee:
     * {@code fee = amount * tax / 100}. Mirrors
     * {@code GSCConfig.getTax()}.
     *
     * <p>Default {@code 0} so existing Config implementations from Phase 2
     * (which pre-date this method) compile against the shared interface
     * without modification.
     */
    default int getTaxPercent() { return 0; }
}
