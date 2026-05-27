/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.abtpay;

public class AbtChargeResponse {
    private String status;
    private String message;
    private String transactionId;
    private double amount;
    private double netAmount;

    public AbtChargeResponse() {
    }

    public AbtChargeResponse(String status, String message, String transactionId, double amount, double netAmount) {
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
        this.amount = amount;
        this.netAmount = netAmount;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getNetAmount() {
        return this.netAmount;
    }

    public void setNetAmount(double netAmount) {
        this.netAmount = netAmount;
    }
}

