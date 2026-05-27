/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums.vippoint;

public enum EventVPTopStrongPrize {
    _1(1, "Honda SH 2017 150cc ABS", "90,000,000 Vin"),
    _2(2, "Iphone 7 plus 128 GB", "22,000,000 Vin"),
    _3(3, "Iphone 7 128 GB", "18,000,000 Vin"),
    _4(4, "10,000,000 Vin", "10,000,000 Vin"),
    _5(5, "8,000,000 Vin", "8,000,000 Vin"),
    _6(6, "5,000,000 Vin", "5,000,000 Vin"),
    _7(7, "3,000,000 Vin", "3,000,000 Vin"),
    _8(8, "2,000,000 Vin", "2,000,000 Vin"),
    _9(9, "2,000,000 Vin", "2,000,000 Vin"),
    _10(10, "2,000,000 Vin", "2,000,000 Vin"),
    _11(11, "1,000,000 Vin", "1,000,000 Vin"),
    _12(12, "1,000,000 Vin", "1,000,000 Vin"),
    _13(13, "1,000,000 Vin", "1,000,000 Vin"),
    _14(14, "1,000,000 Vin", "1,000,000 Vin"),
    _15(15, "1,000,000 Vin", "1,000,000 Vin"),
    _16(16, "1,000,000 Vin", "1,000,000 Vin"),
    _17(17, "1,000,000 Vin", "1,000,000 Vin"),
    _18(18, "1,000,000 Vin", "1,000,000 Vin"),
    _19(19, "1,000,000 Vin", "1,000,000 Vin"),
    _20(20, "1,000,000 Vin", "1,000,000 Vin");
    
    private int id;
    private String value;
    private String valueVin;

    private EventVPTopStrongPrize(int id, String value, String valueVin) {
        this.id = id;
        this.value = value;
        this.valueVin = valueVin;
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

    public String getValueVin() {
        return this.valueVin;
    }

    public void setValueVin(String valueVin) {
        this.valueVin = valueVin;
    }

    public static EventVPTopStrongPrize getById(int id) {
        for (EventVPTopStrongPrize e : EventVPTopStrongPrize.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }
}

