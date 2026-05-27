package game.BotJackpotTimer;

import bitzero.util.common.business.Debug;
import game.GameConfig.GameConfig;
import game.modules.GameUtil;
import game.modules.slot.BikiniModule;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.BikiniRoom;

import java.util.List;

public class BotJackPotBikiniTimer  implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_100 = 0;
    private long timeJackPot_1000 = 0;
    private long timeJackPot_10000 = 0;
    BikiniModule bikiniModule;

    public BotJackPotBikiniTimer(long[] timeJackpot, BikiniModule bikiniModule){
        this.timeJackPot_100 = timeJackpot[0];
        this.timeJackPot_1000 = timeJackpot[1];
        this.timeJackPot_10000 = timeJackpot[2];
        this.bikiniModule = bikiniModule;
    }

    @Override
    public void run() {
        try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
            if (!this.isInitBot) {
                try{
                    this.bots = BotMinigame.getBotsJackPot(500, "vin");
                }catch (Throwable e){
                    Debug.warn("error init bot");
                }
                this.isInitBot = true;

            }
            int moneyToJackPot_100 = GameConfig.getInstance().slotBikiniBotConfig.randomJackPotPer5s(0);
            BikiniRoom room100 = (BikiniRoom)this.bikiniModule.rooms.get(this.bikiniModule.gameName + "_vin_100");
            room100.addMoneyToPot(moneyToJackPot_100);
            int moneyToJackPot_1000 = GameConfig.getInstance().slotBikiniBotConfig.randomJackPotPer5s(1);
            BikiniRoom room1000 = (BikiniRoom)this.bikiniModule.rooms.get(this.bikiniModule.gameName + "_vin_1000");
            room1000.addMoneyToPot(moneyToJackPot_1000);
            int moneyToJackPot_10000 = GameConfig.getInstance().slotBikiniBotConfig.randomJackPotPer5s(2);
            BikiniRoom room10000 = (BikiniRoom)this.bikiniModule.rooms.get(this.bikiniModule.gameName + "_vin_10000");
            room10000.addMoneyToPot(moneyToJackPot_10000);
//
            long currentTime = GameUtil.getTimeStampInSeconds();
            int time = 0;
            if(currentTime > timeJackPot_100){
                time = GameConfig.getInstance().slotBikiniBotConfig.randomTimeBotEat(0);
                timeJackPot_100 = currentTime + time;
                room100.botEatJackpot(this.bikiniModule.keyBotJackpotSlot11IconWild +
                        "_vin_100",timeJackPot_100, this.bots.get(GameUtil.randomMax(this.bots.size())));
            }

            if(currentTime > timeJackPot_1000){
                time = GameConfig.getInstance().slotBikiniBotConfig.randomTimeBotEat(1);
                timeJackPot_1000 = currentTime + time;
                room1000.botEatJackpot(this.bikiniModule.keyBotJackpotSlot11IconWild +
                        "_vin_1000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
            }

            if(currentTime > timeJackPot_10000){
                time = GameConfig.getInstance().slotBikiniBotConfig.randomTimeBotEat(2);
                timeJackPot_10000 = currentTime + time;
                room10000.botEatJackpot(this.bikiniModule.keyBotJackpotSlot11IconWild +
                        "_vin_10000",timeJackPot_10000, this.bots.get(GameUtil.randomMax(this.bots.size())));
            }
        }catch (Exception e){
            Debug.trace(e);
        }
    }
}
