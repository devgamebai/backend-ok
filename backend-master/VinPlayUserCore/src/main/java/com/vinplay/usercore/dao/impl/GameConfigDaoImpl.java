/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.ResultGameConfigResponse
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.GameConfigDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.ResultGameConfigResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameConfigDaoImpl
implements GameConfigDao {
    @Override
    public List<ResultGameConfigResponse> getGameConfigAdmin(String name, String pf) throws SQLException {
        ArrayList<ResultGameConfigResponse> result = new ArrayList<ResultGameConfigResponse>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM vinplay.game_config WHERE 1=1";
            List<String> params = new ArrayList<String>();
            if (pf != null && !pf.equals("")) {
                sql = sql + " and platform=?";
                params.add(pf);
            }
            if (name != null && !name.equals("")) {
                sql = sql + " and name=?";
                params.add(name);
            }
            PreparedStatement stm = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stm.setString(i + 1, params.get(i));
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                ResultGameConfigResponse gc = new ResultGameConfigResponse();
                gc.id = rs.getInt("id");
                gc.name = rs.getString("name");
                gc.value = rs.getString("value");
                gc.version = rs.getString("version");
                gc.platform = rs.getString("platform");
                result.add(gc);
            }
            rs.close();
            stm.close();
        }
        return result;
    }

    @Override
    public Map<String, String> getGameConfig() throws SQLException {
        HashMap<String, String> configs = new HashMap<String, String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " SELECT * FROM vinplay.game_config WHERE name = 'game_config' ";
            PreparedStatement stm = conn.prepareStatement(" SELECT * FROM vinplay.game_config WHERE name = 'game_config' ");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String platform = rs.getString("platform");
                String config = rs.getString("value");
                configs.put(platform, config);
            }
            rs.close();
            stm.close();
        }
        return configs;
    }

    @Override
    public boolean createGameConfig(String name, String value, String version, String platForm) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "INSERT INTO vinplay.game_config(name,value,version,platform) VALUES(?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO vinplay.game_config(name,value,version,platform) VALUES(?,?,?,?)");
            stm.setString(1, name);
            stm.setString(2, value);
            stm.setString(3, version);
            stm.setString(4, platForm);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public boolean updateGameConfig(String id, String value, String version, String platForm) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement("UPDATE vinplay.game_config SET value=?, version=?, platform=? WHERE id=?");
            stm.setString(1, value);
            stm.setString(2, version);
            stm.setString(3, platForm);
            stm.setString(4, id);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public String getGameCommon(String name) throws SQLException {
        String value = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " SELECT * FROM vinplay.game_config WHERE name = ? ";
            PreparedStatement stm = conn.prepareStatement(" SELECT * FROM vinplay.game_config WHERE name = ? ");
            stm.setString(1, name);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                value = rs.getString("value");
            }
            rs.close();
            stm.close();
        }
        return value;
    }
}

