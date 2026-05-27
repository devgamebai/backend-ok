/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.model;

import game.third.usecase.game568win.entities.BonusGame568Win;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import java.time.LocalDateTime;

public class Bonus {
    private String companyKey;
    private String username;
    private double amount;
    private LocalDateTime bonusTime;
    private boolean isGameProviderPromotion;
    private int productType;
    private int gameType;
    private String transferCode;
    private String transactionId;
    private int gameId;
    private int gpid;
    private SeamlessGameExtraInfo seamlessGameExtraInfo;

    public String getCompanyKey() {
        return this.companyKey;
    }

    public void setCompanyKey(String companyKey) {
        this.companyKey = companyKey;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getBonusTime() {
        return this.bonusTime;
    }

    public void setBonusTime(LocalDateTime bonusTime) {
        this.bonusTime = bonusTime;
    }

    public boolean isGameProviderPromotion() {
        return this.isGameProviderPromotion;
    }

    public void setGameProviderPromotion(boolean gameProviderPromotion) {
        this.isGameProviderPromotion = gameProviderPromotion;
    }

    public int getProductType() {
        return this.productType;
    }

    public void setProductType(int productType) {
        this.productType = productType;
    }

    public int getGameType() {
        return this.gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public String getTransferCode() {
        return this.transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getGameId() {
        return this.gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getGpid() {
        return this.gpid;
    }

    public void setGpid(int gpid) {
        this.gpid = gpid;
    }

    public SeamlessGameExtraInfo getSeamlessGameExtraInfo() {
        return this.seamlessGameExtraInfo;
    }

    public void setSeamlessGameExtraInfo(SeamlessGameExtraInfo seamlessGameExtraInfo) {
        this.seamlessGameExtraInfo = seamlessGameExtraInfo;
    }

    public TransactionGame568Win toTransactionGame568Win() {
        TransactionGame568Win transaction = new TransactionGame568Win();
        transaction.setCompanyKey(this.companyKey);
        transaction.setUsername(this.username);
        transaction.setAmount(this.amount);
        transaction.setGameProviderPromotion(this.isGameProviderPromotion);
        transaction.setProductType(this.productType);
        transaction.setGameType(this.gameType);
        transaction.setTransferCode(this.transferCode);
        transaction.setTransactionId(this.transactionId);
        transaction.setGameId(this.gameId);
        transaction.setGpid(this.gpid);
        transaction.setSeamlessGameExtraInfo(this.seamlessGameExtraInfo);
        return transaction;
    }

    public BonusGame568Win toBonusGame568Win() {
        BonusGame568Win bonusGame = new BonusGame568Win();
        bonusGame.setCompanyKey(this.companyKey);
        bonusGame.setUsername(this.username);
        bonusGame.setAmount(this.amount);
        bonusGame.setBonusTime(this.bonusTime);
        bonusGame.setGameProviderPromotion(this.isGameProviderPromotion);
        bonusGame.setProductType(this.productType);
        bonusGame.setGameType(this.gameType);
        bonusGame.setTransferCode(this.transferCode);
        bonusGame.setTransactionId(this.transactionId);
        bonusGame.setGameId(this.gameId);
        bonusGame.setGpid(this.gpid);
        bonusGame.setSeamlessGameExtraInfo(this.seamlessGameExtraInfo);
        return bonusGame;
    }
}

