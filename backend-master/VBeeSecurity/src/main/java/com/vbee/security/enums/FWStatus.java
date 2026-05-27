/*
 * Decompiled with CFR 0.144.
 */
package com.vbee.security.enums;

public enum FWStatus {
    OPEN(1, "OPEN"),
    CLOSE(2, "CLOSE"),
    BLOCK(3, "BLOCK");
    
    private int id;
    private String name;

    private FWStatus(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.id;
    }
}

