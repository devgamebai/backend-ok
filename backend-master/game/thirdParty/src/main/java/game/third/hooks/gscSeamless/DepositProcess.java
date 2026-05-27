package game.third.hooks.gscSeamless;

import com.vinplay.dal.service.seamless.gsc.GscConfigProvider;
import com.vinplay.dal.service.seamless.gsc.GscDepositAggregator;
import com.vinplay.dal.service.seamless.gsc.GscDepositProviderHooks;
import game.third.hooks.gscSeamless.provider.ProviderAdapter;
import game.third.hooks.gscSeamless.provider.ProviderRegistry;
import game.third.hooks.gscSeamless.response.DepositResponse;
import game.third.hooks.gscSeamless.response.SeamlessWalletCode;
import game.third.usecase.config.GSCConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.constans.GSC;
import game.third.usecase.core.hook.Param;
import game.third.utils.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Thin delegator for the GSC seamless {@code /deposit} endpoint
 * (the prize/credit half of the bet/settle pair).
 *
 * <p>All work is done by {@link GscDepositAggregator}. The legacy
 * {@code doExecuteInner} body and the {@code GSC_AGGREGATOR_DEPOSIT_ENABLED}
 * flag check were removed on 2026-05-02 after Phase 3e cutover — the
 * aggregator is now the only path so a stale env var or accidental
 * config drift cannot revert behaviour.
 *
 * <p>SUN-tickets-of-behavior preserved by the aggregator:
 * <ul>
 *   <li>SUN-888 fish-game amount override (CQ9 / Fachai / JILI fish)</li>
 *   <li>SUN-865 / SUN-1201 rebate action_name resolution</li>
 *   <li>SUN-1182 cancel-via-deposit cleanup (Hash Game)</li>
 *   <li>SUN-LIVE-HIST log_gsc_bets settle update</li>
 *   <li>SUN-1196 freespin chain routing (PG Soft / Pragmatic / JILI)</li>
 *   <li>SUN-1184 free-spin row cleanup</li>
 *   <li>SUN-1108/1110 MongoRetry wrapper + Telegram alert</li>
 * </ul>
 */
public class DepositProcess extends CommonProcess {
    private static final Logger logger = LoggerFactory.getLogger("hook");

    /**
     * Cached aggregator singleton — lazily initialized on first call.
     * Volatile for safe-publication; the constructor is side-effect-free.
     */
    private static volatile GscDepositAggregator aggregator;

    /**
     * Bridge {@link GscDepositProviderHooks} → {@code ProviderAdapter}.
     * VinPlayDAL cannot import from the {@code thirdParty} module
     * (dependency direction is {@code thirdParty → VinPlayDAL}), so the
     * aggregator declares a tiny interface in DAL and we wire the
     * concrete {@code ProviderRegistry} lookup here on the thirdParty
     * side as a method-reference adapter.
     */
    private static GscDepositProviderHooks adaptProviderAdapter(final ProviderAdapter pa) {
        if (pa == null) return GscDepositProviderHooks.DEFAULT;
        return new GscDepositProviderHooks() {
            @Override public boolean isCancelLikeDeposit(String action, String wagerStatus) {
                return pa.isCancelLikeDeposit(action, wagerStatus);
            }
            @Override public boolean isFishGame(String gameCode) {
                return pa.isFishGame(gameCode);
            }
            @Override public String resolveLinkId(Object payload, String roundId) {
                return pa.resolveLinkId(payload, roundId);
            }
            @Override public boolean postsCommissionAtSettle() {
                return pa.postsCommissionAtSettle();
            }
        };
    }

    private static GscDepositAggregator aggregator() {
        GscDepositAggregator a = aggregator;
        if (a != null) return a;
        synchronized (DepositProcess.class) {
            if (aggregator == null) {
                aggregator = new GscDepositAggregator(
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
                        new GscDepositProviderHooks.Resolver() {
                            @Override
                            public GscDepositProviderHooks forProduct(int productCode) {
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
            return new DepositResponse(SeamlessWalletCode.INTERNAL_SERVER_ERROR, "MethodNotAllowed", 0, 0).toJson();
        }
        try {
            return aggregator().handle(request);
        } catch (Throwable t) {
            logger.error("DepositProcess aggregator delegate failed", t);
            return new DepositResponse(SeamlessWalletCode.INTERNAL_SERVER_ERROR, "InternalError", 0, 0).toJson();
        }
    }
}
