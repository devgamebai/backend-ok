/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.userMission;

import com.vinplay.api.backend.processors.userMission.ResetUserMissionTask;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class ResetUserMissionUtils {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public static void init() {
        try {
            Timer timer = new Timer();
            timer.schedule((TimerTask)new ResetUserMissionTask(), DateTimeUtils.getSundayEveryWeek());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
    }
}

