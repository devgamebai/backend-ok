/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.gamebase.mission;

import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import com.gamebase.entities.UserMissionRule;
import com.gamebase.mission.MissionProcess;
import com.gamebase.mission.exception.MissionExpiredException;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.statics.Consts;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public abstract class BaseMissionProcess
implements MissionProcess {
    protected final EventMissionService eventMissionService;
    protected final Mission mission;
    protected final UserMissionService userMissionService;
    protected Date expiredAt = null;
    private static final Logger logger = Logger.getLogger((String)"base_game");

    public BaseMissionProcess(EventMissionService eventMissionService, Mission mission, UserMissionService userMissionService) {
        this.eventMissionService = eventMissionService;
        this.mission = mission;
        this.userMissionService = userMissionService;
    }

    @Override
    public String getMissionId() {
        return String.format("%s_%s", this.getMissionType(), this.mission.getId());
    }

    protected void checkExpired() throws Exception {
        EventMission eventMission = this.eventMissionService.getEventMissionById(this.mission.getEvent_id());
        this.expiredAt = eventMission.getExpiredAt();
        if (this.expiredAt.before(new Date())) {
            throw new MissionExpiredException(this.mission.getId(), "Mission expired");
        }
    }

    protected UserMission nextProcessMission(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        this.checkExpired();
        return this.checkStatus(logMoneyUserMessage);
    }

    protected UserMission nextProcessMissionWithGame(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        if (!this.checkGame(logMoneyUserMessage)) {
            return null;
        }
        return this.nextProcessMission(logMoneyUserMessage);
    }

    protected boolean checkGame(LogMoneyUserMessage logMoneyUserMessage) {
        Games game = Games.findGameById(this.mission.getGame_id());
        if (game == null) {
            return false;
        }
        return game.getName().contains(logMoneyUserMessage.getActionName());
    }

    protected boolean checkGameAction(LogMoneyUserMessage logMoneyUserMessage) {
        Games game = Games.findGameByName(logMoneyUserMessage.getActionName());
        return game != null;
    }

    protected boolean checkActionName(LogMoneyUserMessage logMoneyUserMessage, List<String> list) {
        for (String gameName : list) {
            if (!logMoneyUserMessage.getActionName().contains(gameName)) continue;
            return true;
        }
        return false;
    }

    protected UserMission checkStatus(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        UserMission userMission = this.userMissionService.getMission(logMoneyUserMessage.getNickname(), this.mission.getId());
        if (userMission == null || userMission.getStatus() == MissionStatus.COMPLETE || userMission.getStatus() == MissionStatus.REWARD_IN_PROGRESS || userMission.getStatus() == MissionStatus.FINISH || userMission.getStatus() == MissionStatus.NONE) {
            return null;
        }
        return userMission;
    }

    protected void updateMission(UserMission userMission) throws Exception {
        if (userMission.getWork() >= this.mission.getPoint()) {
            this.userMissionService.setStatusMission(userMission.getNickname(), this.mission.getId(), MissionStatus.COMPLETE);
            return;
        }
        long work = userMission.getWork();
        long point = userMission.getPoint();
        double progressLong = (double)work / (double)point * 100.0;
        userMission.setProgress((int)progressLong);
        this.userMissionService.updateMission(userMission);
    }

    protected boolean baseRewardMission(String nickname, long money) throws Exception {
        UserServiceImpl userService = new UserServiceImpl();
        BaseResponseModel res = userService.updateMoneyFromAdmin(nickname, money, "vin", "MISSION", "MISSION", "Ti\u1ec1n th\u01b0\u1edfng ho\u00e0n th\u00e0nh nhi\u1ec7m v\u1ee5: " + this.mission.getName());
        if (res.isSuccess()) {
            this.userMissionService.setStatusMission(nickname, this.mission.getId(), MissionStatus.FINISH);
        }
        return res.isSuccess();
    }

    protected boolean checkDeposit(LogMoneyUserMessage logMoneyUserMessage) {
        return this.checkActionName(logMoneyUserMessage, Consts.NAP_VIN);
    }

    protected void updateMissionComplex(UserMission userMission) throws Exception {
        boolean isComplete = true;
        for (UserMissionRule userMissionRule : userMission.getRules().values()) {
            if (userMissionRule.isCompleted()) continue;
            isComplete = false;
            break;
        }
        if (isComplete) {
            this.userMissionService.setStatusMission(userMission.getNickname(), this.mission.getId(), MissionStatus.COMPLETE);
            userMission.setStatus(MissionStatus.COMPLETE);
        } else {
            double progressLong = 0.0;
            for (UserMissionRule userMissionRule : userMission.getRules().values()) {
                progressLong += (double)userMissionRule.getProgress();
            }
            userMission.setProgress((int)(progressLong /= (double)userMission.getRules().size()));
        }
        this.userMissionService.updateMission(userMission);
    }
}

