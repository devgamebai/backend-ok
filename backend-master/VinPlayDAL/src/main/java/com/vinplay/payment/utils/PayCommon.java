/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Hex
 */
package com.vinplay.payment.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

public class PayCommon {
    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString(array[i] & 0xFF | 0x100).substring(1, 3));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getHMACSHA1(String value, String key) throws Exception {
        try {
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(value.getBytes());
            byte[] hexBytes = new Hex().encode(rawHmac);
            return new String(hexBytes, "UTF-8");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getHMACSHA256(String key, String data) throws Exception {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("ASCII"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return Hex.encodeHexString((byte[])sha256_HMAC.doFinal(data.getBytes("ASCII")));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static enum PAYSTATUS {
        PENDING(0, "pending", "Dang cho xu ly"),
        RECEIVED(1, "received", "Da nhan va dang xu ly"),
        SUCCESS(2, "success", "Da xu ly thanh cong"),
        FAILED(3, "failed", "Da xu ly that bai"),
        COMPLETED(4, "completed", "Giao dich hoan tat"),
        REVIEW(5, "review", "Dang xem xet"),
        SPAM(11, "spam", "Yeu cau bi gui qua nhieu lan"),
        REQUEST(12, "request", "Yeu cau rut tien");

        private Integer id;
        private String key;
        private String alias;

        public Integer getId() {
            return this.id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getKey() {
            return this.key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAlias() {
            return this.alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        private PAYSTATUS(Integer id, String key, String alias) {
            this.id = id;
            this.key = key;
            this.alias = alias;
        }

        public static PAYSTATUS getById(Integer id) {
            for (PAYSTATUS payStatus : PAYSTATUS.values()) {
                if (!payStatus.getId().equals(id)) continue;
                return payStatus;
            }
            return null;
        }

        public static PAYSTATUS getByKey(String key) {
            for (PAYSTATUS payStatus : PAYSTATUS.values()) {
                if (!payStatus.getKey().equals(key)) continue;
                return payStatus;
            }
            return null;
        }
    }
}

