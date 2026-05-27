/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.cashOutByCardPending;

import java.util.Calendar;
import org.apache.log4j.Logger;

public class CashOutByCardPendingUtils {
    private static final Logger logger = Logger.getLogger((String)"report");

    public static void init() {
        try {
            int period = 60;
            Calendar cal = Calendar.getInstance();
            int minute = cal.get(12);
            while (minute % 60 != 0) {
                if (minute >= 59) {
                    minute = 0;
                    cal.add(11, 1);
                    break;
                }
                ++minute;
            }
            cal.set(12, minute);
            cal.set(13, 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
    }
}

