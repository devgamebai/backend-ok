/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.gamebase.mission;

import com.gamebase.entities.Mission;
import com.gamebase.entities.UserMission;
import com.gamebase.mission.BaseMissionProcess;
import com.gamebase.mission.MissionProcess;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import org.apache.log4j.Logger;

public class MissionDeposit
extends BaseMissionProcess
implements MissionProcess {
    private static final Logger logger = Logger.getLogger((String)"base_game");

    public MissionDeposit(EventMissionService eventMissionService, Mission mission, UserMissionService userMissionService) {
        super(eventMissionService, mission, userMissionService);
    }

    @Override
    public String getMissionType() {
        return "deposit";
    }

    @Override
    public void checkMission(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        this.checkExpired();
        if (!this.checkDeposit(logMoneyUserMessage)) {
            return;
        }
        UserMission userMission = this.nextProcessMission(logMoneyUserMessage);
        if (userMission == null) {
            return;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money > 0L) {
            return;
        }
        long work = userMission.getWork() + logMoneyUserMessage.getMoneyExchange();
        userMission.setWork(work);
        this.updateMission(userMission);
    }

    @Override
    public boolean rewardMission(String nickname) throws Exception {
        double money = this.mission.getReward();
        return this.baseRewardMission(nickname, (int)money);
    }

    @Override
    public void scanMission(String username) {
    }
}

