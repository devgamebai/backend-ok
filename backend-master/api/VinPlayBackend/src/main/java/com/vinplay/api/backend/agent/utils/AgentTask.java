/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.agent.utils;

import com.vinplay.api.backend.agent.utils.AgentUtils;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class AgentTask
extends TimerTask {
    private static final Logger logger = Logger.getLogger((String)"backend");

    @Override
    public void run() {
        try {
            Timer timer = new Timer();
            timer.schedule((TimerTask)new AgentTask(), AgentUtils.getFirstDayAfterMonth());
            AgentUtils.refundFeeAgent();
            AgentUtils.bonusTopDoanhSo();
        }
        catch (Exception e) {
            logger.debug((Object)e);
            e.printStackTrace();
        }
    }
}

