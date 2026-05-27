/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.StatisticUserMarketing
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 */
package com.vinplay.usercore.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.CacheService;
import com.vinplay.usercore.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.StatisticUserMarketing;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;

public class UserMakertingUtil {
    private static CacheService cacheService = new CacheServiceImpl();

    public static void newRegisterUser(String campaign, String medium, String source) {
        String key = UserMakertingUtil.buildUserMakertingKey(campaign, medium, source);
        boolean addNew = false;
        StatisticUserMarketing model = null;
        try {
            model = (StatisticUserMarketing)cacheService.getObject(key);
            if (model == null) {
                addNew = true;
            } else {
                model.setNRU(model.getNRU() + 1);
            }
        }
        catch (KeyNotFoundException e) {
            addNew = true;
        }
        if (addNew) {
            model = new StatisticUserMarketing(campaign, medium, source, 1, 0, 0L, 0L);
        }
        cacheService.setObject(key, model);
    }

    public static void userNapVin(String username, long value) {
        UserCacheModel model;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(username) && (model = (UserCacheModel)userMap.get(username)) != null) {
            long totalRecharge = model.getRechargeMoney();
            String campaign = model.getCampaign();
            String medium = model.getMedium();
            String source = model.getSource();
            String key = UserMakertingUtil.buildUserMakertingKey(campaign, medium, source);
            try {
                StatisticUserMarketing sUM = (StatisticUserMarketing)cacheService.getObject(key);
                if (sUM != null) {
                    if (totalRecharge == value) {
                        sUM.setPU(sUM.getPU() + 1);
                    }
                    sUM.addNapVin(value);
                    cacheService.setObject(key, sUM);
                }
            }
            catch (KeyNotFoundException sUM) {
                // empty catch block
            }
        }
    }

    public static void userTieuVin(String username, long value) {
        UserCacheModel model;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(username) && (model = (UserCacheModel)userMap.get(username)) != null) {
            String campaign = model.getCampaign();
            String medium = model.getMedium();
            String source = model.getSource();
            String key = UserMakertingUtil.buildUserMakertingKey(campaign, medium, source);
            try {
                StatisticUserMarketing sUM = (StatisticUserMarketing)cacheService.getObject(key);
                if (sUM != null) {
                    sUM.addTieuVin(value);
                    cacheService.setObject(key, sUM);
                }
            }
            catch (KeyNotFoundException sUM) {
                // empty catch block
            }
        }
    }

    public static String buildUserMakertingKey(String campaign, String medium, String source) {
        return campaign + "_" + medium + "_" + source + "_";
    }
}

