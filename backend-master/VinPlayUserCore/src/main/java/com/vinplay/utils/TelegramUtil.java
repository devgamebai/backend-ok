package com.vinplay.utils;

import org.apache.log4j.Logger;

/**
 * Simplified TelegramUtil - logs messages instead of sending to Telegram.
 * The original version requires TelegramAlert, okhttp3, payment entities etc.
 * which are not available in Backend2Dx.
 */
public class TelegramUtil {

    public static final Logger LOGGER = Logger.getLogger(TelegramUtil.class);

    public static boolean sendMessage(String message, String chatId, String bootToken) {
        LOGGER.info("[TELEGRAM] " + message);
        return true;
    }

    public static boolean warningCheat(String message) {
        LOGGER.warn("[TELEGRAM-WARNING] " + message);
        return true;
    }

    public static void BotMD5(String message) {
        LOGGER.info("[TELEGRAM-BOT-MD5] " + message);
    }

    public static boolean pushNotificationDeposit(String message) {
        LOGGER.info("[TELEGRAM-DEPOSIT] " + message);
        return true;
    }

    public static boolean pushNotificationLode(String message) {
        LOGGER.info("[TELEGRAM-LODE] " + message);
        return true;
    }

    public String pushMessageTelegram(String msg) {
        LOGGER.info("[TELEGRAM-MSG] " + msg);
        return "ok";
    }
}
