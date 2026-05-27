/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums.vippoint;

public enum EventVPTopIntelPrize {
    _1(1, "Moto Kawasaki Z800", "275,000,000 Vin"),
    _2(2, "Honda SH 2017 150cc ABS", "90,000,000 Vin"),
    _3(3, "Honda SH 2017 125cc ABS", "76,000,000 Vin"),
    _4(4, "Iphone 7 plus 128 GB", "22,000,000 Vin"),
    _5(5, "Iphone 7 plus 128 GB", "22,000,000 Vin"),
    _6(6, "10,000,000 Vin", "10,000,000 Vin"),
    _7(7, "8,000,000 Vin", "8,000,000 Vin"),
    _8(8, "5,000,000 Vin", "5,000,000 Vin"),
    _9(9, "5,000,000 Vin", "5,000,000 Vin"),
    _10(10, "3,000,000 Vin", "3,000,000 Vin"),
    _11(11, "3,000,000 Vin", "3,000,000 Vin"),
    _12(12, "3,000,000 Vin", "3,000,000 Vin"),
    _13(13, "3,000,000 Vin", "3,000,000 Vin"),
    _14(14, "3,000,000 Vin", "3,000,000 Vin"),
    _15(15, "2,000,000 Vin", "2,000,000 Vin"),
    _16(16, "2,000,000 Vin", "2,000,000 Vin"),
    _17(17, "2,000,000 Vin", "2,000,000 Vin"),
    _18(18, "2,000,000 Vin", "2,000,000 Vin"),
    _19(19, "2,000,000 Vin", "2,000,000 Vin"),
    _20(20, "1,000,000 Vin", "1,000,000 Vin"),
    _21(21, "1,000,000 Vin", "1,000,000 Vin"),
    _22(22, "1,000,000 Vin", "1,000,000 Vin"),
    _23(23, "1,000,000 Vin", "1,000,000 Vin"),
    _24(24, "1,000,000 Vin", "1,000,000 Vin"),
    _25(25, "1,000,000 Vin", "1,000,000 Vin"),
    _26(26, "1,000,000 Vin", "1,000,000 Vin"),
    _27(27, "1,000,000 Vin", "1,000,000 Vin"),
    _28(28, "1,000,000 Vin", "1,000,000 Vin"),
    _29(29, "1,000,000 Vin", "1,000,000 Vin"),
    _30(30, "1,000,000 Vin", "1,000,000 Vin"),
    _31(31, "1,000,000 Vin", "1,000,000 Vin"),
    _32(32, "1,000,000 Vin", "1,000,000 Vin"),
    _33(33, "1,000,000 Vin", "1,000,000 Vin"),
    _34(34, "1,000,000 Vin", "1,000,000 Vin"),
    _35(35, "1,000,000 Vin", "1,000,000 Vin"),
    _36(36, "1,000,000 Vin", "1,000,000 Vin"),
    _37(37, "1,000,000 Vin", "1,000,000 Vin"),
    _38(38, "1,000,000 Vin", "1,000,000 Vin"),
    _39(39, "1,000,000 Vin", "1,000,000 Vin"),
    _40(40, "1,000,000 Vin", "1,000,000 Vin"),
    _41(41, "1,000,000 Vin", "1,000,000 Vin"),
    _42(42, "1,000,000 Vin", "1,000,000 Vin"),
    _43(43, "1,000,000 Vin", "1,000,000 Vin"),
    _44(44, "1,000,000 Vin", "1,000,000 Vin"),
    _45(45, "1,000,000 Vin", "1,000,000 Vin"),
    _46(46, "1,000,000 Vin", "1,000,000 Vin"),
    _47(47, "1,000,000 Vin", "1,000,000 Vin"),
    _48(48, "1,000,000 Vin", "1,000,000 Vin"),
    _49(49, "1,000,000 Vin", "1,000,000 Vin"),
    _50(50, "1,000,000 Vin", "1,000,000 Vin");
    
    private int id;
    private String value;
    private String valueVin;

    private EventVPTopIntelPrize(int id, String value, String valueVin) {
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

    public static EventVPTopIntelPrize getById(int id) {
        for (EventVPTopIntelPrize e : EventVPTopIntelPrize.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }
}

