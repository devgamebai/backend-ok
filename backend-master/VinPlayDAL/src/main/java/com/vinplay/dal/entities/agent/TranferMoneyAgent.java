/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.agent;

public class TranferMoneyAgent {
    private String nicknameSend;
    private String nicknameReceive;
    private long moneySend;
    private long moneyReceive;
    private int status;

    public TranferMoneyAgent() {
    }

    public TranferMoneyAgent(String nicknameSend, String nicknameReceive, long moneySend, long moneyReceive, int status) {
        this.nicknameSend = nicknameSend;
        this.nicknameReceive = nicknameReceive;
        this.moneySend = moneySend;
        this.moneyReceive = moneyReceive;
        this.status = status;
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

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

