/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.enums.vippoint.EventVPTopIntelPrize
 *  com.vinplay.vbee.common.enums.vippoint.EventVPTopStrongPrize
 *  com.vinplay.vbee.common.models.vippoint.EventVPBonusModel
 *  com.vinplay.vbee.common.models.vippoint.UserVPEventModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.UserUtil
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.dao.VippointDao;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.enums.vippoint.EventVPTopIntelPrize;
import com.vinplay.vbee.common.enums.vippoint.EventVPTopStrongPrize;
import com.vinplay.vbee.common.models.vippoint.EventVPBonusModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.UserUtil;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vippoint.entiies.EventVPMapModel;
import com.vinplay.vippoint.entiies.EventVPTopIntelModel;
import com.vinplay.vippoint.entiies.EventVPTopStrongModel;
import com.vinplay.vippoint.entiies.EventVPTopVipModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;

public class VippointDaoImpl
implements VippointDao {
    @Override
    public List<EventVPBonusModel> getEventVPBonus() throws SQLException {
        ArrayList<EventVPBonusModel> res = new ArrayList<EventVPBonusModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM event_vp";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM event_vp");
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                EventVPBonusModel model = new EventVPBonusModel(rs.getString("name"), rs.getInt("value"), rs.getInt("num"), rs.getInt("use"));
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean updateEventVPBonus(int value) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "UPDATE event_vp SET `use` = `use` + 1, `update_time` = now() WHERE `value`=?";
            PreparedStatement stm = conn.prepareStatement("UPDATE event_vp SET `use` = `use` + 1, `update_time` = now() WHERE `value`=?");
            stm.setInt(1, value);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public int getVPEvent(String nickname) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT vp_event FROM users_vp_event WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT vp_event FROM users_vp_event WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getInt("vp_event");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public int getVPEventReal(String nickname) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT vp_real FROM users_vp_event WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT vp_real FROM users_vp_event WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getInt("vp_real");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<EventVPMapModel> getEventMaps() throws SQLException {
        ArrayList<EventVPMapModel> res = new ArrayList<EventVPMapModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            for (int i = 0; i <= VippointUtils.PLACES.size() - 1; ++i) {
                int min = VippointUtils.PLACES.get(i);
                int place = i + 1;
                EventVPMapModel model = new EventVPMapModel();
                model.setPlace(place);
                model.setMin(min);
                String sql = "SELECT * FROM users_vp_event WHERE place = ? ORDER BY vp_event DESC LIMIT 1";
                PreparedStatement stm = conn.prepareStatement("SELECT * FROM users_vp_event WHERE place = ? ORDER BY vp_event DESC LIMIT 1");
                stm.setInt(1, place);
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    model.setNickname(rs.getString("nick_name"));
                    model.setSubVippoint(rs.getInt("vp_sub"));
                    model.setVippoint(rs.getInt("vp_event"));
                    model.setAvatar(VippointUtils.getAvatar(model.getNickname()));
                }
                res.add(model);
                rs.close();
                stm.close();
            }
        }
        return res;
    }

    @Override
    public List<EventVPTopIntelModel> getEventIntel() throws SQLException {
        ArrayList<EventVPTopIntelModel> res = new ArrayList<EventVPTopIntelModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT nick_name, vp_event, num_add, place_max FROM users_vp_event WHERE place_max > 0 ORDER BY vp_event DESC, num_add DESC, place_max DESC LIMIT 50";
            PreparedStatement stm = conn.prepareStatement("SELECT nick_name, vp_event, num_add, place_max FROM users_vp_event WHERE place_max > 0 ORDER BY vp_event DESC, num_add DESC, place_max DESC LIMIT 50");
            ResultSet rs = stm.executeQuery();
            int i = 1;
            while (rs.next()) {
                EventVPTopIntelModel model = new EventVPTopIntelModel();
                model.setStt(i);
                model.setNickname(rs.getString("nick_name"));
                model.setVippoint(rs.getInt("vp_event"));
                model.setBonus(rs.getInt("num_add"));
                model.setPlace(rs.getInt("place_max"));
                EventVPTopIntelPrize pz = EventVPTopIntelPrize.getById((int)i);
                String prize = pz.getValue();
                String prizeVin = pz.getValueVin();
                model.setPrize(prize);
                model.setPrizeVin(prizeVin);
                res.add(model);
                ++i;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<EventVPTopStrongModel> getEventStrong() throws SQLException {
        ArrayList<EventVPTopStrongModel> res = new ArrayList<EventVPTopStrongModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT nick_name, vp_sub, num_sub, place_max FROM users_vp_event WHERE place_max > 0 ORDER BY vp_sub DESC, num_sub DESC, place_max DESC LIMIT 20";
            PreparedStatement stm = conn.prepareStatement("SELECT nick_name, vp_sub, num_sub, place_max FROM users_vp_event WHERE place_max > 0 ORDER BY vp_sub DESC, num_sub DESC, place_max DESC LIMIT 20");
            ResultSet rs = stm.executeQuery();
            int i = 1;
            while (rs.next()) {
                EventVPTopStrongModel model = new EventVPTopStrongModel();
                model.setStt(i);
                model.setNickname(rs.getString("nick_name"));
                model.setVippointSub(rs.getInt("vp_sub"));
                model.setCount(rs.getInt("num_sub"));
                model.setPlace(rs.getInt("place_max"));
                EventVPTopStrongPrize pz = EventVPTopStrongPrize.getById((int)i);
                String prize = pz.getValue();
                String prizeVin = pz.getValueVin();
                model.setPrize(prize);
                model.setPrizeVin(prizeVin);
                res.add(model);
                ++i;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<EventVPTopVipModel> getEventVips() throws SQLException {
        ArrayList<EventVPTopVipModel> res = new ArrayList<EventVPTopVipModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM users_vp_event ORDER BY vp_event DESC LIMIT 10";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM users_vp_event ORDER BY vp_event DESC LIMIT 10");
            ResultSet rs = stm.executeQuery();
            int i = 1;
            while (rs.next()) {
                EventVPTopVipModel model = new EventVPTopVipModel();
                model.setStt(i);
                model.setNickname(rs.getString("nick_name"));
                model.setVippoint(rs.getInt("vp_event"));
                res.add(model);
                ++i;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public UserVPEventModel getUserVPByNickName(String nickname) throws SQLException {
        UserVPEventModel user = new UserVPEventModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM users_vp_event WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM users_vp_event WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                user = UserUtil.parseResultSetToUserVPEventModel((ResultSet)rs);
            }
            rs.close();
            stm.close();
        }
        return user;
    }

    @Override
    public int getEventIntelIndex(int vpEvent, int numAdd, int placeMax) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT COUNT(*) as cnt FROM users_vp_event WHERE vp_event > ? OR (vp_event = ? AND num_add > ?) OR (vp_event = ? AND num_add = ? AND place_max > ?)";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM users_vp_event WHERE vp_event > ? OR (vp_event = ? AND num_add > ?) OR (vp_event = ? AND num_add = ? AND place_max > ?)");
            stm.setInt(1, vpEvent);
            stm.setInt(2, vpEvent);
            stm.setInt(3, numAdd);
            stm.setInt(4, vpEvent);
            stm.setInt(5, numAdd);
            stm.setInt(6, placeMax);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getInt("cnt") + 1;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public int getEventStrongIndex(int vpSub, int numSub, int placeMax) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT COUNT(*) as cnt FROM users_vp_event WHERE vp_sub > ? OR (vp_sub = ? AND num_sub > ?) OR (vp_sub = ? AND num_sub = ? AND place_max > ?)";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM users_vp_event WHERE vp_sub > ? OR (vp_sub = ? AND num_sub > ?) OR (vp_sub = ? AND num_sub = ? AND place_max > ?)");
            stm.setInt(1, vpSub);
            stm.setInt(2, vpSub);
            stm.setInt(3, numSub);
            stm.setInt(4, vpSub);
            stm.setInt(5, numSub);
            stm.setInt(6, placeMax);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getInt("cnt") + 1;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public int getEventVipsIndex(int vpEvent) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT COUNT(*) as cnt FROM users_vp_event WHERE vp_event > ?";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM users_vp_event WHERE vp_event > ?");
            stm.setInt(1, vpEvent);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getInt("cnt") + 1;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean logVippointEvent(String nickname, int type, int value, int isBot) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_vippoint_event");
        Document doc = new Document();
        doc.append("nick_name", nickname);
        doc.append("value", value);
        doc.append("type", type);
        doc.append("is_bot", isBot);
        doc.append("time_log", VinPlayUtils.getCurrentDateTime());
        doc.append("create_time", new Date());
        col.insertOne(doc);
        return true;
    }

    @Override
    public int getNumRunInDay(String date, int type) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT num_run FROM event_vp_lucky WHERE date_run=? AND type=?";
            PreparedStatement stm = conn.prepareStatement("SELECT num_run FROM event_vp_lucky WHERE date_run=? AND type=?");
            stm.setString(1, date);
            stm.setInt(2, type);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getInt("num_run");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean updateNumInDay(String date, int type) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "INSERT INTO event_vp_lucky (date_run, type, num_run, update_time) VALUES(?, ?, 1, now()) ON DUPLICATE KEY UPDATE num_run = num_run + 1, update_time = now()";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO event_vp_lucky (date_run, type, num_run, update_time) VALUES(?, ?, 1, now()) ON DUPLICATE KEY UPDATE num_run = num_run + 1, update_time = now()");
            stm.setString(1, date);
            stm.setInt(2, type);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public boolean resetEvent() throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql1 = "UPDATE vinplay.event_vp SET `use` = 0 WHERE 1 = 1";
            String sql2 = "TRUNCATE TABLE vinplay.users_vp_event";
            PreparedStatement stm1 = conn.prepareStatement("UPDATE vinplay.event_vp SET `use` = 0 WHERE 1 = 1");
            PreparedStatement stm2 = conn.prepareStatement("TRUNCATE TABLE vinplay.users_vp_event");
            stm1.executeUpdate();
            stm2.executeUpdate();
            res = true;
            stm1.close();
            stm2.close();
        }
        return res;
    }
}

