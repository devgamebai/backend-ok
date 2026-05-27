/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.gamebase.mission;

import com.gamebase.entities.Mission;
import com.gamebase.entities.MissionRule;
import com.gamebase.entities.UserMission;
import com.gamebase.entities.UserMissionRule;
import com.gamebase.mission.BaseMissionProcess;
import com.gamebase.mission.MissionProcess;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import java.util.Map;
import org.apache.log4j.Logger;

public class MissionComplex
extends BaseMissionProcess
implements MissionProcess {
    private static final Logger logger = Logger.getLogger((String)"base_game");

    public MissionComplex(EventMissionService eventMissionService, Mission mission, UserMissionService userMissionService) {
        super(eventMissionService, mission, userMissionService);
    }

    @Override
    public String getMissionType() {
        return "complex";
    }

    @Override
    public void checkMission(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        this.checkExpired();
        UserMission userMission = this.nextProcessMission(logMoneyUserMessage);
        if (userMission == null) {
            return;
        }
        for (MissionRule rule : this.mission.getRules().values()) {
            switch (rule.getType()) {
                case SumDeposit: {
                    userMission = this.processSumDeposit(rule, userMission, logMoneyUserMessage);
                    break;
                }
                case SumBet: {
                    userMission = this.processSumBet(rule, userMission, logMoneyUserMessage);
                    break;
                }
                case SumWin: {
                    userMission = this.processSumWin(rule, userMission, logMoneyUserMessage);
                    break;
                }
                case BetGame: {
                    userMission = this.processBet(rule, userMission, logMoneyUserMessage);
                    break;
                }
                case WinGame: {
                    userMission = this.processWin(rule, userMission, logMoneyUserMessage);
                    break;
                }
            }
        }
        this.updateMissionComplex(userMission);
    }

    @Override
    public boolean rewardMission(String nickname) throws Exception {
        double money = this.mission.getReward();
        return this.baseRewardMission(nickname, (int)money);
    }

    @Override
    public void scanMission(String username) {
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
        if (work >= rule.getPoint()) {
            userMissionRule.setProgress(100);
            userMissionRule.setCompleted(true);
        } else {
            double progressLong = (double)work / (double)rule.getPoint() * 100.0;
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

    private UserMission processSumDeposit(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
        if (!this.checkDeposit(logMoneyUserMessage)) {
            return userMission;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money < 0L) {
            return userMission;
        }
        return this.updateUserMissionRule(rule, userMission, money);
    }

    private UserMission processSumBet(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
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

    private UserMission processSumWin(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
        if (!this.checkGameAction(logMoneyUserMessage)) {
            return userMission;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money < 0L) {
            return userMission;
        }
        money += Math.abs(money);
        return this.updateUserMissionRule(rule, userMission, money);
    }

    private UserMission processBet(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
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

    private UserMission processWin(MissionRule rule, UserMission userMission, LogMoneyUserMessage logMoneyUserMessage) {
        if (!this.checkGame(rule.getGame_id(), logMoneyUserMessage)) {
            return userMission;
        }
        long money = logMoneyUserMessage.getMoneyExchange();
        if (money < 0L) {
            return userMission;
        }
        money += Math.abs(money);
        return this.updateUserMissionRule(rule, userMission, money);
    }
}

