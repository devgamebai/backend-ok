/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

import java.util.Date;

public class CashoutUserDailyResponse {
    private Date cashoutTime;
    private int cashout;

    public CashoutUserDailyResponse(Date cashoutTime, int cashout) {
        this.cashoutTime = cashoutTime;
        this.cashout = cashout;
    }

    public Date getCashoutTime() {
        return this.cashoutTime;
    }

    public void setCashoutTime(Date cashoutTime) {
        this.cashoutTime = cashoutTime;
    }

    public int getCashout() {
        return this.cashout;
    }

    public void setCashout(int cashout) {
        this.cashout = cashout;
    }
}

