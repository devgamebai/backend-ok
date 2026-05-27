/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

public class NganLuongModel {
    private String nickname;
    private int amount;
    private String orderCode;
    private String bank;

    public NganLuongModel(String nickname, int amount, String orderCode, String bank) {
        this.nickname = nickname;
        this.amount = amount;
        this.orderCode = orderCode;
        this.bank = bank;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getOrderCode() {
        return this.orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getBank() {
        return this.bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }
}

