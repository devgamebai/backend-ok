/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogMoneyUserExtraMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String nickname;
    private String actionName;
    private String serviceName;
    private long currentMoney;
    private long moneyExchange;
    private String moneyType;
    private String description;
    private long fee;
    private boolean vp;
    private long transId;
    private boolean isBot;
    private int queryType;

    public LogMoneyUserExtraMessage(int userId, String nickname, String actionName, String serviceName, long currentMoney, long moneyExchange, String moneyType, String description, long fee, boolean vp, long transId, boolean isBot, int queryType) {
        this.userId = userId;
        this.nickname = nickname;
        this.actionName = actionName;
        this.serviceName = serviceName;
        this.currentMoney = currentMoney;
        this.moneyExchange = moneyExchange;
        this.moneyType = moneyType;
        this.description = description;
        this.fee = fee;
        this.vp = vp;
        this.transId = transId;
        this.isBot = isBot;
        this.queryType = queryType;
    }

    public long getTransId() {
        return this.transId;
    }

    public void setTransId(long transId) {
        this.transId = transId;
    }

    public boolean isBot() {
        return this.isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public int getQueryType() {
        return this.queryType;
    }

    public void setQueryType(int queryType) {
        this.queryType = queryType;
    }

    public boolean isVp() {
        return this.vp;
    }

    public void setVp(boolean vp) {
        this.vp = vp;
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

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
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

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }
}

