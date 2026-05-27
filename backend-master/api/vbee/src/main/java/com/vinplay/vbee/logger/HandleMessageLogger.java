/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.logger;

import com.vinplay.vbee.common.utils.CommonUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;

public class HandleMessageLogger {
    private static Logger logger = Logger.getLogger((String)"csvHandleMessage");
    private static final String FORMAT_HANDLE_MESSGAE_LOG = ",%20s,\t%5d,\t%6d,\t%10s,\t%10s";
    private static final String FORMATE_DEFAULT_TIME = "HH:mm:ss dd-MM-yyyy";

    public static void log(String queueName, int commandId, long handleTime, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATE_DEFAULT_TIME);
        String handleRatio = CommonUtils.getRatioTime((long)handleTime);
        logger.debug((Object)String.format(FORMAT_HANDLE_MESSGAE_LOG, queueName, commandId, handleTime, handleRatio, sdf.format(date)));
    }
}

