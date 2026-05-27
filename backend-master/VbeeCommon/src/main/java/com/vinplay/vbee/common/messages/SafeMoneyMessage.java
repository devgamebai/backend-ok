/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMoneyMessage;

public class SafeMoneyMessage
extends BaseMoneyMessage {
    private static final long serialVersionUID = 1L;
    private long safeMoney;

    public SafeMoneyMessage() {
    }

    public SafeMoneyMessage(String id, int userId, String nickname, String actionName, long afterMoneyUse, long afterMoney, long moneyExchange, String moneyType, long safeMoney) {
        super(id, userId, nickname, actionName, afterMoneyUse, afterMoney, moneyExchange, moneyType);
        this.safeMoney = safeMoney;
    }

    public long getSafeMoney() {
        return this.safeMoney;
    }

    public void setSafeMoney(long safeMoney) {
        this.safeMoney = safeMoney;
    }
}

