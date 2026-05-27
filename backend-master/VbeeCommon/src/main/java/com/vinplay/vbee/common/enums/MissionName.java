/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum MissionName {
    SAM(0, "SAM", "Sam", 0, 0),
    BA_CAY(1, "BA_CAY", "BaCay", 0, 0),
    BINH(2, "BINH", "Binh", 0, 0),
    TLMN(3, "TLMN", "Tlmn", 0, 0),
    TALA(4, "TALA", "TaLa", 0, 1),
    LIENG(5, "LIENG", "Lieng", 0, 0),
    XI_TO(6, "XI_TO", "XiTo", 0, 1),
    BAI_CAO(7, "BAI_CAO", "BaiCao", 0, 0),
    POKER(8, "POKER", "Poker", 0, 0),
    POKER_TOUR(9, "POKER_TOUR", "PokerTour", 0, 1),
    XOC_DIA(10, "XOC_DIA", "XocDia", 0, 1),
    XI_DZACH(11, "XI_DZACH", "XiDzach", 0, 1),
    CARO(12, "CARO", "Caro", 0, 0),
    CO_TUONG(13, "CO_TUONG", "CoTuong", 0, 0),
    CO_VUA(14, "CO_VUA", "CoVua", 0, 1),
    CO_UP(15, "CO_UP", "CoUp", 0, 0),
    TAI_XIU(16, "TAI_XIU", "TaiXiu", 1, 0),
    KHO_BAU(17, "KHO_BAU", "KhoBau", 2, 1),
    NU_DIEP_VIEN(18, "NU_DIEP_VIEN", "NuDiepVien", 2, 1),
    SIEU_ANH_HUNG(19, "SIEU_ANH_HUNG", "SieuAnhHung", 2, 1),
    VUONG_QUOC_VIN(20, "VUONG_QUOC_VIN", "VuongQuocVin", 2, 1);
    
    private int id;
    private String value;
    private String name;
    private int type;
    private int show;

    private MissionName(int id, String value, String name, int type, int show) {
        this.id = id;
        this.value = value;
        this.name = name;
        this.type = type;
        this.show = show;
    }

    public static MissionName getMissionById(int id) {
        for (MissionName e : MissionName.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static MissionName getMissionByName(String name) {
        for (MissionName e : MissionName.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }

    public static MissionName getMissionByValue(String value) {
        for (MissionName e : MissionName.values()) {
            if (!e.getValue().equals(value)) continue;
            return e;
        }
        return null;
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

    public int getShow() {
        return this.show;
    }

    public void setShow(int show) {
        this.show = show;
    }
}

