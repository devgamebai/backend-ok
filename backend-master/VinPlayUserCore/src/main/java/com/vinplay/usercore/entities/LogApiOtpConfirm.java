/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.entities;

public class LogApiOtpConfirm {
    private String nickname;
    private String mobile;
    private int amount;
    private String otp;
    private String errorCode;
    private String errorMessage;
    private String requestId;
    private String transId;
    private int code;
    private String des;
    private int money;
    private String timeLog;

    public LogApiOtpConfirm(String nickname, String mobile, int amount, String otp, String errorCode, String errorMessage, String requestId, String transId, int code, String des, int money, String timeLog) {
        this.nickname = nickname;
        this.mobile = mobile;
        this.amount = amount;
        this.otp = otp;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.transId = transId;
        this.code = code;
        this.des = des;
        this.money = money;
        this.timeLog = timeLog;
    }

    public LogApiOtpConfirm() {
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getOtp() {
        return this.otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
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

    public int getMoney() {
        return this.money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public String getTimeLog() {
        return this.timeLog;
    }

    public void setTimeLog(String timeLog) {
        this.timeLog = timeLog;
    }
}

