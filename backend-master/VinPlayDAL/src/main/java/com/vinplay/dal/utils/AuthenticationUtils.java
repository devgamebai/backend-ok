/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.dal.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.statics.Consts;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.UnsupportedEncodingException;

public class AuthenticationUtils {
    public static boolean decodeBaseAuthen(String dataEncryt) {
        boolean response = false;
        try {
            UserCacheModel user;
            String[] dataSplit = VinPlayUtils.decodeBase64((String)dataEncryt).split("\\|");
            String accessToken = dataSplit[0];
            String nickName = dataSplit[1];
            String key = dataSplit[2];
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey(nickName) && (user = (UserCacheModel)userMap.get(nickName)).getAccessToken().equals(accessToken) && Consts.AUTH_SECRET.equals(key)) {
                response = true;
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return response;
    }
}

