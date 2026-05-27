/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 */
package game.third.usecase.game568win.service.impl;

import com.vinplay.vbee.common.enums.Games;
import game.third.usecase.game568win.entities.ProductType;
import game.third.usecase.game568win.entities.ReturnStakeGame568Win;
import game.third.usecase.game568win.entities.SettleGame568Win;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.request.CancelData;
import game.third.usecase.game568win.response.DeductResult;
import game.third.usecase.game568win.service.impl.Game568winServiceBase;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Game568winServiceSeamless
extends Game568winServiceBase {
    @Override
    public DeductResult Deduct(TransactionGame568Win request) {
        long money;
        DeductResult result = new DeductResult();
        double amount = request.getAmount();
        boolean updateTransaction = false;
        TransactionGame568Win transaction = this.transactionGame568WinDAO.getTransactionByTransferCodeAndTransactionId(request.getTransferCode(), request.getTransactionId());
        if (transaction != null) {
            if (request.getStatus() != Status.Cancel) {
                result.setSuccess(false);
                result.setResult(-1);
                return result;
            }
            updateTransaction = true;
        }
        if ((double)(money = this.userMoneyService.getBalance(request.getUsername())) < amount) {
            result.setSuccess(false);
            result.setResult(-2);
            return result;
        }
        long fee = 0L;
        String description = Objects.requireNonNull(ProductType.getById(request.getProductType())).getProductName() + " - BET - " + request.getTransactionId() + " : " + request.getAmount();
        HashMap<String, Object> metaData = new HashMap<String, Object>();
        boolean success = this.userMoneyService.bet(request.getUsername(), (long)request.getAmount(), fee, Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, System.currentTimeMillis(), metaData);
        if (!success) {
            logger.error("Failed to process cancel action for transaction ID: {}", (Object)request.getTransactionId());
            result.setBalance(this.userMoneyService.getBalance(request.getUsername()));
            result.setSuccess(false);
            return result;
        }
        request.setStatus(Status.Running);
        success = updateTransaction ? this.transactionGame568WinDAO.updateAmountTransaction(request.getTransferCode(), request.getTransactionId(), request.getAmount(), Status.Running) : this.transactionGame568WinDAO.createTransaction(request);
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

    @Override
    public int Cancel(CancelData cancelData) {
        double amount = 0.0;
        if (cancelData.isCancelAll()) {
            List<TransactionGame568Win> transactions = this.transactionGame568WinDAO.getTransactionById(cancelData.getTransferCode());
            if (transactions.isEmpty()) {
                return -1;
            }
            boolean allCancel = true;
            for (TransactionGame568Win transaction : transactions) {
                if (transaction.getStatus() == Status.Cancel) continue;
                allCancel = false;
                amount += transaction.getAmount();
            }
            if (allCancel) {
                return -2;
            }
            SettleGame568Win settle = this.settleGame568WinDao.getSettle(cancelData.getTransferCode());
            if (settle != null && settle.getStatus() == Status.Settled) {
                amount -= (double)((long)settle.getWinLoss());
            }
            if (settle != null && settle.getStatus() == Status.Cancel) {
                return -2;
            }
            ReturnStakeGame568Win returnStakeRecord = this.returnStakeGame568WinDao.getReturnStake(cancelData.getTransferCode(), cancelData.getTransactionId());
            if (returnStakeRecord != null) {
                amount -= (double)((long)returnStakeRecord.getCurrentStake());
            }
            HashMap<String, Object> metaData = new HashMap<String, Object>();
            String description = Objects.requireNonNull(ProductType.getById(cancelData.getProductType())).getProductName() + " - Cancel - " + cancelData.getTransactionId() + " : " + amount;
            boolean success = this.userMoneyService.reward(cancelData.getUsername(), (long)amount, Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData);
            if (!success) {
                logger.error("Failed to process Cancel action for transaction Code: {}", (Object)cancelData.getTransactionId());
                return 0;
            }
            success = this.settleGame568WinDao.updateStatus(cancelData.getTransferCode(), Status.Cancel);
            if (!success) {
                logger.error("Failed to update settle for transaction Code: {}", (Object)cancelData.getTransactionId());
            }
            if (!(success = this.transactionGame568WinDAO.updateStatusTransaction(cancelData.getTransferCode(), Status.Cancel))) {
                logger.error("Failed to update transaction for transaction Code: {}", (Object)cancelData.getTransactionId());
            }
            return 1;
        }
        TransactionGame568Win transaction = this.getTransactionBy(cancelData.getTransferCode(), cancelData.getTransactionId());
        if (transaction == null) {
            return -1;
        }
        if (transaction.getStatus() == Status.Cancel) {
            return -2;
        }
        amount = transaction.getAmount();
        HashMap<String, Object> metaData = new HashMap<String, Object>();
        String description = Objects.requireNonNull(ProductType.getById(transaction.getProductType())).getProductName() + " - Cancel - " + transaction.getTransactionId() + " : " + transaction.getAmount();
        boolean success = this.userMoneyService.reward(transaction.getUsername(), (long)amount, Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData);
        if (!success) {
            logger.error("Failed to process Cancel action for transaction ID: {}", (Object)transaction.getTransactionId());
            return 0;
        }
        success = this.transactionGame568WinDAO.updateStatusTransaction(transaction.getTransferCode(), transaction.getTransactionId(), Status.Cancel, transaction.getAmount());
        if (!success) {
            logger.error("Failed to update transaction for transaction ID: {}", (Object)transaction.getTransactionId());
        }
        return 1;
    }
}

