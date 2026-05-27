package com.vinplay.dal.cashback;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Loss Rebate (Hoan Thua / Cashback) configuration service.
 *
 * Provides CRUD over the rate-config tables that drive the live SELF
 * rebate path:
 *   - tbl_cashback_config         — program-level rates + windows
 *   - tbl_cashback_game_config    — per-game % rates (read by RealTimeCommission)
 *   - tbl_cashback_changelog      — audit of config changes
 *
 * The orphan tbl_cashback_logs / tbl_cashback_log_game_detail tables
 * (and their CRUD methods) were removed in SUN-1099 cleanup — actual
 * rebate accruals live in `rebate_logs` (RealTimeCommission writes,
 * c=3082/3083 read).
 */
public class CashbackService {

    private static final Logger logger = Logger.getLogger("api");

    // ── Config ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> listConfigs(boolean activeOnly) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = activeOnly
                ? "SELECT * FROM tbl_cashback_config WHERE is_active = 1 ORDER BY id DESC"
                : "SELECT * FROM tbl_cashback_config ORDER BY id DESC";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);
             ResultSet rs = stm.executeQuery()) {
            while (rs.next()) results.add(rowToMap(rs));
        }
        return results;
    }

    public Map<String, Object> getConfig(int configId) throws SQLException {
        String sql = "SELECT * FROM tbl_cashback_config WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, configId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rowToMap(rs);
            }
        }
        return null;
    }

    public Map<String, Object> getActiveConfig() throws SQLException {
        String sql = "SELECT * FROM tbl_cashback_config WHERE is_active = 1 " +
                "AND (start_date IS NULL OR start_date <= CURDATE()) " +
                "AND (end_date IS NULL OR end_date >= CURDATE()) " +
                "ORDER BY id DESC LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);
             ResultSet rs = stm.executeQuery()) {
            if (rs.next()) return rowToMap(rs);
        }
        return null;
    }

    public int createConfig(String programName, double rebatePercent, long minLoss,
                            long minRebate, long maxRebate, long balanceThreshold,
                            int expiryDays, String startDate, String endDate,
                            String createdBy) throws SQLException {
        String sql = "INSERT INTO tbl_cashback_config " +
                "(program_name, rebate_percent, min_loss, min_rebate, max_rebate, " +
                "balance_threshold, expiry_days, is_active, start_date, end_date, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setString(1, programName);
            stm.setDouble(2, rebatePercent);
            stm.setLong(3, minLoss);
            stm.setLong(4, minRebate);
            stm.setLong(5, maxRebate);
            stm.setLong(6, balanceThreshold);
            stm.setInt(7, expiryDays);
            stm.setString(8, startDate);
            stm.setString(9, endDate);
            stm.setString(10, createdBy);
            int affected = stm.executeUpdate();
            if (affected == 1) {
                try (ResultSet rs = stm.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean updateConfig(int configId, double rebatePercent, long minLoss,
                                long minRebate, long maxRebate, long balanceThreshold,
                                int expiryDays, boolean isActive, String startDate,
                                String endDate, String updatedBy) throws SQLException {
        String sql = "UPDATE tbl_cashback_config SET rebate_percent=?, min_loss=?, min_rebate=?, " +
                "max_rebate=?, balance_threshold=?, expiry_days=?, is_active=?, " +
                "start_date=?, end_date=?, updated_by=? WHERE id=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setDouble(1, rebatePercent);
            stm.setLong(2, minLoss);
            stm.setLong(3, minRebate);
            stm.setLong(4, maxRebate);
            stm.setLong(5, balanceThreshold);
            stm.setInt(6, expiryDays);
            stm.setInt(7, isActive ? 1 : 0);
            stm.setString(8, startDate);
            stm.setString(9, endDate);
            stm.setString(10, updatedBy);
            stm.setInt(11, configId);
            return stm.executeUpdate() == 1;
        }
    }

    // ── Changelog ──────────────────────────────────────────────────────

    public void logChange(String entityType, long entityId, String action,
                          String fieldChanged, String oldValue, String newValue,
                          String changedBy) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(
                     "INSERT INTO tbl_cashback_changelog (entity_type, entity_id, action, field_changed, old_value, new_value, changed_by) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stm.setString(1, entityType);
            stm.setLong(2, entityId);
            stm.setString(3, action);
            stm.setString(4, fieldChanged);
            stm.setString(5, oldValue);
            stm.setString(6, newValue);
            stm.setString(7, changedBy);
            stm.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to log cashback change", e);
        }
    }

    // ── Per-Game Config ────────────────────────────────────────────────

    /**
     * Lấy danh sách game configs cho một cashback program.
     * Trả về tất cả game (bao gồm inactive), sắp xếp theo game_name.
     */
    public List<Map<String, Object>> getGameConfigs(int configId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT * FROM tbl_cashback_game_config WHERE config_id = ? ORDER BY game_name ASC";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, configId);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) results.add(rowToMap(rs));
            }
        }
        return results;
    }

    /**
     * Lấy danh sách game configs đang active cho một cashback program.
     */
    public List<Map<String, Object>> getActiveGameConfigs(int configId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT * FROM tbl_cashback_game_config WHERE config_id = ? AND is_active = 1 ORDER BY game_name ASC";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, configId);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) results.add(rowToMap(rs));
            }
        }
        return results;
    }

    /**
     * Thêm hoặc cập nhật % hoàn cược cho một game trong một cashback program.
     * INSERT ... ON DUPLICATE KEY UPDATE (unique: config_id + game_code).
     */
    public boolean upsertGameConfig(int configId, String gameCode, String gameName,
                                    double rebatePercent, boolean active) throws SQLException {
        String sql = "INSERT INTO tbl_cashback_game_config " +
                "(config_id, game_code, game_name, rebate_percent, is_active) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE game_name=VALUES(game_name), " +
                "rebate_percent=VALUES(rebate_percent), is_active=VALUES(is_active)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, configId);
            stm.setString(2, gameCode);
            stm.setString(3, gameName);
            stm.setDouble(4, rebatePercent);
            stm.setInt(5, active ? 1 : 0);
            return stm.executeUpdate() > 0;
        }
    }

    /**
     * Xóa game config (hard delete).
     */
    public boolean deleteGameConfig(int configId, String gameCode) throws SQLException {
        String sql = "DELETE FROM tbl_cashback_game_config WHERE config_id = ? AND game_code = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, configId);
            stm.setString(2, gameCode);
            return stm.executeUpdate() > 0;
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    // ── Admin CMS read-only methods (restored 2026-04-27) ──────────────
    // SUN-1099 cleanup migrated the live cashback FLOW to rebate_logs (claim
    // via portal c=3083), but admin still needs to view the historical
    // tbl_cashback_logs/changelog data. These read-only helpers were removed
    // in 55344ed6 along with the write-side methods (createLog/approveLog/
    // markPaid/etc.) — only the read paths are restored here.

    public List<Map<String, Object>> queryLogs(String nickName, String status,
                                                String dateFrom, String dateTo,
                                                int configId, int page, int limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM tbl_cashback_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nickName != null && !nickName.isEmpty()) {
            sb.append(" AND nick_name LIKE ?");
            params.add("%" + nickName + "%");
        }
        if (status != null && !status.isEmpty()) {
            sb.append(" AND status = ?");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sb.append(" AND calc_date >= ?");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sb.append(" AND calc_date <= ?");
            params.add(dateTo);
        }
        if (configId > 0) {
            sb.append(" AND config_id = ?");
            params.add(configId);
        }
        sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String) stm.setString(i + 1, (String) p);
                else if (p instanceof Integer) stm.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) stm.setLong(i + 1, (Long) p);
            }
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) results.add(rowToMap(rs));
            }
        }
        return results;
    }

    public int countLogs(String nickName, String status, String dateFrom, String dateTo, int configId) throws SQLException {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM tbl_cashback_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nickName != null && !nickName.isEmpty()) { sb.append(" AND nick_name LIKE ?"); params.add("%" + nickName + "%"); }
        if (status != null && !status.isEmpty())     { sb.append(" AND status = ?");      params.add(status); }
        if (dateFrom != null && !dateFrom.isEmpty()) { sb.append(" AND calc_date >= ?");  params.add(dateFrom); }
        if (dateTo != null && !dateTo.isEmpty())     { sb.append(" AND calc_date <= ?");  params.add(dateTo); }
        if (configId > 0)                            { sb.append(" AND config_id = ?");   params.add(configId); }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String) stm.setString(i + 1, (String) p);
                else if (p instanceof Integer) stm.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) stm.setLong(i + 1, (Long) p);
            }
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Map<String, Object>> getChangelog(String entityType, long entityId,
                                                   int page, int limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM tbl_cashback_changelog WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (entityType != null && !entityType.isEmpty()) {
            sb.append(" AND entity_type = ?");
            params.add(entityType);
        }
        if (entityId > 0) {
            sb.append(" AND entity_id = ?");
            params.add(entityId);
        }
        sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String) stm.setString(i + 1, (String) p);
                else if (p instanceof Integer) stm.setInt(i + 1, (Integer) p);
                else if (p instanceof Long) stm.setLong(i + 1, (Long) p);
            }
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) results.add(rowToMap(rs));
            }
        }
        return results;
    }
}
