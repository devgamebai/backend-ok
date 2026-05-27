/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtc;

public class VTCRechargeResponse {
    private long responseCode;
    private int status;
    private String description;
    private String dataInfo;
    private int amount;
    private int code;

    public VTCRechargeResponse(long responseCode, int status, String description, String dataInfo, int amount, int code) {
        this.responseCode = responseCode;
        this.status = status;
        this.description = description;
        this.dataInfo = dataInfo;
        this.amount = amount;
        this.code = code;
    }

    public VTCRechargeResponse() {
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(long responseCode) {
        this.responseCode = responseCode;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDataInfo() {
        return this.dataInfo;
    }

    public void setDataInfo(String dataInfo) {
        this.dataInfo = dataInfo;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}

