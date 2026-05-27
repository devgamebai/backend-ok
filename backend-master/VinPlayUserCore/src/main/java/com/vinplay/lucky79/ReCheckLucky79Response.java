/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.lucky79;

public class ReCheckLucky79Response {
    private String response_code;
    private String response_message;
    private String cardSerial;
    private String status;
    private String cardType;
    private String date;
    private double card_amount;
    private double net_amount;
    private String txn_id;
    private String id;

    public ReCheckLucky79Response(String response_code, String response_message, String code, int card_amount, int net_amount, String txn_id, String cardSerial, String status, String cardType, String date, String id) {
        this.response_code = response_code;
        this.response_message = response_message;
        this.cardSerial = cardSerial;
        this.status = status;
        this.cardType = cardType;
        this.date = date;
        this.card_amount = card_amount;
        this.net_amount = net_amount;
        this.txn_id = txn_id;
        this.id = id;
    }

    public ReCheckLucky79Response() {
    }

    public String getTxn_id() {
        return this.txn_id;
    }

    public void setTxn_id(String txn_id) {
        this.txn_id = txn_id;
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

    public String getCardType() {
        return this.cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCardSerial() {
        return this.cardSerial;
    }

    public void setCardSerial(String cardSerial) {
        this.cardSerial = cardSerial;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

