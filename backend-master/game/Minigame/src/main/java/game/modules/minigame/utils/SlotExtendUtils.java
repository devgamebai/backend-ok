/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package game.modules.minigame.utils;

import org.apache.log4j.Logger;

public class SlotExtendUtils {
    private static Logger logger = Logger.getLogger((String)"csvSlotExtend");
    private static String FORMAT_PLAY_SLOT_EXTEND = ", %10d, %15s, %8d, %20s, %5d, %5d, %10d, %15s, %20s";

    public static void log(long referenceId, String username, int betValue, String matrix, short result, short moneyType, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        logger.debug(String.format(FORMAT_PLAY_SLOT_EXTEND, referenceId, username, betValue, matrixStr, result, moneyType, handleTime, ratioTime, timeLog));
    }
}

