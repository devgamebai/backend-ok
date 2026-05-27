/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class TransferMoneyBankModel {
    private String nickname;
    private long money;

    public TransferMoneyBankModel(String nickname, long money) {
        this.nickname = nickname;
        this.money = money;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}

