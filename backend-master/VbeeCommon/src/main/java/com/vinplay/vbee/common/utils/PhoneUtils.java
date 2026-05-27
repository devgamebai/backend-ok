/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

public class PhoneUtils {
    private static String[] M_VIETTEL = new String[]{"096", "097", "098", "086", "0162", "0163", "0164", "0165", "0166", "0167", "0168", "0169"};
    private static String[] M_VINAPHONE = new String[]{"091", "094", "0123", "0124", "0125", "0127", "0129", "088"};
    private static String[] M_MOBIFONE = new String[]{"090", "093", "0120", "0121", "0122", "0126", "0128", "089"};
    private static String[] M_VNM = new String[]{"092", "0188", "0186"};
    private static String[] M_BEELINE = new String[]{"099", "0199"};
    private static final int VIETEL = 0;
    private static final int VINAPHONE = 1;
    private static final int MOBIFONE = 2;
    private static final int VNM = 3;
    private static final int BEELINE = 4;
    private static final int OTHER = 5;
    private static final String[] TIENG_VIET = new String[]{"\u0102", "\u00c2", "\u0110", "\u00ca", "\u00d4", "\u01a0", "\u01af"};

    public static int getProviderByMobile(String mobile, boolean isBrandname) {
        int res = -1;
        if (mobile != null && !mobile.isEmpty()) {
            if (mobile.substring(0, 2).equals("84")) {
                mobile = "0" + mobile.substring(2);
            }
            for (String m : M_VIETTEL) {
                if (!mobile.startsWith(m)) continue;
                return 0;
            }
            for (String m : M_VINAPHONE) {
                if (!mobile.startsWith(m)) continue;
                return 1;
            }
            for (String m : M_MOBIFONE) {
                if (!mobile.startsWith(m)) continue;
                return 2;
            }
            if (!isBrandname) {
                for (String m : M_VNM) {
                    if (!mobile.startsWith(m)) continue;
                    return 3;
                }
                for (String m : M_BEELINE) {
                    if (!mobile.startsWith(m)) continue;
                    return 4;
                }
            }
            return 5;
        }
        return res;
    }

    public static int getNumMessage(String content, boolean isBrandname) {
        int num = 0;
        if (content != null && !content.isEmpty()) {
            int l = content.length();
            if (!isBrandname) {
                num = l / 160;
                if (l % 160 != 0) {
                    ++num;
                }
            } else if (PhoneUtils.checkStringUnicode(content)) {
                num = l / 70;
                if (l % 70 != 0) {
                    ++num;
                }
            } else if (l <= 160) {
                num = 1;
            } else if (l <= 306) {
                num = 2;
            } else if (l <= 459) {
                num = 3;
            } else {
                num = l / 160;
                if (l % 160 != 0) {
                    ++num;
                }
            }
        }
        return num;
    }

    public static boolean checkStringUnicode(String s) {
        if (s != null && !s.isEmpty()) {
            String sCheck = s.toUpperCase();
            for (String st : TIENG_VIET) {
                if (!sCheck.contains(st)) continue;
                return true;
            }
        }
        return false;
    }
}

