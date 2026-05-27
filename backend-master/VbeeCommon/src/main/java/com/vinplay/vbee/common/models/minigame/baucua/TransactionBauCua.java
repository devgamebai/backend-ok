/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.minigame.baucua;

public class TransactionBauCua {
    public long referenceId;
    public int room;
    public String username;
    public String transactionCode;
    public String dices;
    public long[] betValues = new long[6];
    public long[] prizes = new long[6];
    public long totalExchange;
    public byte moneyType;
    public String timestamp;
}

