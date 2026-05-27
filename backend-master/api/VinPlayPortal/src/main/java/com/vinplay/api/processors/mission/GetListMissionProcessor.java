/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gamebase.entities.EventMission
 *  com.gamebase.entities.Mission
 *  com.gamebase.entities.UserMission
 *  com.gamebase.mission.ManagerMission
 *  com.gamebase.service.impl.MissionServiceImpl
 *  com.gamebase.service.impl.UserMissionServiceImpl
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.HtmlTemple
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.mission;

import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.gamebase.entities.UserMission;
import com.gamebase.mission.ManagerMission;
import com.gamebase.service.impl.MissionServiceImpl;
import com.gamebase.service.impl.UserMissionServiceImpl;
import com.hazelcast.core.IMap;
import com.vinplay.api.processors.common.AuthProcessor;
import com.vinplay.api.processors.response.UserMissionExtra;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.HtmlTemple;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class GetListMissionProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String eventIdStr = request.getParameter("event_id");
        UserModel userModel = this.getUser(param);
        if (userModel == null) {
            return notAuth;
        }
        BaseResponse baseResponse = new BaseResponse();
        try {
            List allMission;
            MissionServiceImpl missionService = new MissionServiceImpl();
            IMap eventMissionIMap = HazelcastClientFactory.getInstance().getMap("eventMissionCache");
            UserMissionServiceImpl userMissionService = new UserMissionServiceImpl();
            if (StringUtils.isEmpty((CharSequence)eventIdStr)) {
                allMission = userMissionService.getAllMission(userModel.getNickname());
            } else {
                int eventId = Integer.parseInt(eventIdStr);
                allMission = userMissionService.getListMissionByEvent(userModel.getNickname(), eventId);
            }
            Set hideMissions = ManagerMission.Instance().hideMission();
            ArrayList<UserMissionExtra> allMissionResult = new ArrayList<UserMissionExtra>();
            for (Object _um : allMission) {
                UserMission userMission = (UserMission) _um;
                if (hideMissions.contains(userMission.getMissionId())) continue;
                UserMissionExtra missionResult = new UserMissionExtra(userMission);
                Mission mission = missionService.getMission(missionResult.getMissionId());
                if (mission != null) {
                    missionResult.setMissionName(mission.getName());
                    missionResult.setMissionDescription(this.buildContent(mission));
                }
                if (eventMissionIMap.containsKey(missionResult.getEventId())) {
                    EventMission eventMission = (EventMission)eventMissionIMap.get(missionResult.getEventId());
                    missionResult.setExpiredAt(eventMission.getExpiredAt());
                }
                allMissionResult.add(missionResult);
            }
            allMissionResult.sort(Comparator.comparing(UserMission::getCreated_at));
            baseResponse.setSuccess(true);
            baseResponse.setErrorCode("0");
            baseResponse.setMessage("");
            baseResponse.setData(allMissionResult);
            baseResponse.setTotalRecords((long)allMissionResult.size());
            return baseResponse.toJson();
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            return BaseResponse.error((String)"99", (String)"Server Error!");
        }
    }

    private String buildContent(Mission mission) throws Exception {
        HtmlTemple htmlTemple = PortalUtils.loadHtmlTemple();
        String content = htmlTemple.getEvent().replace("[content]", mission.getDescription());
        return String.format("data:text/html,%s", content);
    }
}

