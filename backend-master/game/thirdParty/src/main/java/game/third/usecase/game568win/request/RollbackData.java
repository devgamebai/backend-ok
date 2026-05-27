/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.request;

import game.third.usecase.game568win.model.ExtraInfo;

public class RollbackData {
    private String username;
    private String transferCode;
    private int productType;
    private int gameType;
    private int gpid;
    private ExtraInfo extraInfo;

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
}

