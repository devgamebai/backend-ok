/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 */
package game.third.usecase.game568win.service.impl;

import com.vinplay.vbee.common.enums.Games;
import game.third.usecase.game568win.entities.ProductType;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.response.DeductResult;
import game.third.usecase.game568win.service.impl.Game568winServiceBase;
import java.util.HashMap;
import java.util.Objects;

public class Game568winServiceSBO
extends Game568winServiceBase {
    @Override
    public DeductResult Deduct(TransactionGame568Win request) {
        long money;
        DeductResult result = new DeductResult();
        double amount = request.getAmount();
        boolean updateTransaction = false;
        TransactionGame568Win transaction = this.transactionGame568WinDAO.getTransactionByTransferCodeAndTransactionId(request.getTransferCode(), request.getTransactionId());
        if (transaction != null) {
            if (transaction.getStatus() == Status.Settled || transaction.getStatus() == Status.Cancel) {
                result.setSuccess(false);
                result.setResult(-1);
                return result;
            }
            if (request.getAmount() <= transaction.getAmount()) {
                result.setSuccess(false);
                result.setResult(-3);
                return result;
            }
            amount = request.getAmount() - transaction.getAmount();
            System.out.println("Game568winServiceSBO -> Deduct -> amount: " + amount + " => transaction.getTransferCode(): " + transaction.getTransferCode());
            updateTransaction = true;
        }
        if ((double)(money = this.userMoneyService.getBalance(request.getUsername())) < amount) {
            result.setSuccess(false);
            result.setResult(-2);
            return result;
        }
        long fee = 0L;
        String description = Objects.requireNonNull(ProductType.getById(request.getProductType())).getProductName() + " - BET - " + request.getTransactionId() + " : " + amount;
        HashMap<String, Object> metaData = new HashMap<String, Object>();
        boolean success = this.userMoneyService.bet(request.getUsername(), (long)amount, fee, Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, System.currentTimeMillis(), metaData);
        if (!success) {
            logger.error("Failed to process cancel action for transaction ID: {}", (Object)request.getTransactionId());
            result.setBalance(this.userMoneyService.getBalance(request.getUsername()));
            result.setSuccess(false);
            return result;
        }
        request.setStatus(Status.Running);
        success = updateTransaction ? this.transactionGame568WinDAO.updateAmountTransaction(request.getTransferCode(), request.getTransactionId(), request.getAmount()) : this.transactionGame568WinDAO.createTransaction(request);
        if (!success) {
            logger.error("Failed to create transaction for transaction ID: {}", (Object)request.getTransactionId());
            result.setSuccess(false);
            result.setResult(0);
            return result;
        }
        result.setBalance(this.userMoneyService.getBalance(request.getUsername()));
        result.setSuccess(true);
        result.setResult(1);
        return result;
    }
}

