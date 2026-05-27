package game.third.hooks.gscSeamless;

import com.vinplay.dal.service.seamless.gsc.GscBalanceAggregator;
import com.vinplay.dal.service.seamless.gsc.GscConfigProvider;
import game.third.hooks.gscSeamless.response.BalanceResponse;
import game.third.hooks.gscSeamless.response.BalanceResponseItem;
import game.third.usecase.config.GSCConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.constans.GSC;
import game.third.usecase.core.hook.Param;
import game.third.utils.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

/**
 * Thin delegator for the GSC seamless {@code /balance} endpoint.
 *
 * <p>All work is done by {@link GscBalanceAggregator}. The legacy
 * {@code doExecuteInner} body and the {@code GSC_AGGREGATOR_BALANCE_ENABLED}
 * flag check were removed on 2026-05-02 after Phase 2 cutover — the
 * aggregator is now the only path so a stale env var or accidental
 * config drift cannot revert behaviour to the cache-based code that
 * caused SUN-1229 (new accounts read balance=0 from a cold Hazelcast).
 */
public class BalanceProcess extends CommonProcess {
    private static final Logger logger = LoggerFactory.getLogger("hook");

    /**
     * Cached aggregator singleton — lazily initialized on first call.
     * Volatile for safe-publication; the constructor is side-effect-free.
     */
    private static volatile GscBalanceAggregator aggregator;

    private static GscBalanceAggregator aggregator() {
        GscBalanceAggregator a = aggregator;
        if (a != null) return a;
        synchronized (BalanceProcess.class) {
            if (aggregator == null) {
                aggregator = new GscBalanceAggregator(new GscConfigProvider() {
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
                });
            }
            return aggregator;
        }
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        if (!Request.isPost(request)) {
            return new BalanceResponse(new ArrayList<BalanceResponseItem>()).toJson();
        }
        try {
            return aggregator().handle(request);
        } catch (Throwable t) {
            // The aggregator's own try/catch covers normal exceptions.
            // This catch only fires on class-init / NoClassDefFoundError /
            // similar — return a safe empty response so the GSC provider
            // keeps a valid wire shape.
            logger.error("BalanceProcess aggregator delegate failed", t);
            return new BalanceResponse(new ArrayList<BalanceResponseItem>()).toJson();
        }
    }
}
