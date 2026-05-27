/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LotteryMessage
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.LoDeDao;
import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class LoDeDaoImpl
implements LoDeDao {
    private static final Logger LOGGER = Logger.getLogger(LoDeDaoImpl.class);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void saveTransactionLode(LotteryMessage message) throws SQLException {
        // SUN-1295: stamp bet_unit / rate_at_purchase / prize_multiplier on the
        // row so settle is deterministic from the row alone, independent of any
        // future LotteryMode enum change. Falls back to NULL when caller didn't
        // provide a snapshot (legacy callers); getPrize then re-resolves via
        // the live enum at settle for those rows only.
        String sql = "INSERT INTO lode (user_id, nick_name, bet_value, bet_unit, rate_at_purchase, prize_multiplier, mode, ticket) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, message.getUserId());
            stmt.setString(2, message.getNickName());
            stmt.setLong(3, message.getBetValue());
            if (message.getBetUnit() != null) stmt.setLong(4, message.getBetUnit()); else stmt.setNull(4, java.sql.Types.BIGINT);
            if (message.getRateAtPurchase() != null) stmt.setInt(5, message.getRateAtPurchase()); else stmt.setNull(5, java.sql.Types.INTEGER);
            if (message.getPrizeMultiplier() != null) stmt.setInt(6, message.getPrizeMultiplier()); else stmt.setNull(6, java.sql.Types.INTEGER);
            stmt.setLong(7, message.getMode());
            stmt.setString(8, message.getTicket());
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** SUN-1295: read the per-bet snapshot columns into the message; null on legacy rows. */
    private static void hydrateRateSnapshot(ResultSet rs, LotteryMessage record) throws SQLException {
        Object bu = rs.getObject("bet_unit");
        if (bu != null) record.setBetUnit(((Number) bu).longValue());
        Object rate = rs.getObject("rate_at_purchase");
        if (rate != null) record.setRateAtPurchase(((Number) rate).intValue());
        Object pm = rs.getObject("prize_multiplier");
        if (pm != null) record.setPrizeMultiplier(((Number) pm).intValue());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void updatePrize(long id, long prize) {
        String sql = "UPDATE lode SET prize = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, prize);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<LotteryMessage> getRecordsWithNullPrizeBefore1830Today(Date date) {
        ArrayList<LotteryMessage> records = new ArrayList<LotteryMessage>();
        if (date == null) {
            LOGGER.warn("getRecordsWithNullPrizeBefore1830Today: date parameter is null");
            return records;
        }
        String sql = "SELECT * FROM lode WHERE prize IS NULL AND created_date < ? AND created_date > ? ORDER BY created_date DESC;";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atTime(18, 10);
            LocalDateTime dateTimeBefore = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().minusDays(1L).atTime(18, 59);
            stmt.setObject(1, dateTime);
            stmt.setObject(2, dateTimeBefore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    long userId = rs.getLong("user_id");
                    String nickName = rs.getString("nick_name");
                    long betValue = rs.getLong("bet_value");
                    long mode = rs.getLong("mode");
                    String ticket = rs.getString("ticket");
                    Long prize = null;
                    Object prizeObj = rs.getObject("prize");
                    if (prizeObj != null && !rs.wasNull()) {
                        prize = rs.getLong("prize");
                    }
                    LotteryMessage record = new LotteryMessage(id, userId, nickName, betValue, mode, ticket, prize);
                    hydrateRateSnapshot(rs, record);  // SUN-1295: stamp per-bet rate/prize from row
                    records.add(record);
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<LotteryMessage> getRowsByNickname(String nickname) {
        ArrayList<LotteryMessage> records = new ArrayList<LotteryMessage>();
        String sql = "SELECT * FROM lode WHERE nick_name = ? ORDER BY created_date DESC ;";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nickname);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    long userId = rs.getLong("user_id");
                    String nickName = rs.getString("nick_name");
                    long betValue = rs.getLong("bet_value");
                    long mode = rs.getLong("mode");
                    String ticket = rs.getString("ticket");
                    Timestamp createdDate = rs.getTimestamp("created_date");
                    Long prize = (Long)rs.getObject("prize");
                    if (rs.wasNull()) {
                        prize = null;
                    }
                    LotteryMessage record = new LotteryMessage(id, userId, nickName, betValue, mode, ticket, prize, (Date)createdDate);
                    hydrateRateSnapshot(rs, record);  // SUN-1295: stamp per-bet rate/prize from row
                    records.add(record);
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void saveToDatabase(String jsonData, Date date) {
        String insertDataQuery = "INSERT INTO result_lottery (result, created_date) VALUES (?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(insertDataQuery)) {
            stmt.setString(1, jsonData);
            stmt.setDate(2, new java.sql.Date(date.getTime()));
            stmt.executeUpdate();
            System.out.println("Data inserted successfully into the database.");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String getLatestResult(Date date) {
        String result = null;
        String query = "SELECT * FROM result_lottery WHERE DATEDIFF(DATE(created_date), ?) = 0 ORDER BY created_date DESC LIMIT 1;";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, new java.sql.Date(date.getTime()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString("result");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<String> getListOfResultsByDateRange() {
        ArrayList<String> resultList = new ArrayList<String>();
        String query = "SELECT result FROM result_lottery WHERE created_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) ORDER BY created_date DESC;";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, 6);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultList.add(rs.getString("result"));
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    @Override
    public List<LotteryMessage> search(String nickName, String ticket, String model, String timeStart, String timeEnd, int page, int limit) throws SQLException {
        ArrayList<LotteryMessage> results = new ArrayList<LotteryMessage>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            int num_start = (page - 1) * limit;
            int num_end = limit;
            String condition = "";
            if (nickName != null && !nickName.equals("")) {
                condition = condition + " AND nick_name = '" + nickName + "'";
            }
            if (ticket != null && !ticket.equals("")) {
                condition = condition + " AND ticket = '" + ticket + "'";
            }
            if (model != null && !model.equals("")) {
                condition = condition + " AND mode = " + model + "";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND created_date BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            String sql = "SELECT * FROM vinplay_minigame.lode where 1=1 " + condition + " ORDER BY created_date DESC LIMIT " + num_start + ", " + num_end + "";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryMessage entry = new LotteryMessage();
                entry.setId(rs.getLong("id"));
                entry.setNickName(rs.getString("nick_name"));
                entry.setMode(rs.getLong("mode"));
                entry.setTicket(rs.getString("ticket"));
                entry.setBetValue(rs.getLong("bet_value"));
                entry.setUserId((long)rs.getInt("user_id"));
                entry.setPrize(Long.valueOf(rs.getLong("prize")));
                entry.setCreatedDate((Date)rs.getDate("created_date"));
                entry.setUpdatedDate((Date)rs.getDate("updated_date"));
                results.add(entry);
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public long count(String nickName, String ticket, String model, String timeStart, String timeEnd) throws SQLException {
        Long count = 0L;
        String condition = "";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql;
            PreparedStatement stmt;
            ResultSet rs;
            if (nickName != null && !nickName.equals("")) {
                condition = condition + " AND nick_name = '" + nickName + "'";
            }
            if (ticket != null && !ticket.equals("")) {
                condition = condition + " AND ticket = '" + ticket + "'";
            }
            if (model != null && !model.equals("")) {
                condition = condition + " AND mode = " + model + "";
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND created_date BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            if ((rs = (stmt = conn.prepareStatement(sql = "SELECT count(id) as rc FROM vinplay_minigame.lode where 1=1 " + condition)).executeQuery()).next()) {
                count = rs.getLong("rc");
            }
            rs.close();
            stmt.close();
        }
        return count;
    }
}

