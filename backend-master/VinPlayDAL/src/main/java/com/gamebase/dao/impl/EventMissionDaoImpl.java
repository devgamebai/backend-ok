/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.apache.log4j.Logger
 */
package com.gamebase.dao.impl;

import com.gamebase.dao.EventMissionDao;
import com.gamebase.entities.EventMission;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class EventMissionDaoImpl
implements EventMissionDao {
    private final Logger logger = Logger.getLogger((String)"base_game");

    @Override
    public void createEvent(EventMission eventMission) throws SQLException {
        String content = eventMission.getContent();
        if (content != null && !content.isEmpty()) {
            content = new String(Base64.getEncoder().encode(content.getBytes()));
        }
        String sql = "INSERT INTO event_mission (name, content, `show`, status, expired_at, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, eventMission.getName());
            stmt.setString(2, content);
            stmt.setBoolean(3, eventMission.isShow());
            stmt.setInt(4, eventMission.getStatus());
            stmt.setTimestamp(5, new Timestamp(eventMission.getExpiredAt().getTime()));
            stmt.setTimestamp(6, new Timestamp(new Date().getTime()));
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateEvent(EventMission eventMission) throws SQLException {
        String content = eventMission.getContent();
        if (content != null && !content.isEmpty()) {
            try {
                content = new String(Base64.getEncoder().encode(content.getBytes()));
            }
            catch (Exception e) {
                e.printStackTrace();
                content = eventMission.getContent();
            }
        }
        String sql = "UPDATE event_mission SET name = ?, content = ?, `show` = ?, status = ?, expired_at = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, eventMission.getName());
            stmt.setString(2, content);
            stmt.setBoolean(3, eventMission.isShow());
            stmt.setInt(4, eventMission.getStatus());
            stmt.setTimestamp(5, new Timestamp(eventMission.getExpiredAt().getTime()));
            stmt.setInt(6, eventMission.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteEvent(int id) throws SQLException {
        String sql = "UPDATE event_mission SET deleted_at = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public EventMission getEvent(int id) throws SQLException {
        String sql = "SELECT * FROM event_mission WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery();){
                if (rs.next()) {
                    EventMission eventMission = this.mapResultSetToEventMission(rs);
                    return eventMission;
                }
            }
        }
        return null;
    }

    @Override
    public List<EventMission> getListEvent() throws SQLException {
        String sql = "SELECT * FROM event_mission WHERE deleted_at is null AND expired_at > CURRENT_TIMESTAMP ORDER BY created_at";
        ArrayList<EventMission> events = new ArrayList<EventMission>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery();){
            while (rs.next()) {
                events.add(this.mapResultSetToEventMission(rs));
            }
        }
        return events;
    }

    @Override
    public List<EventMission> getListEventExpired() throws SQLException {
        String sql = "SELECT * FROM event_mission WHERE deleted_at is null AND expired_at <= CURRENT_TIMESTAMP ORDER BY created_at";
        ArrayList<EventMission> events = new ArrayList<EventMission>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery();){
            while (rs.next()) {
                events.add(this.mapResultSetToEventMission(rs));
            }
        }
        return events;
    }

    @Override
    public List<EventMission> getPartitionEvent(String search, int limit, int offset, int status, String start, String end) throws SQLException {
        String sql = "SELECT id, name, `show`, status, expired_at, created_at, updated_at  FROM event_mission WHERE deleted_at is null ";
        ArrayList<EventMission> events = new ArrayList<EventMission>();
        StringBuilder condition = new StringBuilder();
        if (search != null && !search.isEmpty()) {
            condition.append(" AND (name LIKE ? OR content LIKE ?)");
        }
        if (status > 0) {
            condition.append(" AND status = ?");
        }
        if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
            condition.append(" AND created_at BETWEEN ? AND ?");
        }
        sql = sql + condition.toString() + " ORDER BY created_at desc LIMIT ? OFFSET ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            int paramIndex = 1;
            if (search != null && !search.isEmpty()) {
                stmt.setString(paramIndex++, "%" + search + "%");
                stmt.setString(paramIndex++, "%" + search + "%");
            }
            if (status > 0) {
                stmt.setInt(paramIndex++, status);
            }
            if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                stmt.setString(paramIndex++, start);
                stmt.setString(paramIndex++, end);
            }
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex++, offset);
            try (ResultSet rs = stmt.executeQuery();){
                while (rs.next()) {
                    events.add(this.mapResultSetToEventMission(rs));
                }
            }
        }
        return events;
    }

    @Override
    public int getCountEvent(String search, int status, String start, String end) throws SQLException {
        String sql = "SELECT count(*) FROM event_mission WHERE deleted_at is null ";
        ArrayList events = new ArrayList();
        StringBuilder condition = new StringBuilder();
        if (search != null && !search.isEmpty()) {
            condition.append(" AND (name LIKE ? OR content LIKE ?)");
        }
        if (status > 0) {
            condition.append(" AND status = ?");
        }
        if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
            condition.append(" AND created_at BETWEEN ? AND ?");
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            int paramIndex = 1;
            if (search != null && !search.isEmpty()) {
                stmt.setString(paramIndex++, "%" + search + "%");
                stmt.setString(paramIndex++, "%" + search + "%");
            }
            if (status > 0) {
                stmt.setInt(paramIndex++, status);
            }
            if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                stmt.setString(paramIndex++, start);
                stmt.setString(paramIndex++, end);
            }
            try (ResultSet rs = stmt.executeQuery();){
                if (rs.next()) {
                    int n = rs.getInt(1);
                    return n;
                }
            }
        }
        return 0;
    }

    private EventMission mapResultSetToEventMission(ResultSet rs) throws SQLException {
        EventMission eventMission = new EventMission();
        eventMission.setId(rs.getInt("id"));
        eventMission.setName(rs.getString("name"));
        if (this.hasColumn(rs, "content")) {
            String content = rs.getString("content");
            try {
                if (content != null && !content.isEmpty()) {
                    content = new String(Base64.getDecoder().decode(content.getBytes()));
                }
            }
            catch (Exception e) {
                content = rs.getString("content");
            }
            eventMission.setContent(content);
        }
        eventMission.setShow(rs.getBoolean("show"));
        eventMission.setStatus(rs.getInt("status"));
        eventMission.setExpiredAt(rs.getTimestamp("expired_at"));
        eventMission.setCreatedAt(rs.getTimestamp("created_at"));
        eventMission.setUpdatedAt(rs.getTimestamp("updated_at"));
        return eventMission;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; ++i) {
            if (!columnName.equals(metaData.getColumnName(i))) continue;
            return true;
        }
        return false;
    }
}

