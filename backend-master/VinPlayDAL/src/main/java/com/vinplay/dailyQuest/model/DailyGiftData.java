/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dailyQuest.model;

import java.io.Serializable;

public class DailyGiftData
implements Serializable {
    private static final long serialVersionUID = 1123581347L;
    public boolean isSuccess = false;
    public boolean isReceive = false;
    public long currentValue = 0L;

    public DailyGiftData() {
        this.resetData();
    }

    public boolean receiveGift() {
        if (this.isReceive) {
            return false;
        }
        if (this.isSuccess) {
            this.isReceive = true;
            return true;
        }
        return false;
    }

    public void resetData() {
        this.isSuccess = false;
        this.isReceive = false;
        this.currentValue = 0L;
    }
}

