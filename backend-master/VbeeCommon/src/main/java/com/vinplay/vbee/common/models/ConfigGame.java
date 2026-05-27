/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class ConfigGame {
    private int id;
    private String name;
    private byte status;

    public ConfigGame() {
    }

    public ConfigGame(int id, String name, byte status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getStatus() {
        return this.status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}

