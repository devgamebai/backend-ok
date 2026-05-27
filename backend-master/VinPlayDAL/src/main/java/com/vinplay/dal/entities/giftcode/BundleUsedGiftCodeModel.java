/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.giftcode;

import java.sql.Timestamp;

public class BundleUsedGiftCodeModel {
    private Integer id;
    private String username;
    private String giftcode;
    private Timestamp created_at;

    public BundleUsedGiftCodeModel() {
    }

    public BundleUsedGiftCodeModel(Integer id, String username, String giftcode, Timestamp created_at) {
        this.id = id;
        this.username = username;
        this.giftcode = giftcode;
        this.created_at = created_at;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGiftcode() {
        return this.giftcode;
    }

    public void setGiftcode(String giftcode) {
        this.giftcode = giftcode;
    }

    public Timestamp getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
}

