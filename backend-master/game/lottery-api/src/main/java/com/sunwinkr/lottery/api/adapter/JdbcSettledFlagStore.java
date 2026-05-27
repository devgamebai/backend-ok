package com.sunwinkr.lottery.api.adapter;

import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * JDBC adapter for {@link SettledFlagStore} — wraps the
 * {@code vinplay_minigame.result_lottery.settled_at} column added by
 * the PR-2 migration {@code install/config/mysql/migrations/
 * 20260514_lottery_settled_at.sql}.
 *
 * <h3>L-1 fix gate (audit invariant)</h3>
 * {@link #markSettled} is the LAST write of the settle pipeline. Until
 * it commits, REST queries gated on {@link #isSettled} treat today's
 * payload as invisible — closes finding L-1 (pre-settle result reveal).
 *
 * <p>Plan §6 / §2.6 H2.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code settledFlag} @Bean wrapper coexists
 * with this @Component. This is the canonical SettledFlagStore.
 */
@Primary
@Component
public class JdbcSettledFlagStore implements SettledFlagStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSettledFlagStore.class);

    private static final String POOL = "mysqlpool_minigame";

    @Override
    public boolean isSettled(LocalDate vnDate) {
        String sql = "SELECT 1 FROM result_lottery WHERE DATE(created_date) = ? AND settled_at IS NOT NULL LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(vnDate));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.error("JdbcSettledFlagStore.isSettled failed vnDate={}", vnDate, e);
            // Fail closed — return false so the bet gate stays LOCKED if the
            // DB hiccups during the post-lock window.
            return false;
        }
    }

    @Override
    public void markSettled(LocalDate vnDate) {
        // Idempotent: NOW() rewrites are harmless. UPDATE never partial-updates.
        String sql = "UPDATE result_lottery SET settled_at = NOW() WHERE DATE(created_date) = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(vnDate));
            int updated = ps.executeUpdate();
            if (updated == 0) {
                LOG.warn("JdbcSettledFlagStore.markSettled affected 0 rows for vnDate={} — no draw saved yet?", vnDate);
            }
        } catch (SQLException e) {
            LOG.error("JdbcSettledFlagStore.markSettled failed vnDate={}", vnDate, e);
            throw new RuntimeException("markSettled failed", e);
        }
    }
}
