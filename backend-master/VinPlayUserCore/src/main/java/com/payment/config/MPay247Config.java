/*
 * Decompiled with CFR 0.152.
 */
package com.payment.config;

public class MPay247Config {
    private String urlBase;
    private String bankCallbackLink;
    private String cardCallbackLink;
    private String bankOutCallbackLink;
    private String key;
    private String privateKey;

    public String getUrlBase() {
        return this.urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

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

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}

