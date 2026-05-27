/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package game.third.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;

public class ScheduleMain {
    private static final Logger logger = Logger.getLogger((String)"thirdPart");
    private static final String LOG_PROPERTIES_FILE = "config/log4j.properties";

    public static void run() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    }
}

