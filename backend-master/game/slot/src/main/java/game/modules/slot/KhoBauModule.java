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
import com.vinplay.vbee.common.utils.CommonUtils;
import game.modules.slot.cmd.rev.khobau.AutoPlayKhoBauCmd;
import game.modules.slot.cmd.rev.khobau.ChangeRoomKhoBauCmd;
import game.modules.slot.cmd.rev.khobau.MinimizeKhoBauCmd;
import game.modules.slot.cmd.rev.khobau.PlayKhoBauCmd;
import game.modules.slot.cmd.rev.khobau.SubscribePokeGoCmd;
import game.modules.slot.cmd.rev.khobau.UnSubscribePokeGoCmd;
import game.modules.slot.cmd.send.khobau.KhoBauInfoMsg;
import game.modules.slot.cmd.send.khobau.UpdatePotKhoBauMsg;
import game.modules.slot.entities.BotMinigame;
import game.modules.slot.room.KhoBauRoom;
import game.modules.slot.utils.SlotUtils;
import game.util.ConfigGame;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KhoBauModule extends SlotModule {
     private static long referenceId = 1L;
     private String fullLines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
     private Runnable x2Task = new X2Task();
     private byte[] x2Arr = new byte[3];

     public KhoBauModule() {
          this.gameName = Games.KHO_BAU.getName();
     }

     public void init() {
          super.init();
          long[] funds = new long[3];
          int[] initPotValues = new int[6];

          int nextX2Time;
          try {
               String initPotValuesStr = ConfigGame.getValueString("KhoBau_init_pot_values");
               String[] arr = initPotValuesStr.split(",");

               for(nextX2Time = 0; nextX2Time < arr.length; ++nextX2Time) {
                    initPotValues[nextX2Time] = Integer.parseInt(arr[nextX2Time]);
               }

               this.pots = this.service.getPots(Games.KHO_BAU.getName());
               Debug.trace("KHO_BAU POTS: " + CommonUtils.arrayLongToString(this.pots));
               funds = this.service.getFunds(Games.KHO_BAU.getName());
               Debug.trace("KHO_BAU FUNDS: " + CommonUtils.arrayLongToString(funds));
          } catch (Exception var7) {
               Debug.trace(new Object[]{"Init POKE GO error ", var7});
          }

          this.rooms.put(this.gameName + "_vin_100", new KhoBauRoom(this, (byte)0, this.gameName + "_vin_100", (short)1, this.pots[0], funds[0], 100, (long)initPotValues[0]));
          this.rooms.put(this.gameName + "_vin_1000", new KhoBauRoom(this, (byte)1, this.gameName + "_vin_1000", (short)1, this.pots[1], funds[1], 1000, (long)initPotValues[1]));
          this.rooms.put(this.gameName + "_vin_10000", new KhoBauRoom(this, (byte)2, this.gameName + "_vin_10000", (short)1, this.pots[2], funds[2], 10000, (long)initPotValues[2]));
          Debug.trace("INIT KHO BAU DONE");
          this.getParentExtension().addEventListener(BZEventType.USER_DISCONNECT, this);
          referenceId = this.slotService.getLastReferenceId(this.gameName);
          Debug.trace("START KHO_BAU REFERENCE ID= " + referenceId);
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
               UpdatePotKhoBauMsg msg = this.getPotsInfo();
               this.lastTimeUpdatePotToRoom = System.currentTimeMillis();
               SendMsgToAlLUsersThread t = new SendMsgToAlLUsersThread(msg);
               t.start();
          }

     }

     public UpdatePotKhoBauMsg getPotsInfo() {
          UpdatePotKhoBauMsg msg = new UpdatePotKhoBauMsg();
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
          KhoBauRoom room = (KhoBauRoom)user.getProperty("MGROOM_KHO_BAU_INFO");
          if (room != null) {
               room.quitRoom(user);
               room.stopAutoPlay(user);
          }

     }

     /** Cmd offset for production handlers (e.g. handler 12000: offset=10000) */
     public int cmdOffset = 0;

     public void handleClientRequest(User user, DataCmd dataCmd) {
          int cmdId = dataCmd.getId();
          if (cmdId > 2014) {
               this.cmdOffset = cmdId - (cmdId % 1000) - 2000;
               cmdId = 2000 + (cmdId % 1000);
               // Store offset on user so response messages can apply it
               user.setProperty("SLOT_CMD_OFFSET", this.cmdOffset);
          } else {
               user.setProperty("SLOT_CMD_OFFSET", 0);
          }
          switch(cmdId) {
          case 2001:
               this.playKhoBau(user, dataCmd);
          case 2002:
          case 2007:
          case 2008:
          case 2009:
          case 2010:
          case 2011:
          case 2012:
          default:
               break;
          case 2003:
               this.subScribe(user, dataCmd);
               break;
          case 2004:
               this.unSubScribe(user, dataCmd);
               break;
          case 2005:
               this.changeRoom(user, dataCmd);
               break;
          case 2006:
               this.autoPlay(user, dataCmd);
               break;
          case 2013:
               this.minimize(user, dataCmd);
               break;
          case 2014:
               this.speedUp(user, dataCmd);
               break;
          }

     }

     public void updatePotToUser(User user) {
          UpdatePotKhoBauMsg msg = this.getPotsInfo();
          SlotUtils.sendMessageToUser(msg, (User)user);
     }

     protected void subScribe(User user, DataCmd dataCmd) {
          SubscribePokeGoCmd cmd = new SubscribePokeGoCmd(dataCmd);
          KhoBauRoom room = (KhoBauRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.joinRoom(user);
               room.userMaximize(user);
               this.updatePotToUser(user);
               KhoBauInfoMsg msg = new KhoBauInfoMsg();
               msg.ngayX2 = this.ngayX2;
               msg.remain = 0;
               msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), "vin");
               this.send(msg, user);
          } else {
               Debug.trace("KHO_BAU SUBSCRIBE: room " + cmd.roomId + " not found");
          }

     }

     protected void unSubScribe(User user, DataCmd dataCmd) {
          UnSubscribePokeGoCmd cmd = new UnSubscribePokeGoCmd(dataCmd);
          KhoBauRoom room = (KhoBauRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.stopAutoPlay(user);
               room.quitRoom(user);
          } else {
               Debug.trace("KHO_BAU UNSUBSCRIBE: room " + cmd.roomId + " not found");
          }

     }

     protected void minimize(User user, DataCmd dataCmd) {
          MinimizeKhoBauCmd cmd = new MinimizeKhoBauCmd(dataCmd);
          KhoBauRoom room = (KhoBauRoom)this.getRoom(cmd.roomId);
          if (room != null) {
               room.quitRoom(user);
               room.userMinimize(user);
          } else {
               Debug.trace("KHO_BAU MINIMIZE: room " + cmd.roomId + " not found");
          }

     }

     protected void changeRoom(User user, DataCmd dataCmd) {
          ChangeRoomKhoBauCmd cmd = new ChangeRoomKhoBauCmd(dataCmd);
          KhoBauRoom roomLeaved = (KhoBauRoom)this.getRoom(cmd.roomLeavedId);
          KhoBauRoom roomJoined = (KhoBauRoom)this.getRoom(cmd.roomJoinedId);
          if (roomLeaved != null && roomJoined != null) {
               roomLeaved.stopAutoPlay(user);
               roomLeaved.quitRoom(user);
               roomJoined.joinRoom(user);
               this.updatePotToUser(user);
          } else {
               Debug.trace("KHO_BAU: change room error, leaved= " + cmd.roomLeavedId + ", joined= " + cmd.roomJoinedId);
          }

     }

     private void playKhoBau(User user, DataCmd dataCmd) {
          PlayKhoBauCmd cmd = new PlayKhoBauCmd(dataCmd);
          KhoBauRoom room = (KhoBauRoom)user.getProperty("MGROOM_KHO_BAU_INFO");
          if (room != null) {
               room.play(user, cmd.lines);
          }

     }

     private void autoPlay(User user, DataCmd dataCMD) {
          AutoPlayKhoBauCmd cmd = new AutoPlayKhoBauCmd(dataCMD);
          KhoBauRoom room = (KhoBauRoom)user.getProperty("MGROOM_KHO_BAU_INFO");
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
          AutoPlayKhoBauCmd cmd = new AutoPlayKhoBauCmd(dataCMD);
          KhoBauRoom room = (KhoBauRoom)user.getProperty("MGROOM_KHO_BAU_INFO");
          if (room != null) {
               room.speedUp(user, cmd.autoPlay);
          }
     }

     protected String getRoomName(short moneyType, long baseBetting) {
          String moneyTypeStr = "xu";
          if (moneyType == 1) {
               moneyTypeStr = "vin";
          }

          return Games.KHO_BAU.getName() + "_" + moneyTypeStr + "_" + baseBetting;
     }

     protected void gameLoop() {
          ++this.countBot100;
          List bots;
          Iterator var2;
          String bot;
          KhoBauRoom room;
          if (this.countBot100 >= this.getCountTimeBot("KhoBau_bot_100")) {
               if (this.countBot100 == this.getCountTimeBot("KhoBau_bot_100")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue("KhoBau_num_bot_100"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (KhoBauRoom)this.rooms.get(Games.KHO_BAU.getName() + "_vin_100");
                              room.play(bot, this.fullLines);
                         }
                    }
               }

               this.countBot100 = 0;
          }

          ++this.countBot1000;
          if (this.countBot1000 >= this.getCountTimeBot("KhoBau_bot_1000")) {
               if (this.countBot1000 == this.getCountTimeBot("KhoBau_bot_1000")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue("KhoBau_num_bot_1000"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (KhoBauRoom)this.rooms.get(Games.KHO_BAU.getName() + "_vin_1000");
                              room.play(bot, this.fullLines);
                         }
                    }
               }

               this.countBot1000 = 0;
          }

          ++this.countBot10000;
          if (this.countBot10000 >= this.getCountTimeBot("KhoBau_bot_10000")) {
               if (this.countBot10000 == this.getCountTimeBot("KhoBau_bot_10000")) {
                    bots = BotMinigame.getBots(ConfigGame.getIntValue("KhoBau_num_bot_10000"), "vin");
                    var2 = bots.iterator();

                    while(var2.hasNext()) {
                         bot = (String)var2.next();
                         if (bot != null) {
                              room = (KhoBauRoom)this.rooms.get(Games.KHO_BAU.getName() + "_vin_10000");
                              room.play(bot, this.fullLines);
                         }
                    }
               }

               this.countBot10000 = 0;
          }

     }
}
