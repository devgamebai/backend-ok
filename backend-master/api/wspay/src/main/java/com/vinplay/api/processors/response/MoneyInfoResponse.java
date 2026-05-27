/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.api.processors.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MoneyInfoResponse {
    private int errorCode;
    private String nickname;
    private long currentMoney;

    public MoneyInfoResponse(int errorCode, String nickname, long currentMoney) {
        this.errorCode = errorCode;
        this.nickname = nickname;
        this.currentMoney = currentMoney;
    }

    public MoneyInfoResponse() {
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            return "";
        }
    }
}

