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
import java.io.IOException;

public class GetBetStatusRequest {
    @JsonProperty(value="CompanyKey")
    private String companyKey;
    @JsonProperty(value="Username")
    private String username;
    @JsonProperty(value="ProductType")
    private int productType;
    @JsonProperty(value="GameType")
    private int gameType;
    @JsonProperty(value="TransferCode")
    private String transferCode;
    @JsonProperty(value="TransactionId")
    private String transactionId;
    @JsonProperty(value="Gpid")
    private int gpid;

    public static GetBetStatusRequest fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (GetBetStatusRequest)mapper.readValue(json, GetBetStatusRequest.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validate(GetBetStatusRequest getBetStatusRequest) {
        if (getBetStatusRequest.getCompanyKey() == null || getBetStatusRequest.getCompanyKey().isEmpty()) {
            return false;
        }
        return getBetStatusRequest.getUsername() != null && !getBetStatusRequest.getUsername().isEmpty() && getBetStatusRequest.getUsername().length() <= 20 && getBetStatusRequest.getUsername().matches("^[a-zA-Z0-9_]+$");
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

    public int getGpid() {
        return this.gpid;
    }

    public void setGpid(int gpid) {
        this.gpid = gpid;
    }
}

