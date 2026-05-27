/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum BankType {
    BIDV(0, "bidv", "BIDV"),
    VIETIN(1, "vietin", "VietinBank"),
    VIETCOM(2, "vcb", "VietcomBank"),
    MSB(3, "msb", "MSB");
    
    private int id;
    private String value;
    private String name;

    private BankType(int id, String value, String name) {
        this.id = id;
        this.value = value;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static BankType getBankById(int id) {
        for (BankType e : BankType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static BankType getBankByValue(String value) {
        for (BankType e : BankType.values()) {
            if (!e.getValue().equals(value)) continue;
            return e;
        }
        return null;
    }

    public static BankType getBankByName(String name) {
        for (BankType e : BankType.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }
}

