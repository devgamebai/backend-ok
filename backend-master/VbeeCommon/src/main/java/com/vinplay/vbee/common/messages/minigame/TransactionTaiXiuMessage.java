/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame;

import com.vinplay.vbee.common.messages.BaseMessage;

public class TransactionTaiXiuMessage
        extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public int userId;
    public String username;
    public int moneyType;
    public long betValue;
    public short betSide;
    public int inputTime;
    public long referenceId;
    public long prize;
    public long refund;
    public long jackpot;
    public boolean isMD5=false;

    public TransactionTaiXiuMessage() {
    }

    public TransactionTaiXiuMessage(int userId, String username, int moneyType, long betValue, short betSide, int inputTime, long referenceId) {
        this.userId = userId;
        this.username = username;
        this.moneyType = moneyType;
        this.betValue = betValue;
        this.betSide = betSide;
        this.inputTime = inputTime;
        this.referenceId = referenceId;
    }
}

