/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class AgentDSModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private long dsMua;
    private long dsBan;
    private long ds;
    private int gdMua;
    private int gdBan;
    private int gd;
    private String dl1;
    private String nickname;
    private long feeMua;
    private long feeBan;
    private long fee;
    private boolean bAgent1;

    public AgentDSModel(String nickname, long dsMua, long dsBan, int gdMua, int gdBan, long feeMua, long feeBan, long ds, int gd, long fee, boolean bAgent1) {
        this.nickname = nickname;
        this.dsMua = dsMua;
        this.dsBan = dsBan;
        this.gdMua = gdMua;
        this.gdBan = gdBan;
        this.feeMua = feeMua;
        this.feeBan = feeBan;
        this.ds = ds;
        this.gd = gd;
        this.fee = fee;
        this.bAgent1 = bAgent1;
    }

    public AgentDSModel(String nickname, long dsMua, long dsBan, int gdMua, int gdBan, long feeMua, long feeBan, boolean bAgent1) {
        this.nickname = nickname;
        this.dsMua = dsMua;
        this.dsBan = dsBan;
        this.gdMua = gdMua;
        this.gdBan = gdBan;
        this.feeMua = feeMua;
        this.feeBan = feeBan;
        this.bAgent1 = bAgent1;
        this.ds = dsMua + dsBan;
        this.gd = gdMua + gdBan;
        this.fee = feeMua + feeBan;
    }

    public AgentDSModel(long dsMua, long dsBan, long ds, int gdMua, int gdBan, int gd) {
        this.dsMua = dsMua;
        this.dsBan = dsBan;
        this.ds = ds;
        this.gdMua = gdMua;
        this.gdBan = gdBan;
        this.gd = gd;
    }

    public long getDsMua() {
        return this.dsMua;
    }

    public String getDl1() {
        return this.dl1;
    }

    public void setDl1(String dl1) {
        this.dl1 = dl1;
    }

    public AgentDSModel() {
    }

    public void setDsMua(long dsMua) {
        this.dsMua = dsMua;
    }

    public long getDsBan() {
        return this.dsBan;
    }

    public void setDsBan(long dsBan) {
        this.dsBan = dsBan;
    }

    public long getDs() {
        return this.ds;
    }

    public void setDs(long ds) {
        this.ds = ds;
    }

    public int getGdMua() {
        return this.gdMua;
    }

    public void setGdMua(int gdMua) {
        this.gdMua = gdMua;
    }

    public int getGdBan() {
        return this.gdBan;
    }

    public void setGdBan(int gdBan) {
        this.gdBan = gdBan;
    }

    public int getGd() {
        return this.gd;
    }

    public void setGd(int gd) {
        this.gd = gd;
    }

    public AgentDSModel(long dsMua, long dsBan, int gdMua, int gdBan) {
        this.dsMua = dsMua;
        this.dsBan = dsBan;
        this.gdMua = gdMua;
        this.gdBan = gdBan;
        this.ds = dsMua + dsBan;
        this.gd = gdMua + gdBan;
        this.fee = this.feeMua + this.feeBan;
    }

    public boolean isbAgent1() {
        return this.bAgent1;
    }

    public void setbAgent1(boolean bAgent1) {
        this.bAgent1 = bAgent1;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getFeeMua() {
        return this.feeMua;
    }

    public void setFeeMua(long feeMua) {
        this.feeMua = feeMua;
    }

    public long getFeeBan() {
        return this.feeBan;
    }

    public void setFeeBan(long feeBan) {
        this.feeBan = feeBan;
    }
}

