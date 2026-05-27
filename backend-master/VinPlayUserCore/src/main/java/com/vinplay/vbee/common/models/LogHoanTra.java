/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

import java.sql.Date;

public class LogHoanTra {
    private long id;
    private String nickName;
    private Date time;
    private long vipPoint;
    private long totalMoneySport;
    private long hoanTraSport;
    private long totalMoneyCasino;
    private long hoanTraCasino;
    private long totalMoneyGame;
    private long hoanTraGame;
    private long vipIndex;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Date getTime() {
        return this.time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public long getVipPoint() {
        return this.vipPoint;
    }

    public void setVipPoint(long vipPoint) {
        this.vipPoint = vipPoint;
    }

    public long getTotalMoneySport() {
        return this.totalMoneySport;
    }

    public void setTotalMoneySport(long totalMoneySport) {
        this.totalMoneySport = totalMoneySport;
    }

    public long getHoanTraSport() {
        return this.hoanTraSport;
    }

    public void setHoanTraSport(long hoanTraSport) {
        this.hoanTraSport = hoanTraSport;
    }

    public long getTotalMoneyCasino() {
        return this.totalMoneyCasino;
    }

    public void setTotalMoneyCasino(long totalMoneyCasino) {
        this.totalMoneyCasino = totalMoneyCasino;
    }

    public long getHoanTraCasino() {
        return this.hoanTraCasino;
    }

    public void setHoanTraCasino(long hoanTraCasino) {
        this.hoanTraCasino = hoanTraCasino;
    }

    public long getTotalMoneyGame() {
        return this.totalMoneyGame;
    }

    public void setTotalMoneyGame(long totalMoneyGame) {
        this.totalMoneyGame = totalMoneyGame;
    }

    public long getHoanTraGame() {
        return this.hoanTraGame;
    }

    public void setHoanTraGame(long hoanTraGame) {
        this.hoanTraGame = hoanTraGame;
    }

    public long getVipIndex() {
        return this.vipIndex;
    }

    public void setVipIndex(long vipIndex) {
        this.vipIndex = vipIndex;
    }
}

