/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.MissionName
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.userMission.CompleteMissionObj
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.MissionResponse
 *  com.vinplay.vbee.common.models.userMission.NumberCompleteMissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 *  com.vinplay.vbee.common.models.userMission.UserMissionResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.UserMissionService;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.MissionName;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.userMission.CompleteMissionObj;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.MissionResponse;
import com.vinplay.vbee.common.models.userMission.NumberCompleteMissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.userMission.UserMissionResponse;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserMissionServiceImpl
implements UserMissionService {
    @Override
    public UserMissionResponse getUserMission(String nickName) throws Exception {
        UserMissionResponse response = new UserMissionResponse();
        List<MissionObj> listMissionVin = this.getUserMission(nickName, "vin", GameCommon.getValueInt("MAX_LEVEL_MISSION"));
        if (listMissionVin == null || listMissionVin.size() == 0) {
            ArrayList<String> bonusVin = new ArrayList<String>();
            for (int i = 0; i <= 20; ++i) {
                bonusVin.add(GameCommon.getValueStr("BONUS_VIN_" + i));
            }
            listMissionVin = this.initUserMission(nickName, "vin", GameCommon.getValueStr("MATCH_MAX_VIN"), bonusVin);
            if (listMissionVin == null) {
                response.setError("1052");
                return response;
            }
        }
        ArrayList<MissionResponse> lMisVin = new ArrayList<MissionResponse>();
        for (MissionObj obj : listMissionVin) {
            MissionName enumMissionName = MissionName.getMissionByName((String)obj.getMisNa());
            if (enumMissionName == null) {
                response.setError("1049");
                return response;
            }
            if (enumMissionName.getShow() != 0 || enumMissionName.getType() == 2) continue;
            MissionResponse missionResponse = new MissionResponse();
            missionResponse.setMisNa(obj.getMisNa());
            missionResponse.setMisWin(obj.getMisWin());
            missionResponse.setMisMax(obj.getMisMax());
            if (obj.getMisWin() == obj.getMisMax() && obj.getMisLev() == obj.getRecReLev() + 1) {
                missionResponse.setRecReLev(0);
            } else {
                missionResponse.setRecReLev(1);
            }
            if (obj.getMisWin() == obj.getMisMax() && obj.getMisLev() == obj.getRecReLev()) {
                missionResponse.setCompAllLev(0);
            } else {
                missionResponse.setCompAllLev(1);
            }
            String moneyBonus = GameCommon.getValueStr("BONUS_VIN_" + String.valueOf(enumMissionName.getId()));
            String[] moneyBonusArr = moneyBonus.split(",");
            missionResponse.setMoBo(Long.parseLong(moneyBonusArr[obj.getMisLev() - 1]));
            lMisVin.add(missionResponse);
        }
        List<MissionObj> listMissionXu = this.getUserMission(nickName, "xu", GameCommon.getValueInt("MAX_LEVEL_MISSION"));
        if (listMissionXu == null || listMissionXu.size() == 0) {
            ArrayList<String> bonusXu = new ArrayList<String>();
            for (int j = 0; j <= 20; ++j) {
                bonusXu.add(GameCommon.getValueStr("BONUS_XU_" + j));
            }
            listMissionXu = this.initUserMission(nickName, "xu", GameCommon.getValueStr("MATCH_MAX_XU"), bonusXu);
            if (listMissionXu == null) {
                response.setError("1052");
                return response;
            }
        }
        ArrayList<MissionResponse> lMisXu = new ArrayList<MissionResponse>();
        for (MissionObj obj2 : listMissionXu) {
            MissionName enumMissionName2 = MissionName.getMissionByName((String)obj2.getMisNa());
            if (enumMissionName2 == null) {
                response.setError("1049");
                return response;
            }
            if (enumMissionName2.getShow() != 0 || enumMissionName2.getType() == 2) continue;
            MissionResponse missionResponse2 = new MissionResponse();
            missionResponse2.setMisNa(obj2.getMisNa());
            missionResponse2.setMisWin(obj2.getMisWin());
            missionResponse2.setMisMax(obj2.getMisMax());
            if (obj2.getMisWin() == obj2.getMisMax() && obj2.getMisLev() == obj2.getRecReLev() + 1) {
                missionResponse2.setRecReLev(0);
            } else {
                missionResponse2.setRecReLev(1);
            }
            if (obj2.getMisWin() == obj2.getMisMax() && obj2.getMisLev() == obj2.getRecReLev()) {
                missionResponse2.setCompAllLev(0);
            } else {
                missionResponse2.setCompAllLev(1);
            }
            String moneyBonus2 = GameCommon.getValueStr("BONUS_XU_" + String.valueOf(enumMissionName2.getId()));
            String[] moneyBonusArr2 = moneyBonus2.split(",");
            missionResponse2.setMoBo(Long.parseLong(moneyBonusArr2[obj2.getMisLev() - 1]));
            lMisXu.add(missionResponse2);
        }
        response.setnN(nickName);
        response.setError("0");
        response.setlMisVin(lMisVin);
        response.setlMisXu(lMisXu);
        response.setDepTaiXiuVin(GameCommon.getValueInt("MIN_TAI_XIU_VIN"));
        response.setDepTaiXiuXu(GameCommon.getValueInt("MIN_TAI_XIU_XU"));
        return response;
    }

    private List<MissionObj> getUserMission(String nickName, String moneyType, int maxLevel) throws Exception {
        try {
            String cacheName = "";
            cacheName = moneyType.equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserMissionCacheModel> userMissionMap = client.getMap(cacheName);
            UserMissionCacheModel user = new UserMissionCacheModel();
            List<MissionObj> listMissionResponse = new ArrayList();
            if (userMissionMap.containsKey(nickName)) {
                try {
                    userMissionMap.lock(nickName);
                    user = (UserMissionCacheModel)userMissionMap.get(nickName);
                    listMissionResponse = user.getListMission();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                finally {
                    userMissionMap.unlock(nickName);
                }
            }
            try {
                UserDaoImpl dao = new UserDaoImpl();
                user = dao.getListMissionByNickName(nickName, moneyType, maxLevel);
                listMissionResponse = user.getListMission();
                user.setLastMessageId(0L);
                user.setLastActive(new Date());
                userMissionMap.put(nickName, user);
            }
            catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            return listMissionResponse;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            throw e2;
        }
    }

    private List<MissionObj> initUserMission(String nickName, String moneyType, String matchMax, List<String> bonusVin) throws Exception {
        try {
            String cacheName = "";
            cacheName = moneyType.equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap userMissionMap = client.getMap(cacheName);
            UserMissionCacheModel user = new UserMissionCacheModel();
            String[] matchMaxArr = matchMax.split(",");
            ArrayList<MissionObj> listMission = new ArrayList<MissionObj>();
            UserDaoImpl dao = new UserDaoImpl();
            try {
                UserModel userDao = dao.getUserByNickName(nickName);
                if (userDao == null) {
                    return null;
                }
                for (int i = 0; i < bonusVin.size(); ++i) {
                    MissionObj obj = new MissionObj(MissionName.getMissionById((int)i).getName(), 1, 0, Integer.parseInt(matchMaxArr[0]), false, false, 0);
                    listMission.add(obj);
                    dao.insertUserMission(moneyType, obj, userDao);
                }
                user.setLastActive(new Date());
                user.setLastMessageId(Long.parseLong(VinPlayUtils.genMessageId()));
                user.setListMission(listMission);
                user.setNickName(nickName);
                user.setUserId(userDao.getId());
                user.setUserName(userDao.getUsername());
                userMissionMap.put(nickName, user);
            }
            catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            return listMission;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            throw e2;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private CompleteMissionObj updateMissionCompleteTH1(String nickName, String gameName, String moneyType, int maxLevel, String matchMax, String moneyBonus) throws Exception {
        CompleteMissionObj response = new CompleteMissionObj(false, 0L, 0L, "1001");
        try {
            String cacheName = "";
            cacheName = moneyType.equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap userMissionMap = client.getMap(cacheName);
            IMap<String, UserModel> userMap = client.getMap("users");
            try {
                UserDaoImpl dao = new UserDaoImpl();
                userMissionMap.lock(nickName);
                 userMap.lock(nickName);
                UserMissionCacheModel userMission = (UserMissionCacheModel)userMissionMap.get(nickName);
                UserCacheModel user = (UserCacheModel)userMap.get(nickName);
                List<MissionObj> listMission = userMission.getListMission();
                ArrayList<MissionObj> listMissionReturn = new ArrayList<MissionObj>();
                String[] moneyBonusArr = moneyBonus.split(",");
                long moneyExchange = 0L;
                String des = "";
                int levelReceivedReward = 0;
                for (MissionObj obj : listMission) {
                    if (obj.getMisNa().equals(gameName)) {
                        CompleteMissionObj completeMissionObj;
                        if (obj.getMisLev() == obj.getRecReLev()) {
                            response.setError("1048");
                            completeMissionObj = response;
                            return completeMissionObj;
                        }
                        if (obj.getMisWin() < obj.getMisMax()) {
                            response.setError("1047");
                            completeMissionObj = response;
                            return completeMissionObj;
                        }
                        moneyExchange = Long.parseLong(moneyBonusArr[obj.getMisLev() - 1]);
                        des = "Th\u00c6\u00b0\u00e1\u00bb\u0178ng Nhi\u00e1\u00bb\u2021m V\u00e1\u00bb\u00a5 " + gameName + " - Level " + obj.getMisLev() + " - " + moneyType;
                        levelReceivedReward = obj.getMisLev();
                        if (obj.getMisLev() == maxLevel) {
                            obj.setCompAllLev(true);
                            obj.setCompMis(true);
                            obj.setRecReLev(obj.getMisLev());
                        } else {
                            String[] matchMaxArr = matchMax.split(",");
                            obj.setMisMax(Integer.parseInt(matchMaxArr[obj.getMisLev()]));
                            obj.setMisWin(0);
                            obj.setRecReLev(obj.getMisLev());
                            obj.setMisLev(obj.getMisLev() + 1);
                            obj.setCompAllLev(false);
                            obj.setCompMis(false);
                        }
                        dao.updateUserMission(moneyType, nickName, obj);
                    }
                    listMissionReturn.add(obj);
                }
                userMission.setListMission(listMissionReturn);
                userMission.setLastActive(new Date());
                userMissionMap.put(nickName, userMission);
                long moneyUser = user.getVin();
                long currentMoney = user.getVinTotal();
                user.setVin(moneyUser += moneyExchange);
                user.setVinTotal(currentMoney += moneyExchange);
                 userMap.put(nickName, user);
                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), userMission.getUserId(), userMission.getNickName(), "NhiemVu", moneyUser, currentMoney, moneyExchange, moneyType, 0L, 0, 0);
                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(userMission.getUserId(), userMission.getNickName(), "NhiemVu", "Th\u00c6\u00b0\u00e1\u00bb\u0178ng Nhi\u00e1\u00bb\u2021m V\u00e1\u00bb\u00a5", currentMoney, moneyExchange, "vin", des, 0L, false, user.isBot());
                messageLog.setReferralCode(user.getReferralCode());
                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                LogReceivedRewardMissionMessage messageLogReceivedReward = new LogReceivedRewardMissionMessage(userMission.getUserId(), userMission.getUserName(), userMission.getNickName(), gameName, levelReceivedReward, moneyExchange, moneyUser, moneyType);
                // P1 cleanup: queue_user_mission retired (no consumer); LogUpdateUserMissionProcessor + ReceivedRewardMissionProcessor deleted.
                response.setUpdateSuccess(true);
                response.setError("0");
                response.setMoneyBonus(moneyExchange);
                response.setMoneyUser(moneyUser);
                return response;
            }
            catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            finally {
                userMissionMap.unlock(nickName);
                 userMap.unlock(nickName);
            }
        }
        catch (Exception e2) {
            e2.printStackTrace();
            throw e2;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private CompleteMissionObj updateMissionCompleteTH2(String nickName, String gameName, String moneyType, int maxLevel, String matchMax, String moneyBonus) throws Exception {
        CompleteMissionObj response = new CompleteMissionObj(false, 0L, 0L, "1001");
        try {
            String cacheName = "";
            cacheName = moneyType.equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap userMissionMap = client.getMap(cacheName);
            IMap<String, UserModel> userMap = client.getMap("users");
            try {
                 userMap.lock(nickName);
                UserCacheModel user = (UserCacheModel)userMap.get(nickName);
                UserDaoImpl dao = new UserDaoImpl();
                UserMissionCacheModel userMission = dao.getListMissionByNickName(nickName, moneyType, maxLevel);
                List<MissionObj> listMission = userMission.getListMission();
                ArrayList<MissionObj> listMissionReturn = new ArrayList<MissionObj>();
                String[] moneyBonusArr = moneyBonus.split(",");
                long moneyExchange = 0L;
                String des = "";
                int levelReceivedReward = 0;
                for (MissionObj obj : listMission) {
                    if (obj.getMisNa().equals(gameName)) {
                        CompleteMissionObj completeMissionObj;
                        if (obj.getMisLev() == obj.getRecReLev()) {
                            response.setError("1048");
                            completeMissionObj = response;
                            return completeMissionObj;
                        }
                        if (obj.getMisWin() < obj.getMisMax()) {
                            response.setError("1047");
                            completeMissionObj = response;
                            return completeMissionObj;
                        }
                        moneyExchange = Long.parseLong(moneyBonusArr[obj.getMisLev() - 1]);
                        des = "Th\u00c6\u00b0\u00e1\u00bb\u0178ng Nhi\u00e1\u00bb\u2021m V\u00e1\u00bb\u00a5 " + gameName + " - Level " + obj.getMisLev() + " - " + moneyType;
                        levelReceivedReward = obj.getMisLev();
                        if (obj.getMisLev() == maxLevel) {
                            obj.setCompAllLev(true);
                            obj.setCompMis(true);
                            obj.setRecReLev(obj.getMisLev());
                        } else {
                            String[] matchMaxArr = matchMax.split(",");
                            obj.setMisMax(Integer.parseInt(matchMaxArr[obj.getMisLev()]));
                            obj.setMisWin(0);
                            obj.setRecReLev(obj.getMisLev());
                            obj.setMisLev(obj.getMisLev() + 1);
                            obj.setCompAllLev(false);
                            obj.setCompMis(false);
                        }
                        dao.updateUserMission(moneyType, nickName, obj);
                    }
                    listMissionReturn.add(obj);
                }
                userMission.setListMission(listMissionReturn);
                userMission.setLastActive(new Date());
                userMissionMap.put(nickName, userMission);
                long moneyUser = user.getVin();
                long currentMoney = user.getVinTotal();
                user.setVin(moneyUser += moneyExchange);
                user.setVinTotal(currentMoney += moneyExchange);
                 userMap.put(nickName, user);
                MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), userMission.getUserId(), userMission.getNickName(), "NhiemVu", moneyUser, currentMoney, moneyExchange, moneyType, 0L, 0, 0);
                MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage)messageMoney, (int)16);
                LogMoneyUserMessage messageLog = new LogMoneyUserMessage(userMission.getUserId(), userMission.getNickName(), "NhiemVu", "Th\u00c6\u00b0\u00e1\u00bb\u0178ng Nhi\u00e1\u00bb\u2021m V\u00e1\u00bb\u00a5", currentMoney, moneyExchange, "vin", des, 0L, false, user.isBot());
                messageLog.setReferralCode(user.getReferralCode());
                MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage)messageLog, 601);
                LogReceivedRewardMissionMessage messageLogReceivedReward = new LogReceivedRewardMissionMessage(userMission.getUserId(), userMission.getUserName(), userMission.getNickName(), gameName, levelReceivedReward, moneyExchange, moneyUser, moneyType);
                // P1 cleanup: queue_user_mission retired (no consumer); LogUpdateUserMissionProcessor + ReceivedRewardMissionProcessor deleted.
                response.setUpdateSuccess(true);
                response.setError("0");
                response.setMoneyBonus(moneyExchange);
                response.setMoneyUser(moneyUser);
                return response;
            }
            catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            finally {
                 userMap.unlock(nickName);
            }
        }
        catch (Exception e2) {
            e2.printStackTrace();
            throw e2;
        }
    }

    @Override
    public CompleteMissionObj completeMission(String nickName, String gameName, String moneyType) throws Exception {
        CompleteMissionObj response = new CompleteMissionObj(false, 0L, 0L, "1001");
        String moneyBonus = "";
        MissionName enumMissionName = MissionName.getMissionByName((String)gameName);
        if (enumMissionName == null) {
            response.setError("1049");
            return response;
        }
        moneyBonus = moneyType.equals("vin") ? GameCommon.getValueStr("BONUS_VIN_" + String.valueOf(enumMissionName.getId())) : GameCommon.getValueStr("BONUS_XU_" + String.valueOf(enumMissionName.getId()));
        String matchMax = "";
        matchMax = moneyType.equals("vin") ? GameCommon.getValueStr("MATCH_MAX_VIN") : GameCommon.getValueStr("MATCH_MAX_XU");
        String cacheName = "";
        cacheName = moneyType.equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMissionMap = client.getMap(cacheName);
        IMap<String, UserModel> userMap = client.getMap("users");
        if (!userMap.containsKey(nickName)) {
            response.setError("1051");
            return response;
        }
        response = userMissionMap.containsKey(nickName) ? this.updateMissionCompleteTH1(nickName, gameName, moneyType, GameCommon.getValueInt("MAX_LEVEL_MISSION"), matchMax, moneyBonus) : this.updateMissionCompleteTH2(nickName, gameName, moneyType, GameCommon.getValueInt("MAX_LEVEL_MISSION"), matchMax, moneyBonus);
        return response;
    }

    @Override
    public NumberCompleteMissionObj getNumberCompleteMission(String nickName) throws Exception {
        return new NumberCompleteMissionObj(this.getNumCompMiss("vin", nickName), this.getNumCompMiss("xu", nickName));
    }

    private int getNumCompMiss(String moneyType, String nickName) throws Exception {
        int numberComplete;
        block13 : {
            numberComplete = 0;
            try {
                String cacheName = "";
                if (moneyType.equals("vin")) {
                    cacheName = "cacheUserMissionVin";
                } else if (moneyType.equals("xu")) {
                    cacheName = "cacheUserMissionXu";
                }
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMission = client.getMap(cacheName);
                if (userMission.containsKey(nickName)) {
                    try {
                        userMission.lock(nickName);
                        UserMissionCacheModel user = (UserMissionCacheModel)userMission.get(nickName);
                        List<MissionObj> listMission = user.getListMission();
                        for (MissionObj obj : listMission) {
                            if (obj.getMisWin() != obj.getMisMax() || obj.getMisLev() != obj.getRecReLev() + 1) continue;
                            ++numberComplete;
                        }
                        break block13;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                    finally {
                        userMission.unlock(nickName);
                    }
                }
                UserDaoImpl dao = new UserDaoImpl();
                UserMissionCacheModel user2 = dao.getListMissionByNickName(nickName, moneyType, GameCommon.getValueInt("MAX_LEVEL_MISSION"));
                List<MissionObj> listMission2 = user2.getListMission();
                for (MissionObj obj2 : listMission2) {
                    if (obj2.getMisWin() != obj2.getMisMax() || obj2.getMisLev() != obj2.getRecReLev() + 1) continue;
                    ++numberComplete;
                }
            }
            catch (Exception e2) {
                e2.printStackTrace();
                throw e2;
            }
        }
        return numberComplete;
    }
}

