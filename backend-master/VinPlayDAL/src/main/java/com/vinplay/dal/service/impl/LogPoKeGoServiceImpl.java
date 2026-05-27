/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.PokegoResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogPokegoDAOImpl;
import com.vinplay.dal.service.LogPoKeGoService;
import com.vinplay.vbee.common.response.PokegoResponse;
import java.sql.SQLException;
import java.util.List;

public class LogPoKeGoServiceImpl
implements LogPoKeGoService {
    @Override
    public List<PokegoResponse> listLogPokego(String referentId, String userName, String moneyType, String betValue, String timeStart, String timeEnd, int page) throws SQLException {
        LogPokegoDAOImpl dao = new LogPokegoDAOImpl();
        return dao.listLogPokego(referentId, userName, moneyType, betValue, timeStart, timeEnd, page);
    }

    @Override
    public long countLogPokego(String referentId, String userName, String moneyType, String betValue, String timeStart, String timeEnd) throws SQLException {
        LogPokegoDAOImpl dao = new LogPokegoDAOImpl();
        return dao.countLogPokego(referentId, userName, moneyType, betValue, timeStart, timeEnd);
    }
}

