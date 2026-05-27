/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.RechargeBySmsResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.RechargeBySmsPlus9029DAOImpl;
import com.vinplay.dal.service.RechargeBySmsPlus9029Service;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeBySmsResponse;
import java.util.List;

public class RechargeBySmsPlus9029ServiceImpl
implements RechargeBySmsPlus9029Service {
    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeBySmsPlus9029(String timeStart, String timeEnd, String code) {
        RechargeBySmsPlus9029DAOImpl dao = new RechargeBySmsPlus9029DAOImpl();
        return dao.moneyTotalRechargeBySmsPlus9029(timeStart, timeEnd, code);
    }

    @Override
    public List<RechargeBySmsResponse> exportDataRechargeBySmsPlus(String timeStart, String timeEnd, String amount, String code) {
        RechargeBySmsPlus9029DAOImpl dao = new RechargeBySmsPlus9029DAOImpl();
        return dao.exportDataRechargeBySmsPlus(timeStart, timeEnd, amount, code);
    }
}

