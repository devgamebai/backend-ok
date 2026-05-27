package com.vinplay.api.backend.processors.rebate;

import com.vinplay.api.backend.services.DifferentialCommissionCalculator;
import com.vinplay.api.backend.services.DifferentialCommissionCalculator.AgentRate;
import com.vinplay.api.backend.services.DifferentialCommissionCalculator.Distribution;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cron job that auto-calculates differential commission for all users with new bet volume.
 * Runs every hour. Scans user_volume_tracking for total_commission_volume > commission_volume_processed.
 * For each user with new volume, calculates commission for the delta and creates PENDING rebate_logs.
 */
public class CommissionCronJob {

    private static final Logger logger = Logger.getLogger("backend");
    private static final String TAG = "[CommissionCron] ";
    private static volatile boolean running = false;
    private static ScheduledExecutorService scheduler;

    /**
     * Call this once at startup (e.g., from BaseController.initCommands or a ServletContextListener).
     */
    public static void start(long intervalMinutes) {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "commission-cron");
            t.setDaemon(true);
            return t;
        });
        // Initial delay: 2 minutes (let server fully start), then repeat every intervalMinutes
        scheduler.scheduleAtFixedRate(CommissionCronJob::run, 2, intervalMinutes, TimeUnit.MINUTES);
        logger.info(TAG + "Started. Interval=" + intervalMinutes + "min");
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            logger.info(TAG + "Stopped");
        }
    }

    /**
     * Manual trigger (can also be called from an API endpoint).
     */
    public static synchronized String run() {
        if (running) {
            logger.warn(TAG + "Already running, skip");
            return "SKIP_ALREADY_RUNNING";
        }
        running = true;
        int processed = 0;
        int errors = 0;
        try {
            logger.info(TAG + "Starting commission scan...");
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            // Find users with unprocessed volume
            List<VolumeEntry> entries = new ArrayList<>();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id, nick_name, total_commission_volume, commission_volume_processed " +
                    "FROM user_volume_tracking " +
                    "WHERE total_commission_volume > commission_volume_processed");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    VolumeEntry e = new VolumeEntry();
                    e.userId = rs.getInt("user_id");
                    e.nickName = rs.getString("nick_name");
                    e.totalVolume = rs.getLong("total_commission_volume");
                    e.processedVolume = rs.getLong("commission_volume_processed");
                    entries.add(e);
                }
                rs.close();
                ps.close();
            }

            logger.info(TAG + "Found " + entries.size() + " users with unprocessed volume");

            for (VolumeEntry entry : entries) {
                long deltaVolume = entry.totalVolume - entry.processedVolume;
                if (deltaVolume <= 0) continue;

                try {
                    processUser(entry.nickName, deltaVolume, today);
                    // Update watermark
                    updateWatermark(entry.userId, entry.totalVolume);
                    processed++;
                    logger.info(TAG + "OK nick=" + entry.nickName + " delta=" + deltaVolume);
                } catch (Exception e) {
                    errors++;
                    logger.error(TAG + "FAIL nick=" + entry.nickName + " delta=" + deltaVolume, e);
                }
            }

            String result = "processed=" + processed + " errors=" + errors + " total=" + entries.size();
            logger.info(TAG + "Done. " + result);
            return result;
        } catch (Exception e) {
            logger.error(TAG + "Fatal error", e);
            return "ERROR: " + e.getMessage();
        } finally {
            running = false;
        }
    }

    private static void processUser(String userNick, long volume, String today) throws Exception {
        // 1. Get user's referral_code + check if user is also an agent
        String refCode = null;
        int selfAgentId = -1;
        double selfAgentRate = 0;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT referral_code FROM users WHERE nick_name = ?")) {
                ps.setString(1, userNick);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) refCode = rs.getString("referral_code");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nickname, commission_rate FROM vinplay_admin.useragent WHERE nickname = ? OR username = ?")) {
                ps.setString(1, userNick);
                ps.setString(2, userNick);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        selfAgentId = rs.getInt("id");
                        selfAgentRate = rs.getDouble("commission_rate");
                    }
                }
            }
        }

        if ((refCode == null || refCode.isEmpty()) && selfAgentId <= 0) {
            // No agent chain — nothing to do
            return;
        }

        // 2. Build agent chain
        List<AgentRate> chain = (refCode != null && !refCode.isEmpty())
                ? buildAgentChain(refCode) : new ArrayList<>();

        // Self-rebate
        if (selfAgentId > 0) {
            boolean alreadyInChain = false;
            for (AgentRate ar : chain) {
                if (ar.agentId == selfAgentId) { alreadyInChain = true; break; }
            }
            if (!alreadyInChain) {
                chain.add(0, new AgentRate(selfAgentId, selfAgentRate));
            }
        }

        if (chain.isEmpty()) return;

        // 3. Calculate differential commission
        List<Distribution> distributions = DifferentialCommissionCalculator.calculate(chain, volume);

        // 4. Insert PENDING rebate_logs
        for (Distribution d : distributions) {
            String rebateType = (selfAgentId > 0 && d.agentId == selfAgentId) ? "SELF" : "DOWNLINE";
            insertPendingLog(d, userNick, volume, today, today, "DAILY", rebateType);
        }
    }

    private static List<AgentRate> buildAgentChain(String refCode) throws Exception {
        List<AgentRate> chain = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            String currentCode = refCode;
            int maxDepth = 10;
            while (currentCode != null && !currentCode.isEmpty() && maxDepth-- > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, parentid, code, commission_rate FROM useragent WHERE code = ? OR nickname = ?")) {
                    ps.setString(1, currentCode);
                    ps.setString(2, currentCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) break;
                        int agentId = rs.getInt("id");
                        double rate = rs.getDouble("commission_rate");
                        int parentId = rs.getInt("parentid");
                        // Avoid duplicate
                        boolean dup = false;
                        for (AgentRate ar : chain) if (ar.agentId == agentId) { dup = true; break; }
                        if (!dup) chain.add(new AgentRate(agentId, rate));
                        // Walk up
                        if (parentId <= 0) break;
                        try (PreparedStatement ps2 = conn.prepareStatement("SELECT code FROM useragent WHERE id = ?")) {
                            ps2.setInt(1, parentId);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                currentCode = rs2.next() ? rs2.getString("code") : null;
                            }
                        }
                    }
                }
            }
        }
        return chain;
    }

    private static long insertPendingLog(Distribution d, String userNick, long volume,
                                         String periodStart, String periodEnd, String periodType, String rebateType) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // Get agent nickname
            String agentNick = "";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nickname FROM vinplay_admin.useragent WHERE id = ?")) {
                ps.setInt(1, d.agentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) agentNick = rs.getString("nickname");
                }
            }

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO rebate_logs (agent_user_id, agent_nickname, agent_level, " +
                "period_start, period_end, period_type, total_f1_volume, " +
                "rebate_percentage, rebate_amount, share_percentage, own_percentage, " +
                "child_percentage, differential_pct, share_amount, net_rebate, " +
                "status, note, rebate_type, player_nickname) " +
                "VALUES (?,?,0, ? ,?, ?, ?, ?,?,0,?,?,?,0,?, 'PENDING', ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS);
            int idx = 1;
            ps.setInt(idx++, d.agentId);
            ps.setString(idx++, agentNick);
            ps.setString(idx++, periodStart + " 00:00:00");
            ps.setString(idx++, periodEnd + " 23:59:59");
            ps.setString(idx++, periodType);
            ps.setLong(idx++, volume);
            ps.setDouble(idx++, d.ownPct);
            ps.setLong(idx++, d.amount);
            ps.setDouble(idx++, d.ownPct);
            ps.setDouble(idx++, d.childPct);
            ps.setDouble(idx++, d.differentialPct);
            ps.setLong(idx++, d.amount);
            ps.setString(idx++, "Auto-cron from user=" + userNick);
            ps.setString(idx++, rebateType);
            // SUN-1201: cron rollup is per-(agent, player, day); game_action
            // is intentionally NULL (cron sums all games for the day —
            // mapLog renders this as "Tổng hợp").
            ps.setString(idx, userNick);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            long logId = keys.next() ? keys.getLong(1) : -1;
            keys.close();
            ps.close();
            return logId;
        } catch (Exception e) {
            logger.error(TAG + "insertPendingLog error agentId=" + d.agentId, e);
            return -1;
        }
    }

    private static void updateWatermark(int userId, long newWatermark) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE user_volume_tracking SET commission_volume_processed = ? WHERE user_id = ?")) {
            ps.setLong(1, newWatermark);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error(TAG + "updateWatermark error userId=" + userId, e);
        }
    }

    private static class VolumeEntry {
        int userId;
        String nickName;
        long totalVolume;
        long processedVolume;
    }
}
