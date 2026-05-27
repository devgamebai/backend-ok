/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.userMission;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class ResetUserMissionTask
extends TimerTask {
    private static final Logger logger = Logger.getLogger((String)"backend");

    @Override
    public void run() {
        try {
            this.resetCache("vin");
            this.resetCache("xu");
            UserDaoImpl dao = new UserDaoImpl();
            dao.resetUserMission();
        }
        catch (Exception e) {
            logger.debug((Object)e);
            e.printStackTrace();
        }
    }

    private void resetCache(String moneyType) {
        try {
            String cacheName = "";
            String matchMax = "";
            if (moneyType.equals("vin")) {
                cacheName = "cacheUserMissionVin";
                matchMax = GameCommon.getValueStr((String)"MATCH_MAX_VIN");
            } else if (moneyType.equals("xu")) {
                cacheName = "cacheUserMissionXu";
                matchMax = GameCommon.getValueStr((String)"MATCH_MAX_XU");
            }
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap<String, UserMissionCacheModel> userMissionMap = client.getMap(cacheName);
            String[] matchMaxSplit = matchMax.split(",");
            for (Map.Entry userMission : userMissionMap.entrySet()) {
                try {
                    userMissionMap.lock(userMission.getKey().toString());
                    UserMissionCacheModel model = (UserMissionCacheModel)userMission.getValue();
                    List<MissionObj> misList = model.getListMission();
                    ArrayList<MissionObj> misListRes = new ArrayList<MissionObj>();
                    for (MissionObj misObj : misList) {
                        misObj.setMisLev(1);
                        misObj.setMisWin(0);
                        misObj.setMisMax(Integer.parseInt(matchMaxSplit[0]));
                        misObj.setCompMis(false);
                        misObj.setCompAllLev(false);
                        misObj.setRecReLev(0);
                        misListRes.add(misObj);
                    }
                    model.setListMission(misListRes);
                    model.setLastActive(new Date());
                    userMissionMap.put(userMission.getKey().toString(), model);
                }
                catch (Exception e) {
                    logger.debug((Object)e);
                    e.printStackTrace();
                    continue;
                }
                try {
                    userMissionMap.unlock((String) userMission.getKey());
                }
                catch (Exception e) {}
            }
        }
        catch (Exception e2) {
            logger.debug((Object)e2);
            e2.printStackTrace();
        }
    }
}

