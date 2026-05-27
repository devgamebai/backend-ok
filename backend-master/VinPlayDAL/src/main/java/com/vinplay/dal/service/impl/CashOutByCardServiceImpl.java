/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.CashOutByCardResponse
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.CashOutByCardDAOImpl;
import com.vinplay.dal.service.CashOutByCardService;
import com.vinplay.vbee.common.response.CashOutByCardResponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.List;

public class CashOutByCardServiceImpl
implements CashOutByCardService {
    @Override
    public List<CashOutByCardResponse> searchCashOutByCard(String nickName, String provider, String status, String code, String timeStart, String timeEnd, int page, String transId, String partner) {
        CashOutByCardDAOImpl dao = new CashOutByCardDAOImpl();
        return dao.searchCashOutByCard(nickName, provider, status, code, timeStart, timeEnd, page, transId, partner);
    }

    @Override
    public int countSearchCashOutByCard(String nickName, String provider, String status, String code, String timeStart, String timeEnd, String transId, String partner) {
        CashOutByCardDAOImpl dao = new CashOutByCardDAOImpl();
        return dao.countSearchCashOutByCard(nickName, provider, status, code, timeStart, timeEnd, transId, partner);
    }

    @Override
    public long moneyTotal(String nickName, String provider, String status, String code, String timeStart, String timeEnd, String transId, String partner) {
        CashOutByCardDAOImpl dao = new CashOutByCardDAOImpl();
        return dao.moneyTotal(nickName, provider, status, code, timeStart, timeEnd, transId, partner);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalCashOutByCard(String timeStart, String timeEnd, String partner, String code) {
        CashOutByCardDAOImpl dao = new CashOutByCardDAOImpl();
        return dao.moneyTotalCashOutByCard(timeStart, timeEnd, partner, code);
    }

    @Override
    public List<CashOutByCardResponse> exportDataCashOutByCard(String provider, String code, String timeStart, String timeEnd, String partner, String amount) {
        CashOutByCardDAOImpl dao = new CashOutByCardDAOImpl();
        return dao.exportDataCashOutByCard(provider, code, timeStart, timeEnd, partner, amount);
    }
}

