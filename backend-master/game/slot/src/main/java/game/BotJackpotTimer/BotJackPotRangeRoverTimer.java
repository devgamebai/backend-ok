package game.BotJackpotTimer;

import bitzero.util.common.business.Debug;
import game.GameConfig.BotJackpotConfig.SlotRangeRoverBotConfig;
import game.GameConfig.GameConfig;
import game.modules.GameUtil;
import game.modules.slot.RangeRoverModule;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.RangeRoverRoom;

import java.util.List;

public class BotJackPotRangeRoverTimer implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_100 = 0;
    private long timeJackPot_1000 = 0;
    private long timeJackPot_5000 = 0;
    private long timeJackPot_10000 = 0;
    RangeRoverModule rangeRoverModule;

    public BotJackPotRangeRoverTimer(long[] timeJackpot, RangeRoverModule rangeRoverModule){
        this.timeJackPot_100 = timeJackpot[0];
        this.timeJackPot_1000 = timeJackpot[1];
        this.timeJackPot_5000 = timeJackpot[2];
        this.timeJackPot_10000 = timeJackpot[3];
        this.rangeRoverModule = rangeRoverModule;
    }

    @Override
    public void run() {
       try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
           //Debug.trace("BotJackPotMinipokerTimer");
           if (!this.isInitBot) {
               try{
                   this.bots = BotMinigame.getBotsJackPot(500, "vin");
               }catch (Throwable e){
                   Debug.warn("error init bot");
               }
               this.isInitBot = true;

           }
           int moneyToJackPot_100 = GameConfig.getInstance().slotRangeRoverBotConfig.randomJackPotPer5s(0);
           RangeRoverRoom room100 = (RangeRoverRoom)this.rangeRoverModule.rooms.get(this.rangeRoverModule.gameName + "_vin_100");
           room100.addMoneyToPot(moneyToJackPot_100);
           int moneyToJackPot_1000 = GameConfig.getInstance().slotRangeRoverBotConfig.randomJackPotPer5s(1);
           RangeRoverRoom room1000 = (RangeRoverRoom)this.rangeRoverModule.rooms.get(this.rangeRoverModule.gameName + "_vin_1000");
           room1000.addMoneyToPot(moneyToJackPot_1000);
//           int moneyToJackPot_5000 = GameConfig.getInstance().slotRangeRoverBotConfig.randomJackPotPer5s(2);
//           RangeRoverRoom room5000 = (RangeRoverRoom)this.rangeRoverModule.rooms.get(this.rangeRoverModule.gameName + "_vin_5000");
//           room5000.addMoneyToPot(moneyToJackPot_5000);
           int moneyToJackPot_10000 = GameConfig.getInstance().slotRangeRoverBotConfig.randomJackPotPer5s(3);
           RangeRoverRoom room10000 = (RangeRoverRoom)this.rangeRoverModule.rooms.get(this.rangeRoverModule.gameName + "_vin_10000");
           room10000.addMoneyToPot(moneyToJackPot_10000);
//
           long currentTime = GameUtil.getTimeStampInSeconds();
           int time = 0;
           if(currentTime > timeJackPot_100){
               time = GameConfig.getInstance().slotRangeRoverBotConfig.randomTimeBotEat(0);
               timeJackPot_100 = currentTime + time;
               room100.botEatJackpot(this.rangeRoverModule.keyBotJackpotSlot7IconWild +
                       "_vin_100",timeJackPot_100, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
           if(currentTime > timeJackPot_1000){
               time = GameConfig.getInstance().slotRangeRoverBotConfig.randomTimeBotEat(1);
               timeJackPot_1000 = currentTime + time;
               room1000.botEatJackpot(this.rangeRoverModule.keyBotJackpotSlot7IconWild +
                       "_vin_1000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
//           if(currentTime > timeJackPot_5000){
//               time = GameConfig.getInstance().slotRangeRoverBotConfig.randomTimeBotEat(2);
//               timeJackPot_5000 = currentTime + time;
//               room1000.botEatJackpot(this.rangeRoverModule.keyBotJackpotSlot7IconWild +
//                       "_vin_5000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
//           }
           if(currentTime > timeJackPot_10000){
               time = GameConfig.getInstance().slotRangeRoverBotConfig.randomTimeBotEat(3);
               timeJackPot_10000 = currentTime + time;
               room10000.botEatJackpot(this.rangeRoverModule.keyBotJackpotSlot7IconWild +
                       "_vin_10000",timeJackPot_10000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
//           Debug.trace("run finish");
       }catch (Exception e){
           Debug.trace(e);
       }
    }
}
