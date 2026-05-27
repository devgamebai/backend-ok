/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.TaiXiuDetailReponse
 *  com.vinplay.vbee.common.response.TaiXiuResponse
 *  com.vinplay.vbee.common.response.TaiXiuResultResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogTaiXiuDAOImpl;
import com.vinplay.dal.service.LogTaiXiuService;
import com.vinplay.vbee.common.response.TaiXiuDetailReponse;
import com.vinplay.vbee.common.response.TaiXiuResponse;
import com.vinplay.vbee.common.response.TaiXiuResultResponse;
import java.sql.SQLException;
import java.util.List;

public class LogTaiXiuServiceImpl
implements LogTaiXiuService {
    @Override
    public List<TaiXiuResponse> listLogTaiXiu(String referentId, String userName, String betSide, String moneyType, String timeStart, String timeEnd, int page) throws SQLException {
        LogTaiXiuDAOImpl dao = new LogTaiXiuDAOImpl();
        return dao.listLogTaiXiu(referentId, userName, betSide, moneyType, timeStart, timeEnd, page);
    }

    @Override
    public int countLogTaiXiu(String referentId, String userName, String betSide, String moneyType, String timeStart, String timeEnd) throws SQLException {
        LogTaiXiuDAOImpl dao = new LogTaiXiuDAOImpl();
        return dao.countLogTaiXiu(referentId, userName, betSide, moneyType, timeStart, timeEnd);
    }

    @Override
    public List<TaiXiuDetailReponse> getLogTaiXiuDetail(String referent_id, String betSide, String moneyType, String nickName, int page) throws SQLException {
        LogTaiXiuDAOImpl dao = new LogTaiXiuDAOImpl();
        return dao.getLogTaiXiuDetail(referent_id, betSide, moneyType, nickName, page);
    }

    @Override
    public int countLogTaiXiuDetail(String referent_id, String betSide, String moneyType, String nickName) throws SQLException {
        LogTaiXiuDAOImpl dao = new LogTaiXiuDAOImpl();
        return dao.countLogTaiXiuDetail(referent_id, betSide, moneyType, nickName);
    }

    @Override
    public List<TaiXiuResultResponse> listLogTaiXiuResult(String referentId, String moneyType, String timeStart, String timeEnd, int page) throws SQLException {
        LogTaiXiuDAOImpl dao = new LogTaiXiuDAOImpl();
        return dao.listLogTaiXiuResult(referentId, moneyType, timeStart, timeEnd, page);
    }

    @Override
    public int countLogTaiXiuResult(String referentId, String moneyType, String timeStart, String timeEnd) throws SQLException {
        LogTaiXiuDAOImpl dao = new LogTaiXiuDAOImpl();
        return dao.countLogTaiXiuResult(referentId, moneyType, timeStart, timeEnd);
    }
}

