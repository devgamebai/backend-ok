/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.lucky79;

public class ChargeLucky79Response {
    private String code;
    private String message;
    private String txn_id;
    private double card_amount;
    private double net_amount;

    public ChargeLucky79Response(String code, String message, String txn_id, int card_amount, int net_amount) {
        this.code = code;
        this.message = message;
        this.txn_id = txn_id;
    }

    public ChargeLucky79Response() {
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

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

