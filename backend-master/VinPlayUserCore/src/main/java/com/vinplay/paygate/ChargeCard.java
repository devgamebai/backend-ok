/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate;

import com.vinplay.epay.MD5;
import com.vinplay.epay.megacard.Base64Utils;
import com.vinplay.paygate.ws.ChargeRespBean;
import com.vinplay.paygate.ws.Chargeapi;
import com.vinplay.paygate.ws.Chargeapi_Service;
import java.io.PrintStream;

public class ChargeCard {
    private static final String _userName = "shengclub";
    private static final String _partnerId = "638";
    private static final String _MPIN = "883";

    public static void main(String[] args) {
        try {
            String _loginSession = "15A2A2F189BD951CF1BF96C616BE81082015C2856EED72D3";
            Chargeapi_Service srv = new Chargeapi_Service();
            Chargeapi port = srv.getChargeapiPort();
            String strTransID = String.valueOf(System.currentTimeMillis());
            String cSerial = "58120715703";
            String cPin = "6566763116716";
            String telcoCode = "VTT";
            String card_data = "58120715703:6566763116716:VTT";
            String m5Session = MD5.hash("15A2A2F189BD951CF1BF96C616BE81082015C2856EED72D3");
            System.out.println("MD5 SessionId: " + m5Session);
            String enCard_data = Base64Utils.base64Encode("58120715703:6566763116716:VTT".getBytes());
            System.out.println("enCard_data : " + enCard_data);
            String enMpin = Base64Utils.base64Encode(_MPIN.getBytes());
            System.out.println("enMpin : " + enMpin);
            ChargeRespBean chargeResp = port.charge(strTransID, _userName, _partnerId, enMpin, enCard_data, m5Session);
            System.out.println("chargeResp: " + chargeResp.toString());
            String enAmount = chargeResp.getDRemainAmount();
            String sAmount = new String(Base64Utils.base64Decode(enAmount));
            System.out.println("amount: " + sAmount);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

