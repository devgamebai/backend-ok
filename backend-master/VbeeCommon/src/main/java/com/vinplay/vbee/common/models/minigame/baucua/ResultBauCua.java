/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.minigame.baucua;

public class ResultBauCua {
    public long referenceId;
    public byte room;
    public int minBetValue;
    public byte[] dices = new byte[3];
    public byte xPot;
    public byte xValue;
    public long[] totalBetValues = new long[6];
    public long[] totalPrizes = new long[6];

    public ResultBauCua() {
    }

    public ResultBauCua(long referenceId, byte room, int minBetValue) {
        this.referenceId = referenceId;
        this.room = room;
        this.minBetValue = minBetValue;
    }
}

