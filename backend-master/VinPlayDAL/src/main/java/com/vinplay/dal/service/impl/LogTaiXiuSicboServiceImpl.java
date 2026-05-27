/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.LogTaiXiuSicboDAO;
import com.vinplay.dal.dao.impl.LogTaiXiuSicboDAOImpl;
import com.vinplay.dal.service.LogTaiXiuSicboService;
import java.sql.SQLException;

public class LogTaiXiuSicboServiceImpl
implements LogTaiXiuSicboService {
    private LogTaiXiuSicboDAO dao = new LogTaiXiuSicboDAOImpl();

    @Override
    public int deleteLogTaiXiuSicboByDay(int soNgay) throws SQLException {
        return this.dao.deleteLogTaiXiuSicboByDay(soNgay);
    }
}

