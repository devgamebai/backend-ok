/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtcpay.request;

public class CheckAccountRequest {
    private String command;
    private String partnerId;
    private String nickName;
    private String sign;

    public CheckAccountRequest(String command, String partnerId, String nickName, String sign) {
        this.command = command;
        this.partnerId = partnerId;
        this.nickName = nickName;
        this.sign = sign;
    }

    public CheckAccountRequest() {
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

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

