/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class BossXocDiaModel {
    private String sessionId;
    private String nickname;
    private int roomId;
    private int moneyBet;
    private String password;
    private String roomName;
    private long fundInitial;
    private long fund;
    private int status;
    private int fee;
    private long revenue;
    private String createTime;

    public BossXocDiaModel(String sessionId, String nickname, int roomId, int moneyBet, String password, String roomName, long fundInitial, long fund, int status, int fee, long revenue, String createTime) {
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.roomId = roomId;
        this.moneyBet = moneyBet;
        this.password = password;
        this.roomName = roomName;
        this.fundInitial = fundInitial;
        this.fund = fund;
        this.status = status;
        this.fee = fee;
        this.revenue = revenue;
        this.createTime = createTime;
    }

    public long getFund() {
        return this.fund;
    }

    public void setFund(long fund) {
        this.fund = fund;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getRoomId() {
        return this.roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getMoneyBet() {
        return this.moneyBet;
    }

    public void setMoneyBet(int moneyBet) {
        this.moneyBet = moneyBet;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public long getFundInitial() {
        return this.fundInitial;
    }

    public void setFundInitial(long fundInitial) {
        this.fundInitial = fundInitial;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getFee() {
        return this.fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public long getRevenue() {
        return this.revenue;
    }

    public void setRevenue(long revenue) {
        this.revenue = revenue;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}

