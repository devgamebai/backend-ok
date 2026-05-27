/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BauCuaReponseDetail
 *  com.vinplay.vbee.common.response.BauCuaResponse
 *  com.vinplay.vbee.common.response.BauCuaResultResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.LogBauCuaDAOImpl;
import com.vinplay.dal.service.LogBauCuaService;
import com.vinplay.vbee.common.response.BauCuaReponseDetail;
import com.vinplay.vbee.common.response.BauCuaResponse;
import com.vinplay.vbee.common.response.BauCuaResultResponse;
import java.util.List;

public class LogBauCuaServiceImpl
implements LogBauCuaService {
    @Override
    public List<BauCuaResponse> listLogBauCua(String referent_id, String nickName, String room, String timeStart, String timeEnd, String moneyType, int page) {
        LogBauCuaDAOImpl dao = new LogBauCuaDAOImpl();
        return dao.listLogBauCua(referent_id, nickName, room, timeStart, timeEnd, moneyType, page);
    }

    @Override
    public int countLogBauCua(String referent_id, String nickName, String room, String timeStart, String timeEnd, String moneyType) {
        LogBauCuaDAOImpl dao = new LogBauCuaDAOImpl();
        return dao.countLogBauCua(referent_id, nickName, room, timeStart, timeEnd, moneyType);
    }

    @Override
    public List<BauCuaReponseDetail> getLogBauCuaDetail(String referent_id, int page) {
        LogBauCuaDAOImpl dao = new LogBauCuaDAOImpl();
        return dao.getLogBauCuaDetail(referent_id, page);
    }

    @Override
    public int countLogBauCuaDetail(String referent_id) {
        LogBauCuaDAOImpl dao = new LogBauCuaDAOImpl();
        return dao.countLogBauCuaDetail(referent_id);
    }

    @Override
    public List<BauCuaResultResponse> listLogBauCauResult(String referent_id, String room, String timeStart, String timeEnd, int page) {
        LogBauCuaDAOImpl dao = new LogBauCuaDAOImpl();
        return dao.listLogBauCauResult(referent_id, room, timeStart, timeEnd, page);
    }

    @Override
    public long countLogBauCauResult(String referent_id, String room, String timeStart, String timeEnd) {
        LogBauCuaDAOImpl dao = new LogBauCuaDAOImpl();
        return dao.countLogBauCauResult(referent_id, room, timeStart, timeEnd);
    }
}

