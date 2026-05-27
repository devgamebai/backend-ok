/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class StatusUser {
    public static final int BAN_LOGIN = 1;
    public static final int BAN_CASH_OUT = 2;
    public static final int CAN_LOGIN_SANDBOX = 4;
    public static final int BAN_TRANSFER_MONEY = 8;
    public static final int HAS_MOBILE_SECURITY = 16;
    public static final int HAS_EMAIL_SECURITY = 32;
    public static final int HAS_APP_SECURITY = 64;
    public static final int HAS_LOGIN_SECURITY = 128;

    public static int changeStatus(int status, int index, String newBit) {
        String binary = Integer.toBinaryString(status);
        while (binary.length() <= 64) {
            binary = "0" + binary;
        }
        String binaryNew = binary.substring(0, binary.length() - index - 1) + newBit + binary.substring(binary.length() - index);
        int newSrarus = Integer.parseInt(binaryNew, 2);
        return newSrarus;
    }

    public static int changeStatusEx(int status, int indexStart, int indexEnd, String newBit) {
        String binary = Integer.toBinaryString(status);
        while (binary.length() <= 64) {
            binary = "0" + binary;
        }
        String binaryNew = binary.substring(0, binary.length() - indexEnd - 1) + newBit + binary.substring(binary.length() - indexStart);
        int newSrarus = Integer.parseInt(binaryNew, 2);
        return newSrarus;
    }

    public static boolean checkStatus(int status, int id) {
        boolean res = false;
        if ((status & 1 << id) != 0) {
            res = true;
        }
        return res;
    }
}

