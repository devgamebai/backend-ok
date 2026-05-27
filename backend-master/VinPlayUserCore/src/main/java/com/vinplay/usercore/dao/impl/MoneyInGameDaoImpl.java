/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.FreezeModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.UserUtil
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.dao.MoneyInGameDao;
import com.vinplay.usercore.entities.LogTransferAgentModel;
import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.UserUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MoneyInGameDaoImpl
implements MoneyInGameDao {
    @Override
    public FreezeModel getFreezeMoneyAgentTranferBySessionId(String sessionId) throws SQLException {
        FreezeModel response = new FreezeModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " SELECT user_id, nick_name, game_name, room_id, money, money_type, create_time, status  FROM vinplay.freeze_money  WHERE session_id = ? ";
            PreparedStatement stm = conn.prepareStatement(" SELECT user_id, nick_name, game_name, room_id, money, money_type, create_time, status  FROM vinplay.freeze_money  WHERE session_id = ? ");
            stm.setString(1, sessionId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                response.setSessionId(sessionId);
                response.setNickname(rs.getString("nick_name"));
                response.setGameName(rs.getString("game_name"));
                response.setRoomId("room_id");
                response.setMoney(rs.getLong("money"));
                response.setMoneyType(rs.getString("money_type"));
                response.setUserId(rs.getInt("user_id"));
                response.setCreateTime((java.util.Date)rs.getDate("create_time"));
                response.setStatus(rs.getInt("status"));
            }
            rs.close();
            stm.close();
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document conditions = new Document();
            conditions.put("session_id_freeze_money", sessionId);
            Document dc = (Document)db.getCollection("log_chuyen_tien_dai_ly").find((Bson)conditions).first();
            if (dc != null) {
                response.setTransNo(dc.getString("transaction_no"));
            }
        }
        return response;
    }

    @Override
    public String getNickNameFreezeMoneyAgentTranferBySessionId(String sessionId) throws SQLException {
        String response = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " SELECT nick_name  FROM vinplay.freeze_money  WHERE session_id = ? ";
            PreparedStatement stm = conn.prepareStatement(" SELECT nick_name  FROM vinplay.freeze_money  WHERE session_id = ? ");
            stm.setString(1, sessionId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                response = rs.getString("nick_name");
            }
            rs.close();
            stm.close();
        }
        return response;
    }

    @Override
    public List<FreezeModel> getListFreezeMoneyAgentTranfer(String gameName, String nickName, String moneyType, String startTime, String endTime, int page, String status) throws SQLException {
        ArrayList<FreezeModel> response = new ArrayList<FreezeModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " SELECT *  FROM vinplay.freeze_money  WHERE 1 = 1 ";
            String condition = "";
            int numStart = (page - 1) * 50;
            int numEnd = numStart + 50;
            if (gameName != null && !gameName.isEmpty()) {
                condition = condition + " AND game_name = '" + gameName + "' ";
            }
            if (nickName != null && !nickName.isEmpty()) {
                condition = condition + " AND nick_name = '" + nickName + "' ";
            }
            if (moneyType != null && !moneyType.isEmpty()) {
                condition = condition + " AND money_type = '" + moneyType + "' ";
            }
            if (startTime != null && !startTime.isEmpty()) {
                condition = condition + " AND create_time >= '" + startTime + "' ";
            }
            if (endTime != null && !endTime.isEmpty()) {
                condition = condition + " AND create_time <= '" + endTime + "' ";
            }
            if (status != null && !status.isEmpty()) {
                condition = condition + " AND status = " + status;
            }
            sql = sql + condition + " ORDER BY create_time DESC ";
            sql = sql + " LIMIT " + numStart + ", " + numEnd;
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                FreezeModel freezeModel = new FreezeModel();
                freezeModel.setSessionId(rs.getString("session_id"));
                freezeModel.setNickname(rs.getString("nick_name"));
                freezeModel.setGameName(rs.getString("game_name"));
                freezeModel.setRoomId(rs.getString("room_id"));
                freezeModel.setMoney(rs.getLong("money"));
                freezeModel.setMoneyType(rs.getString("money_type"));
                freezeModel.setUserId(rs.getInt("user_id"));
                String strCreateTime = rs.getString("create_time");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    java.util.Date createTime = format.parse(strCreateTime);
                    freezeModel.setCreateTime(createTime);
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }
                freezeModel.setStatus(rs.getInt("status"));
                response.add(freezeModel);
            }
            rs.close();
            stm.close();
        }
        return response;
    }

    @Override
    public UserCacheModel getUserByNickName(String nickName) throws SQLException {
        UserCacheModel response = new UserCacheModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT id, vin, 0 AS vin_total, 0 AS safe FROM vinplay.users WHERE nick_name = ?";
            PreparedStatement stm = conn.prepareStatement("SELECT id, vin, 0 AS vin_total, 0 AS safe FROM vinplay.users WHERE nick_name = ?");
            stm.setString(1, nickName);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                response.setId(rs.getInt("id"));
                response.setVin(rs.getLong("vin"));
                response.setVinTotal(0L);
                response.setSafe(0L);
            }
            rs.close();
            stm.close();
        }
        return response;
    }

    @Override
    public boolean updateSafeMoney(long safeMoney, long userId) throws SQLException {
        // Phase 2 (money_ledger): route through MoneyGateway.setCurrencyAbsolute
        // so the safe write goes through the same audit + dedup + dual-write
        // pipeline as vin.  Legacy callers passed the new absolute value
        // pre-computed from cache state, so use the set-absolute helper rather
        // than convert delta semantics.  txId=null because the existing
        // call sites (FreezeMoneyTranferAgent path in MoneyInGameServiceImpl /
        // MoneyInGameServiceSub) don't carry a transaction id — the gateway
        // accepts a null txId and skips dedup, matching the old behaviour.
        // user lookup: nickname is required for cache push — use a direct
        // single-row SELECT rather than instantiate a UserService just to read
        // the nick_name column.
        String nickname = lookupNickname(userId);
        if (nickname == null) return false;
        com.vinplay.dal.service.MoneyGateway.CreditResult r =
                com.vinplay.dal.service.MoneyGateway.setCurrencyAbsolute(
                        userId, nickname,
                        com.vinplay.vbee.common.statics.Consts.MONEY_SAFE,
                        safeMoney,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_SAFE_FREEZE_DRAIN,
                        null,
                        "MoneyInGameDao.updateSafeMoney");
        return r != null && r.success;
    }

    /**
     * Look up users.nick_name for a given user id.  Used by the gateway-routed
     * setSafeAbsolute / setMoneyVpAbsolute paths to satisfy MoneyGateway's
     * required nickname parameter.
     */
    private static String lookupNickname(long userId) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement("SELECT nick_name FROM vinplay.users WHERE id = ?")) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getString("nick_name");
            }
        }
        return null;
    }

    @Override
    public List<FreezeModel> getAllFreeze() throws SQLException {
        ArrayList<FreezeModel> res = new ArrayList<FreezeModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM freeze_money WHERE status = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM freeze_money WHERE status = 1");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                FreezeModel model = UserUtil.parseResultSetToFreezeModel((ResultSet)rs);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public FreezeModel getFreeze(String sessionId) throws SQLException {
        FreezeModel model = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM freeze_money WHERE session_id=? AND status = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM freeze_money WHERE session_id=? AND status = 1");
            stm.setString(1, sessionId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                model = UserUtil.parseResultSetToFreezeModel((ResultSet)rs);
            }
            rs.close();
            stm.close();
        }
        return model;
    }

    @Override
    public boolean updateVippoint(String nickname, int vp, int moneyVP) throws SQLException {
        // Two concerns in one call:
        //   - vip_point / vip_point_save → game-progression counters (delta)
        //   - money_vp                   → actual currency balance (absolute)
        // Both routed through MoneyGateway: addLoyaltyPoints (counters) and
        // setCurrencyAbsolute (money_vp). Direct UPDATE removed so the
        // canonical-gateway build guard sees no exceptions in this file.
        long userId = lookupUserIdByNickname(nickname);
        if (userId <= 0) return false;
        if (!com.vinplay.dal.service.MoneyGateway.addLoyaltyPoints(userId, nickname, vp, vp)) return false;
        com.vinplay.dal.service.MoneyGateway.CreditResult r =
                com.vinplay.dal.service.MoneyGateway.setCurrencyAbsolute(
                        userId, nickname,
                        com.vinplay.vbee.common.statics.Consts.MONEY_VP,
                        moneyVP,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_VIPPOINT_UPDATE,
                        null,
                        "MoneyInGameDao.updateVippoint");
        return r != null && r.success;
    }

    @Override
    public boolean updateVippointAgent(String nickname, int vp, int vpSave, int moneyVP) throws SQLException {
        // Same as updateVippoint but vp_save can differ from vp.
        long userId = lookupUserIdByNickname(nickname);
        if (userId <= 0) return false;
        if (!com.vinplay.dal.service.MoneyGateway.addLoyaltyPoints(userId, nickname, vp, vpSave)) return false;
        com.vinplay.dal.service.MoneyGateway.CreditResult r =
                com.vinplay.dal.service.MoneyGateway.setCurrencyAbsolute(
                        userId, nickname,
                        com.vinplay.vbee.common.statics.Consts.MONEY_VP,
                        moneyVP,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_VIPPOINT_UPDATE,
                        null,
                        "MoneyInGameDao.updateVippointAgent");
        return r != null && r.success;
    }

    /**
     * Look up users.id for a given nickname.  Used by the gateway-routed
     * money_vp updates which require numeric user_id.
     */
    private static long lookupUserIdByNickname(String nickname) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement("SELECT id FROM vinplay.users WHERE nick_name = ?")) {
            stm.setString(1, nickname);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        return 0L;
    }

    @Override
    public boolean restoreAllGame(List<String> sessionBlockList) throws SQLException {
        return com.vinplay.dal.service.MoneyGateway.systemRecoveryReset(sessionBlockList) > 0;
    }

    @Override
    public LogTransferAgentModel getMoneyAgentTranferBySessionId(String sessionId) throws SQLException {
        LogTransferAgentModel response = new LogTransferAgentModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " SELECT * FROM vinplay.log_tranfer_agent WHERE session_id_freeze_money = ? ";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, sessionId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                response.setAgent_level1(rs.getString("agent_level1"));
                response.setId(rs.getInt("id"));
                response.setMoney_receive(rs.getLong("money_receive"));
                response.setMoney_send(rs.getLong("money_send"));
                response.setNick_name_receive(rs.getString("nick_name_receive"));
                response.setNick_name_send(rs.getString("nick_name_send"));
                response.setTransaction_no(rs.getString("transaction_no"));
            }
            rs.close();
            stm.close();
        }
        return response;
    }
}

