/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.config;

public class GSCConfig {
    private String operator_code;
    private String operatorUrl;
    private String secret_key;
    private String username;
    private String password;
    private String currency;
    private double exchangeRate;
    private int tax;
    private String operatorLobbyUrl;

    public String getOperatorLobbyUrl() {
        return this.operatorLobbyUrl;
    }

    public int getTax() {
        return this.tax;
    }

    public void setTax(int tax) {
        this.tax = tax;
    }

    public void setOperatorLobbyUrl(String operatorLobbyUrl) {
        this.operatorLobbyUrl = operatorLobbyUrl;
    }

    public String getOperatorCode() {
        return this.operator_code;
    }

    public String getSecretKey() {
        return this.secret_key;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setOperatorCode(String operator_code) {
        this.operator_code = operator_code;
    }

    public void setSecretKey(String secret_key) {
        this.secret_key = secret_key;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOperatorUrl() {
        return this.operatorUrl;
    }

    public void setOperatorUrl(String operatorUrl) {
        this.operatorUrl = operatorUrl;
    }

    public String getOperator_code() {
        return this.operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getSecret_key() {
        return this.secret_key;
    }

    public void setSecret_key(String secret_key) {
        this.secret_key = secret_key;
    }

    public GSCConfig(String operator_code, String secret_key, String username, String password) {
        this.operator_code = operator_code;
        this.secret_key = secret_key;
        this.username = username;
        this.password = password;
    }

    public GSCConfig() {
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getExchangeRate() {
        return this.exchangeRate;
    }

    public void setExchangeRate(double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}

