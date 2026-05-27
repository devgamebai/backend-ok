/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.modules.BotJackPotTimer;

import bitzero.util.common.business.Debug;
import game.modules.minigame.entities.BotMinigame;
import java.util.List;

public class BotJackPotCaoThapTimer
implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_1000 = 0L;
    private long timeJackPot_10000 = 0L;
    private long timeJackPot_50000 = 0L;
    private long timeJackPot_100000 = 0L;

    public BotJackPotCaoThapTimer(long[] timeJackpot) {
        this.timeJackPot_1000 = timeJackpot[0];
        this.timeJackPot_10000 = timeJackpot[1];
        this.timeJackPot_50000 = timeJackpot[2];
        this.timeJackPot_100000 = timeJackpot[2];
    }

    @Override
    public void run() {
        block4: {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                if (this.isInitBot) break block4;
                try {
                    this.bots = BotMinigame.getBotsJackPot(500, "vin");
                }
                catch (Throwable e) {
                    Debug.warn((Object[])new Object[]{"error init bot"});
                }
                this.isInitBot = true;
            }
            catch (Exception e) {
                Debug.trace((Object[])new Object[]{e});
            }
        }
    }
}

