/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vippoint.entiies;

public class EventVPTopIntelModel {
    private int stt;
    private String nickname;
    private int place;
    private int vippoint;
    private int bonus;
    private String prize;
    private String prizeVin;

    public EventVPTopIntelModel(int stt, String nickname, int place, int vippoint, int bonus, String prize, String prizeVin) {
        this.stt = stt;
        this.nickname = nickname;
        this.place = place;
        this.vippoint = vippoint;
        this.bonus = bonus;
        this.prize = prize;
        this.prizeVin = prizeVin;
    }

    public EventVPTopIntelModel() {
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

    public int getPlace() {
        return this.place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public int getVippoint() {
        return this.vippoint;
    }

    public void setVippoint(int vippoint) {
        this.vippoint = vippoint;
    }

    public int getBonus() {
        return this.bonus;
    }

    public void setBonus(int bonus) {
        this.bonus = bonus;
    }

    public String getPrize() {
        return this.prize;
    }

    public void setPrize(String prize) {
        this.prize = prize;
    }

    public String getPrizeVin() {
        return this.prizeVin;
    }

    public void setPrizeVin(String prizeVin) {
        this.prizeVin = prizeVin;
    }
}

