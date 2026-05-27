/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.sentsms;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogSentSmsMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String transId;
    private String tel;
    private String sms;
    private String partnerTransId;
    private String status;
    private String message;
    private String transTime;

    public String getTel() {
        return this.tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getTransTime() {
        return this.transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public String getSms() {
        return this.sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public String getTransId() {
        return this.transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
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

    public String getPartnerTransId() {
        return this.partnerTransId;
    }

    public void setPartnerTransId(String partnerTransId) {
        this.partnerTransId = partnerTransId;
    }
}

