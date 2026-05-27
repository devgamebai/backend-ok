/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.gsc.model;

public class GSCGameRequest {
    private String operatorCode;
    private String memberAccount;
    private String password;
    private String currency;
    private String gameCode;
    private int productCode;
    private String gameType;
    private int languageCode;
    private String ip;
    private String platform;
    private String sign;
    private long requestTime;
    private String operatorLobbyUrl;

    public String getOperatorCode() {
        return this.operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getMemberAccount() {
        return this.memberAccount;
    }

    public void setMemberAccount(String memberAccount) {
        this.memberAccount = memberAccount;
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

    public String getGameCode() {
        return this.gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public int getProductCode() {
        return this.productCode;
    }

    public void setProductCode(int productCode) {
        this.productCode = productCode;
    }

    public String getGameType() {
        return this.gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getLanguageCode() {
        return this.languageCode;
    }

    public void setLanguageCode(int languageCode) {
        this.languageCode = languageCode;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public long getRequestTime() {
        return this.requestTime;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }

    public String getOperatorLobbyUrl() {
        return this.operatorLobbyUrl;
    }

    public void setOperatorLobbyUrl(String operatorLobbyUrl) {
        this.operatorLobbyUrl = operatorLobbyUrl;
    }
}

