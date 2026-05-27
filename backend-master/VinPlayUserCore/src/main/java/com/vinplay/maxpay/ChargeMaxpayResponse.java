/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.maxpay;

public class ChargeMaxpayResponse {
    private String code;
    private String message;
    private String txn_id;
    private double card_amount;
    private double net_amount;

    public ChargeMaxpayResponse(String code, String message, String txn_id, int card_amount, int net_amount) {
        this.code = code;
        this.message = message;
        this.txn_id = txn_id;
        this.card_amount = card_amount;
        this.net_amount = net_amount;
    }

    public ChargeMaxpayResponse() {
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTxn_id() {
        return this.txn_id;
    }

    public void setTxn_id(String txn_id) {
        this.txn_id = txn_id;
    }

    public double getCard_amount() {
        return this.card_amount;
    }

    public void setCard_amount(double card_amount) {
        this.card_amount = card_amount;
    }

    public double getNet_amount() {
        return this.net_amount;
    }

    public void setNet_amount(double net_amount) {
        this.net_amount = net_amount;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

