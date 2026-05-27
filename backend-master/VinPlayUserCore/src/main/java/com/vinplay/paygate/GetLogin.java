/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate;

import com.vinplay.epay.megacard.Base64Utils;
import com.vinplay.paygate.ws.Chargeapi;
import com.vinplay.paygate.ws.Chargeapi_Service;
import com.vinplay.paygate.ws.LoginRespBean;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetLogin {
    private static final String partnerId = "638";
    private static final String userName = "shengclub";
    private static final String _password = "KQ6uoXd6ux3E";

    public static void main(String[] args) {
        try {
            Chargeapi_Service srv = new Chargeapi_Service();
            Chargeapi port = srv.getChargeapiPort();
            String enPass = Base64Utils.base64Encode(_password.getBytes());
            System.out.println("enpass: " + enPass);
            LoginRespBean loginResp = port.login(userName, enPass, partnerId);
            System.out.println("login resp: " + loginResp.toString());
            String enSessionId = loginResp.getSessionid();
            String sessionId = new String(Base64Utils.base64Decode(enSessionId));
            System.out.println("sessionId: " + sessionId);
        }
        catch (Exception ex) {
            Logger.getLogger(GetLogin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

