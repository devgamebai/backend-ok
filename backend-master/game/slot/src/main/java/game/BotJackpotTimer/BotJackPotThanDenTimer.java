package game.BotJackpotTimer;

import bitzero.util.common.business.Debug;
import game.GameConfig.GameConfig;
import game.modules.GameUtil;
import game.modules.slot.ThanDenModule;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.ThanDenRoom;

import java.util.List;

public class BotJackPotThanDenTimer implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_100 = 0;
    private long timeJackPot_1000 = 0;
    private long timeJackPot_10000 = 0;
    ThanDenModule thandenModule;

    public BotJackPotThanDenTimer(long[] timeJackpot, ThanDenModule thandenModule){
        this.timeJackPot_100 = timeJackpot[0];
        this.timeJackPot_1000 = timeJackpot[1];
        this.timeJackPot_10000 = timeJackpot[2];
        this.thandenModule = thandenModule;
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
           int moneyToJackPot_100 = GameConfig.getInstance().slotChiemtinhBotConfig.randomJackPotPer5s(0);
           ThanDenRoom room100 = (ThanDenRoom)this.thandenModule.rooms.get(this.thandenModule.gameName + "_vin_100");
           room100.addMoneyToPot(moneyToJackPot_100);
           int moneyToJackPot_1000 = GameConfig.getInstance().slotChiemtinhBotConfig.randomJackPotPer5s(1);
           ThanDenRoom room1000 = (ThanDenRoom)this.thandenModule.rooms.get(this.thandenModule.gameName + "_vin_1000");
           room1000.addMoneyToPot(moneyToJackPot_1000);
           int moneyToJackPot_10000 = GameConfig.getInstance().slotChiemtinhBotConfig.randomJackPotPer5s(2);
           ThanDenRoom room10000 = (ThanDenRoom)this.thandenModule.rooms.get(this.thandenModule.gameName + "_vin_10000");
           room10000.addMoneyToPot(moneyToJackPot_10000);

           long currentTime = GameUtil.getTimeStampInSeconds();
           int time = 0;
           if(currentTime > timeJackPot_100){
               time = GameConfig.getInstance().slotChiemtinhBotConfig.randomTimeBotEat(0);
               timeJackPot_100 = currentTime + time;
               room100.botEatJackpot(this.thandenModule.keyBotJackpotSlot11IconWild +
                       "_vin_100",timeJackPot_100, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
           if(currentTime > timeJackPot_1000){
               time = GameConfig.getInstance().slotChiemtinhBotConfig.randomTimeBotEat(1);
               timeJackPot_1000 = currentTime + time;
               room1000.botEatJackpot(this.thandenModule.keyBotJackpotSlot11IconWild +
                       "_vin_1000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
           if(currentTime > timeJackPot_10000){
               time = GameConfig.getInstance().slotChiemtinhBotConfig.randomTimeBotEat(2);
               timeJackPot_10000 = currentTime + time;
               room10000.botEatJackpot(this.thandenModule.keyBotJackpotSlot11IconWild +
                       "_vin_10000",timeJackPot_10000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
       }catch (Exception e){
           Debug.trace(e);
       }
    }
}
