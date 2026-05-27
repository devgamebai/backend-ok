/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LuckyMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String nickname;
    private String resultVin;
    private String resultXu;
    private String resultSlot;

    public LuckyMessage(int userId, String nickname, String resultVin, String resultXu, String resultSlot) {
        this.userId = userId;
        this.nickname = nickname;
        this.resultVin = resultVin;
        this.resultXu = resultXu;
        this.resultSlot = resultSlot;
    }

    public LuckyMessage() {
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

    public String getResultVin() {
        return this.resultVin;
    }

    public void setResultVin(String resultVin) {
        this.resultVin = resultVin;
    }

    public String getResultXu() {
        return this.resultXu;
    }

    public void setResultXu(String resultXu) {
        this.resultXu = resultXu;
    }

    public String getResultSlot() {
        return this.resultSlot;
    }

    public void setResultSlot(String resultSlot) {
        this.resultSlot = resultSlot;
    }
}

