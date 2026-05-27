/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.megacard.MegaCardResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.RechargeByMegaCardDAOImpl;
import com.vinplay.dal.service.RechargeByMegaCardService;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.megacard.MegaCardResponse;
import java.util.List;

public class RechargeByMegaCardServiceImpl
implements RechargeByMegaCardService {
    @Override
    public List<MegaCardResponse> searchRechargeByMegaCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId) {
        RechargeByMegaCardDAOImpl dao = new RechargeByMegaCardDAOImpl();
        return dao.searchRechargeByMegaCard(nickName, provider, serial, pin, code, timeStart, timeEnd, page, transId);
    }

    @Override
    public int countSearchRechargeByMegaCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByMegaCardDAOImpl dao = new RechargeByMegaCardDAOImpl();
        return dao.countSearchRechargeByMegaCard(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public long moneyTotal(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        RechargeByMegaCardDAOImpl dao = new RechargeByMegaCardDAOImpl();
        return dao.moneyTotal(nickName, provider, serial, pin, code, timeStart, timeEnd, transId);
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByMegaCard(String timeStart, String timeEnd, String code) {
        RechargeByMegaCardDAOImpl dao = new RechargeByMegaCardDAOImpl();
        return dao.moneyTotalRechargeByMegaCard(timeStart, timeEnd, code);
    }
}

