/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LogUpdateCardPendingReponse
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogUpdateCardPendingDAOImpl;
import com.vinplay.dal.service.LogUpdateCardPendingService;
import com.vinplay.vbee.common.response.LogUpdateCardPendingReponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.List;

public class LogUpdateCardPendingServiceImpl
implements LogUpdateCardPendingService {
    @Override
    public List<LogUpdateCardPendingReponse> searchLogUpdateCardPending(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId, String actor) {
        LogUpdateCardPendingDAOImpl dao = new LogUpdateCardPendingDAOImpl();
        return dao.searchLogUpdateCardPending(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transId, actor);
    }

    @Override
    public int countTotalRecordLogUpdateCardPending(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId, String actor) {
        LogUpdateCardPendingDAOImpl dao = new LogUpdateCardPendingDAOImpl();
        return dao.countTotalRecordLogUpdateCardPending(nickName, provider, serial, pin, code, timeStart, timeEnd, transId, actor);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalUpdateCardPengding(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId, String actor) {
        LogUpdateCardPendingDAOImpl dao = new LogUpdateCardPendingDAOImpl();
        return dao.moneyTotalUpdateCardPengding(nickName, provider, serial, pin, code, timeStart, timeEnd, transId, actor);
    }
}

