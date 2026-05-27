package game.init;

import bitzero.util.common.business.Debug;
import game.Jetty.JettyUtils;

public class InitTimer implements Runnable {
    @Override
    public void run() {
        this.init();
    }

    private void init() {
        try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
            JettyUtils.jettyInit();
        } catch (Exception e) {
            Debug.trace(e);
        }
    }
}
