/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.gamebase.mission;

import com.gamebase.dao.LogMoneyUserExtendDao;
import com.gamebase.dao.impl.LogMoneyUserExtendDaoImpl;
import com.gamebase.dao.model.SumUserDepositTimeResult;
import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.gamebase.entities.MissionRule;
import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import com.gamebase.entities.UserMissionRule;
import com.gamebase.mission.BaseMissionProcess;
import com.gamebase.mission.MissionProcess;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map;
import org.apache.log4j.Logger;

public class MissionFirstDepositComplex
extends BaseMissionProcess
implements MissionProcess {
    private static final Logger logger = Logger.getLogger((String)"base_game");
    private final LogMoneyUserExtendDao logMoneyUserExtendDao = new LogMoneyUserExtendDaoImpl();
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MissionFirstDepositComplex(EventMissionService eventMissionService, Mission mission, UserMissionService userMissionService) {
        super(eventMissionService, mission, userMissionService);
    }

    @Override
    public String getMissionType() {
        return "first_deposit_complex";
    }

    @Override
    protected UserMission checkStatus(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        UserMission userMission = this.userMissionService.getMission(logMoneyUserMessage.getNickname(), this.mission.getId());
        if (userMission == null || userMission.getStatus() == MissionStatus.COMPLETE || userMission.getStatus() == MissionStatus.FINISH || userMission.getStatus() == MissionStatus.NONE) {
            return null;
        }
        return userMission;
    }

    @Override
    public void checkMission(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        this.checkExpired();
        UserMission userMission = this.checkStatus(logMoneyUserMessage);
        if (userMission == null) {
            return;
        }
        if (this.checkDeposit(logMoneyUserMessage) && userMission.getStatus() == MissionStatus.REWARD_IN_PROGRESS) {
            for (UserMissionRule missionRule : userMission.getRules().values()) {
                if (missionRule.getProgress() >= 100) continue;
                return;
            }
            double money = (double)this.mission.getReward() / 100.0 * (double)logMoneyUserMessage.getMoneyExchange();
            if (this.baseRewardMission(logMoneyUserMessage.getNickname(), (int)money)) {
                this.userMissionService.setStatusMission(userMission.getNickname(), this.mission.getId(), MissionStatus.FINISH);
                userMission.setStatus(MissionStatus.FINISH);
                this.userMissionService.updateMission(userMission);
            }
            return;
        }
        for (MissionRule rule : this.mission.getRules().values()) {
            switch (rule.getType()) {
                case BetGameMultiWithDeposit: {
                    userMission = this.processBetGameMultiWithDeposit(rule, userMission, logMoneyUserMessage);
                    break;
                }
                case SumBetMultiWithDeposit: {
                    userMission = this.processSumBetMultiWithDeposit(rule, userMission, logMoneyUserMessage);
                    break;
                }
            }
        }
        this.updateMissionComplex(userMission);
    }

    @Override
    public boolean rewardMission(String nickname) throws Exception {
        return false;
    }

    @Override
    public void scanMission(String username) {
    }

    public void checkMetaData(MissionRule rule, UserMission userMission) {
        if (userMission.getMetaData() != null && !userMission.getMetaData().containsKey("deposit")) {
            EventMission eventMission = null;
            try {
                SumUserDepositTimeResult depositTimeResult;
                eventMission = this.eventMissionService.getEventMissionById(this.mission.getEvent_id());
                if (eventMission != null && (depositTimeResult = this.logMoneyUserExtendDao.sumUserDepositTime(userMission.getNickname(), "2020-01-01 00:00:00", this.format.format(eventMission.getCreatedAt()), "")) != null) {
                    userMission.setMetaDataDeposit("deposit", depositTimeResult.getSum());
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private UserMission updateUserMissionRule(MissionRule rule, UserMission userMission, long money) {
        UserMissionRule userMissionRule;
        Map<String, UserMissionRule> rules = userMission.getRules();
        if (rules.containsKey(rule.getId())) {
            userMissionRule = rules.get(rule.getId());
        } else {
            userMissionRule = new UserMissionRule();
            userMissionRule.setId(rule.getId());
            userMissionRule.setType(rule.getType());
            userMissionRule.setGame_id(rule.getGame_id());
            userMissionRule.setPoint(rule.getPoint());
            userMissionRule.setWork(0L);
            userMissionRule.setProgress(0);
        }
        long work = userMissionRule.getWork() + money;
        userMissionRule.setWork(work);
        this.checkMetaData(rule, userMission);
        long point = rule.getPoint() * userMission.getMetaDataDeposit("deposit");
        userMissionRule.setPoint(point);
        if (work >= point) {
            userMissionRule.setProgress(100);
            userMissionRule.setCompleted(true);
        } else {
            double progressLong = (double)work / (double)point * 100.0;
            userMissionRule.setProgress((int)progressLong);
        }
        rules.replace(rule.getId(), userMissionRule);
        return userMission;
    }

    protected boolean checkGame(int gameId, LogMoneyUserMessage logMoneyUserMessage) {
        Games game = Games.findGameById(gameId);
        if (game == null) {
            return false;
        }
        return game.getName().contains(logMoneyUserMessage.getActionName());
    }

    private UserMission processBetGameMultiWithDeposit(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
        if (!this.checkGame(rule.getGame_id(), logMoneyUserMessage)) {
            return userMission;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money > 0L) {
            return userMission;
        }
        money = Math.abs(money);
        return this.updateUserMissionRule(rule, userMission, money);
    }

    private UserMission processSumBetMultiWithDeposit(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
        if (!this.checkGameAction(logMoneyUserMessage)) {
            return userMission;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money > 0L) {
            return userMission;
        }
        money = Math.abs(money);
        return this.updateUserMissionRule(rule, userMission, money);
    }

    @Override
    protected void updateMissionComplex(UserMission userMission) throws Exception {
        boolean isComplete = true;
        for (UserMissionRule userMissionRule : userMission.getRules().values()) {
            if (userMissionRule.isCompleted()) continue;
            isComplete = false;
            break;
        }
        if (isComplete) {
            this.userMissionService.setStatusMission(userMission.getNickname(), this.mission.getId(), MissionStatus.REWARD_IN_PROGRESS);
            userMission.setStatus(MissionStatus.REWARD_IN_PROGRESS);
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

