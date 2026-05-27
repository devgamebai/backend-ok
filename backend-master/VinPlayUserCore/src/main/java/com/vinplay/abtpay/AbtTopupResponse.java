/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.abtpay;

public class AbtTopupResponse {
    private String code;
    private String message;
    private String txn_id;

    public AbtTopupResponse() {
    }

    public AbtTopupResponse(String code, String message, String txn_id) {
        this.code = code;
        this.message = message;
        this.txn_id = txn_id;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
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
}

