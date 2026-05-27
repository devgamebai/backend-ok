/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.dichvuthe.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class CardResponse {
    private List<Card> cards;
    private long currentMoney;
    private int errCode;
    private String errMsg;

    public CardResponse(int code, String message) {
        this.errCode = code;
        this.errMsg = message;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return "{\"errCode\":-1,\"errMsg\":\"failed\"}";
        }
    }

    public List<Card> getCards() {
        return this.cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public int getErrCode() {
        return this.errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getErrMsg() {
        return this.errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }

    public class Card {
        private int amount;
        private String telco;
        private String serial;
        private String pin;

        public int getAmount() {
            return this.amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getTelco() {
            return this.telco;
        }

        public void setTelco(String telco) {
            this.telco = telco;
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
    }
}

