/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.processors.minigame.response;

public class JackpotTaiXiuResponse {
    public long referenceId;
    public int result;
    public String time;
    public String countBet;
    public String moneyJackpot;
    public String data;

    public JackpotTaiXiuResponse(long referenceId, int result, String time, String countBet, String moneyJackpot, String data) {
        this.referenceId = referenceId;
        this.result = result;
        this.time = time;
        this.countBet = countBet;
        this.moneyJackpot = moneyJackpot;
        this.data = data;
    }
}

