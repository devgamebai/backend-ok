/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapMessage
 *  com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateFundMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdatePotMessage
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.CaoThapDAO;
import com.vinplay.dal.dao.impl.CaoThapDAOImpl;
import com.vinplay.dal.entities.caothap.LSGDCaoThap;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.dal.entities.caothap.VinhDanhCaoThap;
import com.vinplay.dal.service.CaoThapService;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogCaoThapMessage;
import com.vinplay.vbee.common.messages.minigame.LogCaoThapWinMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CaoThapServiceImpl
implements CaoThapService {
    CaoThapDAO dao = new CaoThapDAOImpl();

    @Override
    public long[] getPotCaoThap() throws SQLException {
        return this.dao.getPotCaoThap("cao_thap");
    }

    @Override
    public long[] getFundCaoThap() throws SQLException {
        return this.dao.getFundCaoThap("cao_thap");
    }

    @Override
    public void updatePotCaoThap(String potName, long newValue) throws IOException, TimeoutException, InterruptedException {
        UpdatePotMessage message = new UpdatePotMessage();
        message.potName = potName;
        message.newValue = newValue;
        MessageBusFactory.get("queue_pot").publish("queue_pot", (BaseMessage)message, (int)106);
    }

    @Override
    public void updateFundCaoThap(String fundName, long newValue) throws IOException, TimeoutException, InterruptedException {
        UpdateFundMessage message = new UpdateFundMessage();
        message.fundName = fundName;
        message.newValue = newValue;
        MessageBusFactory.get("queue_fund").publish("queue_fund", (BaseMessage)message, (int)110);
    }

    @Override
    public void logCaoThap(long transId, String nickname, long betValue, short result, long prize, String cards, long currentPot, long currentFund, int moneyType, short potBet, int step) throws IOException, TimeoutException, InterruptedException {
        LogCaoThapMessage message = new LogCaoThapMessage();
        message.transId = transId;
        message.nickname = nickname;
        message.betValue = betValue;
        message.result = result;
        message.prize = prize;
        message.cards = cards;
        message.currentPot = currentPot;
        message.currentFund = currentFund;
        message.moneyType = moneyType;
        message.potBet = potBet;
        message.step = step;
        MessageBusFactory.get("queue_caothap").publish("queue_caothap", (BaseMessage)message, 112);
    }

    @Override
    public void logCaoThapWin(long transId, String nickname, long betValue, short result, long prize, String cards, int moneyType) throws IOException, TimeoutException, InterruptedException {
        LogCaoThapWinMessage message = new LogCaoThapWinMessage();
        message.transId = transId;
        message.nickname = nickname;
        message.betValue = betValue;
        message.result = result;
        message.prize = prize;
        message.cards = cards;
        message.moneyType = moneyType;
        MessageBusFactory.get("queue_caothap").publish("queue_caothap", (BaseMessage)message, 113);
    }

    @Override
    public List<LSGDCaoThap> getLichSuGiaoDich(String nickname, int pageNumber, int moneyType) {
        return this.dao.getLichSuGiaoDich(nickname, pageNumber, moneyType);
    }

    @Override
    public List<VinhDanhCaoThap> getBangVinhDanh(int pageNumber, int moneyType) {
        return this.dao.getBangVinhDanh(pageNumber, moneyType);
    }

    @Override
    public int countLichSuGiaoDich(String nickname, int moneyType) {
        return this.dao.countLichSuGiaoDich(nickname, moneyType);
    }

    @Override
    public int countVinhDanh(int moneyType) {
        return this.dao.countVinhDanh(moneyType);
    }

    @Override
    public long getLastReferenceId() {
        return this.dao.getLastReferenceId();
    }

    @Override
    public List<TopCaoThap> geTopCaoThap(String startTime, String endTime) {
        return this.dao.getTop(startTime, endTime);
    }

    @Override
    public void insertBotEvent(String nickname, long betValue, long prize, String cards) {
        try {
            this.logCaoThapWin(-1L, nickname, betValue, (short)4, prize, cards, 1);
        }
        catch (IOException | InterruptedException | TimeoutException ex2) {
            Exception e = ex2;
            e.printStackTrace();
        }
    }
}

