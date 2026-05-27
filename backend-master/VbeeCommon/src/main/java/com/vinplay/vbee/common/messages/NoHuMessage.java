/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.MoneyMessageInGame;

public class NoHuMessage
extends MoneyMessageInGame {
    private static final long serialVersionUID = 1L;
    private String potName;
    private long potValue;

    public NoHuMessage() {
    }

    public NoHuMessage(String id, int userId, String nickname, String actionName, long afterMoneyUse, long afterMoney, long moneyExchange, String moneyType, long freezeMoney, String sessionId, long fee, int moneyVP, int vp, String potName, long potValue) {
        super(id, userId, nickname, actionName, afterMoneyUse, afterMoney, moneyExchange, moneyType, freezeMoney, sessionId, fee, moneyVP, vp);
        this.potName = potName;
        this.potValue = potValue;
    }

    public long getPotValue() {
        return this.potValue;
    }

    public void setPotValue(long potValue) {
        this.potValue = potValue;
    }

    public String getPotName() {
        return this.potName;
    }

    public void setPotName(String potName) {
        this.potName = potName;
    }
}

