/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate;

import com.vinplay.epay.MD5;
import com.vinplay.epay.megacard.Base64Utils;
import com.vinplay.paygate.ChargeRespPaygate;
import com.vinplay.paygate.ws.ChargeRespBean;
import com.vinplay.paygate.ws.Chargeapi;
import com.vinplay.paygate.ws.Chargeapi_Service;
import com.vinplay.paygate.ws.LoginRespBean;

public class PaygateClient {
    private static final String _userName = "shengclub";
    private static final String _password = "KQ6uoXd6ux3E";
    private static final String _partnerId = "638";
    private static final String _MPIN = "883";

    public static String login() {
        Chargeapi_Service srv = new Chargeapi_Service();
        Chargeapi port = srv.getChargeapiPort();
        String enPass = Base64Utils.base64Encode(_password.getBytes());
        LoginRespBean loginResp = port.login(_userName, enPass, _partnerId);
        String enSessionId = loginResp.getSessionid();
        String sessionId = new String(Base64Utils.base64Decode(enSessionId));
        return sessionId;
    }

    public static ChargeRespPaygate rechargeCard(String transId, String cSerial, String cPin, String telcoCode) {
        try {
            ChargeRespPaygate response = new ChargeRespPaygate();
            String _loginSession = PaygateClient.login();
            Chargeapi_Service srv = new Chargeapi_Service();
            Chargeapi port = srv.getChargeapiPort();
            String card_data = cSerial + ":" + cPin + ":" + PaygateClient.convertTelcoCode(telcoCode);
            String m5Session = MD5.hash(_loginSession);
            String enCard_data = Base64Utils.base64Encode(card_data.getBytes());
            String enMpin = Base64Utils.base64Encode(_MPIN.getBytes());
            ChargeRespBean chargeResp = port.charge(transId, _userName, _partnerId, enMpin, enCard_data, m5Session);
            if (chargeResp.getStatus() == 1) {
                response.setAmount(new String(Base64Utils.base64Decode(chargeResp.getDRemainAmount())));
            }
            response.setMessage(chargeResp.getMessage());
            response.setSerial(chargeResp.getSSerialNumber());
            response.setStatus(String.valueOf(chargeResp.getStatus()));
            response.setTransId(String.valueOf(chargeResp.getTransid()));
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertTelcoCode(String provider) {
        if (provider == null) {
            return "";
        }
        switch (provider) {
            case "Viettel": {
                return "VTT";
            }
            case "Mobifone": {
                return "VMS";
            }
            case "Vinaphone": {
                return "VNP";
            }
        }
        return "";
    }
}

