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
import game.modules.slot.cmd.rev.thanbai.AutoPlayThanBaiCmd;
import game.modules.slot.cmd.rev.thanbai.ChangeRoomThanBaiCmd;
import game.modules.slot.cmd.rev.thanbai.PlayThanBaiCmd;
import game.modules.slot.cmd.rev.thanbai.SubscribeThanBaiCmd;
import game.modules.slot.cmd.rev.thanbai.UnSubscribeThanBaiCmd;
import game.modules.slot.cmd.rev.khobau.MinimizeKhoBauCmd;
import game.modules.slot.cmd.send.thanbai.ThanBaiInfoMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.ThanBaiRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ThanBaiModule extends SlotModule {
     private long referenceId = 1L;
     private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
     private Runnable pokeGoX2Task = new X2Task();

     public ThanBaiModule() {
          this.gameName = Games.THANBAI.getName();
     }

     public void init() {
          super.init();
          long[] funds = new long[3];
          int[] initPotValues = new int[6];

          int nextX2Time;
          int[] defaultPots = {500000, 5000000, 50000000};
          try {
               String initPotValuesStr = ConfigGame.getValueString(this.gameName + "_init_pot_values");
               if (initPotValuesStr != null && !initPotValuesStr.isEmpty()) {
                    String[] arr = initPotValuesStr.split(",");
                    for(nextX2Time = 0; nextX2Time < arr.length && nextX2Time < initPotValues.length; ++nextX2Time) {
                         initPotValues[nextX2Time] = Integer.parseInt(arr[nextX2Time]);
                    }
               } else {
                    initPotValues[0] = defaultPots[0]; initPotValues[1] = defaultPots[1]; initPotValues[2] = defaultPots[2];
               }

               this.pots = this.service.getPots(this.gameName);
               if (this.pots == null || this.pots.length < 3) {
                    this.pots = new long[]{0, 0, 0};
               }
               Debug.trace(this.gameName + " POTS: " + CommonUtils.arrayLongToString(this.pots));
               funds = this.service.getFunds(this.gameName);
               if (funds == null || funds.length < 3) {
                    funds = new long[]{0, 0, 0};
               }
               Debug.trace(this.gameName + ": " + CommonUtils.arrayLongToString(funds));
          } catch (Exception var7) {
               Debug.trace(new Object[]{"Init " + this.gameName + " error ", var7});
               initPotValues[0] = defaultPots[0]; initPotValues[1] = defaultPots[1]; initPotValues[2] = defaultPots[2];
          }

          this.rooms.put(this.gameName + "_vin_100", new ThanBaiRoom(this, (byte)0, this.gameName + "_vin_100", (short)1, this.pots[0], funds[0], 100, (long)initPotValues[0]));
          this.rooms.put(this.gameName + "_vin_1000", new ThanBaiRoom(this, (byte)1, this.gameName + "_vin_1000", (short)1, this.pots[1], funds[1], 1000, (long)initPotValues[1]));
          this.rooms.put(this.gameName + "_vin_10000", new ThanBaiRoom(this, (byte)2, this.gameName + "_vin_10000", (short)1, this.pots[2], funds[2], 10000, (long)initPotValues[2]));
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

          this.ngayX2 = ""; // X2 feature disabled
          nextX2Time = -1;
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
          ThanBaiRoom room = (ThanBaiRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.quitRoom(user);
               room.stopAutoPlay(user);
          }

     }

     public void handleClientRequest(User user, DataCmd dataCmd) {
          switch(dataCmd.getId()) {
          case 17001:
               this.play(user, dataCmd);
          case 17002:
          case 17007:
          case 17008:
          case 17009:
          case 17010:
          case 17011:
          case 17012:
          default:
               break;
          case 17003:
               this.subScribe(user, dataCmd);
               break;
          case 17004:
               this.unSubScribe(user, dataCmd);
               break;
          case 17005:
               this.changeRoom(user, dataCmd);
               break;
          case 17006:
               this.autoPlay(user, dataCmd);
               break;
          case 17013:
               this.minimize(user, dataCmd);
               break;
          case 17014:
               this.speedUp(user, dataCmd);
               break;
          }

     }

     public long getNewReferenceId() {
          return ++this.referenceId;
     }

     protected void subScribe(User user, DataCmd dataCmd) {
          SubscribeThanBaiCmd cmd = new SubscribeThanBaiCmd(dataCmd);
          ThanBaiRoom room = (ThanBaiRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.joinRoom(user);
               room.userMaximize(user);
               room.updatePot(user);
               this.updateThanBaiInfo(user, room);
          } else {
               Debug.trace(this.gameName + " SUBSCRIBE: room " + cmd.roomId + " not found");
          }

     }

     private void updateThanBaiInfo(User user, ThanBaiRoom room) {
          ThanBaiInfoMsg msg = new ThanBaiInfoMsg();
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
          UnSubscribeThanBaiCmd cmd = new UnSubscribeThanBaiCmd(dataCmd);
          ThanBaiRoom room = (ThanBaiRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.stopAutoPlay(user);
               room.quitRoom(user);
          } else {
               Debug.trace(this.gameName + " UNSUBSCRIBE: room " + cmd.roomId + " not found");
          }

     }

     protected void minimize(User user, DataCmd dataCmd) {
          MinimizeKhoBauCmd cmd = new MinimizeKhoBauCmd(dataCmd);
          ThanBaiRoom room = (ThanBaiRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.quitRoom(user);
               room.userMinimize(user);
          } else {
               Debug.trace(this.gameName + " MINIMIZE: room " + cmd.roomId + " not found");
          }

     }

     protected void changeRoom(User user, DataCmd dataCmd) {
          ChangeRoomThanBaiCmd cmd = new ChangeRoomThanBaiCmd(dataCmd);
          ThanBaiRoom roomLeaved = (ThanBaiRoom)this.getRoom(cmd.roomLeavedId);
          ThanBaiRoom roomJoined = (ThanBaiRoom)this.getRoom(cmd.roomJoinedId);
          if (roomLeaved != null && roomJoined != null) {
               roomLeaved.stopAutoPlay(user);
               roomLeaved.quitRoom(user);
               roomJoined.joinRoom(user);
               roomJoined.updatePot(user);
               this.updateThanBaiInfo(user, roomJoined);
          } else {
               Debug.trace(this.gameName + ": change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId);
          }

     }

     private void play(User user, DataCmd dataCmd) {
          PlayThanBaiCmd cmd = new PlayThanBaiCmd(dataCmd);
          ThanBaiRoom room = (ThanBaiRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.play(user, cmd.lines);
          }

     }

     private void autoPlay(User user, DataCmd dataCMD) {
          AutoPlayThanBaiCmd cmd = new AutoPlayThanBaiCmd(dataCMD);
          ThanBaiRoom room = (ThanBaiRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
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
          AutoPlayThanBaiCmd cmd = new AutoPlayThanBaiCmd(dataCMD);
          ThanBaiRoom room = (ThanBaiRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
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
          ThanBaiRoom room;
          if (this.countBot100 >= this.getCountTimeBot(this.gameName + "_bot_100")) {
               if (this.countBot100 == this.getCountTimeBot(this.gameName + "_bot_100")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_100"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (ThanBaiRoom)this.rooms.get(this.gameName + "_vin_100");
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
                              room = (ThanBaiRoom)this.rooms.get(this.gameName + "_vin_1000");
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
                              room = (ThanBaiRoom)this.rooms.get(this.gameName + "_vin_10000");
                              room.play(bot, this.fullLines, true);
                         }
                    }
               }

               this.countBot10000 = 0;
          }

     }
}
