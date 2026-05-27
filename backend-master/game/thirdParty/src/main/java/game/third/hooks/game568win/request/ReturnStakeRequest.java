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
import game.third.usecase.game568win.model.ReturnStake;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReturnStakeRequest {
    @JsonProperty(value="CompanyKey")
    private String companyKey;
    @JsonProperty(value="Username")
    private String username;
    @JsonProperty(value="CurrentStake")
    private double currentStake;
    @JsonProperty(value="ReturnStakeTime")
    private String returnStakeTime;
    @JsonProperty(value="ProductType")
    private int productType;
    @JsonProperty(value="GameType")
    private int gameType;
    @JsonProperty(value="TransferCode")
    private String transferCode;
    @JsonProperty(value="TransactionId")
    private String transactionId;

    public static boolean validate(ReturnStakeRequest returnStakeRequest) {
        if (returnStakeRequest.getCompanyKey() == null || returnStakeRequest.getCompanyKey().isEmpty()) {
            return false;
        }
        if (returnStakeRequest.getProductType() <= 0) {
            return false;
        }
        if (returnStakeRequest.getUsername() == null || !returnStakeRequest.getUsername().matches("^[a-zA-Z0-9_]{1,20}$")) {
            return false;
        }
        if (returnStakeRequest.getCurrentStake() <= 0.0) {
            return false;
        }
        if (returnStakeRequest.getTransferCode() == null || returnStakeRequest.getTransferCode().isEmpty()) {
            return false;
        }
        if (returnStakeRequest.getTransactionId() == null || returnStakeRequest.getTransactionId().isEmpty()) {
            return false;
        }
        return returnStakeRequest.getReturnStakeTime() != null;
    }

    public static ReturnStakeRequest fromJson(String string) {
        try {
            return (ReturnStakeRequest)new ObjectMapper().readValue(string, ReturnStakeRequest.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

    public double getCurrentStake() {
        return this.currentStake;
    }

    public void setCurrentStake(double currentStake) {
        this.currentStake = currentStake;
    }

    public String getReturnStakeTime() {
        return this.returnStakeTime;
    }

    public void setReturnStakeTime(String returnStakeTime) {
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

    public ReturnStake getReturnStake() {
        ReturnStake returnStake = new ReturnStake();
        returnStake.setCompanyKey(this.companyKey);
        returnStake.setUsername(this.username);
        returnStake.setCurrentStake(this.currentStake);
        returnStake.setReturnStakeTime(LocalDateTime.parse(this.returnStakeTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        returnStake.setProductType(this.productType);
        returnStake.setGameType(this.gameType);
        returnStake.setTransferCode(this.transferCode);
        returnStake.setTransactionId(this.transactionId);
        return returnStake;
    }
}

