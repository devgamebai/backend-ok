/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dailyQuest;

import com.vinplay.dailyQuest.model.DailyQuestModel;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class DailyQuestUtils {
    private static final Logger logger = Logger.getLogger((String)"rmq");

    public static DailyQuestModel getDailyQuestModel(String userName) {
        DistCache<String, DailyQuestModel> slotMap = CacheFactory.get("dailyQuestCache", DailyQuestModel.class);
        String key = userName;
        if (slotMap.containsKey(userName)) {
            return slotMap.get(key);
        }
        DailyQuestModel dailyQuestModel = new DailyQuestModel(userName);
        slotMap.put(key, dailyQuestModel);
        return dailyQuestModel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean playerReceiveGift(String userName, int index) {
        DistCache<String, DailyQuestModel> slotMap = CacheFactory.get("dailyQuestCache", DailyQuestModel.class);
        String key = userName;
        boolean check = true;
        if (slotMap.containsKey(userName)) {
            try (LockHandle h = slotMap.acquireLock(userName, 5, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("playerReceiveGift: lock timeout for user=" + userName);
                    return false;
                }
                DailyQuestModel dailyQuestModel = slotMap.get(key);
                check = dailyQuestModel.receiveGiftDailyQuest(index);
                slotMap.put(userName, dailyQuestModel);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return check;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void playerPlayGame(String userName, int gameID, long value) {
        DistCache<String, DailyQuestModel> slotMap = CacheFactory.get("dailyQuestCache", DailyQuestModel.class);
        String key = userName;
        if (slotMap.containsKey(userName)) {
            try (LockHandle h = slotMap.acquireLock(userName, 5, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("playerPlayGame: lock timeout for user=" + userName);
                    return;
                }
                DailyQuestModel dailyQuestModel = slotMap.get(key);
                dailyQuestModel.playGame(gameID, value);
                slotMap.put(userName, dailyQuestModel);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void playerLogin(String userName) {
        DistCache<String, DailyQuestModel> slotMap = CacheFactory.get("dailyQuestCache", DailyQuestModel.class);
        String key = userName;
        if (slotMap.containsKey(userName)) {
            try (LockHandle h = slotMap.acquireLock(userName, 5, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("playerLogin: lock timeout for user=" + userName);
                    return;
                }
                DailyQuestModel dailyQuestModel = slotMap.get(key);
                dailyQuestModel.playerLogin();
                slotMap.put(userName, dailyQuestModel);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            DailyQuestModel dailyQuestModel = new DailyQuestModel(userName);
            slotMap.put(key, dailyQuestModel);
        }
    }
}
