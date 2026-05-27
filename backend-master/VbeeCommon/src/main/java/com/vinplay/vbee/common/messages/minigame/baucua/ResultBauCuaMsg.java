/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame.baucua;

import com.vinplay.vbee.common.messages.BaseMessage;

public class ResultBauCuaMsg
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public long referenceId;
    public byte room;
    public int minBetValue;
    public byte[] dices = new byte[3];
    public byte xPot;
    public byte xValue;
    public long[] totalBetValues = new long[6];
    public long[] totalPrizes = new long[6];
}

