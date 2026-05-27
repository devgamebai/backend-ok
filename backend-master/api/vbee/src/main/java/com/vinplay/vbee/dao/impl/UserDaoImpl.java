/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.enums.MissionName
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.MoneyMessageInMinigame
 *  com.vinplay.vbee.common.messages.SafeMoneyMessage
 *  com.vinplay.vbee.common.messages.VippointMessage
 *  com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.userMission.MissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.UserUtil
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.vbee.dao.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.enums.MissionName;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.messages.SafeMoneyMessage;
import com.vinplay.vbee.common.messages.VippointMessage;
import com.vinplay.vbee.common.messages.userMission.LogReceivedRewardMissionMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.userMission.MissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.UserUtil;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vbee.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class UserDaoImpl
        implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);
    private long fee = 0L;

    @Override
    public List<Long> getMoneyUser(int userId, String moneyType) throws SQLException {
        ArrayList<Long> moneyUser = new ArrayList<Long>();
        long money = 0L;
        long moneyTotal = 0L;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT " + moneyType + ", " + moneyType + "_total FROM users WHERE id=?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, userId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                money = rs.getLong(moneyType);
                moneyTotal = rs.getLong(String.valueOf(moneyType) + "_total");
            }
            rs.close();
            stm.close();
        }catch (Exception e){

        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        moneyUser.add(money);
        moneyUser.add(moneyTotal);
        return moneyUser;
    }

    public String getRefCodeByUserId(int userId) throws SQLException {
        // Column in vinplay.users is `referral_code`, not `ref_code`.
        // The legacy name was breaking every log_money_user RMQ message → DLQ.
        String ref_code = "";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try (PreparedStatement stm = conn.prepareStatement("SELECT referral_code FROM vinplay.users WHERE id = ?")) {
            stm.setInt(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    ref_code = rs.getString("referral_code");
                    if (ref_code == null) ref_code = "";
                }
            }
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return ref_code;
    }

    public void UpdateIndexesAffiliate(String nick_name, String field,long money_exchange){
        try {
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
            try {
                PreparedStatement call = null;
                call = conn.prepareStatement("call vinplay.UpdateIndexsAffiliate (?,?,?)");
                call.setString(1, nick_name);
                call.setString(2, field);
                call.setLong(3, money_exchange);
                call.executeUpdate();
                if (call != null) {
                    call.close();
                }
            }finally {
                ConnectionPool.getInstance().releaseConnection(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void UpdateIndexesAffiliateMD5(String nick_name, long money_exchange){
        try {
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
            try {
                PreparedStatement call = null;
                call = conn.prepareStatement("call vinplay.UpdateIndexsAffiliateMD5 (?,?)");
                call.setString(1, nick_name);
                call.setLong(3, money_exchange);
                call.executeUpdate();
                if (call != null) {
                    call.close();
                }
            }finally {
                ConnectionPool.getInstance().releaseConnection(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getFreezeMoney(String sessionId) throws SQLException {
        long money = 0L;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT money FROM freeze_money WHERE session_id=?";
            PreparedStatement stm = conn.prepareStatement("SELECT money FROM freeze_money WHERE session_id=?");
            stm.setString(1, sessionId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                money = rs.getLong("money");
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return money;
    }

    @Override
    public long getSafeMoney(int userId) throws SQLException {
        long money = 0L;
        // SUN-1340: was missing finally — conn leaked on any SQLException.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement("SELECT 0 AS safe FROM users WHERE id=?")) {
            stm.setInt(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    money = 0L;
                }
            }
        }
        return money;
    }

    @Override
    public boolean safeMoney(SafeMoneyMessage message) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            CallableStatement call = null;
            // 5th param (p_money) added in migration 20260515_atomic_money_procs.sql:
            // signed delta on users.vin (vin->safe sends negative, safe->vin positive).
            // Proc applies it as `vin = vin + p_money` under a CAS guard.
            call = conn.prepareCall("CALL safe_money(?,?,?,?,?)");
            int param = 1;
            call.setInt(param++, message.getUserId());
            call.setLong(param++, message.getAfterMoneyUse());
            call.setLong(param++, message.getAfterMoney());
            call.setLong(param++, message.getSafeMoney());
            call.setLong(param++, message.getMoneyExchange());
            call.executeUpdate();
            if (call != null) {
                call.close();
            }
        }catch (Exception e){
            logger.error("safe_money CALL failed userId=" + message.getUserId()
                    + " delta=" + message.getMoneyExchange()
                    + " safe=" + message.getSafeMoney(), e);
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return true;
    }

    @Override
    public boolean updateMoney(MoneyMessageInMinigame message, int type) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_money_user(?,?,?,?,?,?,?,?,?,?)");
            int param = 1;
            call.setInt(param++, message.getUserId());
            call.setLong(param++, message.getMoneyExchange());
            call.setLong(param++, message.getAfterMoneyUse());
            call.setLong(param++, message.getAfterMoney());
            call.setString(param++, message.getMoneyType());
            call.setLong(param++, message.getFee());
            call.setString(param++, message.getActionName());
            call.setInt(param++, message.getMoneyVP());
            call.setInt(param++, message.getVp());
            call.setInt(param++, type);
            call.executeUpdate();
            if (call != null) {
                call.close();
            }
        }catch (Exception e){
            logger.error("update_money_user CALL failed userId=" + message.getUserId()
                    + " delta=" + message.getMoneyExchange()
                    + " action=" + message.getActionName()
                    + " type=" + type, e);
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return true;
    }

    @Override
    public boolean updateVP(VippointMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_vippoint(?,?,?)");
            int param = 1;
            call.setInt(param++, message.getUserId());
            call.setInt(param++, message.getMoneyVP());
            call.setInt(param++, message.getVp());
            success = call.executeUpdate() == 1;
            if (call != null) {
                call.close();
            }
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }

    @Override
    public boolean updateUserMission(String nickName, String missionName, String moneyType, int matchWin) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String tableName = "";
            tableName = moneyType.equals("vin") ? "user_mission_vin" : "user_mission_xu";
            String sql = " UPDATE " + tableName + " SET match_win = ?,      update_time = ?  WHERE nick_name = ?    AND mission_name = ? ";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, matchWin);
            stm.setString(2, DateTimeUtils.getCurrentTime());
            stm.setString(3, nickName);
            stm.setString(4, missionName);
            stm.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return false;
    }

    @Override
    public void logReceivedRewardMission(LogReceivedRewardMissionMessage message) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_received_reward_mission");
        Document doc = new Document();
        doc.append("user_id", (Object)message.getUserId());
        doc.append("user_name", (Object)message.getUserName());
        doc.append("nick_name", (Object)message.getNickName());
        doc.append("game_name", (Object)message.getMissionName());
        doc.append("level_received_reward", (Object)message.getLevelReceivedReward());
        doc.append("money_bonus", (Object)message.getMoneyBonus());
        doc.append("money_user", (Object)message.getMoneyUser());
        doc.append("money_type", (Object)message.getMoneyType());
        doc.append("time_log", (Object)DateTimeUtils.getCurrentTime((String)"yyyy-MM-dd HH:mm:ss"));
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public UserMissionCacheModel getUserMission(String nickName, String moneyType) throws Exception {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        UserMissionCacheModel response = new UserMissionCacheModel();
        ArrayList<MissionObj> listMissionObjResponse = new ArrayList<MissionObj>();
        boolean completeMission = false;
        boolean completeAllLevel = false;
        try {
            int maxLevel = GameCommon.getValueInt((String)"MAX_LEVEL_MISSION");
            String tableName = "";
            tableName = moneyType.equals("vin") ? "user_mission_vin" : "user_mission_xu";
            String sql = " SELECT user_id,         user_name,         nick_name,         mission_name,         level,         match_win,         match_max,         received_reward_level  FROM " + tableName + " WHERE nick_name = ? ";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, nickName);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                completeMission = rs.getInt("match_win") >= rs.getInt("match_max");
                completeAllLevel = rs.getInt("level") == maxLevel && rs.getInt("match_win") == rs.getInt("match_max");
                MissionObj missionObj = new MissionObj(rs.getString("mission_name"), rs.getInt("level"), rs.getInt("match_win"), rs.getInt("match_max"), completeMission, completeAllLevel, rs.getInt("received_reward_level"));
                listMissionObjResponse.add(missionObj);
                response.setUserId(rs.getInt("user_id"));
                response.setUserName(rs.getString("user_name"));
                response.setNickName(rs.getString("nick_name"));
            }
            response.setListMission(listMissionObjResponse);
            rs.close();
            stm.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return response;
    }

    @Override
    public UserModel getUserByNickName(String nickname) throws SQLException {
        UserModel user = null;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT * FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM users WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                user = UserUtil.parseResultSetToUserModel((ResultSet) rs);
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return user;
    }

    @Override
    public void insertUserMission(String moneyType, MissionObj mission, UserModel user) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String tableName = "";
            tableName = moneyType.equals("vin") ? "user_mission_vin" : "user_mission_xu";
            String sql = " INSERT INTO " + tableName + " (user_id, user_name, nick_name, mission_name, level, match_win, match_max, received_reward_level, create_time, update_time)  VALUES  (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, user.getId());
            stm.setString(2, user.getUsername());
            stm.setString(3, user.getNickname());
            stm.setString(4, mission.getMisNa());
            stm.setInt(5, mission.getMisLev());
            stm.setInt(6, mission.getMisWin());
            stm.setInt(7, mission.getMisMax());
            stm.setInt(8, mission.getRecReLev());
            stm.setString(9, DateTimeUtils.getCurrentTime());
            stm.setString(10, DateTimeUtils.getCurrentTime());
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public long getFeeUser(String nickname, String timeEnd) throws Exception {
        try {
            this.fee = 0L;
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject obj = new BasicDBObject();
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            Document conditions = new Document();
            conditions.put("nick_name", (Object)nickname);
            obj.put("$gte", (Object)DateTimeUtils.getFormatTime((String)"yyyy-MM-dd HH:mm:ss", (Date)DateTimeUtils.getStartTimeThisMonth()));
            obj.put("$lte", (Object)timeEnd);
            conditions.put("trans_time", (Object)obj);
            FindIterable iterable = null;
            iterable = db.getCollection("log_money_user_vin").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    UserDaoImpl this$0 = UserDaoImpl.this;
                    UserDaoImpl.access$1(this$0, this$0.fee + document.getLong((Object)"fee"));
                }
            });
            return this.fee;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public UserMissionCacheModel initUserMission(String nickName, String moneyType, String matchMax, List<String> bonusVin) throws Exception {
        try {
            String cacheName = "";
            cacheName = moneyType.equals("vin") ? "cacheUserMissionVin" : "cacheUserMissionXu";
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap userMissionMap = client.getMap(cacheName);
            UserMissionCacheModel user = new UserMissionCacheModel();
            String[] matchMaxArr = matchMax.split(",");
            UserMissionCacheModel response = new UserMissionCacheModel();
            ArrayList<MissionObj> listMission = new ArrayList<MissionObj>();
            UserDaoImpl dao = new UserDaoImpl();
            try {
                UserModel userDao = dao.getUserByNickName(nickName);
                if (userDao == null) {
                    return null;
                }
                for (int i = 0; i < bonusVin.size(); ++i) {
                    MissionObj obj = new MissionObj(MissionName.getMissionById((int)i).getName(), 1, 0, Integer.parseInt(matchMaxArr[0]), false, false, 0);
                    listMission.add(obj);
                    dao.insertUserMission(moneyType, obj, userDao);
                }
                user.setLastActive(new Date());
                user.setLastMessageId(Long.parseLong(VinPlayUtils.genMessageId()));
                user.setListMission(listMission);
                user.setNickName(nickName);
                user.setUserId(userDao.getId());
                user.setUserName(userDao.getUsername());
                userMissionMap.put((Object)nickName, (Object)user);
                response.setLastActive(new Date());
                response.setLastMessageId(Long.parseLong(VinPlayUtils.genMessageId()));
                response.setListMission(listMission);
                response.setNickName(userDao.getNickname());
                response.setUserId(userDao.getId());
                response.setUserName(userDao.getUsername());
            }
            catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            return response;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            throw e2;
        }
    }

    static void access$1(UserDaoImpl userDaoImpl, long fee) {
        userDaoImpl.fee = fee;
    }

}

