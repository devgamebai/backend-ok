package game.modules.slot;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.utils.CommonUtils;
import game.modules.slot.cmd.rev.avengers.AutoPlayAvengersCmd;
import game.modules.slot.cmd.rev.avengers.ChangeRoomAvengersCmd;
import game.modules.slot.cmd.rev.avengers.PlayAvengersCmd;
import game.modules.slot.cmd.rev.avengers.SubscribeAvengersCmd;
import game.modules.slot.cmd.rev.avengers.UnSubscribeAvengersCmd;
import game.modules.slot.cmd.rev.khobau.AutoPlayKhoBauCmd;
import game.modules.slot.cmd.rev.khobau.MinimizeKhoBauCmd;
import game.modules.slot.cmd.send.avengers.AvengersInfoMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.AvengerRoom;
import game.modules.slot.room.KhoBauRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AvengerModule extends SlotModule {
     private long referenceId = 1L;
     private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
     private Runnable pokeGoX2Task = new X2Task();

     public AvengerModule() {
          this.gameName = Games.AVENGERS.getName();
     }

     public void init() {
          super.init();
          long[] funds = new long[3];
          int[] initPotValues = new int[6];

          int nextX2Time;
          try {
               String initPotValuesStr = ConfigGame.getValueString(this.gameName + "_init_pot_values");
               String[] arr = initPotValuesStr.split(",");

               for(nextX2Time = 0; nextX2Time < arr.length; ++nextX2Time) {
                    initPotValues[nextX2Time] = Integer.parseInt(arr[nextX2Time]);
               }

               this.pots = this.service.getPots(this.gameName);
               Debug.trace(this.gameName + " POTS: " + CommonUtils.arrayLongToString(this.pots));
               funds = this.service.getFunds(this.gameName);
               Debug.trace(this.gameName + ": " + CommonUtils.arrayLongToString(funds));
          } catch (Exception var7) {
               Debug.trace(new Object[]{"Init " + this.gameName + " error ", var7});
          }

          this.rooms.put(this.gameName + "_vin_100", new AvengerRoom(this, (byte)0, this.gameName + "_vin_100", (short)1, this.pots[0], funds[0], 100, (long)initPotValues[0]));
          this.rooms.put(this.gameName + "_vin_1000", new AvengerRoom(this, (byte)1, this.gameName + "_vin_1000", (short)1, this.pots[1], funds[1], 1000, (long)initPotValues[1]));
          this.rooms.put(this.gameName + "_vin_10000", new AvengerRoom(this, (byte)2, this.gameName + "_vin_10000", (short)1, this.pots[2], funds[2], 10000, (long)initPotValues[2]));
          Debug.trace("INIT " + this.gameName + " DONE");
          this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
          this.referenceId = this.slotService.getLastReferenceId(this.gameName);
          Debug.trace("START " + this.gameName + " REFERENCE ID= " + this.referenceId);
          CacheServiceImpl sv = new CacheServiceImpl();

          try {
               sv.removeKey(this.gameName + "_last_day_x2");
          } catch (KeyNotFoundException var6) {
               Debug.trace("KEY NOT FOUND");
          }

          int lastDayFinish = SlotUtils.getLastDayX2(this.gameName);
          this.ngayX2 = SlotUtils.calculateTimePokeGoX2AsString(this.gameName, SlotUtils.getX2Days(this.gameName), lastDayFinish);
          nextX2Time = SlotUtils.calculateTimePokeGoX2(this.gameName, SlotUtils.getX2Days(this.gameName), lastDayFinish);
          Debug.trace(this.gameName + " Ngay X2: " + this.ngayX2 + ", remain time = " + nextX2Time);
          if (nextX2Time >= 0) {
               BitZeroServer.getInstance().getTaskScheduler().schedule(this.pokeGoX2Task, nextX2Time, TimeUnit.SECONDS);
          } else {
               this.startX2();
          }

          this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
          BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 2, TimeUnit.SECONDS);
     }

     public void handleServerEvent(IBZEvent ibzevent) throws BZException {
          if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
               User user = (User)ibzevent.getParameter(BZEventParam.USER);
               this.userDis(user);
          }

     }

     private void userDis(User user) {
          AvengerRoom room = (AvengerRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.quitRoom(user);
               room.stopAutoPlay(user);
          }

     }

     public void handleClientRequest(User user, DataCmd dataCmd) {
          int cmdId = dataCmd.getId();
          if (cmdId > 4014) { cmdId = 4000 + (cmdId % 1000); }
          switch(cmdId) {
          case 4001:
               this.play(user, dataCmd);
          case 4002:
          case 4007:
          case 4008:
          case 4009:
          case 4010:
          case 4011:
          case 4012:
          default:
               break;
          case 4003:
               this.subScribe(user, dataCmd);
               break;
          case 4004:
               this.unSubScribe(user, dataCmd);
               break;
          case 4005:
               this.changeRoom(user, dataCmd);
               break;
          case 4006:
               this.autoPlay(user, dataCmd);
               break;
          case 4013:
               this.minimize(user, dataCmd);
               break;
          case 4014:
               this.speedUp(user, dataCmd);
               break;
          }

     }

     public long getNewReferenceId() {
          return ++this.referenceId;
     }

     protected void subScribe(User user, DataCmd dataCmd) {
          SubscribeAvengersCmd cmd = new SubscribeAvengersCmd(dataCmd);
          AvengerRoom room = (AvengerRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.joinRoom(user);
               room.userMaximize(user);
               room.updatePot(user);
               this.updateAvengerInfo(user, room);
          } else {
               Debug.trace(this.gameName + " SUBSCRIBE: room " + cmd.roomId + " not found");
          }

     }

     private void updateAvengerInfo(User user, AvengerRoom room) {
          AvengersInfoMsg msg = new AvengersInfoMsg();
          msg.ngayX2 = this.ngayX2;
          msg.remain = 0;
          msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
          SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.gameName + room.getBetValue(), user.getName());
          if (freeSpin != null && freeSpin.getLines() != null) {
               msg.freeSpin = (byte)freeSpin.getNum();
               msg.lines = freeSpin.getLines();
          }

          this.send(msg, user);
     }

     protected void unSubScribe(User user, DataCmd dataCmd) {
          UnSubscribeAvengersCmd cmd = new UnSubscribeAvengersCmd(dataCmd);
          AvengerRoom room = (AvengerRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.stopAutoPlay(user);
               room.quitRoom(user);
          } else {
               Debug.trace(this.gameName + " UNSUBSCRIBE: room " + cmd.roomId + " not found");
          }

     }

     protected void minimize(User user, DataCmd dataCmd) {
          MinimizeKhoBauCmd cmd = new MinimizeKhoBauCmd(dataCmd);
          AvengerRoom room = (AvengerRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.quitRoom(user);
               room.userMinimize(user);
          } else {
               Debug.trace(this.gameName + " MINIMIZE: room " + cmd.roomId + " not found");
          }

     }

     protected void changeRoom(User user, DataCmd dataCmd) {
          ChangeRoomAvengersCmd cmd = new ChangeRoomAvengersCmd(dataCmd);
          AvengerRoom roomLeaved = (AvengerRoom)this.getRoom(cmd.roomLeavedId);
          AvengerRoom roomJoined = (AvengerRoom)this.getRoom(cmd.roomJoinedId);
          if (roomLeaved != null && roomJoined != null) {
               roomLeaved.stopAutoPlay(user);
               roomLeaved.quitRoom(user);
               roomJoined.joinRoom(user);
               roomJoined.updatePot(user);
               this.updateAvengerInfo(user, roomJoined);
          } else {
               Debug.trace(this.gameName + ": change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId);
          }

     }

     private void play(User user, DataCmd dataCmd) {
          PlayAvengersCmd cmd = new PlayAvengersCmd(dataCmd);
          AvengerRoom room = (AvengerRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.play(user, cmd.lines);
          }

     }

     private void autoPlay(User user, DataCmd dataCMD) {
          AutoPlayAvengersCmd cmd = new AutoPlayAvengersCmd(dataCMD);
          AvengerRoom room = (AvengerRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               if (cmd.autoPlay == 1) {
                    short result = room.play(user, cmd.lines);
                    if (result != 3 && result != 4 && result != 101 && result != 102 && result != 100) {
                         room.autoPlay(user, cmd.lines, result);
                    } else {
                         room.forceStopAutoPlay(user);
                    }
               } else {
                    room.stopAutoPlay(user);
               }
          }

     }

     private void speedUp(User user, DataCmd dataCMD) {
          AutoPlayAvengersCmd cmd = new AutoPlayAvengersCmd(dataCMD);
          AvengerRoom room = (AvengerRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.speedUp(user, cmd.autoPlay);
          }
     }

     protected String getRoomName(short moneyType, long baseBetting) {
          String moneyTypeStr = "xu";
          if (moneyType == 1) {
               moneyTypeStr = "vin";
          }

          return this.gameName + "_" + moneyTypeStr + "_" + baseBetting;
     }

     protected void gameLoop() {
          ++this.countBot100;
          List bots;
          Iterator var2;
          String bot;
          AvengerRoom room;
          if (this.countBot100 >= this.getCountTimeBot(this.gameName + "_bot_100")) {
               if (this.countBot100 == this.getCountTimeBot(this.gameName + "_bot_100")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_100"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (AvengerRoom)this.rooms.get(this.gameName + "_vin_100");
                              room.play(bot, this.fullLines, true);
                         }
                    }
               }

               this.countBot100 = 0;
          }

          ++this.countBot1000;
          if (this.countBot1000 >= this.getCountTimeBot(this.gameName + "_bot_1000")) {
               if (this.countBot1000 == this.getCountTimeBot(this.gameName + "_bot_1000")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_1000"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (AvengerRoom)this.rooms.get(this.gameName + "_vin_1000");
                              room.play(bot, this.fullLines, true);
                         }
                    }
               }

               this.countBot1000 = 0;
          }

          ++this.countBot10000;
          if (this.countBot10000 >= this.getCountTimeBot(this.gameName + "_bot_10000")) {
               if (this.countBot10000 == this.getCountTimeBot(this.gameName + "_bot_10000")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_10000"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (AvengerRoom)this.rooms.get(this.gameName + "_vin_10000");
                              room.play(bot, this.fullLines, true);
                         }
                    }
               }

               this.countBot10000 = 0;
          }

     }
}
