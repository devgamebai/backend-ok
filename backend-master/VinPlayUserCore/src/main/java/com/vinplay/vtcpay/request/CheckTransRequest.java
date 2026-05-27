/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtcpay.request;

public class CheckTransRequest {
    private String command;
    private String partnerId;
    private String requestId;
    private String sign;

    public CheckTransRequest(String command, String partnerId, String requestId, String sign) {
        this.command = command;
        this.partnerId = partnerId;
        this.requestId = requestId;
        this.sign = sign;
    }

    public CheckTransRequest() {
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

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

