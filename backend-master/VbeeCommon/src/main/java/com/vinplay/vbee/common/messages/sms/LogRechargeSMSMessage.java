/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.sms;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogRechargeSMSMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public String nickname;
    public String mobile;
    public String moMessage;
    public int amount;
    public String shortCode;
    public String errorCode;
    public String errorMessage;
    public String requestId;
    public String requestTime;
    public int code;
    public String des;
    public int money;
    public int smsType;

    public LogRechargeSMSMessage() {
    }

    public LogRechargeSMSMessage(String nickname, String mobile, String moMessage, int amount, String shortCode, String errorCode, String errorMessage, String requestId, String requestTime, int code, String des, int money, int smsType) {
        this.nickname = nickname;
        this.mobile = mobile;
        this.moMessage = moMessage;
        this.amount = amount;
        this.shortCode = shortCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.requestTime = requestTime;
        this.code = code;
        this.des = des;
        this.money = money;
        this.smsType = smsType;
    }
}

