/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum I2BType {
    BIDV(0, "BIDV", "BIDV"),
    VietinBank(1, "CTG", "VietinBank"),
    VietcomBank(2, "VCB", "VietcomBank"),
    MaritimeBank(3, "MSB", "MaritimeBank"),
    VPBank(4, "VPB", "VPBank"),
    VietABank(5, "VAB", "VietABank"),
    TechcomBank(6, "TCB", "TechcomBank"),
    EximBank(7, "EIB", "EximBank"),
    VIBBank(8, "VIB", "VIBBank"),
    TPBank(9, "TPB", "TPBank"),
    SHB(10, "SHB", "SHB"),
    SeaBank(11, "SEAB", "SeaBank"),
    SacomBank(12, "STB", "SacomBank"),
    OceanBank(13, "OJB", "OceanBank"),
    MBBank(14, "MB", "MBBank"),
    GPBank(15, "GPB", "GPBank"),
    BacABank(16, "NASB", "BacABank"),
    AgriBank(17, "VARB", "AgriBank"),
    ABBank(18, "ABB", "ABBank"),
    ACB(19, "ACB", "ACB"),
    OricomBank(20, "OCB", "OricomBank"),
    LienVietPostBank(21, "LPB", "LienVietPostBank"),
    DongABank(22, "DAB", "DongABank"),
    BaoVietBank(23, "BVB", "BaoVietBank"),
    HDBank(24, "HDB", "HDBank"),
    KienLongBank(25, "KLB", "KienLongBank"),
    NamABank(26, "NAB", "NamABank"),
    NCB(27, "", "NCB"),
    VRB(28, "VRB", "VRB"),
    SML(50, "SML", "SmartlinkCard");
    
    private int id;
    private String value;
    private String name;

    private I2BType(int id, String value, String name) {
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

    public static I2BType getBankById(int id) {
        for (I2BType e : I2BType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static I2BType getBankByValue(String value) {
        for (I2BType e : I2BType.values()) {
            if (!e.getValue().equals(value)) continue;
            return e;
        }
        return null;
    }

    public static I2BType getBankByName(String name) {
        for (I2BType e : I2BType.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }
}

