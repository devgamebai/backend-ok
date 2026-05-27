/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.service.impl.CashOutServiceImpl
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.cashOutByCardPending;

import com.vinplay.dichvuthe.service.impl.CashOutServiceImpl;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class CashOutByCardPendingTask
extends TimerTask {
    private static final Logger logger = Logger.getLogger((String)"report");

    @Override
    public void run() {
        try {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(11);
            int minute = cal.get(12);
            String date = "";
            date = hour == 0 && minute == 30 ? VinPlayUtils.getYesterday() : VinPlayUtils.getCurrentDate();
            CashOutServiceImpl service = new CashOutServiceImpl();
            logger.info((Object)("Recheck cashout by card " + date + ". Start time: " + new Date()));
            service.reCheckCashOutByCard();
            logger.info((Object)("Recheck cashout by card " + date + " success at " + new Date()));
        }
        catch (Exception e) {
            logger.debug((Object)e);
            e.printStackTrace();
        }
    }
}

