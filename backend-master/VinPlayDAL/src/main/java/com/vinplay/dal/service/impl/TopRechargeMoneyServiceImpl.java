/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.TopRechargeMoneyResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.TopRechargeMoneyDAOImpl;
import com.vinplay.dal.service.TopRechargeMoneyService;
import com.vinplay.vbee.common.response.TopRechargeMoneyResponse;
import java.sql.SQLException;
import java.util.List;

public class TopRechargeMoneyServiceImpl
implements TopRechargeMoneyService {
    @Override
    public List<TopRechargeMoneyResponse> getTopRechargeMoney(int top, String nickName, int page, int bot) throws SQLException {
        TopRechargeMoneyDAOImpl dao = new TopRechargeMoneyDAOImpl();
        return dao.getTopRechargeMoney(top, nickName, page, bot);
    }
}

