/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum ProviderType {
    VIETTEL(0, "vtt", "Viettel", 0),
    VINAPHONE(1, "vnp", "Vinaphone", 0),
    MOBIFONE(2, "vms", "Mobifone", 0),
    VNM(3, "vnm", "VietNamMobile", 0),
    BEELINE(4, "bee", "BeeLine", 0),
    GATE(5, "gate", "Gate", 1),
    ZING(6, "zing", "Zing", 1),
    VCOIN(7, "vcoin", "Vcoin", 1),
    SFONE(8, "sfone", "SFone", 0),
    GMOBILE(9, "gtel", "GMobile", 0),
    GARENA(10, "garena", "Garena", 1),
    VINPLAY(11, "vin", "Vinplay", 1);
    
    private int id;
    private String value;
    private String name;
    private int type;

    private ProviderType(int id, String value, String name, int type) {
        this.id = id;
        this.value = value;
        this.name = name;
        this.type = type;
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

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static ProviderType getProviderById(int id) {
        for (ProviderType e : ProviderType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static ProviderType getProviderByName(String name) {
        for (ProviderType e : ProviderType.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }

    public static ProviderType getProviderByValue(String value) {
        for (ProviderType e : ProviderType.values()) {
            if (!e.getValue().equals(value)) continue;
            return e;
        }
        return null;
    }
}

