/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 */
package com.vinplay.usercore.response;

import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import java.util.List;

public class LogExchangeMoneyResponse {
    public long totalMoney;
    public long totalExchangeMoney;
    public long totalFee;
    public long totalTrans;
    public List<ExchangeMessage> trans;

    public LogExchangeMoneyResponse(long totalMoney, long totalExchangeMoney, long totalFee, long totalTrans, List<ExchangeMessage> trans) {
        this.totalMoney = totalMoney;
        this.totalExchangeMoney = totalExchangeMoney;
        this.totalFee = totalFee;
        this.totalTrans = totalTrans;
        this.trans = trans;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public long getTotalExchangeMoney() {
        return this.totalExchangeMoney;
    }

    public void setTotalExchangeMoney(long totalExchangeMoney) {
        this.totalExchangeMoney = totalExchangeMoney;
    }

    public long getTotalFee() {
        return this.totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public long getTotalTrans() {
        return this.totalTrans;
    }

    public void setTotalTrans(long totalTrans) {
        this.totalTrans = totalTrans;
    }

    public List<ExchangeMessage> getTrans() {
        return this.trans;
    }

    public void setTrans(List<ExchangeMessage> trans) {
        this.trans = trans;
    }
}

