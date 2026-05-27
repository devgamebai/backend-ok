/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models.minigame;

public class CurrentTransactionSicboDetails {
    private String userName;
    private String betSide;
    private long bet;
    private long referenceId;

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBetSide() {
        return this.betSide;
    }

    public void setBetSide(String betSide) {
        this.betSide = betSide;
    }

    public long getBet() {
        return this.bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public long getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(long referenceId) {
        this.referenceId = referenceId;
    }
}

