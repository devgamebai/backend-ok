/*
 * Decompiled with CFR 0.152.
 */
package com.payment.provider.thesieutoc;

public enum Status {
    CHUA_NHAP_API_KEY("54", "Ch\u01b0a Nh\u1eadp API key"),
    SAI_THONG_TIN_API("1", "Sai Th\u00f4ng Tin API"),
    TAI_KHOAN_BI_KHOA("3", "t\u00e0i kho\u1ea3n \u0111\u00e3 b\u1ecb kh\u00f3a"),
    THE_DANG_BAO_TRI("-1089", "Th\u1ebb \u0110ang B\u1ea3o Tr\u00ec"),
    THE_DA_DUOC_SU_DUNG("2", "Th\u1ebb \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng tr\u00ean h\u1ec7 th\u1ed1ng"),
    CHUA_NHAP_SERI("56", "Ch\u01b0a Nh\u1eadp Seri"),
    CHUA_NHAP_MA_THE("55", "Ch\u01b0a Nh\u1eadp M\u00e3 Th\u1ebb"),
    CHUA_CHON_MENH_GIA("52", "Ch\u01b0a Ch\u1ecdn M\u1ec7nh Gi\u00e1"),
    THE_CHO_XU_LY("00", "Th\u1ebb \u0111\u00e3 g\u1eedi l\u00ean h\u1ec7 th\ufffd\ufffdng ch\u1edd x\u1eed l\u00fd!"),
    LOI_KHONG_XAC_DINH("47", "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh");

    private final String code;
    private final String description;

    private Status(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Status findByStatus(String status) {
        for (Status s : Status.values()) {
            if (!s.code.equals(status)) continue;
            return s;
        }
        return null;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}

