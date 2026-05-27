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

public class MissionBet
extends BaseMissionProcess
implements MissionProcess {
    private static final Logger logger = Logger.getLogger((String)"base_game");

    public MissionBet(EventMissionService eventMissionService, Mission mission, UserMissionService userMissionService) {
        super(eventMissionService, mission, userMissionService);
    }

    @Override
    public String getMissionType() {
        return "bet";
    }

    @Override
    public void checkMission(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        UserMission userMission = this.nextProcessMissionWithGame(logMoneyUserMessage);
        if (userMission == null) {
            return;
        }
        if (!this.checkGame(logMoneyUserMessage)) {
            return;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money > 0L) {
            return;
        }
        money = Math.abs(money);
        long work = userMission.getWork() + money;
        userMission.setWork(work);
        this.updateMission(userMission);
    }

    @Override
    public boolean rewardMission(String nickname) throws Exception {
        long money = this.mission.getReward();
        return this.baseRewardMission(nickname, money);
    }

    @Override
    public void scanMission(String username) {
    }
}

