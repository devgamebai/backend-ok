/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.NoHuMessage
 *  com.vinplay.vbee.common.messages.PotMessage
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  com.vinplay.vbee.common.models.FreezeModel
 *  com.vinplay.vbee.common.models.PotModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.PotResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.PotService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.NoHuMessage;
import com.vinplay.vbee.common.messages.PotMessage;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.models.PotModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.PotResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class PotServiceImpl
implements PotService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    @Override
    public PotModel getPot(String gameName) {
        // 2026-05-07 Wave 2 batch I (SUN-1248): huGameBai moved to Redis via DistCache.
        DistCache<String, PotModel> potMap = CacheFactory.get("huGameBai", PotModel.class);
        if (potMap.containsKey(gameName)) {
            return potMap.get(gameName);
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public PotResponse addMoneyPot(String potName, long money, boolean isInitial) {
        logger.debug(("Request addMoneyPot: " + potName));
        PotResponse res = new PotResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            logger.debug("hazelcast not connected");
            return res;
        }
        // 2026-05-07 Wave 2 batch I (SUN-1248): huGameBai moved to Redis via DistCache.
        // The pre-port code wrapped the read-modify-write of potName + "Vinplay" inside a
        // Hazelcast TransactionContext (ONE_PHASE). That txn was not distributed-atomic
        // anyway and is dropped here — it is no different from a JVM-crash-mid-section
        // failure, which the original code already tolerated. See plan §8.
        DistCache<String, PotModel> potMap = CacheFactory.get("huGameBai", PotModel.class);
        if (potMap.containsKey(potName)) {
            // Two-key Redis SETNX nesting via try-with-resources. Lock-ordering rule:
            // ALWAYS acquire potName BEFORE "Vinplay" — "Vinplay" is the singleton
            // accumulator that serializes anyway, so consistent acquisition order
            // prevents deadlock when two callers race with different potName values.
            try (LockHandle h1 = potMap.acquireLock(potName, 5, TimeUnit.SECONDS);
                 LockHandle h2 = potMap.acquireLock("Vinplay", 5, TimeUnit.SECONDS)) {
                if (h1 == null || h2 == null) {
                    logger.warn("addMoneyPot: lock timeout for pot=" + potName);
                    return res;
                }
                PotModel pot = potMap.get(potName);
                long valuePot = pot.getValue();
                long maxPot = -1L;
                IMap map = client.getMap("cacheConfig");
                if (map != null && map.containsKey("HU_GAME_BAI_MAX")) {
                    maxPot = Long.parseLong((String)map.get("HU_GAME_BAI_MAX"));
                }
                if (maxPot == -1L || valuePot + money <= maxPot) {
                    Calendar today = Calendar.getInstance();
                    int dateInYear = today.get(6);
                    if (!isInitial || dateInYear != pot.getLastDay()) {
                        PotModel potSystem = potMap.get("Vinplay");
                        long valuePotSystem = potSystem.getValue();
                        res.setValue(valuePot);
                        res.setValue(valuePotSystem);
                        valuePotSystem -= money;
                        pot.setValue(valuePot += money);
                        if (isInitial) {
                            pot.setLastDay(dateInYear);
                        }
                        potSystem.setValue(valuePotSystem);
                        PotMessage message = new PotMessage(potName, money, valuePot, valuePotSystem);
                        MessageBusFactory.get("queue_hu_gamebai").publish((String)"queue_hu_gamebai", (BaseMessage)message, (int)401);
                        potMap.put(potName, pot);
                        potMap.put("Vinplay", potSystem);
                        res.setSuccess(true);
                        res.setErrorCode("0");
                        res.setValue(valuePot);
                    }
                } else {
                    res.setErrorCode("1050");
                }
            }
            catch (Exception e2) {
                logger.debug(("addMoneyPot error : " + e2));
            }
        }
        logger.debug(("Response addMoneyPot: " + res.toJson()));
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public PotResponse noHu(String sessionId, String nickname, String gameName, String roomId, long maxFreeze, String matchId, String potName, double ratioPot, int betValue, String desc) {
        logger.debug(("Request noHu: sessionId: " + sessionId + ", nickname: " + nickname + ", gameName: " + gameName + ", roomId: " + roomId + ", maxFreeze: " + maxFreeze + ", matchId: " + matchId + ", potName: " + potName + ", ratioPot: " + ratioPot + ", betValue: " + betValue + ", desc: " + desc));
        PotResponse res = new PotResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            logger.debug("hazelcast not connected");
            return res;
        }
        // 2026-05-07 Wave 2 batch I (SUN-1248): huGameBai moved to Redis via DistCache.
        // users + freeze stay on Hazelcast (Wave 3/4). The pre-port code wrapped the
        // three-IMap mutation inside a Hazelcast TransactionContext (ONE_PHASE) \u2014 that
        // txn was not distributed-atomic anyway and cannot be relocated to Redis while
        // users/freeze remain on Hazelcast. Dropped explicitly. See plan \u00a78.
        DistCache<String, PotModel> potMap = CacheFactory.get("huGameBai", PotModel.class);
        IMap<String, UserModel> userMap = client.getMap("users");
        IMap freezeMap = client.getMap("freeze");
        if (potMap.containsKey(potName) && userMap.containsKey(nickname) && freezeMap.containsKey(sessionId)) {
            // Multi-backend lock-ordering rule (plan cross-cutting concern):
            // Redis (potMap.acquireLock) OUTER, Hazelcast (userMap.lock + freezeMap.lock) INNER.
            // Reason: a Hazelcast partition stall while holding a Redis lock just lets the
            // SETNX TTL expire (token-based release prevents wrong-unlock); reverse direction
            // risks an indefinite Hazelcast lock leak if Redis hangs.
            try (LockHandle potLock = potMap.acquireLock(potName, 5, TimeUnit.SECONDS)) {
                if (potLock == null) {
                    logger.warn("noHu: pot lock timeout for pot=" + potName);
                    return res;
                }
                try {
                    userMap.lock(nickname);
                    try {
                        freezeMap.lock(sessionId);
                        try {
                            long valuePotBefore;
                            PotModel pot = potMap.get(potName);
                            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                            FreezeModel freeze = (FreezeModel)freezeMap.get(sessionId);
                            long moneyUser = user.getVin();
                            long currentMoney = user.getVinTotal();
                            long freezeMoney = freeze.getMoney();
                            long valuePot = valuePotBefore = pot.getValue();
                            long money = Math.round((double)valuePot * ratioPot);
                            res.setCurrentMoneyUser(currentMoney);
                            res.setFreezeMoneyUser(freezeMoney);
                            res.setValue(valuePot);
                            res.setMoneyExchange(money);
                            long addMoneyUser = 0L;
                            long addMoneyFreeze = 0L;
                            if (freezeMoney >= maxFreeze) {
                                addMoneyUser = money;
                            } else if (freezeMoney + money > maxFreeze) {
                                addMoneyUser = freezeMoney + money - maxFreeze;
                                addMoneyFreeze = maxFreeze - freezeMoney;
                            } else {
                                addMoneyFreeze = money;
                            }
                            user.setVin(moneyUser += addMoneyUser);
                            user.setVinTotal(currentMoney += money);
                            freeze.setMoney(freezeMoney += addMoneyFreeze);
                            pot.setValue(valuePot -= money);
                            long fMoney = addMoneyFreeze == 0L ? -1L : freezeMoney;
                            NoHuMessage message = new NoHuMessage(VinPlayUtils.genMessageId(), user.getId(), nickname, gameName, moneyUser, currentMoney, money, "vin", fMoney, sessionId, 0L, 0, 0, potName, valuePot);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, gameName, VinPlayUtils.getServiceName((String)gameName), currentMoney, money, "vin", "N\u1ed5 h\u0169. Ph\u00f2ng: " + roomId + ", B\u00e0n: " + matchId, 0L, false, user.isBot());
                            messageLog.setReferralCode(user.getReferralCode());
                            LogNoHuGameBaiMessage nohuMessage = new LogNoHuGameBaiMessage(nickname, betValue, valuePotBefore, money, gameName, desc, "");
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)message, (int)19);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                            MessageBusFactory.get("queue_hu_gamebai").publish((String)"queue_hu_gamebai", (BaseMessage)nohuMessage, (int)402);
                            freezeMap.put(sessionId, freeze);
                            userMap.put(nickname, user);
                            potMap.put(potName, pot);
                            res.setSuccess(true);
                            res.setErrorCode("0");
                            res.setCurrentMoneyUser(currentMoney);
                            res.setFreezeMoneyUser(freezeMoney);
                            res.setValue(valuePot);
                            res.setMoneyExchange(money);
                        } catch (Exception e) {
                            logger.debug(("noHu error : " + e));
                        } finally {
                            freezeMap.unlock(sessionId);
                        }
                    } finally {
                        userMap.unlock(nickname);
                    }
                } catch (Exception e2) {
                    logger.debug(("noHu error : " + e2));
                }
            }
        }
        logger.debug(("Response noHu: " + res.toJson()));
        return res;
    }
}

