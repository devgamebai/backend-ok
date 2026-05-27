/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Vippoint
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.usercore.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.LuckyDaoImpl;
import com.vinplay.usercore.entities.vqmm.LuckyModel;
import com.vinplay.usercore.entities.vqmm.LuckyVipModel;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.Vippoint;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LuckyUtils {
    public static final int LUCKY_TYPE_VIN = 1;
    public static final int LUCKY_TYPE_XU = 2;
    public static final int LUCKY_TYPE_SLOT = 3;
    public static final int LUCKY_TYPE_MULTI = 4;
    public static final String TRUOT_ROI = "fail";
    public static final String THEM_LUOT = "more";
    public static List<LuckyVipModel> LUCKY_VIP = new ArrayList<LuckyVipModel>();
    public static List<LuckyModel> LUCKY = new ArrayList<LuckyModel>();

    public static double generateResult() {
        double result = new Random().nextDouble() * 100.0;
        return result;
    }

    public static void init(String luckyVipJson, String luckyJson, int userType) throws JSONException {
        JSONObject vipObj = new JSONObject(luckyVipJson);
        JSONObject vinVipObj = vipObj.getJSONObject("vin");
        JSONObject multiObj = vipObj.getJSONObject("multi");
        JSONArray num = vipObj.getJSONArray("num");
        for (int type = 0; type < num.length(); ++type) {
            HashMap<Integer, Integer> vinVip = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> multi = new HashMap<Integer, Integer>();
            Iterator iter = vinVipObj.keys();
            while (iter.hasNext()) {
                String key = (String)iter.next();
                int percent = vinVipObj.getJSONArray(key).getInt(type);
                vinVip.put(Integer.parseInt(key), percent);
            }
            Iterator iter2 = multiObj.keys();
            while (iter2.hasNext()) {
                String key2 = (String)iter2.next();
                int percent2 = multiObj.getJSONArray(key2).getInt(type);
                multi.put(Integer.parseInt(key2), percent2);
            }
            int numRotate = num.getInt(type);
            LUCKY_VIP.add(new LuckyVipModel(vinVip, multi, numRotate));
        }
        JSONObject obj = new JSONObject(luckyJson);
        JSONObject slotObj = obj.getJSONObject("slot");
        JSONObject vinObj = obj.getJSONObject("vin");
        JSONObject xuObj = obj.getJSONObject("xu");
        for (int type2 = 0; type2 < userType; ++type2) {
            HashMap<String, Integer> slot = new HashMap<String, Integer>();
            HashMap<String, Integer> vin = new HashMap<String, Integer>();
            HashMap<String, Integer> xu = new HashMap<String, Integer>();
            Iterator iter3 = slotObj.keys();
            while (iter3.hasNext()) {
                String key3 = (String)iter3.next();
                int percent3 = slotObj.getJSONArray(key3).getInt(type2);
                slot.put(key3, percent3);
            }
            Iterator iter4 = vinObj.keys();
            while (iter4.hasNext()) {
                String key4 = (String)iter4.next();
                int percent4 = vinObj.getJSONArray(key4).getInt(type2);
                vin.put(key4, percent4);
            }
            Iterator iter5 = xuObj.keys();
            while (iter5.hasNext()) {
                String key5 = (String)iter5.next();
                int percent5 = xuObj.getJSONArray(key5).getInt(type2);
                xu.put(key5, percent5);
            }
            LUCKY.add(new LuckyModel(slot, vin, xu));
        }
    }

    public static void initSlotMap() throws SQLException {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        IMap slotMap = instance.getMap("cacheSlotFree");
        slotMap.clear();
        LuckyDaoImpl dao = new LuckyDaoImpl();
        Map<String, SlotFreeDaily> imap = dao.getAllSlotFree();
        for (Map.Entry<String, SlotFreeDaily> entry : imap.entrySet()) {
            slotMap.put(entry.getKey(), entry.getValue());
        }
    }

    public static int getUserVipType(int level) {
        int type = -1;
        if (level == Vippoint.VANG.getId()) {
            type = 0;
        } else if (level == Vippoint.BACH_KIM_1.getId() || level == Vippoint.BACH_KIM_2.getId()) {
            type = 1;
        } else if (level == Vippoint.KIM_CUONG_1.getId() || level == Vippoint.KIM_CUONG_2.getId() || level == Vippoint.KIM_CUONG_3.getId()) {
            type = 2;
        }
        return type;
    }

    public static int getUserType(boolean mobileSer, long rechargeMoney) throws NumberFormatException, KeyNotFoundException {
        long rechargeIndex = GameCommon.getValueLong("LUCKY_RECHARGE_INDEX");
        int type = -1;
        if (!mobileSer && rechargeMoney == 0L) {
            type = 0;
        } else if (!mobileSer && rechargeMoney > 0L || mobileSer && rechargeMoney == 0L) {
            type = 1;
        } else if (mobileSer && rechargeMoney > 0L && rechargeMoney <= rechargeIndex) {
            type = 2;
        } else if (mobileSer && rechargeMoney > rechargeIndex) {
            type = 3;
        }
        return type;
    }

    public static int getNumLuckyVip(int userType) {
        return LUCKY_VIP.get(userType).getNumRotate();
    }

    public static int getResultLuckyVip(double rd, int luckyType, int userType) {
        int res = -1;
        Map<Integer, Integer> prize = new HashMap();
//        Map<Object, Object> prize = new HashMap();
        if (luckyType == 1) {
            prize = (Map<Integer, Integer>)LUCKY_VIP.get(userType).getVin();
        } else {
            if (luckyType != 4) {
                return res;
            }
            prize = LUCKY_VIP.get(userType).getMulti();
        }
        int percent = 0;
        for (Map.Entry<Integer, Integer> entry : prize.entrySet()) {
            if ((Integer)entry.getValue() <= 0 || !(rd < (double)(percent += ((Integer)entry.getValue()).intValue()))) continue;
            res = (Integer)entry.getKey();
            break;
        }
        return res;
    }

    public static String getResultLucky(double rd, int luckyType, int userType) {
        String res = "";
        Map<String, Integer> prize = new HashMap();
        if (luckyType == 1) {
            prize = LUCKY.get(userType).getVin();
        } else if (luckyType == 2) {
            prize = LUCKY.get(userType).getXu();
        } else {
            if (luckyType != 3) {
                return res;
            }
            prize = LUCKY.get(userType).getSlot();
        }
        int percent = 0;
        for (Map.Entry entry : prize.entrySet()) {
            if ((Integer)entry.getValue() <= 0 || !(rd < (double)(percent += ((Integer)entry.getValue()).intValue()))) continue;
            res = (String)entry.getKey();
            break;
        }
        return res;
    }
}

