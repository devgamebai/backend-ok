package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.DepositPromotionDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DepositPromotionDaoImpl implements DepositPromotionDao {

    private static final Logger logger = Logger.getLogger("api");
    private static volatile Boolean depositPromotionsHasGateColumn;

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    // ── Config CRUD ──────────────────────────────────────────────────

    @Override
    public long createPromotion(int promoType, String gate, double bonusPercent, int maxUsers,
                                double turnoverFactor, long maxBonusPerTx,
                                Integer maxClaimsDaily, Long maxBonusDaily,
                                String startTime, String endTime,
                                String title, String description, String conditionText) throws SQLException {
        // Normalize gate
        if (gate == null || gate.isEmpty()) gate = "ALL";
        gate = gate.toUpperCase();
        if (!gate.equals("BANK") && !gate.equals("CRYPTO")) gate = "ALL";

        long generatedId = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            boolean hasGateColumn = hasDepositPromotionsGateColumn(conn);
            String sql = hasGateColumn
                    ? "INSERT INTO deposit_promotions " +
                    "(promo_type, gate, bonus_percent, max_users, turnover_factor, max_bonus_per_tx, " +
                    " max_claims_daily, max_bonus_daily, start_time, end_time, status, " +
                    " title, description, condition_text) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)"
                    : "INSERT INTO deposit_promotions " +
                    "(promo_type, bonus_percent, max_users, turnover_factor, max_bonus_per_tx, " +
                    " max_claims_daily, max_bonus_daily, start_time, end_time, status, " +
                    " title, description, condition_text) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)";
            try (PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int idx = 1;
                stm.setInt(idx++, promoType);
                if (hasGateColumn) {
                    stm.setString(idx++, gate);
                }
                stm.setDouble(idx++, bonusPercent);
                stm.setInt(idx++, maxUsers);
                stm.setDouble(idx++, turnoverFactor);
                stm.setLong(idx++, maxBonusPerTx);
                if (maxClaimsDaily != null) {
                    stm.setInt(idx++, maxClaimsDaily);
                } else {
                    stm.setNull(idx++, Types.INTEGER);
                }
                if (maxBonusDaily != null) {
                    stm.setLong(idx++, maxBonusDaily);
                } else {
                    stm.setNull(idx++, Types.BIGINT);
                }
                stm.setString(idx++, startTime);
                stm.setString(idx++, endTime);
                stm.setString(idx++, title);
                stm.setString(idx++, description);
                stm.setString(idx, conditionText);

                int affected = stm.executeUpdate();
                if (affected == 1) {
                    try (ResultSet rs = stm.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getLong(1);
                        }
                    }
                }
            }

            if (generatedId > 0) {
                deactivateConflictingPromotions(conn, generatedId, promoType, gate, hasGateColumn);
            }
        }
        return generatedId;
    }

    @Override
    public Map<String, Object> getPromotion(long promoId) throws SQLException {
        String sql = "SELECT * FROM deposit_promotions WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, promoId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> listPromotions(int promoType, String gate) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM deposit_promotions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(buildListPromotionsSql(conn, sb, params, promoType, gate))) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) stm.setInt(i + 1, (Integer) p);
                else stm.setString(i + 1, (String) p);
            }
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public boolean updatePromotion(long promoId, String gate, double bonusPercent, int maxUsers,
                                   double turnoverFactor, long maxBonusPerTx,
                                   Integer maxClaimsDaily, Long maxBonusDaily,
                                   String startTime, String endTime, int status,
                                   String title, String description, String conditionText) throws SQLException {
        // Normalize gate
        if (gate == null || gate.isEmpty()) gate = "ALL";
        gate = gate.toUpperCase();
        if (!gate.equals("BANK") && !gate.equals("CRYPTO")) gate = "ALL";

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(buildUpdatePromotionSql(conn))) {
            Map<String, Object> currentPromo = getPromotion(promoId);
            if (currentPromo == null) {
                return false;
            }

            boolean hasGateColumn = hasDepositPromotionsGateColumn(conn);
            int idx = 1;
            if (hasGateColumn) {
                stm.setString(idx++, gate);
            }
            stm.setDouble(idx++, bonusPercent);
            stm.setInt(idx++, maxUsers);
            stm.setDouble(idx++, turnoverFactor);
            stm.setLong(idx++, maxBonusPerTx);
            if (maxClaimsDaily != null) {
                stm.setInt(idx++, maxClaimsDaily);
            } else {
                stm.setNull(idx++, Types.INTEGER);
            }
            if (maxBonusDaily != null) {
                stm.setLong(idx++, maxBonusDaily);
            } else {
                stm.setNull(idx++, Types.BIGINT);
            }
            stm.setString(idx++, startTime);
            stm.setString(idx++, endTime);
            stm.setInt(idx++, status);
            stm.setString(idx++, title);
            stm.setString(idx++, description);
            stm.setString(idx++, conditionText);
            stm.setLong(idx, promoId);

            boolean updated = stm.executeUpdate() == 1;
            if (updated && status == 1) {
                int promoType = ((Number) currentPromo.get("promo_type")).intValue();
                deactivateConflictingPromotions(conn, promoId, promoType, gate, hasGateColumn);
            }
            return updated;
        }
    }

    @Override
    public Map<String, Object> getActivePromotion(int promoType, String gate) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(buildActivePromotionSql(conn, true, gate))) {
            bindActivePromotionParams(stm, conn, promoType, gate);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getActivePromotions(int promoType, String gate) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(buildActivePromotionSql(conn, false, gate))) {
            bindActivePromotionParams(stm, conn, promoType, gate);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public boolean deletePromotion(long promoId) throws SQLException {
        String sql = "DELETE FROM deposit_promotions WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, promoId);
            return stm.executeUpdate() > 0;
        }
    }

    private String buildListPromotionsSql(Connection conn, StringBuilder sb, List<Object> params,
                                          int promoType, String gate) throws SQLException {
        if (promoType > 0) {
            sb.append(" AND promo_type = ?");
            params.add(promoType);
        }

        boolean hasGateColumn = hasDepositPromotionsGateColumn(conn);
        boolean hasGateFilter = hasGateColumn && gate != null && !gate.isEmpty() && !"ALL".equalsIgnoreCase(gate);
        if (hasGateFilter) {
            sb.append(" AND (gate = ? OR gate = 'ALL')");
            params.add(gate.toUpperCase());
        }

        sb.append(" ORDER BY updated_at DESC, id DESC");
        return sb.toString();
    }

    private String buildUpdatePromotionSql(Connection conn) throws SQLException {
        boolean hasGateColumn = hasDepositPromotionsGateColumn(conn);
        StringBuilder sb = new StringBuilder("UPDATE deposit_promotions SET ");
        if (hasGateColumn) {
            sb.append("gate = ?, ");
        }
        sb.append("bonus_percent = ?, max_users = ?, turnover_factor = ?, max_bonus_per_tx = ?, ")
                .append("max_claims_daily = ?, max_bonus_daily = ?, ")
                .append("start_time = ?, end_time = ?, status = ?, ")
                .append("title = ?, description = ?, condition_text = ?, updated_at = NOW() ")
                .append("WHERE id = ?");
        return sb.toString();
    }

    private String buildActivePromotionSql(Connection conn, boolean singleResult, String gate) throws SQLException {
        boolean hasGateColumn = hasDepositPromotionsGateColumn(conn);
        boolean hasGateFilter = hasGateColumn && gate != null && !gate.isEmpty() && !"ALL".equalsIgnoreCase(gate);

        StringBuilder sb = new StringBuilder(
                "SELECT * FROM deposit_promotions " +
                        "WHERE promo_type = ? AND status = 1 " +
                        "AND NOW() BETWEEN start_time AND end_time");
        if (hasGateFilter) {
            sb.append(" AND (gate = ? OR gate = 'ALL')");
        }

        appendPromotionOrdering(sb, hasGateFilter);
        if (singleResult) {
            sb.append(" LIMIT 1");
        }
        return sb.toString();
    }

    private void appendPromotionOrdering(StringBuilder sb, boolean hasGateFilter) {
        sb.append(" ORDER BY ");
        if (hasGateFilter) {
            sb.append("CASE WHEN gate = ? THEN 0 ELSE 1 END, ");
        }
        sb.append("updated_at DESC, id DESC");
    }

    private void bindActivePromotionParams(PreparedStatement stm, Connection conn, int promoType, String gate) throws SQLException {
        boolean hasGateColumn = hasDepositPromotionsGateColumn(conn);
        boolean hasGateFilter = hasGateColumn && gate != null && !gate.isEmpty() && !"ALL".equalsIgnoreCase(gate);

        int idx = 1;
        stm.setInt(idx++, promoType);
        if (hasGateFilter) {
            String normalizedGate = gate.toUpperCase();
            stm.setString(idx++, normalizedGate);
            stm.setString(idx, normalizedGate);
        }
    }

    private boolean hasDepositPromotionsGateColumn(Connection conn) throws SQLException {
        Boolean cached = depositPromotionsHasGateColumn;
        if (cached != null) {
            return cached;
        }

        boolean hasGateColumn;
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, "deposit_promotions", "gate")) {
            hasGateColumn = rs.next();
        }
        depositPromotionsHasGateColumn = hasGateColumn;
        return hasGateColumn;
    }

    private void deactivateConflictingPromotions(Connection conn, long currentPromoId, int promoType,
                                                 String gate, boolean hasGateColumn) throws SQLException {
        String sql;
        if (!hasGateColumn) {
            sql = "UPDATE deposit_promotions SET status = 0, updated_at = NOW() " +
                    "WHERE promo_type = ? AND status = 1 AND id <> ?";
            try (PreparedStatement stm = conn.prepareStatement(sql)) {
                stm.setInt(1, promoType);
                stm.setLong(2, currentPromoId);
                stm.executeUpdate();
            }
            return;
        }

        if (gate == null || gate.isEmpty()) gate = "ALL";
        gate = gate.toUpperCase();
        if (!gate.equals("BANK") && !gate.equals("CRYPTO")) gate = "ALL";

        if ("ALL".equals(gate)) {
            sql = "UPDATE deposit_promotions SET status = 0, updated_at = NOW() " +
                    "WHERE promo_type = ? AND status = 1 AND id <> ?";
            try (PreparedStatement stm = conn.prepareStatement(sql)) {
                stm.setInt(1, promoType);
                stm.setLong(2, currentPromoId);
                stm.executeUpdate();
            }
            return;
        }

        sql = "UPDATE deposit_promotions SET status = 0, updated_at = NOW() " +
                "WHERE promo_type = ? AND status = 1 AND id <> ? AND (gate = ? OR gate = 'ALL')";
        try (PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, promoType);
            stm.setLong(2, currentPromoId);
            stm.setString(3, gate);
            stm.executeUpdate();
        }
    }

    // ── Eligibility queries ──────────────────────────────────────────

    @Override
    public int countDistinctUsers(long promoId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM deposit_promotion_logs WHERE promo_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, promoId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public boolean hasFirstDepositClaim(long userId) throws SQLException {
        String sql = "SELECT 1 FROM deposit_promotion_logs WHERE user_id = ? AND promo_type = 1 LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean hasFirstDepositClaimToday(long userId) throws SQLException {
        String sql = "SELECT 1 FROM deposit_promotion_logs " +
                "WHERE user_id = ? AND promo_type = 1 AND claim_date = CURDATE() LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int countUserClaimsToday(long userId, long promoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM deposit_promotion_logs " +
                "WHERE user_id = ? AND promo_id = ? AND claim_date = CURDATE()";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            stm.setLong(2, promoId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public long sumUserBonusToday(long userId, long promoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(bonus_amount), 0) FROM deposit_promotion_logs " +
                "WHERE user_id = ? AND promo_id = ? AND claim_date = CURDATE()";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            stm.setLong(2, promoId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    // ── Log operations ───────────────────────────────────────────────

    @Override
    public long insertClaimLog(long promoId, int promoType, long userId, String nickName,
                               long depositTxId, long depositAmount, long bonusAmount) throws SQLException {
        String sql = "INSERT INTO deposit_promotion_logs " +
                "(promo_id, promo_type, user_id, nick_name, deposit_tx_id, " +
                " deposit_amount, bonus_amount, is_completed, claim_date, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURDATE(), NOW())";
        long generatedId = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stm.setLong(1, promoId);
            stm.setInt(2, promoType);
            stm.setLong(3, userId);
            stm.setString(4, nickName);
            stm.setLong(5, depositTxId);
            stm.setLong(6, depositAmount);
            stm.setLong(7, bonusAmount);
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
    public List<Map<String, Object>> listClaimLogs(Long promoId, Long userId, String nickName,
                                                    String dateFrom, String dateTo,
                                                    int page, int limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM deposit_promotion_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (promoId != null) {
            sb.append(" AND promo_id = ?");
            params.add(promoId);
        }
        if (userId != null) {
            sb.append(" AND user_id = ?");
            params.add(userId);
        }
        if (nickName != null && !nickName.isEmpty()) {
            sb.append(" AND nick_name LIKE ?");
            params.add("%" + nickName + "%");
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sb.append(" AND claim_date >= ?");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sb.append(" AND claim_date <= ?");
            params.add(dateTo);
        }

        sb.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        int offset = (page - 1) * limit;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sb.toString())) {
            int idx = 1;
            for (Object p : params) {
                if (p instanceof Long) {
                    stm.setLong(idx++, (Long) p);
                } else {
                    stm.setString(idx++, (String) p);
                }
            }
            stm.setInt(idx++, limit);
            stm.setInt(idx, offset);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        }
        return results;
    }

    @Override
    public boolean markCompleted(long logId) throws SQLException {
        String sql = "UPDATE deposit_promotion_logs SET is_completed = 1 WHERE id = ? AND is_completed = 0";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, logId);
            return stm.executeUpdate() == 1;
        }
    }
}
