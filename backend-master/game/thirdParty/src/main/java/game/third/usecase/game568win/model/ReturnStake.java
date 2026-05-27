/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.model;

import game.third.usecase.game568win.entities.ReturnStakeGame568Win;
import java.time.LocalDateTime;

public class ReturnStake {
    private String companyKey;
    private String username;
    private double currentStake;
    private LocalDateTime returnStakeTime;
    private int productType;
    private int gameType;
    private String transferCode;
    private String transactionId;

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

    public double getCurrentStake() {
        return this.currentStake;
    }

    public void setCurrentStake(double currentStake) {
        this.currentStake = currentStake;
    }

    public LocalDateTime getReturnStakeTime() {
        return this.returnStakeTime;
    }

    public void setReturnStakeTime(LocalDateTime returnStakeTime) {
        this.returnStakeTime = returnStakeTime;
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

    public ReturnStakeGame568Win getReturnStake() {
        ReturnStakeGame568Win returnStake = new ReturnStakeGame568Win();
        returnStake.setCompanyKey(this.companyKey);
        returnStake.setUsername(this.username);
        returnStake.setCurrentStake(this.currentStake);
        returnStake.setReturnStakeTime(this.returnStakeTime);
        returnStake.setProductType(this.productType);
        returnStake.setGameType(this.gameType);
        returnStake.setTransferCode(this.transferCode);
        returnStake.setTransactionId(this.transactionId);
        return returnStake;
    }
}

