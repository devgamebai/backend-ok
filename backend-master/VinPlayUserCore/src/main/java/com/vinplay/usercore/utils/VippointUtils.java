/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Vippoint
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.vippoint.EventVPBonusModel
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.MapUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.usercore.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import com.vinplay.vbee.common.enums.Vippoint;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.vippoint.EventVPBonusModel;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.MapUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VippointUtils {
    private static final Logger logger = Logger.getLogger((String)"user_core");
    public static String START = "";
    public static String END = "";
    public static String START_LUCKY_TIME = "";
    public static String END_LUCKY_TIME = "";
    public static String START_UNLUCKY_TIME = "";
    public static String END_UNLUCKY_TIME = "";
    public static int LUCKY_IN_DAY = 0;
    public static int UNLUCKY_IN_DAY = 0;
    public static int DAY_RUN = 0;
    public static double RATE_SUB_VP = 0.3;
    public static List<Integer> PLACES = Arrays.asList(10, 30, 60, 100, 150, 210, 300, 400, 540, 750, 1000, 1500, 2200, 3000, 4000, 6000);
    public static Map<Integer, Integer> ADD_VP_MAP = new HashMap<Integer, Integer>();
    public static Map<Integer, Integer> ADD_VIN_MAP = new HashMap<Integer, Integer>();
    public static final int TYPE_UPDATE_VP = 0;
    public static final int TYPE_ADD_VP = 1;
    public static final int TYPE_SUB_VP = 2;
    public static final int TYPE_ADD_VIN = 3;
    public static Date START_TIME;
    public static Date END_X2_TIME;

    public static void init() throws JSONException, SQLException, ParseException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        JSONObject vpeObj = new JSONObject(dao.getGameCommon("vippoint_event"));
        START = vpeObj.getString("start");
        END = vpeObj.getString("end");
        START_TIME = VinPlayUtils.getDateTime((String)START);
        END_X2_TIME = VinPlayUtils.getDateTime((String)vpeObj.getString("end_x2"));
        JSONObject luckyTimeStr = vpeObj.getJSONObject("lucky_time");
        START_LUCKY_TIME = luckyTimeStr.getString("start");
        END_LUCKY_TIME = luckyTimeStr.getString("end");
        JSONObject unluckyTimeStr = vpeObj.getJSONObject("unlucky_time");
        START_UNLUCKY_TIME = unluckyTimeStr.getString("start");
        END_UNLUCKY_TIME = unluckyTimeStr.getString("end");
        LUCKY_IN_DAY = vpeObj.getInt("lucky_in_day");
        UNLUCKY_IN_DAY = vpeObj.getInt("unlucky_in_day");
        DAY_RUN = (int)((VinPlayUtils.getDateTime((String)END).getTime() - VinPlayUtils.getDateTime((String)START).getTime()) / 86400000L);
        RATE_SUB_VP = vpeObj.getDouble("sub_vp");
        JSONArray jArrayVP = vpeObj.getJSONArray("add_vp");
        if (jArrayVP != null) {
            for (int i = 0; i < jArrayVP.length(); ++i) {
                JSONObject jObj = jArrayVP.getJSONObject(i);
                Iterator keys = jObj.keys();
                while (keys.hasNext()) {
                    String key = (String)keys.next();
                    ADD_VP_MAP.put(Integer.parseInt(key), jObj.getInt(key));
                }
            }
        }
        ADD_VP_MAP = MapUtils.sortMapIntByValue(ADD_VP_MAP);
        JSONArray jArrayVin = vpeObj.getJSONArray("add_vin");
        if (jArrayVin != null) {
            for (int j = 0; j < jArrayVin.length(); ++j) {
                JSONObject jObj2 = jArrayVin.getJSONObject(j);
                Iterator keys2 = jObj2.keys();
                while (keys2.hasNext()) {
                    String key2 = (String)keys2.next();
                    ADD_VIN_MAP.put(Integer.parseInt(key2), jObj2.getInt(key2));
                }
            }
        }
        ADD_VIN_MAP = MapUtils.sortMapIntByValue(ADD_VIN_MAP);
        VippointUtils.calculateNumBonusInDay();
    }

    public static long calculateMoneyVP(String moneyType, Long transId, HazelcastInstance client, String nickname, String gameName, long money, TransType type) {
        long moneyVP = 0L;
        if (moneyType.equals("vin")) {
            if (transId != null) {
                IMap vpCache = client.getMap("VPMinigame");
                String vpCacheId = nickname + gameName + transId;
                if (type.getId() == TransType.END_TRANS.getId()) {
                    if (vpCache.containsKey(vpCacheId)) {
                        moneyVP = Math.abs((Long)vpCache.get(vpCacheId) + money);
                        vpCache.remove(vpCacheId);
                    }
                } else if (vpCache.containsKey(vpCacheId)) {
                    vpCache.put(vpCacheId, ((Long)vpCache.get(vpCacheId) + money));
                } else {
                    vpCache.put(vpCacheId, money);
                }
            } else if (type.getId() == TransType.VIPPOINT.getId()) {
                moneyVP = Math.abs(money);
            }
        }
        return moneyVP;
    }

    public static List<Integer> calculateVP(HazelcastInstance client, String nickname, long moneyVP, boolean daiLy) {
        boolean inEvent = false;
        boolean inEventX2 = false;
        int vpEvent = 0;
        int vp = 0;
        try {
            if (!daiLy) {
                inEvent = VippointUtils.checkEventRunning();
                inEventX2 = VippointUtils.checkEventX2Running();
            }
            int vpIndex = GameCommon.getValueInt("VIPPOINT_INDEX");
            if (inEventX2) {
                vpIndex /= 2;
            }
            vp = (int)(moneyVP / (long)vpIndex);
            if (inEvent) {
                vpEvent = vp;
                com.vinplay.vbee.common.cache.DistCache<String, Integer> usersPlayGame =
                        com.vinplay.vbee.common.cache.CacheFactory.get("cacheUsersPlayGame", Integer.class);
                usersPlayGame.put(nickname, 1, 120L, TimeUnit.SECONDS);
            }
            moneyVP %= (long)vpIndex;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<Integer> res = new ArrayList<Integer>();
        res.add(vp);
        res.add((int)moneyVP);
        res.add(vpEvent);
        return res;
    }

    public static List<Integer> calculateVPAgent(int moneyVP, int money, int vpUser, int vpSaveUser) {
        int vp = 0;
        int vpSave = 0;
        if (money < 0) {
            int vpIndex = 5000000;
            if (moneyVP + money < 0) {
                vp = (moneyVP += money) / 5000000 - 1;
                moneyVP = 5000000 + moneyVP % 5000000;
                vpSave = vp + vpSaveUser < 0 ? -vpSaveUser : vp;
                if (vp + vpUser < 0) {
                    vp = -vpUser;
                    moneyVP = 0;
                }
            } else {
                moneyVP = 0;
            }
        }
        ArrayList<Integer> res = new ArrayList<Integer>();
        res.add(vp);
        res.add(vpSave);
        res.add(moneyVP);
        return res;
    }

    public static long getMoneyCashout(int vpSave, int vp) {
        long money = 0L;
        money = vpSave <= Vippoint.DA.getMoney() ? (long)(vp * Vippoint.DA.getRatio())
                : (vpSave <= Vippoint.DONG.getMoney() ? (long)(vp * Vippoint.DONG.getRatio())
                    : (vpSave <= Vippoint.BAC.getMoney() ? (long)(vp * Vippoint.BAC.getRatio())
                        : (vpSave <= Vippoint.VANG.getMoney() ? (long)(vp * Vippoint.VANG.getRatio())
                            : (vpSave <= Vippoint.BACH_KIM_1.getMoney() ? (long)(vp * Vippoint.BACH_KIM_1.getRatio())
                                : (vpSave <= Vippoint.BACH_KIM_2.getMoney() ? (long)(vp * Vippoint.BACH_KIM_2.getRatio())
                                    : (vpSave <= Vippoint.KIM_CUONG_1.getMoney() ? (long)(vp * Vippoint.KIM_CUONG_1.getRatio())
                                        : (vpSave <= Vippoint.KIM_CUONG_2.getMoney() ? (long)(vp * Vippoint.KIM_CUONG_2.getRatio())
                                            : (long)(vp * Vippoint.KIM_CUONG_3.getRatio()))))))));
        return money;
    }

    public static int getLevel(int vpSave) {
        int level = 0;
        level = vpSave <= Vippoint.DA.getMoney() ? Vippoint.DA.getId() : (vpSave <= Vippoint.DONG.getMoney() ? Vippoint.DONG.getId() : (vpSave <= Vippoint.BAC.getMoney() ? Vippoint.BAC.getId() : (vpSave <= Vippoint.VANG.getMoney() ? Vippoint.VANG.getId() : (vpSave <= Vippoint.BACH_KIM_1.getMoney() ? Vippoint.BACH_KIM_1.getId() : (vpSave <= Vippoint.BACH_KIM_2.getMoney() ? Vippoint.BACH_KIM_2.getId() : (vpSave <= Vippoint.KIM_CUONG_1.getMoney() ? Vippoint.KIM_CUONG_1.getId() : (vpSave <= Vippoint.KIM_CUONG_2.getMoney() ? Vippoint.KIM_CUONG_2.getId() : Vippoint.KIM_CUONG_3.getId())))))));
        return level;
    }

    public static boolean checkEventRunning() {
        boolean res = false;
        try {
            int status = GameCommon.getValueInt("VIPPOINT_EVENT_STATUS");
            if (status == 1) {
                res = true;
            }
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (KeyNotFoundException e2) {
            e2.printStackTrace();
        }
        return res;
    }

    public static boolean checkEventX2Running() {
        boolean res = false;
        try {
            int status = GameCommon.getValueInt("VIPPOINT_EVENT_X2_STATUS");
            if (status == 1) {
                res = true;
            }
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (KeyNotFoundException e2) {
            e2.printStackTrace();
        }
        return res;
    }

    public static List<Date> randomUnluckyTime(Date start, Date end) throws ParseException, SQLException {
        ArrayList<Date> unluckyTime = new ArrayList<Date>();
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd " + END_UNLUCKY_TIME);
        SimpleDateFormat format3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        Date endDay = format3.parse(format2.format(now));
        if (now.getTime() < endDay.getTime()) {
            long min;
            long max;
            Random random = new Random();
            VippointDaoImpl dao = new VippointDaoImpl();
            int runInDay = dao.getNumRunInDay(VinPlayUtils.getCurrentDate(), 0);
            int unlucky = UNLUCKY_IN_DAY - runInDay;
            if (unlucky > 0 && (min = now.getTime() + 60000L) < (max = endDay.getTime())) {
                for (int i = 1; i <= unlucky; ++i) {
                    long randomValue = min + (long)(random.nextDouble() * (double)(max - min));
                    unluckyTime.add(new Date(randomValue));
                }
            }
        }
        return unluckyTime;
    }

    public static List<Date> randomLuckyTime(Date start, Date end) throws ParseException, SQLException {
        ArrayList<Date> unluckyTime = new ArrayList<Date>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd " + END_LUCKY_TIME);
        Date now = new Date();
        Date endDay = format.parse(format2.format(now));
        if (now.getTime() < endDay.getTime()) {
            long min;
            long max;
            Random random = new Random();
            VippointDaoImpl dao = new VippointDaoImpl();
            int runInDay = dao.getNumRunInDay(VinPlayUtils.getCurrentDate(), 1);
            int lucky = LUCKY_IN_DAY - runInDay;
            if (lucky > 0 && (min = now.getTime() + 60000L) < (max = endDay.getTime())) {
                for (int i = 1; i <= lucky; ++i) {
                    long randomValue = min + (long)(random.nextDouble() * (double)(max - min));
                    unluckyTime.add(new Date(randomValue));
                }
            }
        }
        return unluckyTime;
    }

    public static int calculatePlace(int vpEvent) {
        int place = 0;
        for (int i = VippointUtils.PLACES.size() - 1; i >= 0; --i) {
            if (vpEvent < PLACES.get(i)) continue;
            place = i + 1;
            break;
        }
        return place;
    }

    public static String getAvatar(String nickname) {
        String avatar = "0";
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            if (userMap.containsKey(nickname)) {
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                avatar = user.getAvatar();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return avatar;
    }

    public static int getTypeBonus(Date timeVP, Date timeVin) throws ParseException {
        int res = -1;
        boolean bonusVP = true;
        boolean bonusVin = true;
        Date now = new Date();
        if (timeVin != null && VinPlayUtils.compareDate((Date)timeVin, (Date)now) == 0) {
            bonusVin = false;
        }
        if (bonusVin) {
            Random rd = new Random();
            res = rd.nextInt(2);
        } else {
            res = bonusVin ? 1 : 0;
        }
        return res;
    }

    public static int getVPBonus(int vpReal) {
        int vp = 0;
        ArrayList<Integer> listBonus = new ArrayList<Integer>();
        for (Map.Entry<Integer, Integer> entry : ADD_VP_MAP.entrySet()) {
            if (vpReal < entry.getKey()) continue;
            listBonus.add(entry.getValue());
        }
        if (listBonus.size() > 0) {
            Random rd = new Random();
            vp = (Integer)listBonus.get(rd.nextInt(listBonus.size()));
        }
        return vp;
    }

    public static List<Integer> getVinBonus(int vpReal) {
        ArrayList<Integer> vinBonus = new ArrayList<Integer>();
        for (Map.Entry<Integer, Integer> entry : ADD_VIN_MAP.entrySet()) {
            if (vpReal < entry.getKey()) continue;
            int vin = entry.getValue();
            vinBonus.add(vin);
        }
        if (vinBonus.size() > 1) {
            Collections.shuffle(vinBonus);
        }
        return vinBonus;
    }

    public static int getVippointSub(int vpEvent, int place) {
        int subVP = 0;
        if (place >= 1) {
            Random rd = new Random();
            subVP = Math.round(vpEvent * (15 + rd.nextInt(16)) / 100);
            if (place >= 2) {
                int vp = vpEvent - subVP;
                int vpPlaceMin = PLACES.get(place - 2);
                if (vp < vpPlaceMin) {
                    subVP = vpEvent - vpPlaceMin;
                }
            } else {
                subVP = vpEvent - PLACES.get(place - 1);
            }
        }
        return subVP;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void calculateNumBonusInDay() throws SQLException, ParseException {
        VippointDaoImpl dao = new VippointDaoImpl();
        List<EventVPBonusModel> bonusList = dao.getEventVPBonus();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        Date now = new Date();
        int dayRuned = (int)(format.parse(format2.format(now)).getTime() - format.parse(format2.format(START_TIME)).getTime()) / 86400000;
        if (dayRuned >= 0) {
            // 2026-05-07 Wave 2 batch G (SUN-1248): cacheEventVpBonus moved to Redis via DistCache.
            DistCache<String, Integer> bonusMap = CacheFactory.get("cacheEventVpBonus", Integer.class);
            for (EventVPBonusModel model : bonusList) {
                String vinKey = String.valueOf(model.getValue());
                // Always acquire the lock. Original code only locked if key already present (cold-start put
                // raced); SETNX is cheap and the always-lock path closes that latent race.
                try (LockHandle h = bonusMap.acquireLock(vinKey, 5, TimeUnit.SECONDS)) {
                    if (h == null) {
                        // Per-model loop — skip this row and process the next one rather than aborting
                        // the whole daily reset (a single hot-key contender shouldn't poison the cron run).
                        logger.warn("calculateNumBonusInDay: lock timeout for vin=" + model.getValue());
                        continue;
                    }
                    int num = model.getNum();
                    int use = model.getUse();
                    int maxInday = Math.round(num / DAY_RUN);
                    int useToday = maxInday * (dayRuned + 1) - use;
                    if (useToday < 0 || num - use <= 0) {
                        useToday = 0;
                    } else if (useToday > num - use) {
                        useToday = num - use;
                    }
                    bonusMap.put(vinKey, useToday);
                }
                catch (Exception e) {
                    logger.error(e);
                }
            }
        }
    }
}

