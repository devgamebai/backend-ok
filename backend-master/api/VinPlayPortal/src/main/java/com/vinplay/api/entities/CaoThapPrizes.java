/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.api.entities;

public class CaoThapPrizes {
    String prize;
    String money;
    String num;

    public CaoThapPrizes(String prize, String money, String num) {
        this.prize = prize;
        this.money = money;
        this.num = num;
    }

    public String getPrize() {
        return this.prize;
    }

    public void setPrize(String prize) {
        this.prize = prize;
    }

    public String getMoney() {
        return this.money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getNum() {
        return this.num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}

