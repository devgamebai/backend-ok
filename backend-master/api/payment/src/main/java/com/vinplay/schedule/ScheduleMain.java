/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.schedule;

import com.vinplay.schedule.task.TaskSendPaymentReport;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleMain {
    public static void run() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);
        executor.scheduleAtFixedRate(new TaskSendPaymentReport(), 0L, 5L, TimeUnit.MINUTES);
    }
}

