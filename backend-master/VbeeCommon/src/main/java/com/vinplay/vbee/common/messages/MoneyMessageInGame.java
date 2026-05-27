/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMoneyMessage;

public class MoneyMessageInGame
extends BaseMoneyMessage {
    private static final long serialVersionUID = 1L;
    private long freezeMoney;
    private String sessionId;
    private long fee;
    private int moneyVP;
    private int vp;

    public MoneyMessageInGame() {
    }

    public MoneyMessageInGame(String id, int userId, String nickname, String actionName, long afterMoneyUse, long afterMoney, long moneyExchange, String moneyType, long freezeMoney, String sessionId, long fee, int moneyVP, int vp) {
        super(id, userId, nickname, actionName, afterMoneyUse, afterMoney, moneyExchange, moneyType);
        this.freezeMoney = freezeMoney;
        this.sessionId = sessionId;
        this.fee = fee;
        this.moneyVP = moneyVP;
        this.vp = vp;
    }

    public int getMoneyVP() {
        return this.moneyVP;
    }

    public void setMoneyVP(int moneyVP) {
        this.moneyVP = moneyVP;
    }

    public int getVp() {
        return this.vp;
    }

    public void setVp(int vp) {
        this.vp = vp;
    }

    public long getFreezeMoney() {
        return this.freezeMoney;
    }

    public void setFreezeMoney(long freezeMoney) {
        this.freezeMoney = freezeMoney;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

