/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LogVipPointEventResponse
 *  com.vinplay.vbee.common.response.UserVipPointEventResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.VipPointEventDaoImpl;
import com.vinplay.dal.service.VipPointEventService;
import com.vinplay.vbee.common.response.LogVipPointEventResponse;
import com.vinplay.vbee.common.response.UserVipPointEventResponse;
import java.sql.SQLException;
import java.util.List;

public class VipPointEventServiceImpl
implements VipPointEventService {
    @Override
    public List<LogVipPointEventResponse> listLogVipPointEvent(String nickName, String value, String type, String timeStart, String timeEnd, int page, String bot) {
        VipPointEventDaoImpl dao = new VipPointEventDaoImpl();
        return dao.listLogVipPointEvent(nickName, value, type, timeStart, timeEnd, page, bot);
    }

    @Override
    public long countLogVipPointEvent(String nickName, String value, String type, String timeStart, String timeEnd, String bot) {
        VipPointEventDaoImpl dao = new VipPointEventDaoImpl();
        return dao.countLogVipPointEvent(nickName, value, type, timeStart, timeEnd, bot);
    }

    @Override
    public long totalVipPointEvent(String nickName, String value, String type, String timeStart, String timeEnd, String bot) {
        VipPointEventDaoImpl dao = new VipPointEventDaoImpl();
        return dao.totalVipPointEvent(nickName, value, type, timeStart, timeEnd, bot);
    }

    @Override
    public List<UserVipPointEventResponse> listuserVipPoint(String nickName, String sort, String filed, String timeStart, String timeEnd, int page, String bot) throws SQLException {
        VipPointEventDaoImpl dao = new VipPointEventDaoImpl();
        return dao.listuserVipPoint(nickName, sort, filed, timeStart, timeEnd, page, bot);
    }

    @Override
    public long countUserVipPoint(String nickName, String sort, String filed, String timeStart, String timeEnd, String bot) throws SQLException {
        VipPointEventDaoImpl dao = new VipPointEventDaoImpl();
        return dao.countUserVipPoint(nickName, sort, filed, timeStart, timeEnd, bot);
    }
}

