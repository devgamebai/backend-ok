package com.sunwinkr.lottery.api.adapter;

import com.google.gson.Gson;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.port.ResultStore;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC adapter for {@link ResultStore} — wraps
 * {@code vinplay_minigame.result_lottery}.
 *
 * <h3>L-1 fix (pre-settle visibility)</h3>
 * {@link #listSettled} filters {@code WHERE settled_at IS NOT NULL} so
 * today's payload is invisible to REST consumers until the settle loop
 * has structurally completed. Closes audit L-1.
 *
 * <p>Stored format: a single TEXT column {@code result} holds the
 * raw Gson JSON (preserving the literal {@code ĐB} Unicode field name).
 * The legacy {@code LoDeServiceImpl} did the same.
 *
 * <p>Plan §2.6 H2, §6.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code results} @Bean wrapper coexists with
 * this @Component. This is the canonical ResultStore.
 */
@Primary
@Component
public class JdbcResultStore implements ResultStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcResultStore.class);

    private static final String POOL = "mysqlpool_minigame";

    private final Gson gson = new Gson();

    @Override
    public Optional<LotteryResult> findByDate(LocalDate vnDate) {
        String sql = "SELECT result FROM result_lottery WHERE DATE(created_date) = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(vnDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString("result");
                    if (raw == null || raw.isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(gson.fromJson(raw, LotteryResult.class));
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcResultStore.findByDate failed vnDate={}", vnDate, e);
            throw new RuntimeException("findByDate failed", e);
        }
        return Optional.empty();
    }

    @Override
    public void save(String rawJson, LocalDate vnDate) {
        // Idempotent at the table level: caller (DrawIngest) guards via findByDate.
        // The row is written with settled_at=NULL — JdbcSettledFlagStore flips
        // it as the LAST step of the settle pipeline.
        String sql = "INSERT INTO result_lottery (result, created_date) VALUES (?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rawJson);
            ps.setDate(2, Date.valueOf(vnDate));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("JdbcResultStore.save failed vnDate={}", vnDate, e);
            throw new RuntimeException("save failed", e);
        }
    }

    /**
     * History endpoint backing — return all draws in the date range whose
     * {@code settled_at IS NOT NULL}. Pre-settle rows invisible to REST
     * consumers (audit L-1).
     */
    @Override
    public List<LotteryResult> listSettled(LocalDate from, LocalDate to) {
        String sql = "SELECT result FROM result_lottery "
                   + "WHERE DATE(created_date) >= ? AND DATE(created_date) <= ? AND settled_at IS NOT NULL "
                   + "ORDER BY created_date DESC";
        List<LotteryResult> out = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String raw = rs.getString("result");
                    if (raw != null && !raw.isEmpty()) {
                        LotteryResult lr = gson.fromJson(raw, LotteryResult.class);
                        if (lr != null) out.add(lr);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcResultStore.listSettled failed from={} to={}", from, to, e);
            throw new RuntimeException("listSettled failed", e);
        }
        return out;
    }
}
