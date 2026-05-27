/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;
import java.util.Date;

public class FreezeModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String nickname;
    private String gameName;
    private String roomId;
    private long money;
    private String moneyType;
    private int userId;
    private Date createTime;
    private String transNo;
    private int status;

    public FreezeModel() {
    }

    public FreezeModel(String sessionId, String nickname, String gameName, String roomId, long money, String moneyType, Date createTime, String transNo, int status) {
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.gameName = gameName;
        this.roomId = roomId;
        this.money = money;
        this.moneyType = moneyType;
        this.createTime = createTime;
        this.transNo = transNo;
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTransNo() {
        return this.transNo;
    }

    public void setTransNo(String transNo) {
        this.transNo = transNo;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getGameName() {
        return this.gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getRoomId() {
        return this.roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }
}

