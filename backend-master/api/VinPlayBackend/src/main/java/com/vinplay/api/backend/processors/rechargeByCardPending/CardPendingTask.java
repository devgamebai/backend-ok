/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.service.impl.RechargeServiceImpl
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.rechargeByCardPending;

import com.vinplay.dichvuthe.service.impl.RechargeServiceImpl;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class CardPendingTask
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
            RechargeServiceImpl rechargeService = new RechargeServiceImpl();
            logger.info((Object)("Recheck recharge by card " + date + ". Start time: " + new Date()));
            rechargeService.reCheckRechargeByCard();
            logger.info((Object)("Recheck recharge by card " + date + " success at " + new Date()));
        }
        catch (Exception e) {
            logger.debug((Object)e);
            e.printStackTrace();
        }
    }
}

