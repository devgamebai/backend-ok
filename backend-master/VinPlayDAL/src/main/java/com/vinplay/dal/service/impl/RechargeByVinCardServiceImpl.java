/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.VinCardResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.RechargeByVinCardDAOImpl;
import com.vinplay.dal.service.RechargeByVinCardService;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.VinCardResponse;
import java.util.List;

public class RechargeByVinCardServiceImpl
implements RechargeByVinCardService {
    @Override
    public List<VinCardResponse> searchRechargeByVinCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId) {
        RechargeByVinCardDAOImpl dao = new RechargeByVinCardDAOImpl();
        return dao.searchRechargeByVinCard(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transId);
    }

    @Override
    public int countSearchRechargeByVinCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByVinCardDAOImpl dao = new RechargeByVinCardDAOImpl();
        return dao.countSearchRechargeByVinCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public long moneyTotal(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByVinCardDAOImpl dao = new RechargeByVinCardDAOImpl();
        return dao.moneyTotal(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByVinplayCard(String timeStart, String timeEnd, String code) {
        RechargeByVinCardDAOImpl dao = new RechargeByVinCardDAOImpl();
        return dao.moneyTotalRechargeByVinplayCard(timeStart, timeEnd, code);
    }
}

