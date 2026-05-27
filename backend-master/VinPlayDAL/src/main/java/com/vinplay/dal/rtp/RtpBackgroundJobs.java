package com.vinplay.dal.rtp;

import com.vinplay.vbee.common.models.rtp.GameRtpSchedule;
import com.vinplay.vbee.common.models.rtp.RtpAutoHistory;
import com.vinplay.vbee.common.models.rtp.RtpAutoPolicy;
import org.apache.log4j.Logger;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RtpBackgroundJobs {
    private static final Logger logger = Logger.getLogger("api");
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static RtpScheduleService scheduleService = new RtpScheduleService();
    private static RtpAutoTargeterService autoTargeterService = new RtpAutoTargeterService();
    private static RtpConfigService configService = new RtpConfigService();
    private static PnlService pnlService = new PnlService();

    public static void start() {
        // Scheduler Job: Every 1 minute
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processSchedules();
            } catch (Exception e) {
                logger.error("Error in processSchedules", e);
            }
        }, 1, 1, TimeUnit.MINUTES);

        // Auto-Targeter Job: Every 30 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processAutoTargeting();
            } catch (Exception e) {
                logger.error("Error in processAutoTargeting", e);
            }
        }, 5, 30, TimeUnit.MINUTES);

        logger.info("RTP Background Jobs started successfully.");
    }

    private static void processSchedules() {
        List<GameRtpSchedule> schedules = scheduleService.listActiveSchedules();
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (GameRtpSchedule s : schedules) {
            try {
                if (CronExpression.isValidExpression(s.getCronExpr())) {
                    CronExpression cron = new CronExpression(s.getCronExpr());
                    Date lastFired = s.getLastFiredAt() != null ? sdf.parse(s.getLastFiredAt()) : null;
                    Date nextFire = lastFired == null ? cron.getNextValidTimeAfter(new Date(now.getTime() - 60000)) : cron.getNextValidTimeAfter(lastFired);

                    if (nextFire != null && nextFire.before(now)) {
                        logger.info("Triggering RTP Schedule for Game: " + s.getGameCode() + " -> " + s.getWinRatePct() + "%");
                        try { configService.updateGameConfig(s.getGameCode(), s.getWinRatePct(), "Scheduled RTP change", "SYSTEM"); } catch (java.sql.SQLException e) { logger.error("Schedule RTP update failed", e); }
                        scheduleService.updateLastFiredAt(s.getId());
                        
                        // If there is duration, maybe auto revert? For now, we rely on the Admin
                        // handling duration via another cron schedule, or we leave 'revert' out of scope for MVP
                    }
                }
            } catch (ParseException e) {
                logger.error("Invalid cron expr for schedule " + s.getId(), e);
            }
        }
    }

    private static void processAutoTargeting() {
        List<RtpAutoPolicy> policies = autoTargeterService.listActivePolicies();
        if (policies.isEmpty()) return;

        for (RtpAutoPolicy policy : policies) {
            // Find users who have won more than maxWinAmount in timeWindowMin
            String startTime = getPastTime(policy.getTimeWindowMin());
            String endTime = sdf().format(new Date());

            // Get Pnl grouped by user in this timeframe
            // We reuse PnlService. We need a way to get top winners.
            // Temporarily fetching top 50 winners using PnlService
            List<Map<String, Object>> topWinners = pnlService.getTopWinners(startTime, endTime, 50);
            
            for (Map<String, Object> winner : topWinners) {
                int userId = (Integer) winner.get("user_id");
                String nickName = (String) winner.get("nick_name");
                long netWin = (Long) winner.get("net_win"); // Assume this is returned

                if (netWin >= policy.getMaxWinAmount()) {
                    logger.info("Auto-Targeter caught user: " + nickName + " winning " + netWin + " >= " + policy.getMaxWinAmount());
                    // Apply override
                    String expiresAt = getFutureTime(policy.getActionDuration());
                    try { configService.setUserOverride(userId, "ALL", policy.getActionRtpPct(), expiresAt, "Auto-target: win=" + netWin, "SYSTEM"); } catch (java.sql.SQLException e) { logger.error("Auto-target override failed for user " + nickName, e); }
                    
                    // Log history
                    RtpAutoHistory history = new RtpAutoHistory();
                    history.setUserId(userId);
                    history.setNickName(nickName);
                    history.setPolicyId(policy.getId());
                    history.setTriggerWin(netWin);
                    history.setAppliedRtp(policy.getActionRtpPct());
                    history.setExpiresAt(expiresAt);
                    autoTargeterService.logHistory(history);
                }
            }
        }
    }

    private static SimpleDateFormat sdf() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    private static String getPastTime(int minutesAgo) {
        long time = System.currentTimeMillis() - ((long) minutesAgo * 60 * 1000);
        return sdf().format(new Date(time));
    }

    private static String getFutureTime(int minutesAhead) {
        long time = System.currentTimeMillis() + ((long) minutesAhead * 60 * 1000);
        return sdf().format(new Date(time));
    }
}
