/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LogCaoThapResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogCaoThapDAOImpl;
import com.vinplay.dal.service.LogCaoThapService;
import com.vinplay.vbee.common.response.LogCaoThapResponse;
import java.util.List;

public class LogCaoThapServiceImpl
implements LogCaoThapService {
    @Override
    public List<LogCaoThapResponse> listCaoThap(String nickName, String transId, String bet_value, String timeStart, String timeEnd, String moneyType, int page) {
        LogCaoThapDAOImpl dao = new LogCaoThapDAOImpl();
        return dao.listCaoThap(nickName, transId, bet_value, timeStart, timeEnd, moneyType, page);
    }

    @Override
    public int countCaoThap(String nickName, String transId, String bet_value, String timeStart, String timeEnd, String moneyType) {
        LogCaoThapDAOImpl dao = new LogCaoThapDAOImpl();
        return dao.countCaoThap(nickName, transId, bet_value, timeStart, timeEnd, moneyType);
    }
}

