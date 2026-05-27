/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.request;

public class RegisterAgent {
    private String username;
    private String password;
    private String currency;
    private int min;
    private int max;
    private int maxPerMatch;
    private int casinoTableLimit;
    private String serverId;

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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
        return this.maxPerMatch;
    }

    public void setMaxPerMatch(int maxPerMatch) {
        this.maxPerMatch = maxPerMatch;
    }

    public int getCasinoTableLimit() {
        return this.casinoTableLimit;
    }

    public void setCasinoTableLimit(int casinoTableLimit) {
        this.casinoTableLimit = casinoTableLimit;
    }

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}

