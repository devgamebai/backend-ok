/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.json.simple.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.chargeCore;

import com.vinplay.dichvuthe.client.HttpClient;
import com.vinplay.dichvuthe.encode.AES;
import com.vinplay.dichvuthe.entities.ChargeObj;
import com.vinplay.usercore.utils.GameCommon;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class VinplayChargeCore {
    public static ChargeObj rechargeByCard(String reqId, String provider, String serial, String pin) throws Exception {
        String res = HttpClient.get(GameCommon.getValueStr("CHARGE_CORE_URL") + "/api/charge?uId=" + GameCommon.getValueStr("CHARGE_CORE_UID") + "&reqId=" + reqId + "&provider=" + provider + "&serial=" + serial + "&pin=" + AES.encrypt(pin, GameCommon.getValueStr("CHARGE_CORE_KEY")));
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        ChargeObj result = VinplayChargeCore.convertToChargeObj(json);
        return result;
    }

    public static ChargeObj checkTrans(String reqId) throws Exception {
        String res = HttpClient.get(GameCommon.getValueStr("CHARGE_CORE_URL") + "/api/check-trans?uId=" + GameCommon.getValueStr("CHARGE_CORE_UID") + "&reqId=" + reqId);
        JSONObject json = (JSONObject)new JSONParser().parse(res);
        ChargeObj result = VinplayChargeCore.convertToChargeObj(json);
        return result;
    }

    private static ChargeObj convertToChargeObj(JSONObject jData) throws Exception {
        ChargeObj response = new ChargeObj();
        if (jData.get("reqId") != null) {
            response.setId(String.valueOf(jData.get("reqId")));
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
            response.setChannel(VinplayChargeCore.mapPartner(String.valueOf(jData.get("channel"))));
        }
        if (jData.get("uId") != null) {
            response.setUserId(String.valueOf(jData.get("uId")));
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
        if (channel.equalsIgnoreCase("abtpay")) {
            return "abtpay";
        }
        return channel;
    }
}

