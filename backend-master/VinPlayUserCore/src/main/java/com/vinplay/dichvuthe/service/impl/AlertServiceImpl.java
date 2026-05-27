/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.BrandnameMessage
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.dichvuthe.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.vinplay.brandname.enties.ListPhone;
import com.vinplay.brandname.service.APISMSLocator;
import com.vinplay.brandname.service.IAPISMS;
import com.vinplay.dichvuthe.client.VinplayClient;
import com.vinplay.dichvuthe.service.AlertService;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.BrandnameMessage;

import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;


public class AlertServiceImpl implements AlertService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    @Override
    public boolean sendSMS2List(List<String> receives, String content, boolean call) {
        return this.sendSMS(receives, content, call);
    }

    @Override
    public boolean SendSMSEsms(String phone, String message) {
        try {
            String messageText = URLEncoder.encode(message, "UTF-8");
            String urlStr = String.format("http://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_get?Phone=%s&Content=%s&ApiKey=%s&SecretKey=%s&IsUnicode=%s&SmsType=%s&brandname=%s",
                    phone, messageText,
                    GameCommon.getValueStr("ESMS_API_KEY"),
                    GameCommon.getValueStr("ESMS_SECRET_KEY"),
                    GameCommon.getValueStr("ESMS_IS_UNICODE"),
                    GameCommon.getValueStr("ESMS_SMS_TYPE"),
                    GameCommon.getValueStr("ESMS_BRAND_NAME"));
            URL url = new URL(urlStr);
            HttpURLConnection request = (HttpURLConnection)url.openConnection();
            request.setConnectTimeout(90000);
            request.setUseCaches(false);
            request.setDoOutput(true);
            request.setDoInput(true);
            HttpURLConnection.setFollowRedirects(true);
            request.setInstanceFollowRedirects(true);
            request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            request.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String result = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                result = result.concat(line);
            }
            JSONObject json = (JSONObject)new JSONParser().parse(result);
            if(json != null){
                String testData = (String) json.get("CodeResult");
            }

            return json != null && json.get("CodeResult") != null && String.valueOf(json.get("CodeResult")).equals("100");
        }
        catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean sendSMS2One(String mobile, String content, boolean call) {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private boolean sendSMS(List<String> receives, String content, boolean call) {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap map = instance.getMap("cacheConfig");
            if (!map.containsKey("BRAND_NAME_ID")) return false;
            try {
                map.lock("BRAND_NAME_ID");
                long brandNameID = Long.parseLong((String)map.get("BRAND_NAME_ID"));
                APISMSLocator localtor = new APISMSLocator();
                IAPISMS service = localtor.getBasicHttpBinding_IAPISMS();
                if (content == null) return false;
                if (receives == null) return false;
                if (receives.size() < 1) return false;
                if (GameCommon.getValueInt("BRANDNAME_OPEN") == 1) {
                    String code = "-1";
                    ArrayList<ListPhone> listPhone = new ArrayList<ListPhone>();
                    try {
                        if (receives.size() == 1) {
                            map.put("BRAND_NAME_ID", String.valueOf(++brandNameID));
                            code = service.pushMsg2PhoneDLVR(GameCommon.BRANDNAME_SENDER, content, receives.get(0), String.valueOf(brandNameID), GameCommon.BRANDNAME_CLIENT_ID, GameCommon.BRANDNAME_USER, GameCommon.BRANDNAME_PASS);
                        } else {
                            for (String mobile : receives) {
                                ListPhone lp = new ListPhone(this.revertMobile(mobile), String.valueOf(++brandNameID));
                                listPhone.add(lp);
                            }
                            map.put("BRAND_NAME_ID", String.valueOf(brandNameID));
                            ListPhone[] arrPhone = listPhone.toArray(new ListPhone[listPhone.size()]);
                            code = service.pushMsg2ListPhoneDLVR(GameCommon.BRANDNAME_SENDER, content, arrPhone, GameCommon.BRANDNAME_CLIENT_ID, GameCommon.BRANDNAME_USER, GameCommon.BRANDNAME_PASS);
                        }
                    }
                    catch (Exception e) {
                        logger.debug(e);
                    }
                    if (listPhone.size() >= 1) {
                        for (ListPhone lp2 : listPhone) {
                            BrandnameMessage message = new BrandnameMessage(lp2.getRequestId(), GameCommon.BRANDNAME_SENDER, content, lp2.getPhone(), code);
                            // P1 cleanup: queue_otp retired (no consumer); SaveLogBrandnameProcessor deleted.
                        }
                    } else {
                        BrandnameMessage message2 = new BrandnameMessage(String.valueOf(brandNameID), GameCommon.BRANDNAME_SENDER, content, receives.get(0), code);
                        // P1 cleanup: queue_otp retired (no consumer); SaveLogBrandnameProcessor deleted.
                    }
                    if (!code.equals("1")) return false;
                    boolean message2 = true;
                    return message2;
                }
                if (GameCommon.getValueInt("DVT_SMS_OPEN") != 1) return false;
                VinplayClient.sendAleftSMS(receives, content, call);
                boolean code = true;
                return code;
            }
            catch (Exception e2) {
                logger.debug(e2);
                return false;
            }
            finally {
                map.unlock("BRAND_NAME_ID");
            }
        }
        catch (Exception e3) {
            logger.debug(e3);
        }
        return false;
    }

    @Override
    public boolean alert2List(List<String> receives, String content, boolean call) {
        return this.alert(receives, content, call);
    }

    @Override
    public boolean alert2One(String mobile, String content, boolean call) {
        ArrayList<String> receives = new ArrayList<String>();
        receives.add(mobile);
        return this.alert(receives, content, call);
    }

    private boolean alert(List<String> receives, String content, boolean call) {
        try {
            VinplayClient.aleft(receives, content, call);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            return false;
        }
    }

    private String revertMobile(String mobile) {
        if (mobile.substring(0, 1).equals("0")) {
            return "84" + mobile.substring(1);
        }
        return mobile;
    }

    private String revertMobileBegin84(String mobile) {
        if (mobile.substring(0, 1).equals("0")) {
            return "84" + mobile.substring(1);
        }
        return mobile;
    }

    private String revertMobileBegin0(String mobile) {
        if (mobile.substring(0, 2).equals("84")) {
            return "0" + mobile.substring(2);
        }
        return mobile;
    }

    @Override
    public boolean sendEmail(String subject, String template, String params, List<String> receives) {
        try {
            for (String receive: receives) {
                VinplayClient.sendEmailApi(receive, subject, template, params);
                return true;
            }
        }
        catch (Exception e) {
            logger.debug(e);
        }
        return true;
    }

    @Override
    public boolean sendSMS2User(String username, String content) {
        String mobile;
        UserCacheModel model;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(username) && (model = (UserCacheModel)userMap.get(username)) != null && model.isHasMobileSecurity() && (mobile = model.getMobile()) != null && !mobile.isEmpty()) {
            try {
                return this.sendSMS2One(mobile, content, false);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }
}

