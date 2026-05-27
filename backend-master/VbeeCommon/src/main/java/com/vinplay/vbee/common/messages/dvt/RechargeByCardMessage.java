/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.dvt;

import com.vinplay.vbee.common.messages.BaseMessage;

public class RechargeByCardMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private String referenceId;
    private String provider;
    private String serial;
    private String pin;
    private int amount;
    private int status;
    private String message;
    private int code;
    private int money;
    private String timeLog;
    private String error;
    private String partner;
    private String tranId;
    private String requestId;
    private String platform;
    private String userNameMega;

    public RechargeByCardMessage(String nickname, String referenceId, String provider, String serial, String pin, int amount, int status, String message, int code, int money, String timeLog, String error, String partner, String platform, String userNameMega) {
        this.nickname = nickname;
        this.referenceId = referenceId;
        this.provider = provider;
        this.serial = serial;
        this.pin = pin;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.code = code;
        this.money = money;
        this.timeLog = timeLog;
        this.error = error;
        this.partner = partner;
        this.platform = platform;
        this.userNameMega = userNameMega;
    }

    public RechargeByCardMessage() {
    }

    public String getUserNameMega() {
        return this.userNameMega;
    }

    public void setUserNameMega(String userNameMega) {
        this.userNameMega = userNameMega;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTranId() {
        return this.tranId;
    }

    public void setTranId(String tranId) {
        this.tranId = tranId;
    }

    public String getError() {
        return this.error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getTimeLog() {
        return this.timeLog;
    }

    public void setTimeLog(String timeLog) {
        this.timeLog = timeLog;
    }

    public int getMoney() {
        return this.money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}

