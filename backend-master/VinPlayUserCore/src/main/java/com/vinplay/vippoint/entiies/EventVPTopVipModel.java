/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vippoint.entiies;

public class EventVPTopVipModel {
    private int stt;
    private String nickname;
    private int vippoint;

    public EventVPTopVipModel(int stt, String nickname, int vippoint) {
        this.stt = stt;
        this.nickname = nickname;
        this.vippoint = vippoint;
    }

    public EventVPTopVipModel() {
    }

    public int getStt() {
        return this.stt;
    }

    public void setStt(int stt) {
        this.stt = stt;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getVippoint() {
        return this.vippoint;
    }

    public void setVippoint(int vippoint) {
        this.vippoint = vippoint;
    }
}

