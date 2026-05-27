/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import java.util.Calendar;

public class GameFreeCodePackage {
    public int id;
    public String gamename;
    public int type;
    public int quantity;
    public int amount;
    public Calendar expire;
    public Calendar createTime;
    public String creater;

    public GameFreeCodePackage(int id, String gamename, int type, int quantity, int amount, Calendar expire, Calendar createTime, String creater) {
        this.id = id;
        this.gamename = gamename;
        this.type = type;
        this.quantity = quantity;
        this.amount = amount;
        this.expire = expire;
        this.createTime = createTime;
        this.creater = creater;
    }

    public GameFreeCodePackage() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGamename() {
        return this.gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Calendar getExpire() {
        return this.expire;
    }

    public void setExpire(Calendar expire) {
        this.expire = expire;
    }

    public Calendar getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Calendar createTime) {
        this.createTime = createTime;
    }

    public String getCreater() {
        return this.creater;
    }

    public void setCreater(String creater) {
        this.creater = creater;
    }
}

