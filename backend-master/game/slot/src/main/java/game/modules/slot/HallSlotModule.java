package game.modules.slot;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import game.modules.slot.cmd.send.hall.ListAutoPlayInfoMsg;
import game.modules.slot.cmd.send.hall.UpdateJackpotsMsg;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONObject;

public class HallSlotModule extends BaseClientRequestHandler {
     private Set usersSub = new HashSet();
     private Runnable updateJackpotsTask = new UpdateJackpotsTask();

     public HallSlotModule() {
          BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.updateJackpotsTask, 10, 4, TimeUnit.SECONDS);
     }

     public void handleClientRequest(User user, DataCmd dataCmd) {
          switch(dataCmd.getId()) {
          case 10001:
               this.subScribe(user, dataCmd);
               break;
          case 10002:
               this.unSubscribe(user, dataCmd);
          }

     }

     protected void subScribe(User user, DataCmd dataCmd) {
          synchronized(this.usersSub) {
               this.usersSub.add(user);
          }

          UpdateJackpotsMsg msg = new UpdateJackpotsMsg();
          msg.json = this.buildJsonJackpots();
          this.send(msg, user);
          ListAutoPlayInfoMsg listAutoMsg = new ListAutoPlayInfoMsg();
          boolean auto;
          if (user.getProperty("auto_" + Games.KHO_BAU.getName()) != null) {
               auto = (Boolean)user.getProperty("auto_" + Games.KHO_BAU.getName());
               listAutoMsg.autoKhoBau = auto;
          }

          if (user.getProperty("auto_" + Games.NU_DIEP_VIEN.getName()) != null) {
               auto = (Boolean)user.getProperty("auto_" + Games.NU_DIEP_VIEN.getName());
               listAutoMsg.autoNDV = auto;
          }

          if (user.getProperty("auto_" + Games.AVENGERS.getName()) != null) {
               auto = (Boolean)user.getProperty("auto_" + Games.AVENGERS.getName());
               listAutoMsg.autoAvenger = auto;
          }

          if (user.getProperty("auto_" + Games.VUONG_QUOC_VIN.getName()) != null) {
               auto = (Boolean)user.getProperty("auto_" + Games.VUONG_QUOC_VIN.getName());
               listAutoMsg.autoVQV = auto;
          }

          this.send(listAutoMsg, user);
     }

     protected void unSubscribe(User user, DataCmd dataCmd) {
          synchronized(this.usersSub) {
               this.usersSub.remove(user);
          }
     }

     private String buildJsonJackpots() {
          JSONObject json = new JSONObject();
          // Keys must match client Lobby.LobbyController.ts UPDATE_JACKPOT_SLOTS handler
          json.put("spartan", this.buildGameSlotInfo(Games.SPARTAN.getName()));
          json.put("audition", this.buildGameSlotInfo(Games.AUDITION.getName()));
          json.put("chiemtinh", this.buildGameSlotInfo(Games.CHIEMTINH.getName()));
          json.put("maybach", this.buildGameSlotInfo(Games.NU_DIEP_VIEN.getName()));
          json.put("tamhung", this.buildGameSlotInfo(Games.TAMHUNG.getName()));
          json.put("rangeRover", this.buildGameSlotInfo(Games.KHO_BAU.getName()));
          json.put("benley", this.buildGameSlotInfo(Games.AVENGERS.getName()));
          json.put("bikini", this.buildGameSlotInfo(Games.THETHAO.getName()));
          json.put("galaxy", this.buildGameSlotInfo(Games.GALAXY.getName()));
          json.put("minipoker", this.buildGameSlotInfo(Games.MINI_POKER.getName()));
          json.put("rollRoye", this.buildGameSlotInfo(Games.VUONG_QUOC_VIN.getName()));
          json.put("caothap", this.buildGameSlotInfo(Games.CAO_THAP.getName()));
          json.put("pokemon", this.buildGameSlotInfo(Games.NU_DIEP_VIEN.getName()));
          json.put("xocdia", this.buildGameSlotInfo(Games.CANDY.getName()));
          json.put("dragonball", this.buildGameSlotInfo(Games.DRAGONBALL.getName()));
          return json.toJSONString();
     }

     private JSONObject buildGameSlotInfo(String gameName) {
          JSONObject jsonGame = new JSONObject();
          // Include ALL bet levels that any client game might access
          int[] levels = {100, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 5000000};
          for (int level : levels) {
               jsonGame.put(String.valueOf(level), this.buildRoomSlotInfo(gameName, level));
          }
          return jsonGame;
     }

     private JSONObject buildRoomSlotInfo(String gameName, int room) {
          CacheService cacheService = new CacheServiceImpl();
          JSONObject jsonValue = new JSONObject();

          try {
               int pot = cacheService.getValueInt(gameName + "_vin_" + room);
               jsonValue.put("p", pot);
               int x2 = cacheService.getValueInt(gameName + "_vin_" + room + "_x2");
               jsonValue.put("x2", x2);
          } catch (Exception var7) {
               // Return defaults so client doesn't crash on missing data
               jsonValue.put("p", 0);
               jsonValue.put("x2", 0);
          }

          return jsonValue;
     }

     static void access$2(HallSlotModule hallSlotModule, BaseMsg baseMsg, User user) {
          hallSlotModule.send(baseMsg, user);
     }

     private class UpdateJackpotsTask implements Runnable {
          private UpdateJackpotsTask() {
          }

          public void run() {
               try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                    String result = HallSlotModule.this.buildJsonJackpots();
                    UpdateJackpotsMsg msg = new UpdateJackpotsMsg();
                    msg.json = result;
                    synchronized(HallSlotModule.this.usersSub) {
                         Iterator var4 = HallSlotModule.this.usersSub.iterator();

                         while(var4.hasNext()) {
                              User user = (User)var4.next();
                              if (user != null) {
                                   HallSlotModule.access$2(HallSlotModule.this, msg, user);
                              }
                         }
                    }
               } catch (Throwable var8) {
                    Debug.trace("Update slot exception: " + var8.getMessage());
               }

          }

          // $FF: synthetic method
          UpdateJackpotsTask(Object x1) {
               this();
          }
     }
}
