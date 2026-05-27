/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.MoonEventResponse
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.EventDAO;
import com.vinplay.dal.entities.event.EventModel;
import com.vinplay.dal.entities.event.MoonEventModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.MoonEventResponse;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EventDAOImpl
implements EventDAO {
    @Override
    public long countlistEvent(String name, Long amount, int flagtime, String startTime, String endTime) {
        long count = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            java.util.Date date;
            int index = 1;
            Boolean b_name = name == null || name.trim().isEmpty();
            Boolean b_amount = amount == null;
            Boolean b_startTime = startTime == null || startTime.trim().isEmpty();
            Boolean b_endTime = endTime == null || endTime.trim().isEmpty();
            String created_date = (b_startTime != false ? "" : " and created_date >= ?") + (b_endTime != false ? "" : " and created_date <= ?");
            String expired_date = (b_startTime != false ? "" : " and expired_date >= ?") + (b_endTime != false ? "" : " and expired_date <= ?");
            String sql = "Select count(*) as cnt from vinplay.event where 1=1 " + (b_name != false ? "" : " and name = ?") + (b_amount != false ? "" : " and amount = ?") + (flagtime == 1 ? created_date : "") + (flagtime == 2 ? expired_date : "");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_name.booleanValue()) {
                stmt.setString(index++, name);
            }
            if (!b_amount.booleanValue()) {
                stmt.setLong(index++, amount);
            }
            if (!b_startTime.booleanValue()) {
                date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startTime);
                stmt.setDate(index++, new Date(date.getTime()));
            }
            if (!b_endTime.booleanValue()) {
                date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endTime);
                stmt.setDate(index++, new Date(date.getTime()));
            }
            if ((rs = stmt.executeQuery()).next()) {
                count = rs.getInt("cnt");
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public List<EventModel> listEvent(String name, Long amount, int flagtime, String startTime, String endTime, int page, int maxItem) {
        ArrayList<EventModel> events = new ArrayList<EventModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            java.util.Date date;
            page = page - 1 < 0 ? 0 : page - 1;
            int index = 1;
            Boolean b_name = name == null || name.trim().isEmpty();
            Boolean b_amount = amount == null;
            Boolean b_startTime = startTime == null || startTime.trim().isEmpty();
            Boolean b_endTime = endTime == null || endTime.trim().isEmpty();
            String created_date = (b_startTime != false ? "" : " and created_date >= ?") + (b_endTime != false ? "" : " and created_date <= ?");
            String expired_date = (b_startTime != false ? "" : " and expired_date >= ?") + (b_endTime != false ? "" : " and expired_date <= ?");
            String sql = "Select * from vinplay.event where 1=1 " + (b_name != false ? "" : " and name = ?") + (b_amount != false ? "" : " and amount = ?") + (flagtime == 1 ? created_date : "") + (flagtime == 2 ? expired_date : "") + (maxItem != -1 ? " order by id desc limit ?,?" : "");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_name.booleanValue()) {
                stmt.setString(index++, name);
            }
            if (!b_amount.booleanValue()) {
                stmt.setLong(index++, amount);
            }
            if (!b_startTime.booleanValue()) {
                date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startTime);
                stmt.setDate(index++, new Date(date.getTime()));
            }
            if (!b_endTime.booleanValue()) {
                date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endTime);
                stmt.setDate(index++, new Date(date.getTime()));
            }
            if (maxItem != -1) {
                stmt.setInt(index++, page * maxItem);
                stmt.setInt(index, maxItem);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                EventModel eventModel = new EventModel();
                eventModel.setId(rs.getInt("id"));
                eventModel.setName(rs.getString("name"));
                eventModel.setAmount(rs.getLong("amount"));
                eventModel.setCreated_date(rs.getDate("created_date"));
                eventModel.setExpired_date(rs.getDate("expired_date"));
                events.add(eventModel);
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return events;
    }

    @Override
    public Boolean addNewEvent(String name, String created_date, Long amount, String expired_date) {
        String sql = "INSERT INTO vinplay.event (name, created_date, amount, expired_date) VALUES(?,?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            java.util.Date date1 = created_date == null || created_date.trim().isEmpty() ? new java.util.Date() : new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(created_date);
            java.util.Date date2 = expired_date == null || expired_date.trim().isEmpty() ? new java.util.Date() : new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(expired_date);
            stm.setString(1, name);
            stm.setDate(2, new Date(date1.getTime()));
            stm.setLong(3, amount);
            stm.setDate(4, new Date(date2.getTime()));
            stm.executeUpdate();
            stm.close();
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            return false;
        }
        catch (ParseException e) {
            return false;
        }
        return true;
    }

    @Override
    public EventModel eventDetail(Integer id) {
        EventModel eventModel = new EventModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            Boolean b_id = id == null;
            String sql = "Select * from vinplay.event where id = ? ";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                eventModel.setId(rs.getInt("id"));
                eventModel.setName(rs.getString("name"));
                eventModel.setAmount(rs.getLong("amount"));
                eventModel.setCreated_date(rs.getDate("created_date"));
                eventModel.setExpired_date(rs.getDate("expired_date"));
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return eventModel;
    }

    @Override
    public Boolean updateEventById(Integer id, String name, String created_date, Long amount, String expired_date) {
        String sql = "UPDATE event SET " + (amount == null ? "" : "amount = ?") + (created_date != null && !created_date.trim().isEmpty() ? ", " : " ") + (created_date == null || created_date.trim().isEmpty() ? "" : "created_date = ?") + (expired_date != null && !expired_date.trim().isEmpty() ? ", " : " ") + (expired_date == null || expired_date.trim().isEmpty() ? "" : "expired_date = ?") + (name != null && !name.trim().isEmpty() ? ", " : " ") + (name == null || name.trim().isEmpty() ? "" : "name = ?") + "WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int index = 1;
            if (amount != null) {
                stm.setLong(index++, amount);
            }
            if (created_date != null && !created_date.trim().isEmpty()) {
                stm.setString(index++, created_date);
            }
            if (expired_date != null && !expired_date.trim().isEmpty()) {
                stm.setString(index++, expired_date);
            }
            if (name != null && !name.trim().isEmpty()) {
                stm.setString(index++, name);
            }
            stm.setInt(index++, id);
            stm.executeUpdate();
        }
        catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean updateEventByName(String name, String created_date, Long amount, String expired_date) {
        String sql = "UPDATE event SET " + (amount == null ? "" : "amount = ?") + (created_date != null && !created_date.trim().isEmpty() ? ", " : " ") + (created_date == null || created_date.trim().isEmpty() ? "" : "created_date = ?") + (expired_date != null && !expired_date.trim().isEmpty() ? ", " : " ") + (expired_date == null || expired_date.trim().isEmpty() ? "" : "expired_date = ?") + "WHERE name = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int index = 1;
            if (amount != null) {
                stm.setLong(index++, amount);
            }
            if (created_date != null && !created_date.trim().isEmpty()) {
                stm.setString(index++, created_date);
            }
            if (expired_date != null && !expired_date.trim().isEmpty()) {
                stm.setString(index++, expired_date);
            }
            stm.setString(index++, name);
            stm.executeUpdate();
        }
        catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean deleteEvent(Integer id) {
        String sql = "DELETE FROM vinplay.event where id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, id);
            stm.executeUpdate();
            stm.close();
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean deleteEvent(String name) {
        String sql = "DELETE FROM vinplay.event where name = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, name);
            stm.executeUpdate();
            stm.close();
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public List<MoonEventResponse> getListEventsMoon() {
        ArrayList<MoonEventResponse> events = new ArrayList<MoonEventResponse>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT id, name FROM vinplay.event WHERE name like 'moon-night%';";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MoonEventResponse eventModel = new MoonEventResponse();
                eventModel.setIdEvent(rs.getInt("id"));
                eventModel.setNameEvent(rs.getString("name"));
                events.add(eventModel);
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    @Override
    public MoonEventModel buyPackMoon(String nickname, int eventId) throws SQLException {
        MoonEventModel eModel = new MoonEventModel();
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL SP_BuyPackMoon(?, ?, ?, ?)");
            int param = 1;
            call.setString(param++, nickname);
            call.setInt(param++, eventId);
            call.registerOutParameter(param++, -5);
            call.registerOutParameter(param++, 4);
            call.execute();
            eModel.setAmount(call.getLong(3));
            eModel.setErrorCode(call.getInt(4));
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
        return eModel;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public int addNewEventByAgent(String name, String created_date, Long amount, String expired_date, String agent_nick_name) {
        String sql = "INSERT INTO vinplay.event (name, created_date, amount, expired_date, create_by) VALUES(?,?,?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql, 1);
            java.util.Date date1 = created_date == null || created_date.trim().isEmpty() ? new java.util.Date() : new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(created_date);
            java.util.Date date2 = expired_date == null || expired_date.trim().isEmpty() ? new java.util.Date() : new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(expired_date);
            stm.setString(1, name);
            stm.setDate(2, new Date(date1.getTime()));
            stm.setLong(3, amount);
            stm.setDate(4, new Date(date2.getTime()));
            stm.setString(5, agent_nick_name);
            stm.executeUpdate();
            int id = 0;
            try {
                ResultSet rs = stm.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
            catch (Exception e) {
                return -1;
            }
            stm.close();
            return id;
        }
        catch (SQLException e) {
            return -1;
        }
        catch (ParseException e) {
            return -1;
        }
    }
}

