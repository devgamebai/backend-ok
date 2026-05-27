/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 */
package com.vinplay.vbee.common.models;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;

public class HoanTraVipConfig {
    public static int[] EXP_VIP = new int[]{80, 800, 4500, 8600, 50000, 1000000};
    public static int[] HOAN_TRA_SPORT = new int[]{50, 50, 60, 70, 80, 100};
    public static int[] HOAN_TRA_CASINO = new int[]{30, 30, 40, 50, 60, 80};
    public static int[] HOAN_TRA_GAME = new int[]{10, 10, 15, 20, 30, 50};

    public static int getIndexExpVip(int exp) {
        for (int i = 0; i < EXP_VIP.length; ++i) {
            if (exp >= EXP_VIP[i]) continue;
            return i;
        }
        return EXP_VIP.length - 1;
    }

    public static long getHoanTraSport(int exp, long money) {
        int index = HoanTraVipConfig.getIndexExpVip(exp);
        return money * (long)HOAN_TRA_SPORT[index] / 10000L;
    }

    public static long getHoanTraCasino(int exp, long money) {
        int index = HoanTraVipConfig.getIndexExpVip(exp);
        return money * (long)HOAN_TRA_CASINO[index] / 10000L;
    }

    public static long getHoanTraGame(int exp, long money) {
        int index = HoanTraVipConfig.getIndexExpVip(exp);
        return money * (long)HOAN_TRA_GAME[index] / 10000L;
    }

    static {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap map = instance.getMap("cacheConfig");
            String EXP_VIP_STR = (String)map.get("EXP_VIP");
            String HOAN_TRA_SPORT_STR = (String)map.get("HOAN_TRA_SPORT");
            String HOAN_TRA_CASINO_STR = (String)map.get("HOAN_TRA_CASINO");
            String HOAN_TRA_EGAME_STR = (String)map.get("HOAN_TRA_EGAME");
            int i = 0;
            int j = 0;
            int k = 0;
            int l = 0;
            for (String exp : EXP_VIP_STR.split(";")) {
                HoanTraVipConfig.EXP_VIP[i] = Integer.parseInt(exp.trim());
                ++i;
            }
            for (String exp : HOAN_TRA_SPORT_STR.split(";")) {
                HoanTraVipConfig.HOAN_TRA_SPORT[j] = Integer.parseInt(exp.trim());
                ++j;
            }
            for (String exp : HOAN_TRA_CASINO_STR.split(";")) {
                HoanTraVipConfig.HOAN_TRA_CASINO[k] = Integer.parseInt(exp.trim());
                ++k;
            }
            for (String exp : HOAN_TRA_EGAME_STR.split(";")) {
                HoanTraVipConfig.HOAN_TRA_GAME[l] = Integer.parseInt(exp.trim());
                ++l;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

