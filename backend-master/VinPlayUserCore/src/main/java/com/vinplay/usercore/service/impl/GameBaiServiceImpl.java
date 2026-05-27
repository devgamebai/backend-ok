/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.GameTourDaoImpl;
import com.vinplay.usercore.service.GameBaiService;
import java.sql.SQLException;

public class GameBaiServiceImpl
implements GameBaiService {
    @Override
    public String getString(String key) throws SQLException {
        GameTourDaoImpl gtDao = new GameTourDaoImpl();
        return gtDao.getString(key);
    }

    @Override
    public boolean saveString(String key, String value) throws SQLException {
        GameTourDaoImpl gtDao = new GameTourDaoImpl();
        return gtDao.saveString(key, value);
    }
}

