/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.minigame;

import java.io.Serializable;

public class TopWin
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private long money;

    public TopWin() {
    }

    public TopWin(String username, long money) {
        this.username = username;
        this.money = money;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}

