package game.modules.slot;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.utils.CommonUtils;
import game.modules.slot.cmd.rev.khobau.AutoPlayKhoBauCmd;
import game.modules.slot.cmd.rev.ndv.SubscribeNDVCmd;
import game.modules.slot.cmd.rev.vqv.AutoPlayVQVCmd;
import game.modules.slot.cmd.rev.vqv.ChangeRoomVQVCmd;
import game.modules.slot.cmd.rev.vqv.MinimizeVQVCmd;
import game.modules.slot.cmd.rev.vqv.PlayVQVCmd;
import game.modules.slot.cmd.rev.vqv.UnSubscribeVQVCmd;
import game.modules.slot.cmd.send.vqv.UpdatePotVQVMsg;
import game.modules.slot.cmd.send.vqv.VQVInfoMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.KhoBauRoom;
import game.modules.slot.room.VQVRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VQVModule extends SlotModule {
     private static long referenceId = 1L;
     private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
     private Runnable x2Task = new SlotModule.X2Task();
     private byte[] x2Arr = new byte[3];
     private List users = new ArrayList();
     private int countUpdateJackpot = 0;

     public VQVModule() {
          this.gameName = Games.VUONG_QUOC_VIN.getName();
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
               Debug.trace(this.gameName + " FUNDS: " + CommonUtils.arrayLongToString(funds));
          } catch (Exception var7) {
               Debug.trace(new Object[]{"Init " + this.gameName + " error ", var7});
          }

          this.rooms.put(this.gameName + "_vin_100", new VQVRoom(this, (byte)0, this.gameName + "_vin_100", (short)1, this.pots[0], funds[0], 100, (long)initPotValues[0]));
          this.rooms.put(this.gameName + "_vin_1000", new VQVRoom(this, (byte)1, this.gameName + "_vin_1000", (short)1, this.pots[1], funds[1], 1000, (long)initPotValues[1]));
          this.rooms.put(this.gameName + "_vin_10000", new VQVRoom(this, (byte)2, this.gameName + "_vin_10000", (short)1, this.pots[2], funds[2], 10000, (long)initPotValues[2]));
          Debug.trace("INIT " + this.gameName + " DONE");
          this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
          referenceId = this.slotService.getLastReferenceId(this.gameName);
          Debug.trace("START " + this.gameName + " REFERENCE ID= " + referenceId);
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
               BitZeroServer.getInstance().getTaskScheduler().schedule(this.x2Task, nextX2Time, TimeUnit.SECONDS);
          } else {
               this.startX2();
          }

          BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 2, TimeUnit.SECONDS);
     }

     public long getNewReferenceId() {
          return ++referenceId;
     }

     public void updatePot(byte id, long value, byte x2) {
          this.pots[id] = value;
          this.x2Arr[id] = x2;
          long currentTime = System.currentTimeMillis();
          if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
               UpdatePotVQVMsg msg = this.getPotsInfo();
               this.lastTimeUpdatePotToRoom = System.currentTimeMillis();
               SlotModule.SendMsgToAlLUsersThread t = new SlotModule.SendMsgToAlLUsersThread(msg);
               t.start();
          }

     }

     public UpdatePotVQVMsg getPotsInfo() {
          UpdatePotVQVMsg msg = new UpdatePotVQVMsg();
          msg.value100 = this.pots[0];
          msg.value1000 = this.pots[1];
          msg.value10000 = this.pots[2];
          msg.x2Room100 = this.x2Arr[0];
          msg.x2Room1000 = this.x2Arr[1];
          return msg;
     }

     public void handleServerEvent(IBZEvent ibzevent) throws BZException {
          if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
               User user = (User)ibzevent.getParameter(BZEventParam.USER);
               this.userDis(user);
          }

     }

     private void userDis(User user) {
          VQVRoom room = (VQVRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.quitRoom(user);
               room.stopAutoPlay(user);
          }

     }

     public void handleClientRequest(User user, DataCmd dataCmd) {
          switch(dataCmd.getId()) {
          case 5001:
               this.playVQV(user, dataCmd);
          case 5002:
          case 5007:
          case 5008:
          case 5009:
          case 5010:
          case 5011:
          case 5012:
          default:
               break;
          case 5003:
               this.subScribe(user, dataCmd);
               break;
          case 5004:
               this.unSubScribe(user, dataCmd);
               break;
          case 5005:
               this.changeRoom(user, dataCmd);
               break;
          case 5006:
               this.autoPlay(user, dataCmd);
               break;
          case 5013:
               this.minimize(user, dataCmd);
          break;
          case 5014:
               this.speedUp(user, dataCmd);
               break;
          }

     }

     public void updatePotToUser(User user) {
          UpdatePotVQVMsg msg = this.getPotsInfo();
          SlotUtils.sendMessageToUser(msg, (User)user);
     }

     public void updateUserInfo(User user, VQVRoom room) {
          VQVInfoMsg msg = new VQVInfoMsg();
          msg.ngayX2 = this.ngayX2;
          msg.remain = 0;
          msg.currentRoom = room.getId();
          SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.gameName + room.getBetValue(), user.getName());
          if (freeSpin != null && freeSpin.getLines() != null) {
               msg.freeSpin = (byte)freeSpin.getNum();
               msg.lines = freeSpin.getLines();
          }

          msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
          this.send(msg, user);
     }

     protected void subScribe(User user, DataCmd dataCmd) {
          SubscribeNDVCmd cmd = new SubscribeNDVCmd(dataCmd);
          if (cmd.roomId == -1) {
               this.updatePotToUser(user);
               synchronized(this.users) {
                    this.users.add(user);
               }
          } else {
               synchronized(this.users) {
                    this.users.remove(user);
               }

               VQVRoom room = (VQVRoom)this.getRoom(cmd.roomId);
               if (room != null) {
                    room.joinRoom(user);
                    room.userMaximize(user);
                    this.updatePotToUser(user);
                    this.updateUserInfo(user, room);
               } else {
                    Debug.trace(this.gameName + " SUBSCRIBE: room " + cmd.roomId + " not found");
               }

          }
     }

     protected void unSubScribe(User user, DataCmd dataCmd) {
          UnSubscribeVQVCmd cmd = new UnSubscribeVQVCmd(dataCmd);
          if (cmd.roomId == -1) {
               this.updatePotToUser(user);
               synchronized(this.users) {
                    this.users.remove(user);
               }
          } else {
               VQVRoom room = (VQVRoom)this.getRoom(cmd.roomId);
               if (room != null) {
                    room.stopAutoPlay(user);
                    room.quitRoom(user);
               } else {
                    Debug.trace(this.gameName + " UNSUBSCRIBE: room " + cmd.roomId + " not found");
               }

          }
     }

     protected void minimize(User user, DataCmd dataCmd) {
          MinimizeVQVCmd cmd = new MinimizeVQVCmd(dataCmd);
          VQVRoom room = (VQVRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.quitRoom(user);
               room.userMinimize(user);
          } else {
               Debug.trace(this.gameName + " MINIMIZE: room " + cmd.roomId + " not found");
          }

     }

     protected void changeRoom(User user, DataCmd dataCmd) {
          ChangeRoomVQVCmd cmd = new ChangeRoomVQVCmd(dataCmd);
          VQVRoom roomLeaved = (VQVRoom)this.getRoom(cmd.roomLeavedId);
          VQVRoom roomJoined = (VQVRoom)this.getRoom(cmd.roomJoinedId);
          if (roomLeaved != null && roomJoined != null) {
               roomLeaved.stopAutoPlay(user);
               roomLeaved.quitRoom(user);
               roomJoined.joinRoom(user);
               this.updatePotToUser(user);
               this.updateUserInfo(user, roomJoined);
          } else {
               Debug.trace(this.gameName + ": change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId);
          }

     }

     private void playVQV(User user, DataCmd dataCmd) {
          PlayVQVCmd cmd = new PlayVQVCmd(dataCmd);
          VQVRoom room = (VQVRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.play(user, cmd.lines);
          }

     }

     private void speedUp(User user, DataCmd dataCMD) {
          AutoPlayVQVCmd cmd = new AutoPlayVQVCmd(dataCMD);
          VQVRoom room = (VQVRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
          if (room != null) {
               room.speedUp(user, cmd.autoPlay);
          }
     }

     private void autoPlay(User user, DataCmd dataCMD) {
          AutoPlayVQVCmd cmd = new AutoPlayVQVCmd(dataCMD);
          VQVRoom room = (VQVRoom)user.getProperty("MGROOM_" + this.gameName + "_INFO");
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

     protected String getRoomName(short moneyType, long baseBetting) {
          String moneyTypeStr = "xu";
          if (moneyType == 1) {
               moneyTypeStr = "vin";
          }

          return this.gameName + "_" + moneyTypeStr + "_" + baseBetting;
     }

     public void sendMessageToRoomLobby(BaseMsg msg) {
          List usersCopy = new ArrayList(this.users);
          Iterator var3 = usersCopy.iterator();

          while(var3.hasNext()) {
               User user = (User)var3.next();
               ExtensionUtility.getExtension().send(msg, user);
          }

     }

     protected void gameLoop() {
          ++this.countBot100;
          List bots;
          Iterator var2;
          String bot;
          VQVRoom room;
          if (this.countBot100 >= this.getCountTimeBot(this.gameName + "_bot_100")) {
               if (this.countBot100 == this.getCountTimeBot(this.gameName + "_bot_100")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue(this.gameName + "_num_bot_100"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (VQVRoom)this.rooms.get(this.gameName + "_vin_100");
                              room.play(bot, this.fullLines);
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
                              room = (VQVRoom)this.rooms.get(this.gameName + "_vin_1000");
                              room.play(bot, this.fullLines);
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
                              room = (VQVRoom)this.rooms.get(this.gameName + "_vin_10000");
                              room.play(bot, this.fullLines);
                         }
                    }
               }

               this.countBot10000 = 0;
          }

          ++this.countUpdateJackpot;
          if (this.countUpdateJackpot >= 3) {
               UpdatePotVQVMsg msg = this.getPotsInfo();
               this.countUpdateJackpot = 0;
               this.sendMessageToRoomLobby(msg);
          }

     }
}
