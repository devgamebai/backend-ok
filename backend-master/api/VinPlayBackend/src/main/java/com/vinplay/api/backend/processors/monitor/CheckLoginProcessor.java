/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.api.backend.processors.monitor;

import com.vinplay.api.backend.report.utils.Config;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class CheckLoginProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        AlertServiceImpl alertService = new AlertServiceImpl();
        try {
            String URI_PORTAL = "http://api.vinplay.com:8081/api";
            String rawPassword = "tuyennguyen";
            String md5Password = "";
            try {
                md5Password = VinPlayUtils.getMD5Hash((String)"tuyennguyen");
            }
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            URL url = new URL(String.valueOf("http://api.vinplay.com:8081/api") + "?c=3&un=tuyennd&pw=" + md5Password + "&s=&at=");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                if (++Config.numErrorLogin == 2) {
                    alertService.sendSMS2One("0984574749", "Login Vinplay error: response code= " + conn.getResponseCode() + ", time= " + DateTimeUtils.getCurrentTime(), false);
                }
                return "error 200";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output = br.readLine();
            try {
                JSONObject json = new JSONObject(output);
                String errorCode = json.getString("errorCode");
                if (!errorCode.equals("0")) {
                    if (++Config.numErrorLogin == 2) {
                        alertService.sendSMS2One("0984574749", "Login Vinplay error: code= " + errorCode + ", time= " + DateTimeUtils.getCurrentTime(), false);
                    }
                    return "error code " + errorCode;
                }
                Config.numErrorLogin = 0;
            }
            catch (JSONException e2) {
                e2.printStackTrace();
            }
            conn.disconnect();
        }
        catch (Exception e3) {
            e3.printStackTrace();
            alertService.sendSMS2One("0984574749", "Login Vinplay error:  " + e3.getMessage() + ", time= " + DateTimeUtils.getCurrentTime(), false);
        }
        return "ok";
    }
}

