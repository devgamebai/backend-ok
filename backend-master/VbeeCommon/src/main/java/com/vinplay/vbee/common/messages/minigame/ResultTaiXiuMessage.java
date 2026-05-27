/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.minigame;

import com.vinplay.vbee.common.messages.BaseMessage;

public class ResultTaiXiuMessage
        extends BaseMessage {
    private static final long serialVersionUID = 1L;
    public long referenceId;
    public int result;
    public int dice1;
    public int dice2;
    public int dice3;
    public long totalTai;
    public long totalXiu;
    public int numBetTai;
    public int numBetXiu;
    public long totalPrize;
    public long totalRefundTai;
    public long totalRefundXiu;
    public long totalRevenue;
    public int moneyType;
    public long totalJackpot;
    public boolean isMD5=false;

    public String before_md5;
    public String md5;

    public ResultTaiXiuMessage() {
    }

    public ResultTaiXiuMessage(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType, long totalJackpot) {
        this.referenceId = referenceId;
        this.result = result;
        this.dice1 = dice1;
        this.dice2 = dice2;
        this.dice3 = dice3;
        this.totalTai = totalTai;
        this.totalXiu = totalXiu;
        this.numBetTai = numBetTai;
        this.numBetXiu = numBetXiu;
        this.totalPrize = totalPrize;
        this.totalRefundTai = totalRefundTai;
        this.totalRefundXiu = totalRefundXiu;
        this.totalRevenue = totalRevenue;
        this.moneyType = moneyType;
        this.totalJackpot = totalJackpot;
    }
}

