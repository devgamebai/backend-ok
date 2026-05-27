/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.abtpay;

public class AbtRecheckResponse {
    private String response_code;
    private String response_message;
    private String code;
    private double amount;
    private double responseAmount;

    public AbtRecheckResponse() {
    }

    public AbtRecheckResponse(String response_code, String response_message, String code, double amount, double responseAmount) {
        this.response_code = response_code;
        this.response_message = response_message;
        this.code = code;
        this.amount = amount;
        this.responseAmount = responseAmount;
    }

    public String getResponse_code() {
        return this.response_code;
    }

    public void setResponse_code(String response_code) {
        this.response_code = response_code;
    }

    public String getResponse_message() {
        return this.response_message;
    }

    public void setResponse_message(String response_message) {
        this.response_message = response_message;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getResponseAmount() {
        return this.responseAmount;
    }

    public void setResponseAmount(double responseAmount) {
        this.responseAmount = responseAmount;
    }
}

