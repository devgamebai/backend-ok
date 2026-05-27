/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay._1pay;

import com.vinplay._1pay.StatusObj;

public class BuyCardResponseObj {
    private StatusObj status;
    private String transactionId;
    private String encryptCards;
    private String dataSign;

    public StatusObj getStatus() {
        return this.status;
    }

    public void setStatus(StatusObj status) {
        this.status = status;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getEncryptCards() {
        return this.encryptCards;
    }

    public void setEncryptCards(String encryptCards) {
        this.encryptCards = encryptCards;
    }

    public String getDataSign() {
        return this.dataSign;
    }

    public void setDataSign(String dataSign) {
        this.dataSign = dataSign;
    }
}

