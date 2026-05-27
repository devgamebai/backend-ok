/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate;

public class ChargeRespPaygate {
    private String amount;
    private String serial;
    private String message;
    private String status;
    private String transId;

    public ChargeRespPaygate() {
    }

    public ChargeRespPaygate(String amount, String serial, String message, String status, String transId) {
        this.amount = amount;
        this.serial = serial;
        this.message = message;
        this.status = status;
        this.transId = transId;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }
}

