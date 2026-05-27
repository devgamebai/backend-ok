package game.BotJackpotTimer;

import bitzero.util.common.business.Debug;

import com.pengrad.telegrambot.model.Game;
import game.GameConfig.BotJackpotConfig.SlotMayBachBotConfig;
import game.GameConfig.GameConfig;
import game.modules.GameUtil;
import game.modules.slot.MayBachModule;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.MayBachRoom;

import java.util.List;

public class BotJackPotMayBachTimer implements Runnable {
    public boolean isInitBot = false;
    public List<String> bots;
    private long timeJackPot_100 = 0;
    private long timeJackPot_1000 = 0;
    private long timeJackPot_10000 = 0;
    MayBachModule mayBachModule;

    public BotJackPotMayBachTimer(long[] timeJackpot, MayBachModule mayBachModule){
        this.timeJackPot_100 = timeJackpot[0];
        this.timeJackPot_1000 = timeJackpot[1];
        this.timeJackPot_10000 = timeJackpot[2];
        this.mayBachModule = mayBachModule;
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
           int moneyToJackPot_100 = GameConfig.getInstance().slotMayBachBotConfig.randomJackPotPer5s(0);
           MayBachRoom room100 = (MayBachRoom)this.mayBachModule.rooms.get(this.mayBachModule.gameName + "_vin_100");
           room100.addMoneyToPot(moneyToJackPot_100);
           int moneyToJackPot_1000 = GameConfig.getInstance().slotMayBachBotConfig.randomJackPotPer5s(1);
           MayBachRoom room1000 = (MayBachRoom)this.mayBachModule.rooms.get(this.mayBachModule.gameName + "_vin_1000");
           room1000.addMoneyToPot(moneyToJackPot_1000);
           int moneyToJackPot_10000 = GameConfig.getInstance().slotMayBachBotConfig.randomJackPotPer5s(2);
           MayBachRoom room10000 = (MayBachRoom)this.mayBachModule.rooms.get(this.mayBachModule.gameName + "_vin_10000");
           room10000.addMoneyToPot(moneyToJackPot_10000);
//
           long currentTime = GameUtil.getTimeStampInSeconds();
           int time = 0;
           if(currentTime > timeJackPot_100){
               time = GameConfig.getInstance().slotMayBachBotConfig.randomTimeBotEat(0);
               timeJackPot_100 = currentTime + time;
               room100.botEatJackpot(this.mayBachModule.keyBotJackpotSlot9Icon +
                       "_vin_100",timeJackPot_100, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
           if(currentTime > timeJackPot_1000){
               time = GameConfig.getInstance().slotMayBachBotConfig.randomTimeBotEat(1);
               timeJackPot_1000 = currentTime + time;
               room1000.botEatJackpot(this.mayBachModule.keyBotJackpotSlot9Icon +
                       "_vin_1000",timeJackPot_1000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
           if(currentTime > timeJackPot_10000){
               time = GameConfig.getInstance().slotMayBachBotConfig.randomTimeBotEat(2);
               timeJackPot_10000 = currentTime + time;
               room10000.botEatJackpot(this.mayBachModule.keyBotJackpotSlot9Icon +
                       "_vin_10000",timeJackPot_10000, this.bots.get(GameUtil.randomMax(this.bots.size())));
           }
//           Debug.trace("run finish");
       }catch (Exception e){
           Debug.trace(e);
       }
    }
}
