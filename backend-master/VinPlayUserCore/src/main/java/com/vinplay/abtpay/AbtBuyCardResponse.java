/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.abtpay;

public class AbtBuyCardResponse {
    private String code;
    private int card_amount;
    private String expired_at;
    private String pin;
    private String seri;
    private String txn_id;
    private String checksum;

    public AbtBuyCardResponse() {
    }

    public AbtBuyCardResponse(String code, int card_amount, String expired_at, String pin, String seri, String txn_id, String checksum) {
        this.code = code;
        this.card_amount = card_amount;
        this.expired_at = expired_at;
        this.pin = pin;
        this.seri = seri;
        this.txn_id = txn_id;
        this.checksum = checksum;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCard_amount() {
        return this.card_amount;
    }

    public void setCard_amount(int card_amount) {
        this.card_amount = card_amount;
    }

    public String getExpired_at() {
        return this.expired_at;
    }

    public void setExpired_at(String expired_at) {
        this.expired_at = expired_at;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getSeri() {
        return this.seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

    public String getTxn_id() {
        return this.txn_id;
    }

    public void setTxn_id(String txn_id) {
        this.txn_id = txn_id;
    }

    public String getChecksum() {
        return this.checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}

