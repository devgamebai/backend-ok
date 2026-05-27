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

public class ExchangeMoneyResponse {
    private String merchantTransId;
    private int errorCode;
    private String nickname;
    private long exchangeMoney;
    private long currentMoney;
    private String transNo;

    public ExchangeMoneyResponse() {
    }

    public ExchangeMoneyResponse(String merchantTransId, int errorCode, String nickname, long exchangeMoney, long currentMoney, String transNo) {
        this.merchantTransId = merchantTransId;
        this.errorCode = errorCode;
        this.nickname = nickname;
        this.exchangeMoney = exchangeMoney;
        this.currentMoney = currentMoney;
        this.transNo = transNo;
    }

    public String getTransNo() {
        return this.transNo;
    }

    public void setTransNo(String transNo) {
        this.transNo = transNo;
    }

    public String getMerchantTransId() {
        return this.merchantTransId;
    }

    public void setMerchantTransId(String merchantTransId) {
        this.merchantTransId = merchantTransId;
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

    public long getExchangeMoney() {
        return this.exchangeMoney;
    }

    public void setExchangeMoney(long exchangeMoney) {
        this.exchangeMoney = exchangeMoney;
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

