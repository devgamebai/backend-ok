/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class FreezeMoneyMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private int userId;
    private String nickname;
    private String gameName;
    private String roomId;
    private long money;
    private long moneyUse;
    private long moneyTotal;
    private String moneyType;
    private String transactionNo;

    public FreezeMoneyMessage() {
    }

    public FreezeMoneyMessage(String id, String sessionId, int userId, String nickname, String gameName, String roomId, long money, long moneyUse, long moneyTotal, String moneyType, String transactionNo) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.nickname = nickname;
        this.gameName = gameName;
        this.roomId = roomId;
        this.money = money;
        this.moneyUse = moneyUse;
        this.moneyTotal = moneyTotal;
        this.moneyType = moneyType;
        this.transactionNo = transactionNo;
    }

    public String getTransactionNo() {
        return this.transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public long getMoneyUse() {
        return this.moneyUse;
    }

    public void setMoneyUse(long moneyUse) {
        this.moneyUse = moneyUse;
    }

    public long getMoneyTotal() {
        return this.moneyTotal;
    }

    public void setMoneyTotal(long moneyTotal) {
        this.moneyTotal = moneyTotal;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

