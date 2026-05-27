/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.api.entities;

public class UserTransaction {
    private long money;
    private String name;
    private long time;
    private String type;

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

