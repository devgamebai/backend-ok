/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  okhttp3.MultipartBody
 *  okhttp3.MultipartBody$Builder
 *  okhttp3.OkHttpClient
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.RequestBody
 *  okhttp3.Response
 */
package com.vinplay.usercore.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.shotfish.entites.TeleBotConfig;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelegramUtils {
    public static int postRequest(String chatId, String content) throws IOException {
        try {
            TeleBotConfig botConfig = TelegramUtils.getConfig();
            String url = "https://api.telegram.org/bot" + botConfig.secretKey + "/sendMessage";
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("chat_id", chatId).addFormDataPart("text", content).build();
            Request request = new Request.Builder().url(url).method("POST", (RequestBody)body).build();
            Response response = client.newCall(request).execute();
            return response.code();
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static TeleBotConfig getConfig() {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap configCache = instance.getMap("cacheConfig");
            String value = ((String)configCache.get("TELEGRAMBOTCONFIGCACHE")).toString();
            Type type = new TypeToken<TeleBotConfig>(){}.getType();
            TeleBotConfig teleBotConfig = (TeleBotConfig)new Gson().fromJson(value, type);
            return teleBotConfig;
        }
        catch (Exception e) {
            return null;
        }
    }
}

