/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class VipTourModel {
    private String nickname;
    private String vipTourId;
    private String value;

    public VipTourModel(String nickname, String vipTourId, String value) {
        this.nickname = nickname;
        this.vipTourId = vipTourId;
        this.value = value;
    }

    public VipTourModel() {
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getVipTourId() {
        return this.vipTourId;
    }

    public void setVipTourId(String vipTourId) {
        this.vipTourId = vipTourId;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

