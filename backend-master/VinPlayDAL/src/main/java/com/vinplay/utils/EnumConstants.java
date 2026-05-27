/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.utils;

import java.util.HashMap;
import java.util.Map;

public interface EnumConstants {

    public static enum ErrorBank {
        SUCCESS(0, "Success"),
        ERR_TRANSACTION(-99, "Error transaction"),
        ERR_OVER_BANK_NUMBER(-1, "Qu\u00fd kh\u00e1ch \u0111\u00e3 th\u00eam \u0111\u1ee7 5 t\u00e0i kho\u1ea3n ng\u00e2n h\u00e0ng , vui l\u00f2ng li\u00ean h\u1ec7 CSKH !"),
        ERR_CUSTOMER_NAME(-2, "Ch\u1ee7 kho\u1ea3n ph\u1ea3i tr\u00f9ng v\u1edbi t\u00ean ch\u1ee7 kho\u1ea3n \u0111\u00e3 th\u00eam \u0111\u1ea7u ti\u00ean."),
        ERR_EXISTED_BANK_NUMBER(-3, "\u0110\u00e3 t\u1ed3n t\u1ea1i s\u1ed1 t\u00e0i kho\u1ea3n n\u00e0y , vui l\u00f2ng nh\u1eadp l\u1ea1i"),
        ERR_NOT_EXIST_ID(-4, "not exist id"),
        ERR_USER_NOT_EXIST(-5, "User not exist"),
        ERR_RECORD_NOT_EXIST(-6, "Record not exist"),
        ERR_CHANGE_BANKNUMBER(-7, "Qu\u00fd kh\u00e1ch kh\u00f4ng th\u1ec3 thay \u0111\u1ed5i STK ng\u00e2n h\u00e0ng . Vui l\u00f2ng li\u00ean h\u1ec7 CSKH !"),
        ERR_CHANGE_CUSTOMERNAME(-8, "Qu\u00fd kh\u00e1ch kh\u00f4ng th\u1ec3 thay \u0111\u1ed5i t\u00ean ch\u1ee7 kho\u1ea3n . Vui l\u00f2ng li\u00ean h\u1ec7 CSKH !"),
        ERR_SYSTEM(-100, "System error");

        private int key;
        private String value;
        private static final Map<Integer, String> lookup;

        private ErrorBank(int key, String value) {
            this.value = value;
            this.key = key;
        }

        public static String getValue(int key) {
            return lookup.get(key);
        }

        public String getValue() {
            return this.value;
        }

        public int getKey() {
            return this.key;
        }

        static {
            lookup = new HashMap<Integer, String>();
            for (ErrorBank d : ErrorBank.values()) {
                lookup.put(d.getKey(), d.getValue());
            }
        }
    }

    public static enum HttpStatus {
        SUCCESS(0, "Success"),
        ERR_SYSTEM(-100, "System error");

        private int key;
        private String value;

        private HttpStatus(int key, String value) {
            this.value = value;
            this.key = key;
        }

        public String getValue() {
            return this.value;
        }

        public int getKey() {
            return this.key;
        }
    }
}

