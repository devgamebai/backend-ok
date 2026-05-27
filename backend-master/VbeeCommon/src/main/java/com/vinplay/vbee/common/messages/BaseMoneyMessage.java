/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public abstract class BaseMoneyMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String nickname;
    private String actionName;
    private long afterMoneyUse;
    private long afterMoney;
    private long moneyExchange;
    private String moneyType;

    public BaseMoneyMessage(String id, int userId, String nickname, String actionName, long afterMoneyUse, long afterMoney, long moneyExchange, String moneyType) {
        this.id = id;
        this.userId = userId;
        this.nickname = nickname;
        this.actionName = actionName;
        this.afterMoneyUse = afterMoneyUse;
        this.afterMoney = afterMoney;
        this.moneyExchange = moneyExchange;
        this.moneyType = moneyType;
    }

    public BaseMoneyMessage() {
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getActionName() {
        return this.actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public long getAfterMoneyUse() {
        return this.afterMoneyUse;
    }

    public void setAfterMoneyUse(long afterMoneyUse) {
        this.afterMoneyUse = afterMoneyUse;
    }

    public long getAfterMoney() {
        return this.afterMoney;
    }

    public void setAfterMoney(long afterMoney) {
        this.afterMoney = afterMoney;
    }

    public long getMoneyExchange() {
        return this.moneyExchange;
    }

    public void setMoneyExchange(long moneyExchange) {
        this.moneyExchange = moneyExchange;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }
}

