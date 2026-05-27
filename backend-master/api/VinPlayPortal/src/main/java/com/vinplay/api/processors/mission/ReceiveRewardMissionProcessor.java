/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gamebase.entities.MissionStatus
 *  com.gamebase.entities.UserMission
 *  com.gamebase.messages.userMission.RewardUserMissionMessage
 *  com.gamebase.service.impl.UserMissionServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.BaseResponse
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.mission;

import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import com.gamebase.messages.userMission.RewardUserMissionMessage;
import com.gamebase.service.impl.UserMissionServiceImpl;
import com.vinplay.api.processors.common.AuthProcessor;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class ReceiveRewardMissionProcessor
extends AuthProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String missionId = request.getParameter("missionId");
        if (StringUtils.isEmpty((CharSequence)missionId)) {
            return BaseResponse.error((String)"5", (String)"Missing missionId");
        }
        UserModel userModel = this.getUser(param);
        if (userModel == null) {
            return notAuth;
        }
        if (!userModel.isVerifyMobile()) {
            return BaseResponse.error((String)"2", (String)"B\u1ea1n ch\u01b0a x\u00e1c th\u1ef1c s\u1ed1 \u0111i\u1ec7n tho\u1ea1i");
        }
        try {
            UserMissionServiceImpl userMissionService = new UserMissionServiceImpl();
            UserMission mission = userMissionService.getMission(userModel.getNickname(), missionId);
            if (mission == null) {
                return BaseResponse.error((String)"15", (String)"Nhi\u1ec7m v\u1ee5 kh\u00f4ng t\u1ed3n t\u1ea1i");
            }
            if (mission.isReward()) {
                return BaseResponse.error((String)"15", (String)"B\u1ea1n \u0111\u00e3 nh\u1eadn th\u01b0\u1edfng nhi\u1ec7m v\u1ee5 n\u00e0y");
            }
            if (!mission.isComplete()) {
                return BaseResponse.error((String)"15", (String)"Nhi\u1ec7m v\u1ee5 ch\u01b0a ho\u00e0n th\u00e0nh");
            }
            userMissionService.setStatusMission(userModel.getNickname(), missionId, MissionStatus.REWARD_IN_PROGRESS);
            RewardUserMissionMessage msg = new RewardUserMissionMessage();
            msg.setNickname(mission.getNickname());
            msg.setMissionId(mission.getMissionId());
            // P1 cleanup: queue_user_mission retired (no consumer); ReceivedRewardMissionProcessor deleted.
            return BaseResponse.success((String)"0", (String)"\u0110ang nh\u1eadn th\u01b0\u1edfng nhi\u1ec7m v\u1ee5", null);
        }
        catch (Exception e) {
            logger.debug(e);
            return BaseResponse.error((String)"99", (String)"Server Error!");
        }
    }
}

