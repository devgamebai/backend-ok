/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum StatusGames {
    RUN(0),
    SANDBOX(1),
    MAINTAIN(2);
    
    private int id;

    private StatusGames(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static StatusGames getById(int id) {
        for (StatusGames e : StatusGames.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }
}

