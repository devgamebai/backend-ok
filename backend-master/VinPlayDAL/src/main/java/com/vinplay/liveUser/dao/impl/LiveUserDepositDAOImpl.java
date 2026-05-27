/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.apache.logging.log4j.util.Strings
 */
package com.vinplay.liveUser.dao.impl;

import com.vinplay.liveUser.dao.LiveUserDepositDAO;
import com.vinplay.liveUser.entities.LiveUserDepositEntity;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class LiveUserDepositDAOImpl
implements LiveUserDepositDAO {
    @Override
    public boolean create(LiveUserDepositEntity entity) throws SQLException {
        String sql = "INSERT INTO live_user_game_deposit (nick_name, action_name, cash, msg_success,`type`, fid, run, deposit_at, created_at) VALUE (?,?,?,?,?,?,?,?,?)";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, entity.getNick_name());
            stmt.setString(param++, entity.getAction_name());
            stmt.setInt(param++, entity.getCash());
            stmt.setString(param++, entity.getMsgSuccess());
            stmt.setString(param++, entity.getType());
            stmt.setString(param++, entity.getFid());
            stmt.setBoolean(param++, entity.isRun());
            stmt.setTimestamp(param++, new Timestamp(entity.getDeposit_at().getTime()));
            stmt.setTimestamp(param++, new Timestamp(date.getTime()));
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
    public boolean setRan(int id) throws SQLException {
        String sql = "Update live_user_game_deposit set run = true, last_updated_at = ?  where id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setTimestamp(param++, new Timestamp(date.getTime()));
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

    @Override
    public List<LiveUserDepositEntity> runNow(Date startTime) throws SQLException {
        List<LiveUserDepositEntity> res = new ArrayList();
        String order = " order by ";
        String sort2 = "id desc";
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select * from live_user_game_deposit where run = false and deposit_at <= ?";
            sql = query + order + sort2;
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setTimestamp(1, new Timestamp(startTime.getTime()));
            ResultSet rs = stm.executeQuery();
            res = this.mapResult(rs);
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public int count(String nickname, String type, String startTime, String endTime) throws SQLException {
        int cnt = 0;
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select count(*) as cnt from live_user_game_deposit where 1=1";
            String condition = this.getQuery(nickname, type, startTime, endTime);
            sql = query + condition;
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                cnt = rs.getInt("cnt");
            }
            rs.close();
            stm.close();
        }
        return cnt;
    }

    @Override
    public Long sum(String nickname, String type, String startTime, String endTime) throws SQLException {
        long sum = 0L;
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select sum(cash) as sum from live_user_game_deposit where run = true";
            String condition = this.getQuery(nickname, type, startTime, endTime);
            sql = query + condition;
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                sum = rs.getLong("sum");
            }
            rs.close();
            stm.close();
        }
        return sum;
    }

    private String getQuery(String nickname, String type, String startTime, String endTime) {
        String condition = "";
        if (!Strings.isBlank((String)type)) {
            condition = condition + " AND `type` = '" + type + "'";
        }
        if (startTime != null && !startTime.equals("") && endTime != null && !endTime.equals("")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                Calendar c = Calendar.getInstance();
                c.setTime(format.parse(startTime));
                c.set(10, 0);
                c.set(13, 0);
                c.set(12, 0);
                Calendar ce = Calendar.getInstance();
                ce.setTime(format.parse(endTime));
                ce.set(10, 23);
                ce.set(13, 59);
                ce.set(12, 59);
                condition = condition + " AND created_at BETWEEN '" + format2.format(c.getTime()) + "' AND '" + format2.format(ce.getTime()) + "'";
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (nickname != null && !nickname.equals("")) {
            condition = condition + " AND nick_name like '%" + nickname + "%'";
        }
        return condition;
    }

    @Override
    public List<LiveUserDepositEntity> search(String nickname, String type, String startTime, String endTime, int page, int totalRecord) throws SQLException {
        List<LiveUserDepositEntity> res = new ArrayList();
        String order = " order by ";
        String sort2 = "id desc";
        int num_start = (page - 1) * totalRecord;
        String limit = " LIMIT " + num_start + ", " + totalRecord + "";
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select * from live_user_game_deposit where 1=1";
            String condition = this.getQuery(nickname, type, startTime, endTime);
            sql = query + condition + order + sort2 + limit;
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            res = this.mapResult(rs);
            rs.close();
            stm.close();
        }
        return res;
    }

    private List<LiveUserDepositEntity> mapResult(ResultSet rs) throws SQLException {
        List<LiveUserDepositEntity> res = new ArrayList<LiveUserDepositEntity>();
        while (rs.next()) {
            LiveUserDepositEntity model = new LiveUserDepositEntity();
            model.setId(rs.getInt("id"));
            model.setNick_name(rs.getString("nick_name"));
            model.setAction_name(rs.getString("action_name"));
            model.setMsgSuccess(rs.getString("msg_success"));
            model.setType(rs.getString("type"));
            model.setFid(rs.getString("fid"));
            model.setRun(rs.getBoolean("run"));
            model.setCash(rs.getInt("cash"));
            model.setDeposit_at(rs.getTimestamp("deposit_at"));
            model.setCreated_at(rs.getTimestamp("created_at"));
            model.setLast_updated_at(rs.getTimestamp("last_updated_at"));
            res.add(model);
        }
        return res;
    }
}

