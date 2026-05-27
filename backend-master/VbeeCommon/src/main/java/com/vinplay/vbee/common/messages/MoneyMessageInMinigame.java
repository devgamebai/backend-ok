/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMoneyMessage;

public class MoneyMessageInMinigame
extends BaseMoneyMessage {
    private static final long serialVersionUID = 1L;
    private long fee;
    private int moneyVP;
    private int vp;

    public MoneyMessageInMinigame() {
    }

    public MoneyMessageInMinigame(String id, int userId, String nickname, String actionName, long afterMoneyUse, long afterMoney, long moneyExchange, String moneyType, long fee, int moneyVP, int vp) {
        super(id, userId, nickname, actionName, afterMoneyUse, afterMoney, moneyExchange, moneyType);
        this.fee = fee;
        this.moneyVP = moneyVP;
        this.vp = vp;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
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
}

