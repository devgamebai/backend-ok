/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.topupVTCPay;

public class LogTopupVTCPay {
    private String transId;
    private String nickName;
    private int price;
    private int status;
    private String responseCode;
    private String description;
    private String timeRequest;
    private String timeResponse;

    public LogTopupVTCPay(String transId, String nickName, int price, int status, String responseCode, String description, String timeRequest, String timeResponse) {
        this.transId = transId;
        this.nickName = nickName;
        this.price = price;
        this.status = status;
        this.responseCode = responseCode;
        this.description = description;
        this.timeRequest = timeRequest;
        this.timeResponse = timeResponse;
    }

    public LogTopupVTCPay() {
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeRequest() {
        return this.timeRequest;
    }

    public void setTimeRequest(String timeRequest) {
        this.timeRequest = timeRequest;
    }

    public String getTimeResponse() {
        return this.timeResponse;
    }

    public void setTimeResponse(String timeResponse) {
        this.timeResponse = timeResponse;
    }
}

