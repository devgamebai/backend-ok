package game.BotJackpotTimer;

import bitzero.util.common.business.Debug;
import game.GameConfig.GameConfig;
import game.modules.GameUtil;
import game.modules.slot.ThanTaiModule;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.ThanTaiRoom;

import java.util.List;

public class BotJackPotThanTaiTimer implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_100 = 0;
    private long timeJackPot_1000 = 0;
    private long timeJackPot_10000 = 0;
    ThanTaiModule thanTaiModule;

    public BotJackPotThanTaiTimer(long[] timeJackpot, ThanTaiModule thanTaiModule) {
        this.timeJackPot_100 = timeJackpot[0];
        this.timeJackPot_1000 = timeJackpot[1];
        this.timeJackPot_10000 = timeJackpot[2];
        this.thanTaiModule = thanTaiModule;
    }

    @Override
    public void run() {
        try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
            if (!this.isInitBot) {
                try {
                    this.bots = BotMinigame.getBotsJackPot(500, "vin");
                } catch (Throwable e) {
                    Debug.warn("error init bot");
                }
                this.isInitBot = true;
            }

            ThanTaiRoom room100 = (ThanTaiRoom) this.thanTaiModule.rooms.get(this.thanTaiModule.gameName + "_vin_100");
            ThanTaiRoom room1000 = (ThanTaiRoom) this.thanTaiModule.rooms.get(this.thanTaiModule.gameName + "_vin_1000");
            ThanTaiRoom room10000 = (ThanTaiRoom) this.thanTaiModule.rooms.get(this.thanTaiModule.gameName + "_vin_10000");

            long currentTime = GameUtil.getTimeStampInSeconds();
            int time = 0;
            if (currentTime > timeJackPot_100 && room100 != null) {
                timeJackPot_100 = currentTime + 300;
                room100.botEatJackpot(this.thanTaiModule.keyBotJackpotThanTai +
                        "_vin_100", timeJackPot_100, this.bots.get(GameUtil.randomMax(this.bots.size())));
            }
            if (currentTime > timeJackPot_1000 && room1000 != null) {
                timeJackPot_1000 = currentTime + 300;
                room1000.botEatJackpot(this.thanTaiModule.keyBotJackpotThanTai +
                        "_vin_1000", timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
            }
            if (currentTime > timeJackPot_10000 && room10000 != null) {
                timeJackPot_10000 = currentTime + 300;
                room10000.botEatJackpot(this.thanTaiModule.keyBotJackpotThanTai +
                        "_vin_10000", timeJackPot_10000, this.bots.get(GameUtil.randomMax(this.bots.size())));
            }
        } catch (Exception e) {
            Debug.trace(e);
        }
    }
}
