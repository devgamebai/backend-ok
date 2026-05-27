package com.vinplay.dal.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface UserBankDao {

    List<Map<String, Object>> getActiveBanks() throws SQLException;

    Map<String, Object> getUserBank(long userId) throws SQLException;

    // bankId is the FK into banks.id and is the canonical reference (added in
    // the 2026-05-12 users_bank.bank_id migration). Pass -1 when only a legacy
    // bank-name string is available — the row's bank_id will be left NULL and
    // resolved at read time by name-string fallback. See getUserBank.
    boolean insertUserBank(long userId, String nickName, String bankName, String customerName, String bankNumber, String branch, int bankId) throws SQLException;

    boolean updateUserBank(long userId, String bankName, String customerName, String bankNumber, String branch, String editor, int bankId) throws SQLException;

    boolean bankNumberExists(String bankNumber) throws SQLException;

    // SUN-1389: same customer_name registered at the same bank_id by a
    // DIFFERENT user is the strong multi-account signal. Account-number
    // duplication across banks remains allowed (SUN-602). Pass the
    // current user_id so the caller's own row is excluded (matters on
    // the admin update path; redundant for inserts but harmless).
    boolean nameOnBankExistsForOtherUser(int bankId, String customerName, long userId) throws SQLException;

    boolean userHasBank(long userId) throws SQLException;

    String getBankNameById(int bankId) throws SQLException;

    Map<String, Object> getActiveCompanyBank() throws SQLException;

    boolean updateUserBankStatus(long userId, int status, String editor) throws SQLException;
}
