package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.GscBetsDao;
import com.vinplay.dal.entities.gsc.GscBet;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link GscBetsDao} — the MySQL mirror of Mongo
 * {@code log_gsc_bets}. See the interface javadoc for the contract; this
 * class only documents implementation-level details (SQL shape, null
 * handling, return-value semantics for the DAO callers).
 *
 * <p>Pool: uses {@code mysqlpoolname} — the same connection pool every
 * other DAO uses, so dual-write traffic shares the existing pool budget
 * with no new resource line item.
 */
public class GscBetsDaoImpl implements GscBetsDao {

    private static final Logger logger = Logger.getLogger("api");

    /**
     * Single-bet path (event_key NULL). INSERT IGNORE on uk_wager_code.
     * No JSON arrays seeded — the row represents one txn so there's
     * nothing to merge against.
     */
    private static final String INSERT_NO_EVENTKEY_SQL =
            "INSERT IGNORE INTO vinplay.gsc_bets " +
            "(user_id, user_name, nick_name, bet_value, prize, fee, " +
            " product_code, game_code, game_key, game_name, txn_id, " +
            " wager_code, currency, bet_type, settled, settle_time, " +
            " settle_txn_id, vendor_game_id, event_key, create_time, time_log) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    /**
     * P6.2 — multi-bet hand path (event_key non-null). INSERT ... ON
     * DUPLICATE KEY UPDATE on uk_event_key. First call inserts with
     * txn_ids = JSON_ARRAY(?) and details = JSON_ARRAY(JSON_OBJECT(...));
     * subsequent calls increment bet_value/fee, dedup-append the txn_id
     * into txn_ids, and push the detail row into details.
     *
     * <p>Mirrors Mongo's {@code $setOnInsert + $inc + $addToSet + $push}
     * semantics from {@code GscBetSideEffectPublisher.runBetInsert}
     * lines 200-207. Numeric/string fields stay at their first-insert
     * value via $setOnInsert / unchanged on duplicate (we don't list
     * them in the UPDATE clause).
     */
    private static final String INSERT_WITH_EVENTKEY_SQL =
            "INSERT INTO vinplay.gsc_bets " +
            "(user_id, user_name, nick_name, bet_value, prize, fee, " +
            " product_code, game_code, game_key, game_name, txn_id, " +
            " wager_code, currency, bet_type, settled, settle_time, " +
            " settle_txn_id, vendor_game_id, event_key, create_time, time_log, " +
            " txn_ids, details) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, " +
            "        JSON_ARRAY(?), JSON_ARRAY(?)) " +
            "ON DUPLICATE KEY UPDATE " +
            "  bet_value = bet_value + VALUES(bet_value), " +
            "  fee = fee + VALUES(fee), " +
            "  txn_ids = CASE WHEN txn_ids IS NULL THEN JSON_ARRAY(?) " +
            "                 WHEN JSON_CONTAINS(txn_ids, JSON_QUOTE(?)) THEN txn_ids " +
            "                 ELSE JSON_ARRAY_APPEND(txn_ids, '$', ?) END, " +
            "  details = CASE WHEN details IS NULL THEN JSON_ARRAY(CAST(? AS JSON)) " +
            "                 ELSE JSON_ARRAY_APPEND(details, '$', CAST(? AS JSON)) END";

