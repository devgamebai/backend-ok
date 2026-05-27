/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.doisoat.entities;

import com.vinplay.doisoat.entities.DoisoatBrandnameProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DoisoatBrandname {
    private Map<Integer, DoisoatBrandnameProvider> providers = new HashMap<Integer, DoisoatBrandnameProvider>();
    private long brandnameFee = 0L;
    private long holdFee = 150000L;
    private long sendFee = 0L;
    private long totalSms = 0L;
    private long totalMoney = this.brandnameFee + this.holdFee + this.sendFee;

    public DoisoatBrandname() {
        for (int i = 0; i < 6; ++i) {
            if (i == 3 || i == 4) continue;
            DoisoatBrandnameProvider model = this.getProvider(i);
            this.providers.put(i, model);
        }
    }

    private DoisoatBrandnameProvider getProvider(int id) {
        int price = 0;
        switch (id) {
            case 0: {
                price = 780;
                break;
            }
            case 1: {
                price = 720;
                break;
            }
            case 2: {
                price = 720;
                break;
            }
            default: {
                price = 360;
            }
        }
        DoisoatBrandnameProvider model = new DoisoatBrandnameProvider(id, price);
        return model;
    }

    public void calculate() {
        for (Map.Entry<Integer, DoisoatBrandnameProvider> entry : this.providers.entrySet()) {
            DoisoatBrandnameProvider model = entry.getValue();
            this.totalSms += model.getSms();
            this.totalMoney += model.getMoney();
        }
    }

    public Map<Integer, DoisoatBrandnameProvider> getProviders() {
        return this.providers;
    }

    public void setProviders(Map<Integer, DoisoatBrandnameProvider> providers) {
        this.providers = providers;
    }

    public long getBrandnameFee() {
        return this.brandnameFee;
    }

    public void setBrandnameFee(long brandnameFee) {
        this.brandnameFee = brandnameFee;
    }

    public long getHoldFee() {
        return this.holdFee;
    }

    public void setHoldFee(long holdFee) {
        this.holdFee = holdFee;
    }

    public long getSendFee() {
        return this.sendFee;
    }

    public void setSendFee(long sendFee) {
        this.sendFee = sendFee;
    }

    public long getTotalSms() {
        return this.totalSms;
    }

    public void setTotalSms(long totalSms) {
        this.totalSms = totalSms;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }
}

