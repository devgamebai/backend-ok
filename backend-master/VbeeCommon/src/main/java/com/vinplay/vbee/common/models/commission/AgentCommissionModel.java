/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.commission;

import java.io.Serializable;
import java.util.Date;

public class AgentCommissionModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private Date lastActive;
    private long lastMessageId;
    private int userId;
    private String nickName;
    private long fee;
    private long volume;

    public AgentCommissionModel(int userId, String nickName, long fee, long volume, Date lastActive, long lastMessageId) {
        this.userId = userId;
        this.nickName = nickName;
        this.fee = fee;
        this.volume = volume;
        this.lastActive = lastActive;
        this.lastMessageId = lastMessageId;
    }

    public AgentCommissionModel() {
    }

    public Date getLastActive() {
        return this.lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public long getLastMessageId() {
        return this.lastMessageId;
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getVolume() {
        return this.volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }
}

