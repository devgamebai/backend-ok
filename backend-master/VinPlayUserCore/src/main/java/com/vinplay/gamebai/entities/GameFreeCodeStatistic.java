/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class GameFreeCodeStatistic {
    public int amount;
    public int total;
    public int totalInactive;
    public int totalUsed;
    public int totalLocked;
    public int totalExpired;
    public int totalRemain;

    public GameFreeCodeStatistic(int amount, int total, int totalInactive, int totalUsed, int totalLocked, int totalExpired, int totalRemain) {
        this.amount = amount;
        this.total = total;
        this.totalInactive = totalInactive;
        this.totalUsed = totalUsed;
        this.totalLocked = totalLocked;
        this.totalExpired = totalExpired;
        this.totalRemain = totalRemain;
    }

    public GameFreeCodeStatistic() {
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getTotal() {
        return this.total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalInactive() {
        return this.totalInactive;
    }

    public void setTotalInactive(int totalInactive) {
        this.totalInactive = totalInactive;
    }

    public int getTotalUsed() {
        return this.totalUsed;
    }

    public void setTotalUsed(int totalUsed) {
        this.totalUsed = totalUsed;
    }

    public int getTotalLocked() {
        return this.totalLocked;
    }

    public void setTotalLocked(int totalLocked) {
        this.totalLocked = totalLocked;
    }

    public int getTotalRemain() {
        return this.totalRemain;
    }

    public void setTotalRemain(int totalRemain) {
        this.totalRemain = totalRemain;
    }
}

