package com.sunwinkr.minigame.api.adapter;

import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.Date;

/**
 * Writes bets and settle results from the standalone TaiXiu scheduler into the
 * two legacy history stores read by c=303 ({@code GameHistoryService}):
 *
 * <ul>
 *   <li><b>Mongo {@code log_taixiu}</b> — upsert-on-insert (at bet time); set
 *       {@code prize}/{@code is_win} on settle. Field layout mirrors
 *       {@code SaveTransactionDetailTaiXiuProcessor} (SUN-685).</li>
 *   <li><b>MySQL {@code vinplay_minigame.transaction_tai_xiu_sicbo}</b> — INSERT
 *       via {@code CALL save_transaction_tai_xiu_sicbo(...)} at bet time
 *       (prize=0); UPDATE prize via direct SQL on settle.</li>
 * </ul>
 *
 * <p>All failures are logged at WARN and swallowed — this port is fire-and-forget
 * so the primary wallet write is never blocked by history failures.
 *
 * <p>Plan SUN-1341 §F1.
 */
@Component
public class LegacyTaixiuHistoryPort {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyTaixiuHistoryPort.class);

    private static final String MONGO_COLLECTION = "log_taixiu";
    private static final String MYSQL_POOL       = "mysqlpool_minigame";

    // -----------------------------------------------------------------------
    // Bet-acceptance write (on bet)
    // -----------------------------------------------------------------------

    /**
     * Write a bet-acceptance row into both legacy stores.
     *
     * <p>Mongo: upsert keyed on {@code (reference_id, user_name, money_type)}.
     * Mirrors the SUN-685 field set from
     * {@code SaveTransactionDetailTaiXiuProcessor}.
     *
     * <p>MySQL: {@code CALL save_transaction_tai_xiu_sicbo()} with prize=0
     * (pending).
     *
     * @param referenceId round identifier
     * @param nickname    player nickname (user_name / nick_name)
     * @param betValue    bet amount in VND units
     * @param betSide     1=TAI, 0=XIU
     * @param moneyType   1=vin, 0=xu
     * @param currentMoney player balance after debit (for money_before calc)
     */
    public void recordBet(long referenceId,
                          String nickname,
                          long betValue,
                          int betSide,
                          int moneyType,
                          long currentMoney) {
        writeMongoOnBet(referenceId, nickname, betValue, betSide, moneyType, currentMoney);
        writeMysqlOnBet(referenceId, nickname, betValue, betSide, moneyType);
    }

    // -----------------------------------------------------------------------
    // Settle write (on round settle)
    // -----------------------------------------------------------------------

    /**
     * Update both legacy stores with settle result for one bet.
     *
     * <p>Mongo: {@code $set prize, is_win} on the existing doc.
     * MySQL: UPDATE {@code total_prize} on the existing row.
     *
     * @param referenceId round identifier
     * @param nickname    player nickname
     * @param betSide     1=TAI, 0=XIU
     * @param moneyType   1=vin, 0=xu
     * @param prize       actual prize credited (0 for losers)
     * @param isWin       true if this bet won
     */
    public void recordSettle(long referenceId,
                             String nickname,
                             int betSide,
                             int moneyType,
                             long prize,
                             boolean isWin) {
        updateMongoOnSettle(referenceId, nickname, moneyType, prize, isWin);
        updateMysqlOnSettle(referenceId, nickname, betSide, moneyType, prize);
    }

    // -----------------------------------------------------------------------
    // Mongo helpers
    // -----------------------------------------------------------------------

    private void writeMongoOnBet(long referenceId,
                                 String nickname,
                                 long betValue,
                                 int betSide,
                                 int moneyType,
                                 long currentMoney) {
        try {
            Document filter = new Document("reference_id", referenceId)
                    .append("user_name", nickname)
                    .append("money_type", moneyType);

            Document setFields = new Document()
                    .append("reference_id", referenceId)
                    .append("user_id", 0)
                    .append("user_name", nickname)
                    .append("nick_name", nickname)
                    .append("money_type", moneyType)
                    .append("bet_value", betValue)
                    .append("bet_side", betSide)
                    .append("prize", 0L)
                    .append("refund", 0L)
                    .append("input_time", 0)
                    .append("is_md5", false)
                    .append("is_win", false)
                    .append("current_money", currentMoney);

            Document update = new Document("$set", setFields)
                    .append("$setOnInsert", new Document("create_time", new Date()));

            MongoDBConnectionFactory.getDB().getCollection(MONGO_COLLECTION)
                    .updateOne(filter, update,
                               new com.mongodb.client.model.UpdateOptions().upsert(true));
        } catch (Throwable t) {
            LOG.warn("LegacyTaixiuHistoryPort.writeMongoOnBet failed " +
                     "ref={} user={} betValue={}", referenceId, nickname, betValue, t);
        }
    }

    private void updateMongoOnSettle(long referenceId,
                                     String nickname,
                                     int moneyType,
                                     long prize,
                                     boolean isWin) {
        try {
            Document filter = new Document("reference_id", referenceId)
                    .append("user_name", nickname)
                    .append("money_type", moneyType);

            Document setFields = new Document()
                    .append("prize", prize)
                    .append("is_win", isWin);

            Document update = new Document("$set", setFields);

            MongoDBConnectionFactory.getDB().getCollection(MONGO_COLLECTION)
                    .updateOne(filter, update);
        } catch (Throwable t) {
            LOG.warn("LegacyTaixiuHistoryPort.updateMongoOnSettle failed " +
                     "ref={} user={} prize={}", referenceId, nickname, prize, t);
        }
    }

    // -----------------------------------------------------------------------
    // MySQL helpers
    // -----------------------------------------------------------------------

    private void writeMysqlOnBet(long referenceId,
                                 String nickname,
                                 long betValue,
                                 int betSide,
                                 int moneyType) {
        Connection conn = null;
        CallableStatement cs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(MYSQL_POOL);
            // CALL save_transaction_tai_xiu_sicbo(
            //   referenceId, userId, username, betValue,
            //   betSide, prize, refund, moneyType, totalJackpot)
            cs = conn.prepareCall("CALL save_transaction_tai_xiu_sicbo(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int p = 1;
            cs.setLong(p++, referenceId);
            cs.setInt(p++, 0);           // userId (unknown in standalone path)
            cs.setString(p++, nickname);
            cs.setLong(p++, betValue);
            cs.setByte(p++, (byte) betSide);
            cs.setLong(p++, 0L);         // prize = 0 (pending)
            cs.setLong(p++, 0L);         // refund = 0
            cs.setByte(p++, (byte) moneyType);
            cs.setLong(p++, 0L);         // totalJackpot = 0
            cs.execute();
        } catch (Throwable t) {
            LOG.warn("LegacyTaixiuHistoryPort.writeMysqlOnBet failed " +
                     "ref={} user={} betValue={}", referenceId, nickname, betValue, t);
        } finally {
            closeQuietly(cs, conn);
        }
    }

    private void updateMysqlOnSettle(long referenceId,
                                     String nickname,
                                     int betSide,
                                     int moneyType,
                                     long prize) {
        Connection conn = null;
        java.sql.PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(MYSQL_POOL);
            // Update the row inserted at bet time. The stored proc doesn't
            // support partial updates, so we use a direct UPDATE.
            ps = conn.prepareStatement(
                    "UPDATE vinplay_minigame.transaction_tai_xiu_sicbo " +
                    "SET total_prize=? " +
                    "WHERE reference_id=? AND user_name=? AND bet_side=? " +
                    "  AND money_type=? AND total_prize=0 " +
                    "LIMIT 1");
            ps.setLong(1, prize);
            ps.setLong(2, referenceId);
            ps.setString(3, nickname);
            ps.setByte(4, (byte) betSide);
            ps.setByte(5, (byte) moneyType);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                LOG.info("LegacyTaixiuHistoryPort.updateMysqlOnSettle: no-op " +
                         "(already settled or not found) ref={} user={} side={} moneyType={}",
                         referenceId, nickname, betSide, moneyType);
            }
        } catch (Throwable t) {
            LOG.warn("LegacyTaixiuHistoryPort.updateMysqlOnSettle failed " +
                     "ref={} user={} prize={}", referenceId, nickname, prize, t);
        } finally {
            closeQuietly(ps, conn);
        }
    }

    // -----------------------------------------------------------------------
    // Close helpers
    // -----------------------------------------------------------------------

    private static void closeQuietly(java.sql.Statement st, Connection conn) {
        if (st != null) {
            try { st.close(); } catch (Throwable ignored) { /* no-op */ }
        }
        if (conn != null) {
            try { conn.close(); } catch (Throwable ignored) { /* no-op */ }
        }
    }
}
