/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogChuyenTienDaiLyMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String transactionId;
    private int isFreezeMoney;
    private String agentLevel1;
    private String sessionIdFreezeMoney;
    private String nicknameSend;
    private String nicknameReceive;
    private long moneySend;
    private long moneyReceive;
    private long fee;
    private String transTime;
    private int status;
    private String desSend;
    private String desReceive;

    public LogChuyenTienDaiLyMessage(String nicknameSend, String nicknameReceive, long moneySend, long moneyReceive, long fee, String transTime, int status, String desSend, String desReceive, String transactionId, int isFreezeMoney, String agentLevel1, String sessionIdFreezeMoney) {
        this.nicknameSend = nicknameSend;
        this.nicknameReceive = nicknameReceive;
        this.moneySend = moneySend;
        this.moneyReceive = moneyReceive;
        this.fee = fee;
        this.transTime = transTime;
        this.status = status;
        this.desSend = desSend;
        this.desReceive = desReceive;
        this.transactionId = transactionId;
        this.isFreezeMoney = isFreezeMoney;
        this.agentLevel1 = agentLevel1;
        this.sessionIdFreezeMoney = sessionIdFreezeMoney;
    }

    public String getSessionIdFreezeMoney() {
        return this.sessionIdFreezeMoney;
    }

    public void setSessionIdFreezeMoney(String sessionIdFreezeMoney) {
        this.sessionIdFreezeMoney = sessionIdFreezeMoney;
    }

    public String getAgentLevel1() {
        return this.agentLevel1;
    }

    public void setAgentLevel1(String agentLevel1) {
        this.agentLevel1 = agentLevel1;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getIsFreezeMoney() {
        return this.isFreezeMoney;
    }

    public void setIsFreezeMoney(int isFreezeMoney) {
        this.isFreezeMoney = isFreezeMoney;
    }

    public String getDesSend() {
        return this.desSend;
    }

    public void setDesSend(String desSend) {
        this.desSend = desSend;
    }

    public String getDesReceive() {
        return this.desReceive;
    }

    public void setDesReceive(String desReceive) {
        this.desReceive = desReceive;
    }

    public String getNicknameSend() {
        return this.nicknameSend;
    }

    public void setNicknameSend(String nicknameSend) {
        this.nicknameSend = nicknameSend;
    }

    public String getNicknameReceive() {
        return this.nicknameReceive;
    }

    public void setNicknameReceive(String nicknameReceive) {
        this.nicknameReceive = nicknameReceive;
    }

    public long getMoneySend() {
        return this.moneySend;
    }

    public void setMoneySend(long moneySend) {
        this.moneySend = moneySend;
    }

    public long getMoneyReceive() {
        return this.moneyReceive;
    }

    public void setMoneyReceive(long moneyReceive) {
        this.moneyReceive = moneyReceive;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getTransTime() {
        return this.transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

