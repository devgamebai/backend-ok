/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class TransactionBetTX
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private long betValue;
    private short betSide;
    private short moneyType;
    private boolean bot;

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getBetValue() {
        return this.betValue;
    }

    public void setBetValue(long betValue) {
        this.betValue = betValue;
    }

    public short getBetSide() {
        return this.betSide;
    }

    public void setBetSide(short betSide) {
        this.betSide = betSide;
    }

    public short getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(short moneyType) {
        this.moneyType = moneyType;
    }

    public boolean isBot() {
        return this.bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }
}

