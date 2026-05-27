/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.vinplay.usercore.dao.GameConfigDao
 *  com.vinplay.usercore.dao.impl.GameConfigDaoImpl
 *  com.vinplay.usercore.entities.OTPSenderResponse
 *  com.vinplay.vbee.common.models.UserModel
 *  org.apache.http.HttpEntity
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.entity.StringEntity
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClients
 *  org.apache.log4j.Logger
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.otp.sender.impl;

import bitzero.util.common.business.Debug;
import com.vinplay.otp.sender.OTPSenderService;
import com.vinplay.otp.sender.impl.Utils;
import com.vinplay.usercore.dao.GameConfigDao;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.vbee.common.models.UserModel;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSInfobipSender
implements OTPSenderService {
    private static final GameConfigDao dao = new GameConfigDaoImpl();
    private static final Logger logger = Logger.getLogger((String)"otp");
    private Config config = new Config();

    public SMSInfobipSender() {
        try {
            String configStr = dao.getGameCommon("infobip_sms");
            JSONObject jsonObject = new JSONObject(configStr);
            this.config.baseUrl = jsonObject.getString("baseUrl");
            this.config.token = jsonObject.getString("token");
            this.config.sender = jsonObject.getString("sender");
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"SMSInfobipSender " + e.getMessage()});
        }
    }

    @Override
    public OTPSenderService.Sender name() {
        return OTPSenderService.Sender.SMS;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public OTPSenderResponse send(UserModel userModel, String number, String text) throws Exception {
        OTPSenderResponse res = new OTPSenderResponse(false);
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://" + this.config.baseUrl + "/sms/3/messages");
            if (number.startsWith("0")) {
                StringBuilder sb = new StringBuilder(number);
                sb.deleteCharAt(0);
                number = "+84" + sb.toString();
            }
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Authorization", "App " + this.config.token);
            String json = this.buildJson(userModel, number, text);
            StringEntity params = new StringEntity(json);
            httpPost.setEntity((HttpEntity)params);
            try (CloseableHttpResponse response = httpclient.execute((HttpUriRequest)httpPost);){
                if (response.getStatusLine().getStatusCode() == 201 || response.getStatusLine().getStatusCode() == 200) {
                    res.setSuccess(true);
                } else {
                    Debug.info((Object[])new Object[]{"Send sms to " + number + " => " + response});
                }
                String msg = Utils.getResponse(response);
                Debug.trace((Object[])new Object[]{"JSON sms -> " + msg});
            }
        }
        catch (Exception e) {
            logger.debug(e);
            e.printStackTrace();
            res.setMessage(e.getMessage());
        }
        return res;
    }

    private String buildJson(UserModel userModel, String number, String text) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject message = new JSONObject();
            message.put("sender", this.config.sender);
            message.put("destinations", new JSONArray().put(new JSONObject().put("to", number)));
            message.put("content", new JSONObject().put("text", text));
            jsonObject.put("messages", new JSONArray().put(message));
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject.toString();
    }

    public class Config {
        public String baseUrl;
        public String token;
        public String sender;
    }
}

