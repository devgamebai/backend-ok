/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;

public class MoneyLogger {
    private static Logger loggerVin = Logger.getLogger((String)"csvMoneyVin");
    private static Logger loggerXu = Logger.getLogger((String)"csvMoneyXu");
    private static final String FORMAT_HANDLE_MESSGAE_LOG = ",%15s,%8s,%11d,%5d,%20s,%5s,%15s,%15s";
    private static final String FORMATE_DEFAULT_TIME = "HH:mm:ss dd-MM-yyyy";
    private static Logger logGameVin = Logger.getLogger((String)"csvGameVin");
    private static Logger logGameXu = Logger.getLogger((String)"csvGameXu");
    private static final String FORMAT_HANDLE_MESSGAE_LOG_GAME = ",%5s,%5s,%8s,%15s,%11s,%10s,%20s,%15s";

    public static void log(String nickname, String gamename, long money, long fee, String moneyType, String description, String errorCode, String exception) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATE_DEFAULT_TIME);
        if (moneyType.equals("vin")) {
            loggerVin.debug(String.format(FORMAT_HANDLE_MESSGAE_LOG, nickname, gamename, money, fee, description, errorCode, exception, sdf.format(new Date())));
        } else {
            loggerXu.debug(String.format(FORMAT_HANDLE_MESSGAE_LOG, nickname, gamename, money, fee, description, errorCode, exception, sdf.format(new Date())));
        }
    }

    public static void logGame(String moneyType, String moneyBet, String roomId, String gameId, String nickname, String money, String action, String description) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATE_DEFAULT_TIME);
        if (moneyType.equals("vin")) {
            logGameVin.debug(String.format(FORMAT_HANDLE_MESSGAE_LOG_GAME, moneyBet, roomId, gameId, nickname, money, action, description, sdf.format(new Date())));
        } else {
            logGameXu.debug(String.format(FORMAT_HANDLE_MESSGAE_LOG_GAME, moneyBet, roomId, gameId, nickname, money, action, description, sdf.format(new Date())));
        }
    }
    public static void logGameThe(String moneyType){
        loggerXu.debug(moneyType);
    }
}

