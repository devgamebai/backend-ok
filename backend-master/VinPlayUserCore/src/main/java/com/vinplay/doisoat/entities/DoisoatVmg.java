/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.doisoat.entities;

import com.vinplay.doisoat.entities.DoisoatVmgByProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DoisoatVmg {
    private Map<Integer, DoisoatVmgByProvider> providers = new HashMap<Integer, DoisoatVmgByProvider>();
    private long totalMo = 0L;
    private long totalMt = 0L;
    private long totalSms = 0L;
    private long totalMtLimit = 0L;
    private long totalMtDif = 0L;
    private long totalMtMoneyPay = 0L;
    private long totalMoneySMS = 0L;
    private double ratio = 0.0;
    private long moneyVinplayReceive = 0L;
    private long moneyVMGReceive = 0L;
    private long minVMGReceive = 0L;
    private long moneyViettelFee = 0L;
    private long money = 0L;
    private long vat = 0L;
    private long totalMoney = 0L;

    public DoisoatVmg() {
        for (int i = 0; i < 6; ++i) {
            DoisoatVmgByProvider model = this.getProvider(i);
            this.providers.put(i, model);
        }
    }

    public void calculate() {
        for (Map.Entry<Integer, DoisoatVmgByProvider> entry : this.providers.entrySet()) {
            DoisoatVmgByProvider model = entry.getValue();
            this.totalMo += model.getMo();
            this.totalMt += model.getMt();
            this.totalSms += model.getSms();
            this.totalMtLimit += model.getMtLimit();
            this.totalMtDif += model.getMtDif();
            this.totalMtMoneyPay += model.getMtMoneyPay();
            this.totalMoneySMS += model.getTotalMoney();
        }
        this.ratio = this.totalMoney < 200000000L ? 0.87 : (this.totalMoney < 500000000L ? 0.89 : (this.totalMoney <= 100000000L ? 0.91 : 0.93));
        this.moneyVinplayReceive = Math.round((double)this.totalMoneySMS * this.ratio);
        this.moneyVMGReceive = this.totalMoneySMS - this.moneyVinplayReceive;
        this.minVMGReceive = 2000000L;
        this.moneyViettelFee = Math.round(5454.545454545454);
        this.money = this.moneyVMGReceive > this.minVMGReceive ? this.moneyVinplayReceive - this.totalMtMoneyPay - this.moneyViettelFee : this.totalMoneySMS - this.minVMGReceive - this.totalMtMoneyPay - this.moneyViettelFee;
        this.vat = Math.round((double)this.money * 0.1);
        this.totalMoney = this.money + this.vat;
    }

    private DoisoatVmgByProvider getProvider(int id) {
        double ratio = 0.0;
        int mtPerMo = 0;
        int price = 0;
        int price2 = 0;
        switch (id) {
            case 0: {
                ratio = 1.0;
                mtPerMo = 1;
                price = 236;
                price2 = 545;
                break;
            }
            case 1: {
                ratio = 1.0;
                mtPerMo = 2;
                price = 191;
                price2 = 455;
                break;
            }
            case 2: {
                ratio = 1.0;
                mtPerMo = 2;
                price = 191;
                price2 = 318;
                break;
            }
            case 3: {
                ratio = 1.0;
                mtPerMo = 1;
                price = 236;
                price2 = 455;
                break;
            }
            case 4: {
                ratio = 1.0;
                mtPerMo = 0;
                price = 104;
                price2 = 181;
            }
        }
        DoisoatVmgByProvider model = new DoisoatVmgByProvider(id, ratio, mtPerMo, price, price2);
        return model;
    }

    public Map<Integer, DoisoatVmgByProvider> getProviders() {
        return this.providers;
    }

    public void setProviders(Map<Integer, DoisoatVmgByProvider> providers) {
        this.providers = providers;
    }

    public long getTotalMo() {
        return this.totalMo;
    }

    public void setTotalMo(long totalMo) {
        this.totalMo = totalMo;
    }

    public long getTotalMt() {
        return this.totalMt;
    }

    public void setTotalMt(long totalMt) {
        this.totalMt = totalMt;
    }

    public long getTotalSms() {
        return this.totalSms;
    }

    public void setTotalSms(long totalSms) {
        this.totalSms = totalSms;
    }

    public long getTotalMtLimit() {
        return this.totalMtLimit;
    }

    public void setTotalMtLimit(long totalMtLimit) {
        this.totalMtLimit = totalMtLimit;
    }

    public long getTotalMtDif() {
        return this.totalMtDif;
    }

    public void setTotalMtDif(long totalMtDif) {
        this.totalMtDif = totalMtDif;
    }

    public long getTotalMtMoneyPay() {
        return this.totalMtMoneyPay;
    }

    public void setTotalMtMoneyPay(long totalMtMoneyPay) {
        this.totalMtMoneyPay = totalMtMoneyPay;
    }

    public long getTotalMoneySMS() {
        return this.totalMoneySMS;
    }

    public void setTotalMoneySMS(long totalMoneySMS) {
        this.totalMoneySMS = totalMoneySMS;
    }

    public double getRatio() {
        return this.ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public long getMoneyVinplayReceive() {
        return this.moneyVinplayReceive;
    }

    public void setMoneyVinplayReceive(long moneyVinplayReceive) {
        this.moneyVinplayReceive = moneyVinplayReceive;
    }

    public long getMoneyVMGReceive() {
        return this.moneyVMGReceive;
    }

    public void setMoneyVMGReceive(long moneyVMGReceive) {
        this.moneyVMGReceive = moneyVMGReceive;
    }

    public long getMinVMGReceive() {
        return this.minVMGReceive;
    }

    public void setMinVMGReceive(long minVMGReceive) {
        this.minVMGReceive = minVMGReceive;
    }

    public long getMoneyViettelFee() {
        return this.moneyViettelFee;
    }

    public void setMoneyViettelFee(long moneyViettelFee) {
        this.moneyViettelFee = moneyViettelFee;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getVat() {
        return this.vat;
    }

    public void setVat(long vat) {
        this.vat = vat;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }
}

