package com.sunwinkr.lottery.api.adapter;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.model.SettleStatus;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC adapter for {@link BetStore} — wraps {@code vinplay_minigame.lode}.
 *
 * <h3>SECURITY — H3 fix (closes audit HIGH SQL injection)</h3>
 * Legacy {@code LoDeDaoImpl.search} / {@code count} built SQL via string
 * concatenation, opening a clear SQLi surface. <b>Every binding in this
 * adapter MUST go through {@link PreparedStatement}.</b> The port
 * {@link BetStore.SearchFilter} is strongly typed — there is no string
 * pre-baked SQL receiving path. Filter values flow through
 * {@code setString/setInt/setTimestamp} only.
 *
 * <h3>TZ FIX (AMBIGUOUS #7)</h3>
 * Pending-bet windowing anchors {@code created_date} comparisons to
 * {@link LotteryClock#VN} (Vietnam wall time), not
 * {@code ZoneId.systemDefault()} — closes the legacy
 * {@code LoDeDaoImpl} JVM-TZ leak.
 *
 * <h3>Snapshot persistence (SUN-1295)</h3>
 * INSERT writes {@code bet_unit}, {@code rate_at_purchase},
 * {@code prize_multiplier} from the engine ticket — settle reads back
 * from the row (NEVER live enum) so a future rate change can't
 * retroactively alter a pending bet.
 *
 * <p>Plan §2.6 H3, §6.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code bets} @Bean wrapper coexists with
 * this @Component. This is the canonical BetStore.
 */
@Primary
@Component
public class JdbcBetStore implements BetStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcBetStore.class);

    private static final String POOL = "mysqlpool_minigame";

    /**
     * Derives the {@code yyyymmdd} round-id from a bet's {@code created_date}
     * in Vietnam wall time ({@link LotteryClock#VN} = Asia/Ho_Chi_Minh).
     * Never reads the JVM default TZ which is Asia/Seoul on this stack.
     */
    private static long roundIdFromCreatedDate(LocalDateTime createdDate) {
        LocalDate vnDate = createdDate.atZone(LotteryClock.VN).toLocalDate();
        return Long.parseLong(vnDate.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    @Override
    public LotteryTicket insert(LotteryTicket t) {
        // SUN-1339 A4: round_id column added by A1 migration. Derived from
        // created_date in VN wall time so bets placed near the Seoul→VN
        // midnight boundary are attributed to the correct Vietnamese draw day.
        long roundId = roundIdFromCreatedDate(t.getCreatedDate());
        String sql = "INSERT INTO lode (user_id, nick_name, bet_value, bet_unit, rate_at_purchase, prize_multiplier, mode, ticket, created_date, round_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, t.getUserId());
            ps.setString(2, t.getNickname());
            ps.setLong(3, t.getBetValue());
            ps.setLong(4, t.getBetUnit());
            ps.setInt(5, t.getRateAtPurchase());
            ps.setInt(6, t.getPrizeMultiplier());
            ps.setInt(7, t.getModeId());
            ps.setString(8, t.getTicket());
            ps.setTimestamp(9, Timestamp.valueOf(t.getCreatedDate()));
            ps.setLong(10, roundId);
            ps.executeUpdate();
            long ticketId = -1L;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ticketId = keys.getLong(1);
                }
            }
            return new LotteryTicket(
                ticketId,
                t.getUserId(),
                t.getNickname(),
                t.getBetValue(),
                t.getModeId(),
                t.getTicket(),
                t.getPrize(),
                t.getCreatedDate(),
                t.getSettledAt(),
                t.getBetUnit(),
                t.getRateAtPurchase(),
                t.getPrizeMultiplier());
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.insert failed for user={} ticket={}", t.getNickname(), t.getTicket(), e);
            throw new RuntimeException("bet insert failed", e);
        }
    }

    @Override
    public List<LotteryTicket> findPendingForDate(LocalDate vnDate, LocalTime lockTime) {
        // Window: (yesterday lockTime, today lockTime) — VN wall clock.
        LocalDateTime from = LocalDateTime.of(vnDate.minusDays(1), lockTime);
        LocalDateTime to = LocalDateTime.of(vnDate, lockTime);
        String sql = "SELECT id, user_id, nick_name, bet_value, bet_unit, rate_at_purchase, prize_multiplier, mode, ticket, prize, created_date, settled_at "
                   + "FROM lode WHERE created_date > ? AND created_date <= ? AND settled_at IS NULL";
        List<LotteryTicket> out = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.findPendingForDate failed for vnDate={}", vnDate, e);
            throw new RuntimeException("pending lookup failed", e);
        }
        return out;
    }

    @Override
    public void markSettled(long ticketId, long prize) {
        // SUN-1339 B1: also stamp settle_status='SETTLED' atomically with prize.
        String sql = "UPDATE lode SET prize = ?, settled_at = NOW(), settle_status = 'SETTLED' WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, prize);
            ps.setLong(2, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.markSettled failed for ticketId={}", ticketId, e);
            throw new RuntimeException("markSettled failed", e);
        }
    }

    @Override
    public Optional<LotteryTicket> findById(long ticketId) {
        String sql = "SELECT id, user_id, nick_name, bet_value, bet_unit, rate_at_purchase, prize_multiplier, mode, ticket, prize, created_date, settled_at, settle_status "
                   + "FROM lode WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapWithStatus(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.findById failed for ticketId={}", ticketId, e);
            throw new RuntimeException("findById failed", e);
        }
        return Optional.empty();
    }

    @Override
    public int voidTicket(long ticketId) {
        // Conditional UPDATE: only flip SETTLED → VOIDED. If the row is PENDING
        // or already VOIDED the WHERE clause matches nothing and returns 0.
        String sql = "UPDATE lode SET settle_status = 'VOIDED' WHERE id = ? AND settle_status = 'SETTLED'";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.voidTicket failed for ticketId={}", ticketId, e);
            throw new RuntimeException("voidTicket failed", e);
        }
    }

    @Override
    public List<LotteryTicket> findByUser(String nickname, Paging paging) {
        String sql = "SELECT id, user_id, nick_name, bet_value, bet_unit, rate_at_purchase, prize_multiplier, mode, ticket, prize, created_date, settled_at "
                   + "FROM lode WHERE nick_name = ? ORDER BY id DESC LIMIT ? OFFSET ?";
        List<LotteryTicket> out = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setInt(2, paging.limit);
            ps.setInt(3, paging.offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.findByUser failed for nickname={}", nickname, e);
            throw new RuntimeException("findByUser failed", e);
        }
        return out;
    }

    @Override
    public long countByUser(String nickname) {
        String sql = "SELECT COUNT(*) FROM lode WHERE nick_name = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.countByUser failed for nickname={}", nickname, e);
            throw new RuntimeException("countByUser failed", e);
        }
        return 0L;
    }

    @Override
    public List<LotteryTicket> search(SearchFilter filter) {
        // H3 FIX: every clause threaded through PreparedStatement — no string
        // interpolation. The strongly typed Optional<T> fields make injection
        // structurally impossible.
        StringBuilder sql = new StringBuilder(
            "SELECT id, user_id, nick_name, bet_value, bet_unit, rate_at_purchase, prize_multiplier, mode, ticket, prize, created_date, settled_at "
          + "FROM lode WHERE 1=1");
        List<Object> binds = new ArrayList<>();
        appendFilters(sql, binds, filter);
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        binds.add(filter.paging.limit);
        binds.add(filter.paging.offset);

        List<LotteryTicket> out = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindAll(ps, binds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.search failed", e);
            throw new RuntimeException("search failed", e);
        }
        return out;
    }

    @Override
    public long count(SearchFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM lode WHERE 1=1");
        List<Object> binds = new ArrayList<>();
        appendFilters(sql, binds, filter);

        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindAll(ps, binds);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            LOG.error("JdbcBetStore.count failed", e);
            throw new RuntimeException("count failed", e);
        }
    }

    /** Append filter clauses as parameterized placeholders only. */
    private static void appendFilters(StringBuilder sql, List<Object> binds, SearchFilter f) {
        f.nickname.ifPresent(v -> { sql.append(" AND nick_name = ?"); binds.add(v); });
        f.ticket.ifPresent(v   -> { sql.append(" AND ticket = ?");    binds.add(v); });
        f.modeId.ifPresent(v   -> { sql.append(" AND mode = ?");      binds.add(v); });
        f.from.ifPresent(v     -> { sql.append(" AND created_date >= ?"); binds.add(Timestamp.valueOf(v.atStartOfDay())); });
        f.to.ifPresent(v       -> { sql.append(" AND created_date < ?");  binds.add(Timestamp.valueOf(v.plusDays(1).atStartOfDay())); });
    }

    private static void bindAll(PreparedStatement ps, List<Object> binds) throws SQLException {
        for (int i = 0; i < binds.size(); i++) {
            Object v = binds.get(i);
            int idx = i + 1;
            if (v instanceof String) {
                ps.setString(idx, (String) v);
            } else if (v instanceof Integer) {
                ps.setInt(idx, (Integer) v);
            } else if (v instanceof Long) {
                ps.setLong(idx, (Long) v);
            } else if (v instanceof Timestamp) {
                ps.setTimestamp(idx, (Timestamp) v);
            } else if (v == null) {
                ps.setNull(idx, Types.VARCHAR);
            } else {
                ps.setString(idx, String.valueOf(v));
            }
        }
    }

    /**
     * Map a ResultSet row that does NOT include {@code settle_status} (legacy
     * SELECT paths). Defaults to {@link SettleStatus#PENDING}.
     */
    private static LotteryTicket map(ResultSet rs) throws SQLException {
        Long prize = rs.getObject("prize") == null ? null : rs.getLong("prize");
        Timestamp settledTs = rs.getTimestamp("settled_at");
        LocalDateTime settledAt = settledTs == null ? null : settledTs.toLocalDateTime();
        return new LotteryTicket(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("nick_name"),
            rs.getLong("bet_value"),
            rs.getInt("mode"),
            rs.getString("ticket"),
            prize,
            rs.getTimestamp("created_date").toLocalDateTime(),
            settledAt,
            rs.getLong("bet_unit"),
            rs.getInt("rate_at_purchase"),
            rs.getInt("prize_multiplier"));
    }

    /**
     * SUN-1339 B1: Map a ResultSet row that includes {@code settle_status}.
     * Used by {@link #findById} and any future query that needs lifecycle state.
     */
    private static LotteryTicket mapWithStatus(ResultSet rs) throws SQLException {
        Long prize = rs.getObject("prize") == null ? null : rs.getLong("prize");
        Timestamp settledTs = rs.getTimestamp("settled_at");
        LocalDateTime settledAt = settledTs == null ? null : settledTs.toLocalDateTime();
        String statusStr = rs.getString("settle_status");
        SettleStatus status;
        try {
            status = statusStr != null ? SettleStatus.valueOf(statusStr) : SettleStatus.PENDING;
        } catch (IllegalArgumentException e) {
            status = SettleStatus.PENDING;
        }
        return new LotteryTicket(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("nick_name"),
            rs.getLong("bet_value"),
            rs.getInt("mode"),
            rs.getString("ticket"),
            prize,
            rs.getTimestamp("created_date").toLocalDateTime(),
            settledAt,
            rs.getLong("bet_unit"),
            rs.getInt("rate_at_purchase"),
            rs.getInt("prize_multiplier"),
            status);
    }
}
