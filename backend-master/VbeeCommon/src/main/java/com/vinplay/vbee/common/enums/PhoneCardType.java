/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum PhoneCardType {
    _10K(0, 10000),
    _20K(1, 20000),
    _50K(2, 50000),
    _100K(3, 100000),
    _200K(4, 200000),
    _500K(5, 500000),
    _1M(6, 1000000),
    _2M(7, 2000000),
    _5M(8, 5000000);
    
    private int id;
    private int value;

    private PhoneCardType(int id, int value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static PhoneCardType getPhoneCardById(int id) {
        for (PhoneCardType e : PhoneCardType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static PhoneCardType getPhoneCardByValue(int value) {
        for (PhoneCardType e : PhoneCardType.values()) {
            if (e.getValue() != value) continue;
            return e;
        }
        return null;
    }
}

