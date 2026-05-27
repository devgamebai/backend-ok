/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 */
package game.third.usecase.config;

import com.google.gson.annotations.SerializedName;

public class Game568winConfig {
    @SerializedName(value="CompanyKey")
    private String CompanyKey;
    @SerializedName(value="Server")
    private String Server;
    @SerializedName(value="Agent")
    private String Agent;
    @SerializedName(value="AgentPassword")
    private String AgentPassword;
    @SerializedName(value="ServerId")
    private String ServerId;
    @SerializedName(value="Currency")
    private String Currency = "VNO";
    @SerializedName(value="Min")
    private int min;
    @SerializedName(value="Max")
    private int max;
    @SerializedName(value="MaxPerMatch")
    private int MaxPerMatch;
    @SerializedName(value="CasinoTableLimit")
    private int CasinoTableLimit;
    @SerializedName(value="UserGroup")
    private String UserGroup = "novip";

    public String getUserGroup() {
        return this.UserGroup;
    }

    public void setUserGroup(String userGroup) {
        this.UserGroup = userGroup;
    }

    public String getServerId() {
        return this.ServerId;
    }

    public void setServerId(String serverId) {
        this.ServerId = serverId;
    }

    public String getCompanyKey() {
        return this.CompanyKey;
    }

    public void setCompanyKey(String CompanyKey) {
        this.CompanyKey = CompanyKey;
    }

    public String getServer() {
        return this.Server;
    }

    public void setServer(String server) {
        this.Server = server;
    }

    public String getAgent() {
        return this.Agent;
    }

    public void setAgent(String agent) {
        this.Agent = agent;
    }

    public String getAgentPassword() {
        return this.AgentPassword;
    }

    public void setAgentPassword(String agentPassword) {
        this.AgentPassword = agentPassword;
    }

    public String getCurrency() {
        return this.Currency;
    }

    public void setCurrency(String currency) {
        this.Currency = currency;
    }

    public int getMin() {
        return this.min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return this.max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMaxPerMatch() {
        return this.MaxPerMatch;
    }

    public void setMaxPerMatch(int maxPerMatch) {
        this.MaxPerMatch = maxPerMatch;
    }

    public int getCasinoTableLimit() {
        return this.CasinoTableLimit;
    }

    public void setCasinoTableLimit(int casinoTableLimit) {
        this.CasinoTableLimit = casinoTableLimit;
    }
}

