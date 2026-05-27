/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.ResultGameConfigResponse
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.response.ResultGameConfigResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface GameConfigDao {
    public List<ResultGameConfigResponse> getGameConfigAdmin(String var1, String var2) throws SQLException;

    public Map<String, String> getGameConfig() throws SQLException;

    public boolean createGameConfig(String var1, String var2, String var3, String var4) throws SQLException;

    public boolean updateGameConfig(String var1, String var2, String var3, String var4) throws SQLException;

    public String getGameCommon(String var1) throws SQLException;
}

