package com.sunwinkr.minigame.api.adapter.sicbo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPrizePerBet;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;

/**
 * Dual-writer: mirrors every Sicbo bet placed via the REST / standalone-scheduler
 * path into the two legacy stores that {@code c=303} reads from:
 *
 * <ol>
 *   <li><b>MongoDB {@code log_sicbo}</b> — one document per
 *       {@code (reference_id, user_name, money_type, bet_side)}, upserted via
 *       {@code $inc} on {@code bet_value} and {@code $setOnInsert} for
 *       immutable fields. Schema mirrors
 *       {@code SaveTransactionDetailTaiXiuSicboProcessor} (SUN-685/938).</li>
 *   <li><b>MySQL {@code vinplay_minigame.transaction_tai_xiu_sicbo}</b> — one
 *       row per {@code (reference_id, user_name, bet_side)} inserted at bet-time
 *       with {@code total_prize=0}; updated at settle-time via
 *       {@link #updateSettled}.</li>
 * </ol>
 *
 * <h3>Failure policy</h3>
 * Both write paths are fire-and-forget. Failures are logged at WARN level and
 * never propagate — the wallet / {@code sicbo_bet} path must remain unaffected.
 *
 * <p>SUN-1341 F2.
 */
@Component
public class LegacySicboHistoryPort {

    private static final Logger LOG = LoggerFactory.getLogger(LegacySicboHistoryPort.class);

    private static final String MONGO_COLLECTION = "log_sicbo";
    private static final String MYSQL_POOL       = "mysqlpool_minigame";

    // -----------------------------------------------------------------------
    // Bet-time write (called right after the bet is accepted)
    // -----------------------------------------------------------------------

    /**
     * Upsert this bet into {@code log_sicbo} (Mongo) and insert a PENDING row
     * into {@code transaction_tai_xiu_sicbo} (MySQL).
     *
     * @param referenceId the current round reference id
     * @param entry       the accepted bet entry from {@link com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState}
     */
    public void recordBet(long referenceId, SicboPotEntry entry) {
        writeMongoOnBet(referenceId, entry);
        writeMysqlOnBet(referenceId, entry);
    }

    // -----------------------------------------------------------------------
    // Settle-time write (called per-bet after prizes are resolved)
    // -----------------------------------------------------------------------

    /**
     * Update {@code log_sicbo.prize/refund} (Mongo) and
     * {@code transaction_tai_xiu_sicbo.total_prize/total_refund} (MySQL)
     * for one settled bet.
     *
     * @param roundId the round's reference id
     * @param result  the per-bet prize result (carries the {@code SicboPotEntry} back-pointer)
     */
    public void updateSettled(long roundId, SicboPrizePerBet result) {
        updateMongoOnSettle(roundId, result);
        updateMysqlOnSettle(roundId, result);
    }

    /**
     * Bulk-settle convenience: iterate all per-bet results, tolerating
     * individual failures.
     */
    public void updateAllSettled(long roundId, List<SicboPrizePerBet> perBet) {
        if (perBet == null) {
            return;
        }
        for (SicboPrizePerBet r : perBet) {
            try {
                updateSettled(roundId, r);
            } catch (Throwable t) {
                LOG.warn("LegacySicboHistoryPort.updateAllSettled: unexpected error roundId={} nickname={}",
                         roundId, r.bet.nickname, t);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Mongo — bet
    // -----------------------------------------------------------------------

    /**
     * Upsert into {@code log_sicbo}.
     *
     * <p>Filter: {@code (reference_id, user_name, money_type, bet_side)} — one
     * document per user per side per round, matching
     * {@code SaveTransactionDetailTaiXiuSicboProcessor} lines 26-29.
     *
     * <p>{@code $inc bet_value}: accumulates multi-bet on same side in same round.
     * {@code $setOnInsert}: immutable fields written only on first insert.
     */
    private void writeMongoOnBet(long referenceId, SicboPotEntry entry) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document filter = new Document("reference_id", referenceId)
                    .append("user_name", entry.nickname)
                    .append("money_type", (int) entry.moneyType)
                    .append("bet_side", entry.betSideId);

            Document incFields = new Document("bet_value", entry.betValue);

            Document setOnInsert = new Document("create_time", new Date())
                    .append("user_id",       entry.userId)
                    .append("user_name",     entry.nickname)
                    .append("nick_name",     entry.nickname)
                    .append("money_type",    (int) entry.moneyType)
                    .append("bet_side",      entry.betSideId)
                    .append("reference_id",  referenceId)
                    .append("prize",         0L)
                    .append("refund",        0L)
                    .append("input_time",    (int) entry.inputTime)
                    .append("current_money", entry.currentMoney);

            Document update = new Document("$inc", incFields)
                    .append("$setOnInsert", setOnInsert);

            db.getCollection(MONGO_COLLECTION).updateOne(filter, update,
                    new UpdateOptions().upsert(true));
        } catch (Throwable t) {
            LOG.warn("LegacySicboHistoryPort.writeMongoOnBet: failed refId={} user={} side={}",
                     referenceId, entry.nickname, entry.betSideId, t);
        }
    }

    // -----------------------------------------------------------------------
    // Mongo — settle
    // -----------------------------------------------------------------------

    private void updateMongoOnSettle(long roundId, SicboPrizePerBet result) {
        SicboPotEntry entry = result.bet;
        long prize  = result.refund ? 0L : result.prize;
        long refund = result.refund ? entry.betValue : 0L;
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document filter = new Document("reference_id", roundId)
                    .append("user_name", entry.nickname)
                    .append("money_type", (int) entry.moneyType)
                    .append("bet_side", entry.betSideId);

            Document setFields = new Document("prize", prize)
                    .append("refund", refund);
            Document update = new Document("$set", setFields);

            db.getCollection(MONGO_COLLECTION).updateOne(filter, update);
        } catch (Throwable t) {
            LOG.warn("LegacySicboHistoryPort.updateMongoOnSettle: failed roundId={} user={} side={}",
                     roundId, entry.nickname, entry.betSideId, t);
        }
    }

