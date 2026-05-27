/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.vbee.common.models.SoftpinJson
 *  org.json.simple.JSONArray
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.dichvuthe.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dichvuthe.client.HttpClient;
import com.vinplay.dichvuthe.entities.*;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.models.SoftpinJson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class VinplayClient {
    public static ChargeObj rechargeByCard(String id, String provider, String serial, String pin) throws Exception {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("id", id);
        param.put("provider", provider);
        param.put("serial", serial);
        param.put("pin", pin);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/charge", mapper.writeValueAsString(param));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        ChargeObj result = VinplayClient.convertToChargeObj(json);
        return result;
    }

    public static ChargeVCObj rechargeByVinCard(String id, String partner, String serial, String pin, String target) throws Exception {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("id", id);
        param.put("partner", partner);
        if (serial != null && !serial.isEmpty()) {
            param.put("serial", serial);
        }
        param.put("pin", pin);
        if (target != null && !target.isEmpty()) {
            param.put("target", target);
        }
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("VIN_CARD_URL") + "/api/charge", mapper.writeValueAsString(param));
        ChargeVCObj result = (ChargeVCObj)mapper.readValue(res, ChargeVCObj.class);
        return result;
    }

    public static ChargeObj reCheckRechargeByCard(String id) throws Exception {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("id", id);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/charge-check", mapper.writeValueAsString(param));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        ChargeObj result = VinplayClient.convertToChargeObj(json);
        return result;
    }

    public static BankrcObj rechargeByBank() throws IOException {
        return null;
    }

    public static SoftpinObj cashOutByCard(String id, String provider, int amount, int quantity, String sign) throws Exception {
        SendSoftpinObj param = new SendSoftpinObj(id, provider, amount, quantity, sign);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/buy-card", mapper.writeValueAsString(param));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        SoftpinObj result = VinplayClient.convertToSoftpinObj(json);
        return result;
    }

    public static SoftpinObj reCheckCashOutByCard(String id, String sign) throws Exception {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("id", String.valueOf(id));
        param.put("sign", sign);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/re-buy-card", mapper.writeValueAsString(param));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        SoftpinObj result = VinplayClient.convertToSoftpinObj(json);
        return result;
    }

    public static TopupObj cashOutByTopUp(String id, String target, int type, int amount, String sign) throws Exception {
        SendTopupObj param = new SendTopupObj(id, target, type, amount, sign);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/topup", mapper.writeValueAsString(param));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        TopupObj result = VinplayClient.convertToTopupObj(json);
        return result;
    }

    public static BankcoObj cashOutByBank(String id, String bank, String account, String name, int amount, String sign) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SendBankObj param = new SendBankObj(id, bank, account, name, amount, sign);
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/payment", mapper.writeValueAsString(param));
        BankcoObj result = (BankcoObj)mapper.readValue(res, BankcoObj.class);
        return result;
    }

    public static BankcoObj reCheckCashOutByBank(String id, String sign) throws Exception {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("id", String.valueOf(id));
        param.put("sign", sign);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/check-payment", mapper.writeValueAsString(param));
        BankcoObj result = (BankcoObj)mapper.readValue(res, BankcoObj.class);
        return result;
    }

    public static String sendAleftSMS(List<String> receives, String content, boolean call) throws Exception {
        AlertObj obj = new AlertObj(receives, content, call);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/alert", mapper.writeValueAsString(obj));
        return res;
    }

    public static String aleft(List<String> receives, String content, boolean call) throws Exception {
        AlertObj obj = new AlertObj(receives, content, call);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("ALERT_URL"), mapper.writeValueAsString(obj));
        return res;
    }

    public static String sendEmail(String subject, String content, List<String> receives) throws Exception {
        EmailObj obj = new EmailObj(subject, content, receives);
        ObjectMapper mapper = new ObjectMapper();
        String res = HttpClient.post(GameCommon.getValueStr("DVT_URL") + "/api/email", mapper.writeValueAsString(obj));
        return res;
    }

    public static String sendEmailApi(String toEmail, String subject, String template, String params) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EmailApiObj emailApiObj = new EmailApiObj();
        emailApiObj.setToEmail(toEmail);
        emailApiObj.setTemplate(template);
        emailApiObj.setSubject(subject);
        emailApiObj.setParams(params);
        emailApiObj.setFromEmail(GameCommon.getValueStr("EMAIL_API_FROM_EMAIL"));
        emailApiObj.setFromName(GameCommon.getValueStr("EMAIL_API_FROM_NAME"));
        emailApiObj.setDomain(GameCommon.getValueStr("EMAIL_API_DOMAIN"));
        emailApiObj.setSiteName(GameCommon.getValueStr("EMAIL_API_SITE_NAME"));
        emailApiObj.setModule(GameCommon.getValueStr("EMAIL_API_MODULE"));
        String res = HttpClient.sendPostEmail(GameCommon.getValueStr("EMAIL_API_API_KEY"),
                GameCommon.getValueStr("EMAIL_API_URL"), mapper.writeValueAsString(emailApiObj));
        return res;
    }

    private static TopupObj convertToTopupObj(JSONObject jData) throws Exception {
        TopupObj response = new TopupObj();
        if (jData.get("id") != null) {
            response.setId(String.valueOf(jData.get("id")));
        }
        if (jData.get("target") != null) {
            response.setTarget(String.valueOf(jData.get("target")));
        }
        if (jData.get("type") != null) {
            response.setType(Integer.parseInt(String.valueOf(jData.get("type"))));
        }
        if (jData.get("amount") != null) {
            response.setAmount(Integer.parseInt(String.valueOf(jData.get("amount"))));
        }
        if (jData.get("status") != null) {
            response.setStatus(Integer.parseInt(String.valueOf(jData.get("status"))));
        }
        if (jData.get("message") != null) {
            response.setMessage(String.valueOf(jData.get("message")));
        }
        if (jData.get("channel") != null) {
            response.setPartner(VinplayClient.mapPartner(String.valueOf(jData.get("channel"))));
        }
        if (jData.get("sign") != null) {
            response.setSign(String.valueOf(jData.get("sign")));
        }
        return response;
    }

    private static ChargeObj convertToChargeObj(JSONObject jData) throws Exception {
        ChargeObj response = new ChargeObj();
        if (jData.get("id") != null) {
            response.setId(String.valueOf(jData.get("id")));
        }
        if (jData.get("provider") != null) {
            response.setProvider(String.valueOf(jData.get("provider")));
        }
        if (jData.get("serial") != null) {
            response.setSerial(String.valueOf(jData.get("serial")));
        }
        if (jData.get("pin") != null) {
            response.setPin(String.valueOf(jData.get("pin")));
        }
        if (jData.get("amount") != null) {
            response.setAmount(Integer.parseInt(String.valueOf(jData.get("amount"))));
        }
        if (jData.get("status") != null) {
            response.setStatus(Integer.parseInt(String.valueOf(jData.get("status"))));
        }
        if (jData.get("message") != null) {
            response.setMessage(String.valueOf(jData.get("message")));
        }
        if (jData.get("channel") != null) {
            response.setChannel(VinplayClient.mapPartner(String.valueOf(jData.get("channel"))));
        }
        return response;
    }

    private static SoftpinObj convertToSoftpinObj(JSONObject jData) throws Exception {
        SoftpinObj response = new SoftpinObj();
        if (jData.get("id") != null) {
            response.setId(String.valueOf(jData.get("id")));
            response.setPartnerTransId(String.valueOf(jData.get("id")));
        }
        if (jData.get("provider") != null) {
            response.setProvider(String.valueOf(jData.get("provider")));
        }
        if (jData.get("amount") != null) {
            response.setAmount(Integer.parseInt(String.valueOf(jData.get("amount"))));
        }
        if (jData.get("quantity") != null) {
            response.setQuantity(Integer.parseInt(String.valueOf(jData.get("quantity"))));
        }
        if (jData.get("status") != null) {
            response.setStatus(Integer.parseInt(String.valueOf(jData.get("status"))));
        }
        if (jData.get("message") != null) {
            response.setMessage(String.valueOf(jData.get("message")));
        }
        if (jData.get("softpinList") != null) {
            JSONArray arr = (JSONArray)jData.get("softpinList");
            ArrayList<SoftpinJson> softpinList = new ArrayList<SoftpinJson>();
            for (int i = 0; i < arr.size(); ++i) {
                JSONObject obj = (JSONObject)arr.get(i);
                SoftpinJson softpin = new SoftpinJson();
                softpin.setProvider(String.valueOf(obj.get("provider")));
                softpin.setAmount(Integer.parseInt(String.valueOf(obj.get("amount"))));
                softpin.setSerial(String.valueOf(obj.get("serial")));
                softpin.setPin(String.valueOf(obj.get("pin")));
                softpin.setExpire(String.valueOf(obj.get("expire")));
                softpinList.add(softpin);
            }
            response.setSoftpinList(softpinList);
        }
        if (jData.get("channel") != null) {
            response.setPartner(VinplayClient.mapPartner(String.valueOf(jData.get("channel"))));
        }
        if (jData.get("sign") != null) {
            response.setSign(String.valueOf(jData.get("sign")));
        }
        return response;
    }

    private static String mapPartner(String channel) {
        if (channel.equalsIgnoreCase("store")) {
            return "dvt";
        }
        if (channel.equalsIgnoreCase("epay")) {
            return "epay";
        }
        if (channel.equalsIgnoreCase("vtc")) {
            return "vtc";
        }
        if (channel.equalsIgnoreCase("1pay")) {
            return "1pay";
        }
        if (channel.equalsIgnoreCase("maxpay")) {
            return "maxpay";
        }
        return channel;
    }
}

