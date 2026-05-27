/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.apache.logging.log4j.util.Strings
 */
package com.vinplay.liveUser.dao.impl;

import com.vinplay.liveUser.dao.LiveUserGameDAO;
import com.vinplay.liveUser.entities.LiveUserGameEntity;
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

public class LiveUserGameDAOImpl
implements LiveUserGameDAO {
    @Override
    public LiveUserGameEntity getByNickname(String nickname) throws SQLException {
        LiveUserGameEntity res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select * from live_user_games where nick_name like '%" + nickname + "%'";
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            List<LiveUserGameEntity> list = this.mapResult(rs);
            if (list.size() > 0) {
                res = list.get(0);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean create(LiveUserGameEntity entity) throws SQLException {
        String sql = "INSERT INTO live_user_games (nick_name, active, note, expired_at, created_by, created_at) VALUE (?,?,?,?,?,?)";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, entity.getNick_name());
            stmt.setBoolean(param++, entity.getActive());
            stmt.setString(param++, entity.getNote());
            stmt.setTimestamp(param++, new Timestamp(entity.getExpired_at().getTime()));
            stmt.setString(param++, entity.getCreated_by());
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
    public boolean update(LiveUserGameEntity entity) throws SQLException {
        String sql = "Update live_user_games set  active = ?, note = ?, expired_at = ?, last_updated_by = ?, last_updated_at = ? where nick_name = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        boolean ok = false;
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setBoolean(param++, entity.getActive());
            stmt.setString(param++, entity.getNote());
            stmt.setTimestamp(param++, new Timestamp(entity.getExpired_at().getTime()));
            stmt.setString(param++, entity.getLast_updated_by());
            stmt.setTimestamp(param++, new Timestamp(date.getTime()));
            stmt.setString(param++, entity.getNick_name());
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
    public boolean delete(int id, String userAction) throws SQLException {
        String sql = "DElete from live_user_games where id = ?";
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

    @Override
    public LiveUserGameEntity get(int id) throws SQLException {
        LiveUserGameEntity res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select * from live_user_games where id = ?";
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            List<LiveUserGameEntity> list = this.mapResult(rs);
            if (list.size() > 0) {
                res = list.get(0);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    private String getQuery(String nickname, String timeExpired, String status) {
        String condition = "";
        if (!Strings.isBlank((String)status)) {
            condition = status == "true" ? condition + " AND active = true" : condition + " AND active = false";
        }
        if (timeExpired != null && !timeExpired.equals("") && timeExpired != null && !timeExpired.equals("")) {
            condition = condition + " AND expired_at >= '" + timeExpired + "'";
        }
        if (nickname != null && !nickname.equals("")) {
            condition = condition + " AND nick_name like '%" + nickname + "%'";
        }
        return condition;
    }

    @Override
    public int count(String nickname, String timeExpired, String status) throws SQLException {
        int cnt = 0;
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select count(*) as cnt from live_user_games where 1=1";
            String condition = this.getQuery(nickname, timeExpired, status);
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
    public List<LiveUserGameEntity> search(String nickname, String timeExpired, String status, int page, int totalRecord) throws SQLException {
        List<LiveUserGameEntity> res = new ArrayList();
        String order = " order by ";
        String sort2 = "id desc";
        int num_start = (page - 1) * totalRecord;
        String limit = " LIMIT " + num_start + ", " + totalRecord + "";
        String sql = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String query = "select * from live_user_games where 1=1";
            String condition = "";
            if (!Strings.isBlank((String)status)) {
                condition = status == "true" ? condition + " AND active = true" : condition + " AND active = false";
            }
            if (timeExpired != null && !timeExpired.equals("") && timeExpired != null && !timeExpired.equals("")) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                try {
                    Calendar c = Calendar.getInstance();
                    c.setTime(format.parse(timeExpired));
                    condition = condition + " AND expired_at >= '" + format2.format(c.getTime()) + "'";
                }
                catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            if (nickname != null && !nickname.equals("")) {
                condition = condition + " AND nick_name like '%" + nickname + "%'";
            }
            sql = query + condition + order + sort2 + limit;
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            res = this.mapResult(rs);
            rs.close();
            stm.close();
        }
        return res;
    }

    private List<LiveUserGameEntity> mapResult(ResultSet rs) throws SQLException {
        List<LiveUserGameEntity> res = new ArrayList<LiveUserGameEntity>();
        while (rs.next()) {
            LiveUserGameEntity model = new LiveUserGameEntity();
            model.setId(rs.getInt("id"));
            model.setNick_name(rs.getString("nick_name"));
            model.setActive(rs.getBoolean("active"));
            model.setExpired_at(rs.getDate("expired_at"));
            model.setNote(rs.getString("note"));
            model.setCreated_at(rs.getDate("created_at"));
            model.setLast_updated_at(rs.getDate("last_updated_at"));
            model.setCreated_by(rs.getString("created_by"));
            model.setLast_updated_by(rs.getString("last_updated_by"));
            res.add(model);
        }
        return res;
    }
}

