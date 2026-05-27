/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.api.processors.vippoint;

import com.vinplay.api.processors.vippoint.TopVippoint;
import java.util.TimerTask;

public class TopEventVippointTask
extends TimerTask {
    @Override
    public void run() {
        try {
            TopVippoint.getTop();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

