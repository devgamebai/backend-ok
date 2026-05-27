/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.GetUserIndexDAOImpl;
import com.vinplay.dal.service.GetUserIndexService;
import java.sql.SQLException;

public class GetUserIndexServiceImpl
implements GetUserIndexService {
    @Override
    public int getRegister(String timeStart, String timeEnd, String referralCode) throws SQLException {
        GetUserIndexDAOImpl dao = new GetUserIndexDAOImpl();
        return dao.getRegister(timeStart, timeEnd, referralCode);
    }

    @Override
    public int getRecharge(String timeStart, String timeEnd, String referralCode) throws SQLException {
        GetUserIndexDAOImpl dao = new GetUserIndexDAOImpl();
        return dao.getRecharge(timeStart, timeEnd, referralCode);
    }

    @Override
    public int getSecMobile(String timeStart, String timeEnd, String referralCode) throws SQLException {
        GetUserIndexDAOImpl dao = new GetUserIndexDAOImpl();
        return dao.getSecMobile(timeStart, timeEnd, referralCode);
    }

    @Override
    public int getBoth(String timeStart, String timeEnd, String referralCode) throws SQLException {
        GetUserIndexDAOImpl dao = new GetUserIndexDAOImpl();
        return dao.getBoth(timeStart, timeEnd, referralCode);
    }
}

