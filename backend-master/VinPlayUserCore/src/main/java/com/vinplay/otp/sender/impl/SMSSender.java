/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
 *  org.json.JSONObject
 */
package com.vinplay.otp.sender.impl;

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
import org.json.JSONObject;

public class SMSSender
implements OTPSenderService {
    private static final GameConfigDao dao = new GameConfigDaoImpl();
    private static final Logger logger = Logger.getLogger((String)"otp");
    private Config config = new Config();

    public SMSSender() {
        try {
            String configStr = dao.getGameCommon("sh_sms");
            JSONObject jsonObject = new JSONObject(configStr);
            this.config.token = jsonObject.getString("token");
            this.config.url = jsonObject.getString("url");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public OTPSenderService.Sender name() {
        return OTPSenderService.Sender.SMS;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public OTPSenderResponse send(UserModel userModel, String number, String text) throws Exception {
        OTPSenderResponse res = new OTPSenderResponse(false);
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            String url = this.config.url + "?token=" + this.config.token;
            HttpPost request = new HttpPost(url);
            if (number.startsWith("0")) {
                StringBuilder sb = new StringBuilder(number);
                sb.deleteCharAt(0);
                number = "+84" + sb.toString();
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("phone", number);
            jsonObject.put("msg", text);
            StringEntity params = new StringEntity(jsonObject.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity((HttpEntity)params);
            try (CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);){
                if (response.getStatusLine().getStatusCode() == 201 || response.getStatusLine().getStatusCode() == 200) {
                    String msg = Utils.getResponse(response);
                    JSONObject jsonObject1 = new JSONObject(msg);
                    if (jsonObject1.getBoolean("success")) {
                        res.setSuccess(true);
                    } else {
                        res.setMessage(jsonObject1.getString("msg"));
                    }
                    OTPSenderResponse oTPSenderResponse = res;
                    return oTPSenderResponse;
                }
                logger.trace(("Send sms to " + number + " => " + response));
                String msg = Utils.getResponse(response);
                logger.trace(("JSON sms -> " + msg));
                return res;
            }
        }
        catch (Exception e2) {
            logger.debug(e2);
            res.setMessage(e2.getMessage());
            return res;
        }
    }

    public class Config {
        public String token;
        public String url;
    }
}

