package com.vinplay.vbee.deposit;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 * Bootstraps all deposit Telegram bot components:
 *   - DepositTelegramBot (Telegram API client)
 *   - DepositTransactionDao (database access)
 *   - DepositQueueConsumer (RabbitMQ processor, initialized for shared state)
 *   - DepositTelegramPoller (long-polling thread for button callbacks)
 *   - DepositRecoveryWorker (periodic sweep for stale/orphaned transactions)
 *
 * Called from VBeeMain after RMQ.start().
 * Gracefully skips if TELEGRAM_DEPOSIT_BOT_TOKEN is not set.
 */
public class DepositBotLauncher {

    private static final Logger logger = Logger.getLogger("api");

    private static DepositTelegramPoller poller;
    private static ScheduledExecutorService scheduler;

    /**
     * Start the deposit Telegram bot subsystem.
     * Safe to call even if env vars are not configured — will log and skip.
     */
    public static void start() {
        try {
            // SUN-796: when vbee is scaled horizontally (vbee, vbee-2, …),
            // only ONE instance should run the Telegram long-poll — otherwise
            // Telegram returns HTTP 409 "terminated by other getUpdates
            // request" on every poll. Replica instances set DEPOSIT_BOT_ENABLED=false.
            String enabled = System.getenv("DEPOSIT_BOT_ENABLED");
            if (enabled != null && ("false".equalsIgnoreCase(enabled) || "0".equals(enabled))) {
                logger.info("DepositBotLauncher: disabled via DEPOSIT_BOT_ENABLED=" + enabled
                        + " (expected on vbee replicas other than the primary)");
                return;
            }

            String token = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
            String chatId = System.getenv("TELEGRAM_DEPOSIT_CHAT_ID");

            if (token == null || token.isEmpty()) {
                logger.info("DepositBotLauncher: TELEGRAM_DEPOSIT_BOT_TOKEN not set, deposit Telegram bot disabled");
                return;
            }
            if (chatId == null || chatId.isEmpty()) {
                logger.info("DepositBotLauncher: TELEGRAM_DEPOSIT_CHAT_ID not set, deposit Telegram bot disabled");
                return;
            }

            logger.info("DepositBotLauncher: initializing deposit Telegram bot...");

            // 1. Create shared instances
            DepositTelegramBot bot = new DepositTelegramBot();
            DepositTransactionDao dao = new DepositTransactionDao();

            // 2. Initialize the RabbitMQ processor with shared state
            DepositQueueConsumer.init(bot, dao);

            // 3. Start the Telegram callback poller thread
            poller = new DepositTelegramPoller(bot, dao);
            Thread pollerThread = new Thread(poller, "deposit-telegram-poller");
            pollerThread.setDaemon(true);
            pollerThread.start();
            logger.info("DepositBotLauncher: Telegram poller thread started");

            // 4. Start the recovery worker (every 5 minutes, first run after 1 minute)
            DepositRecoveryWorker recovery = new DepositRecoveryWorker(bot, dao);
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(recovery, 1, 5, TimeUnit.MINUTES);
            logger.info("DepositBotLauncher: recovery worker scheduled (every 5 min)");

            logger.info("DepositBotLauncher: deposit Telegram bot started successfully");

        } catch (Exception e) {
            logger.error("DepositBotLauncher: failed to start deposit Telegram bot (non-fatal)", e);
        }
    }

    /**
     * Gracefully stop all deposit bot components.
     */
    public static void stop() {
        try {
            if (poller != null) {
                poller.stop();
                logger.info("DepositBotLauncher: poller stopped");
            }
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler.awaitTermination(10, TimeUnit.SECONDS);
                logger.info("DepositBotLauncher: scheduler stopped");
            }
        } catch (Exception e) {
            logger.error("DepositBotLauncher: error during shutdown", e);
        }
    }
}
