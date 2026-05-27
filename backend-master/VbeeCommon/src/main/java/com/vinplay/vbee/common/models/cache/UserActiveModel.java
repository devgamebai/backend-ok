/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class UserActiveModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private boolean isBot;
    private long lastMessageId;
    private long lastActive;
    private long lastActiveVin;
    private long lastActiveXu;
    private boolean updateMySQL;

    public UserActiveModel(String nickname, boolean isBot) {
        this.nickname = nickname;
        this.isBot = isBot;
    }

    public UserActiveModel(String nickname, boolean isBot, long lastMessageId, long lastActive, long lastActiveVin, long lastActiveXu) {
        this.nickname = nickname;
        this.isBot = isBot;
        this.lastMessageId = lastMessageId;
        this.lastActive = lastActive;
        this.lastActiveVin = lastActiveVin;
        this.lastActiveXu = lastActiveXu;
    }

    public UserActiveModel() {
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isBot() {
        return this.isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public long getLastMessageId() {
        return this.lastMessageId;
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public long getLastActive() {
        return this.lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public long getLastActiveVin() {
        return this.lastActiveVin;
    }

    public void setLastActiveVin(long lastActiveVin) {
        this.lastActiveVin = lastActiveVin;
    }

    public long getLastActiveXu() {
        return this.lastActiveXu;
    }

    public void setLastActiveXu(long lastActiveXu) {
        this.lastActiveXu = lastActiveXu;
    }

    public boolean isUpdateMySQL() {
        return this.updateMySQL;
    }

    public void setUpdateMySQL(boolean updateMySQL) {
        this.updateMySQL = updateMySQL;
    }
}

