package game.third.hooks.gscSeamless;

import com.vinplay.dal.service.seamless.gsc.GscConfigProvider;
import com.vinplay.dal.service.seamless.gsc.GscTransferAggregator;
import game.third.hooks.gscSeamless.response.BaseResponse;
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
 * Thin delegator for the GSC seamless {@code /transfer} endpoint
 * (FREEBET / SETTLED / CANCEL / JACKPOT / BONUS / PROMO /
 * LEADERBOARD / PRESERVE_REFUND).
 *
 * <p>All work is done by {@link GscTransferAggregator}. The legacy
 * {@code doExecuteInner} body and the {@code GSC_AGGREGATOR_TRANSFER_ENABLED}
 * flag check were removed on 2026-05-02 after Phase 3b cutover —
 * the aggregator is now the only path. Removing legacy also closes
 * the dormant {@code CommonProcess.actionReward} bug (legacy called
 * {@code userMoneyService.bet} which always debits, even on credit-side
 * actions like FREEBET / JACKPOT / BONUS); the aggregator maps each
 * action to its correct intended direction per GSC's contract. See
 * {@link GscTransferAggregator} class doc for the full map.
 */
public class TransferProcess extends CommonProcess {
    private static final Logger logger = LoggerFactory.getLogger("hook");

    /**
     * Cached aggregator singleton — lazily initialized on first call.
     * Volatile for safe-publication; the constructor is side-effect-free.
     */
    private static volatile GscTransferAggregator aggregator;

    private static GscTransferAggregator aggregator() {
        GscTransferAggregator a = aggregator;
        if (a != null) return a;
        synchronized (TransferProcess.class) {
            if (aggregator == null) {
                aggregator = new GscTransferAggregator(new GscConfigProvider() {
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
            return new BaseResponse(SeamlessWalletCode.INTERNAL_SERVER_ERROR, "MethodNotAllowed", 0, 0).toJson();
        }
        try {
            return aggregator().handle(request);
        } catch (Throwable t) {
            logger.error("TransferProcess aggregator delegate failed", t);
            return new BaseResponse(SeamlessWalletCode.INTERNAL_SERVER_ERROR, "InternalError", 0, 0).toJson();
        }
    }
}
