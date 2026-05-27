/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 */
package com.vinplay.dichvuthe.utils;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NapasUtils {
    static final char[] HEX_TABLE = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String hashAllFields(Map<String, String> fields) throws KeyNotFoundException {
        ArrayList<String> fieldNames = new ArrayList<String>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuffer buf = new StringBuffer();
        buf.append(GameCommon.getValueStr("NAPAS_SECRET_KEY"));
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue == null || fieldValue.length() <= 0) continue;
            buf.append(fieldValue);
        }
        MessageDigest md5 = null;
        byte[] ba = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            ba = md5.digest(buf.toString().getBytes("UTF-8"));
        }
        catch (Exception fieldValue) {
            // empty catch block
        }
        return NapasUtils.hex(ba);
    }

    private static String hex(byte[] input) {
        StringBuffer sb = new StringBuffer(input.length * 2);
        for (int i = 0; i < input.length; ++i) {
            sb.append(HEX_TABLE[input[i] >> 4 & 15]);
            sb.append(HEX_TABLE[input[i] & 15]);
        }
        return sb.toString();
    }

    public static String getRedirectUrl(Map<String, String> fields) throws UnsupportedEncodingException, KeyNotFoundException {
        StringBuffer buf = new StringBuffer();
        buf.append(GameCommon.getValueStr("NAPAS_URL"));
        buf.append("?");
        String secretKey = GameCommon.getValueStr("NAPAS_SECRET_KEY");
        if (secretKey != null && secretKey.length() > 0) {
            String secureHash = NapasUtils.hashAllFields(fields);
            fields.put("vpc_SecureHash", secureHash);
        }
        ArrayList<String> fieldNames = new ArrayList<String>(fields.keySet());
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String)itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                buf.append(URLEncoder.encode(fieldName, "UTF-8"));
                buf.append('=');
                buf.append(URLEncoder.encode(fieldValue, "UTF-8"));
            }
            if (!itr.hasNext()) continue;
            buf.append('&');
        }
        return buf.toString();
    }

    public static String getResponseDescription(String vResponseCode) {
        String result = "";
        switch (vResponseCode) {
            case "0": {
                result = "Giao d\u1ecbch th\u00e0nh c\u00f4ng";
                break;
            }
            case "1": {
                result = "Th\u1ebb ho\u1eb7c t\u00e0i kho\u1ea3n b\u1ecb kh\u00f3a";
                break;
            }
            case "2": {
                result = "Th\u00f4ng tin th\u1ebb kh\u00f4ng h\u1ee3p l\u1ec7";
                break;
            }
            case "3": {
                result = "Th\u1ebb h\u1ebft h\u1ea1n";
                break;
            }
            case "4": {
                result = "Giao d\u1ecbch kh\u00f4ng th\u1ef1c hi\u1ec7n \u0111\u01b0\u1ee3c do sai th\u00f4ng tin 3 l\u1ea7n";
                break;
            }
            case "5": {
                result = "Kh\u00f4ng nh\u1eadn \u0111\u01b0\u1ee3c tr\u1ea3 l\u1eddi c\u1ee7a Ng\u00e2n h\u00e0ng";
                break;
            }
            case "6": {
                result = "Kh\u00f4ng giao ti\u1ebfp \u0111\u01b0\u1ee3c v\u1edbi Ng\u00e2n h\u00e0ng";
                break;
            }
            case "7": {
                result = "T\u00e0i kho\u1ea3n kh\u00f4ng \u0111\u1ee7 ti\u1ec1n";
                break;
            }
            case "8": {
                result = "L\u1ed7i x\u00e1c th\u1ef1c d\u1eef li\u1ec7u (checksum)";
                break;
            }
            case "9": {
                result = "Ki\u1ec3u giao d\u1ecbch kh\u00f4ng \u0111\u01b0\u1ee3c ph\u00e9p";
                break;
            }
            case "10": {
                result = "L\u1ed7i kh\u00e1c";
                break;
            }
            case "11": {
                result = "Giao d\u1ecbch ch\u01b0a \u0111\u01b0\u1ee3c x\u00e1c th\u1ef1c OTP";
                break;
            }
            case "12": {
                result = "Giao d\u1ecbch kh\u00f4ng th\u00e0nh c\u00f4ng, th\u1ebb v\u01b0\u1ee3t h\u1ea1n m\u1ee9c thanh to\u00e1n ng\u00e0y";
                break;
            }
            case "13": {
                result = "Th\u1ebb ch\u01b0a \u0111\u0103ng k\u00fd s\u1eed d\u1ee5ng d\u1ecbch v\u1ee5 giao d\u1ecbch qua Internet";
                break;
            }
            case "14": {
                result = "Sai OTP";
                break;
            }
            case "15": {
                result = "Sai m\u1eadt kh\u1ea9u";
                break;
            }
            case "16": {
                result = "Sai t\u00ean ch\u1ee7 th\u1ebb";
                break;
            }
            case "17": {
                result = "Sai s\u1ed1 th\u1ebb";
                break;
            }
            case "18": {
                result = "Sai ng\u00e0y ph\u00e1t h\u00e0nh";
                break;
            }
            case "19": {
                result = "Sai ng\u00e0y h\u1ebft h\u1ea1n";
                break;
            }
            case "20": {
                result = "OTP timeout";
                break;
            }
            case "21": {
                result = "OTP timeout";
                break;
            }
            case "22": {
                result = "Ch\u01b0a x\u00e1c th\u1ef1c th\u00f4ng tin th\u1ebb";
                break;
            }
            case "23": {
                result = "Kh\u00f4ng \u0111\u1ee7 \u0111i\u1ec1u ki\u1ec7n thanh to\u00e1n (Th\u1ebb/T\u00e0i kho\u1ea3n kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c TK kh\u00f4ng \u0111\u1ee7 s\u1ed1 d\u01b0)";
                break;
            }
            case "24": {
                result = "GD kh\u00f4ng th\u00e0nh c\u00f4ng, s\u1ed1 ti\u1ec1n GD v\u01b0\u1ee3t qu\u00e1 h\u1ea1n m\u1ee9c 1 l\u1ea7n thanh to\u00e1n";
                break;
            }
            case "25": {
                result = "GD kh\u00f4ng th\u00e0nh c\u00f4ng, GD v\u01b0\u1ee3t qu\u00e1 h\u1ea1n m\u1ee9c thanh to\u00e1n";
                break;
            }
            case "26": {
                result = "Giao d\u1ecbch ch\u1edd x\u00e1c nh\u1eadn t\u1eeb NHPH";
                break;
            }
            case "27": {
                result = "Sai th\u00f4ng tin x\u00e1c th\u1ef1c ( \u00e1p d\u1ee5ng cho c\u00e1c NH th\u1ef1c hi\u1ec7n x\u00e1c th\u1ef1c qua Internet Banking c\u1ee7a NH)";
                break;
            }
            case "28": {
                result = "Timeout giao d\u1ecbch";
                break;
            }
            case "29": {
                result = "L\u1ed7i x\u1eed l\u00fd giao d\u1ecbch t\u1ea1i h\u1ec7 th\u1ed1ng NHPH";
                break;
            }
            default: {
                result = "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            }
        }
        return result;
    }

    public static String getQueryData(String url) throws IOException {
        URL queryURL = new URL(url);
        BufferedReader in = new BufferedReader(new InputStreamReader(queryURL.openStream()));
        String inputLine = null;
        inputLine = in.readLine();
        in.close();
        return inputLine;
    }
}

