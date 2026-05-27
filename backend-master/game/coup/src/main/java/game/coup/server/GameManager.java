package game.coup.server;

import game.coup.server.cmd.send.SendUpdateAutoStart;
import game.coup.server.logic.Gamble;

public class GameManager {
   public static final int GS_NO_START = 0;
   public static final int GS_GAME_PLAYING = 1;
   public static final int GS_GAME_END = 3;
   public static final int NO_ACTION = -1;
   public static final int IN_TURN = 0;
   public static final int CHANGE_TURN = 1;
   public int roomOwnerChair = 20;
   public int roomCreatorUserId;
   public int rCuocLon = 1;
   public volatile int gameState = 0;
   public volatile int gameAction = -1;
   public volatile int countDown = 0;
   public volatile boolean isAutoStart = false;
   public volatile int currentChair = -1;
   public volatile int lastWinChair = -1;
   public Gamble game = new Gamble();
   public CoupGameServer gameServer;

   public int getGameState() {
      return this.gameState;
   }

   public void prepareNewGame() {
      this.game.reset();
      this.isAutoStart = false;
      this.gameServer.kiemTraTuDongBatDau(5);
   }

   public void gameLoop() {
      if (this.gameState == 0 && this.isAutoStart) {
         --this.countDown;
         if (this.countDown <= 0) {
            this.gameState = 1;
            this.gameServer.start();
         }
      } else if (this.gameState == 0 && !this.isAutoStart) {
         if (this.gameServer.playerCount > 1) {
            this.gameServer.kiemTraTuDongBatDau(5);
         }
      } else if (this.gameState == 1) {
         --this.countDown;
         if (this.countDown <= 0) {
            if (this.gameAction == 1) {
               this.changeTurn();
            } else if (this.gameAction == 0) {
               this.tudongChoi();
            }
         }

         this.updatePlayingTime();
      } else if (this.gameState == 3) {
         --this.countDown;
         if (this.countDown <= 0) {
            this.gameServer.pPrepareNewGame();
         }
      }

   }

   public void updatePlayingTime() {
      this.gameServer.updatePlayingTime();
   }

   public void notifyAutoStartToUsers(int after) {
      SendUpdateAutoStart msg = new SendUpdateAutoStart();
      msg.isAutoStart = this.isAutoStart;
      msg.autoStartTime = (byte)after;
      this.gameServer.send(msg);
   }

   public void cancelAutoStart() {
      if (this.isAutoStart) {
         this.isAutoStart = false;
         this.notifyAutoStartToUsers(0);
      }

   }

   public void makeAutoStart(int after) {
      if (this.gameState == 0) {
         if (!this.isAutoStart) {
            this.countDown = after;
         } else if (after < this.countDown) {
            this.countDown = after;
         } else {
            after = this.countDown;
         }

         this.isAutoStart = true;
         this.notifyAutoStartToUsers(after);
      }
   }

   private void tudongChoi() {
      this.gameServer.tudongChoi();
   }

   private void changeTurn() {
      this.gameServer.changeTurn();
   }

   public int currentChair() {
      return this.currentChair;
   }

   public boolean canOutRoom() {
      return this.getGameState() == 0;
   }
}
