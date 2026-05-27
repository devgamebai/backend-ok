/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.megacard;

public class MegaCardResponse {
    public String referenceId;
    public String nickName;
    public String provider;
    public String pin;
    public String serial;
    public int amount;
    public int money;
    public int status;
    public String message;
    public int code;
    public String timelog;
    public String update_time;
    public String partner;
    public String platform;

    public MegaCardResponse() {
    }

    public MegaCardResponse(String referenceId, String nickName, String provider, String pin, String serial, int amount, int money, int status, String message, int code, String timelog, String update_time, String partner, String platform) {
        this.referenceId = referenceId;
        this.nickName = nickName;
        this.provider = provider;
        this.pin = pin;
        this.serial = serial;
        this.amount = amount;
        this.money = money;
        this.status = status;
        this.message = message;
        this.code = code;
        this.timelog = timelog;
        this.update_time = update_time;
        this.partner = partner;
        this.platform = platform;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getMoney() {
        return this.money;
    }

    public void setMoney(int money) {
        this.money = money;
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

    public String getTimelog() {
        return this.timelog;
    }

    public void setTimelog(String timelog) {
        this.timelog = timelog;
    }

    public String getUpdate_time() {
        return this.update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }

    public String getPartner() {
        return this.partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}

