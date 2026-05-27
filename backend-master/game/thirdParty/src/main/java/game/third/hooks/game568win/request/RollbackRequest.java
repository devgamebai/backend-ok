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
import game.third.usecase.game568win.request.RollbackData;
import java.io.IOException;

public class RollbackRequest {
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
    @JsonProperty(value="Gpid")
    private int gpid;
    @JsonProperty(value="ExtraInfo")
    private ExtraInfo extraInfo;

    public static RollbackRequest fromJson(String string) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return (RollbackRequest)objectMapper.readValue(string, RollbackRequest.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validate(RollbackRequest rollbackRequest) {
        if (rollbackRequest.getCompanyKey() == null || rollbackRequest.getCompanyKey().isEmpty()) {
            return false;
        }
        if (rollbackRequest.getUsername() == null || !rollbackRequest.getUsername().matches("^[a-zA-Z0-9_]{1,20}$")) {
            return false;
        }
        if (rollbackRequest.getProductType() <= 0) {
            return false;
        }
        if (rollbackRequest.getGameType() <= 0) {
            return false;
        }
        return rollbackRequest.getTransferCode() != null && !rollbackRequest.getTransferCode().isEmpty();
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

    public RollbackData getRollbackData() {
        RollbackData rollbackData = new RollbackData();
        rollbackData.setTransferCode(this.transferCode);
        rollbackData.setUsername(this.username);
        rollbackData.setProductType(this.productType);
        rollbackData.setGameType(this.gameType);
        rollbackData.setGpid(this.gpid);
        rollbackData.setExtraInfo(this.extraInfo);
        return rollbackData;
    }
}

