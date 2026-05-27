/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.SendMailDao;
import com.vinplay.usercore.entities.SendMail;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class SendMailDaoImpl
implements SendMailDao {
    private static final Logger logger = Logger.getLogger(SendMailDaoImpl.class);

    @Override
    public List<SendMail> search(String search, int status, int type, String timeStart, String timeEnd, int page, int totalRecord) throws SQLException {
        List<SendMail> res = new ArrayList<SendMail>();
        String order = " order by ";
        String sort2 = "id desc";
        int num_start = (page - 1) * totalRecord;
        String limit = " LIMIT " + num_start + ", " + totalRecord + "";
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select id, title, type, status, created_time, updated_time from send_mails where 1=1";
            String condition = "";
            if (status > 0) {
                condition = condition + " AND status = " + status + "";
            }
            if (type > 0) {
                condition = condition + " AND type = " + type + "";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND create_time BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            if (search != null && !search.equals("")) {
                condition = condition + " AND title like '%" + search + "%'";
            }
            sql = query + condition + order + sort2 + limit;
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            res = this.mapResult(rs, false);
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public int count(String search, int status, int type, String timeStart, String timeEnd) throws SQLException {
        int cnt = 0;
        String order = "";
        String sort2 = "";
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm;
            ResultSet rs;
            String query = "select count(*) as cnt from send_mails where 1=1";
            String condition = "";
            if (status > 0) {
                condition = condition + " AND status = " + status + "";
            }
            if (type > 0) {
                condition = condition + " AND type = " + type + "";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND create_time BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            if (search != null && !search.equals("")) {
                condition = condition + " AND title like '%" + search + "%'";
            }
            if ((rs = (stm = conn.prepareStatement(sql = query + condition + order + sort2)).executeQuery()).next()) {
                cnt = rs.getInt("cnt");
            }
            rs.close();
            stm.close();
        }
        return cnt;
    }

    @Override
    public List<SendMail> getListByType(int type) throws SQLException {
        List<SendMail> res = new ArrayList<SendMail>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM send_mails WHERE type=?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, type);
            ResultSet rs = stm.executeQuery();
            res = this.mapResult(rs, true);
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public SendMail getMail(int id) throws SQLException {
        String sql = "select * from send_mails where id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setInt(param++, id);
            ResultSet rs = stmt.executeQuery();
            List<SendMail> res = this.mapResult(rs, true);
            if (res.size() > 0) {
                SendMail sendMail = res.get(0);
                return sendMail;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return null;
    }

    private List<SendMail> mapResult(ResultSet rs, boolean all) throws SQLException {
        ArrayList<SendMail> res = new ArrayList<SendMail>();
        while (rs.next()) {
            SendMail sendMail = new SendMail();
            sendMail.setId(rs.getInt("id"));
            sendMail.setTitle(rs.getString("title"));
            if (all) {
                sendMail.setMessage(rs.getString("message"));
                sendMail.setExtra_data(rs.getString("extra_data"));
            }
            sendMail.setType(rs.getInt("type"));
            sendMail.setStatus(rs.getInt("status"));
            sendMail.setCreated_at(rs.getString("created_time"));
            sendMail.setUpdated_at(rs.getString("updated_time"));
            res.add(sendMail);
        }
        return res;
    }

    @Override
    public boolean createMail(SendMail mail) throws SQLException {
        String sql = "INSERT INTO send_mails (title, message, extra_data, type, status, created_time) VALUE (?,?,?,?,?,?)";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, mail.getTitle());
            stmt.setString(param++, mail.getMessage());
            stmt.setString(param++, mail.getExtra_data());
            stmt.setInt(param++, mail.getType());
            stmt.setInt(param++, mail.getStatus());
            stmt.setDate(param++, new java.sql.Date(date.getTime()));
            int ex = stmt.executeUpdate();
            stmt.close();
            ok = ex > 0;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return ok;
    }

    @Override
    public boolean updateMail(SendMail mail) throws SQLException {
        String sql = "Update send_mails set  title = ?, message = ?, extra_data = ?, status = ?, updated_time = ? where id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, mail.getTitle());
            stmt.setString(param++, mail.getMessage());
            stmt.setString(param++, mail.getExtra_data());
            stmt.setInt(param++, mail.getStatus());
            stmt.setDate(param++, new java.sql.Date(date.getTime()));
            stmt.setInt(param++, mail.getId());
            int ex = stmt.executeUpdate();
            stmt.close();
            ok = ex > 0;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return ok;
    }

    @Override
    public boolean deleteMail(int id) throws SQLException {
        String sql = "DElete from send_mails where id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setInt(param++, id);
            int ex = stmt.executeUpdate();
            stmt.close();
            ok = ex > 0;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return ok;
    }
}

