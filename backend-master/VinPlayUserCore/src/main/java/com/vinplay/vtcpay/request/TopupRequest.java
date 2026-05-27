/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtcpay.request;

public class TopupRequest {
    private String command;
    private String partnerId;
    private String requestId;
    private String nickName;
    private String price;
    private String timeRequest;
    private String sign;

    public TopupRequest(String command, String partnerId, String requestId, String nickName, String price, String timeRequest, String sign) {
        this.command = command;
        this.partnerId = partnerId;
        this.requestId = requestId;
        this.nickName = nickName;
        this.price = price;
        this.timeRequest = timeRequest;
        this.sign = sign;
    }

    public TopupRequest() {
    }

    public String getTimeRequest() {
        return this.timeRequest;
    }

    public void setTimeRequest(String timeRequest) {
        this.timeRequest = timeRequest;
    }

    public String getCommand() {
        return this.command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getPartnerId() {
        return this.partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPrice() {
        return this.price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

