package com.sunwinkr.minigame.api.adapter;

import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService.TaiXiuBetSettlePort;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * JDBC adapter implementing {@link TaiXiuBetSettlePort} for the standalone
 * TaiXiu round scheduler (SUN-1341 E1).
 *
 * <p>Distinct from {@link JdbcTaixiuBetSettlePort} (used by the legacy bridge)
 * because the scheduler settle path uses {@code perBetTxId} as the lookup key
 * rather than the autoincrement {@code id}, and also writes the {@code prize}
 * column on settle.
 *
 * <p>Pool: {@code mysqlpool_minigame} (vinplay_minigame database).
 */
@Component
public class JdbcTaixiuSchedulerSettlePort implements TaiXiuBetSettlePort {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcTaixiuSchedulerSettlePort.class);

    private static final String POOL = "mysqlpool_minigame";

    /**
     * Mark a bet row SETTLED using a conditional UPDATE on
     * {@code per_bet_tx_id} and {@code settle_status='PENDING'}.
     *
     * @return {@code true} if the row was newly flipped; {@code false} if
     *         already settled (idempotency hit) or not found
     */
    @Override
    public boolean markSettled(long perBetTxId, long roundId, long prize) throws SettlePortException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            ps = conn.prepareStatement(
                "UPDATE taixiu_bet "
                + "SET settle_status='SETTLED', settled_at=?, prize=? "
                + "WHERE per_bet_tx_id=? AND settle_status='PENDING'");
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setLong(2, prize);
            ps.setLong(3, perBetTxId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                LOG.info("JdbcTaixiuSchedulerSettlePort.markSettled: no-op " +
                         "perBetTxId={} roundId={} — already settled or not found",
                         perBetTxId, roundId);
                return false;
            }
            return true;
        } catch (Throwable t) {
            throw new SettlePortException(
                "markSettled failed perBetTxId=" + perBetTxId + " roundId=" + roundId, t);
        } finally {
            close(ps, conn);
        }
    }

    private static void close(PreparedStatement ps, Connection conn) {
        if (ps != null) {
            try { ps.close(); } catch (Throwable ignored) { /* no-op */ }
        }
        if (conn != null) {
            try { conn.close(); } catch (Throwable ignored) { /* no-op */ }
        }
    }
}
