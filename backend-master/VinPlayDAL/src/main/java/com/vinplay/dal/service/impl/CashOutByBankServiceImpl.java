/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.CashOutByBankReponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.CashOutByBankDAOImpl;
import com.vinplay.dal.service.CashOutByBankService;
import com.vinplay.vbee.common.response.CashOutByBankReponse;
import java.util.List;

public class CashOutByBankServiceImpl
implements CashOutByBankService {
    @Override
    public List<CashOutByBankReponse> searchCashOutByBank(String nickName, String bank, String status, String code, String timeStart, String timeEnd, int page, String transid) {
        CashOutByBankDAOImpl dao = new CashOutByBankDAOImpl();
        return dao.searchCashOutByBank(nickName, bank, status, code, timeStart, timeEnd, page, transid);
    }

    @Override
    public int countSearchCashOutByBank(String nickName, String bank, String status, String code, String timeStart, String timeEnd, String transid) {
        CashOutByBankDAOImpl dao = new CashOutByBankDAOImpl();
        return dao.countSearchCashOutByBank(nickName, bank, status, code, timeStart, timeEnd, transid);
    }

    @Override
    public long moneyTotal(String nickName, String bank, String status, String code, String timeStart, String timeEnd, String transid) {
        CashOutByBankDAOImpl dao = new CashOutByBankDAOImpl();
        return dao.moneyTotal(nickName, bank, status, code, timeStart, timeEnd, transid);
    }
}

