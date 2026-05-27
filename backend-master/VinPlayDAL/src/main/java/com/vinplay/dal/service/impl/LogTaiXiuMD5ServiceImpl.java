/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.LogTaiXiuMD5DAO;
import com.vinplay.dal.dao.impl.LogTaiXiuMD5DAOImpl;
import com.vinplay.dal.service.LogTaiXiuMD5Service;
import java.sql.SQLException;

public class LogTaiXiuMD5ServiceImpl
implements LogTaiXiuMD5Service {
    private LogTaiXiuMD5DAO dao = new LogTaiXiuMD5DAOImpl();

    @Override
    public int deleteLogTaiXiuMD5ByDay(int soNgay) throws SQLException {
        return this.dao.deleteLogTaiXiuMD5ByDay(soNgay);
    }
}

