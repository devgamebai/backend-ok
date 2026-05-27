/*
 * Decompiled with CFR 0.152.
 */
package com.payment.config;

public class SieuTocConfig {
    private String baseUrl;
    private String bankCallbackLink;
    private String cardCallbackLink;
    private String bankOutCallbackLink;
    private String apiKey;
    private String pin;

    public String getBankCallbackLink() {
        return this.bankCallbackLink;
    }

    public void setBankCallbackLink(String bankCallbackLink) {
        this.bankCallbackLink = bankCallbackLink;
    }

    public String getCardCallbackLink() {
        return this.cardCallbackLink;
    }

    public void setCardCallbackLink(String cardCallbackLink) {
        this.cardCallbackLink = cardCallbackLink;
    }

    public String getBankOutCallbackLink() {
        return this.bankOutCallbackLink;
    }

    public void setBankOutCallbackLink(String bankOutCallbackLink) {
        this.bankOutCallbackLink = bankOutCallbackLink;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

