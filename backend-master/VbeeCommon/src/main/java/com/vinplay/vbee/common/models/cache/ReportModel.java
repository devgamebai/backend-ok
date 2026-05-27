/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class ReportModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    public long moneyWin;
    public long moneyLost;
    public long moneyOther;
    public long fee;
    public boolean isBot;

    public ReportModel(long moneyWin, long moneyLost, long moneyOther, long fee, boolean isBot) {
        this.moneyWin = moneyWin;
        this.moneyLost = moneyLost;
        this.moneyOther = moneyOther;
        this.fee = fee;
        this.isBot = isBot;
    }

    public ReportModel() {
    }
}

