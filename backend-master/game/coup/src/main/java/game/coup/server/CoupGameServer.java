package game.coup.server;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEvent;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import game.coup.server.cmd.receive.RevCheatCard;
import game.coup.server.cmd.receive.RevDongYHoa;
import game.coup.server.cmd.receive.RevDongYThachDau;
import game.coup.server.cmd.receive.RevTakeTurn;
import game.coup.server.cmd.receive.RevThachDau;
import game.coup.server.cmd.send.SendCauHoa;
import game.coup.server.cmd.send.SendChangeTurn;
import game.coup.server.cmd.send.SendDangKyChoi;
import game.coup.server.cmd.send.SendDeadPiece;
import game.coup.server.cmd.send.SendEndGame;
import game.coup.server.cmd.send.SendGameInfo;
import game.coup.server.cmd.send.SendJoinRoomSuccess;
import game.coup.server.cmd.send.SendKetQuaCauHoa;
import game.coup.server.cmd.send.SendKetQuaThachDau;
import game.coup.server.cmd.send.SendKetQuaXinThua;
import game.coup.server.cmd.send.SendKickRoom;
import game.coup.server.cmd.send.SendNewUserJoin;
import game.coup.server.cmd.send.SendNotifyReqQuitRoom;
import game.coup.server.cmd.send.SendReconnectInfo;
import game.coup.server.cmd.send.SendStartGame;
import game.coup.server.cmd.send.SendTakeTurn;
import game.coup.server.cmd.send.SendThachDau;
import game.coup.server.cmd.send.SendUpdateMatch;
import game.coup.server.cmd.send.SendUserExitRoom;
import game.coup.server.rule.Board;
import game.coup.server.rule.GameController;
import game.coup.server.rule.GameResult;
import game.coup.server.rule.ai.Move;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.cmd.send.SendNoHu;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ListGameMoneyInfo;
import game.modules.gameRoom.entities.MoneyException;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.utils.GameUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class CoupGameServer extends GameServer {
   public volatile boolean isRegisterLoop = false;
   public ScheduledFuture task;
   public static final int gsNoPlay = 0;
   public static final int gsPlay = 1;
   public static final int gsResult = 2;
   public static final int PHONG_CO_KHOA = 1;
   public static final int PHONG_KHONG_CO_KHOA = 2;
   public static final String USER_CHAIR = "user_chair";
   public final GameManager gameMgr = new GameManager();
   public final Vector playerList = new Vector(20);
   public int playingCount = 0;
   public int winType;
   public volatile int serverState = 0;
   public volatile int groupIndex;
   public volatile int playerCount;
   public volatile int registerPlayerCount = 0;
   StringBuilder gameLog = new StringBuilder();
   public final Runnable gameLoopTask = new CoupGameServer.GameLoopTask();
   public int turnIndex = 0;
   public ThongTinThangLon thongTinNoHu = null;

   public synchronized void onGameMessage(User user, DataCmd dataCmd) {
      switch(dataCmd.getId()) {
      case 3101:
         this.takeTurn(user, dataCmd);
         break;
      case 3102:
         this.cauHoa(user, dataCmd);
      case 3103:
      case 3104:
      case 3107:
      case 3110:
      case 3113:
      case 3114:
      case 3115:
      case 3116:
      default:
         break;
      case 3105:
         this.xinThua(user, dataCmd);
         break;
      case 3106:
         this.thachDau(user, dataCmd);
         break;
      case 3108:
         this.dangKyChoi(user, dataCmd);
         break;
      case 3109:
         this.huyDangKyChoi(user, dataCmd);
         break;
      case 3111:
         this.pOutRoom(user, dataCmd);
         break;
      case 3112:
         this.dongYHoa(user, dataCmd);
         break;
      case 3117:
         this.dongYThachDau(user, dataCmd);
      }

   }

   private void thachDau(User user, DataCmd data) {
      RevThachDau cmd = new RevThachDau(data);
      GamePlayer gp = this.getPlayerByUser(user);
      SendKetQuaThachDau msg;
      if (!gp.dangThachDau && gp != null && !gp.isPlaying() && gp.checkMoreMoneyThan((long)cmd.moneyBet)) {
         ++gp.thachDau;
         if (gp.thachDau <= 10) {
            gp = this.getPlayerByName(cmd.enemy);
            if (gp != null && !gp.isPlaying() && !gp.dangThachDau && gp.checkMoreMoneyThan((long)cmd.moneyBet)) {
               SendThachDau msg1 = new SendThachDau();
               msg1.name = user.getName();
               msg1.moneyBet = cmd.moneyBet;
               this.send(msg1, gp.getUser());
               gp.dangThachDau = true;
            }
         } else {
            msg = new SendKetQuaThachDau();
            msg.Error = 1;
            this.send(msg, user);
         }
      } else {
         msg = new SendKetQuaThachDau();
         msg.Error = 3;
         this.send(msg, user);
      }

   }

   private void dongYThachDau(User user, DataCmd dataCmd) {
      RevDongYThachDau cmd = new RevDongYThachDau(dataCmd);
      GamePlayer gp = this.getPlayerByUser(user);
      GamePlayer gp1 = this.getPlayerByName(cmd.enemy);
      if (gp != null && !gp.isPlaying() && cmd.dongYThachDau && gp1 != null && gp1.dangThachDau && !gp1.isPlaying()) {
         GameRoomSetting setting = new GameRoomSetting(this.room.setting, this.room.setting.moneyType, cmd.moneyBet, 2, this.room.setting.rule);
         User enemy = gp1.getUser();
         if (enemy != null) {
            user.setProperty("GAME_ROOM_SETTING", setting);
            user.setProperty("ENEMY_USER", enemy);
            enemy.setProperty("GAME_ROOM_SETTING", setting);
            enemy.setProperty("ENEMY_USER", user);
         }

         GameRoomManager.instance().leaveRoom(user, this.room);
         GameRoomManager.instance().leaveRoom(enemy, this.room);
      } else if (gp == null || gp1 != null && gp1.dangThachDau) {
         gp1.dangThachDau = false;
         SendKetQuaThachDau msg = new SendKetQuaThachDau();
         msg.Error = 2;
         this.send(msg, gp1.getUser());
      }

   }

   private void cauHoa(User user, DataCmd dataCmd) {
      if (this.gameMgr.gameState == 1) {
         GamePlayer gp = this.getPlayerByUser(user);
         SendKetQuaCauHoa msg;
         if (gp != null && gp.isPlaying() && !gp.dangCauHoa) {
            ++gp.cauHoa;
            if (gp.cauHoa <= 3) {
               int otherChair = (gp.gameChair + 1) % 2;
               GamePlayer gp1 = this.getPlayerByGameChair(otherChair);
               if (gp1.isPlaying() && !gp1.dangCauHoa) {
                  SendCauHoa msg1 = new SendCauHoa();
                  this.send(msg1, gp1.getUser());
                  gp.dangCauHoa = true;
               }
            } else {
               msg = new SendKetQuaCauHoa();
               msg.Error = 1;
               this.send(msg, user);
            }
         } else {
            msg = new SendKetQuaCauHoa();
            msg.Error = 1;
            this.send(msg, user);
         }

      }
   }

   private void dongYHoa(User user, DataCmd dataCmd) {
      GamePlayer gp = this.getPlayerByUser(user);
      int otherChair = (gp.gameChair + 1) % 2;
      GamePlayer gp1 = this.getPlayerByGameChair(otherChair);
      RevDongYHoa cmd = new RevDongYHoa(dataCmd);
      if (cmd.dongYHoa && gp1.dangCauHoa) {
         GameResult res = new GameResult();
         res.result = GameResult.Name.DRAW;
         this.endGame(res);
      } else {
         gp1.dangCauHoa = false;
         SendKetQuaCauHoa msg = new SendKetQuaCauHoa();
         msg.Error = 2;
         this.send(msg, gp1.getUser());
      }

   }

   private synchronized void xinThua(User user, DataCmd dataCmd) {
      if (this.gameMgr.gameState == 1) {
         int size = this.gameMgr.game.board.moveList.size();
         if (size > 20) {
            GamePlayer gp = this.getPlayerByUser(user);
            GameResult res = new GameResult();
            res.result = GameResult.Name.RESIGN;
            if (gp != null && gp.isPlaying()) {
               if (this.gameMgr.currentChair == gp.gameChair) {
                  this.gameMgr.currentChair = (gp.gameChair + 1) % 2;
               }

               this.endGame(res);
            }
         } else {
            SendKetQuaXinThua msg = new SendKetQuaXinThua();
            msg.Error = 1;
            this.send(msg, user);
         }

      }
   }

   public void dangKyChoi(User user, DataCmd dataCmd) {
      SendDangKyChoi msg = new SendDangKyChoi();
      if (this.registerPlayerCount < 2) {
         GamePlayer gp = this.getPlayerByUser(user);
         if (gp != null && !gp.regToPlay) {
            gp.playerStatus = 2;
            GamePlayer gp1 = null;
            if (this.registerPlayerCount == 1) {
               gp1 = this.getPlayerByGameChair(0);
            }

            if (gp1 != null) {
               gp.gameChair = 1;
            } else {
               gp.gameChair = 0;
            }

            ++this.registerPlayerCount;
            gp.regToPlay = true;
            msg.gp = gp;
            this.send(msg);
         }

      }
   }

   public void huyDangKyChoi(User user, DataCmd dataCmd) {
      SendDangKyChoi msg = new SendDangKyChoi();
      msg.action = 2;
      GamePlayer gp = this.getPlayerByUser(user);
      int res = this.huyDangKy(gp);
      if (res == 1) {
         gp.playerStatus = 1;
         msg.gp = gp;
         this.send(msg);
      } else if (res == 2) {
         msg.action = 3;
         msg.gp = gp;
         this.send(msg);
      }

   }

   public int huyDangKy(GamePlayer gp) {
      if (gp != null && gp.regToPlay) {
         if (!gp.isPlaying()) {
            gp.gameChair = -1;
            --this.registerPlayerCount;
            gp.regToPlay = false;
            gp.playerStatus = 1;
            this.kiemTraTuDongBatDau(5);
            return 1;
         } else {
            gp.regToView = true;
            return 2;
         }
      } else {
         return 0;
      }
   }

   public void takeTurn(User user, DataCmd dataCmd) {
      RevTakeTurn cmd = new RevTakeTurn(dataCmd);
      GamePlayer gp = this.getPlayerByUser(user);
      if (this.gameMgr.gameAction == 0 && gp != null && gp.isPlaying() && gp.gameChair == this.gameMgr.currentChair) {
         int[] from = new int[cmd.from.length];
         int[] to = new int[cmd.to.length];

         int i;
         for(i = 0; i < cmd.from.length; ++i) {
            from[i] = cmd.from[i];
         }

         for(i = 0; i < cmd.to.length; ++i) {
            to[i] = cmd.to[i];
         }

         this.takeTurn(gp, from, to);
      }

   }

   public void takeTurn(GamePlayer gp, int[] from, int[] to) {
      GameController controller = this.gameMgr.game.controller;
      Board board = this.gameMgr.game.board;
      if (board.player == gp.spInfo.chessColor) {
         controller.printBoard(board);
         Move move = controller.moveChess(from, to, board);
         controller.printBoard(board);
         this.logMove(move);
         if (move.result.result == GameResult.Name.CONTINUE) {
            this.nextChair();
         }

         SendTakeTurn msg = new SendTakeTurn();
         byte[] f = new byte[move.from.length];
         byte[] t = new byte[move.to.length];

         int i;
         for(i = 0; i < move.from.length; ++i) {
            f[i] = (byte)move.from[i];
         }

         for(i = 0; i < move.to.length; ++i) {
            t[i] = (byte)move.to[i];
         }

         msg.from = f;
         msg.to = t;
         msg.chair = gp.chair;
         if (move.piece != null) {
            if (move.isTrans) {
               msg.key = move.piece.transKey;
               msg.isTrans = move.isTrans;
               msg.newKey = move.piece.key;
            } else {
               msg.key = move.piece.key;
               msg.isTrans = move.isTrans;
               msg.newKey = move.piece.transKey;
            }
         } else {
            msg.key = "x";
         }

         if (move.eatenPiece != null) {
            if (move.eatenPiece.deadBeforeTrans) {
               msg.die = "x";
            } else {
               msg.die = move.eatenPiece.key;
            }

            if (move.isOK()) {
               SendDeadPiece deadMsg = new SendDeadPiece();
               deadMsg.key = move.eatenPiece.key;
               this.send(deadMsg, gp.getUser());
            }
         }

         msg.result = move.result.result.ordinal();
         this.send(msg);
         if (move.result.result == GameResult.Name.WIN_LOST || move.result.result == GameResult.Name.DRAW) {
            this.endGame(move.result);
         }

      }
   }

   public void takeTurn(GamePlayer gp, Move m) {
      GameController controller = this.gameMgr.game.controller;
      Board board = this.gameMgr.game.board;
      if (board.player == gp.spInfo.chessColor) {
         controller.printBoard(board);
         Move move = controller.moveChess(m, board);
         controller.printBoard(board);
         this.logMove(move);
         if (move.result.result == GameResult.Name.CONTINUE) {
            this.nextChair();
         }

         SendTakeTurn msg = new SendTakeTurn();
         byte[] f = new byte[move.from.length];
         byte[] t = new byte[move.to.length];

         int i;
         for(i = 0; i < move.from.length; ++i) {
            f[i] = (byte)move.from[i];
         }

         for(i = 0; i < move.to.length; ++i) {
            t[i] = (byte)move.to[i];
         }

         msg.from = f;
         msg.to = t;
         msg.chair = gp.chair;
         if (move.piece != null) {
            if (move.isTrans) {
               msg.key = move.piece.transKey;
               msg.isTrans = move.isTrans;
               msg.newKey = move.piece.key;
            } else {
               msg.key = move.piece.key;
               msg.isTrans = move.isTrans;
               msg.newKey = move.piece.transKey;
            }
         } else {
            msg.key = "x";
         }

         if (move.eatenPiece != null) {
            if (move.eatenPiece.deadBeforeTrans) {
               msg.die = "x";
            } else {
               msg.die = move.eatenPiece.key;
            }

            if (move.isOK()) {
               SendDeadPiece deadMsg = new SendDeadPiece();
               deadMsg.key = move.eatenPiece.key;
               this.send(deadMsg, gp.getUser());
            }
         }

         msg.result = move.result.result.ordinal();
         this.send(msg);
         if (move.result.result == GameResult.Name.WIN_LOST || move.result.result == GameResult.Name.DRAW) {
            this.endGame(move.result);
         }

      }
   }

   public void logMove(Move move) {
      this.gameLog.append("DQ<");
      this.gameLog.append(this.gameMgr.currentChair).append(";");
      this.gameLog.append(move.piece.key).append("/").append(move.piece.transKey).append(";");
      this.gameLog.append(move.from[0]).append(";");
      this.gameLog.append(move.from[1]).append(";");
      this.gameLog.append(move.to[0]).append(";");
      this.gameLog.append(move.to[1]).append(";");
      if (move.eatenPiece != null) {
         this.gameLog.append(move.eatenPiece.key).append("/").append(move.eatenPiece.transKey).append(";");
      }

      this.gameLog.append(move.result.result.ordinal()).append(";");
      this.gameLog.append(">");
   }

   public void nextChair() {
      this.gameMgr.currentChair = (this.gameMgr.currentChair + 1) % 2;
      this.changeTurn();
   }

   public void init(GameRoom ro) {
      this.room = ro;
      this.gameMgr.gameServer = this;

      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = new GamePlayer();
         gp.chair = i;
         this.playerList.add(gp);
      }

   }

   public GameManager getGameManager() {
      return this.gameMgr;
   }

   public int getServerState() {
      return this.serverState;
   }

   public GamePlayer getPlayerByChair(int i) {
      return i >= 0 && i < 20 ? (GamePlayer)this.playerList.get(i) : null;
   }

   public GamePlayer getPlayerByGameChair(int gameChair) {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.gameChair == gameChair) {
            return gp;
         }
      }

      return null;
   }

   public GamePlayer getPlayerByName(String name) {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.pInfo != null && gp.pInfo.nickName.equalsIgnoreCase(name)) {
            return gp;
         }
      }

      return null;
   }

   public long getMoneyBet() {
      return this.room.setting.moneyBet;
   }

   public byte getPlayerCount() {
      return (byte)this.playerCount;
   }

   public boolean checkPlayerChair(int chair) {
      return chair >= 0 && chair < 20;
   }

   public synchronized void onGameUserExit(User user) {
      Integer chair = (Integer)user.getProperty("user_chair");
      if (chair != null) {
         GamePlayer gp = this.getPlayerByChair(chair);
         if (gp != null) {
            if (gp.isPlaying()) {
               gp.reqQuitRoom = true;
               this.gameLog.append("DIS<").append(chair).append(">");
            } else {
               boolean disconnect = user.isConnected();
               this.huyDangKy(gp);
               this.removePlayerAtChair(chair, !disconnect);
               this.kiemTraTuDongBatDau(5);
            }

            if (this.room.userManager.size() == 0) {
               this.resetPlayDisconnect();
               this.destroy();
            }

         }
      }
   }

   public void resetPlayDisconnect() {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.pInfo != null) {
            gp.pInfo.setIsHold(false);
         }
      }

   }

   public void onGameUserDis(User user) {
      Integer chair = (Integer)user.getProperty("user_chair");
      if (chair != null) {
         GamePlayer gp = this.getPlayerByChair(chair);
         if (gp != null) {
            if (gp.isPlaying()) {
               gp.reqQuitRoom = true;
               this.gameLog.append("DIS<").append(chair).append(">");
            } else {
               GameRoomManager.instance().leaveRoom(user, this.room);
            }

         }
      }
   }

   public synchronized void onGameUserReturn(User user) {
      if (user != null) {
         for(int i = 0; i < 20; ++i) {
            GamePlayer gp = (GamePlayer)this.playerList.get(i);
            if (gp.playerStatus != 0 && gp.pInfo != null && gp.pInfo.userId == user.getId()) {
               this.gameLog.append("RE<").append(i).append(">");
               GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty("GAME_MONEY_INFO");
               if (moneyInfo != null && gp.gameMoneyInfo.sessionId != moneyInfo.sessionId) {
                  ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, -1);
               }

               user.setProperty("user_chair", gp.chair);
               gp.user = user;
               gp.reqQuitRoom = false;
               user.setProperty("GAME_MONEY_INFO", gp.gameMoneyInfo);
               gp.user.setProperty("user_chair", gp.chair);
               this.sendReconnectInfo(gp.chair);
               break;
            }
         }

      }
   }

   public synchronized void onGameUserEnter(User user) {
      if (user != null) {
         PlayerInfo pInfo = PlayerInfo.getInfo(user);
         if (pInfo != null) {
            GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty("GAME_MONEY_INFO");
            if (moneyInfo != null) {
               int i;
               GamePlayer gp;
               for(i = 0; i < 20; ++i) {
                  gp = (GamePlayer)this.playerList.get(i);
                  if (gp.playerStatus != 0 && gp.pInfo != null && gp.pInfo.userId == user.getId()) {
                     this.gameLog.append("RE<").append(i).append(">");
                     if (moneyInfo != null && gp.gameMoneyInfo.sessionId != moneyInfo.sessionId) {
                        ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, -1);
                     }

                     user.setProperty("user_chair", gp.chair);
                     gp.user = user;
                     gp.reqQuitRoom = false;
                     user.setProperty("GAME_MONEY_INFO", gp.gameMoneyInfo);
                     gp.user.setProperty("user_chair", gp.chair);
                     if (this.serverState == 1) {
                        this.sendGameInfo(gp.chair);
                     } else {
                        this.notifyUserEnter(gp);
                     }

                     return;
                  }
               }

               for(i = 0; i < 20; ++i) {
                  gp = (GamePlayer)this.playerList.get(i);
                  if (gp.playerStatus == 0) {
                     gp.playerStatus = 1;
                     gp.takeChair(user, pInfo, moneyInfo);
                     ++this.playerCount;
                     if (this.playerCount == 1) {
                        this.gameMgr.roomCreatorUserId = user.getId();
                        this.gameMgr.roomOwnerChair = i;
                        this.init();
                     }

                     this.notifyUserEnter(gp);
                     if (this.serverState == 1) {
                        this.sendGameInfo(gp.chair);
                     }
                     break;
                  }
               }

               this.kiemTraTuDongBatDau(5);
            }
         }
      }
   }

   public int getNumTotalPlayer() {
      return this.playerCount;
   }

   public void sendMsgToPlayingUser(BaseMsg msg) {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.isPlaying()) {
            this.send(msg, gp.getUser());
         }
      }

   }

   public void send(BaseMsg msg) {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.getUser() != null) {
            ExtensionUtility.getExtension().send(msg, gp.getUser());
         }
      }

   }

   public boolean coTheChoiTiep(GamePlayer gp) {
      return gp.hasUser() && gp.canPlayNextGame() && gp.gameMoneyInfo.freezeMoney >= this.getMoneyBet();
   }

   public boolean coTheOLaiBan(GamePlayer gp) {
      return gp.hasUser() && !gp.reqQuitRoom && gp.gameMoneyInfo.moneyCheck() && gp.countToOutRoom < 2;
   }

   public synchronized void start() {
      this.gameMgr.isAutoStart = false;
      this.gameLog.setLength(0);
      this.gameLog.append("BD<");
      this.playingCount = 0;
      this.serverState = 1;
      int count = 0;

      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (this.coTheChoiTiep(gp)) {
            gp.playerStatus = 3;
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId(gp.pInfo.nickName, this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(gp.gameChair).append(";");
            gp.choiTiepVanSau = false;
            ++count;
         }
      }

      if (count == 2) {
         this.gameLog.append(this.room.setting.moneyType).append(";");
         this.gameLog.append(">");
         this.xacDinhLuotDi();
         this.gameMgr.gameAction = 1;
         this.gameMgr.countDown = 3;
         this.logStartGame();
      } else {
         this.kiemTraTuDongBatDau(5);
      }

   }

   public void xacDinhLuotDi() {
      if (this.gameMgr.lastWinChair < 0 || this.gameMgr.lastWinChair >= 2) {
         Random rd = new Random();
         this.gameMgr.lastWinChair = Math.abs(rd.nextInt() % 2);
      }

      int starter = (this.gameMgr.lastWinChair + 1) % 2;
      int later = this.gameMgr.lastWinChair;
      GamePlayer start = this.getPlayerByGameChair(starter);
      start.spInfo.chessColor = 'b';
      GamePlayer late = this.getPlayerByGameChair(later);
      late.spInfo.chessColor = 'r';
      SendStartGame msg = new SendStartGame();
      msg.starter = start.gameChair;
      this.gameMgr.currentChair = start.gameChair;

      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.isPlaying()) {
            msg.hasInfoAtChair[i] = true;
            msg.gamePlayers[i] = gp;
            gp.spInfo.start();
         }
      }

      this.send(msg);
   }

   public void logStartGame() {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.isPlaying()) {
            GameUtils.logStartGame(this.gameMgr.game.id, gp.pInfo.nickName, this.gameMgr.game.logTime, this.room.setting.moneyType);
         }
      }

   }

   public int demSoNguoiChoiTiep() {
      int count = 0;

      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (this.coTheChoiTiep(gp)) {
            ++count;
            if (count == 2) {
               break;
            }
         }
      }

      return count;
   }

   public int demSoNguoiDangChoi() {
      int count = 0;

      for(int i = 0; i < 20; ++i) {
         this.getPlayerByChair(i);
         ++count;
      }

      return count;
   }

   public void kiemTraTuDongBatDau(int after) {
      if (this.gameMgr.gameState == 0) {
         int count = this.demSoNguoiChoiTiep();
         if (count < 2) {
            this.gameMgr.cancelAutoStart();
         } else {
            this.gameMgr.makeAutoStart(after);
         }
      }

   }

   public synchronized void removePlayerAtChair(int chair, boolean disconnect) {
      if (this.checkPlayerChair(chair)) {
         GamePlayer gp = (GamePlayer)this.playerList.get(chair);
         gp.outChair();
         this.notifyUserExit(gp, disconnect);
         if (gp.user != null) {
            gp.user.removeProperty("user_chair");
            gp.user.removeProperty("GAME_ROOM");
            gp.user.removeProperty("GAME_MONEY_INFO");
         }

         gp.user = null;
         gp.pInfo = null;
         if (gp.gameMoneyInfo != null) {
            ListGameMoneyInfo.instance().removeGameMoneyInfo(gp.gameMoneyInfo, this.room.getId());
         }

         gp.gameMoneyInfo = null;
         gp.playerStatus = 0;
         --this.playerCount;
         this.kiemTraTuDongBatDau(5);
      }
   }

   public void notifyUserEnter(GamePlayer gamePlayer) {
      User user = gamePlayer.getUser();
      if (user != null) {
         gamePlayer.timeJoinRoom = System.currentTimeMillis();
         SendNewUserJoin msg = new SendNewUserJoin();
         msg.money = gamePlayer.gameMoneyInfo.currentMoney;
         msg.uStatus = gamePlayer.playerStatus;
         msg.setBaseInfo(gamePlayer.pInfo);
         msg.uChair = gamePlayer.chair;
         msg.gameChair = gamePlayer.gameChair;
         this.sendMsgExceptMe(msg, user);
         this.notifyJoinRoomSuccess(gamePlayer);
         this.dangKyChoi(user, (DataCmd)null);
      }
   }

   public void notifyJoinRoomSuccess(GamePlayer gamePlayer) {
      SendJoinRoomSuccess msg = new SendJoinRoomSuccess();
      msg.uChair = gamePlayer.chair;
      msg.roomId = this.room.getId();
      msg.comission = this.room.setting.commisionRate;
      msg.comissionJackpot = this.room.setting.rule;
      msg.moneyType = this.room.setting.moneyType;
      msg.rule = this.room.setting.rule;
      msg.gameId = this.gameMgr.game.id;
      msg.moneyBet = this.room.setting.moneyBet;
      msg.roomOwner = (byte)this.gameMgr.roomOwnerChair;

      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         msg.playerStatus[i] = (byte)gp.playerStatus;
         msg.playerList[i] = gp;
         msg.moneyInfoList[i] = gp.gameMoneyInfo;
      }

      msg.gameAction = (byte)this.gameMgr.gameState;
      msg.gameAction = (byte)this.gameMgr.gameAction;
      msg.curentChair = (byte)this.gameMgr.currentChair;
      msg.countDownTime = (byte)this.gameMgr.countDown;
      this.send(msg, gamePlayer.getUser());
   }

   public void notifyUserExit(GamePlayer gamePlayer, boolean disconnect) {
      if (gamePlayer.pInfo != null) {
         gamePlayer.pInfo.setIsHold(false);
         SendUserExitRoom msg = new SendUserExitRoom();
         msg.nChair = (byte)gamePlayer.chair;
         msg.nickName = gamePlayer.pInfo.nickName;
         this.send(msg);
      }

   }

   public GamePlayer getPlayerByUser(User user) {
      Integer chair = (Integer)user.getProperty("user_chair");
      if (chair != null) {
         GamePlayer gp = this.getPlayerByChair(chair);
         return gp != null && gp.pInfo != null && gp.pInfo.nickName.equalsIgnoreCase(user.getName()) ? gp : null;
      } else {
         return null;
      }
   }

   public void sendReconnectInfo(int chair) {
      GamePlayer me = this.getPlayerByChair(chair);
      if (me != null) {
         SendReconnectInfo msg = new SendReconnectInfo();
         msg.currentChair = this.gameMgr.currentChair;
         msg.gameState = this.gameMgr.gameState;
         msg.gameAction = this.gameMgr.gameAction;
         msg.countdownTime = this.gameMgr.countDown;
         msg.maxUserPerRoom = this.room.setting.maxUserPerRoom;
         msg.moneyType = this.room.setting.moneyType;
         msg.roomBet = this.room.setting.moneyBet;
         msg.gameId = this.gameMgr.game.id;
         msg.map = this.gameMgr.game.board.getMapKey();
         msg.statusMap = this.gameMgr.game.board.getMapStatus();
         msg.diePieces = this.gameMgr.game.board.getDeadPieces(me.spInfo.chessColor);
         msg.roomId = this.room.getId();
         msg.initPrivateInfo(me);

         for(int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.hasUser()) {
               msg.hasInfoAtChair[i] = true;
               msg.pInfos[i] = gp;
            } else {
               msg.hasInfoAtChair[i] = false;
            }
         }

         Move move = this.gameMgr.game.board.getLastMove();
         if (move != null) {
            msg.lastMove[0] = (byte)move.to[0];
            msg.lastMove[1] = (byte)move.to[1];
         } else {
            msg.lastMove[0] = -1;
            msg.lastMove[1] = -1;
         }

         this.send(msg, me.getUser());
      }

   }

   public void sendGameInfo(int chair) {
      GamePlayer me = this.getPlayerByChair(chair);
      if (me != null) {
         SendGameInfo msg = new SendGameInfo();
         msg.currentChair = this.gameMgr.currentChair;
         msg.gameState = this.gameMgr.gameState;
         msg.gameAction = this.gameMgr.gameAction;
         msg.countdownTime = this.gameMgr.countDown;
         msg.maxUserPerRoom = this.room.setting.maxUserPerRoom;
         msg.moneyType = this.room.setting.moneyType;
         msg.roomBet = this.room.setting.moneyBet;
         msg.gameId = this.gameMgr.game.id;
         msg.map = this.gameMgr.game.board.getMapKey();
         msg.statusMap = this.gameMgr.game.board.getMapStatus();
         msg.roomId = this.room.getId();
         msg.initPrivateInfo(me);

         for(int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
               msg.hasInfoAtChair[i] = true;
               msg.pInfos[i] = gp;
            } else {
               msg.hasInfoAtChair[i] = false;
            }
         }

         Move move = this.gameMgr.game.board.getLastMove();
         if (move != null) {
            msg.lastMove[0] = (byte)move.to[0];
            msg.lastMove[1] = (byte)move.to[1];
         } else {
            msg.lastMove[0] = -1;
            msg.lastMove[1] = -1;
         }

         this.send(msg, me.getUser());
      }

   }

   public void pOutRoom(User user, DataCmd dataCmd) {
      GamePlayer gp = this.getPlayerByUser(user);
      if (gp != null) {
         if (gp.isPlaying()) {
            gp.reqQuitRoom = !gp.reqQuitRoom;
            this.notifyRegisterOutRoom(gp);
         } else {
            if (!gp.dangThachDau) {
               user.removeProperty("GAME_ROOM_SETTING");
               user.removeProperty("ENEMY_USER");
            }

            GameRoomManager.instance().leaveRoom(user, this.room);
         }
      }

   }

   private void notifyRegisterOutRoom(GamePlayer gp) {
      SendNotifyReqQuitRoom msg = new SendNotifyReqQuitRoom();
      msg.chair = (byte)gp.gameChair;
      msg.reqQuitRoom = gp.reqQuitRoom;
      this.send(msg);
   }

   public synchronized void endGame(GameResult res) {
      this.gameMgr.gameState = 3;
      this.gameMgr.countDown = 8;
      this.gameMgr.lastWinChair = this.gameMgr.currentChair;
      UserScore score = new UserScore();
      UserScore otherScore = new UserScore();
      GamePlayer gp;
      int otherChair;
      GamePlayer otherPlayer;
      if (res.result != GameResult.Name.WIN_LOST && res.result != GameResult.Name.TIME_OUT && res.result != GameResult.Name.RESIGN) {
         if (res.result == GameResult.Name.DRAW) {
            otherScore.money = (long)((double)(-this.getMoneyBet()) * ((double)this.room.setting.commisionRate / 100.0D));
            otherScore.wastedMoney = otherScore.money;
            otherChair = (this.gameMgr.currentChair + 1) % 2;
            otherPlayer = this.getPlayerByGameChair(otherChair);

            try {
               otherScore.money = otherPlayer.gameMoneyInfo.chargeMoneyInGame(otherScore, this.room.getId(), this.gameMgr.game.id);
               this.dispatchAddEventScore(otherPlayer.getUser(), otherScore);
            } catch (MoneyException var13) {
               CommonHandle.writeErrLog("ERROR WHEN CHARGE MONEY INGAME" + otherPlayer.gameMoneyInfo.toString());
               otherPlayer.reqQuitRoom = true;
            }

            score.money = (long)((double)(-this.getMoneyBet()) * ((double)this.room.setting.commisionRate / 100.0D));
            score.wastedMoney = score.money;
            gp = this.getPlayerByGameChair(this.gameMgr.currentChair);

            try {
               score.money = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
               this.dispatchAddEventScore(gp.getUser(), score);
            } catch (MoneyException var12) {
               CommonHandle.writeErrLog("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString());
               gp.reqQuitRoom = true;
            }
         }
      } else {
         otherScore.money = -this.getMoneyBet();
         otherChair = (this.gameMgr.currentChair + 1) % 2;
         otherScore.lostCount = 1;
         otherPlayer = this.getPlayerByGameChair(otherChair);

         try {
            otherScore.money = otherPlayer.gameMoneyInfo.chargeMoneyInGame(otherScore, this.room.getId(), this.gameMgr.game.id);
            this.dispatchAddEventScore(otherPlayer.getUser(), otherScore);
         } catch (MoneyException var15) {
            CommonHandle.writeErrLog("ERROR WHEN CHARGE MONEY INGAME" + otherPlayer.gameMoneyInfo.toString());
            otherPlayer.reqQuitRoom = true;
            otherScore.money = 0L;
         }

         long moneyWin = -otherScore.money;
         gp = this.getPlayerByGameChair(this.gameMgr.currentChair);
         long wastedMoney = (long)((double)(moneyWin * (long)this.room.setting.commisionRate) / 100.0D);
         moneyWin -= wastedMoney;
         score.money = moneyWin;
         score.winCount = 1;
         score.wastedMoney = wastedMoney;

         try {
            score.money = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            this.dispatchAddEventScore(gp.getUser(), score);
         } catch (MoneyException var14) {
            CommonHandle.writeErrLog("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString());
            gp.reqQuitRoom = true;
         }
      }

      SendEndGame msg = new SendEndGame();

      for(int i = 0; i < 20; ++i) {
         gp = this.getPlayerByChair(i);
         if (gp.isPlaying()) {
            msg.moneyArray[gp.gameChair] = gp.gameMoneyInfo.getCurrentMoneyFromCache();
         }
      }

      msg.result = (byte)res.result.ordinal();
      msg.countdown = this.gameMgr.countDown;
      msg.winChair = (byte)this.gameMgr.currentChair;
      if (res.result != GameResult.Name.DRAW) {
         msg.moneyWin = score.money;
         msg.moneyLost = -otherScore.money;
      } else {
         msg.moneyWin = score.money;
         msg.moneyLost = otherScore.money;
      }

      this.send(msg);
      this.logEndGame(msg);
   }

   private void logEndGame(SendEndGame msg) {
      this.gameLog.append("HC<");
      this.gameLog.append(msg.result).append(";");
      this.gameLog.append(msg.winChair).append(";");
      this.gameLog.append(msg.moneyWin).append(";");
      this.gameLog.append(msg.moneyLost).append(";");
      String[][] endMap = this.gameMgr.game.board.getMapKey();

      for(int i = 0; i < endMap.length; ++i) {
         for(int j = 0; j < endMap[i].length; ++j) {
            this.gameLog.append(endMap[i][j]).append(",");
         }

         this.gameLog.append("/");
      }

      this.gameLog.append(">");
      this.logEndGame();
   }

   public void dispatchAddEventScore(User user, UserScore score) {
      if (user != null) {
         score.moneyType = this.room.setting.moneyType;
         UserScore newScore = score.clone();
         Map evtParams = new HashMap();
         evtParams.put(GameEventParam.USER, user);
         evtParams.put(GameEventParam.USER_SCORE, newScore);
         ExtensionUtility.dispatchEvent(new BZEvent(GameEventType.EVENT_ADD_SCORE, evtParams));
      }
   }

   public void notifyKickRoom(GamePlayer gp, byte reason) {
      SendKickRoom msg = new SendKickRoom();
      msg.reason = reason;
      this.send(msg, gp.getUser());
   }

   public boolean checkMoneyPlayer(GamePlayer gp) {
      return false;
   }

   public synchronized void pPrepareNewGame() {
      this.gameMgr.gameState = 0;
      SendUpdateMatch msg = new SendUpdateMatch();

      int i;
      GamePlayer gp;
      for(i = 0; i < 20; ++i) {
         gp = this.getPlayerByChair(i);
         if (gp.playerStatus != 0) {
            if (GameUtils.isMainTain) {
               gp.reqQuitRoom = true;
               this.notifyKickRoom(gp, (byte)2);
            }

            if (!this.coTheOLaiBan(gp)) {
               if (!gp.checkMoneyCanPlay()) {
                  this.notifyKickRoom(gp, (byte)1);
               }

               if (gp.getUser() != null && this.room != null) {
                  GameRoom gameRoom = (GameRoom)gp.getUser().getProperty("GAME_ROOM");
                  if (gameRoom == this.room) {
                     GameRoomManager.instance().leaveRoom(gp.getUser(), this.room);
                  }
               } else {
                  this.removePlayerAtChair(i, false);
               }

               msg.hasInfoAtChair[i] = false;
            } else {
               msg.hasInfoAtChair[i] = true;
               msg.pInfos[i] = gp;
            }

            gp.prepareNewGame();
            if (gp.regToView) {
               gp.regToView = false;
               this.huyDangKyChoi(gp.getUser(), (DataCmd)null);
            }
         } else {
            msg.hasInfoAtChair[i] = false;
         }
      }

      for(i = 0; i < 20; ++i) {
         gp = this.getPlayerByChair(i);
         if (msg.hasInfoAtChair[i]) {
            msg.chair = (byte)i;
            this.send(msg, gp.getUser());
         }
      }

      this.gameMgr.prepareNewGame();
      this.serverState = 0;
   }

   public void logEndGame() {
      GameUtils.logEndGame(this.gameMgr.game.id, this.gameLog.toString(), this.gameMgr.game.logTime);
   }

   public synchronized void tudongChoi() {
   }

   public void botAutoPlay() {
      if (GameUtils.dev_mod) {
         GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
      }
   }

   public void pCheatCards(User user, DataCmd dataCmd) {
      if (GameUtils.isCheat) {
         RevCheatCard cmd = new RevCheatCard(dataCmd);
         if (cmd.isCheat) {
            this.configGame(cmd.cards, cmd.moneyArray, cmd.chair);
         } else {
            this.gameMgr.game.isCheat = false;
         }

      }
   }

   public void configGame(byte[] cards, long[] moneyArray, int dealer) {
      this.gameMgr.game.isCheat = true;
   }

   public void pDangKyChoiTiep(User user, DataCmd dataCmd) {
      GamePlayer gp = this.getPlayerByUser(user);
      if (gp != null) {
         gp.choiTiepVanSau = true;
      }

   }

   public synchronized void onNoHu(ThongTinThangLon info) {
      this.thongTinNoHu = info;
   }

   public void notifyNoHu() {
      try {
         if (this.thongTinNoHu != null) {
            for(int i = 0; i < 20; ++i) {
               GamePlayer gp = this.getPlayerByChair(i);
               if (gp.gameMoneyInfo.sessionId.equalsIgnoreCase(this.thongTinNoHu.moneySessionId) && gp.gameMoneyInfo.nickName.equalsIgnoreCase(this.thongTinNoHu.nickName)) {
                  gp.gameMoneyInfo.currentMoney = this.thongTinNoHu.currentMoney;
                  break;
               }
            }

            SendNoHu msg = new SendNoHu();
            msg.info = this.thongTinNoHu;
            Iterator var11 = this.room.userManager.entrySet().iterator();

            while(var11.hasNext()) {
               Entry entry = (Entry)var11.next();
               User u = (User)entry.getValue();
               if (u != null) {
                  this.send(msg, u);
               }
            }
         }
      } catch (Exception var8) {
         CommonHandle.writeErrLog(var8);
      } finally {
         this.thongTinNoHu = null;
      }

   }

   public void choNoHu(String nickName) {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.getUser() != null && gp.getUser().getName().equalsIgnoreCase(nickName)) {
         }
      }

   }

   public synchronized void changeTurn() {
      this.gameMgr.gameAction = 0;
      this.gameMgr.countDown = 60;
      SendChangeTurn msg = new SendChangeTurn();
      msg.curentChair = this.gameMgr.currentChair;
      GamePlayer gp = this.getPlayerByGameChair(this.gameMgr.currentChair);
      gp.spInfo.turnTime = this.gameMgr.countDown;
      GameManager var10000 = this.gameMgr;
      var10000.countDown += 3;
      msg.gameTime = gp.spInfo.gameTime;
      msg.turnTime = gp.spInfo.turnTime;
      this.send(msg);
   }

   public synchronized void updatePlayingTime() {
      for(int i = 0; i < 20; ++i) {
         GamePlayer gp = this.getPlayerByChair(i);
         if (gp.isPlaying() && gp.gameChair == this.gameMgr.currentChair) {
            boolean res = gp.updatePlayingTime();
            if (!res) {
               GameResult result = new GameResult();
               result.result = GameResult.Name.TIME_OUT;
               this.gameMgr.currentChair = (this.gameMgr.currentChair + 1) % 2;
               this.endGame(result);
               ++gp.countToOutRoom;
            }
         }
      }

   }

   public synchronized void gameLoop() {
      try {
         this.gameMgr.gameLoop();
      } catch (Exception var2) {
         CommonHandle.writeErrLog("Error in game loop");
         CommonHandle.writeErrLog(var2);
      }

   }

   public void init() {
      if (!this.isRegisterLoop) {
         this.task = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 0, 1, TimeUnit.SECONDS);
         this.isRegisterLoop = true;
      }

   }

   public void destroy() {
      this.task.cancel(false);
      this.isRegisterLoop = false;
   }

   public GameRoom getRoom() {
      return this.room;
   }

   public void setRoom(GameRoom room) {
      this.room = room;
   }

   public String toString() {
      try {
         JSONObject json = this.toJONObject();
         return json != null ? json.toString() : "{}";
      } catch (Exception var2) {
         return "{}";
      }
   }

   public JSONObject toJONObject() {
      try {
         JSONObject json = new JSONObject();
         json.put("gameState", this.gameMgr.gameState);
         json.put("gameAction", this.gameMgr.gameAction);
         JSONArray arr = new JSONArray();

         for(int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            arr.put(gp.toJSONObject());
         }

         json.put("players", arr);
         return json;
      } catch (Exception var5) {
         return null;
      }
   }

   public final class GameLoopTask implements Runnable {
      public void run() {
         try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
            CoupGameServer.this.gameLoop();
         } catch (Throwable var2) {
            var2.printStackTrace();
         }

      }
   }
}
