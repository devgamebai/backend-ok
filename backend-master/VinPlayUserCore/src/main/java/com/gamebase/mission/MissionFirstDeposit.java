/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.gamebase.mission;

import com.gamebase.dao.LogMoneyUserExtendDao;
import com.gamebase.dao.impl.LogMoneyUserExtendDaoImpl;
import com.gamebase.entities.Mission;
import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import com.gamebase.mission.BaseMissionProcess;
import com.gamebase.mission.MissionProcess;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.vinplay.dal.entities.log.LogMoneyUserNapTieuVinModel;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

public class MissionFirstDeposit
extends BaseMissionProcess
implements MissionProcess {
    private static final Logger logger = Logger.getLogger((String)"base_game");
    LogMoneyUserExtendDao logMoneyUserExtendDao = new LogMoneyUserExtendDaoImpl();
    Set<String> userDidDeposit = new HashSet<String>();

    public MissionFirstDeposit(EventMissionService eventMissionService, Mission mission, UserMissionService userMissionService) {
        super(eventMissionService, mission, userMissionService);
    }

    @Override
    public String getMissionType() {
        return "first_deposit";
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
        if (this.userDidDeposit.contains(logMoneyUserMessage.getNickname())) {
            return;
        }
        LogMoneyUserNapTieuVinModel firstUserDepositTime = this.logMoneyUserExtendDao.getFirstUserDepositTime(logMoneyUserMessage.getNickname(), logMoneyUserMessage.getCreateTime(), logMoneyUserMessage.getCreateTime(), "");
        if (firstUserDepositTime == null) {
            boolean success = this.rewardMission(logMoneyUserMessage.getNickname(), logMoneyUserMessage.getMoneyExchange());
            if (!success) {
                throw new Exception("Reward mission failed");
            }
            this.userDidDeposit.add(logMoneyUserMessage.getNickname());
            return;
        }
        long count = this.logMoneyUserExtendDao.countUserDepositTime(logMoneyUserMessage.getNickname(), logMoneyUserMessage.getCreateTime(), logMoneyUserMessage.getCreateTime(), "");
        if (count > 3L) {
            this.userDidDeposit.add(logMoneyUserMessage.getNickname());
            return;
        }
        if (firstUserDepositTime.getCurrent_money().longValue() != logMoneyUserMessage.getCurrentMoney() || firstUserDepositTime.getMoney_exchange().longValue() != logMoneyUserMessage.getMoneyExchange()) {
            this.userDidDeposit.add(logMoneyUserMessage.getNickname());
            return;
        }
        boolean success = this.rewardMission(logMoneyUserMessage.getNickname(), logMoneyUserMessage.getMoneyExchange());
        if (!success) {
            throw new Exception("Reward mission failed");
        }
        this.userDidDeposit.add(logMoneyUserMessage.getNickname());
    }

    public boolean rewardMission(String nickname, long moneyDeposit) throws Exception {
        UserServiceImpl userService = new UserServiceImpl();
        double money = (double)moneyDeposit / 100.0 * (double)this.mission.getReward();
        BaseResponseModel res = userService.updateMoneyFromAdmin(nickname, (long)money, "vin", "MISSION", "MISSION", "Ti\u1ec1n th\u01b0\u1edfng n\u1ea1p ti\u1ec1n l\u1ea7n \u0111\u1ea7u trong s\u1ef1 ki\u1ec7n ");
        if (res.isSuccess()) {
            this.userMissionService.setStatusMission(nickname, this.mission.getId(), MissionStatus.FINISH);
        }
        return res.isSuccess();
    }

    @Override
    public boolean rewardMission(String nickname) throws Exception {
        double money = (double)this.mission.getReward() / 100.0 * (double)this.mission.getPoint();
        return this.baseRewardMission(nickname, (long)money);
    }

    @Override
    public void scanMission(String username) {
    }
}

