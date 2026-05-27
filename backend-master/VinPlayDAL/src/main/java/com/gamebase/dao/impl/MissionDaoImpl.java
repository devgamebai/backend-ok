/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.apache.log4j.Logger
 */
package com.gamebase.dao.impl;

import com.gamebase.dao.MissionDao;
import com.gamebase.entities.Mission;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class MissionDaoImpl
implements MissionDao {
    private final Logger logger = Logger.getLogger((String)"base_game");

    @Override
    public void createMission(Mission mission) throws SQLException {
        String sql = "INSERT INTO mission (name, description, type, point, event_id, game_id, reward, status, created_at, id, rules) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, mission.getName());
            stmt.setString(2, mission.getDescription());
            stmt.setString(3, mission.getType());
            stmt.setLong(4, mission.getPoint());
            stmt.setInt(5, mission.getEvent_id());
            stmt.setInt(6, mission.getGame_id());
            stmt.setLong(7, mission.getReward());
            stmt.setInt(8, mission.getStatus());
            stmt.setTimestamp(9, new Timestamp(mission.getCreated_at().getTime()));
            stmt.setString(10, mission.getId());
            stmt.setString(11, mission.getRuleString());
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateMission(Mission mission) throws SQLException {
        String sql = "UPDATE mission SET name = ?, description = ?, point = ?, reward = ?, status = ?, updated_at = ?, rules = ?, type = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, mission.getName());
            stmt.setString(2, mission.getDescription());
            stmt.setLong(3, mission.getPoint());
            stmt.setLong(4, mission.getReward());
            stmt.setInt(5, mission.getStatus());
            stmt.setTimestamp(6, new Timestamp(mission.getUpdated_at().getTime()));
            stmt.setString(7, mission.getRuleString());
            stmt.setString(8, mission.getType());
            stmt.setString(9, mission.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteMission(String id) throws SQLException {
        String sql = "DELETE FROM mission WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public Mission getMission(String id) throws SQLException {
        String sql = "SELECT * FROM mission WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery();){
                if (rs.next()) {
                    Mission mission = this.mapResultSetToMission(rs);
                    return mission;
                }
            }
        }
        return null;
    }

    @Override
    public List<Mission> getListMission() throws SQLException {
        String sql = "SELECT mission.* FROM mission join vinplay.event_mission em on mission.event_id = em.id WHERE em.expired_at > ? ORDER BY event_id, game_id, created_at";
        ArrayList<Mission> missions = new ArrayList<Mission>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                missions.add(this.mapResultSetToMission(rs));
            }
        }
        return missions;
    }

    @Override
    public List<Mission> getPartitionMission(String search, int limit, int offset, int event_id, int game_id, int status, String type, String start, String end) throws SQLException {
        String sql = "SELECT  id,name, type, point, rules, event_id, game_id, reward, status, created_at, updated_at FROM mission WHERE 1=1";
        ArrayList<Mission> missions = new ArrayList<Mission>();
        StringBuilder condition = new StringBuilder();
        if (search != null && !search.isEmpty()) {
            condition.append(" AND (name LIKE ? OR description LIKE ?)");
        }
        if (event_id > 0) {
            condition.append(" AND event_id = ?");
        }
        if (game_id > 0) {
            condition.append(" AND game_id = ?");
        }
        if (status > 0) {
            condition.append(" AND status = ?");
        }
        if (type != null && !type.isEmpty()) {
            condition.append(" AND type = ?");
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
            if (event_id > 0) {
                stmt.setInt(paramIndex++, event_id);
            }
            if (game_id > 0) {
                stmt.setInt(paramIndex++, game_id);
            }
            if (status > 0) {
                stmt.setInt(paramIndex++, status);
            }
            if (type != null && !type.isEmpty()) {
                stmt.setString(paramIndex++, type);
            }
            if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                stmt.setString(paramIndex++, start);
                stmt.setString(paramIndex++, end);
            }
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex++, offset);
            try (ResultSet rs = stmt.executeQuery();){
                while (rs.next()) {
                    missions.add(this.mapResultSetToMission(rs));
                }
            }
        }
        return missions;
    }

    @Override
    public int getCountMission(String search, int event_id, int game_id, int status, String type, String start, String end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mission WHERE 1=1";
        StringBuilder condition = new StringBuilder();
        if (search != null && !search.isEmpty()) {
            condition.append(" AND (name LIKE ? OR description LIKE ?)");
        }
        if (event_id > 0) {
            condition.append(" AND event_id = ?");
        }
        if (game_id > 0) {
            condition.append(" AND game_id = ?");
        }
        if (status > 0) {
            condition.append(" AND status = ?");
        }
        if (type != null && !type.isEmpty()) {
            condition.append(" AND type = ?");
        }
        if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
            condition.append(" AND created_at BETWEEN ? AND ?");
        }
        sql = sql + condition.toString();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            int paramIndex = 1;
            if (search != null && !search.isEmpty()) {
                stmt.setString(paramIndex++, "%" + search + "%");
                stmt.setString(paramIndex++, "%" + search + "%");
            }
            if (event_id > 0) {
                stmt.setInt(paramIndex++, event_id);
            }
            if (game_id > 0) {
                stmt.setInt(paramIndex++, game_id);
            }
            if (status > 0) {
                stmt.setInt(paramIndex++, status);
            }
            if (type != null && !type.isEmpty()) {
                stmt.setString(paramIndex++, type);
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

    private Mission mapResultSetToMission(ResultSet rs) throws SQLException {
        Mission mission = new Mission();
        mission.setId(rs.getString("id"));
        mission.setName(rs.getString("name"));
        if (this.hasColumn(rs, "description")) {
            mission.setDescription(rs.getString("description"));
        }
        mission.setType(rs.getString("type"));
        mission.setPoint(rs.getLong("point"));
        mission.setEvent_id(rs.getInt("event_id"));
        mission.setGame_id(rs.getInt("game_id"));
        mission.setReward(rs.getLong("reward"));
        mission.setStatus(rs.getInt("status"));
        mission.setCreated_at(rs.getTimestamp("created_at"));
        mission.setUpdated_at(rs.getTimestamp("updated_at"));
        if (mission.getType().contains("complex") && rs.findColumn("rules") > 0) {
            mission.setRuleString(rs.getString("rules"));
        }
        return mission;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; ++i) {
                if (!columnName.equals(metaData.getColumnName(i))) continue;
                return true;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

