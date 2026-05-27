/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 */
package com.vinplay.dichvuthe.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DvtUtils {
    public static final Integer[] SMS_AMOUNT = new Integer[]{1000, 2000, 3000, 4000, 5000, 10000, 15000, 20000, 30000, 50000, 100000};
    public static final String MA_NAP = "NAP";

    public static void initDVT(boolean check) {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap map = instance.getMap("cacheConfig");
        if (!check || !map.containsKey("VIN_CARD_SYSTEM_DAILY")) {
            map.put("VIN_CARD_SYSTEM_DAILY", "0");
        }
    }

    public static void errorDvt(HazelcastInstance client, String actionName) {
        IMap mapDvt = client.getMap("cacheDvt");
        if (mapDvt.containsKey(actionName)) {
            mapDvt.put(actionName, ((Integer)mapDvt.get(actionName) + 1));
        } else {
            mapDvt.put(actionName, 1);
        }
    }

    public static long checkRechargeFail(int rechargeFail, Date rechargeFailTime) throws NumberFormatException, KeyNotFoundException {
        long time = 0L;
        if (rechargeFail > 0 && rechargeFailTime != null) {
            time = (long)Math.round(rechargeFail / GameCommon.getValueInt("NUM_RECHARGE_FAIL")) * 60L - (long)Math.round((new Date().getTime() - rechargeFailTime.getTime()) / 60000L);
        }
        return time;
    }

    public static long checkRechargeFailLucky79(int rechargeFail, Date rechargeFailTime) throws NumberFormatException, KeyNotFoundException {
        long time = 0L;
        if (rechargeFail < GameCommon.getValueInt("NUM_RECHARGE_FAIL")) {
            return time;
        }
        if (rechargeFail > 0 && rechargeFailTime != null) {
            time = (long)Math.round(rechargeFail / GameCommon.getValueInt("NUM_RECHARGE_FAIL")) * 15L - (long)Math.round((new Date().getTime() - rechargeFailTime.getTime()) / 60000L);
        }
        return time;
    }

    public static long checkApiOtpFail(int apiOtpFail, Date apiOtpFailTime) throws NumberFormatException, KeyNotFoundException {
        long time = 0L;
        if (apiOtpFail > 0 && apiOtpFailTime != null) {
            time = apiOtpFail / GameCommon.getValueInt("API_OTP_FAIL_NUM_LOCK") * 60 - Math.round((new Date().getTime() - apiOtpFailTime.getTime()) / 60000L);
        }
        return time;
    }

    public static long checkCodeGameFail(int fail, Date failTime) throws NumberFormatException, KeyNotFoundException {
        long time = 0L;
        if (fail > 0 && failTime != null) {
            time = Math.round((double)fail / 5.0) * 60L - (long)Math.round((new Date().getTime() - failTime.getTime()) / 60000L);
        }
        return time;
    }

    public static long checkApiOtpTimeDelay(Date apiOtpFailTime) throws NumberFormatException, KeyNotFoundException {
        long time = 0L;
        if (apiOtpFailTime != null) {
            time = GameCommon.getValueInt("API_OTP_FAIL_DELAY") - Math.round((new Date().getTime() - apiOtpFailTime.getTime()) / 60000L);
        }
        return time;
    }

    public static String revertMobile0To84(String mobile) {
        if (mobile.substring(0, 1).equals("0")) {
            return "84" + mobile.substring(1);
        }
        return mobile;
    }

    public static String revertMobile84To(String mobile) {
        if (mobile.substring(0, 2).equals("84")) {
            return "0" + mobile.substring(2);
        }
        return mobile;
    }

    public static int calculateMessageBN(String message) {
        int len;
        int n = 1;
        if (message != null && !message.isEmpty() && (len = message.length()) > 160) {
            if (len <= 306) {
                n = 2;
            } else if (len <= 459) {
                n = 3;
            }
        }
        return n;
    }

    public static String getProviderSMS(String telco) {
        String provider = "";
        switch (telco) {
            case "vnp": {
                provider = "Vinaphone";
                break;
            }
            case "vms": {
                provider = "Mobifone";
                break;
            }
            case "vtm": {
                provider = "Viettel";
            }
        }
        return provider;
    }

    public static long getAmountSMSFromMaNap(String nap) {
        long amount = 0L;
        switch (nap) {
            case "NAP1": {
                amount = 1000L;
                break;
            }
            case "NAP2": {
                amount = 2000L;
                break;
            }
            case "NAP3": {
                amount = 3000L;
                break;
            }
            case "NAP4": {
                amount = 4000L;
                break;
            }
            case "NAP5": {
                amount = 5000L;
                break;
            }
            case "NAP10": {
                amount = 10000L;
                break;
            }
            case "NAP15": {
                amount = 15000L;
                break;
            }
            case "NAP20": {
                amount = 20000L;
                break;
            }
            case "NAP30": {
                amount = 30000L;
                break;
            }
            case "NAP50": {
                amount = 50000L;
                break;
            }
            case "NAP100": {
                amount = 100000L;
            }
        }
        return amount;
    }

    public static int getAmountSMSFromShortCode(String shortCode) {
        int amount = 0;
        switch (shortCode) {
            case "8198": {
                amount = 1000;
                break;
            }
            case "8298": {
                amount = 2000;
                break;
            }
            case "8398": {
                amount = 3000;
                break;
            }
            case "8498": {
                amount = 4000;
                break;
            }
            case "8598": {
                amount = 5000;
                break;
            }
            case "8698": {
                amount = 10000;
                break;
            }
            case "8798": {
                amount = 15000;
            }
        }
        return amount;
    }

    public static String getSMSPlusHMAC(Map<String, String> fields) throws UnsupportedEncodingException, KeyNotFoundException {
        StringBuffer buf = new StringBuffer();
        ArrayList<String> fieldNames = new ArrayList<String>(fields.keySet());
        Collections.sort(fieldNames);
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String)itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                buf.append(fieldName);
                buf.append('=');
                buf.append(fieldValue);
            }
            if (!itr.hasNext()) continue;
            buf.append('&');
        }
        return buf.toString();
    }

    public static int getCodeApiOTP(String errorCode, boolean confirm) {
        switch (errorCode) {
            case "00": {
                return 0;
            }
            case "07": {
                return 2;
            }
            case "08": {
                return 5;
            }
            case "10": {
                return 3;
            }
            case "11": {
                return 4;
            }
            case "16": {
                return 6;
            }
            case "18": {
                return confirm? 1: 0;
            }
        }
        return 1;
    }

    public static String getDesbyCodeApiOTP(int code) {
        switch (code) {
            case 0: {
                return "Th\u00c3\u00a0nh c\u00c3\u00b4ng";
            }
            case 2: {
                return "Thu\u00c3\u00aa bao kh\u00c3\u00b4ng h\u00e1\u00bb\u00a3p l\u00e1\u00bb\u2021";
            }
            case 5: {
                return "Kh\u00c3\u00b4ng g\u00e1\u00bb\u00adi \u00c4\u2018\u00c6\u00b0\u00e1\u00bb\u00a3c m\u00c3\u00a3 OTP";
            }
            case 3: {
                return "T\u00c3\u00a0i kho\u00e1\u00ba\u00a3n kh\u00c3\u00b4ng \u00c4\u2018\u00e1\u00bb\u00a7 ti\u00e1\u00bb\ufffdn";
            }
            case 4: {
                return "OTP sai";
            }
            case 6: {
                return "S\u00e1\u00bb\u2018 ti\u00e1\u00bb\ufffdn n\u00e1\u00ba\u00a1p trong ng\u00c3\u00a0y l\u00e1\u00bb\u203an h\u00c6\u00a1n 500.000 vnd";
            }
        }
        return "L\u00e1\u00bb\u2014i h\u00e1\u00bb\u2021 th\u00e1\u00bb\u2018ng";
    }
}

