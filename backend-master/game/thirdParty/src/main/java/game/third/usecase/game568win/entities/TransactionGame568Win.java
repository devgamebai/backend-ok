/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.entities;

import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.model.ExtraInfo;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import java.time.LocalDateTime;

public class TransactionGame568Win {
    private double amount;
    private String transferCode;
    private String transactionId;
    private LocalDateTime betTime;
    private String gameRoundId;
    private String gamePeriodId;
    private String orderDetail;
    private String playerIp;
    private String gameTypeName;
    private String companyKey;
    private String username;
    private int productType;
    private int gameType;
    private int gameId;
    private int gpid;
    private ExtraInfo extraInfo;
    private SeamlessGameExtraInfo seamlessGameExtraInfo;
    private Status status;
    private double returnAmount;
    private double currentStake;
    private double winLoss;
    private boolean isGameProviderPromotion;
    private boolean returnStake;

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

    public LocalDateTime getBetTime() {
        return this.betTime;
    }

    public void setBetTime(LocalDateTime betTime) {
        this.betTime = betTime;
    }

    public String getGameRoundId() {
        return this.gameRoundId;
    }

    public void setGameRoundId(String gameRoundId) {
        this.gameRoundId = gameRoundId;
    }

    public String getGamePeriodId() {
        return this.gamePeriodId;
    }

    public void setGamePeriodId(String gamePeriodId) {
        this.gamePeriodId = gamePeriodId;
    }

    public String getOrderDetail() {
        return this.orderDetail;
    }

    public void setOrderDetail(String orderDetail) {
        this.orderDetail = orderDetail;
    }

    public String getPlayerIp() {
        return this.playerIp;
    }

    public void setPlayerIp(String playerIp) {
        this.playerIp = playerIp;
    }

    public String getGameTypeName() {
        return this.gameTypeName;
    }

    public void setGameTypeName(String gameTypeName) {
        this.gameTypeName = gameTypeName;
    }

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

    public ExtraInfo getExtraInfo() {
        return this.extraInfo;
    }

    public void setExtraInfo(ExtraInfo extraInfo) {
        this.extraInfo = extraInfo;
    }

    public SeamlessGameExtraInfo getSeamlessGameExtraInfo() {
        return this.seamlessGameExtraInfo;
    }

    public void setSeamlessGameExtraInfo(SeamlessGameExtraInfo seamlessGameExtraInfo) {
        this.seamlessGameExtraInfo = seamlessGameExtraInfo;
    }

    public double getReturnAmount() {
        return this.returnAmount;
    }

    public void setReturnAmount(double returnAmount) {
        this.returnAmount = returnAmount;
    }

    public double getCurrentStake() {
        return this.currentStake;
    }

    public void setCurrentStake(double currentStake) {
        this.currentStake = currentStake;
    }

    public double getWinLoss() {
        return this.winLoss;
    }

    public void setWinLoss(double winLoss) {
        this.winLoss = winLoss;
    }

    public void setGameProviderPromotion(boolean isGameProviderPromotion) {
        this.isGameProviderPromotion = isGameProviderPromotion;
    }

    public boolean isGameProviderPromotion() {
        return this.isGameProviderPromotion;
    }

    public boolean isReturnStake() {
        return this.returnStake;
    }

    public void setReturnStake(boolean returnStake) {
        this.returnStake = returnStake;
    }
}

