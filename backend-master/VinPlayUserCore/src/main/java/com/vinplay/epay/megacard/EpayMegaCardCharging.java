/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 */
package com.vinplay.epay.megacard;

import com.vinplay.epay.MD5;
import com.vinplay.epay.megacard.ChargeReponse;
import com.vinplay.epay.megacard.LoginResponse;
import com.vinplay.epay.megacard.RSAEncryption;
import com.vinplay.epay.megacard.ServicesSoapBindingStub;
import com.vinplay.epay.megacard.TripleDESEncryption;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vtc.ResponseCode;
import java.net.URL;
import javax.xml.rpc.Service;

public class EpayMegaCardCharging {
    private static String mega_url;
    private static String partnerCode;
    private static String partnerId;
    private static String mpin;
    private static String user;
    private static String pass;
    private static String publicKey;
    private static String privateKey;

    public static void init() {
        try {
            if (GameCommon.getValueInt("MEGA_IS_VAT") == 0) {
                mega_url = GameCommon.getValueStr("MEGA_URL");
                partnerCode = GameCommon.getValueStr("MEGA_PARTNER_CODE");
                partnerId = GameCommon.getValueStr("MEGA_PARTNER_ID");
                mpin = GameCommon.getValueStr("MEGA_MPIN");
                user = GameCommon.getValueStr("MEGA_USER");
                pass = GameCommon.getValueStr("MEGA_PASS");
                publicKey = GameCommon.getValueStr("MEGA_PUBLIC_KEY");
                privateKey = GameCommon.getValueStr("MEGA_PRIVATE_KEY");
            } else if (GameCommon.getValueInt("MEGA_IS_VAT") == 1) {
                mega_url = GameCommon.getValueStr("MEGA_URL_VAT");
                partnerCode = GameCommon.getValueStr("MEGA_PARTNER_CODE_VAT");
                partnerId = GameCommon.getValueStr("MEGA_PARTNER_ID_VAT");
                mpin = GameCommon.getValueStr("MEGA_MPIN_VAT");
                user = GameCommon.getValueStr("MEGA_USER_VAT");
                pass = GameCommon.getValueStr("MEGA_PASS_VAT");
                publicKey = GameCommon.getValueStr("MEGA_PUBLIC_KEY_VAT");
                privateKey = GameCommon.getValueStr("MEGA_PRIVATE_KEY_VAT");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateConfigMega(String userNameMega) {
        try {
            if (GameCommon.getValueStr("MEGA_USER").equals(userNameMega)) {
                mega_url = GameCommon.getValueStr("MEGA_URL");
                partnerCode = GameCommon.getValueStr("MEGA_PARTNER_CODE");
                partnerId = GameCommon.getValueStr("MEGA_PARTNER_ID");
                mpin = GameCommon.getValueStr("MEGA_MPIN");
                user = GameCommon.getValueStr("MEGA_USER");
                pass = GameCommon.getValueStr("MEGA_PASS");
                publicKey = GameCommon.getValueStr("MEGA_PUBLIC_KEY");
                privateKey = GameCommon.getValueStr("MEGA_PRIVATE_KEY");
            } else if (GameCommon.getValueStr("MEGA_USER_VAT").equals(userNameMega)) {
                mega_url = GameCommon.getValueStr("MEGA_URL_VAT");
                partnerCode = GameCommon.getValueStr("MEGA_PARTNER_CODE_VAT");
                partnerId = GameCommon.getValueStr("MEGA_PARTNER_ID_VAT");
                mpin = GameCommon.getValueStr("MEGA_MPIN_VAT");
                user = GameCommon.getValueStr("MEGA_USER_VAT");
                pass = GameCommon.getValueStr("MEGA_PASS_VAT");
                publicKey = GameCommon.getValueStr("MEGA_PUBLIC_KEY_VAT");
                privateKey = GameCommon.getValueStr("MEGA_PRIVATE_KEY_VAT");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String login() {
        try {
            URL url = new URL(mega_url);
            ServicesSoapBindingStub service = new ServicesSoapBindingStub(url, null);
            RSAEncryption rsa = new RSAEncryption();
            rsa.setPrivateKeyStr(privateKey);
            rsa.setPublicKeyStr(publicKey);
            String encryptpass = rsa.encrypt(pass);
            LoginResponse response = service.login(user, encryptpass, partnerId);
            ResponseCode responseCode = EpayMegaCardCharging.mappingCode(response.getStatus());
            if (responseCode.getStatus() == 0) {
                return rsa.decrypt(response.getSessionid());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "";
    }

    public static ChargeReponse chargingMegaCard(String sessionId, String transId, String serial, String pin) {
        ChargeReponse response = new ChargeReponse();
        try {
            URL url = new URL(mega_url);
            ServicesSoapBindingStub service = new ServicesSoapBindingStub(url, null);
            String strTransID = partnerCode + "_" + transId;
            String card_data = serial.trim() + ":" + pin.trim() + "::MGC";
            String m5Session = MD5.hash(sessionId);
            String enCard_date = TripleDESEncryption.encrypt(sessionId, card_data);
            String enMpin = TripleDESEncryption.encrypt(sessionId, mpin);
            response = service.cardCharging(strTransID, user, partnerId, enMpin, "", enCard_date, m5Session);
            Thread.sleep(500L);
            ResponseCode responseCode = EpayMegaCardCharging.mappingCode(response.getStatus());
            if (responseCode.getStatus() == 0) {
                response.setAmount(TripleDESEncryption.decrypt(sessionId, response.getResponseamount()));
            } else {
                response.setAmount("0");
            }
            response.setMessage(responseCode.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    public static ChargeReponse getTransactionStatus(String sessionId, String transId) {
        ChargeReponse response = new ChargeReponse();
        try {
            URL url = new URL(mega_url);
            ServicesSoapBindingStub service = new ServicesSoapBindingStub(url, null);
            String m5Session = MD5.hash(sessionId);
            String strTransID = partnerCode + "_" + transId;
            response = service.getTransactionStatus(strTransID, user, partnerId, m5Session);
            ResponseCode responseCode = EpayMegaCardCharging.mappingCode(response.getStatus());
            if (responseCode.getStatus() == 0) {
                response.setAmount(TripleDESEncryption.decrypt(sessionId, response.getResponseamount()));
            } else {
                response.setAmount("0");
            }
            response.setMessage(responseCode.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private static ResponseCode mappingCode(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return new ResponseCode(30, "Kh\u00f4ng nh\u1eadn \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
        }
        switch (errorCode) {
            case "-24": {
                return new ResponseCode(1, "D\u1eef li\u1ec7u card data kh\u00f4ng \u0111\u00fang");
            }
            case "-11": {
                return new ResponseCode(1, "Nh\u00e0 cung c\u1ea5p kh\u00f4ng t\u1ed3n t\u1ea1i");
            }
            case "-10": {
                return new ResponseCode(1, "M\u00e3 th\u1ebb sai \u0111\u1ecbnh d\u1ea1ng");
            }
            case "0": {
                return new ResponseCode(1, "Transaction fail");
            }
            case "1": {
                return new ResponseCode(0, "Giao d\u1ecbch th\u00e0nh c\u00f4ng");
            }
            case "3": {
                return new ResponseCode(1, "Sai Session");
            }
            case "4": {
                return new ResponseCode(1, "Th\u1ebb kh\u00f4ng s\u1eed d\u1ee5ng \u0111\u01b0\u1ee3c");
            }
            case "5": {
                return new ResponseCode(1, "Partner nh\u1eadp sai m\u00e3 th\u1ebb qu\u00e1 5 l\u1ea7n");
            }
            case "7": {
                return new ResponseCode(1, "Session h\u1ebft h\u1ea1n");
            }
            case "8": {
                return new ResponseCode(1, "Sai Ip");
            }
            case "9": {
                return new ResponseCode(1, "T\u1ea1m th\u1eddi kh\u00f3a k\u00eanh n\u1ea1p VMS do qu\u00e1 t\u1ea3i");
            }
            case "10": {
                return new ResponseCode(1, "H\u1ec7 th\u1ed1ng nh\u00e0 cung c\u1ea5p g\u1eb7p l\u1ed7i");
            }
            case "11": {
                return new ResponseCode(1, "K\u1ebft n\u1ed1i v\u1edbi nh\u00e0 cung c\u1ea5p t\u1ea1m th\u1eddi b\u1ecb gi\u00e1n \u0111o\u1ea1n");
            }
            case "12": {
                return new ResponseCode(1, "Tr\u00f9ng transactionID v\u1edbi m\u1ed9t giao d\u1ecbch tr\u01b0\u1edbc \u0111\u00f3");
            }
            case "13": {
                return new ResponseCode(1, "H\u1ec7 th\u1ed1ng t\u1ea1m th\u1eddi b\u1eadn");
            }
            case "-2": {
                return new ResponseCode(1, "Th\u1ebb \u0111\u00e3 b\u1ecb kh\u00f3a");
            }
            case "-3": {
                return new ResponseCode(1, "Th\u1ebb h\u1ebft h\u1ea1n s\u1eed d\u1ee5ng");
            }
            case "50": {
                return new ResponseCode(1, "Th\u1ebb \u0111\u00e3 s\u1eed d\u1ee5ng ho\u1eb7c kh\u00f4ng t\u1ed3n t\u1ea1i");
            }
            case "51": {
                return new ResponseCode(1, "Seri th\u1ebb kh\u00f4ng \u0111\u00fang");
            }
            case "52": {
                return new ResponseCode(1, "M\u00e3 th\u1ebb v\u00e0 serial kh\u00f4ng kh\u1edbp");
            }
            case "53": {
                return new ResponseCode(1, "Serial ho\u1eb7c m\u00e3 th\u1ebb kh\u00f4ng \u0111\u00fang");
            }
            case "55": {
                return new ResponseCode(1, "Card t\u1ea1m th\u1eddi b\u1ecb block 24h");
            }
            case "62": {
                return new ResponseCode(1, "Sai m\u1eadt kh\u1ea9u");
            }
            case "57": {
                return new ResponseCode(1, "Sai mpin");
            }
            case "58": {
                return new ResponseCode(1, "Sai tham s\u1ed1 \u0111\u1ea7u v\u00e0o");
            }
            case "59": {
                return new ResponseCode(1, "M\u00e3 th\u1ebb ch\u01b0a \u0111\u01b0\u1ee3c k\u00edch ho\u1ea1t");
            }
            case "60": {
                return new ResponseCode(1, "Sai partnerid");
            }
            case "61": {
                return new ResponseCode(1, "Sai user");
            }
            case "56": {
                return new ResponseCode(1, "Target account t\u1ea1m th\u1eddi b\u1ecb kh\u00f3a do charging sai nhi\u1ec1u l\u1ea7n");
            }
            case "16": {
                return new ResponseCode(1, "M\u00e3 giao d\u1ecbch kh\u00f4ng t\u1ed3n t\u1ea1i");
            }
            case "63": {
                return new ResponseCode(1, "Kh\u00f4ng t\u00ecm th\u1ea5y giao d\u1ecbch n\u00e0y");
            }
            case "64": {
                return new ResponseCode(1, "Gi\u00e3i m\u00e3 d\u1eef li\u1ec7u fail m\u1eadt kh\u1ea9u, ho\u1eb7c mpin g\u1eedi l\u00ean kh\u00f4ng th\u00e0nh c\u00f4ng");
            }
            case "65": {
                return new ResponseCode(1, "S\u1ed1 l\u01b0\u1ee3ng k\u1ebft n\u1ed1i c\u1ee7a partner qu\u00e1 m\u1ee9c cho ph\u00e9p");
            }
            case "66": {
                return new ResponseCode(1, "M\u00e3 th\u1ebb \u0111\u00e3 g\u1eedi m\u1ed9t giao d\u1ecbch th\u00e0nh c\u00f4ng l\u00ean h\u1ec7 th\u1ed1ng");
            }
            case "67": {
                return new ResponseCode(1, "Serial ho\u1eb7c m\u00e3 th\u1ebb kh\u00f4ng \u0111\u00fang \u0111\u1ecbnh d\u1ea1ng c\u1ee7a h\u1ec7 th\u1ed1ng");
            }
            case "99": {
                return new ResponseCode(30, "Ch\u01b0a nh\u1eadn \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1 t\u1eeb nh\u00e0 cung c\u1ea5p m\u00e3 th\u1ebb");
            }
            case "68": {
                return new ResponseCode(1, "Nh\u00e0 cung c\u1ea5p b\u1ecb l\u1ed7i ho\u1eb7c \u0111\u00e3 b\u1ecb kh\u00f3a");
            }
        }
        return new ResponseCode(30, "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
    }
}

