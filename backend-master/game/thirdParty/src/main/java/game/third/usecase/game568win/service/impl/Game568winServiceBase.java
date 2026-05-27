/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.third.usecase.game568win.service.impl;

import com.vinplay.vbee.common.enums.Games;
import game.third.usecase.game568win.dao.BonusGame568WinDao;
import game.third.usecase.game568win.dao.ReturnStakeGame568WinDao;
import game.third.usecase.game568win.dao.SettleGame568WinDao;
import game.third.usecase.game568win.dao.TransactionGame568WinDAO;
import game.third.usecase.game568win.dao.impl.BonusGame568WinDaoImpl;
import game.third.usecase.game568win.dao.impl.ReturnStakeGame568WinDaoImpl;
import game.third.usecase.game568win.dao.impl.SettleGame568WinDaoImpl;
import game.third.usecase.game568win.dao.impl.TransactionGame568WinDAOImpl;
import game.third.usecase.game568win.entities.BonusGame568Win;
import game.third.usecase.game568win.entities.ProductType;
import game.third.usecase.game568win.entities.ReturnStakeGame568Win;
import game.third.usecase.game568win.entities.SettleGame568Win;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.model.Bonus;
import game.third.usecase.game568win.model.ReturnStake;
import game.third.usecase.game568win.request.CancelData;
import game.third.usecase.game568win.request.RollbackData;
import game.third.usecase.game568win.request.SettleData;
import game.third.usecase.game568win.response.DeductResult;
import game.third.usecase.game568win.service.Game568winService;
import game.third.usecase.service.UserMoneyService;
import game.third.usecase.service.impl.UserMoneyServiceImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game568winServiceBase
implements Game568winService {
    protected final TransactionGame568WinDAO transactionGame568WinDAO = new TransactionGame568WinDAOImpl();
    protected final SettleGame568WinDao settleGame568WinDao = new SettleGame568WinDaoImpl();
    protected final UserMoneyService userMoneyService = new UserMoneyServiceImpl();
    protected final ReturnStakeGame568WinDao returnStakeGame568WinDao = new ReturnStakeGame568WinDaoImpl();
    protected final BonusGame568WinDao bonusGame568WinDao = new BonusGame568WinDaoImpl();
    protected static final Logger logger = LoggerFactory.getLogger((String)"service");

    @Override
    public DeductResult Deduct(TransactionGame568Win request) {
        DeductResult result = new DeductResult();
        double amount = request.getAmount();
        TransactionGame568Win transaction = this.transactionGame568WinDAO.getTransactionByTransferCodeAndTransactionId(request.getTransferCode(), request.getTransactionId());
        if (transaction != null) {
            result.setSuccess(false);
            result.setResult(-1);
            return result;
        }
        long money = this.userMoneyService.getBalance(request.getUsername());
        if ((double)money < amount) {
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
        success = this.transactionGame568WinDAO.createTransaction(request);
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

    protected TransactionGame568Win getTransactionBy(String transferCode, String TransactionId) {
        TransactionGame568Win transaction = this.transactionGame568WinDAO.getTransactionByTransferCodeAndTransactionId(transferCode, TransactionId);
        if (transaction == null) {
            logger.error("Transaction not found for TransferCode: {} and TransactionId: {}", (Object)transferCode, (Object)TransactionId);
            return null;
        }
        if (!transaction.getTransferCode().contains(transferCode) || !transaction.getTransactionId().contains(TransactionId)) {
            return null;
        }
        return transaction;
    }

    @Override
    public int Settle(SettleData request) {
        List<TransactionGame568Win> transactions;
        boolean updateSettle = false;
        SettleGame568Win settle = this.settleGame568WinDao.getSettle(request.getTransferCode());
        if (settle != null) {
            if (settle.getStatus() == Status.Settled) {
                return -2;
            }
            updateSettle = true;
        }
        if ((transactions = this.transactionGame568WinDAO.getTransactionById(request.getTransferCode())).isEmpty()) {
            return -1;
        }
        boolean haveRunning = false;
        for (TransactionGame568Win transaction : transactions) {
            if (transaction.getStatus() != Status.Running) continue;
            haveRunning = true;
            break;
        }
        if (!haveRunning) {
            return -3;
        }
        if (request.getWinLoss() != 0.0) {
            boolean success = updateSettle ? this.settleGame568WinDao.updateWinLoss(request.getTransferCode(), Status.Settled, request.getWinLoss()) : this.settleGame568WinDao.createSettle(request.toSettleGame568Win());
            if (!success) {
                logger.error("Failed to Settle transaction for transaction Code: {}", (Object)request.getTransferCode());
                return 0;
            }
            success = this.transactionGame568WinDAO.updateStatusTransaction(request.getTransferCode(), Status.Settled);
            if (!success) {
                logger.error("Failed to Update status transaction for transaction Code: {}", (Object)request.getTransferCode());
                return 0;
            }
            HashMap<String, Object> metaData = new HashMap<String, Object>();
            String description = Objects.requireNonNull(ProductType.getById(request.getProductType())).getProductName() + " - WinLost - " + request.getTransferCode() + " : " + request.getWinLoss();
            success = this.userMoneyService.reward(request.getUsername(), (long)request.getWinLoss(), Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData);
            if (!success) {
                logger.error("Failed to process settle action for transaction Code: {}", (Object)request.getTransferCode());
                return 0;
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int Rollback(RollbackData rollbackData) {
        List<TransactionGame568Win> transactions = this.transactionGame568WinDAO.getTransactionById(rollbackData.getTransferCode());
        if (transactions.isEmpty()) {
            return -1;
        }
        HashMap<String, Object> metaData = new HashMap<String, Object>();
        boolean isRunning = true;
        for (TransactionGame568Win transaction : transactions) {
            if (transaction.getStatus() == Status.Running) continue;
            isRunning = false;
            break;
        }
        if (isRunning) {
            return -2;
        }
        boolean success = false;
        SettleGame568Win settle = this.settleGame568WinDao.getSettle(rollbackData.getTransferCode());
        if (settle != null && settle.getStatus() == Status.Settled) {
            double amount = settle.getWinLoss();
            String description = Objects.requireNonNull(ProductType.getById(settle.getProductType())).getProductName() + " - Rollback Settled - " + settle.getTransferCode() + " : - " + amount;
            success = this.userMoneyService.reward(settle.getUsername(), (long)(-amount), Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData, true);
            if (!success) {
                logger.error("Failed to process rollback Settled for transaction Code: {}", (Object)settle.getTransferCode());
                return 0;
            }
            success = this.settleGame568WinDao.updateStatus(settle.getTransferCode(), Status.Rollback);
            if (!success) {
                logger.error("Failed to process set rollback  for transaction Code: {}", (Object)settle.getTransferCode());
                return 0;
            }
            success = this.transactionGame568WinDAO.updateStatusTransaction(settle.getTransferCode(), Status.Running);
            if (!success) {
                logger.error("Failed to Update transaction for transaction Code: {}", (Object)settle.getTransferCode());
            }
            return 1;
        }
        double amount = 0.0;
        for (TransactionGame568Win transaction : transactions) {
            if (transaction.getStatus() != Status.Cancel) continue;
            amount += transaction.getAmount();
        }
        if (amount == 0.0) {
            return 1;
        }
        String description = ProductType.getById(rollbackData.getProductType()).getProductName() + " - Rollback Bet - " + rollbackData.getTransferCode() + " : - " + amount;
        success = this.userMoneyService.reward(rollbackData.getUsername(), (long)(-amount), Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData, true);
        if (!success) {
            logger.error("Failed to process rollback Cancel for transaction Code: {}", (Object)rollbackData.getTransferCode());
            return 0;
        }
        success = this.transactionGame568WinDAO.updateStatusTransaction(rollbackData.getTransferCode(), Status.Running);
        if (!success) {
            logger.error("Failed to Update transaction for transaction Code: {}", (Object)rollbackData.getTransferCode());
        }
        return 1;
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

    @Override
    public int Bonus(Bonus bonus) {
        BonusGame568Win bonusGame568Win = this.bonusGame568WinDao.getBonus(bonus.getTransferCode(), bonus.getTransactionId());
        if (bonusGame568Win != null) {
            return -1;
        }
        HashMap<String, Object> metaData = new HashMap<String, Object>();
        String description = ProductType.getById(bonus.getProductType()).getProductName() + " - Bonus - " + bonus.getTransactionId() + " : " + bonus.getAmount();
        boolean success = this.userMoneyService.reward(bonus.getUsername(), (long)bonus.getAmount(), Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData);
        if (!success) {
            logger.error("Failed to process Bonus action for transaction Code: {}", (Object)bonus.getTransferCode());
            return 0;
        }
        success = this.bonusGame568WinDao.createBonus(bonus.toBonusGame568Win());
        if (!success) {
            logger.error("Failed to Update transaction for transaction Code: {}", (Object)bonus.getTransferCode());
        }
        return 1;
    }

    @Override
    public int ReturnStake(ReturnStake returnStake) {
        TransactionGame568Win transaction = this.getTransactionBy(returnStake.getTransferCode(), returnStake.getTransactionId());
        if (transaction == null) {
            return -1;
        }
        if (transaction.getStatus() == Status.Cancel) {
            return -3;
        }
        ReturnStakeGame568Win returnStakeRecord = this.returnStakeGame568WinDao.getReturnStake(returnStake.getTransferCode(), returnStake.getTransactionId());
        if (returnStakeRecord != null) {
            return -2;
        }
        HashMap<String, Object> metaData = new HashMap<String, Object>();
        String description = Objects.requireNonNull(ProductType.getById(returnStake.getProductType())).getProductName() + " ReturnStake - " + returnStake.getTransactionId() + " : " + returnStake.getCurrentStake();
        boolean success = this.userMoneyService.reward(returnStake.getUsername(), (long)returnStake.getCurrentStake(), Games.LIVE_GAME.getName(), Games.LIVE_GAME.getName(), description, metaData);
        if (!success) {
            logger.error("Failed to process Bonus action for transaction ID: {}", (Object)returnStake.getTransactionId());
            return 0;
        }
        success = this.returnStakeGame568WinDao.createReturnStake(returnStake.getReturnStake());
        if (!success) {
            logger.error("Failed to Update transaction Stake for transaction ID: {}", (Object)returnStake.getTransactionId());
            return 0;
        }
        return 1;
    }

    @Override
    public TransactionGame568Win GetBetStatus(String TransferCode, String TransactionId) {
        return this.transactionGame568WinDAO.getTransactionByTransferCodeAndTransactionId(TransferCode, TransactionId);
    }
}

