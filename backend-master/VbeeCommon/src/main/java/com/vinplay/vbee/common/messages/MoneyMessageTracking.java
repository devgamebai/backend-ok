/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import java.io.Serializable;

public class MoneyMessageTracking
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String messageId;
    private long afterMoney;
    private String moneyType;

    public MoneyMessageTracking(String messageId, long afterMoney, String moneyType) {
        this.messageId = messageId;
        this.afterMoney = afterMoney;
        this.moneyType = moneyType;
    }

    public MoneyMessageTracking() {
    }

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getAfterMoney() {
        return this.afterMoney;
    }

    public void setAfterMoney(long afterMoney) {
        this.afterMoney = afterMoney;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }
}

