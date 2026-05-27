/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.ResultGameConfigResponse
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.usercore.service.GameConfigService;
import com.vinplay.vbee.common.response.ResultGameConfigResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class GameConfigServiceImpl
implements GameConfigService {
    @Override
    public Map<String, String> getGameConfig() throws SQLException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        return dao.getGameConfig();
    }

    @Override
    public List<ResultGameConfigResponse> getGameConfigAdmin(String name, String pf) throws SQLException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        return dao.getGameConfigAdmin(name, pf);
    }

    @Override
    public boolean createGameConfig(String name, String value, String version, String flatForm) throws SQLException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        return dao.createGameConfig(name, value, version, flatForm);
    }

    @Override
    public boolean updateGameConfig(String id, String value, String version, String flatForm) throws SQLException {
        GameConfigDaoImpl dao = new GameConfigDaoImpl();
        return dao.updateGameConfig(id, value, version, flatForm);
    }
}

