/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.maxpay;

public class ReCheckMaxpayResponse {
    private String response_code;
    private String response_message;
    private String code;
    private double card_amount;
    private double net_amount;

    public ReCheckMaxpayResponse(String response_code, String response_message, String code, int card_amount, int net_amount) {
        this.response_code = response_code;
        this.response_message = response_message;
        this.code = code;
        this.card_amount = card_amount;
        this.net_amount = net_amount;
    }

    public ReCheckMaxpayResponse() {
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
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
}

