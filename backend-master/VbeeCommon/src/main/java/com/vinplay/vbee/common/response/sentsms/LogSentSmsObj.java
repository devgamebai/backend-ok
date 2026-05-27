/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.sentsms;

public class LogSentSmsObj {
    public String transId;
    public String smsContent;
    public String phoneNumber;
    public String status;
    public String message;
    public String transTime;

    public LogSentSmsObj() {
    }

    public LogSentSmsObj(String transId, String smsContent, String phoneNumber, String status, String message, String transTime) {
        this.transId = transId;
        this.smsContent = smsContent;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.message = message;
        this.transTime = transTime;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String getSmsContent() {
        return this.smsContent;
    }

    public void setSmsContent(String smsContent) {
        this.smsContent = smsContent;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTransTime() {
        return this.transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }
}

