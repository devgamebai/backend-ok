package game.third.hooks.gscSeamless;

import com.vinplay.dal.service.seamless.gsc.GscConfigProvider;
import com.vinplay.dal.service.seamless.gsc.GscWithdrawAggregator;
import com.vinplay.dal.service.seamless.gsc.GscWithdrawProviderHooks;
import game.third.hooks.gscSeamless.provider.ProviderAdapter;
import game.third.hooks.gscSeamless.provider.ProviderRegistry;
import game.third.hooks.gscSeamless.response.SeamlessWalletCode;
import game.third.hooks.gscSeamless.response.WithdrawResponse;
import game.third.usecase.config.GSCConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.constans.GSC;
import game.third.usecase.core.hook.Param;
import game.third.utils.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Thin delegator for the GSC seamless {@code /withdraw} endpoint
 * (the bet-debit half of the bet/settle pair).
 *
 * <p>All work is done by {@link GscWithdrawAggregator}. The legacy
 * {@code doExecuteInner} body and the {@code GSC_AGGREGATOR_WITHDRAW_ENABLED}
 * flag check were removed on 2026-05-02 after Phase 3f cutover — the
 * aggregator is now the only path so a stale env var or accidental
 * config drift cannot revert behaviour to the legacy code that did
 * not stamp a per-bet {@code msg.setId("gsc:" + txnId)} on the rebate
 * publish (root cause of SUN-1227 — 21 hands collapsing into one
 * rebate row).
 *
 * <p>SUN-tickets-of-behavior preserved by the aggregator:
 * <ul>
 *   <li>SUN-888 fish-game amount override (CQ9 / Fachai / JILI fish)</li>
 *   <li>SUN-980 commission-eligibility skip (transfer-in)</li>
 *   <li>SUN-1184 free-spin trigger amount=0 row skip</li>
 *   <li>SUN-1196 freespin / Blackjack chain link</li>
 *   <li>SUN-1108/1110 MongoRetry wrapper + Telegram alert</li>
 *   <li>SUN-1205/1206 hedge-bet commissionVolume (Dream Gaming)</li>
 *   <li>SUN-LIVE-HIST log_gsc_bets insert</li>
 * </ul>
 */
public class WithdrawProcess extends CommonProcess {
    private static final Logger logger = LoggerFactory.getLogger("hook");

    /**
     * Cached aggregator singleton — lazily initialized on first call.
     * Volatile for safe-publication; the constructor is side-effect-free.
     */
    private static volatile GscWithdrawAggregator aggregator;

    /**
     * Bridge {@link GscWithdrawProviderHooks} → {@code ProviderAdapter}.
     * VinPlayDAL cannot import from {@code thirdParty} (dependency
     * direction is {@code thirdParty → VinPlayDAL}), so the aggregator
     * declares a tiny interface in DAL and we wire the concrete
     * {@code ProviderRegistry} lookup here on the thirdParty side.
     */
    private static GscWithdrawProviderHooks adaptProviderAdapter(final ProviderAdapter pa) {
        if (pa == null) return GscWithdrawProviderHooks.DEFAULT;
        return new GscWithdrawProviderHooks() {
            @Override public boolean isFishGame(String gameCode) {
                return pa.isFishGame(gameCode);
            }
            @Override public long resolveBetAmount(double rawAmount, double validBetAmount,
                                                    double betAmount, boolean isFishGame, double fxIn) {
                return pa.resolveBetAmount(rawAmount, validBetAmount, betAmount, isFishGame, fxIn);
            }
            @Override public long resolveCommissionVolume(double validBetAmount, double betAmount,
                                                           long deductAmount, double fxIn) {
                return pa.resolveCommissionVolume(validBetAmount, betAmount, deductAmount, fxIn);
            }
            @Override public String resolveLinkId(Object payload, String roundId) {
                return pa.resolveLinkId(payload, roundId);
            }
        };
    }

    private static GscWithdrawAggregator aggregator() {
        GscWithdrawAggregator a = aggregator;
        if (a != null) return a;
        synchronized (WithdrawProcess.class) {
            if (aggregator == null) {
                aggregator = new GscWithdrawAggregator(
                        new GscConfigProvider() {
                            @Override
                            public String getSecretKey() {
                                GSCConfig c = ThirdPartyLoad.getGscConfig();
                                return c == null ? null : c.getSecretKey();
                            }
                            @Override
                            public double getOperatorExchangeRate() {
                                GSCConfig c = ThirdPartyLoad.getGscConfig();
                                return c == null ? 1.0 : c.getExchangeRate();
                            }
                            @Override
                            public int getCurrencyExchangeRate(String currencyCode) {
                                GSC.CurrencyCode cc = GSC.CurrencyCode.findByName(currencyCode);
                                return cc == null ? 0 : cc.getExchangeRate();
                            }
                            @Override
                            public int getTaxPercent() {
                                GSCConfig c = ThirdPartyLoad.getGscConfig();
                                return c == null ? 0 : c.getTax();
                            }
                        },
                        new GscWithdrawProviderHooks.Resolver() {
                            @Override
                            public GscWithdrawProviderHooks forProduct(int productCode) {
                                return adaptProviderAdapter(ProviderRegistry.forProduct(productCode));
                            }
                        });
            }
            return aggregator;
        }
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        if (!Request.isPost(request)) {
            return new WithdrawResponse(SeamlessWalletCode.INCORRECT_AGENT_KEY, "MethodNotAllowed", 0, 0).toJson();
        }
        try {
            return aggregator().handle(request);
        } catch (Throwable t) {
            logger.error("WithdrawProcess aggregator delegate failed", t);
            return new WithdrawResponse(SeamlessWalletCode.INTERNAL_SERVER_ERROR, "InternalError", 0, 0).toJson();
        }
    }
}
