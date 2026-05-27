/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MiniPokerResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogMiniPokerDAOImpl;
import com.vinplay.dal.service.LogMiniPokerService;
import com.vinplay.vbee.common.response.MiniPokerResponse;
import java.util.List;

public class LogMiniPokerServiceImpl
implements LogMiniPokerService {
    @Override
    public List<MiniPokerResponse> listMiniPoker(String nickName, String bet_value, String timeStart, String timeEnd, String moneyType, int page) {
        LogMiniPokerDAOImpl dao = new LogMiniPokerDAOImpl();
        return dao.listMiniPoker(nickName, bet_value, timeStart, timeEnd, moneyType, page);
    }

    @Override
    public int countMiniPoker(String nickName, String bet_value, String timeStart, String timeEnd, String moneyType) {
        LogMiniPokerDAOImpl dao = new LogMiniPokerDAOImpl();
        return dao.countMiniPoker(nickName, bet_value, timeStart, timeEnd, moneyType);
    }
}

