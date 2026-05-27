package com.vinplay.dal.rebate;

import com.vinplay.dal.service.CommissionRateResolver;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * SUN-1205/1206/1208 follow-up — extracted auto-commission cascade so
 * the deferred-rebate reconciler can post commission for Dream Gaming
 * wagers using the SAME per-game-rate logic that vbee's
 * {@code LogMoneyUserExtraProcessor.triggerAutoCommission} uses for
 * eager BET-time posting.
 *
 * <p>Why: VinPlayDAL can't depend on the vbee module (wrong direction —
 * vbee depends on VinPlayDAL). Pre-extraction, the reconciler called
 * {@link RealTimeCommission#calculate} which uses the legacy
 * {@code useragent.commission_rate} (global) instead of per-game rates
 * from {@code vinplay.game_commission_rate}. For Dream agents whose
 * global rate is 0% but per-game rate is 1.05%/1.15%/1.25%, that
 * silently produced zero-commission rows. This class fixes that gap.
 *
 * <p>Scope: this is a one-shot post path. No
 * {@code incrementExistingAutoCommission} aggregation — each Dream
 * wager produces one cascade. Pre-existing eager-path rows from before
 * the deferred refactor stay untouched (they were created at BET time
 * with the placeholder volume; if drift correction is needed for them,
 * the reverse-and-recreate route in {@link RebateService#reverseGscByWagerCode}
 * applies).
 *
 * <p>Math: scale=4 BigDecimal differential cascade matching SUN-1209's
 * lossless precision storage. Wallet credit rounds to whole vin via
 * {@code amountForWallet} (HALF_UP scale=0).
 */
public final class AutoCommissionPipeline {

    private static final Logger logger = Logger.getLogger("dal");

    /** Anchor period_start/end to Asia/Seoul to match the rest of the
     *  rebate pipeline (rolling-history filters use Seoul dates). */
    private static final TimeZone SEOUL_TZ = TimeZone.getTimeZone("Asia/Seoul");

    public static final class Outcome {
        public boolean ok;
        public int rowsCreated;
        public BigDecimal totalAmount = BigDecimal.ZERO;
        public String error;
        @Override public String toString() {
            return "ok=" + ok + " rows=" + rowsCreated + " total=" + totalAmount
                    + (error != null ? " err=" + error : "");
        }
    }

    /**
     * Post commission for a single bet through the agent chain.
     *
     * @param playerNickname     the bettor (lookup key for users.nick_name)
     * @param actionName         canonical action / game_key (e.g. "gsc_1052_10302")
     * @param sourceKey          unique-per-bet identifier embedded in note
     *                           (e.g. "gsc:1052:10302:&lt;wager_code&gt;")
     * @param productCode        numeric product_code for the service field
     *                           ("1052" form)
     * @param volume             commission volume in vin (long)
     * @return {@link Outcome} with row count + total credited
     */
    public static Outcome post(String playerNickname, String actionName,
                                String sourceKey, String productCode, long volume) {
        Outcome o = new Outcome();
        if (volume <= 0L
                || playerNickname == null || playerNickname.isEmpty()
                || actionName == null || actionName.isEmpty()) {
            o.ok = true;   // nothing to do, not an error
            return o;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // 1. Resolve player → referral_code + self-agent (rare for Dream
            //    but the agent-self-bet path would need it).
            String refCode = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT referral_code FROM vinplay.users WHERE nick_name = ?")) {
                ps.setString(1, playerNickname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) refCode = rs.getString("referral_code");
                }
            }
            int selfAgentId = -1;
            String selfAgentNickname = null;
            double selfAgentRate = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent "
                            + "WHERE nickname = ? OR username = ? LIMIT 1")) {
                ps.setString(1, playerNickname);
                ps.setString(2, playerNickname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        selfAgentId = rs.getInt("id");
                        selfAgentNickname = rs.getString("nickname");
                        selfAgentRate = rs.getDouble("commission_rate");
                    }
                }
            }

            // 2. Build agent chain: direct parent (via referral_code) + ancestors.
            List<AgentRate> chain = new ArrayList<>();
            if (refCode != null && !refCode.trim().isEmpty()) {
                chain.addAll(buildAgentChain(conn, refCode.trim(), actionName));
            }
            // SUN-DRIFT: users.referral_code (STRING joined against useragent.code)
            // and users.parent_agent_id (INTEGER useragent.id) are two parallel
            // upline fields that can drift apart — admin-side write paths don't
            // enforce that they stay in sync. When referral_code lookup returns
            // empty, fall back to walking by parent_agent_id. Warn so ops sees
            // how often the safety net catches drift.
            if (chain.isEmpty()) {
                int parentAgentId = -1;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT parent_agent_id FROM vinplay.users WHERE nick_name = ?")) {
                    ps.setString(1, playerNickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            parentAgentId = rs.getInt("parent_agent_id");
                            if (rs.wasNull()) parentAgentId = -1;
                        }
                    }
                }
                if (parentAgentId > 0) {
                    chain.addAll(buildAgentChainById(conn, parentAgentId, actionName));
                    if (!chain.isEmpty()) {
                        logger.warn("SUN-DRIFT: auto commission used parent_agent_id fallback "
                                + "(referral_code=" + refCode + " did not resolve in useragent) player="
                                + playerNickname + " parent_agent_id=" + parentAgentId);
                    }
                }
            }
            // Self-agent bet: include at chain head with own per-game rate.
            if (selfAgentId > 0 && !containsAgent(chain, selfAgentId)) {
                double selfEff = effectiveRateFor(conn, selfAgentNickname, selfAgentRate, actionName);
                chain.add(0, new AgentRate(selfAgentId, selfAgentNickname, selfEff));
            }
            if (chain.isEmpty()) {
                o.ok = true;
                return o;
            }

            // 3. Differential cascade.
            List<Distribution> distributions = calculateDifferential(chain, volume);
            if (distributions.isEmpty()) {
                o.ok = true;
                return o;
            }

            // 4. Insert rebate_logs rows + credit agency wallets.
            String[] period = resolveDailyPeriod(System.currentTimeMillis());
            for (Distribution d : distributions) {
                String rebateType = (selfAgentId > 0 && d.agentId == selfAgentId) ? "SELF" : "DOWNLINE";
                String note = "AUTO_COMMISSION source=" + sourceKey
                        + " type=" + rebateType
                        + " user=" + playerNickname
                        + " action=" + actionName
                        + " service=" + productCode;
                insertRebateLog(conn, d, playerNickname, actionName, productCode,
                        volume, period[0], period[1], rebateType, note);
                o.rowsCreated++;
                o.totalAmount = o.totalAmount.add(d.amount);
            }
            o.ok = true;
        } catch (Exception e) {
            o.ok = false;
            o.error = e.getMessage();
            logger.error("AutoCommissionPipeline.post failed player=" + playerNickname
                    + " action=" + actionName + " volume=" + volume, e);
        }
        return o;
    }

    // ------------------------------------------------------------
    // Internals — copy/derived from vbee LogMoneyUserExtraProcessor
    // ------------------------------------------------------------

    private static List<AgentRate> buildAgentChain(Connection conn, String refCode, String gameKey) throws Exception {
        List<AgentRate> chain = new ArrayList<>();
        int directId = -1;
        String directNickname = null;
        String ancestorsStr = null;
        double directRate = 0.0;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, nickname, ancestors, commission_rate FROM vinplay_admin.useragent WHERE code = ? LIMIT 1")) {
            ps.setString(1, refCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    directId = rs.getInt("id");
                    directNickname = rs.getString("nickname");
                    ancestorsStr = rs.getString("ancestors");
                    directRate = rs.getDouble("commission_rate");
                }
            }
        }
        if (directId <= 0) return chain;

        chain.add(new AgentRate(directId, directNickname,
                effectiveRateFor(conn, directNickname, directRate, gameKey)));
        if (ancestorsStr != null && !ancestorsStr.trim().isEmpty()) {
            String norm = ancestorsStr.trim();
            if (!norm.matches("^[0-9,]+$")) {
                logger.warn("AutoCommissionPipeline invalid ancestors=" + norm + " refCode=" + refCode);
                return chain;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent "
                            + "WHERE id IN (" + norm + ") ORDER BY level DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int ancId = rs.getInt("id");
                        String ancNick = rs.getString("nickname");
                        double ancGlobal = rs.getDouble("commission_rate");
                        chain.add(new AgentRate(ancId, ancNick,
                                effectiveRateFor(conn, ancNick, ancGlobal, gameKey)));
                    }
                }
            }
        }
        return chain;
    }

    /**
     * SUN-DRIFT fallback: build the chain starting from a known
     * useragent.id when referral_code → useragent.code lookup returns
     * nothing. Mirrors {@link #buildAgentChain} except the entry query
     * is by id; ancestors are still walked via the {@code ancestors}
     * column on the direct row.
     */
    private static List<AgentRate> buildAgentChainById(Connection conn, int parentAgentId, String gameKey) throws Exception {
        List<AgentRate> chain = new ArrayList<>();
        int directId = -1;
        String directNickname = null;
        String ancestorsStr = null;
        double directRate = 0.0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, nickname, ancestors, commission_rate FROM vinplay_admin.useragent WHERE id = ? LIMIT 1")) {
            ps.setInt(1, parentAgentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    directId = rs.getInt("id");
                    directNickname = rs.getString("nickname");
                    ancestorsStr = rs.getString("ancestors");
                    directRate = rs.getDouble("commission_rate");
                }
            }
        }
        if (directId <= 0) return chain;
        chain.add(new AgentRate(directId, directNickname,
                effectiveRateFor(conn, directNickname, directRate, gameKey)));
        if (ancestorsStr != null && !ancestorsStr.trim().isEmpty()) {
            String norm = ancestorsStr.trim();
            if (!norm.matches("^[0-9,]+$")) {
                logger.warn("AutoCommissionPipeline invalid ancestors=" + norm + " parent_agent_id=" + parentAgentId);
                return chain;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent "
                            + "WHERE id IN (" + norm + ") ORDER BY level DESC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int ancId = rs.getInt("id");
                        String ancNick = rs.getString("nickname");
                        double ancGlobal = rs.getDouble("commission_rate");
                        chain.add(new AgentRate(ancId, ancNick,
                                effectiveRateFor(conn, ancNick, ancGlobal, gameKey)));
                    }
                }
            }
        }
        return chain;
    }

    private static double effectiveRateFor(Connection conn, String agentNickname,
                                             double globalRate, String gameKey) {
        if (agentNickname == null || agentNickname.isEmpty()) return 0.0;
        if (gameKey == null || gameKey.isEmpty()) return globalRate;
        CommissionRateResolver.Result res =
                CommissionRateResolver.resolve(conn, agentNickname, gameKey);
        if (res.layer != CommissionRateResolver.Layer.NONE) return res.rate;
        return 0.0;
    }

    private static boolean containsAgent(List<AgentRate> chain, int agentId) {
        for (AgentRate a : chain) if (a.agentId == agentId) return true;
        return false;
    }

    private static List<Distribution> calculateDifferential(List<AgentRate> chain, long volume) {
        List<Distribution> out = new ArrayList<>();
        BigDecimal previousRate = BigDecimal.ZERO;
        for (AgentRate node : chain) {
            BigDecimal ownRate = BigDecimal.valueOf(node.commissionRate);
            BigDecimal differential = ownRate.subtract(previousRate);
            // SUN-1209: scale=4 matches vbee's writer + the widened
            // rebate_amount column for lossless cascade math.
            BigDecimal amount = BigDecimal.valueOf(volume)
                    .multiply(differential.signum() > 0 ? differential : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            out.add(new Distribution(node.agentId, node.nickname, node.commissionRate,
                    previousRate.doubleValue(),
                    differential.signum() > 0 ? differential.doubleValue() : 0.0,
                    amount));
            if (differential.signum() > 0) previousRate = ownRate;
        }
        return out;
    }

    private static void insertRebateLog(Connection conn, Distribution d,
                                          String playerNickname, String actionName,
                                          String productCode, long volume,
                                          String periodStart, String periodEnd,
                                          String rebateType, String note) throws Exception {
        // Status is driven purely by rebate_type (matches vbee logic):
        //   SELF      → PENDING (claim via c=3083 → vin)
        //   DOWNLINE  → PAID    (auto-credit agency_wallet)
        String status = "SELF".equals(rebateType) ? "PENDING" : "PAID";
        String sql = "INSERT INTO vinplay.rebate_logs "
                + "(agent_user_id, agent_nickname, agent_level, period_start, period_end, period_type, "
                + " total_f1_volume, rebate_percentage, share_percentage, own_percentage, child_percentage, "
                + " differential_pct, rebate_amount, share_amount, net_rebate, status, note, rebate_type, "
                + " player_nickname, game_action, created_at) "
                + "VALUES (?, ?, 0, ?, ?, 'DAILY', ?, ?, 0, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.agentId);
            ps.setString(2, d.agentNickname != null ? d.agentNickname : "");
            ps.setString(3, periodStart + " 00:00:00");
            ps.setString(4, periodEnd + " 23:59:59");
            ps.setLong(5, volume);
            ps.setDouble(6, d.ownPct);
            ps.setDouble(7, d.ownPct);
            ps.setDouble(8, d.childPct);
            ps.setDouble(9, d.differentialPct);
            ps.setBigDecimal(10, d.amount);
            ps.setBigDecimal(11, d.amount);
            ps.setString(12, status);
            ps.setString(13, note);
            ps.setString(14, rebateType);
            ps.setString(15, playerNickname);
            ps.setString(16, actionName);
            ps.executeUpdate();
        }

        // Instant credit to agency_wallet — DOWNLINE only. SELF is claimed
        // via c=3083 (cashback claim) which moves it to user vin.
        if (d.amount.signum() > 0 && "DOWNLINE".equals(rebateType)) {
            try {
                boolean credited = RebateService.creditAgencyWallet(
                        d.agentId,
                        d.amountForWallet,
                        "COMMISSION_DOWNLINE",
                        playerNickname,
                        actionName,
                        "AUTO_COMMISSION (deferred) from " + playerNickname + " game=" + actionName);
                if (!credited) {
                    logger.error("AutoCommissionPipeline: agency_wallet credit FAILED agent="
                            + d.agentNickname + " amount=" + d.amount
                            + " — rebate_log is PAID but wallet not updated!");
                }
            } catch (Exception e) {
                logger.error("AutoCommissionPipeline: creditAgencyWallet threw agent="
                        + d.agentNickname + " amount=" + d.amount, e);
            }
        }
    }

    private static String[] resolveDailyPeriod(long epochMs) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setTimeZone(SEOUL_TZ);
        String date = fmt.format(new Date(epochMs));
        return new String[]{ date, date };
    }

    private static final class AgentRate {
        final int agentId;
        final String nickname;
        final double commissionRate;
        AgentRate(int agentId, String nickname, double commissionRate) {
            this.agentId = agentId;
            this.nickname = nickname != null ? nickname : "";
            this.commissionRate = commissionRate;
        }
    }

    private static final class Distribution {
        final int agentId;
        final String agentNickname;
        final double ownPct;
        final double childPct;
        final double differentialPct;
        final BigDecimal amount;
        final long amountForWallet;
        Distribution(int agentId, String agentNickname, double ownPct,
                      double childPct, double differentialPct, BigDecimal amount) {
            this.agentId = agentId;
            this.agentNickname = agentNickname != null ? agentNickname : "";
            this.ownPct = ownPct;
            this.childPct = childPct;
            this.differentialPct = differentialPct;
            this.amount = amount != null ? amount : BigDecimal.ZERO;
            this.amountForWallet = this.amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
    }

    private AutoCommissionPipeline() {}
}
