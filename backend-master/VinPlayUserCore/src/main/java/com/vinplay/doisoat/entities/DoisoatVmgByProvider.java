/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.doisoat.entities;

public class DoisoatVmgByProvider {
    private int id;
    private long mo;
    private long mt;
    private long sms;
    private double ratio;
    private int mtPerMo;
    private long mtLimit;
    private long mtDif;
    private int price;
    private int price2;
    private long mtMoneyPay;
    private long totalMoney;

    public DoisoatVmgByProvider(int id, double ratio, int mtPerMo, int price, int price2) {
        this.id = id;
        this.ratio = ratio;
        this.mtPerMo = mtPerMo;
        this.price = price;
        this.price2 = price2;
        this.mo = 0L;
        this.mt = 0L;
        this.sms = 0L;
        this.mtLimit = 0L;
        this.mtDif = 0L;
        this.mtMoneyPay = 0L;
        this.totalMoney = 0L;
    }

    public DoisoatVmgByProvider() {
    }

    public void calculate() {
        this.sms = this.mo > this.mt ? this.mt : this.mo;
        this.mtLimit = this.id > 0 ? this.mo * (long)(this.mtPerMo - 1) : this.mo * (long)this.mtPerMo;
        if (this.mtLimit < 0L) {
            this.mtLimit = 0L;
        }
        if (this.mt > this.mo) {
            this.mtDif = this.mt - this.mo;
        }
        this.mtMoneyPay = this.mtDif > this.mtLimit ? (this.mtDif - this.mtLimit) * (long)this.price2 + this.mtLimit * 91L : this.mtDif * 91L;
        this.totalMoney = Math.round((double)this.sms * this.ratio * (double)this.price);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getMo() {
        return this.mo;
    }

    public void setMo(long mo) {
        this.mo = mo;
    }

    public long getMt() {
        return this.mt;
    }

    public void setMt(long mt) {
        this.mt = mt;
    }

    public long getSms() {
        return this.sms;
    }

    public void setSms(long sms) {
        this.sms = sms;
    }

    public double getRatio() {
        return this.ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public int getMtPerMo() {
        return this.mtPerMo;
    }

    public void setMtPerMo(int mtPerMo) {
        this.mtPerMo = mtPerMo;
    }

    public long getMtLimit() {
        return this.mtLimit;
    }

    public void setMtLimit(long mtLimit) {
        this.mtLimit = mtLimit;
    }

    public long getMtDif() {
        return this.mtDif;
    }

    public void setMtDif(long mtDif) {
        this.mtDif = mtDif;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getMtMoneyPay() {
        return this.mtMoneyPay;
    }

    public void setMtMoneyPay(long mtMoneyPay) {
        this.mtMoneyPay = mtMoneyPay;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public int getPrice2() {
        return this.price2;
    }

    public void setPrice2(int price2) {
        this.price2 = price2;
    }
}

