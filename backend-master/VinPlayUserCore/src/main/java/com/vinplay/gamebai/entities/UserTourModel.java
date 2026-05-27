/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import java.util.Date;

public class UserTourModel {
    private String nickname;
    private String tourId;
    private int fee;
    private Date tourStartTime;
    private int mark;
    private int top;
    private int userTotal;
    private String prize;

    public UserTourModel() {
    }

    public UserTourModel(String nickname, String tourId, int fee, Date tourStartTime, int mark, int top, int userTotal, String prize) {
        this.nickname = nickname;
        this.tourId = tourId;
        this.fee = fee;
        this.tourStartTime = tourStartTime;
        this.mark = mark;
        this.top = top;
        this.userTotal = userTotal;
        this.prize = prize;
    }

    public String getPrize() {
        return this.prize;
    }

    public void setPrize(String prize) {
        this.prize = prize;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTourId() {
        return this.tourId;
    }

    public void setTourId(String tourId) {
        this.tourId = tourId;
    }

    public int getFee() {
        return this.fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public Date getTourStartTime() {
        return this.tourStartTime;
    }

    public void setTourStartTime(Date tourStartTime) {
        this.tourStartTime = tourStartTime;
    }

    public int getMark() {
        return this.mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public int getTop() {
        return this.top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getUserTotal() {
        return this.userTotal;
    }

    public void setUserTotal(int userTotal) {
        this.userTotal = userTotal;
    }
}

