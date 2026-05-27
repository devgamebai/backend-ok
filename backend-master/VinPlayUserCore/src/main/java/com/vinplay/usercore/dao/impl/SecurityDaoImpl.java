/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.dao.SecurityDao;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import org.bson.Document;
import org.bson.conversions.Bson;

public class SecurityDaoImpl
implements SecurityDao {
    @Override
    public boolean checkEmail(String email) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT COUNT(*) as cnt FROM users WHERE email=?";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM users WHERE email=?");
            stm.setString(1, email);
            ResultSet rs = stm.executeQuery();
            if (rs.next() && rs.getInt("cnt") == 1) {
                res = true;
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return res;
    }

    @Override
    public boolean checkMobile(String mobile) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT COUNT(*) as cnt FROM users WHERE mobile=?";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM users WHERE mobile=?");
            stm.setString(1, mobile);
            ResultSet rs = stm.executeQuery();
            if (rs.next() && rs.getInt("cnt") == 1) {
                res = true;
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return res;
    }

    @Override
    public boolean checkIdentification(String identification) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT COUNT(*) as cnt FROM users WHERE identification=?";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM users WHERE identification=?");
            stm.setString(1, identification);
            ResultSet rs = stm.executeQuery();
            if (rs.next() && rs.getInt("cnt") == 1) {
                res = true;
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return res;
    }

    @Override
    public boolean updateUserInfo(int userId, String value, int type) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL update_user_info(?,?,?)");
            int param = 1;
            call.setInt(param++, userId);
            call.setString(param++, value);
            call.setInt(param++, type);
            res = call.executeUpdate() == 1;
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
    public boolean updateUserInfos(int userId, String identification, String email, String mobile) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "UPDATE users SET identification=?, email=?, mobile=? WHERE id=?";
            PreparedStatement stm = conn.prepareStatement("UPDATE users SET identification=?, email=?, mobile=? WHERE id=?");
            stm.setString(1, identification);
            stm.setString(2, email);
            stm.setString(3, mobile);
            stm.setInt(4, userId);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return res;
    }

    @Override
    public boolean updateUserVipInfo(int userId, String birthday, boolean gender, String address) throws SQLException, ParseException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "UPDATE users SET birthday=?, gender=?, address=? WHERE id=?";
            PreparedStatement stm = conn.prepareStatement("UPDATE users SET birthday=?, gender=?, address=? WHERE id=?");
            stm.setDate(1, new Date(VinPlayUtils.getDateTimeFromDate((String)birthday).getTime()));
            stm.setBoolean(2, gender);
            stm.setString(3, address);
            stm.setInt(4, userId);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return res;
    }

    @Override
    public UserModel getStatus(String nickname) throws SQLException {
        UserModel user = null;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT status,id FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT status,id FROM users WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                user = new UserModel();
                user.setStatus(rs.getInt("status"));
                user.setId(rs.getInt("id"));
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return user;
    }

    @Override
    public boolean updateNewMobile(String nickname, String mobile, String mobileOld) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_new_mobile");
        Document doc = new Document();
        doc.append("nick_name", nickname);
        doc.append("mobile", mobile);
        doc.append("mobile_old", mobileOld);
        doc.append("update_time", VinPlayUtils.getCurrentDateTime());
        col.insertOne(doc);
        return true;
    }

    @Override
    public boolean checkNewMobile(String mobile) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("mobile", mobile);
        FindIterable iterable = db.getCollection("user_new_mobile").find((Bson)conditions).limit(1);
        Document dc = iterable != null ? (Document)iterable.first() : null;
        return dc != null;
    }

    @Override
    public boolean updateInfoLogin(int userId, String username, String nickname, String ip, String agent, int type) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_login_info");
        Document doc = new Document();
        doc.append("user_id", userId);
        doc.append("user_name", username);
        doc.append("nick_name", nickname);
        doc.append("ip", ip);
        doc.append("agent", agent);
        doc.append("type", type);
        doc.append("time_log", VinPlayUtils.getCurrentDateTime());
        col.insertOne(doc);
        return true;
    }
}

