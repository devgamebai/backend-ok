/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.service.impl.UserMissionServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 *  com.vinplay.vbee.common.response.userMission.UpdateMatchWinResponse
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.userMission;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.impl.UserMissionServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import com.vinplay.vbee.common.response.userMission.UpdateMatchWinResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class UpdateMatchWinMissionProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        UpdateMatchWinResponse response = new UpdateMatchWinResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String gameName = request.getParameter("gn");
        String moneyType = request.getParameter("mt");
        String matchWin = request.getParameter("mw");
        if (nickName == null || nickName.isEmpty() || gameName == null || gameName.isEmpty() || moneyType == null || moneyType.isEmpty() || matchWin == null || matchWin.isEmpty()) {
            return "MISSING INPUT PARAMETER";
        }
        try {
            block13 : {
                if (!moneyType.equalsIgnoreCase("vin") && !moneyType.equalsIgnoreCase("xu")) {
                    return "Money type khong dung";
                }
                String cacheName = "";
                cacheName = moneyType.equalsIgnoreCase("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMissionMap = client.getMap(cacheName);
                UserMissionCacheModel user = new UserMissionCacheModel();
                if (!userMissionMap.containsKey((Object)nickName)) {
                    return "Nick name khong co tren cache";
                }
                try {
                    userMissionMap.lock((Object)nickName);
                    user = (UserMissionCacheModel)userMissionMap.get((Object)nickName);
                    List<MissionObj> listMissionObj = user.getListMission();
                    ArrayList<MissionObj> listMissionObjResponse = new ArrayList<MissionObj>();
                    for (MissionObj missionObj : listMissionObj) {
                        if (missionObj.getMisNa().equalsIgnoreCase(gameName)) {
                            if (missionObj.getMisWin() + Integer.parseInt(matchWin) > missionObj.getMisMax()) {
                                return "So tran thang muon tang them lon hon so tran max";
                            }
                            if (missionObj.getMisWin() + Integer.parseInt(matchWin) < 0) {
                                return "So tran thang muon giam di vuot qua so tran thang hien tai";
                            }
                            missionObj.setMisWin(missionObj.getMisWin() + Integer.parseInt(matchWin));
                        }
                        listMissionObjResponse.add(missionObj);
                    }
                    user.setListMission(listMissionObjResponse);
                    user.setLastActive(new Date());
                    user.setLastMessageId(Long.parseLong(VinPlayUtils.genMessageId()));
                    userMissionMap.put((Object)nickName, (Object)user);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    break block13;
                }
                try {
                    userMissionMap.unlock((Object)nickName);
                }
                catch (Exception e) {
                    // empty catch block
                }
            }
            UserMissionServiceImpl service = new UserMissionServiceImpl();
            service.updateUserMission(nickName, gameName, moneyType, Integer.parseInt(matchWin));
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
        return response.toJson();
    }
}

