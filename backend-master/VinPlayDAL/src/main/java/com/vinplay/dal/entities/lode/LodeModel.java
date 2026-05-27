/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.lode;

import java.time.LocalDateTime;

public class LodeModel {
    private long id;
    private long userId;
    private String nickName;
    private long betValue;
    private long mode;
    private String ticket;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private long prize;

    public LodeModel(long id, long userId, String nickName, long betValue, long mode, String ticket, LocalDateTime createdDate, LocalDateTime updatedDate, long prize) {
        this.id = id;
        this.userId = userId;
        this.nickName = nickName;
        this.betValue = betValue;
        this.mode = mode;
        this.ticket = ticket;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.prize = prize;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getBetValue() {
        return this.betValue;
    }

    public void setBetValue(long betValue) {
        this.betValue = betValue;
    }

    public long getMode() {
        return this.mode;
    }

    public void setMode(long mode) {
        this.mode = mode;
    }

    public String getTicket() {
        return this.ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return this.updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public long getPrize() {
        return this.prize;
    }

    public void setPrize(long prize) {
        this.prize = prize;
    }
}

