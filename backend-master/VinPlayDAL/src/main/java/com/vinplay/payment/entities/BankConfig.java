/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

public class BankConfig {
    private String key;
    private String imageUrl;
    private String name;
    private Integer status;
    private Integer isWithdraw;

    public BankConfig(String key, String imageUrl, String name, Integer status, Integer isWithdraw) {
        this.key = key;
        this.imageUrl = imageUrl;
        this.name = name;
        this.status = status;
        this.isWithdraw = isWithdraw;
    }

    public BankConfig() {
    }

    public Integer getIsWithdraw() {
        return this.isWithdraw;
    }

    public void setIsWithdraw(Integer isWithdraw) {
        this.isWithdraw = isWithdraw;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