    /**
     * P6.3 — settle UPDATE base; the WHERE clause is built dynamically
     * by the settle() method per the two-tier filter precedence in the
     * interface javadoc.
     *
     * <p>$set / $inc / $addToSet semantics:
     * <ul>
     *   <li>{@code prize = prize + ?} — $inc</li>
     *   <li>{@code settled = 1 / settle_time = NOW() / settle_txn_id = ?} — $set</li>
     *   <li>{@code event_key = COALESCE(?, event_key)} — $set when arg
     *       non-null (legacy stamps event_key only when msg.eventKey
     *       != null; we COALESCE so a null arg leaves the existing
     *       value unchanged)</li>
     *   <li>{@code settle_txn_ids} — $addToSet via JSON_CONTAINS guard
     *       + JSON_ARRAY_APPEND</li>
     * </ul>
     */
    private static final String SETTLE_UPDATE_SET_CLAUSE =
            "   SET prize = prize + ?, " +
            "       settled = 1, " +
            "       settle_time = CURRENT_TIMESTAMP(3), " +
            "       settle_txn_id = ?, " +
            "       event_key = COALESCE(?, event_key), " +
            "       settle_txn_ids = CASE " +
            "           WHEN settle_txn_ids IS NULL THEN " +
            "               (CASE WHEN ? IS NULL THEN NULL ELSE JSON_ARRAY(?) END) " +
            "           WHEN ? IS NULL THEN settle_txn_ids " +
            "           WHEN JSON_CONTAINS(settle_txn_ids, JSON_QUOTE(?)) THEN settle_txn_ids " +
            "           ELSE JSON_ARRAY_APPEND(settle_txn_ids, '$', ?) END ";

    private static final String DELETE_BY_WAGER_SQL =
            "DELETE FROM vinplay.gsc_bets WHERE wager_code = ?";

    private static final String FIND_UNSETTLED_SQL =
            "SELECT * FROM vinplay.gsc_bets " +
            " WHERE settled = 0 " +
            "   AND bet_value > 0 " +
            "   AND create_time < ? " +
            " ORDER BY create_time ASC " +
            " LIMIT ?";

    private static final String FIND_BY_USER_SQL =
            "SELECT * FROM vinplay.gsc_bets " +
            " WHERE user_name = ? " +
            " ORDER BY create_time DESC " +
            " LIMIT ? OFFSET ?";

    private static final JSONParser JSON_PARSER = new JSONParser();

    @Override
    public boolean insert(GscBet bet) throws SQLException {
        if (bet == null) return false;
        if (bet.wagerCode == null || bet.wagerCode.isEmpty()) {
            // wager_code is the natural key + UNIQUE constraint; an
            // empty value would either fail the NOT NULL or all collide
            // on the same empty-string row. Reject upstream.
            throw new SQLException("GscBetsDao.insert: wager_code is required");
        }

        boolean withEventKey = bet.eventKey != null && !bet.eventKey.isEmpty();
        String sql = withEventKey ? INSERT_WITH_EVENTKEY_SQL : INSERT_NO_EVENTKEY_SQL;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, bet.userId);
            ps.setString(i++, bet.userName);
            setNullableString(ps, i++, bet.nickName);
            ps.setLong(i++, bet.betValue);
            ps.setLong(i++, bet.prize);
            ps.setLong(i++, bet.fee);
            ps.setInt(i++, bet.productCode);
            setNullableString(ps, i++, bet.gameCode);
            setNullableString(ps, i++, bet.gameKey);
            setNullableString(ps, i++, bet.gameName);
            setNullableString(ps, i++, bet.txnId);
            ps.setString(i++, bet.wagerCode);
            setNullableString(ps, i++, bet.currency);
            setNullableString(ps, i++, bet.betType);
            ps.setInt(i++, bet.settled ? 1 : 0);
            setNullableTimestamp(ps, i++, bet.settleTime);
            setNullableString(ps, i++, bet.settleTxnId);
            setNullableString(ps, i++, bet.vendorGameId);
            setNullableString(ps, i++, bet.eventKey);
            // create_time: caller may supply (replay/backfill) or leave
            // null and let MySQL default to CURRENT_TIMESTAMP(3).
            if (bet.createTime != null) {
                ps.setTimestamp(i++, new Timestamp(bet.createTime.getTime()));
            } else {
                ps.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
            }
            setNullableTimestamp(ps, i++, bet.timeLog);

