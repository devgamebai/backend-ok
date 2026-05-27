/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StatisticUserMarketing
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String campaign;
    private String medium;
    private String source;
    private int NRU;
    private int PU;
    private long totalNapVin;
    private long totalTieuVin;
    private Date updateTime;

    public StatisticUserMarketing(String campaign, String medium, String source, int nRU, int pU, long totalNapVin, long totalTieuVin) {
        this.campaign = campaign;
        this.medium = medium;
        this.source = source;
        this.NRU = nRU;
        this.PU = pU;
        this.totalNapVin = totalNapVin;
        this.totalTieuVin = totalTieuVin;
        this.updateTime = new Date();
    }

    public StatisticUserMarketing() {
        this.updateTime = new Date();
    }

    public void addPU(long value) {
        this.PU = (int)((long)this.PU + value);
    }

    public void addNapVin(long value) {
        this.totalNapVin += value;
    }

    public void addTieuVin(long value) {
        this.totalTieuVin += value;
    }

    public String getCampaign() {
        return this.campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    public String getMedium() {
        return this.medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getNRU() {
        return this.NRU;
    }

    public void setNRU(int nRU) {
        this.NRU = nRU;
    }

    public int getPU() {
        return this.PU;
    }

    public void setPU(int pU) {
        this.PU = pU;
    }

    public long getTotalNapVin() {
        return this.totalNapVin;
    }

    public void setTotalNapVin(long totalNapVin) {
        this.totalNapVin = totalNapVin;
    }

    public long getTotalTieuVin() {
        return this.totalTieuVin;
    }

    public void setTotalTieuVin(long totalTieuVin) {
        this.totalTieuVin = totalTieuVin;
    }

    public Date getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(Date newDate) {
        this.updateTime = newDate;
    }

    public String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(this.updateTime);
    }
}

