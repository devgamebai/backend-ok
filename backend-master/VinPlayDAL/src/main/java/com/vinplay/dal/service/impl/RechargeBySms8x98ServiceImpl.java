/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.RechargeBySmsResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.RechargeBySms8x98DAOImpl;
import com.vinplay.dal.service.RechargeBySms8x98Service;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeBySmsResponse;
import java.util.List;

public class RechargeBySms8x98ServiceImpl
implements RechargeBySms8x98Service {
    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeBySms8x98(String timeStart, String timeEnd, String code) {
        RechargeBySms8x98DAOImpl dao = new RechargeBySms8x98DAOImpl();
        return dao.moneyTotalRechargeBySms8x98(timeStart, timeEnd, code);
    }

    @Override
    public List<RechargeBySmsResponse> exportDataRechargeBySms(String timeStart, String timeEnd, String amount, String code) {
        RechargeBySms8x98DAOImpl dao = new RechargeBySms8x98DAOImpl();
        return dao.exportDataRechargeBySms(timeStart, timeEnd, amount, code);
    }
}