            if (withEventKey) {
                // INSERT VALUES: txn_ids = JSON_ARRAY(?), details = JSON_ARRAY(?)
                ps.setString(i++, bet.txnId != null ? bet.txnId : "");
                ps.setString(i++, buildDetailJson(bet));
                // ON DUPLICATE KEY UPDATE — txn_ids merge:
                //   ? for JSON_ARRAY(?), ? for JSON_QUOTE(?), ? for JSON_ARRAY_APPEND(?)
                String txn = bet.txnId != null ? bet.txnId : "";
                ps.setString(i++, txn);
                ps.setString(i++, txn);
                ps.setString(i++, txn);
                // ON DUPLICATE KEY UPDATE — details merge:
                //   ? for JSON_ARRAY(CAST(? AS JSON)), ? for JSON_ARRAY_APPEND(details, '$', CAST(? AS JSON))
                String detailJson = buildDetailJson(bet);
                ps.setString(i++, detailJson);
                ps.setString(i++, detailJson);
            }

            int affected = ps.executeUpdate();
            // INSERT IGNORE: 0 = duplicate, 1 = inserted.
            // ON DUPLICATE KEY UPDATE: 1 = inserted, 2 = updated, 0 = no-op.
            return affected == 1;
        }
    }

    @Override
    public int settle(String memberAccount,
                      int productCode,
                      String gameCode,
                      String wagerCode,
                      String eventKey,
                      String linkRoundId,
                      long prizeIncrement,
                      String settleTxnId) throws SQLException {
        boolean hasLinkRound = linkRoundId != null && !linkRoundId.isEmpty();
        boolean hasEventKey = eventKey != null && !eventKey.isEmpty();
        boolean hasWager = wagerCode != null && !wagerCode.isEmpty();

        // Tier 1: freespin chain routing (SUN-1196).
        if (hasLinkRound) {
            if (memberAccount == null || memberAccount.isEmpty() || productCode <= 0) {
                logger.warn("GscBetsDao.settle: linkRoundId set but member/productCode missing — skipping");
                return 0;
            }
            return runSettleUpdate(
                    "WHERE user_name = ? AND product_code = ? AND vendor_game_id = ?",
                    new Object[] { memberAccount, productCode, linkRoundId },
                    eventKey, prizeIncrement, settleTxnId);
        }

        // Tier 2: event_key first, fall back to (user, product, game, wager), then wager_code.
        // Mirrors Mongo's $or candidate precedence in DepositProcess.
        if (hasEventKey && hasWager) {
            int rc = runSettleUpdate(
                    "WHERE event_key = ?",
                    new Object[] { eventKey },
                    eventKey, prizeIncrement, settleTxnId);
            if (rc > 0) return rc;

            if (memberAccount != null && !memberAccount.isEmpty()
                    && productCode > 0
                    && gameCode != null && !gameCode.isEmpty()) {
                rc = runSettleUpdate(
                        "WHERE user_name = ? AND product_code = ? AND game_code = ? AND wager_code = ?",
                        new Object[] { memberAccount, productCode, gameCode, wagerCode },
                        eventKey, prizeIncrement, settleTxnId);
                if (rc > 0) return rc;
            }

            return runSettleUpdate(
                    "WHERE wager_code = ?",
                    new Object[] { wagerCode },
                    eventKey, prizeIncrement, settleTxnId);
        }

        // Tier 3: wager_code only.
        if (hasWager) {
            return runSettleUpdate(
                    "WHERE wager_code = ?",
                    new Object[] { wagerCode },
                    eventKey, prizeIncrement, settleTxnId);
        }

        // Tier 4: legacy fallback (user, game, settled=0).
        if (memberAccount != null && !memberAccount.isEmpty()
                && gameCode != null && !gameCode.isEmpty()) {
            return runSettleUpdate(
                    "WHERE user_name = ? AND game_code = ? AND settled = 0",
                    new Object[] { memberAccount, gameCode },
                    eventKey, prizeIncrement, settleTxnId);
        }

        logger.warn("GscBetsDao.settle: no usable filter — wager/event/link/member all empty");
        return 0;
    }

    /**
     * Internal — assemble the full UPDATE statement from the SET clause
     * + caller-supplied WHERE clause, bind the parameters, and run.
     *
     * <p>The SET clause has 8 bind slots in this order:
     * {@code [prize_inc, settle_txn_id, event_key, settle_txn_id (init NULL check),
     *  settle_txn_id (init value), settle_txn_id (skip-when-null guard),
     *  settle_txn_id (dedup), settle_txn_id (append)]}.
     * The WHERE clause's filter args follow.
     */
    private int runSettleUpdate(String whereClause,
                                Object[] whereArgs,
                                String eventKey,
                                long prizeIncrement,
                                String settleTxnId) throws SQLException {
        String sql = "UPDATE vinplay.gsc_bets " + SETTLE_UPDATE_SET_CLAUSE + whereClause;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, prizeIncrement);
            setNullableString(ps, i++, settleTxnId);
            setNullableString(ps, i++, eventKey);
            setNullableString(ps, i++, settleTxnId);
            setNullableString(ps, i++, settleTxnId);
            setNullableString(ps, i++, settleTxnId);
            setNullableString(ps, i++, settleTxnId);
            setNullableString(ps, i++, settleTxnId);
            for (Object a : whereArgs) {
                if (a instanceof Integer) ps.setInt(i++, (Integer) a);
                else if (a instanceof Long) ps.setLong(i++, (Long) a);
                else ps.setString(i++, String.valueOf(a));
            }
            return ps.executeUpdate();
        }
    }

    @Override
    public int deleteByWagerCode(String wagerCode) throws SQLException {
        if (wagerCode == null || wagerCode.isEmpty()) return 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_WAGER_SQL)) {
            ps.setString(1, wagerCode);
            return ps.executeUpdate();
        }
    }

    @Override
    public int deleteByFilter(String userName, Integer productCode,
                              String gameCode, String wagerCode) throws SQLException {
        // SUN-1184 free-spin row cleanup — drop bet_value=0 rows
        // matching the supplied filter. We compose dynamically so the
        // caller can leave fields null.
        StringBuilder sb = new StringBuilder("DELETE FROM vinplay.gsc_bets WHERE bet_value = 0");
        List<Object> params = new ArrayList<>();
        if (userName != null) {
            sb.append(" AND user_name = ?");
            params.add(userName);
        }
        if (productCode != null) {
            sb.append(" AND product_code = ?");
            params.add(productCode);
        }
        if (gameCode != null) {
            sb.append(" AND game_code = ?");
            params.add(gameCode);
        }
        if (wagerCode != null) {
            sb.append(" AND wager_code = ?");
            params.add(wagerCode);
        }
        // Defense in depth: refuse to run an unfiltered DELETE — every
        // legitimate caller provides at least one filter.
        if (params.isEmpty()) {
            logger.warn("GscBetsDao.deleteByFilter: refusing unfiltered DELETE");
            return 0;
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else {
                    ps.setString(i + 1, p.toString());
                }
            }
            return ps.executeUpdate();
        }
    }

    @Override
    public List<GscBet> findUnsettledOlderThan(Date cutoff, int limit) throws SQLException {
        List<GscBet> out = new ArrayList<>();
        if (cutoff == null) return out;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(FIND_UNSETTLED_SQL)) {
            ps.setTimestamp(1, new Timestamp(cutoff.getTime()));
            ps.setInt(2, limit > 0 ? limit : 200);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rowToBet(rs));
            }
        }
        return out;
    }

    @Override
    public List<GscBet> findByUserName(String userName, int limit, int offset) throws SQLException {
        List<GscBet> out = new ArrayList<>();
        if (userName == null || userName.isEmpty()) return out;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USER_SQL)) {
            ps.setString(1, userName);
            ps.setInt(2, limit > 0 ? limit : 50);
            ps.setInt(3, Math.max(offset, 0));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rowToBet(rs));
            }
        }
        return out;
    }

    private static GscBet rowToBet(ResultSet rs) throws SQLException {
        GscBet b = new GscBet();
        b.id = rs.getLong("id");
        b.userId = rs.getLong("user_id");
        b.userName = rs.getString("user_name");
        b.nickName = rs.getString("nick_name");
        b.betValue = rs.getLong("bet_value");
        b.prize = rs.getLong("prize");
        b.fee = rs.getLong("fee");
        b.productCode = rs.getInt("product_code");
        b.gameCode = rs.getString("game_code");
        b.gameKey = rs.getString("game_key");
        b.gameName = rs.getString("game_name");
        b.txnId = rs.getString("txn_id");
        b.wagerCode = rs.getString("wager_code");
        b.currency = rs.getString("currency");
        b.betType = rs.getString("bet_type");
        b.settled = rs.getInt("settled") != 0;
        Timestamp st = rs.getTimestamp("settle_time");
        b.settleTime = st != null ? new Date(st.getTime()) : null;
        b.settleTxnId = rs.getString("settle_txn_id");
        b.vendorGameId = rs.getString("vendor_game_id");
        b.eventKey = rs.getString("event_key");
        Timestamp ct = rs.getTimestamp("create_time");
        b.createTime = ct != null ? new Date(ct.getTime()) : null;
        Timestamp tl = rs.getTimestamp("time_log");
        b.timeLog = tl != null ? new Date(tl.getTime()) : null;
        // P6.1 — JSON columns. Best-effort parse; on malformed JSON
        // (shouldn't happen, MySQL validates writes) return null and log
        // so reader cutover doesn't NPE on a bad row.
        b.txnIds = parseJsonStringArray(rs.getString("txn_ids"));
        b.settleTxnIds = parseJsonStringArray(rs.getString("settle_txn_ids"));
        b.details = parseJsonObjectArray(rs.getString("details"));
        return b;
    }

    private static void setNullableString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v);
    }

    private static void setNullableTimestamp(PreparedStatement ps, int idx, Date v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.TIMESTAMP);
        else ps.setTimestamp(idx, new Timestamp(v.getTime()));
    }

    /**
     * Build the per-bet detail JSON object the multi-bet upsert path
     * pushes into the {@code details} array. Mirrors the legacy
     * {@code Document detail = ...} at
     * {@code GscBetSideEffectPublisher.runBetInsert} lines 194-198.
     */
    @SuppressWarnings("unchecked")
    private static String buildDetailJson(GscBet bet) {
        JSONObject o = new JSONObject();
        o.put("txn_id", bet.txnId);
        o.put("amount", bet.betValue);
        if (bet.details != null && !bet.details.isEmpty()) {
            // Caller supplied a richer detail (e.g. raw_amount, action) —
            // use the first element as authoritative for this insert.
            // Subsequent merge-path appends use the same single-element
            // shape per call.
            Map<String, Object> first = bet.details.get(0);
            if (first.containsKey("raw_amount")) o.put("raw_amount", first.get("raw_amount"));
            if (first.containsKey("action")) o.put("action", first.get("action"));
            if (first.containsKey("amount")) o.put("amount", first.get("amount"));
        }
        return o.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseJsonStringArray(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            Object parsed = JSON_PARSER.parse(json);
            if (parsed instanceof JSONArray) {
                List<String> out = new ArrayList<>();
                for (Object e : (JSONArray) parsed) {
                    out.add(e != null ? e.toString() : null);
                }
                return out;
            }
        } catch (Throwable t) {
            logger.warn("GscBetsDao.parseJsonStringArray: malformed JSON: " + t.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseJsonObjectArray(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            Object parsed = JSON_PARSER.parse(json);
            if (parsed instanceof JSONArray) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object e : (JSONArray) parsed) {
                    if (e instanceof JSONObject) {
                        out.add(new LinkedHashMap<>((JSONObject) e));
                    } else if (e instanceof Map) {
                        out.add(new LinkedHashMap<>((Map<String, Object>) e));
                    } else {
                        Map<String, Object> m = new HashMap<>();
                        m.put("value", e);
                        out.add(m);
                    }
                }
                return out;
            }
        } catch (Throwable t) {
            logger.warn("GscBetsDao.parseJsonObjectArray: malformed JSON: " + t.getMessage());
        }
        return null;
    }
}
