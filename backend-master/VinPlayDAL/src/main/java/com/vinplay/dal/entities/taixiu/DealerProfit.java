/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.entities.taixiu;

public class DealerProfit {
    public int id;
    public long phienid;
    public int result;
    public long total_money_tai = 0L;
    public long total_money_xiu = 0L;
    public long total_profit = 0L;
    public long last_balance = 0L;
    public String created_time;

    public DealerProfit() {
    }

    public DealerProfit(int id,long phienid, int result, long total_money_tai
            , long total_money_xiu, long total_profit, long last_balance, String created_time) {
        this.id = id;
        this.phienid = phienid;
        this.result = result;
        this.total_money_tai = total_money_tai;
        this.total_money_xiu = total_money_xiu;
        this.total_profit = total_profit;
        this.created_time = created_time;
        this.last_balance = last_balance;
    }
}

