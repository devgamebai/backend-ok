/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 *  javax.servlet.http.HttpServletRequest
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.third.hooks.gscSeamless;

import com.vinplay.vbee.common.enums.Games;
import game.third.hooks.gscSeamless.request.Transaction;
import game.third.usecase.config.GSCConfig;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.constans.GSC;
import game.third.usecase.core.hook.AbstractHookProcessor;
import game.third.usecase.service.UserMoneyService;
import game.third.usecase.service.impl.UserMoneyServiceImpl;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommonProcess
extends AbstractHookProcessor<HttpServletRequest, String> {
    protected final UserMoneyService userMoneyService = new UserMoneyServiceImpl();
    protected static final Logger logger = LoggerFactory.getLogger((String)"hook");
    protected GSCConfig gscConfig = ThirdPartyLoad.getGscConfig();

    protected boolean TransactionAction(GSC.CurrencyCode currencyCode, String account, Transaction transaction) {
        String actionType;
        switch (actionType = transaction.action()) {
            case "BET": {
                logger.info("Processing BET action for transaction ID: {}", (Object)transaction.getId());
                return this.actionBet(currencyCode, account, transaction, "Bet " + transaction.getId());
            }
            case "FREEBET": {
                logger.info("Processing FREEBET action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "FreeBet " + transaction.getId());
                return true;
            }
            case "SETTLED": {
                logger.info("Processing SETTLED action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Settled " + transaction.getId());
                return true;
            }
            case "ROLLBACK": {
                logger.info("Processing ROLLBACK action for transaction ID: {}", (Object)transaction.getId());
                return true;
            }
            case "CANCEL": {
                logger.info("Processing CANCEL action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Cancel Bet " + transaction.getId());
                return true;
            }
            case "ADJUSTMENT": {
                logger.info("Processing ADJUSTMENT action for transaction ID: {}", (Object)transaction.getId());
                this.actionBet(currencyCode, account, transaction, "ADJUSTMENT " + transaction.getId());
                return true;
            }
            case "JACKPOT": {
                logger.info("Processing JACKPOT action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Jackpot " + transaction.getId());
                return true;
            }
            case "BONUS": {
                logger.info("Processing BONUS action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Bonus " + transaction.getId());
                return true;
            }
            case "TIP": {
                logger.info("Processing TIP action for transaction ID: {}", (Object)transaction.getId());
                this.actionBet(currencyCode, account, transaction, "Tip Bet" + transaction.getId());
                return true;
            }
            case "PROMO": {
                logger.info("Processing PROMO action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Promo " + transaction.getId());
                return true;
            }
            case "LEADERBOARD": {
                logger.info("Processing LEADERBOARD action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Leaderboard " + transaction.getId());
                return true;
            }
            case "BET_PRESERVE": {
                logger.info("Processing BET_PRESERVE action for transaction ID: {}", (Object)transaction.getId());
                this.actionBet(currencyCode, account, transaction, "Bet Preserve " + transaction.getId());
                return true;
            }
            case "PRESERVE_REFUND": {
                logger.info("Processing PRESERVE_REFUND action for transaction ID: {}", (Object)transaction.getId());
                this.actionReward(currencyCode, account, transaction, "Preserve Refund " + transaction.getId());
                return true;
            }
        }
        logger.error("Unknown action type: {}", (Object)actionType);
        return false;
    }

    protected boolean actionBet(GSC.CurrencyCode currencyCode, String account, Transaction transaction, String description) {
        GSCConfig gscConfig = ThirdPartyLoad.getGscConfig();
        try {
            boolean success;
            long amount = Long.parseLong(transaction.getBet_amount());
            amount = (long)((double)amount / this.getExchangeRateIn(currencyCode));
            long fee = 0L;
            if (gscConfig.getTax() > 0) {
                fee = amount * (long)gscConfig.getTax() / 100L;
            }
            if (!(success = this.userMoneyService.bet(account, amount, fee, Games.LIVE_CASINO.getName(), Games.LIVE_CASINO.getName(), description, System.currentTimeMillis(), new HashMap<String, Object>()))) {
                logger.error("Failed to process bet action for transaction ID: {}", (Object)transaction.getId());
                return false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    protected boolean actionReward(GSC.CurrencyCode currencyCode, String account, Transaction transaction, String description) {
        GSCConfig gscConfig = ThirdPartyLoad.getGscConfig();
        try {
            boolean success;
            long amount = Long.parseLong(transaction.getBet_amount());
            amount = (long)((double)amount / this.getExchangeRateOut(currencyCode));
            long fee = 0L;
            if (gscConfig.getTax() > 0) {
                fee = amount * (long)gscConfig.getTax() / 100L;
            }
            if (!(success = this.userMoneyService.bet(account, amount, fee, Games.LIVE_CASINO.getName(), Games.LIVE_CASINO.getName(), description + " - " + amount, System.currentTimeMillis(), new HashMap<String, Object>()))) {
                logger.error("Failed to process reward action for transaction ID: {}", (Object)transaction.getId());
                return false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    protected double getExchangeRateIn(GSC.CurrencyCode currencyCode) {
        // Use currency enum's built-in exchange rate (e.g. IDR2=1000, IDR=1)
        if (currencyCode != null && currencyCode.getExchangeRate() > 1) {
            return (double) currencyCode.getExchangeRate();
        }
        if (this.gscConfig.getExchangeRate() == 0.0 || this.gscConfig.getExchangeRate() == 1.0) {
            return 1.0;
        }
        return this.gscConfig.getExchangeRate();
    }

    protected double getExchangeRateOut(GSC.CurrencyCode currencyCode) {
        // Use currency enum's built-in exchange rate (e.g. IDR2=1000 → divide balance by 1000)
        if (currencyCode != null && currencyCode.getExchangeRate() > 1) {
            return 1.0 / (double) currencyCode.getExchangeRate();
        }
        if (this.gscConfig.getExchangeRate() == 0.0 || this.gscConfig.getExchangeRate() == 1.0) {
            return 1.0;
        }
        return 1.0 / this.gscConfig.getExchangeRate();
    }
}

