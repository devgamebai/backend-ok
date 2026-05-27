/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

import java.sql.Timestamp;

public class GiftCodes {
    private long id;
    private String giftcode;
    private long type;
    private long money;
    private long timeUsed;
    private long maxUse;
    private Timestamp from;
    private Timestamp exprired;
    private Timestamp createdAt;
    private String createdBy;
    private long event;
    private String userName;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGiftcode() {
        return this.giftcode;
    }

    public void setGiftcode(String giftcode) {
        this.giftcode = giftcode;
    }

    public long getType() {
        return this.type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getTimeUsed() {
        return this.timeUsed;
    }

    public void setTimeUsed(long timeUsed) {
        this.timeUsed = timeUsed;
    }

    public long getMaxUse() {
        return this.maxUse;
    }

    public void setMaxUse(long maxUse) {
        this.maxUse = maxUse;
    }

    public Timestamp getFrom() {
        return this.from;
    }

    public void setFrom(Timestamp from) {
        this.from = from;
    }

    public Timestamp getExprired() {
        return this.exprired;
    }

    public void setExprired(Timestamp exprired) {
        this.exprired = exprired;
    }

    public Timestamp getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getEvent() {
        return this.event;
    }

    public void setEvent(long event) {
        this.event = event;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}

