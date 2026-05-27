/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.vippoint.VippointEventMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.dao.impl.AgentDaoImpl;
import com.vinplay.usercore.dao.impl.MoneyInGameDaoImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.entities.VPResponse;
import com.vinplay.usercore.entities.VippointResponse;
import com.vinplay.usercore.service.VippointService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class VippointServiceImpl
implements VippointService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public VippointResponse cashoutVP(String nickname) {
        VippointResponse res = new VippointResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.isHasMobileSecurity()) {
                    long moneyUser = user.getVin();
                    long currentMoney = user.getVinTotal();
                    res.setCurrentMoney(currentMoney);
                    if (user.getVippoint() > 0 && user.getVippointSave() > 0) {
                        int vp = user.getVippoint();
                        int vpSave = user.getVippointSave();
                        long moneyAdd = VippointUtils.getMoneyCashout(vpSave, vp);
                        if (moneyAdd > 0L) {
                            String description = "\u0110\u1ed5i " + vp + " Vippoint";
                            user.setVin(moneyUser += moneyAdd);
                            user.setVinTotal(currentMoney += moneyAdd);
                            user.setVippoint(0);
                            MoneyMessageInMinigame message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashoutByVP", moneyUser, currentMoney, moneyAdd, "vin", 0L, 0, -vp);
                            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashoutByVP", "Nh\u1eadn th\u01b0\u1edfng Vippoint", currentMoney, moneyAdd, "vin", description, 0L, false, user.isBot());
                            messageLog.setReferralCode(user.getReferralCode());
                            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)message, (int)16);
                            MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                            userMap.put(nickname, user);
                            res.setErrorCode("0");
                            res.setSuccess(true);
                            res.setCurrentMoney(currentMoney);
                            res.setMoneyAdd(moneyAdd);
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public VPResponse getVippoint(String nickname) {
        VPResponse res = new VPResponse(false, "1001");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                res.setSuccess(true);
                res.setErrorCode("0");
                res.setVippoint(user.getVippoint());
                res.setVippointSave(user.getVippointSave());
            }
            catch (Exception e) {
                logger.debug(e);
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public byte checkCashoutVP(String nickname) {
        int res = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (user.isHasMobileSecurity()) {
                    if (user.getVippoint() > 0 && user.getVippointSave() > 0) {
                        res = 0;
                    }
                } else {
                    res = 2;
                }
            }
            catch (Exception e) {
                logger.debug(e);
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        return (byte)res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<String> subVippointEvent() {
        ArrayList<String> res = new ArrayList<String>();
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            // cacheUsersPlayGame stores Integer (sentinel '1') keyed by nickname.
            // VippointUtils writes via DistCache; readers here only use entry.getKey().
            com.vinplay.vbee.common.cache.DistCache<String, Integer> usersPlayGame =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheUsersPlayGame", Integer.class);
            IMap<String, UserModel> userMap = client.getMap("users");
            Random rd = new Random();
            VippointDaoImpl dao = new VippointDaoImpl();
            dao.updateNumInDay(VinPlayUtils.getCurrentDate(), 0);
            for (Map.Entry entry : usersPlayGame.entrySet()) {
                String nickname = (String)entry.getKey();
                if (!userMap.containsKey(nickname)) continue;
                try {
                    int i;
                    int subVP;
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    boolean bSub = false;
                    if (user.isBot()) {
                        i = rd.nextInt(GameCommon.getValueInt("VIPPOINT_EVENT_RATE_SUB_BOT"));
                        if (i == 0) {
                            bSub = true;
                        }
                    } else {
                        i = rd.nextInt(GameCommon.getValueInt("VIPPOINT_EVENT_RATE_SUB"));
                        if (i == 0) {
                            bSub = true;
                        }
                    }
                    if (!bSub || (subVP = VippointUtils.getVippointSub(user.getVpEvent(), user.getPlace())) <= 0) continue;
                    int vpEvent = user.getVpEvent();
                    int vpSub = user.getVpSub();
                    int numSub = user.getNumSub();
                    int place = VippointUtils.calculatePlace(vpEvent -= subVP);
                    int placeMax = place > user.getPlace() ? place : user.getPlace();
                    user.setVpEvent(vpEvent);
                    user.setSubVPTime(new Date());
                    user.setVpSub(vpSub += subVP);
                    user.setNumSub(++numSub);
                    user.setPlace(place);
                    user.setPlaceMax(placeMax);
                    VippointEventMessage vpEventMessage = new VippointEventMessage(user.getId(), nickname, 0, vpEvent, 0, 0, vpSub, numSub, place, placeMax, subVP, 2);
                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage)vpEventMessage, (int)801);
                    userMap.put(nickname, user);
                    res.add(nickname);
                }
                catch (Exception e) {
                    logger.error(e);
                }
                finally {
                    userMap.unlock(nickname);
                }
            }
        }
        catch (Exception e2) {
            logger.error(e2);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<String> addVippointEvent() {
        UserServiceImpl userSer = new UserServiceImpl();
        ArrayList<String> res = new ArrayList<String>();
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            com.vinplay.vbee.common.cache.DistCache<String, Integer> usersPlayGame =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheUsersPlayGame", Integer.class);
            IMap<String, UserModel> userMap = client.getMap("users");
            // 2026-05-07 Wave 2 batch G (SUN-1248): cacheEventVpBonus moved to Redis via DistCache.
            // Lock-ordering rule: Redis (bonusMap) acquired OUTSIDE Hazelcast (userMap) \u2014 see typeBonus==1 path below.
            DistCache<String, Integer> bonusMap = CacheFactory.get("cacheEventVpBonus", Integer.class);
            Random rd = new Random();
            VippointDaoImpl dao = new VippointDaoImpl();
            dao.updateNumInDay(VinPlayUtils.getCurrentDate(), 1);
            for (Map.Entry entry : usersPlayGame.entrySet()) {
                String nickname = (String)entry.getKey();
                int moneyBonus = 0;
                if (userMap.containsKey(nickname)) {
                    int i;
                    // Unlocked peek to make the random-gate + branch decision. Stale read is benign:
                    // worst case we pick the wrong branch and skip granting a bonus this tick.
                    UserCacheModel userPeek = (UserCacheModel)userMap.get(nickname);
                    boolean bAdd = false;
                    if (userPeek.isBot()) {
                        i = rd.nextInt(GameCommon.getValueInt("VIPPOINT_EVENT_RATE_ADD_BOT"));
                        if (i == 0) {
                            bAdd = true;
                        }
                    } else {
                        i = rd.nextInt(GameCommon.getValueInt("VIPPOINT_EVENT_RATE_ADD"));
                        if (i == 0) {
                            bAdd = true;
                        }
                    }
                    if (bAdd) {
                        int typeBonus = VippointUtils.getTypeBonus(userPeek.getAddVPTime(), userPeek.getAddVPVinTime());
                        if (typeBonus == 1) {
                            List<Integer> vinBonus = VippointUtils.getVinBonus(userPeek.getVpEventReal());
                            if (vinBonus.size() > 0) {
                                for (Integer vin : vinBonus) {
                                    if (moneyBonus != 0) break;
                                    String vinKey = String.valueOf(vin);
                                    if (!bonusMap.containsKey(vinKey)) continue;
                                    // INVERTED lock order: Redis bonusMap OUTER, Hazelcast userMap INNER.
                                    // Reason: a Hazelcast partition stall while holding a Redis SETNX lock
                                    // would let the TTL expire and a peer steal the lock; reversing avoids
                                    // a Hazelcast lock leak if the Redis call hangs (token-based release
                                    // ensures we never unlock someone else's hold).
                                    try (LockHandle bonusLock = bonusMap.acquireLock(vinKey, 5, TimeUnit.SECONDS)) {
                                        if (bonusLock == null) {
                                            logger.warn("addVippointEvent: bonus lock timeout for vin=" + vin);
                                            continue;
                                        }
                                        Integer numBonusTodayObj = bonusMap.get(vinKey);
                                        if (numBonusTodayObj == null) continue;
                                        int numBonusToday = numBonusTodayObj;
                                        if (numBonusToday <= 0) continue;
                                        try {
                                            userMap.lock(nickname);
                                            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                            moneyBonus = vin;
                                            bonusMap.put(vinKey, (--numBonusToday));
                                            int numAdd = user.getNumAdd();
                                            user.setAddVPVinTime(new Date());
                                            user.setNumAdd(++numAdd);
                                            VippointEventMessage vpEventMessage = new VippointEventMessage(user.getId(), nickname, 0, 0, 0, numAdd, 0, 0, 0, 0, moneyBonus, 3);
                                            MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage)vpEventMessage, (int)801);
                                            res.add(nickname);
                                            userMap.put(nickname, user);
                                        } finally {
                                            userMap.unlock(nickname);
                                        }
                                    } catch (Exception e) {
                                        logger.error(e);
                                    }
                                }
                            }
                        } else if (typeBonus == 0) {
                            int vpBonus = VippointUtils.getVPBonus(userPeek.getVpEventReal());
                            if (vpBonus > 0) {
                                try {
                                    userMap.lock(nickname);
                                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                                    int vpAdd = user.getVpAdd();
                                    int numAdd2 = user.getNumAdd();
                                    int vpEvent = user.getVpEvent();
                                    int place = VippointUtils.calculatePlace(vpEvent += vpBonus);
                                    int placeMax = place > user.getPlace() ? place : user.getPlace();
                                    user.setVpEvent(vpEvent);
                                    user.setAddVPTime(new Date());
                                    user.setVpAdd(vpAdd += vpBonus);
                                    user.setNumAdd(++numAdd2);
                                    user.setPlace(place);
                                    user.setPlaceMax(placeMax);
                                    VippointEventMessage vpEventMessage2 = new VippointEventMessage(user.getId(), nickname, 0, vpEvent, vpAdd, numAdd2, 0, 0, place, placeMax, vpBonus, 1);
                                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage)vpEventMessage2, (int)801);
                                    res.add(nickname);
                                    userMap.put(nickname, user);
                                } catch (Exception e2) {
                                    logger.error(e2);
                                } finally {
                                    userMap.unlock(nickname);
                                }
                            }
                        }
                    }
                }
                if (moneyBonus <= 0) continue;
                userSer.updateMoney(nickname, moneyBonus, "vin", "EventVPBonus", "S\u1ef1 ki\u1ec7n Vippoint t\u1eb7ng qu\u00e0", "G\u00f3i qu\u00e1 " + moneyBonus + " Vin", 0L, null, TransType.NO_VIPPOINT);
            }
        }
        catch (Exception e3) {
            logger.error(e3);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int updateVippointEvent(String nickname, int value, String type) {
        int res = 1;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                if (value > 0) {
                    int vpEvent = user.getVpEvent();
                    int vpSub = user.getVpSub();
                    int numSub = user.getNumSub();
                    int vpAdd = user.getVpAdd();
                    int numAdd = user.getNumAdd();
                    VippointEventMessage vpEventMessage = null;
                    if (type.equals("1")) {
                        int place = VippointUtils.calculatePlace(vpEvent += value);
                        int placeMax = place > user.getPlace() ? place : user.getPlace();
                        user.setAddVPTime(new Date());
                        user.setVpAdd(vpAdd += value);
                        user.setNumAdd(++numAdd);
                        user.setVpEvent(vpEvent);
                        user.setPlace(place);
                        user.setPlaceMax(placeMax);
                        vpEventMessage = new VippointEventMessage(user.getId(), nickname, 0, vpEvent, vpAdd, numAdd, 0, 0, place, placeMax, value, 1);
                    } else {
                        int place = VippointUtils.calculatePlace(vpEvent -= value);
                        int placeMax = place > user.getPlace() ? place : user.getPlace();
                        user.setSubVPTime(new Date());
                        user.setVpSub(vpSub += value);
                        user.setNumSub(++numSub);
                        user.setVpEvent(vpEvent);
                        user.setPlace(place);
                        user.setPlaceMax(placeMax);
                        vpEventMessage = new VippointEventMessage(user.getId(), nickname, 0, vpEvent, 0, 0, vpSub, numSub, place, placeMax, value, 2);
                    }
                    MessageBusFactory.get("queue_vippoint_event").publish("queue_vippoint_event", (BaseMessage)vpEventMessage, (int)801);
                    userMap.put(nickname, user);
                    res = 0;
                }
            }
            catch (Exception e) {
                logger.error(e);
            }
            finally {
                userMap.unlock(nickname);
            }
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean updateVippointAgent(String nicknameSend, String nicknameReceive, long moneySend, long moneyReceive, int status) {
        long money = 0L;
        String nickname = null;
        try {
            AgentDaoImpl dao = new AgentDaoImpl();
            if (status == 1) {
                return true;
            }
            if (status == 2) {
                return true;
            }
            if (status == 3) {
                money = moneySend;
                nickname = nicknameSend;
            } else {
                if (status != 6) {
                    return true;
                }
                money = moneySend;
                nickname = dao.getNicknameAgent1(nicknameSend);
            }
            if (nickname != null && money != 0L) {
                int vp = 0;
                int moneyVPs = 0;
                int vpSave = 0;
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                if (userMap.containsKey(nickname)) {
                    try {
                        userMap.lock(nickname);
                        UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                        if (money > 0L) {
                            List<Integer> vpLst = VippointUtils.calculateVP(client, user.getNickname(), (long)user.getMoneyVP() + Math.abs(money), true);
                            vp = vpLst.get(0);
                            moneyVPs = vpLst.get(1);
                            vpSave = vp;
                        } else {
                            List<Integer> vpLst = VippointUtils.calculateVPAgent(user.getMoneyVP(), (int)money, user.getVippoint(), user.getVippointSave());
                            vp = vpLst.get(0);
                            vpSave = vpLst.get(1);
                            moneyVPs = vpLst.get(2);
                        }
                        user.setVippoint(user.getVippoint() + vp);
                        user.setVippointSave(user.getVippointSave() + vpSave);
                        user.setMoneyVP(moneyVPs);
                        userMap.put(nickname, user);
                    }
                    catch (Exception e) {
                        logger.error(e);
                    }
                    finally {
                        userMap.unlock(nickname);
                    }
                } else {
                    UserDaoImpl userDao = new UserDaoImpl();
                    UserModel model = userDao.getUserByNickName(nickname);
                    if (money > 0L) {
                        List<Integer> vpLst2 = VippointUtils.calculateVP(client, model.getNickname(), (long)model.getMoneyVP() + Math.abs(money), true);
                        vp = vpLst2.get(0);
                        moneyVPs = vpLst2.get(1);
                        vpSave = vp;
                    } else {
                        List<Integer> vpLst2 = VippointUtils.calculateVPAgent(model.getMoneyVP(), (int)money, model.getVippoint(), model.getVippointSave());
                        vp = vpLst2.get(0);
                        vpSave = vpLst2.get(1);
                        moneyVPs = vpLst2.get(2);
                    }
                }
                MoneyInGameDaoImpl mndao = new MoneyInGameDaoImpl();
                mndao.updateVippointAgent(nickname, vp, vpSave, moneyVPs);
                return true;
            }
        }
        catch (Exception e2) {
            logger.debug(e2);
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean resetEvent() {
        boolean res = false;
        AlertServiceImpl alertSer = new AlertServiceImpl();
        try {
            VippointDaoImpl dao = new VippointDaoImpl();
            res = dao.resetEvent();
            if (!res) {
                alertSer.sendSMS2One("0986354389", "Reset Vippoint Event Mysql Error", false);
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserModel> userMap = client.getMap("users");
            for (Map.Entry entry : userMap.entrySet()) {
                String nickname = (String)entry.getKey();
                try {
                    userMap.lock(nickname);
                    UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                    user.setAddVPTime((Date)null);
                    user.setSubVPTime((Date)null);
                    user.setAddVPVinTime((Date)null);
                    user.setVpEvent(0);
                    user.setVpEventReal(0);
                    user.setVpAdd(0);
                    user.setVpSub(0);
                    user.setNumAdd(0);
                    user.setNumSub(0);
                    user.setPlace(0);
                    user.setPlaceMax(0);
                    userMap.put(nickname, user);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    alertSer.sendSMS2One("0986354389", "Reset Vippoint Event Error, nickname: " + nickname + ", ex: " + e.getMessage(), false);
                }
                finally {
                    userMap.unlock(nickname);
                }
            }
            alertSer.sendSMS2One("0986354389", "Finish Reset Vippoint Event", false);
            res = true;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            alertSer.sendSMS2One("0986354389", "Reset Vippoint Event Fail: " + e2.getMessage(), false);
        }
        return res;
    }
}

