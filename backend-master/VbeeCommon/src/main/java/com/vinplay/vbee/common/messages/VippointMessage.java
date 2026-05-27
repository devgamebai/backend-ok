/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class VippointMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String nickname;
    private int moneyVP;
    private int vp;

    public VippointMessage(int userId, String nickname, int moneyVP, int vp) {
        this.userId = userId;
        this.nickname = nickname;
        this.moneyVP = moneyVP;
        this.vp = vp;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getMoneyVP() {
        return this.moneyVP;
    }

    public void setMoneyVP(int moneyVP) {
        this.moneyVP = moneyVP;
    }

    public int getVp() {
        return this.vp;
    }

    public void setVp(int vp) {
        this.vp = vp;
    }
}

