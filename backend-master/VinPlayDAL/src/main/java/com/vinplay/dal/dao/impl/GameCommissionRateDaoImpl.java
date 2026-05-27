package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.GameCommissionRateDao;
import com.vinplay.dal.entities.agent.GameCommissionRate;
import com.vinplay.vbee.common.pools.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameCommissionRateDaoImpl implements GameCommissionRateDao {

    @Override
    public List<GameCommissionRate> searchByNickName(String nickName) throws SQLException {
        List<GameCommissionRate> list = new ArrayList<>();
        String sql = "SELECT * FROM vinplay.game_commission_rate WHERE nick_name = ? AND status = 1 ORDER BY game_id";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, nickName);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    list.add(new GameCommissionRate(rs));
                }
            }
        }
        return list;
    }

    @Override
    public Map<Integer, Double> getRateMapByNickName(String nickName) throws SQLException {
        Map<Integer, Double> map = new HashMap<>();
        String sql = "SELECT game_id, commission_rate FROM vinplay.game_commission_rate WHERE nick_name = ? AND status = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, nickName);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("game_id"), rs.getDouble("commission_rate"));
                }
            }
        }
        return map;
    }

    @Override
    public Map<String, Map<Integer, Double>> getRateMapByNickNames(List<String> nickNames) throws SQLException {
        Map<String, Map<Integer, Double>> result = new HashMap<>();
        if (nickNames == null || nickNames.isEmpty()) return result;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < nickNames.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        String sql = "SELECT nick_name, game_id, commission_rate FROM vinplay.game_commission_rate " +
                "WHERE nick_name IN (" + placeholders + ") AND status = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            for (int i = 0; i < nickNames.size(); i++) {
                stm.setString(i + 1, nickNames.get(i));
            }
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    String nn = rs.getString("nick_name");
                    int gameId = rs.getInt("game_id");
                    double rate = rs.getDouble("commission_rate");
                    result.computeIfAbsent(nn, k -> new HashMap<>()).put(gameId, rate);
                }
            }
        }
        return result;
    }

    @Override
    public boolean insertOrUpdate(GameCommissionRate rate) throws SQLException {
        String sql = "INSERT INTO vinplay.game_commission_rate " +
                "(nick_name, game_id, game_name, commission_rate, status, created_date, updated_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE commission_rate = VALUES(commission_rate), " +
                "game_name = VALUES(game_name), status = VALUES(status), updated_date = VALUES(updated_date)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            stm.setString(1, rate.getNickName());
            stm.setInt(2, rate.getGameId());
            stm.setString(3, rate.getGameName());
            stm.setDouble(4, rate.getCommissionRate());
            stm.setInt(5, rate.getStatus() > 0 ? rate.getStatus() : 1);
            stm.setTimestamp(6, now);
            stm.setTimestamp(7, now);
            return stm.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM vinplay.game_commission_rate WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, id);
            return stm.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteByNickNameAndGameId(String nickName, int gameId) throws SQLException {
        String sql = "DELETE FROM vinplay.game_commission_rate WHERE nick_name = ? AND game_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, nickName);
            stm.setInt(2, gameId);
            return stm.executeUpdate() > 0;
        }
    }
}
