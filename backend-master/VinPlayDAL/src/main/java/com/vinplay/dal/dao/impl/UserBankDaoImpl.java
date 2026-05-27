package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.UserBankDao;
import com.vinplay.vbee.common.pools.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserBankDaoImpl implements UserBankDao {

    @Override
    public List<Map<String, Object>> getActiveBanks() throws SQLException {
        List<Map<String, Object>> banks = new ArrayList<Map<String, Object>>();
        // 2026-05-13: bank_name now ships as "Korean (CODE)" so player picker
        // shows both. code remains separate for FEs that want them apart.
        String sql = "SELECT id, " +
                "CASE WHEN code IS NOT NULL AND code <> '' " +
                "     THEN CONCAT(bank_name, ' (', code, ')') " +
                "     ELSE bank_name END AS bank_name, " +
                "bank_name AS bank_name_raw, code, logo FROM banks WHERE status = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);
             ResultSet rs = stm.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> bank = new HashMap<String, Object>();
                bank.put("id", rs.getInt("id"));
                bank.put("bank_name", rs.getString("bank_name"));
                bank.put("bank_name_raw", rs.getString("bank_name_raw"));
                bank.put("code", rs.getString("code"));
                bank.put("logo", rs.getString("logo"));
                banks.add(bank);
            }
        }
        return banks;
    }

    @Override
    public Map<String, Object> getUserBank(long userId) throws SQLException {
        // Prefer the canonical FK (ub.bank_id → banks.id) added in the
        // 2026-05-12 migration. Fall back to bank-name string match for
        // legacy rows (bank_id IS NULL) so pre-migration data still resolves
        // when its snapshot name still happens to match a banks row. If even
        // that fails, the read returns ub.bank_name verbatim (the snapshot)
        // and null code/logo — used as the laviai/Vietcombank escape hatch.
        Map<String, Object> result = null;
        // 2026-05-13: combine Korean name + short code into bank_name so
        // single-field display surfaces (player FE, history rows) show both
        // (e.g. "씨티은행 (CITI)"). canonical_bank_name still returned raw for
        // callers that want components separately.
        String sql = "SELECT ub.id, ub.user_id, ub.nick_name, ub.bank_id, ub.bank_name AS ub_bank_name, " +
                "ub.customer_name, ub.bank_number, ub.status, ub.is_locked, ub.create_date, ub.branch, " +
                "ub.update_date, ub.last_editor, " +
                "b.bank_name AS canonical_bank_name, b.code, b.logo, " +
                "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                "     ELSE ub.bank_name END AS bank_name_combined " +
                "FROM users_bank ub " +
                "LEFT JOIN banks b ON (ub.bank_id IS NOT NULL AND b.id = ub.bank_id) " +
                "                  OR (ub.bank_id IS NULL AND b.bank_name = ub.bank_name) " +
                "WHERE ub.user_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    result = new HashMap<String, Object>();
                    result.put("id", rs.getLong("id"));
                    result.put("user_id", rs.getLong("user_id"));
                    result.put("nick_name", rs.getString("nick_name"));
                    int bankId = rs.getInt("bank_id");
                    result.put("bank_id", rs.wasNull() ? null : bankId);
                    String combined = rs.getString("bank_name_combined");
                    String snapshot = rs.getString("ub_bank_name");
                    // 2026-05-13: bank_name now ships as "Korean (CODE)" e.g.
                    // "씨티은행 (CITI)" when resolved, fallback to snapshot
                    // for legacy rows where the FK can't be resolved.
                    result.put("bank_name", combined != null ? combined : snapshot);
                    result.put("customer_name", rs.getString("customer_name"));
                    result.put("bank_number", rs.getString("bank_number"));
                    result.put("status", rs.getInt("status"));
                    result.put("is_locked", rs.getInt("is_locked"));
                    result.put("create_date", rs.getString("create_date"));
                    result.put("branch", rs.getString("branch"));
                    result.put("update_date", rs.getString("update_date"));
                    result.put("last_editor", rs.getString("last_editor"));
                    result.put("code", rs.getString("code"));
                    result.put("logo", rs.getString("logo"));
                }
            }
        }
        return result;
    }

    @Override
    public boolean insertUserBank(long userId, String nickName, String bankName, String customerName, String bankNumber, String branch, int bankId) throws SQLException {
        // bank_name is still written as a denormalized snapshot for legacy
        // readers (WithdrawBankProcessor, VinPlayUserCore stored procs). bank_id
        // is the new canonical reference; renames in `banks` propagate via it.
        String sql = "INSERT INTO users_bank (user_id, nick_name, bank_name, bank_id, customer_name, bank_number, branch, status, is_locked, create_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, NOW())";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            stm.setString(2, nickName);
            stm.setString(3, bankName);
            if (bankId > 0) {
                stm.setInt(4, bankId);
            } else {
                stm.setNull(4, java.sql.Types.INTEGER);
            }
            stm.setString(5, customerName);
            stm.setString(6, bankNumber);
            stm.setString(7, branch);
            return stm.executeUpdate() == 1;
        }
    }

    @Override
    public boolean updateUserBank(long userId, String bankName, String customerName, String bankNumber, String branch, String editor, int bankId) throws SQLException {
        String sql = "UPDATE users_bank SET bank_name = ?, bank_id = ?, customer_name = ?, bank_number = ?, branch = ?, last_editor = ?, update_date = NOW() " +
                "WHERE user_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, bankName);
            if (bankId > 0) {
                stm.setInt(2, bankId);
            } else {
                stm.setNull(2, java.sql.Types.INTEGER);
            }
            stm.setString(3, customerName);
            stm.setString(4, bankNumber);
            stm.setString(5, branch);
            stm.setString(6, editor);
            stm.setLong(7, userId);
            return stm.executeUpdate() > 0;
        }
    }

    @Override
    public boolean bankNumberExists(String bankNumber) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users_bank WHERE bank_number = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, bankNumber);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public boolean nameOnBankExistsForOtherUser(int bankId, String customerName, long userId) throws SQLException {
        // Case- and whitespace-insensitive match on customer_name within
        // a single bank_id. Status filter mirrors userHasBank — locked
        // (status=0) rows don't gate a new registration. New idx
        // idx_users_bank_bankid_name (bank_id, customer_name(64))
        // backs this lookup.
        String sql = "SELECT 1 FROM users_bank "
                   + "WHERE bank_id = ? "
                   + "  AND UPPER(TRIM(customer_name)) = UPPER(TRIM(?)) "
                   + "  AND user_id <> ? "
                   + "  AND status = 1 "
                   + "LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, bankId);
            stm.setString(2, customerName);
            stm.setLong(3, userId);
            try (ResultSet rs = stm.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean userHasBank(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users_bank WHERE user_id = ? AND status = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, userId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public String getBankNameById(int bankId) throws SQLException {
        String sql = "SELECT bank_name FROM banks WHERE id = ? AND status = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, bankId);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("bank_name");
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getActiveCompanyBank() throws SQLException {
        // SUN-85x: unify with admin_banks — the only table admin CMS / admin-next
        // actually manages. company_banks had a stale seed row that CreateDeposit
        // kept stamping onto transactions, so new deposits showed a different
        // bank than what admin saw in the bank-management page.
        //
        // Schema mapping admin_banks → company_banks call contract:
        //   id            ← id
        //   bank_name     ← bank_name
        //   bank_number   ← bank_number
        //   account_holder ← customer_name (admin_banks has this instead)
        //   code          ← bank_name        (fallback; admin_banks lacks a separate code col)
        //
        // ORDER BY id ASC + LIMIT 1 matches the "first active admin bank" that
        // c=3014 GetReceiverBankProcessor already returns for the player list.
        Map<String, Object> result = null;
        // 2026-05-12: JOIN banks via admin_banks.bank_id FK so the canonical
        // Korean bank_name comes through (used by CreateDeposit to stamp the
        // bank name onto each new deposit_transactions row).
        // 2026-05-13: combine into "Korean (CODE)" format for single-field
        // display consumers.
        String sql = "SELECT ab.id, " +
                "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                "     ELSE ab.bank_name END AS bank_name, " +
                "b.code AS bank_code, " +
                "ab.bank_number, ab.customer_name, ab.status " +
                "FROM admin_banks ab " +
                "LEFT JOIN banks b ON b.id = ab.bank_id " +
                "WHERE ab.status = 1 ORDER BY ab.id ASC LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    result = new HashMap<String, Object>();
                    result.put("id", rs.getInt("id"));
                    result.put("bank_name", rs.getString("bank_name"));
                    result.put("bank_number", rs.getString("bank_number"));
                    result.put("account_holder", rs.getString("customer_name"));
                    String code = rs.getString("bank_code");
                    result.put("code", code != null ? code : rs.getString("bank_name"));
                }
            }
        }
        return result;
    }

    @Override
    public boolean updateUserBankStatus(long userId, int status, String editor) throws SQLException {
        String sql = "UPDATE users_bank SET status = ?, last_editor = ?, update_date = NOW() WHERE user_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setInt(1, status);
            stm.setString(2, editor);
            stm.setLong(3, userId);
            return stm.executeUpdate() > 0;
        }
    }
}
