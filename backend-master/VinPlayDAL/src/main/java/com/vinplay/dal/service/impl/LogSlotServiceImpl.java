/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.SlotResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogSlotDAOImpl;
import com.vinplay.dal.service.LogSlotService;
import com.vinplay.vbee.common.response.SlotResponse;
import java.sql.SQLException;
import java.util.List;

public class LogSlotServiceImpl
implements LogSlotService {
    @Override
    public List<SlotResponse> listLogSlot(String referentId, String userName, String betValue, String timeStart, String timeEnd, int page, String gameName) throws SQLException {
        LogSlotDAOImpl dao = new LogSlotDAOImpl();
        return dao.listLogSlot(referentId, userName, betValue, timeStart, timeEnd, page, gameName);
    }

    @Override
    public long countLogKhoBau(String referentId, String userName, String betValue, String timeStart, String timeEnd) throws SQLException {
        LogSlotDAOImpl dao = new LogSlotDAOImpl();
        return dao.countLogKhoBau(referentId, userName, betValue, timeStart, timeEnd);
    }
}

