/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.vtc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dichvuthe.client.HttpClient;
import com.vinplay.dichvuthe.encode.TripleDES;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vtc.ResponseCode;
import com.vinplay.vtc.VTCRechargeRequestObj;
import com.vinplay.vtc.VTCRechargeResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class VTCRechargeClient {
    public static VTCRechargeResponse rechargeVcoinCard(String transId, String seri, String pin, String nickName) {
        VTCRechargeResponse response = new VTCRechargeResponse();
        try {
            String url = GameCommon.getValueStr("vcoin_url");
            String partnerKey = GameCommon.getValueStr("vcoin_partner_key");
            String partnerId = GameCommon.getValueStr("vcoin_partner_id");
            String functionName = "UseCard";
            String cardType = "VCOIN";
            String pinEncrypt = TripleDES.Encrypt(pin, partnerKey);
            VTCRechargeRequestObj param = new VTCRechargeRequestObj("UseCard", seri, pinEncrypt, partnerId, "VCOIN", transId, nickName, "", "");
            ObjectMapper mapper = new ObjectMapper();
            String res = HttpClient.post(url, mapper.writeValueAsString(param));
            JSONObject json = (JSONObject)new JSONParser().parse(res);
            response = VTCRechargeClient.convertToVTCRechargeResponse(json);
            ResponseCode resCode = new ResponseCode();
            int amount = 0;
            if (response.getStatus() == 1) {
                amount = Integer.parseInt(TripleDES.Decrypt(response.getDataInfo(), partnerKey));
                resCode.setStatus(0);
                resCode.setMessage("Giao dich thanh cong");
            } else if (response.getStatus() == 0) {
                resCode.setStatus(30);
                resCode.setMessage("Giao dich pending");
            } else {
                resCode = VTCRechargeClient.mappingCode(String.valueOf(response.getResponseCode()));
            }
            response.setAmount(amount);
            response.setCode(resCode.getStatus());
            response.setDescription(resCode.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            return response;
        }
        return response;
    }

    private static VTCRechargeResponse convertToVTCRechargeResponse(JSONObject jData) throws Exception {
        VTCRechargeResponse response = new VTCRechargeResponse();
        if (jData.get("ResponseCode") != null) {
            response.setResponseCode(Long.parseLong(String.valueOf(jData.get("ResponseCode"))));
        }
        if (jData.get("Status") != null) {
            response.setStatus(Integer.parseInt(String.valueOf(jData.get("Status"))));
        }
        if (jData.get("Description") != null) {
            response.setDescription(String.valueOf(jData.get("Description")));
        }
        if (jData.get("DataInfo") != null) {
            response.setDataInfo(String.valueOf(jData.get("DataInfo")));
        }
        return response;
    }

    private static ResponseCode mappingCode(String errorCode) {
        if (errorCode.isEmpty()) {
            return new ResponseCode(30, "Kh\u00f4ng nh\u1eadn \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
        }
        switch (errorCode) {
            case "-1": {
                return new ResponseCode(31, "Th\u1ebb \u0111\u00e3 s\u1eed d\u1ee5ng");
            }
            case "-2": {
                return new ResponseCode(32, "Th\u1ebb \u0111\u00e3 b\u1ecb kh\u00f3a");
            }
            case "-3": {
                return new ResponseCode(34, "Th\u1ebb h\u1ebft h\u1ea1n s\u1eed d\u1ee5ng");
            }
            case "-4": {
                return new ResponseCode(33, "Th\u1ebb ch\u01b0a k\u00edch ho\u1ea1t");
            }
            case "-5": {
                return new ResponseCode(1, "M\u00e3 \u0111\u1ed1i t\u00e1c kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-6": {
                return new ResponseCode(35, "M\u00e3 th\u1ebb v\u00e0 s\u1ed1 Serial kh\u00f4ng kh\u1edbp");
            }
            case "-8": {
                return new ResponseCode(1, "C\u1ea3nh b\u00e1o s\u1ed1 l\u1ea7n giao d\u1ecbch l\u1ed7i c\u1ee7a m\u1ed9t t\u00e0i kho\u1ea3n");
            }
            case "-9": {
                return new ResponseCode(1, "Th\u1ebb th\u1eed qu\u00e1 s\u1ed1 l\u1ea7n cho ph\u00e9p");
            }
            case "-10": {
                return new ResponseCode(36, "CardSerial kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-11": {
                return new ResponseCode(35, "CardCode kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-12": {
                return new ResponseCode(1, "Th\u1ebb kh\u00f4ng t\u1ed3n t\u1ea1i");
            }
            case "-13": {
                return new ResponseCode(1, "Ch\u1eef k\u00fd kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-14": {
                return new ResponseCode(1, "M\u00e3 d\u1ecbch v\u1ee5 kh\u00f4ng t\u1ed3n t\u1ea1i");
            }
            case "-15": {
                return new ResponseCode(1, "D\u1eef li\u1ec7u truy\u1ec1n l\u00ean kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-16": {
                return new ResponseCode(1, "M\u00e3 giao d\u1ecbch kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-17": {
                return new ResponseCode(1, "gi\u00e1 tr\u1ecb Amount truy\u1ec1n l\u00ean kh\u00f4ng h\u1ee3p l\u1ec7");
            }
            case "-18": {
                return new ResponseCode(1, "Sai do ph\u01b0\u01a1ng ph\u00e1p m\u00e3 h\u00f3a, key m\u00e3 h\u00f3a ho\u1eb7c truy\u1ec1n sai Partner ID");
            }
            case "-90": {
                return new ResponseCode(1, "Sai t\u00ean h\u00e0m");
            }
            case "-98": 
            case "-99": {
                return new ResponseCode(1, "Giao d\u1ecbch th\u1ea5t b\u1ea1i do L\u1ed7i h\u1ec7 th\u1ed1ng");
            }
            case "-999": {
                return new ResponseCode(1, "H\u1ec7 th\u1ed1ng Telco t\u1ea1m ng\u1eebng");
            }
            case "-100": {
                return new ResponseCode(30, "Giao d\u1ecbch nghi v\u1ea5n (x\u00e1c minh k\u1ebft qu\u1ea3 qua k\u00eanh \u0111\u1ed1i so\u00e1t)");
            }
        }
        return new ResponseCode(30, "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
    }
}

