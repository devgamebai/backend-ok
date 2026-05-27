/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.userMission.UpdateUserMissionMessage
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.userMission.processor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.userMission.UpdateUserMissionMessage;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import com.vinplay.vbee.dao.impl.ExceptionDaoImpl;
import com.vinplay.vbee.dao.impl.UserDaoImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class UpdateUserMissionProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        byte[] body = (byte[])param.get();
        UpdateUserMissionMessage message = (UpdateUserMissionMessage)BaseMessage.fromBytes((byte[])body);
        String cacheName = "";
        UserDaoImpl dao = new UserDaoImpl();
        int matchWin = 0;
        try {
            UserMissionCacheModel user;
            block21 : {
                cacheName = message.getMoneyType().equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMissionMap = client.getMap(cacheName);
                long processId = 0L;
                user = new UserMissionCacheModel();
                if (userMissionMap.containsKey((Object)message.getNickname())) {
                    try {
                        userMissionMap.lock((Object)message.getNickname());
                        user = (UserMissionCacheModel)userMissionMap.get((Object)message.getNickname());
                        processId = user.getLastMessageId();
                        if (Long.parseLong(message.getId()) < processId) {
                            logger.error((Object)("message kh\u00f4ng x\u1eed l\u00fd: " + message.getId()));
                            ExceptionDaoImpl ex = new ExceptionDaoImpl();
                            ex.insertLogExceptionDB(message.getMoneyType(), message.toJson(), "Message kh\u00f4ng x\u1eed l\u00fd, current process ID: " + processId);
                            return false;
                        }
                        List<MissionObj>  listMissionObj = user.getListMission();
                        ArrayList<MissionObj> listMissionObjResponse = new ArrayList<MissionObj>();
                        for (MissionObj missionObj : listMissionObj) {
                            if (missionObj.getMisNa().equalsIgnoreCase(message.getActionName()) && missionObj.getMisWin() < missionObj.getMisMax()) {
                                matchWin = missionObj.getMisWin() + 1;
                                missionObj.setMisWin(matchWin);
                            }
                            listMissionObjResponse.add(missionObj);
                        }
                        user.setListMission(listMissionObjResponse);
                        user.setLastActive(new Date());
                        user.setLastMessageId(Long.parseLong(message.getId()));
                        userMissionMap.put((Object)message.getNickname(), (Object)user);
                    }
                    catch (Exception e) {
                        logger.error((Object)e);
                        e.printStackTrace();
                        break block21;
                    }
                    try {
                        userMissionMap.unlock((Object)message.getNickname());
                    }
                    catch (Exception e) {}
                } else {
                    user = dao.getUserMission(message.getNickname(), message.getMoneyType());
                    if (user == null || user.getListMission().size() == 0) {
                        int i;
                        String matchMax = "";
                        ArrayList<String> listBonus = new ArrayList<String>();
                        if (message.getMoneyType().equals("vin")) {
                            matchMax = GameCommon.getValueStr((String)"MATCH_MAX_VIN");
                            for (i = 0; i <= 20; ++i) {
                                listBonus.add(GameCommon.getValueStr((String)("BONUS_VIN_" + i)));
                            }
                        } else {
                            matchMax = GameCommon.getValueStr((String)"MATCH_MAX_XU");
                            for (i = 0; i <= 20; ++i) {
                                listBonus.add(GameCommon.getValueStr((String)("BONUS_XU_" + i)));
                            }
                        }
                        user = dao.initUserMission(message.getNickname(), message.getMoneyType(), matchMax, listBonus);
                    }
                    if (user == null) {
                        return false;
                    }
                    try {
                        List<MissionObj> listMissionObj = user.getListMission();
                        ArrayList<MissionObj> listMissionObjResponse = new ArrayList<MissionObj>();
                        for (MissionObj missionObj : listMissionObj) {
                            if (missionObj.getMisNa().equals(message.getActionName()) && missionObj.getMisWin() < missionObj.getMisMax()) {
                                matchWin = missionObj.getMisWin() + 1;
                                missionObj.setMisWin(matchWin);
                            }
                            listMissionObjResponse.add(missionObj);
                        }
                        user.setListMission(listMissionObjResponse);
                        user.setLastActive(new Date());
                        user.setLastMessageId(Long.parseLong(message.getId()));
                        userMissionMap.put((Object)message.getNickname(), (Object)user);
                    }
                    catch (Exception e) {
                        logger.error((Object)e);
                        e.printStackTrace();
                    }
                }
            }
            dao.updateUserMission(user.getNickName(), message.getActionName(), message.getMoneyType(), matchWin);
        }
        catch (Exception e2) {
            e2.printStackTrace();
            logger.error((Object)e2);
        }
        return false;
    }
}

