/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.doisoat.entities;

public class DoisoatBrandnameProvider {
    private int id;
    private long sms;
    private int price;
    private long money;

    public DoisoatBrandnameProvider(int id, int price) {
        this.id = id;
        this.price = price;
        this.sms = 0L;
        this.money = 0L;
    }

    public void calculate() {
        this.money = this.sms * (long)this.price;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getSms() {
        return this.sms;
    }

    public void setSms(long sms) {
        this.sms = sms;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}

