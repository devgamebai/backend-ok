/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.utils;

import java.util.Arrays;
import java.util.List;

public interface PaymentConstant {
    public static final int MAINTAINCE = 99;
    public static final int TOO_MANY_REQUEST = 20;
    public static final int SUCCESS = 0;

    public static enum PayType {
        ONLINE(0, "ONLINE"),
        OFFLINE(1, "OFFLINE"),
        WITHDRAW(3, "WITHDRAW"),
        MOMO_DEP(4, "MOMO_DEP"),
        ZALO_DEP(5, "ZALO_DEP");

        private int key;
        private String value;

        private PayType(int key, String value) {
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

    public static interface BANK_STATUS {
        public static final int ACTIVE = 1;
        public static final int INACTIVE = 0;
    }

    public static interface PRINCEPAY {
        public static final int SILVER_PAY = 907;
        public static final int BANK_TRANS = 908;
        public static final int ZALO_PAY = 921;
        public static final int MOMO_PAY = 923;
        public static final int VIETNAM_PAY = 712;
        public static final List<Integer> PAY_TYPE = Arrays.asList(907, 908, 921, 923, 712);
    }

    public static interface PAYWELL {
        public static final String PAY_ONLINE = "IB_ONLINE";
        public static final String PAY_OFFLINE = "PW_OFFLINE";
    }

    public static interface PROVIDER {
        public static final String PAYWELL = "paywell";
        public static final String PRINCE_PAY = "princepay";
        public static final String ROYAL_PAY = "royalpay";
        public static final String CLICK_PAY = "clickpay";
        public static final String MANUAL_BANK = "manualbank";
        public static final String SC = "SC";
    }
}

