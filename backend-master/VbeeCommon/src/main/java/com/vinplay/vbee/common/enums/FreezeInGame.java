/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum FreezeInGame {
    MORE(1),
    SET(2),
    ALL_MIN(3),
    ALL(4);
    
    private int id;

    private FreezeInGame(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

