/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.api.utils;

import com.vinplay.usercore.utils.GameCommon;

public class CaoThapUtils {

    public static String getPrize(int type, int stt, String platform) {
        String[] prizes = (type == 0) ? getWinPrizesDaily() : getWinPrizesMonthly();
        if (stt > prizes.length) return "";
        return prizes[stt - 1];
    }

    public static String[] getWinPrizesDaily() {
        String [] winPrizesDaily = new String[0];
        try {
            winPrizesDaily = GameCommon.getValueStr("TPS_WIN_PRIZES_DAILY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  winPrizesDaily;
    }

    public static String[] getWinPrizesMonthly() {
        String [] winPrizesMonthly = new String[0];
        try {
            winPrizesMonthly = GameCommon.getValueStr("TPS_WIN_PRIZES_MONTHLY").split(",");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return  winPrizesMonthly;
    }
}

