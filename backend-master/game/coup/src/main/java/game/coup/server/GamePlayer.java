package game.coup.server;

import bitzero.server.entities.User;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import org.json.JSONObject;

public class GamePlayer {
   public static final int psNO_LOGIN = 0;
   public static final int psVIEW = 1;
   public static final int psSIT = 2;
   public static final int psPLAY = 3;
   public static final int THANG_SAM = 1;
   public static final int THANG_TRANG = 2;
   public static final int THANG_THUONG = 3;
   public int chair;
   public int gameChair = -1;
   public long timeJoinRoom = 0L;
   public int countToOutRoom = 0;
   public boolean reqQuitRoom = false;
   public boolean regToPlay = false;
   public boolean regToView = false;
   public boolean choiTiepVanSau = true;
   public User user = null;
   public PlayerInfo pInfo = null;
   public GameMoneyInfo gameMoneyInfo = null;
   public sPlayerInfo spInfo = new sPlayerInfo();
   public CoupGameServer gameServer;
   public boolean autoBuyIn = true;
   public volatile long lastTimeBuyIn = 0L;
   public int cauHoa = 0;
   public int thachDau = 0;
   public boolean dangCauHoa = false;
   public boolean dangThachDau = false;
   public volatile int playerStatus = 0;

   public void prepareNewGame() {
      if (this.gameChair >= 0) {
         this.playerStatus = 2;
      }

      this.cauHoa = 0;
      this.dangCauHoa = false;
      this.thachDau = 0;
      this.dangThachDau = false;
   }

   public User getUser() {
      return this.user;
   }

   public PlayerInfo getPlayerInfo() {
      return this.pInfo;
   }

   public void takeChair(User user, PlayerInfo pInfo, GameMoneyInfo moneyInfo) {
      this.user = user;
      this.pInfo = pInfo;
      this.gameMoneyInfo = moneyInfo;
      this.reqQuitRoom = false;
      user.setProperty("user_chair", this.chair);
   }

   public boolean isPlaying() {
      return this.playerStatus == 3;
   }

   public boolean canPlayNextGame() {
      return !this.reqQuitRoom && this.checkMoneyCanPlay() && this.regToPlay;
   }

   public boolean hasUser() {
      return this.playerStatus != 0;
   }

   public boolean checkMoneyCanPlay() {
      return this.gameMoneyInfo.moneyCheck();
   }

   public String toString() {
      try {
         JSONObject json = this.toJSONObject();
         return json != null ? json.toString() : "{}";
      } catch (Exception var2) {
         return "{}";
      }
   }

   public JSONObject toJSONObject() {
      try {
         JSONObject json = new JSONObject();
         json.put("reqQuitRoom", this.reqQuitRoom);
         json.put("playerStatus", this.playerStatus);
         if (this.gameMoneyInfo != null) {
            json.put("gameMoneyInfo", this.gameMoneyInfo.toJSONObject());
         }

         return json;
      } catch (Exception var2) {
         return null;
      }
   }

   public boolean updatePlayingTime() {
      --this.spInfo.turnTime;
      --this.spInfo.gameTime;
      return this.spInfo.turnTime > 0 && this.spInfo.gameTime > 0;
   }

   public boolean checkMoreMoneyThan(long money) {
      if (this.gameMoneyInfo != null) {
         return this.gameMoneyInfo.getCurrentMoneyFromCache() >= money;
      } else {
         return false;
      }
   }

   public void outChair() {
      this.regToView = false;
      this.choiTiepVanSau = true;
      this.countToOutRoom = 0;
      this.gameChair = -1;
      this.cauHoa = 0;
      this.dangCauHoa = false;
      this.thachDau = 0;
      this.dangThachDau = false;
   }
}
