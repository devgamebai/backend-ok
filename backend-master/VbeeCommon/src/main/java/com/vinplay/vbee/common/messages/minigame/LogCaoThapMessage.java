/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogCaoThapMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public long transId;
    public String nickname;
    public int potBet;
    public int step;
    public long betValue;
    public int result;
    public long prize;
    public String cards;
    public long currentPot;
    public long currentFund;
    public int moneyType;
}

