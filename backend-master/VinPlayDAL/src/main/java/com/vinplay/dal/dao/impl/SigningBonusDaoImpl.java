package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.SigningBonusDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

public class SigningBonusDaoImpl implements SigningBonusDao {

    private static final Logger logger = Logger.getLogger("api");

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    // ── Device Install Tracking ──────────────────────────────────────

    @Override
    public boolean isDeviceRegistered(String deviceFingerprint) throws SQLException {
        String sql = "SELECT 1 FROM tbl_device_install WHERE device_fingerprint = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, deviceFingerprint);
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean registerDevice(String deviceFingerprint, String platform,
                                  String appVersion, String ipAddress) throws SQLException {
        String sql = "INSERT IGNORE INTO tbl_device_install " +
                "(device_fingerprint, platform, app_version, ip_address, first_open_at) " +
                "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, deviceFingerprint);
            stm.setString(2, platform != null ? platform : "unknown");
            stm.setString(3, appVersion);
            stm.setString(4, ipAddress);
            int affected = stm.executeUpdate();
            return affected == 1; // 1 = new insert, 0 = duplicate ignored
        }
    }

    @Override
    public boolean linkDeviceToUser(String deviceFingerprint, long userId,
                                    String nickName) throws SQLException {
        String sql = "UPDATE tbl_device_install SET user_id = ?, nick_name = ? " +
                "WHERE device_fingerprint = ? AND user_id IS NULL";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setString(3, deviceFingerprint);
            return stm.executeUpdate() == 1;
        }
    }

    // ── Bonus Config ─────────────────────────────────────────────────

    @Override
    public Map<String, Object> getActiveConfig() throws SQLException {
        String sql = "SELECT * FROM tbl_signing_bonus_config WHERE status = 1 " +
                "ORDER BY id DESC LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getConfig(int configId) throws SQLException {
        String sql = "SELECT * FROM tbl_signing_bonus_config WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, configId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateConfig(int configId, long bonusAmount, int status,
                                int wagerEnabled, double wagerMultiplier,
                                String updatedBy) throws SQLException {
        String sql = "UPDATE tbl_signing_bonus_config SET bonus_amount = ?, status = ?, " +
                "wager_enabled = ?, wager_multiplier = ?, updated_by = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, bonusAmount);
            stm.setInt(2, status);
            stm.setInt(3, wagerEnabled);
            stm.setDouble(4, wagerMultiplier);
            stm.setString(5, updatedBy);
            stm.setInt(6, configId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public List<Map<String, Object>> listConfigs() throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT * FROM tbl_signing_bonus_config ORDER BY id DESC";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    // ── Bonus Claim Logs ─────────────────────────────────────────────

    @Override
    public boolean hasUserReceivedBonus(long userId) throws SQLException {
        String sql = "SELECT 1 FROM tbl_signing_bonus_log WHERE user_id = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean hasDeviceReceivedBonus(String deviceFingerprint) throws SQLException {
        String sql = "SELECT 1 FROM tbl_signing_bonus_log WHERE device_fingerprint = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, deviceFingerprint);
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public long insertBonusLog(long userId, String nickName, String deviceFingerprint,
                               long bonusAmount, int configId) throws SQLException {
        // INSERT IGNORE: if UNIQUE constraint (user_id or device_fingerprint) fails,
        // returns 0 affected rows instead of throwing an exception → race condition safe
        String sql = "INSERT IGNORE INTO tbl_signing_bonus_log " +
                "(user_id, nick_name, device_fingerprint, bonus_amount, config_id, status, payout_mode, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 1, 'auto', NOW())";
        long generatedId = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setString(3, deviceFingerprint);
            stm.setLong(4, bonusAmount);
            stm.setInt(5, configId);
            int affected = stm.executeUpdate();
            if (affected == 1) {
                try (ResultSet rs = stm.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                    }
                }
            }
        }
        return generatedId;
    }

    @Override
    public List<Map<String, Object>> listBonusLogs(String nickName, String dateFrom,
                                                    String dateTo, int page,
                                                    int limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM tbl_signing_bonus_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nickName != null && !nickName.isEmpty()) {
            sb.append(" AND nick_name LIKE ?");
            params.add("%" + nickName + "%");
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sb.append(" AND created_at >= ?");
            params.add(dateFrom + " 00:00:00");
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sb.append(" AND created_at <= ?");
            params.add(dateTo + " 23:59:59");
        }

        sb.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        int offset = (page - 1) * limit;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sb.toString())) {
            int idx = 1;
            for (Object p : params) {
                stm.setString(idx++, (String) p);
            }
            stm.setInt(idx++, limit);
            stm.setInt(idx++, offset);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public int countBonusLogs(String nickName, String dateFrom,
                              String dateTo) throws SQLException {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM tbl_signing_bonus_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nickName != null && !nickName.isEmpty()) {
            sb.append(" AND nick_name LIKE ?");
            params.add("%" + nickName + "%");
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sb.append(" AND created_at >= ?");
            params.add(dateFrom + " 00:00:00");
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sb.append(" AND created_at <= ?");
            params.add(dateTo + " 23:59:59");
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sb.toString())) {
            int idx = 1;
            for (Object p : params) {
                stm.setString(idx++, (String) p);
            }
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // ── Manual Payout ────────────────────────────────────────────────

    @Override
    public long insertBonusLogManual(long userId, String nickName, String deviceFingerprint,
                                     long bonusAmount, int configId,
                                     String payoutBy, String reason) throws SQLException {
        String sql = "INSERT IGNORE INTO tbl_signing_bonus_log " +
                "(user_id, nick_name, device_fingerprint, bonus_amount, config_id, status, " +
                "payout_mode, payout_by, reason, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 1, 'manual', ?, ?, NOW())";
        long generatedId = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setString(3, deviceFingerprint != null ? deviceFingerprint : "manual_" + userId);
            stm.setLong(4, bonusAmount);
            stm.setInt(5, configId);
            stm.setString(6, payoutBy);
            stm.setString(7, reason);
            int affected = stm.executeUpdate();
            if (affected == 1) {
                try (ResultSet rs = stm.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                    }
                }
            }
        }
        return generatedId;
    }

    // ── Bonus Log Status / Rollback ────────────────────────────────

    @Override
    public boolean updateBonusLogStatus(long logId, int status) throws SQLException {
        String sql = "UPDATE tbl_signing_bonus_log SET status = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, status);
            stm.setLong(2, logId);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean deleteBonusLog(long logId) throws SQLException {
        String sql = "DELETE FROM tbl_signing_bonus_log WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, logId);
            return stm.executeUpdate() == 1;
        }
    }

    // ── Per-User Config ──────────────────────────────────────────────

    @Override
    public Map<String, Object> getUserConfig(long userId) throws SQLException {
        String sql = "SELECT * FROM tbl_signing_bonus_user_config WHERE user_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getUserConfigByNickName(String nickName) throws SQLException {
        String sql = "SELECT * FROM tbl_signing_bonus_user_config WHERE nick_name = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, nickName);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean upsertUserConfig(long userId, String nickName, int enabled,
                                    String payoutMode, String reason,
                                    String updatedBy) throws SQLException {
        String sql = "INSERT INTO tbl_signing_bonus_user_config " +
                "(user_id, nick_name, enabled, payout_mode, reason, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), " +
                "payout_mode = VALUES(payout_mode), reason = VALUES(reason), " +
                "updated_by = VALUES(updated_by), updated_at = NOW()";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setInt(3, enabled);
            stm.setString(4, payoutMode != null ? payoutMode : "auto");
            stm.setString(5, reason);
            stm.setString(6, updatedBy);
            return stm.executeUpdate() > 0;
        }
    }
}
