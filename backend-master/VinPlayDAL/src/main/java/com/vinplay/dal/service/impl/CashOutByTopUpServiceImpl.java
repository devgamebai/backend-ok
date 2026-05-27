/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.CashOutByTopUpResponse
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.CashOutByTopUpDAOImpl;
import com.vinplay.dal.service.CashOutByTopUpService;
import com.vinplay.vbee.common.response.CashOutByTopUpResponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.List;

public class CashOutByTopUpServiceImpl
implements CashOutByTopUpService {
    @Override
    public List<CashOutByTopUpResponse> searchCashOutByTopUp(String nickName, String target, String status, String code, String timeStart, String timeEnd, int page, String transId, String partner, String type) {
        CashOutByTopUpDAOImpl dao = new CashOutByTopUpDAOImpl();
        return dao.searchCashOutByTopUp(nickName, target, status, code, timeStart, timeEnd, page, transId, partner, type);
    }

    @Override
    public int countSearchCashOutByTopUp(String nickName, String target, String status, String code, String timeStart, String timeEnd, String transId, String partner, String type) {
        CashOutByTopUpDAOImpl dao = new CashOutByTopUpDAOImpl();
        return dao.countSearchCashOutByTopUp(nickName, target, status, code, timeStart, timeEnd, transId, partner, type);
    }

    @Override
    public long moneyTotal(String nickName, String target, String status, String code, String timeStart, String timeEnd, String transId, String partner, String type) {
        CashOutByTopUpDAOImpl dao = new CashOutByTopUpDAOImpl();
        return dao.moneyTotal(nickName, target, status, code, timeStart, timeEnd, transId, partner, type);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalCashOutByTopup(String timeStart, String timeEnd, String partner, String code, String type) {
        CashOutByTopUpDAOImpl dao = new CashOutByTopUpDAOImpl();
        return dao.moneyTotalCashOutByTopup(timeStart, timeEnd, partner, code, type);
    }

    @Override
    public List<CashOutByTopUpResponse> exportDataCashOutByTopup(String provider, String code, String timeStart, String timeEnd, String partner, String amount, String type) {
        CashOutByTopUpDAOImpl dao = new CashOutByTopUpDAOImpl();
        return dao.exportDataCashOutByTopup(provider, code, timeStart, timeEnd, partner, amount, type);
    }
}

