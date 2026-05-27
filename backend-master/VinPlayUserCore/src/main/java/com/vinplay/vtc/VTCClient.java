/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.SoftpinJson
 *  javax.xml.rpc.Service
 */
package com.vinplay.vtc;

import com.vinplay.dichvuthe.encode.RSA;
import com.vinplay.dichvuthe.entities.SoftpinObj;
import com.vinplay.dichvuthe.entities.TopupObj;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.models.SoftpinJson;
import com.vinplay.vtc.GenReqData;
import com.vinplay.vtc.ResponseCode;
import com.vinplay.vtc.tempuri.GoodsPaygateSoapStub;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.rpc.Service;

public class VTCClient {
    private static GoodsPaygateSoapStub service = null;

    public static GoodsPaygateSoapStub getService() throws Exception {
        if (service == null) {
            URL oUrl = new URL(GameCommon.getValueStr("VTC_SERVICE_URL"));
            service = new GoodsPaygateSoapStub(oUrl, null);
            service.setTimeout(40000);
        }
        return service;
    }

    public static SoftpinObj buyCard(String id, String provider, String price, int quantity) throws Exception {
        SoftpinObj response = new SoftpinObj();
        try {
            String serviceCode = VTCClient.mappingServiceCodeSoftpin(provider);
            String transDate = VTCClient.getCurrentTime();
            String dataSign = serviceCode + "-" + price + "-" + quantity + "-" + GameCommon.getValueStr("VTC_PARTNER_CODE") + "-" + transDate + "-" + id;
            String sign = RSA.sign(dataSign, GameCommon.getValueStr("VTC_PRIVATE_KEY"));
            String requestTransactionResult = VTCClient.getService().requestTransaction(new GenReqData().buyCard(serviceCode, Integer.parseInt(price), quantity, transDate, id, sign), GameCommon.getValueStr("VTC_PARTNER_CODE"), "BuyCard", "1.0");
            String[] split = requestTransactionResult.split("\\|", -1);
            ResponseCode responseCode = VTCClient.mappingCode(split[0]);
            if ("1".equals(split[0])) {
                response = VTCClient.getCards(provider, price, quantity, split[2]);
            }
            response.setId(split[1]);
            response.setProvider(provider);
            response.setAmount(Integer.parseInt(price));
            response.setQuantity(quantity);
            response.setStatus(Integer.parseInt(split[0]));
            response.setMessage(responseCode.getMessage());
            response.setPartnerTransId(split[2]);
            response.setSign(split[4]);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Exception: " + e.getMessage());
            throw e;
        }
        return response;
    }

    public static SoftpinObj getCards(String provider, String price, int quantity, String orgTransId) throws Exception {
        SoftpinObj response = new SoftpinObj();
        try {
            String serviceCode = VTCClient.mappingServiceCodeSoftpin(provider);
            String dataSign = serviceCode + "-" + price + "-" + GameCommon.getValueStr("VTC_PARTNER_CODE") + "-" + orgTransId;
            String sign = RSA.sign(dataSign, GameCommon.getValueStr("VTC_PRIVATE_KEY"));
            String requestTransactionResult = VTCClient.getService().requestTransaction(new GenReqData().getCard(serviceCode, Integer.parseInt(price), quantity, orgTransId, sign), GameCommon.getValueStr("VTC_PARTNER_CODE"), "GetCard", "1.0");
            String decryptData = new GenReqData().decrypt(GameCommon.getValueStr("VTC_PARTNER_SECRET_KEY"), requestTransactionResult);
            String[] split = decryptData.split("\\|", -1);
            String resCode = split[0];
            String orgTranID = split[1];
            String listCard = decryptData.substring(decryptData.indexOf("|", 2) + 1);
            ResponseCode responseCode = VTCClient.mappingCode(resCode);
            if ("1".equals(resCode)) {
                String[] subCard = listCard.split("\\|", -1);
                ArrayList<SoftpinJson> softpinList = new ArrayList<SoftpinJson>();
                for (int i = 0; i < subCard.length; ++i) {
                    String card = subCard[i];
                    String[] splitCard = card.split(":", -1);
                    SoftpinJson softpinJson = new SoftpinJson();
                    softpinJson.setProvider(provider);
                    softpinJson.setAmount(Integer.parseInt(price));
                    softpinJson.setPin(splitCard[0]);
                    softpinJson.setSerial(splitCard[1]);
                    softpinJson.setExpire(splitCard[2]);
                    softpinList.add(softpinJson);
                }
                response.setSoftpinList(softpinList);
            }
            response.setId(orgTranID);
            response.setProvider(provider);
            response.setAmount(Integer.parseInt(price));
            response.setQuantity(quantity);
            response.setStatus(Integer.parseInt(resCode));
            response.setMessage(responseCode.getMessage());
            response.setSign(sign);
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
        try {
            String serviceCode = VTCClient.mappingServiceCodeTopup(VTCClient.mapProviderByTarget(target), type);
            String transDate = VTCClient.getCurrentTime();
            String dataSign = serviceCode + "-" + target + "-" + amount + "-" + GameCommon.getValueStr("VTC_PARTNER_CODE") + "-" + transDate + "-" + id;
            String sign = RSA.sign(dataSign, GameCommon.getValueStr("VTC_PRIVATE_KEY"));
            String requestTransactionResult = VTCClient.getService().requestTransaction(new GenReqData().topupTelco(serviceCode, target, amount, transDate, id, sign), GameCommon.getValueStr("VTC_PARTNER_CODE"), "TopupTelco", "1.0");
            String[] split = requestTransactionResult.split("\\|", -1);
            ResponseCode responseCode = VTCClient.mappingCode(split[0]);
            response.setId(id);
            response.setTarget(target);
            response.setType(type);
            response.setAmount(amount);
            response.setStatus(Integer.parseInt(split[0]));
            response.setMessage(responseCode.getMessage());
            response.setSign(split[3]);
            response.setProvider(VTCClient.mapProviderByTarget(target));
            response.setPartnerTransId(split[1]);
        }
        catch (SocketTimeoutException timeout) {
            timeout.printStackTrace();
            response.setMessage("Exception: " + timeout.getMessage());
            throw timeout;
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Exception: " + e.getMessage());
            throw e;
        }
        return response;
    }

    private static String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return df.format(new Date());
    }

