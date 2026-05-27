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
import game.third.usecase.game568win.model.Bonus;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BonusRequest {
    @JsonProperty(value="CompanyKey")
    private String companyKey;
    @JsonProperty(value="Username")
    private String username;
    @JsonProperty(value="Amount")
    private double amount;
    @JsonProperty(value="BonusTime")
    private String bonusTime;
    @JsonProperty(value="IsGameProviderPromotion")
    private boolean isGameProviderPromotion;
    @JsonProperty(value="ProductType")
    private int productType;
    @JsonProperty(value="GameType")
    private int gameType;
    @JsonProperty(value="TransferCode")
    private String transferCode;
    @JsonProperty(value="TransactionId")
    private String transactionId;
    @JsonProperty(value="GameId")
    private int gameId;
    @JsonProperty(value="Gpid")
    private int gpid;
    @JsonProperty(value="SeamlessGameExtraInfo")
    private SeamlessGameExtraInfo seamlessGameExtraInfo;

    public static BonusRequest fromJson(String json) {
        try {
            return (BonusRequest)new ObjectMapper().readValue(json, BonusRequest.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validate(BonusRequest bonusRequest) {
        if (bonusRequest.companyKey == null || bonusRequest.companyKey.isEmpty()) {
            return false;
        }
        if (bonusRequest.productType <= 0) {
            return false;
        }
        if (bonusRequest.username == null || !bonusRequest.username.matches("^[a-zA-Z0-9_]{1,20}$")) {
            return false;
        }
        if (bonusRequest.amount <= 0.0) {
            return false;
        }
        if (bonusRequest.transferCode == null || bonusRequest.transferCode.isEmpty()) {
            return false;
        }
        if (bonusRequest.transactionId == null || bonusRequest.transactionId.isEmpty()) {
            return false;
        }
        return bonusRequest.bonusTime != null;
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

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getBonusTime() {
        return this.bonusTime;
    }

    public void setBonusTime(String bonusTime) {
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

    public Bonus getBonus() {
        Bonus bonus = new Bonus();
        bonus.setAmount(this.amount);
        bonus.setBonusTime(LocalDateTime.parse(this.bonusTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        bonus.setCompanyKey(this.companyKey);
        bonus.setGameId(this.gameId);
        bonus.setGameType(this.gameType);
        bonus.setGpid(this.gpid);
        bonus.setGameProviderPromotion(this.isGameProviderPromotion);
        bonus.setProductType(this.productType);
        bonus.setSeamlessGameExtraInfo(this.seamlessGameExtraInfo);
        bonus.setTransactionId(this.transactionId);
        bonus.setTransferCode(this.transferCode);
        bonus.setUsername(this.username);
        return bonus;
    }
}

