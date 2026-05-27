/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.SoftpinJson
 *  javax.xml.rpc.Service
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package com.vinplay.epay;

import com.vinplay.dichvuthe.encode.RSA;
import com.vinplay.dichvuthe.entities.SoftpinObj;
import com.vinplay.dichvuthe.entities.TopupObj;
import com.vinplay.epay.DownloadSoftpinResult;
import com.vinplay.epay.InterfacesSoapBindingStub;
import com.vinplay.epay.TopupResult;
import com.vinplay.epay.TripleDES;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.models.SoftpinJson;
import com.vinplay.vtc.ResponseCode;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.rpc.Service;
import org.json.JSONArray;
import org.json.JSONObject;

public class EpayClient {
    private static InterfacesSoapBindingStub service = null;

    public static InterfacesSoapBindingStub getService() throws Exception {
        try {
            if (service == null) {
                URL oUrl = new URL(GameCommon.getValueStr("CDV_WEBSERVICE_URL"));
                service = new InterfacesSoapBindingStub(oUrl, null);
                service.setTimeout(60000);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return service;
    }

    public static boolean echo() {
        try {
            if (EpayClient.getService().echo() == 0) {
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static SoftpinObj downloadSoftpin(String id, String provider, int amount, int quantity) throws Exception {
        SoftpinObj response = new SoftpinObj();
        if (!EpayClient.echo()) {
            response.setStatus(1);
            return response;
        }
        try {
            String transactionId = GameCommon.getValueStr("CDV_PARTNER_NAME") + "_" + id;
            String dataSign = transactionId + GameCommon.getValueStr("CDV_PARTNER_NAME") + EpayClient.mappingProvider(provider) + String.valueOf(amount) + String.valueOf(quantity);
            String sign = RSA.sign(dataSign, GameCommon.getValueStr("CDV_PRIVATE_KEY"));
            DownloadSoftpinResult result = EpayClient.getService().downloadSoftpin(transactionId, GameCommon.getValueStr("CDV_PARTNER_NAME"), EpayClient.mappingProvider(provider), amount, quantity, sign);
            if (result != null) {
                ResponseCode responseCode = EpayClient.mappingCode(String.valueOf(result.getErrorCode()));
                response.setProvider(provider);
                response.setAmount(amount);
                response.setQuantity(quantity);
                response.setStatus(result.getErrorCode());
                response.setMessage(responseCode.getMessage());
                response.setPartnerTransId(transactionId);
                response.setPartner("epay");
                response.setSign(sign);
                if (result.getErrorCode() == 0) {
                    String data = TripleDES.Decrypt(result.getListCards(), GameCommon.getValueStr("CDV_KEY_SOFTPIN"));
                    ArrayList<SoftpinJson> softpinList = new ArrayList<SoftpinJson>();
                    JSONArray jsonArray = new JSONObject(data).getJSONArray("listCards");
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String st = obj.toString();
                        String[] split = st.split("\\|");
                        SoftpinJson softpinJson = new SoftpinJson();
                        softpinJson.setProvider(EpayClient.mappingProviderReverse(split[0]));
                        softpinJson.setAmount(Integer.parseInt(split[1]));
                        softpinJson.setSerial(split[2]);
                        softpinJson.setPin(split[3]);
                        softpinJson.setExpire(split[4]);
                        softpinList.add(softpinJson);
                    }
                    response.setSoftpinList(softpinList);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Exception: " + e.getMessage());
            throw e;
        }
        return response;
    }

    public static SoftpinObj reDownloadSoftpin(String transactionId, String provider, int amount, int quantity) throws Exception {
        SoftpinObj response = new SoftpinObj();
        if (!EpayClient.echo()) {
            response.setStatus(1);
            return response;
        }
        try {
            String dataSign = transactionId + GameCommon.getValueStr("CDV_PARTNER_NAME");
            String sign = RSA.sign(dataSign, GameCommon.getValueStr("CDV_PRIVATE_KEY"));
            DownloadSoftpinResult result = EpayClient.getService().reDownloadSoftpin(transactionId, GameCommon.getValueStr("CDV_PARTNER_NAME"), sign);
            if (result != null) {
                ResponseCode responseCode = EpayClient.mappingCode(String.valueOf(result.getErrorCode()));
                response.setProvider(provider);
                response.setAmount(amount);
                response.setQuantity(quantity);
                response.setStatus(result.getErrorCode());
                response.setMessage(responseCode.getMessage());
                response.setPartnerTransId(transactionId);
                response.setPartner("epay");
                response.setSign(sign);
                if (result.getErrorCode() == 0) {
                    String data = TripleDES.Decrypt(result.getListCards(), GameCommon.getValueStr("CDV_KEY_SOFTPIN"));
                    ArrayList<SoftpinJson> softpinList = new ArrayList<SoftpinJson>();
                    JSONArray jsonArray = new JSONObject(data).getJSONArray("listCards");
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String st = obj.toString();
                        String[] split = st.split("\\|");
                        SoftpinJson softpinJson = new SoftpinJson();
                        softpinJson.setProvider(EpayClient.mappingProviderReverse(split[0]));
                        softpinJson.setAmount(Integer.parseInt(split[1]));
                        softpinJson.setSerial(split[2]);
                        softpinJson.setPin(split[3]);
                        softpinJson.setExpire(split[4]);
                        softpinList.add(softpinJson);
                    }
                    response.setSoftpinList(softpinList);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Exception: " + e.getMessage());
            throw e;
        }
        return response;
    }

    public static TopupObj topupTelco(String id, String target, byte type, int amount) throws Exception {
        TopupObj response = new TopupObj();
        if (!EpayClient.echo()) {
            response.setStatus(1);
            return response;
        }
        try {
            String provider = EpayClient.mapProviderByTarget(target);
            String requestId = GameCommon.getValueStr("CDV_PARTNER_NAME") + "_" + id;
            String dataSign = requestId + GameCommon.getValueStr("CDV_PARTNER_NAME") + EpayClient.mappingProvider(provider) + String.valueOf(target) + String.valueOf(amount);
            String sign = RSA.sign(dataSign, GameCommon.getValueStr("CDV_PRIVATE_KEY"));
            TopupResult result = EpayClient.getService().topup(requestId, GameCommon.getValueStr("CDV_PARTNER_NAME"), EpayClient.mappingProvider(provider), target, amount, sign);
            if (result != null) {
                ResponseCode responseCode = EpayClient.mappingCode(String.valueOf(result.getErrorCode()));
                response.setId(requestId);
                response.setTarget(target);
                response.setType(type);
                response.setAmount(amount);
                response.setStatus(result.getErrorCode());
                response.setMessage(responseCode.getMessage());
                response.setSign(sign);
                response.setProvider(provider);
                response.setPartnerTransId(requestId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Exception: " + e.getMessage());
            throw e;
        }
        return response;
    }

    private static String mappingProviderReverse(String provider) {
        if (provider == null) {
            return "null";
        }
        switch (provider) {
            case "VTT": {
                return "vtt";
            }
            case "VNP": {
                return "vnp";
            }
            case "VMS": {
                return "vms";
            }
            case "VNM": {
                return "vnm";
            }
            case "BEE": {
                return "gtel";
            }
            case "GATE": {
                return "gate";
            }
            case "ZING": {
                return "zing";
            }
            case "VCOIN": {
                return "vcoin";
            }
        }
        return provider;
    }

    private static String mappingProvider(String provider) {
        if (provider == null) {
            return "null";
        }
        switch (provider) {
            case "vtt": {
                return "VTT";
            }
            case "vnp": {
                return "VNP";
            }
            case "vms": {
                return "VMS";
            }
            case "vnm": {
                return "VNM";
            }
            case "gtel": {
                return "BEE";
            }
            case "gate": {
                return "GATE";
            }
            case "zing": {
                return "ZING";
            }
            case "vcoin": {
                return "VCOIN";
            }
        }
        return provider;
    }

    private static String mapProviderByTarget(String target) {
        if (target == null) {
            return "null";
        }
        if (target.matches("^(096|097|098|0162|0163|0164|0165|0166|0167|0168|0169|086|082)[\\d]{7}$")) {
            return "vtt";
        }
        if (target.matches("^(091|094|0123|0124|0125|0127|0129|088)[\\d]{7}$")) {
            return "vnp";
        }
        if (target.matches("^(090|093|0120|0121|0122|0126|0128|089)[\\d]{7}$")) {
            return "vms";
        }
        if (target.matches("^(092|0188|0186)[\\d]{7}$")) {
            return "vnm";
        }
        if (target.matches("^(099|0199)[\\d]{7}$")) {
            return "gtel";
        }
        return "other";
    }

    private static ResponseCode mappingCode(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return new ResponseCode(30, "Kh\u00f4ng nh\u1eadn \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
        }
        switch (errorCode) {
            case "0": {
                return new ResponseCode(0, "Giao d\u1ecbch th\u00e0nh c\u00f4ng.");
            }
            case "23": {
                return new ResponseCode(30, "T\u00e0i kho\u1ea3n \u0111ang \u0111\u01b0\u1ee3c n\u1ea1p ti\u1ec1n.");
            }
            case "99": {
                return new ResponseCode(30, "Giao d\u1ecbch \u0111ang pending.");
            }
            case "10": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n \u0111ang b\u1ecb kh\u00f3a.");
            }
            case "11": {
                return new ResponseCode(1, "T\u00ean partner kh\u00f4ng \u0111\u00fang.");
            }
            case "12": {
                return new ResponseCode(1, "\u0110\u1ecba ch\u1ec9 IP kh\u00f4ng cho ph\u00e9p.");
            }
            case "13": {
                return new ResponseCode(1, "M\u00e3 \u0111\u01a1n h\u00e0ng b\u1ecb l\u1ed7i.");
            }
            case "14": {
                return new ResponseCode(1, "M\u00e3 \u0111\u01a1n h\u00e0ng \u0111\u00e3 t\u1ed3n t\u1ea1i.");
            }
            case "15": {
                return new ResponseCode(1, "M\u00e3 \u0111\u01a1n h\u00e0ng kh\u00f4ng t\u1ed3n t\u1ea1i.");
            }
            case "17": {
                return new ResponseCode(1, "Sai t\u1ed5ng ti\u1ec1n.");
            }
            case "21": {
                return new ResponseCode(1, "Sai ch\u1eef k\u00fd.");
            }
            case "22": {
                return new ResponseCode(1, "D\u1eef li\u1ec7u g\u1eedi l\u00ean r\u1ed7ng ho\u1eb7c c\u00f3 k\u00fd t\u1ef1 \u0111\u1eb7c bi\u00eat.");
            }
            case "30": {
                return new ResponseCode(1, "S\u1ed1 d\u01b0 kh\u1ea3 d\u1ee5ng kh\u00f4ng \u0111\u1ee7.");
            }
            case "31": {
                return new ResponseCode(1, "Chi\u1ebft kh\u1ea5u ch\u01b0a \u0111\u01b0\u1ee3c c\u1eadp nh\u1eadt cho partner.");
            }
            case "32": {
                return new ResponseCode(1, "Partner ch\u01b0a c\u1eadp nh\u1eadt Public key.");
            }
            case "33": {
                return new ResponseCode(1, "Partner ch\u01b0a \u0111\u01b0\u1ee3c set IP.");
            }
            case "35": {
                return new ResponseCode(1, "H\u1ec7 th\u1ed1ng \u0111ang b\u1eadn.");
            }
            case "52": {
                return new ResponseCode(1, "Lo\u1ea1i h\u00ecnh thanh to\u00e1n kh\u00f4ng h\u1ed7 tr\u1ee3.");
            }
            case "101": {
                return new ResponseCode(1, "M\u00e3 giao d\u1ecbch truy\u1ec1n l\u00ean sai \u0111\u1ecbnh d\u1ea1ng.");
            }
            case "102": {
                return new ResponseCode(1, "M\u00e3 giao d\u1ecbch \u0111\u00e3 t\u1ed3n t\u1ea1i.");
            }
            case "103": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n n\u1ea1p ti\u1ec1n b\u1ecb sai.");
            }
            case "104": {
                return new ResponseCode(1, "Sai m\u00e3 nh\u00e0 cung c\u1ea5p ho\u1eb7c nh\u00e0 cung c\u1ea5p h\u1ec7 th\u1ed1ng kh\u00f4ng h\u1ed7 tr\u1ee3.");
            }
            case "105": {
                return new ResponseCode(1, "M\u1ec7nh gi\u00e1 n\u1ea1p ti\u1ec1n kh\u00f4ng h\u1ed7 tr\u1ee3.");
            }
            case "106": {
                return new ResponseCode(1, "M\u1ec7nh gi\u00e1 th\u1ebb kh\u00f4ng t\u1ed3n t\u1ea1i.");
            }
            case "107": {
                return new ResponseCode(1, "Th\u1ebb trong kho kh\u00f4ng \u0111\u1ee7 cho giao d\u1ecbch.");
            }
            case "108": {
                return new ResponseCode(1, "S\u1ed1 l\u01b0\u1ee3ng th\u1ebb mua v\u01b0\u1ee3t gi\u1edbi h\u1ea1n cho ph\u00e9p.");
            }
            case "109": {
                return new ResponseCode(1, "K\u00eanh n\u1ea1p ti\u1ec1n \u0111ang b\u1ea3o tr\u00ec.");
            }
            case "110": {
                return new ResponseCode(1, "Giao d\u1ecbch th\u1ea5t b\u1ea1i.");
            }
            case "111": {
                return new ResponseCode(1, "M\u00e3 giao d\u1ecbch kh\u00f4ng t\u1ed3n t\u1ea1i.");
            }
            case "112": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n ch\u01b0a c\u00f3 key m\u00e3 h\u00f3a softpin.");
            }
            case "113": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n nh\u1eadn ti\u1ec1n kh\u00f4ng \u0111\u00fang.");
            }
        }
        return new ResponseCode(30, "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
    }
}