    private static ResponseCode mappingCode(String errorCode) {
        if (errorCode.isEmpty()) {
            return new ResponseCode(30, "Kh\u00f4ng nh\u1eadn \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
        }
        switch (errorCode) {
            case "1": {
                return new ResponseCode(0, "Giao d\u1ecbch th\u00e0nh c\u00f4ng.");
            }
            case "-55": {
                return new ResponseCode(1, "S\u1ed1 d\u01b0 t\u00e0i kho\u1ea3n kh\u00f4ng \u0111\u1ee7 \u0111\u1ec3 th\u1ef1c hi\u1ec7n giao d\u1ecbch n\u00e0y.");
            }
            case "-302": {
                return new ResponseCode(1, "Partner kh\u00f4ng t\u1ed3n t\u1ea1i ho\u1eb7c \u0111ang t\u1ea1m d\u1eebng ho\u1ea1t \u0111\u1ed9ng.");
            }
            case "-304": {
                return new ResponseCode(1, "D\u1ecbch v\u1ee5 n\u00e0y kh\u00f4ng t\u1ed3n t\u1ea1i ho\u1eb7c \u0111ang t\u1ea1m d\u1eebng.");
            }
            case "-305": {
                return new ResponseCode(1, "Ch\u1eef k\u00fd kh\u00f4ng h\u1ee3p l\u1ec7.");
            }
            case "-306": {
                return new ResponseCode(1, "M\u1ec7nh gi\u00e1 kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c \u0111ang t\u1ea1m d\u1eebng.");
            }
            case "-307": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n n\u1ea1p ti\u1ec1n kh\u00f4ng t\u1ed3n t\u1ea1i ho\u1eb7c kh\u00f4ng h\u1ee3p l\u1ec7.");
            }
            case "-308": {
                return new ResponseCode(1, "RequesData kh\u00f4ng h\u1ee3p l\u1ec7.");
            }
            case "-309": {
                return new ResponseCode(1, "Ng\u00e0y giao d\u1ecbch truy\u1ec1n kh\u00f4ng \u0111\u00fang.");
            }
            case "-310": {
                return new ResponseCode(1, "H\u1ebft h\u1ea1n m\u1ee9c cho ph\u00e9p s\u1eed d\u1ee5ng d\u1ecbch v\u1ee5 n\u00e0y.");
            }
            case "-311": {
                return new ResponseCode(1, "RequesData ho\u1eb7c PartnerCode kh\u00f4ng \u0111\u00fang.");
            }
            case "-315": {
                return new ResponseCode(1, "Ph\u1ea3i truy\u1ec1n CommandType.");
            }
            case "-316": {
                return new ResponseCode(1, "Ph\u1ea3i truy\u1ec1n version.");
            }
            case "-317": {
                return new ResponseCode(1, "S\u1ed1 l\u01b0\u1ee3ng th\u1ebb kh\u00f4ng h\u1ee3p l\u1ec7.");
            }
            case "-318": {
                return new ResponseCode(1, "ServiceCode kh\u00f4ng \u0111\u00fang.");
            }
            case "-320": {
                return new ResponseCode(1, "H\u1ec7 th\u1ed1ng gi\u00e1n \u0111o\u1ea1n.");
            }
            case "-348": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n b\u1ecb Block.");
            }
            case "-350": {
                return new ResponseCode(1, "T\u00e0i kho\u1ea3n kh\u00f4ng t\u1ed3n t\u1ea1i.");
            }
            case "-500": {
                return new ResponseCode(1, "Lo\u1ea1i th\u1ebb n\u00e0y trong kho hi\u1ec7n \u0111\u00e3 h\u1ebft ho\u1eb7c t\u1ea1m ng\u1eebng xu\u1ea5t.");
            }
            case "-501": {
                return new ResponseCode(1, "Giao d\u1ecbch kh\u00f4ng th\u00e0nh c\u00f4ng.");
            }
            case "-502": {
                return new ResponseCode(1, "Kh\u00f4ng t\u1ed3n t\u1ea1i giao d\u1ecbch.");
            }
            case "-503": {
                return new ResponseCode(1, "\u0110\u1ed1i t\u00e1c kh\u00f4ng \u0111\u01b0\u01a1c th\u1ef1c hi\u1ec7n  ch\u1ee9c n\u0103ng n\u00e0y.");
            }
            case "-509": {
                return new ResponseCode(1, "Giao d\u1ecbch b\u1ecb h\u1ee7y (th\u1ea5t b\u1ea1i).");
            }
            case "-600": {
                return new ResponseCode(1, "Qu\u00e1 h\u1ea1n m\u1ee9c.");
            }
            case "0": {
                return new ResponseCode(30, "Giao d\u1ecbch ch\u01b0a x\u00e1c \u0111\u1ecbnh.");
            }
            case "-1": {
                return new ResponseCode(30, "L\u1ed7i h\u1ec7 th\u1ed1ng.");
            }
            case "-99": {
                return new ResponseCode(30, "L\u1ed7i ch\u01b0a x\u00e1c \u0111\u1ecbnh.");
            }
            case "-290": {
                return new ResponseCode(30, "Th\u00f4ng tin l\u1ec7nh n\u1ea1p ti\u1ec1n h\u1ee3p l\u1ec7. \u0110ang ch\u1edd k\u1ebft qu\u1ea3 x\u1eed l\u00fd.");
            }
            case "-504": {
                return new ResponseCode(30, "M\u00e3 giao d\u1ecbch n\u00e0y \u0111\u00e3 check qu\u00e1 t\u1ed1i  \u0111a s\u1ed1 l\u1ea7n cho ph\u00e9p.");
            }
            case "-505": {
                return new ResponseCode(30, "S\u1ed1 l\u1ea7n check v\u01b0\u1ee3t qu\u00e1 h\u1ea1n m\u1ee9c  cho ph\u00e9p trong ng\u00e0y.");
            }
        }
        return new ResponseCode(30, "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh \u0111\u01b0\u1ee3c k\u1ebft qu\u1ea3 tr\u1ea3 v\u1ec1.");
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

