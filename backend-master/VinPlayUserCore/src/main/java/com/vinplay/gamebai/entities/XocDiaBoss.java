/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class XocDiaBoss {
    private String sessionId;
    private String nickname;
    private int roomId;
    private String roomSetting;
    private long fundInitial;
    private int status;
    private int fee;
    private long revenue;
    private String createTime;
    private boolean isSystemLogin;

    public XocDiaBoss() {
    }

    public XocDiaBoss(String sessionId, String nickname, int roomId, String roomSetting, long fundInitial, int status, int fee, long revenue, String createTime) {
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.roomId = roomId;
        this.roomSetting = roomSetting;
        this.fundInitial = fundInitial;
        this.status = status;
        this.fee = fee;
        this.revenue = revenue;
        this.createTime = createTime;
        this.isSystemLogin = false;
    }

    public boolean isSystemLogin() {
        return this.isSystemLogin;
    }

    public void setSystemLogin(boolean isSystemLogin) {
        this.isSystemLogin = isSystemLogin;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
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

    public String getRoomSetting() {
        return this.roomSetting;
    }

    public void setRoomSetting(String roomSetting) {
        this.roomSetting = roomSetting;
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

    public int getRoomId() {
        return this.roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
}

