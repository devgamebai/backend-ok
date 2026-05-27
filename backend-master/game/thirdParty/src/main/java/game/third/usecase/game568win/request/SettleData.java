/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.request;

import game.third.usecase.game568win.entities.SettleGame568Win;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.model.ExtraInfo;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;

public class SettleData {
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

    public SettleGame568Win toSettleGame568Win() {
        SettleGame568Win settleGame568Win = new SettleGame568Win();
        settleGame568Win.setTransferCode(this.transferCode);
        settleGame568Win.setWinLoss(this.winLoss);
        settleGame568Win.setResultType(this.resultType);
        settleGame568Win.setResultTime(this.resultTime);
        settleGame568Win.setCommissionStake(this.commissionStake);
        settleGame568Win.setGameResult(this.gameResult);
        settleGame568Win.setCompanyKey(this.companyKey);
        settleGame568Win.setUsername(this.username);
        settleGame568Win.setProductType(this.productType);
        settleGame568Win.setGameType(this.gameType);
        settleGame568Win.setGpid(this.gpid);
        settleGame568Win.setCashOut(this.isCashOut);
        settleGame568Win.setExtraInfo(this.extraInfo);
        settleGame568Win.setSeamlessGameExtraInfo(this.seamlessGameExtraInfo);
        settleGame568Win.setStatus(Status.Settled);
        return settleGame568Win;
    }
}