    // -----------------------------------------------------------------------
    // MySQL — bet
    // -----------------------------------------------------------------------

    /**
     * Insert via {@code CALL save_transaction_tai_xiu_sicbo(?,?,?,?,?,?,?,?,?)}.
     *
     * <p>9-parameter signature:
     * {@code (reference_id, user_id, user_name, bet_value, bet_side,
     * total_prize, total_refund, money_type, total_jackpot)}.
     * At bet-time {@code total_prize=0}, {@code total_refund=0},
     * {@code total_jackpot=0}; updated at settle.
     */
    private void writeMysqlOnBet(long referenceId, SicboPotEntry entry) {
        Connection conn = null;
        CallableStatement call = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(MYSQL_POOL);
            call = conn.prepareCall("CALL save_transaction_tai_xiu_sicbo(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int p = 1;
            call.setLong(p++,   referenceId);
            call.setInt(p++,    entry.userId);
            call.setString(p++, entry.nickname);
            call.setLong(p++,   entry.betValue);
            call.setByte(p++,   (byte) entry.betSideId);
            call.setLong(p++,   0L);             // total_prize — set at settle
            call.setLong(p++,   0L);             // total_refund — set at settle
            call.setByte(p++,   (byte) entry.moneyType);
            call.setLong(p++,   0L);             // total_jackpot
            call.execute();
        } catch (Throwable t) {
            LOG.warn("LegacySicboHistoryPort.writeMysqlOnBet: failed refId={} user={} side={}",
                     referenceId, entry.nickname, entry.betSideId, t);
        } finally {
            closeQuietly(call, conn);
        }
    }

    // -----------------------------------------------------------------------
    // MySQL — settle
    // -----------------------------------------------------------------------

    /**
     * Update {@code total_prize} and {@code total_refund} for the matching
     * {@code (reference_id, user_name, bet_side)} row.
     *
     * <p>The {@code AND total_prize = 0} guard provides idempotency: a
     * second settle attempt will match 0 rows and log at INFO — matching
     * the pattern used by {@link JdbcSicboBetStore#markSettled}.
     */
    private void updateMysqlOnSettle(long roundId, SicboPrizePerBet result) {
        SicboPotEntry entry = result.bet;
        long prize  = result.refund ? 0L : result.prize;
        long refund = result.refund ? entry.betValue : 0L;

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(MYSQL_POOL);
            ps = conn.prepareStatement(
                "UPDATE vinplay_minigame.transaction_tai_xiu_sicbo " +
                "SET total_prize = ?, total_refund = ? " +
                "WHERE reference_id = ? AND user_name = ? AND bet_side = ? " +
                "  AND total_prize = 0");
            ps.setLong(1,   prize);
            ps.setLong(2,   refund);
            ps.setLong(3,   roundId);
            ps.setString(4, entry.nickname);
            ps.setByte(5,   (byte) entry.betSideId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                LOG.info("LegacySicboHistoryPort.updateMysqlOnSettle: no-op " +
                         "(already settled or row missing) roundId={} user={} side={}",
                         roundId, entry.nickname, entry.betSideId);
            }
        } catch (Throwable t) {
            LOG.warn("LegacySicboHistoryPort.updateMysqlOnSettle: failed roundId={} user={} side={}",
                     roundId, entry.nickname, entry.betSideId, t);
        } finally {
            closeQuietly(ps, conn);
        }
    }

    // -----------------------------------------------------------------------
    // Resource helpers
    // -----------------------------------------------------------------------

    private static void closeQuietly(java.sql.Statement stmt, Connection conn) {
        if (stmt != null) {
            try { stmt.close(); } catch (Throwable ignored) { /* no-op */ }
        }
        if (conn != null) {
            try { conn.close(); } catch (Throwable ignored) { /* no-op */ }
        }
    }
}
