/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

public class TaiXiuItemResponse {
    public long referenceId;
    public String user_name;
    public long bet_value;
    public int bet_side;
    public int bet_time;
    public long balance;

    public TaiXiuItemResponse(long referenceId, String user_name, long bet_value, int bet_side, int bet_time, long balance) {
        this.referenceId = referenceId;
        this.user_name = user_name;
        this.bet_value = bet_value;
        this.bet_side = bet_side;
        this.bet_time = bet_time;
        this.balance = balance;
    }
}

