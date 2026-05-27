/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class MarketingModel {
    public String campaign;
    public String medium;
    public String source;
    public int NRU;
    public int PU;
    public long doanhthu;
    public String dateStr;

    public MarketingModel() {
    }

    public MarketingModel(String Campaign, String Medium, String Source, int NRU, int PU, long doanhthu, String dateStr) {
        this.campaign = Campaign;
        this.medium = Medium;
        this.source = Source;
        this.NRU = NRU;
        this.PU = PU;
        this.doanhthu = doanhthu;
        this.dateStr = dateStr;
    }
}

