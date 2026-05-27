/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities;

public class LogSMSPlusCheckMO {
    private String mobile;
    private String moMessage;
    private int amount;
    private String shortCode;
    private int code;
    private String des;
    private String timeLog;

    public LogSMSPlusCheckMO(String mobile, String moMessage, int amount, String shortCode, int code, String des, String timeLog) {
        this.mobile = mobile;
        this.moMessage = moMessage;
        this.amount = amount;
        this.shortCode = shortCode;
        this.code = code;
        this.des = des;
        this.timeLog = timeLog;
    }

    public LogSMSPlusCheckMO() {
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMoMessage() {
        return this.moMessage;
    }

    public void setMoMessage(String moMessage) {
        this.moMessage = moMessage;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getShortCode() {
        return this.shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDes() {
        return this.des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getTimeLog() {
        return this.timeLog;
    }

    public void setTimeLog(String timeLog) {
        this.timeLog = timeLog;
    }
}

