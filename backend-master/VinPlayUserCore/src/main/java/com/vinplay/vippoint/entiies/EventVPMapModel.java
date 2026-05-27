/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vippoint.entiies;

public class EventVPMapModel {
    private String nickname;
    private String avatar;
    private int vippoint;
    private int subVippoint;
    private int place;
    private int min;

    public EventVPMapModel(String nickname, String avatar, int vippoint, int subVippoint, int place, int min) {
        this.nickname = nickname;
        this.avatar = avatar;
        this.vippoint = vippoint;
        this.subVippoint = subVippoint;
        this.place = place;
        this.min = min;
    }

    public EventVPMapModel() {
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getVippoint() {
        return this.vippoint;
    }

    public void setVippoint(int vippoint) {
        this.vippoint = vippoint;
    }

    public int getSubVippoint() {
        return this.subVippoint;
    }

    public void setSubVippoint(int subVippoint) {
        this.subVippoint = subVippoint;
    }

    public int getPlace() {
        return this.place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public int getMin() {
        return this.min;
    }

    public void setMin(int min) {
        this.min = min;
    }
}

