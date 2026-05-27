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
 *  org.apache.http.client.entity.UrlEncodedFormEntity
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClients
 *  org.apache.http.message.BasicNameValuePair
 *  org.apache.http.util.EntityUtils
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.otp.sender.impl;

import bitzero.util.common.business.Debug;
import com.vinplay.otp.sender.OTPSenderService;
import com.vinplay.usercore.dao.GameConfigDao;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.vbee.common.models.UserModel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class SMSTwilioSender
implements OTPSenderService {
    private static final GameConfigDao dao = new GameConfigDaoImpl();
    private static final Logger logger = Logger.getLogger((String)"otp");
    private Config config = new Config();

    public SMSTwilioSender() {
        try {
            String configStr = dao.getGameCommon("twilio_sms");
            JSONObject jsonObject = new JSONObject(configStr);
            this.config.accountId = jsonObject.getString("accountId");
            this.config.phone = jsonObject.getString("phone");
            this.config.authToken = jsonObject.getString("authToken");
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"SMSSender " + e.getMessage()});
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
            HttpPost httpPost = new HttpPost("https://api.twilio.com/2010-04-01/Accounts/" + this.config.accountId + "/Messages.json");
            ArrayList<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
            if (number.startsWith("0")) {
                StringBuilder sb = new StringBuilder(number);
                sb.deleteCharAt(0);
                number = "+84" + sb.toString();
            }
            nvps.add(new BasicNameValuePair("To", number));
            nvps.add(new BasicNameValuePair("From", this.config.phone));
            nvps.add(new BasicNameValuePair("Body", text));
            httpPost.setEntity((HttpEntity)new UrlEncodedFormEntity(nvps));
            String auth = this.config.accountId + ":" + this.config.authToken;
            String base64Creds = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            httpPost.setHeader("Authorization", "Basic " + base64Creds);
            try (CloseableHttpResponse response = httpclient.execute((HttpUriRequest)httpPost);){
                if (response.getStatusLine().getStatusCode() == 201) {
                    res.setSuccess(true);
                } else {
                    Debug.info((Object[])new Object[]{"Send sms to " + number + " => " + response});
                }
                HttpEntity entity2 = response.getEntity();
                EntityUtils.consume((HttpEntity)entity2);
            }
        }
        catch (Exception e) {
            logger.debug(e);
            res.setMessage(e.getMessage());
        }
        return res;
    }

    public class Config {
        public String accountId;
        public String serviceSid;
        public String phone;
        public String authToken;
    }
}

