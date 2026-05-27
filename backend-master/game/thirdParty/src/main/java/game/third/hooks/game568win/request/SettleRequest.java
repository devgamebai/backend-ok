/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.hooks.game568win.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.third.usecase.game568win.model.ExtraInfo;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import game.third.usecase.game568win.request.SettleData;
import java.io.IOException;

public class SettleRequest {
    @JsonProperty(value="TransferCode")
    private String transferCode;
    @JsonProperty(value="WinLoss")
    private double winLoss;
    @JsonProperty(value="ResultType")
    private int resultType;
    @JsonProperty(value="ResultTime")
    private String resultTime;
    @JsonProperty(value="CommissionStake")
    private double commissionStake;
    @JsonProperty(value="GameResult")
    private String gameResult;
    @JsonProperty(value="CompanyKey")
    private String companyKey;
    @JsonProperty(value="Username")
    private String username;
    @JsonProperty(value="ProductType")
    private int productType;
    @JsonProperty(value="GameType")
    private int gameType;
    @JsonProperty(value="Gpid")
    private int gpid;
    @JsonProperty(value="IsCashOut")
    private boolean isCashOut;
    @JsonProperty(value="ExtraInfo")
    private ExtraInfo extraInfo;
    @JsonProperty(value="SeamlessGameExtraInfo")
    private SeamlessGameExtraInfo seamlessGameExtraInfo;

    public static SettleRequest fromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return (SettleRequest)objectMapper.readValue(json, SettleRequest.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validate(SettleRequest settleRequest) {
        if (settleRequest.getCompanyKey() == null || settleRequest.getCompanyKey().isEmpty()) {
            return false;
        }
        if (settleRequest.getUsername() == null || !settleRequest.getUsername().matches("^[a-zA-Z0-9_]{1,20}$")) {
            return false;
        }
        if (settleRequest.getProductType() <= 0) {
            return false;
        }
        if (settleRequest.getCommissionStake() < 0.0) {
            return false;
        }
        if (settleRequest.getTransferCode() == null || settleRequest.getTransferCode().isEmpty()) {
            return false;
        }
        return settleRequest.getResultTime() != null;
    }

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

    public SettleData getTransaction() {
        SettleData settleData = new SettleData();
        settleData.setTransferCode(this.transferCode);
        settleData.setWinLoss(this.winLoss);
        settleData.setResultType(this.resultType);
        settleData.setResultTime(this.resultTime);
        settleData.setCommissionStake(this.commissionStake);
        settleData.setGameResult(this.gameResult);
        settleData.setCompanyKey(this.companyKey);
        settleData.setUsername(this.username);
        settleData.setProductType(this.productType);
        settleData.setGameType(this.gameType);
        settleData.setGpid(this.gpid);
        settleData.setCashOut(this.isCashOut);
        settleData.setExtraInfo(this.extraInfo);
        settleData.setSeamlessGameExtraInfo(this.seamlessGameExtraInfo);
        return settleData;
    }
}

