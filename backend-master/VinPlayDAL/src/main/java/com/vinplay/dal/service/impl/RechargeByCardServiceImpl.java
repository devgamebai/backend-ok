/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.RechargeByCardReponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.RechargeByCardDAOImpl;
import com.vinplay.dal.service.RechargeByCardService;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeByCardReponse;
import java.util.List;

public class RechargeByCardServiceImpl
implements RechargeByCardService {
    @Override
    public List<RechargeByCardReponse> searchRechargeByCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId) {
        RechargeByCardDAOImpl dao = new RechargeByCardDAOImpl();
        return dao.searchRechargeByCard(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transId);
    }

    @Override
    public int countSearchRechargeByCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByCardDAOImpl dao = new RechargeByCardDAOImpl();
        return dao.countSearchRechargeByCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public long moneyTotal(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByCardDAOImpl dao = new RechargeByCardDAOImpl();
        return dao.moneyTotal(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByCardDAOImpl dao = new RechargeByCardDAOImpl();
        return dao.moneyTotalRechargeByCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> doiSoatRechargeByCard(int code, String timeStart, String timeEnd) {
        RechargeByCardDAOImpl dao = new RechargeByCardDAOImpl();
        return dao.doiSoatRechargeByCard(code, timeStart, timeEnd);
    }

    @Override
    public List<RechargeByCardReponse> exportDataRechargeByCard(String provider, String timeStart, String timeEnd, String amount, String code) {
        RechargeByCardDAOImpl dao = new RechargeByCardDAOImpl();
        return dao.exportDataRechargeByCard(provider, timeStart, timeEnd, amount, code);
    }
}

