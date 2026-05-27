package game.third.hooks.gscSeamless;

import com.vinplay.dal.service.seamless.gsc.GscConfigProvider;
import com.vinplay.dal.service.seamless.gsc.GscPushBetAggregator;
import game.third.hooks.gscSeamless.response.PushBetResponse;
import game.third.usecase.config.GSCConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.constans.GSC;
import game.third.usecase.core.hook.Param;
import game.third.utils.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Thin delegator for the GSC seamless {@code /pushbet} +
 * {@code /pushbetdata} endpoints (advance bet metadata, audit-only).
 *
 * <p>All work is done by {@link GscPushBetAggregator}. The legacy
 * {@code doExecuteInner} body and the {@code GSC_AGGREGATOR_PUSHBET_ENABLED}
 * flag check were removed on 2026-05-02 after Phase 3a cutover.
 * Removing legacy also closes the dormant double-debit bug — the
 * legacy code called {@code userMoneyService.bet} on every transaction
 * inside PushBet, in addition to the WithdrawProcess deduction that
 * GSC's contract specifies as the actual money-moving call. The
 * aggregator path is correctly audit-only.
 */
public class PushBetProcess extends CommonProcess {
    private static final Logger logger = LoggerFactory.getLogger("hook");

    /**
     * Cached aggregator singleton — lazily initialized on first call.
     * Volatile for safe-publication; the constructor is side-effect-free.
     */
    private static volatile GscPushBetAggregator aggregator;

    private static GscPushBetAggregator aggregator() {
        GscPushBetAggregator a = aggregator;
        if (a != null) return a;
        synchronized (PushBetProcess.class) {
            if (aggregator == null) {
                aggregator = new GscPushBetAggregator(new GscConfigProvider() {
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
                });
            }
            return aggregator;
        }
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        if (!Request.isPost(request)) {
            return new PushBetResponse(1, "MethodNotAllowed").toJson();
        }
        try {
            return aggregator().handle(request);
        } catch (Throwable t) {
            logger.error("PushBetProcess aggregator delegate failed", t);
            return new PushBetResponse(1, "InternalError").toJson();
        }
    }
}
