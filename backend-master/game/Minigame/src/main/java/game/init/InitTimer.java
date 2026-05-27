/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.init;

import bitzero.util.common.business.Debug;
import game.Jetty.JettyUtils;

public class InitTimer
implements Runnable {
    @Override
    public void run() {
        this.init();
    }

    private void init() {
        try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
            JettyUtils.jettyInit();
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
    }
}

