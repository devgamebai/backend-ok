/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.statics;

public enum TransType {
    START_TRANS(1),
    IN_TRANS(2),
    END_TRANS(3),
    VIPPOINT(4),
    NO_VIPPOINT(5);
    
    private int id;

    private TransType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

