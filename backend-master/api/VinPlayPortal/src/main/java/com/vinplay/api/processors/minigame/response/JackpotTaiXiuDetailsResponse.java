/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.processors.minigame.response;

public class JackpotTaiXiuDetailsResponse {
    public long referenceId;
    public int result;
    public String time;
    public String countBet;
    public String moneyJackpot;
    public String nickName;
    public long money;

    public JackpotTaiXiuDetailsResponse(long referenceId, int result, String time, String countBet, String moneyJackpot, String nickName, long money) {
        this.referenceId = referenceId;
        this.result = result;
        this.time = time;
        this.countBet = countBet;
        this.moneyJackpot = moneyJackpot;
        this.nickName = nickName;
        this.money = money;
    }
}

