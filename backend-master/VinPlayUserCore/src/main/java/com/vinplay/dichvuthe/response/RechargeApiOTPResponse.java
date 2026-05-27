/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

public class RechargeApiOTPResponse {
    private byte code;
    private String requestId;
    private String transId;
    private String url;
    private long currentMoney;
    private int fail;
    private long time;

    public RechargeApiOTPResponse() {
    }

    public RechargeApiOTPResponse(byte code, String requestId, String transId, String url, long currentMoney, int fail, long time) {
        this.code = code;
        this.requestId = requestId;
        this.transId = transId;
        this.url = url;
        this.currentMoney = currentMoney;
        this.fail = fail;
        this.time = time;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public byte getCode() {
        return this.code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }

    public int getFail() {
        return this.fail;
    }

    public void setFail(int fail) {
        this.fail = fail;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

