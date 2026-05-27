/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.usecase.game568win.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import game.third.usecase.game568win.model.ExtraInfo;
import java.io.IOException;

public class CancelData {
    private String companyKey;
    private String username;
    private String transferCode;
    private int productType;
    private int gameType;
    private boolean isCancelAll;
    private String transactionId;
    private int gpid;
    private ExtraInfo extraInfo;

    public static CancelData fromJson(String json) {
        try {
            return (CancelData)new ObjectMapper().readValue(json, CancelData.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validate(CancelData cancelRequest) {
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
}

