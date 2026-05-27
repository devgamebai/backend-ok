/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.giftcode;

import java.sql.Timestamp;

public class GiftCodeUsedModel {
    private Integer giftcode_id;
    private String username;
    private Timestamp created_at;
    private Timestamp updated_at;
    private String id_number;
    public String giftcode;
    public int type;
    public int money;
    public int time_used = 0;
    public int max_use;
    public Timestamp from;
    public Timestamp exprired;
    public int event = 0;

    public GiftCodeUsedModel() {
    }

    public GiftCodeUsedModel(Integer giftcode_id, String username, Timestamp created_at, Timestamp updated_at, String id_number) {
        this.giftcode_id = giftcode_id;
        this.username = username;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.id_number = id_number;
    }

    public Integer getGiftcode_id() {
        return this.giftcode_id;
    }

    public void setGiftcode_id(Integer giftcode_id) {
        this.giftcode_id = giftcode_id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Timestamp getUpdated_at() {
        return this.updated_at;
    }

    public void setUpdated_at(Timestamp updated_at) {
        this.updated_at = updated_at;
    }

    public String getId_number() {
        return this.id_number;
    }

    public void setId_number(String id_number) {
        this.id_number = id_number;
    }
}

