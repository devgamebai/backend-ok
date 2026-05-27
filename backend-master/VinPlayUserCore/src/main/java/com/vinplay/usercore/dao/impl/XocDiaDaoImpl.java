/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.FreezeModel
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.gamebai.entities.BossXocDiaModel;
import com.vinplay.gamebai.entities.XocDiaBoss;
import com.vinplay.usercore.dao.XocDiaDao;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class XocDiaDaoImpl
implements XocDiaDao {
    @Override
    public boolean saveRoomBoss(XocDiaBoss boss) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL save_xd_boss(?,?,?,?,?)");
            int param = 1;
            call.setString(param++, boss.getSessionId());
            call.setString(param++, boss.getNickname());
            call.setInt(param++, boss.getRoomId());
            call.setString(param++, boss.getRoomSetting());
            call.setLong(param++, boss.getFundInitial());
            call.executeUpdate();
            res = true;
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return res;
    }

    @Override
    public boolean updateRoomBoss(String sessionId, String nickname, long fundInitial, int fee, long revenue, int type) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL update_xd_boss(?,?,?,?,?,?)");
            int param = 1;
            call.setString(param++, sessionId);
            call.setString(param++, nickname);
            call.setLong(param++, fundInitial);
            call.setInt(param++, fee);
            call.setLong(param++, revenue);
            call.setInt(param++, type);
            call.executeUpdate();
            res = true;
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return res;
    }

    @Override
    public XocDiaBoss getRoomBoss(String nickname, int roomId) throws SQLException {
        XocDiaBoss boss = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_boss WHERE `nick_name`=? AND room_id = ? AND `status` = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM xoc_dia_boss WHERE `nick_name`=? AND room_id = ? AND `status` = 1");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                String sessionId = rs.getString("session_id");
                String roomSetting = rs.getString("room_setting");
                long fundInitial = rs.getLong("fund_initial");
                int fee = rs.getInt("fee");
                long revenue = rs.getLong("revenue");
                String createTime = rs.getString("create_time");
                boss = new XocDiaBoss(sessionId, nickname, roomId, roomSetting, fundInitial, 1, fee, revenue, createTime);
            }
            rs.close();
            stm.close();
        }
        return boss;
    }

    @Override
    public Map<Integer, XocDiaBoss> getListRoomBossActive() throws SQLException {
        HashMap<Integer, XocDiaBoss> bossList = new HashMap<Integer, XocDiaBoss>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_boss WHERE `status` = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM xoc_dia_boss WHERE `status` = 1");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String sessionId = rs.getString("session_id");
                String nickname = rs.getString("nick_name");
                int roomId = rs.getInt("room_id");
                String roomSetting = rs.getString("room_setting");
                long fundInitial = rs.getLong("fund_initial");
                int fee = rs.getInt("fee");
                long revenue = rs.getLong("revenue");
                String createTime = rs.getString("create_time");
                XocDiaBoss boss = new XocDiaBoss(sessionId, nickname, roomId, roomSetting, fundInitial, 1, fee, revenue, createTime);
                bossList.put(roomId, boss);
            }
            rs.close();
            stm.close();
        }
        return bossList;
    }

    @Override
    public List<String> getListBossActive() throws SQLException {
        ArrayList<String> bossList = new ArrayList<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_boss WHERE `status` = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM xoc_dia_boss WHERE `status` = 1");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String nickname = rs.getString("nick_name");
                bossList.add(nickname);
            }
            rs.close();
            stm.close();
        }
        return bossList;
    }

    @Override
    public List<String> getListSessionActive() throws SQLException {
        ArrayList<String> sessionList = new ArrayList<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_boss WHERE `status` = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM xoc_dia_boss WHERE `status` = 1");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String sessionId = rs.getString("session_id");
                sessionList.add(sessionId);
            }
            rs.close();
            stm.close();
        }
        return sessionList;
    }

    @Override
    public List<BossXocDiaModel> getListRoomBoss(String nickname, int roomId, int status, int moneyBet) throws SQLException, JSONException {
        ArrayList<BossXocDiaModel> bossList = new ArrayList<BossXocDiaModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_boss WHERE 1 = 1";
            if (nickname != null && !nickname.isEmpty()) {
                sql = sql + " AND nick_name = ?";
            }
            if (roomId >= 0) {
                sql = sql + " AND room_id = ?";
            }
            if (status >= 0) {
                sql = sql + " AND status = ?";
            }
            sql = sql + " ORDER BY create_time DESC";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (nickname != null && !nickname.isEmpty()) {
                stm.setString(param++, nickname);
            }
            if (roomId >= 0) {
                stm.setInt(param++, roomId);
            }
            if (status >= 0) {
                stm.setInt(param++, status);
            }
            ResultSet rs = stm.executeQuery();
            MoneyInGameServiceImpl mnSer = new MoneyInGameServiceImpl();
            while (rs.next()) {
                JSONObject roomSetting = new JSONObject(rs.getString("room_setting"));
                int mnB = roomSetting.getInt("moneyBet");
                if (moneyBet >= 0 && mnB != moneyBet) continue;
                String sessionId = rs.getString("session_id");
                String nn = rs.getString("nick_name");
                int rId = rs.getInt("room_id");
                String password = roomSetting.getString("password");
                String roomName = roomSetting.getString("roomName");
                long fundInitial = rs.getLong("fund_initial");
                long fund = 0L;
                FreezeModel model = mnSer.getFreeze(sessionId);
                if (model != null) {
                    fund = model.getMoney();
                }
                int st = rs.getInt("status");
                int fee = rs.getInt("fee");
                long revenue = rs.getLong("revenue");
                String createTime = rs.getString("create_time");
                BossXocDiaModel boss = new BossXocDiaModel(sessionId, nn, rId, mnB, password, roomName, fundInitial, fund, st, fee, revenue, createTime);
                bossList.add(boss);
            }
            rs.close();
            stm.close();
        }
        return bossList;
    }

    @Override
    public List<com.vinplay.vbee.common.models.minigame.TopWin> getTopAward() throws SQLException {
        java.util.ArrayList<com.vinplay.vbee.common.models.minigame.TopWin> topWins = new java.util.ArrayList<>();
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_history_award WHERE id > 0 AND `created_at` >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) ORDER BY `award` DESC";
            java.sql.PreparedStatement stm = conn.prepareStatement(sql);
            java.sql.ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                com.vinplay.vbee.common.models.minigame.TopWin data = new com.vinplay.vbee.common.models.minigame.TopWin(rs.getString("user_name"), rs.getLong("award"));
                topWins.add(data);
            }
            rs.close();
            stm.close();
        }
        return topWins;
    }

    @Override
    public List<com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia> getHistoryJackpotXocDia() throws SQLException {
        java.util.ArrayList<com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia> histories = new java.util.ArrayList<>();
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM histoty_jackpot_xoc_dia WHERE `id` > 0 ORDER BY id DESC ";
            java.sql.PreparedStatement stm = conn.prepareStatement(sql);
            java.sql.ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Long accumulate = rs.getLong("accumulate");
                java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                Integer dice = rs.getInt("dice");
                String matchId = rs.getString("match_id");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia history = new com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia(sdf.format(createdAt), dice, accumulate, matchId);
                histories.add(history);
            }
            rs.close();
            stm.close();
        }
        return histories;
    }

    @Override
    public List<com.vinplay.vbee.common.models.minigame.TopWin> getTopJackpotAward(String matchId) throws SQLException {
        java.util.ArrayList<com.vinplay.vbee.common.models.minigame.TopWin> topWins = new java.util.ArrayList<>();
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM xoc_dia_jackpot_history_award WHERE id > 0 AND match_id = ? ORDER BY `award` DESC LIMIT 50";
            java.sql.PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, matchId);
            java.sql.ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                com.vinplay.vbee.common.models.minigame.TopWin data = new com.vinplay.vbee.common.models.minigame.TopWin(rs.getString("user_name"), rs.getLong("award"));
                topWins.add(data);
            }
            rs.close();
            stm.close();
        }
        return topWins;
    }
}

