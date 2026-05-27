/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogMiniPokerMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateFundMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdatePotMessage
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.MiniGameDAO;
import com.vinplay.dal.dao.MiniPokerDAO;
import com.vinplay.dal.dao.impl.MiniGameDAOImpl;
import com.vinplay.dal.dao.impl.MiniPokerDAOImpl;
import com.vinplay.dal.entities.minipoker.LSGDMiniPoker;
import com.vinplay.dal.entities.minipoker.VinhDanhMiniPoker;
import com.vinplay.dal.service.MiniPokerService;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogMiniPokerMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MiniPokerServiceImpl
implements MiniPokerService {
    MiniPokerDAO dao = new MiniPokerDAOImpl();
    MiniGameDAO miniGameDAO = new MiniGameDAOImpl();

    @Override
    public long[] getPotMiniPoker() throws SQLException {
        return this.miniGameDAO.getPots(Games.MINI_POKER.getName());
    }

    @Override
    public long[] getFundMiniPoker() throws SQLException {
        return this.miniGameDAO.getFunds(Games.MINI_POKER.getName());
    }

    @Override
    public void updatePotMiniPoker(String potName, long newValue) throws IOException, TimeoutException, InterruptedException {
        UpdatePotMessage message = new UpdatePotMessage();
        message.potName = potName;
        message.newValue = newValue;
        MessageBusFactory.get("queue_pot").publish("queue_pot", (BaseMessage)message, (int)106);
    }

    @Override
    public void updateFundMiniPoker(String fundName, long newValue) throws IOException, TimeoutException, InterruptedException {
        UpdateFundMessage message = new UpdateFundMessage();
        message.fundName = fundName;
        message.newValue = newValue;
        MessageBusFactory.get("queue_fund").publish("queue_fund", (BaseMessage)message, (int)110);
    }

    @Override
    public void logMiniPoker(String username, long betValue, short result, long prize, String cards, long currentPot, long currentFund, int moneyType) throws IOException, TimeoutException, InterruptedException {
        LogMiniPokerMessage message = new LogMiniPokerMessage();
        message.username = username;
        message.betValue = betValue;
        message.result = result;
        message.prize = prize;
        message.cards = cards;
        message.currentPot = currentPot;
        message.currentFund = currentFund;
        message.moneyType = moneyType;
        MessageBusFactory.get("queue_minipoker").publish("queue_minipoker", (BaseMessage)message, 111);
    }

    @Override
    public List<LSGDMiniPoker> getLichSuGiaoDich(String username, int pageNumber, int moneyType) {
        return this.dao.getLichSuGiaoDich(username, pageNumber, moneyType);
    }

    @Override
    public List<VinhDanhMiniPoker> getBangVinhDanh(int moneyType, int page) {
        return this.dao.getBangVinhDanh(moneyType, page);
    }

    @Override
    public int countLichSuGiaoDich(String username, int moneyType) {
        return this.dao.countLichSuGiaoDich(username, moneyType);
    }
}

