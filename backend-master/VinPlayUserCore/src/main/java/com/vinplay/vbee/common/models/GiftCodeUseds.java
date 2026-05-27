/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

import java.sql.Timestamp;

public class GiftCodeUseds {
    private long giftcodeId;
    private String username;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String idNumber;

    public long getGiftcodeId() {
        return this.giftcodeId;
    }

    public void setGiftcodeId(long giftcodeId) {
        this.giftcodeId = giftcodeId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIdNumber() {
        return this.idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
}