    private static String mappingServiceCodeTopup(String provider, byte type) {
        if ("vtt".equals(provider) && type == 1) {
            return "VTC0056";
        }
        if ("vtt".equals(provider) && type == 2) {
            return "VTC0329";
        }
        if ("vnp".equals(provider) && type == 1) {
            return "VTC0057";
        }
        if ("vnp".equals(provider) && type == 2) {
            return "VTC0201";
        }
        if ("vms".equals(provider) && type == 1) {
            return "VTC0058";
        }
        if ("vms".equals(provider) && type == 2) {
            return "VTC0130";
        }
        if ("vnm".equals(provider)) {
            return "VTC0176";
        }
        if ("gtel".equals(provider)) {
            return "VTC0177";
        }
        return null;
    }

    private static String mappingServiceCodeSoftpin(String provider) {
        switch (provider) {
            case "vtt": {
                return "VTC0027";
            }
            case "vnp": {
                return "VTC0028";
            }
            case "vms": {
                return "VTC0029";
            }
            case "vnm": {
                return "VTC0154";
            }
            case "gate": {
                return "VTC0068";
            }
            case "zing": {
                return "VTC0067";
            }
            case "vcoin": {
                return "VTC0114";
            }
            case "sfone": {
                return "VTC0030";
            }
            case "gtel": {
                return "VTC0173";
            }
            case "garena": {
                return "VTC0319";
            }
        }
        return null;
    }
}

