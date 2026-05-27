/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.vinplay.usercore.dao.GameConfigDao
 *  com.vinplay.usercore.dao.impl.GameConfigDaoImpl
 *  com.vinplay.usercore.entities.OTPSenderResponse
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  org.apache.http.HttpEntity
 *  org.apache.http.HttpResponse
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.entity.StringEntity
 *  org.apache.http.impl.client.DefaultHttpClient
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.otp.sender.impl;

import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vinplay.otp.sender.OTPSenderService;
import com.vinplay.usercore.dao.GameConfigDao;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.utils.GoogleFirebaseConfig;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class FirebaseOTPSender
implements OTPSenderService {
    private static final GameConfigDao dao = new GameConfigDaoImpl();
    private static final Logger logger = Logger.getLogger((String)"otp");
    private GoogleFirebaseConfig googleFirebaseConfig;

    public FirebaseOTPSender() {
        try {
            String gg_firebase = dao.getGameCommon("gg_firebase");
            Type type = new TypeToken<GoogleFirebaseConfig>(){}.getType();
            this.googleFirebaseConfig = (GoogleFirebaseConfig)new Gson().fromJson(gg_firebase, type);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"SMSSender " + e.getMessage()});
        }
    }

    @Override
    public OTPSenderService.Sender name() {
        return OTPSenderService.Sender.SMS;
    }

    @Override
    public OTPSenderResponse send(UserModel userModel, String number, String text) throws Exception {
        OTPSenderResponse res = new OTPSenderResponse(false);
        try {
            if (number.startsWith("0")) {
                StringBuilder sb = new StringBuilder(number);
                sb.deleteCharAt(0);
                number = "+84" + sb.toString();
            }
        }
        catch (Exception e) {
            logger.debug(e);
            res.setMessage(e.getMessage());
        }
        return res;
    }

    public String sendVerifyCode(String nickname, String phoneNumber, String recaptchaToken) {
        DefaultHttpClient client = new DefaultHttpClient();
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            String inputLine;
            HttpPost request = new HttpPost(this.googleFirebaseConfig.urlSendCode + this.googleFirebaseConfig.browerKey);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("phoneNumber", phoneNumber);
            jsonObject.put("recaptchaToken", recaptchaToken);
            StringEntity params = new StringEntity(jsonObject.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity((HttpEntity)params);
            HttpResponse httpResponse = client.execute((HttpUriRequest)request);
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            JSONObject jsonObj = new JSONObject(response.toString());
            return jsonObj.getString("sessionInfo");
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{ex});
            return "";
        }
    }

    public BaseResponse<String> verifyPhoneNumber(String nickname, String sessionInfo, String code) {
        DefaultHttpClient client = new DefaultHttpClient();
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            String inputLine;
            HttpPost request = new HttpPost(this.googleFirebaseConfig.urlVerifyPhone + this.googleFirebaseConfig.serverKey);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sessionInfo", sessionInfo);
            jsonObject.put("code", code);
            StringEntity params = new StringEntity(jsonObject.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity((HttpEntity)params);
            HttpResponse httpResponse = client.execute((HttpUriRequest)request);
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            JSONObject jsonObj = new JSONObject(response.toString());
            String phoneNumber = jsonObj.getString("phoneNumber");
            if (phoneNumber == null || "".equals(phoneNumber)) {
                return new BaseResponse("-1", jsonObj.toString());
            }
            return new BaseResponse("0", phoneNumber);
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{"verifyphone" + ex});
            return new BaseResponse("99", ex.getMessage());
        }
    }

    public class Config {
        public String accountId;
        public String serviceSid;
        public String phone;
        public String authToken;
    }
}

