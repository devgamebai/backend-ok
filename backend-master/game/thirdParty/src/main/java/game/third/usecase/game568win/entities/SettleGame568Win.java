/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.entities;

import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.model.ExtraInfo;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;

public class SettleGame568Win {
    private String transferCode;
    private double winLoss;
    private int resultType;
    private String resultTime;
    private double commissionStake;
    private String gameResult;
    private String companyKey;
    private String username;
    private int productType;
    private int gameType;
    private int gpid;
    private boolean isCashOut;
    private ExtraInfo extraInfo;
    private SeamlessGameExtraInfo seamlessGameExtraInfo;
    private Status status;

    public String getTransferCode() {
        return this.transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
    }

    public double getWinLoss() {
        return this.winLoss;
    }

    public void setWinLoss(double winLoss) {
        this.winLoss = winLoss;
    }

    public int getResultType() {
        return this.resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public String getResultTime() {
        return this.resultTime;
    }

    public void setResultTime(String resultTime) {
        this.resultTime = resultTime;
    }

    public double getCommissionStake() {
        return this.commissionStake;
    }

    public void setCommissionStake(double commissionStake) {
        this.commissionStake = commissionStake;
    }

    public String getGameResult() {
        return this.gameResult;
    }

    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
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

    public int getGpid() {
        return this.gpid;
    }

    public void setGpid(int gpid) {
        this.gpid = gpid;
    }

    public boolean isCashOut() {
        return this.isCashOut;
    }

    public void setCashOut(boolean cashOut) {
        this.isCashOut = cashOut;
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

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

