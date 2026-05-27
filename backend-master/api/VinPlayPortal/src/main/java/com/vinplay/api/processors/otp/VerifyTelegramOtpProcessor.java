/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.shotfish.entites.TeleBotConfig
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  okhttp3.MultipartBody
 *  okhttp3.MultipartBody$Builder
 *  okhttp3.OkHttpClient
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.RequestBody
 *  okhttp3.Response
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.otp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.common.AuthProcessor;
import com.vinplay.shotfish.entites.TeleBotConfig;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class VerifyTelegramOtpProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String otp = request.getParameter("otp");
        String phoneNumber = request.getParameter("m");
        if (StringUtils.isBlank((CharSequence)phoneNumber)) {
            return BaseResponse.error((String)"5", (String)"S\u1ed1 \u0111i\u1ec7n tho\u1ea1i kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng");
        }
        if (StringUtils.isBlank((CharSequence)otp)) {
            return BaseResponse.error((String)"5", (String)"M\u00e3 OTP kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng");
        }
        UserModel userModel = this.getUser(param);
        if (userModel == null) {
            return notAuth;
        }
        String nickName = userModel.getNickname();
        phoneNumber = phoneNumber.replace(" ", "");
        if ((phoneNumber = phoneNumber.replace("+", "")).startsWith("84")) {
            phoneNumber = "0" + phoneNumber.substring(2);
        }
        try {
            com.vinplay.vbee.common.cache.DistCache<String, String> otpCache =
                    com.vinplay.vbee.common.cache.CacheFactory.get("telegramOtpCache", String.class);
            String cacheKey = "telegram_otp_" + phoneNumber;
            String foundOtpData = otpCache.get(cacheKey);
            if (foundOtpData == null) {
                return BaseResponse.error((String)"99", (String)"M\u00e3 OTP kh\u00f4ng t\u1ed3n t\u1ea1i ho\u1eb7c \u0111\u00e3 h\u1ebft h\u1ea1n. Vui l\u00f2ng y\u00eau c\u1ea7u m\u00e3 OTP m\u1edbi.");
            }
            String[] parts = foundOtpData.split("\\|");
            if (parts.length != 3) {
                return BaseResponse.error((String)"99", (String)"D\u1eef li\u1ec7u OTP kh\u00f4ng h\u1ee3p l\u1ec7.");
            }
            String cachedTelegramId = parts[0];
            String cachedPhoneNumber = parts[1];
            String cachedOtp = parts[2];
            if (!cachedPhoneNumber.equals(phoneNumber)) {
                return BaseResponse.error((String)"5", (String)"S\u1ed1 \u0111i\u1ec7n tho\u1ea1i kh\u00f4ng kh\u1edbp.");
            }
            if (!cachedOtp.equals(otp)) {
                return BaseResponse.error((String)"5", (String)"M\u00e3 OTP kh\u00f4ng \u0111\u00fang.");
            }
            UserDaoImpl userDao = new UserDaoImpl();
            boolean updated = userDao.updateTeleIdAndPhoneByNickName(cachedTelegramId, phoneNumber, nickName);
            if (!updated) {
                return BaseResponse.error((String)"99", (String)"C\u00f3 l\u1ed7i x\u1ea3y ra khi c\u1eadp nh\u1eadt th\u00f4ng tin. Vui l\u00f2ng th\u1eed l\u1ea1i!");
            }
            otpCache.remove(cacheKey);
            this.updateCached(nickName, phoneNumber, cachedTelegramId);
            try {
                VerifyTelegramOtpProcessor.postRequest("https://api.telegram.org/bot" + GameCommon.telegramConfig.getLogin().getBootToken() + "/sendMessage", cachedTelegramId, "B\u1ea1n \u0111\u00e3 x\u00e1c th\u1ef1c th\u00e0nh c\u00f4ng s\u1ed1 \u0111i\u1ec7n tho\u1ea1i " + phoneNumber + " cho " + nickName + ".");
            }
            catch (Exception e) {
                logger.error(("Error sending Telegram message: " + e.getMessage()));
            }
            return BaseResponse.success((String)"0", (String)"X\u00e1c th\u1ef1c s\u1ed1 \u0111i\u1ec7n tho\u1ea1i th\u00e0nh c\u00f4ng!", null);
        }
        catch (Exception e) {
            logger.error(("VerifyTelegramOtpProcessor error: " + e.getMessage()), (Throwable)e);
            return BaseResponse.error((String)"99", (String)"C\u00f3 l\u1ed7i x\u1ea3y ra. Vui l\u00f2ng th\u1eed l\u1ea1i!");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateCached(String nickName, String mobile, String telegramId) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("users");
        if (userMap.containsKey(nickName)) {
            try {
                userMap.lock(nickName);
                UserCacheModel user = (UserCacheModel)userMap.get(nickName);
                user.setMobile(mobile);
                user.setTeleId(telegramId);
                user.setVerifyMobile(true);
                userMap.put(nickName, user);
            }
            finally {
                userMap.unlock(nickName);
            }
        }
    }

    private String getTelegramBotToken() {
        try {
            TypeToken<TeleBotConfig> type;
            Gson gson;
            TeleBotConfig teleBotConfig;
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap configCache = instance.getMap("cacheConfig");
            String value = (String)configCache.get("TELEGRAMBOTCONFIGCACHE");
            if (value != null && (teleBotConfig = (TeleBotConfig)(gson = new Gson()).fromJson(value, (type = new TypeToken<TeleBotConfig>(){}).getType())) != null) {
                return teleBotConfig.secretKey;
            }
        }
        catch (Exception e) {
            logger.error(("Error getting Telegram bot token: " + e.getMessage()));
        }
        return "";
    }

    public static int postRequest(String url, String chatId, String content) throws IOException {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("chat_id", chatId).addFormDataPart("text", content).build();
            Request request = new Request.Builder().url(url).method("POST", (RequestBody)body).build();
            Response response = client.newCall(request).execute();
            return response.code();
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(("postRequest | API: " + url));
            return 0;
        }
    }
}

