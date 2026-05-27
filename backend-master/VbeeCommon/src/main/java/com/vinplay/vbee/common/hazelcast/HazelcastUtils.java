/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 */
package com.vinplay.vbee.common.hazelcast;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserActiveModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserMoneyModel;
import com.vinplay.vbee.common.models.cache.UserVippointEventModel;
import java.util.ArrayList;
import java.util.List;

public class HazelcastUtils {
    public static IMap<String, UserCacheModel> getUserMap(String nickname) {
        return HazelcastClientFactory.getInstance().getMap("cache_user");
    }

    public static IMap<String, UserActiveModel> getActiveMap(String nickname) {
        return HazelcastClientFactory.getInstance().getMap("cache_user_active");
    }

    public static IMap<String, UserMoneyModel> getMoneyMap(String nickname) {
        return HazelcastClientFactory.getInstance().getMap("cache_user_money");
    }

    public static IMap<String, UserVippointEventModel> getVPEventMap(String nickname) {
        return HazelcastClientFactory.getInstance().getMap("cache_user_vp_event");
    }

    public static List<IMap<String, UserCacheModel>> getAllUserMap() {
        ArrayList<IMap<String, UserCacheModel>> allMap = new ArrayList<IMap<String, UserCacheModel>>();
        allMap.add(HazelcastUtils.getUserMap(""));
        return allMap;
    }

    public static List<IMap<String, UserMoneyModel>> getAllMoneyMap() {
        ArrayList<IMap<String, UserMoneyModel>> allMap = new ArrayList<IMap<String, UserMoneyModel>>();
        allMap.add(HazelcastUtils.getMoneyMap(""));
        return allMap;
    }
}

