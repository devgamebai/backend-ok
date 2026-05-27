/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum I2BNLType {
    BIDV(0, "BIDV", "BIDV"),
    VietinBank(1, "ICB", "VietinBank"),
    VietcomBank(2, "VCB", "VietcomBank"),
    MaritimeBank(3, "MSB", "MaritimeBank"),
    VPBank(4, "VPB", "VPBank"),
    VietABank(5, "VAB", "VietABank"),
    TechcomBank(6, "TCB", "TechcomBank"),
    EximBank(7, "EXB", "EximBank"),
    VIBBank(8, "VIB", "VIBBank"),
    TPBank(9, "TPB", "TPBank"),
    SHB(10, "SHB", "SHB"),
    SacomBank(12, "SCB", "SacomBank"),
    OceanBank(13, "OJB", "OceanBank"),
    MBBank(14, "MB", "MBBank"),
    GPBank(15, "GPB", "GPBank"),
    AgriBank(17, "AGB", "AgriBank"),
    ACB(19, "ACB", "ACB"),
    DongABank(22, "DAB", "DongABank"),
    HDBank(24, "HDB", "HDBank"),
    NamABank(26, "NAB", "NamABank");
    
    private int id;
    private String value;
    private String name;

    private I2BNLType(int id, String value, String name) {
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

    public static I2BNLType getBankById(int id) {
        for (I2BNLType e : I2BNLType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static I2BNLType getBankByValue(String value) {
        for (I2BNLType e : I2BNLType.values()) {
            if (!e.getValue().equals(value)) continue;
            return e;
        }
        return null;
    }

    public static I2BNLType getBankByName(String name) {
        for (I2BNLType e : I2BNLType.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }
}

