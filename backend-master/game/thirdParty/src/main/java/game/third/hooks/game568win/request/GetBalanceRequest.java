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

public class GetBalanceRequest {
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

    public static GetBalanceRequest fromJson(String json) throws IOException {
        return (GetBalanceRequest)new ObjectMapper().readValue(json, GetBalanceRequest.class);
    }

    public static boolean validate(GetBalanceRequest balanceRequest) {
        if (balanceRequest.getCompanyKey() == null || balanceRequest.getCompanyKey().isEmpty()) {
            return false;
        }
        String username = balanceRequest.getUsername();
        if (username == null || username.length() > 20 || !username.matches("[a-zA-Z0-9_]+")) {
            return false;
        }
        return balanceRequest.getProductType() != 0;
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
}

