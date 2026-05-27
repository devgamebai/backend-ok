/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vippoint.entiies;

public class EventVPTopStrongModel {
    private int stt;
    private String nickname;
    private int vippointSub;
    private int count;
    private int place;
    private String prize;
    private String prizeVin;

    public EventVPTopStrongModel(int stt, String nickname, int vippointSub, int count, int place, String prize, String prizeVin) {
        this.stt = stt;
        this.nickname = nickname;
        this.vippointSub = vippointSub;
        this.count = count;
        this.place = place;
        this.prize = prize;
        this.prizeVin = prizeVin;
    }

    public EventVPTopStrongModel() {
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

    public int getVippointSub() {
        return this.vippointSub;
    }

    public void setVippointSub(int vippointSub) {
        this.vippointSub = vippointSub;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getPlace() {
        return this.place;
    }

    public void setPlace(int place) {
        this.place = place;
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

