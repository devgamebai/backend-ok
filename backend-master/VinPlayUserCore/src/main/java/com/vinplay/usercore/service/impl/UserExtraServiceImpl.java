/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserExtraInfoModel
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.UserExtraService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserExtraInfoModel;

public class UserExtraServiceImpl
implements UserExtraService {
    @Override
    public UserExtraInfoModel getModelFromToken(String accessToken) {
        IMap tokenMap;
        HazelcastInstance instance;
        UserExtraInfoModel model = null;
        if (accessToken != null && !accessToken.isEmpty() && (tokenMap = (instance = HazelcastClientFactory.getInstance()).getMap("cacheToken")).containsKey(accessToken)) {
            String nickname = (String)tokenMap.get(accessToken);
            IMap userExtraMap = instance.getMap("cache_user_extra_info");
            if (userExtraMap.containsKey(nickname)) {
                model = (UserExtraInfoModel)userExtraMap.get(nickname);
            }
        }
        return model;
    }

    @Override
    public String getPlatformFromToken(String accessToken) {
        String platform = "wp";
        UserExtraInfoModel model = this.getModelFromToken(accessToken);
        if (model != null) {
            platform = model.getPlatfrom();
        }
        return platform;
    }
}

