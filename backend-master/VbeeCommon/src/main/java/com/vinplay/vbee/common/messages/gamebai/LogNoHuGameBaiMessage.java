/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.gamebai;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogNoHuGameBaiMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private int room;
    private long potValue;
    private long moneyWin;
    private String gamename;
    private String description;
    private String tourId;

    public LogNoHuGameBaiMessage(String nickname, int room, long potValue, long moneyWin, String gamename, String description, String tourId) {
        this.nickname = nickname;
        this.room = room;
        this.potValue = potValue;
        this.moneyWin = moneyWin;
        this.gamename = gamename;
        this.description = description;
        this.tourId = tourId;
    }

    public LogNoHuGameBaiMessage() {
    }

    public String getTourId() {
        return this.tourId;
    }

    public void setTourId(String tourId) {
        this.tourId = tourId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getRoom() {
        return this.room;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public long getPotValue() {
        return this.potValue;
    }

    public void setPotValue(long potValue) {
        this.potValue = potValue;
    }

    public long getMoneyWin() {
        return this.moneyWin;
    }

    public void setMoneyWin(long moneyWin) {
        this.moneyWin = moneyWin;
    }

    public String getGamename() {
        return this.gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

