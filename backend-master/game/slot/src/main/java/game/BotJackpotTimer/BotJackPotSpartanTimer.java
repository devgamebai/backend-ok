package game.BotJackpotTimer;

import bitzero.util.common.business.Debug;
import com.pengrad.telegrambot.model.Game;
import game.GameConfig.BotJackpotConfig.SlotSpartanBotConfig;
import game.GameConfig.GameConfig;
import game.modules.GameUtil;
import game.modules.slot.SpartanModule;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.SpartanRoom;

import java.util.List;

public class BotJackPotSpartanTimer implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_100 = 0;
    private long timeJackPot_1000 = 0;
    private long timeJackPot_5000 = 0;
    private long timeJackPot_10000 = 0;
    SpartanModule spartanModule;

    public BotJackPotSpartanTimer(long[] timeJackpot, SpartanModule spartanModule){
        this.timeJackPot_100 = timeJackpot[0];
        this.timeJackPot_1000 = timeJackpot[1];
        this.timeJackPot_5000 = timeJackpot[2];
        this.timeJackPot_10000 = timeJackpot[3];
        this.spartanModule = spartanModule;
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
           int moneyToJackPot_100 = GameConfig.getInstance().slotSpartanBotConfig.randomJackPotPer5s(0);
           SpartanRoom room100 = (SpartanRoom)this.spartanModule.rooms.get(this.spartanModule.gameName + "_vin_100");
           room100.addMoneyToPot(moneyToJackPot_100);
           int moneyToJackPot_1000 = GameConfig.getInstance().slotSpartanBotConfig.randomJackPotPer5s(1);
           SpartanRoom room1000 = (SpartanRoom)this.spartanModule.rooms.get(this.spartanModule.gameName + "_vin_1000");
           room1000.addMoneyToPot(moneyToJackPot_1000);
//           int moneyToJackPot_5000 = GameConfig.getInstance().slotSpartanBotConfig.randomJackPotPer5s(2);
//           SpartanRoom room5000 = (SpartanRoom)this.spartanModule.rooms.get(this.spartanModule.gameName + "_vin_5000");
//           room5000.addMoneyToPot(moneyToJackPot_5000);
           int moneyToJackPot_10000 = GameConfig.getInstance().slotSpartanBotConfig.randomJackPotPer5s(3);
           SpartanRoom room10000 = (SpartanRoom)this.spartanModule.rooms.get(this.spartanModule.gameName + "_vin_10000");
           room10000.addMoneyToPot(moneyToJackPot_10000);
//
           long currentTime = GameUtil.getTimeStampInSeconds();
           int time = 0;
           if(currentTime > timeJackPot_100){
               time = GameConfig.getInstance().slotSpartanBotConfig.randomTimeBotEat(0);
               timeJackPot_100 = currentTime + time;
               room100.botEatJackpot(this.spartanModule.keyBotJackpotSlot11IconWild +
                       "_vin_100",timeJackPot_100, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
           if(currentTime > timeJackPot_1000){
               time = GameConfig.getInstance().slotSpartanBotConfig.randomTimeBotEat(1);
               timeJackPot_1000 = currentTime + time;
               room1000.botEatJackpot(this.spartanModule.keyBotJackpotSlot11IconWild +
                       "_vin_1000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
//           if(currentTime > timeJackPot_5000){
//               time = GameConfig.getInstance().slotSpartanBotConfig.randomTimeBotEat(2);
//               timeJackPot_5000 = currentTime + time;
//               room5000.botEatJackpot(this.spartanModule.keyBotJackpotSlot11IconWild +
//                       "_vin_5000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
//           }
           if(currentTime > timeJackPot_10000){
               time = GameConfig.getInstance().slotSpartanBotConfig.randomTimeBotEat(3);
               timeJackPot_10000 = currentTime + time;
               room10000.botEatJackpot(this.spartanModule.keyBotJackpotSlot11IconWild +
                       "_vin_10000",timeJackPot_10000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
//           Debug.trace("run finish");
       }catch (Exception e){
           Debug.trace(e);
       }
    }
}
