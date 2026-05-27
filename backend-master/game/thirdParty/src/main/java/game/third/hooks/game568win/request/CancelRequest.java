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
import game.third.usecase.game568win.request.CancelData;
import java.io.IOException;

public class CancelRequest {
    @JsonProperty(value="CompanyKey")
    private String companyKey;
    @JsonProperty(value="Username")
    private String username;
    @JsonProperty(value="TransferCode")
    private String transferCode;
    @JsonProperty(value="ProductType")
    private int productType;
    @JsonProperty(value="GameType")
    private int gameType;
    @JsonProperty(value="IsCancelAll")
    private boolean isCancelAll;
    @JsonProperty(value="TransactionId")
    private String transactionId;
    @JsonProperty(value="Gpid")
    private int gpid;
    @JsonProperty(value="ExtraInfo")
    private ExtraInfo extraInfo;

    public static CancelRequest fromJson(String json) {
        try {
            return (CancelRequest)new ObjectMapper().readValue(json, CancelRequest.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validate(CancelRequest cancelRequest) {
        if (cancelRequest.companyKey == null || cancelRequest.companyKey.isEmpty()) {
            return false;
        }
        if (cancelRequest.productType <= 0) {
            return false;
        }
        if (cancelRequest.gameType <= 0) {
            return false;
        }
        if (cancelRequest.username == null || cancelRequest.username.isEmpty() || cancelRequest.username.length() > 20 || !cancelRequest.username.matches("[a-zA-Z0-9_]+")) {
            return false;
        }
        if (cancelRequest.transferCode == null || cancelRequest.transferCode.isEmpty()) {
            return false;
        }
        if (cancelRequest.transactionId == null || cancelRequest.transactionId.isEmpty()) {
            return false;
        }
        return cancelRequest.isCancelAll || !cancelRequest.isCancelAll;
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

    public String getTransferCode() {
        return this.transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
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

    public boolean isCancelAll() {
        return this.isCancelAll;
    }

    public void setCancelAll(boolean cancelAll) {
        this.isCancelAll = cancelAll;
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

    public ExtraInfo getExtraInfo() {
        return this.extraInfo;
    }

    public void setExtraInfo(ExtraInfo extraInfo) {
        this.extraInfo = extraInfo;
    }

    public CancelData toCancelData() {
        CancelData cancelData = new CancelData();
        cancelData.setCompanyKey(this.companyKey);
        cancelData.setUsername(this.username);
        cancelData.setTransferCode(this.transferCode);
        cancelData.setProductType(this.productType);
        cancelData.setGameType(this.gameType);
        cancelData.setCancelAll(this.isCancelAll);
        cancelData.setTransactionId(this.transactionId);
        cancelData.setGpid(this.gpid);
        cancelData.setExtraInfo(this.extraInfo);
        return cancelData;
    }
}

