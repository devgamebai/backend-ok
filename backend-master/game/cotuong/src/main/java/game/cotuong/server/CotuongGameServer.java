/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEvent
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.common.business.Debug
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.eventHandlers.GameEventParam
 *  game.eventHandlers.GameEventType
 *  game.modules.gameRoom.cmd.send.SendNoHu
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  game.modules.gameRoom.entities.GameRoom
 *  game.modules.gameRoom.entities.GameRoomManager
 *  game.modules.gameRoom.entities.GameRoomSetting
 *  game.modules.gameRoom.entities.GameServer
 *  game.modules.gameRoom.entities.ListGameMoneyInfo
 *  game.modules.gameRoom.entities.MoneyException
 *  game.modules.gameRoom.entities.ThongTinThangLon
 *  game.utils.GameUtils
 *  org.json.JSONArray
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.cotuong.server;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEvent;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import game.cotuong.server.GameManager;
import game.cotuong.server.GamePlayer;
import game.cotuong.server.cmd.receive.RevCheatCard;
import game.cotuong.server.cmd.receive.RevDongYHoa;
import game.cotuong.server.cmd.receive.RevDongYThachDau;
import game.cotuong.server.cmd.receive.RevTakeTurn;
import game.cotuong.server.cmd.receive.RevThachDau;
import game.cotuong.server.cmd.send.SendCauHoa;
import game.cotuong.server.cmd.send.SendChangeTurn;
import game.cotuong.server.cmd.send.SendDangKyChoi;
import game.cotuong.server.cmd.send.SendEndGame;
import game.cotuong.server.cmd.send.SendGameInfo;
import game.cotuong.server.cmd.send.SendJoinRoomSuccess;
import game.cotuong.server.cmd.send.SendKetQuaCauHoa;
import game.cotuong.server.cmd.send.SendKetQuaThachDau;
import game.cotuong.server.cmd.send.SendKetQuaXinThua;
import game.cotuong.server.cmd.send.SendKickRoom;
import game.cotuong.server.cmd.send.SendNewUserJoin;
import game.cotuong.server.cmd.send.SendNotifyReqQuitRoom;
import game.cotuong.server.cmd.send.SendReconnectInfo;
import game.cotuong.server.cmd.send.SendStartGame;
import game.cotuong.server.cmd.send.SendTakeTurn;
import game.cotuong.server.cmd.send.SendThachDau;
import game.cotuong.server.cmd.send.SendUpdateMatch;
import game.cotuong.server.cmd.send.SendUserExitRoom;
import game.cotuong.server.logic.Gamble;
import game.cotuong.server.rule.Board;
import game.cotuong.server.rule.GameController;
import game.cotuong.server.rule.GameResult;
import game.cotuong.server.rule.Piece;
import game.cotuong.server.rule.ai.Move;
import game.cotuong.server.sPlayerInfo;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CotuongGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    public ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int gsResult = 2;
    public static final int PHONG_CO_KHOA = 1;
    public static final int PHONG_KHONG_CO_KHOA = 2;
    public static final String USER_CHAIR = "user_chair";
    public final GameManager gameMgr = new GameManager();
    public final Vector<GamePlayer> playerList = new Vector(20);
    public int playingCount = 0;
    public int winType;
    public volatile int serverState = 0;
    public volatile int groupIndex;
    public volatile int playerCount;
    public volatile int registerPlayerCount = 0;
    StringBuilder gameLog = new StringBuilder();
    public final Logger logger = LoggerFactory.getLogger((String)"debug");
    public final Runnable gameLoopTask = new GameLoopTask();
    public int turnIndex = 0;
    public ThongTinThangLon thongTinNoHu = null;

    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        this.logger.info("onGameMessage: ", (Object)dataCmd.getId(), (Object)user.getName());
        switch (dataCmd.getId()) {
            case 3111: {
                this.pOutRoom(user, dataCmd);
                break;
            }
            case 3101: {
                this.takeTurn(user, dataCmd);
                break;
            }
            case 3102: {
                this.cauHoa(user, dataCmd);
                break;
            }
            case 3112: {
                this.dongYHoa(user, dataCmd);
                break;
            }
            case 3105: {
                this.xinThua(user, dataCmd);
                break;
            }
            case 3106: {
                this.thachDau(user, dataCmd);
                break;
            }
            case 3117: {
                this.dongYThachDau(user, dataCmd);
                break;
            }
            case 3108: {
                this.dangKyChoi(user, dataCmd);
                break;
            }
            case 3109: {
                this.huyDangKyChoi(user, dataCmd);
            }
        }
    }

    private void thachDau(User user, DataCmd data) {
        RevThachDau cmd = new RevThachDau(data);
        GamePlayer gp = this.getPlayerByUser(user);
        if (!gp.dangThachDau && gp != null && !gp.isPlaying() && gp.checkMoreMoneyThan(cmd.moneyBet)) {
            ++gp.thachDau;
            if (gp.thachDau <= 10) {
                GamePlayer gp1 = this.getPlayerByName(cmd.enemy);
                if (gp1 != null && !gp1.isPlaying() && !gp1.dangThachDau && gp1.checkMoreMoneyThan(cmd.moneyBet)) {
                    SendThachDau msg = new SendThachDau();
                    msg.name = user.getName();
                    msg.moneyBet = cmd.moneyBet;
                    this.send((BaseMsg)msg, gp1.getUser());
                    gp.dangThachDau = true;
                }
            } else {
                SendKetQuaThachDau msg = new SendKetQuaThachDau();
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        } else {
            SendKetQuaThachDau msg = new SendKetQuaThachDau();
            msg.Error = 3;
            this.send((BaseMsg)msg, user);
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
                user.setProperty((Object)"GAME_ROOM_SETTING", (Object)setting);
                user.setProperty((Object)"ENEMY_USER", (Object)enemy);
                enemy.setProperty((Object)"GAME_ROOM_SETTING", (Object)setting);
                enemy.setProperty((Object)"ENEMY_USER", (Object)user);
            }
            GameRoomManager.instance().leaveRoom(user, this.room);
            GameRoomManager.instance().leaveRoom(enemy, this.room);
        } else if (gp == null || gp1 != null && gp1.dangThachDau) {
            gp1.dangThachDau = false;
            SendKetQuaThachDau msg = new SendKetQuaThachDau();
            msg.Error = 2;
            this.send((BaseMsg)msg, gp1.getUser());
        }
    }

    private void cauHoa(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameState != 1) {
            return;
        }
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && gp.isPlaying() && !gp.dangCauHoa) {
            ++gp.cauHoa;
            if (gp.cauHoa <= 3) {
                int otherChair = (gp.gameChair + 1) % 2;
                GamePlayer gp1 = this.getPlayerByGameChair(otherChair);
                if (gp1.isPlaying() && !gp1.dangCauHoa) {
                    SendCauHoa msg = new SendCauHoa();
                    this.send((BaseMsg)msg, gp1.getUser());
                    gp.dangCauHoa = true;
                }
            } else {
                SendKetQuaCauHoa msg = new SendKetQuaCauHoa();
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        } else {
            SendKetQuaCauHoa msg = new SendKetQuaCauHoa();
            msg.Error = 1;
            this.send((BaseMsg)msg, user);
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
            this.send((BaseMsg)msg, gp1.getUser());
        }
    }

    private synchronized void xinThua(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameState != 1) {
            return;
        }
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
            this.send((BaseMsg)msg, user);
        }
    }

    public void dangKyChoi(User user, DataCmd dataCmd) {
        SendDangKyChoi msg = new SendDangKyChoi();
        if (this.registerPlayerCount >= 2) {
            return;
        }
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && !gp.regToPlay) {
            gp.playerStatus = 2;
            GamePlayer gp1 = null;
            if (this.registerPlayerCount == 1) {
                gp1 = this.getPlayerByGameChair(0);
            }
            gp.gameChair = gp1 != null ? 1 : 0;
            ++this.registerPlayerCount;
            gp.regToPlay = true;
            msg.gp = gp;
            this.send(msg);
        }
    }

    public void huyDangKyChoi(User user, DataCmd dataCmd) {
        SendDangKyChoi msg = new SendDangKyChoi();
        msg.action = (byte)2;
        GamePlayer gp = this.getPlayerByUser(user);
        int res = this.huyDangKy(gp);
        if (res == 1) {
            gp.playerStatus = 1;
            msg.gp = gp;
            this.send(msg);
        } else if (res == 2) {
            msg.action = (byte)3;
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
            }
            gp.regToView = true;
            return 2;
        }
        return 0;
    }

    public void takeTurn(User user, DataCmd dataCmd) {
        RevTakeTurn cmd = new RevTakeTurn(dataCmd);
        GamePlayer gp = this.getPlayerByUser(user);
        if (this.gameMgr.gameAction == 0 && gp != null && gp.isPlaying() && gp.gameChair == this.gameMgr.currentChair) {
            int i;
            int[] from = new int[cmd.from.length];
            int[] to = new int[cmd.to.length];
            for (i = 0; i < cmd.from.length; ++i) {
                from[i] = cmd.from[i];
            }
            for (i = 0; i < cmd.to.length; ++i) {
                to[i] = cmd.to[i];
            }
            this.takeTurn(gp, from, to);
        } else {
            Debug.trace((Object[])new Object[]{"Cannot play:", gp.gameChair == this.gameMgr.currentChair, this.gameMgr.gameAction == 0});
        }
    }

    public void takeTurn(GamePlayer gp, int[] from, int[] to) {
        int i;
        GameController controller = this.gameMgr.game.controller;
        Board board = this.gameMgr.game.board;
        if (board.player != gp.spInfo.chessColor) {
            return;
        }
        Move move = controller.moveChess(from, to, board);
        this.logMove(move);
        if (move.result.result == GameResult.Name.CONTINUE) {
            this.nextChair();
        }
        SendTakeTurn msg = new SendTakeTurn();
        byte[] f = new byte[move.from.length];
        byte[] t = new byte[move.to.length];
        for (i = 0; i < move.from.length; ++i) {
            f[i] = (byte)move.from[i];
        }
        for (i = 0; i < move.to.length; ++i) {
            t[i] = (byte)move.to[i];
        }
        msg.from = f;
        msg.to = t;
        msg.chair = gp.chair;
        msg.key = move.piece != null ? move.piece.key : "x";
        if (move.eatenPiece != null) {
            msg.die = move.eatenPiece.key;
        }
        msg.result = move.result.result.ordinal();
        Debug.trace((Object[])new Object[]{"Move: ", move});
        this.send(msg);
        if (move.result.result == GameResult.Name.WIN_LOST || move.result.result == GameResult.Name.DRAW) {
            this.endGame(move.result);
        }
    }

    public void logMove(Move move) {
        this.gameLog.append("DQ<");
        this.gameLog.append(this.gameMgr.currentChair).append(";");
        this.gameLog.append(move.piece.key).append(";");
        this.gameLog.append(move.from[0]).append(";");
        this.gameLog.append(move.from[1]).append(";");
        this.gameLog.append(move.to[0]).append(";");
        this.gameLog.append(move.to[1]).append(";");
        if (move.eatenPiece != null) {
            this.gameLog.append(move.eatenPiece.key).append(";");
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
        int i = 0;
        while (i < 20) {
            GamePlayer gp = new GamePlayer();
            gp.chair = i++;
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
        if (i >= 0 && i < 20) {
            return this.playerList.get(i);
        }
        return null;
    }

    public GamePlayer getPlayerByGameChair(int gameChair) {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.gameChair != gameChair) continue;
            return gp;
        }
        return null;
    }

    public GamePlayer getPlayerByName(String name) {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null || !gp.pInfo.nickName.equalsIgnoreCase(name)) continue;
            return gp;
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
        Integer chair = (Integer)user.getProperty((Object)USER_CHAIR);
        if (chair == null) {
            Debug.trace((Object[])new Object[]{"onGameUserExit", "chair null", user.getName()});
            return;
        }
        GamePlayer gp = this.getPlayerByChair(chair);
        if (gp == null) {
            return;
        }
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

    public void resetPlayDisconnect() {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null) continue;
            gp.pInfo.setIsHold(false);
        }
    }

    public void onGameUserDis(User user) {
        Integer chair = (Integer)user.getProperty((Object)USER_CHAIR);
        if (chair == null) {
            Debug.trace((Object[])new Object[]{"onGameUserExit", "chair null", user.getName()});
            return;
        }
        GamePlayer gp = this.getPlayerByChair(chair);
        if (gp == null) {
            return;
        }
        if (gp.isPlaying()) {
            gp.reqQuitRoom = true;
            this.gameLog.append("DIS<").append(chair).append(">");
        } else {
            GameRoomManager.instance().leaveRoom(user, this.room);
        }
    }

    public synchronized void onGameUserReturn(User user) {
        if (user == null) {
            return;
        }
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (gp.playerStatus == 0 || gp.pInfo == null || gp.pInfo.userId != user.getId()) continue;
            this.gameLog.append("RE<").append(i).append(">");
            GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
            if (moneyInfo != null && gp.gameMoneyInfo.sessionId != moneyInfo.sessionId) {
                ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, -1);
            }
            user.setProperty((Object)USER_CHAIR, (Object)gp.chair);
            gp.user = user;
            gp.reqQuitRoom = false;
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)gp.gameMoneyInfo);
            gp.user.setProperty((Object)USER_CHAIR, (Object)gp.chair);
            this.sendReconnectInfo(gp.chair);
            break;
        }
    }

    public synchronized void onGameUserEnter(User user) {
        int i;
        GamePlayer gp;
        if (user == null) {
            return;
        }
        PlayerInfo pInfo = PlayerInfo.getInfo((User)user);
        if (pInfo == null) {
            return;
        }
        GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
        if (moneyInfo == null) {
            return;
        }
        for (i = 0; i < 20; ++i) {
            gp = this.playerList.get(i);
            if (gp.playerStatus == 0 || gp.pInfo == null || gp.pInfo.userId != user.getId()) continue;
            this.gameLog.append("RE<").append(i).append(">");
            if (moneyInfo != null && gp.gameMoneyInfo.sessionId != moneyInfo.sessionId) {
                ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, -1);
            }
            user.setProperty((Object)USER_CHAIR, (Object)gp.chair);
            gp.user = user;
            gp.reqQuitRoom = false;
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)gp.gameMoneyInfo);
            gp.user.setProperty((Object)USER_CHAIR, (Object)gp.chair);
            if (this.serverState == 1) {
                this.sendGameInfo(gp.chair);
            } else {
                this.notifyUserEnter(gp);
            }
            return;
        }
        for (i = 0; i < 20; ++i) {
            gp = this.playerList.get(i);
            if (gp.playerStatus != 0) continue;
            gp.playerStatus = 1;
            gp.takeChair(user, pInfo, moneyInfo);
            ++this.playerCount;
            if (this.playerCount == 1) {
                this.gameMgr.roomCreatorUserId = user.getId();
                this.gameMgr.roomOwnerChair = i;
                this.init();
            }
            this.notifyUserEnter(gp);
            if (this.serverState != 1) break;
            this.sendGameInfo(gp.chair);
            break;
        }
        this.kiemTraTuDongBatDau(5);
    }

    public int getNumTotalPlayer() {
        return this.playerCount;
    }

    public void sendMsgToPlayingUser(BaseMsg msg) {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.send(msg, gp.getUser());
        }
    }

    public void send(BaseMsg msg) {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
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
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            gp.playerStatus = 3;
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)gp.pInfo.nickName, (int)this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(gp.gameChair).append(";");
            gp.choiTiepVanSau = false;
            ++count;
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
        start.spInfo.chessColor = (char)98;
        GamePlayer late = this.getPlayerByGameChair(later);
        late.spInfo.chessColor = (char)114;
        SendStartGame msg = new SendStartGame();
        msg.starter = start.gameChair;
        this.gameMgr.currentChair = start.gameChair;
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            msg.hasInfoAtChair[i] = true;
            msg.gamePlayers[i] = gp;
            gp.spInfo.start();
        }
        Debug.trace((Object[])new Object[]{"starter: ", msg.starter, "color: ", Character.valueOf(start.spInfo.chessColor)});
        this.send(msg);
    }

    public void logStartGame() {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            Debug.trace((Object[])new Object[]{"logStartGame", gp.gameChair, gp.pInfo.nickName});
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public int demSoNguoiChoiTiep() {
        GamePlayer gp;
        int count = 0;
        for (int i = 0; !(i >= 20 || this.coTheChoiTiep(gp = this.getPlayerByChair(i)) && ++count == 2); ++i) {
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
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
        if (!this.checkPlayerChair(chair)) {
            Debug.trace((Object[])new Object[]{"removePlayerAtChair error", chair});
            return;
        }
        GamePlayer gp = this.playerList.get(chair);
        gp.outChair();
        this.notifyUserExit(gp, disconnect);
        if (gp.user != null) {
            gp.user.removeProperty((Object)USER_CHAIR);
            gp.user.removeProperty((Object)"GAME_ROOM");
            gp.user.removeProperty((Object)"GAME_MONEY_INFO");
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

    public void notifyUserEnter(GamePlayer gamePlayer) {
        User user = gamePlayer.getUser();
        if (user == null) {
            return;
        }
        gamePlayer.timeJoinRoom = System.currentTimeMillis();
        SendNewUserJoin msg = new SendNewUserJoin();
        msg.money = gamePlayer.gameMoneyInfo.currentMoney;
        msg.uStatus = gamePlayer.playerStatus;
        msg.setBaseInfo(gamePlayer.pInfo);
        msg.uChair = gamePlayer.chair;
        msg.gameChair = gamePlayer.gameChair;
        this.sendMsgExceptMe((BaseMsg)msg, user);
        this.notifyJoinRoomSuccess(gamePlayer);
        this.dangKyChoi(user, null);
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
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.playerStatus;
            msg.playerList[i] = gp;
            msg.moneyInfoList[i] = gp.gameMoneyInfo;
        }
        msg.gameAction = (byte)this.gameMgr.gameState;
        msg.gameAction = (byte)this.gameMgr.gameAction;
        msg.curentChair = (byte)this.gameMgr.currentChair;
        msg.countDownTime = (byte)this.gameMgr.countDown;
        this.send((BaseMsg)msg, gamePlayer.getUser());
    }

    public void notifyUserExit(GamePlayer gamePlayer, boolean disconnect) {
        if (gamePlayer.pInfo != null) {
            Debug.trace((Object[])new Object[]{gamePlayer.pInfo.nickName, gamePlayer.chair, "exit room ", disconnect});
            gamePlayer.pInfo.setIsHold(false);
            SendUserExitRoom msg = new SendUserExitRoom();
            msg.nChair = (byte)gamePlayer.chair;
            msg.nickName = gamePlayer.pInfo.nickName;
            this.send(msg);
        } else {
            Debug.trace((Object[])new Object[]{gamePlayer.chair, "exit room playerInfo null"});
        }
    }

    public GamePlayer getPlayerByUser(User user) {
        Integer chair = (Integer)user.getProperty((Object)USER_CHAIR);
        Debug.trace((Object[])new Object[]{"getPlayerByUser: ", user.getName(), chair});
        if (chair != null) {
            GamePlayer gp = this.getPlayerByChair(chair);
            if (gp != null && gp.pInfo != null && gp.pInfo.nickName.equalsIgnoreCase(user.getName())) {
                return gp;
            }
            return null;
        }
        return null;
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
            msg.roomId = this.room.getId();
            msg.initPrivateInfo(me);
            for (int i = 0; i < 20; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (gp.hasUser()) {
                    msg.hasInfoAtChair[i] = true;
                    msg.pInfos[i] = gp;
                    continue;
                }
                msg.hasInfoAtChair[i] = false;
            }
            Move move = this.gameMgr.game.board.getLastMove();
            if (move != null) {
                msg.lastMove[0] = (byte)move.to[0];
                msg.lastMove[1] = (byte)move.to[1];
            } else {
                msg.lastMove[0] = -1;
                msg.lastMove[1] = -1;
            }
            this.send((BaseMsg)msg, me.getUser());
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
            msg.roomId = this.room.getId();
            msg.initPrivateInfo(me);
            for (int i = 0; i < 20; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (gp.isPlaying()) {
                    msg.hasInfoAtChair[i] = true;
                    msg.pInfos[i] = gp;
                    continue;
                }
                msg.hasInfoAtChair[i] = false;
            }
            Move move = this.gameMgr.game.board.getLastMove();
            if (move != null) {
                msg.lastMove[0] = (byte)move.to[0];
                msg.lastMove[1] = (byte)move.to[1];
            } else {
                msg.lastMove[0] = -1;
                msg.lastMove[1] = -1;
            }
            this.send((BaseMsg)msg, me.getUser());
        }
    }

    public void pOutRoom(User user, DataCmd dataCmd) {
        Debug.trace((Object[])new Object[]{"pOutRoom", user.getName()});
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            if (gp.isPlaying()) {
                gp.reqQuitRoom = !gp.reqQuitRoom;
                this.notifyRegisterOutRoom(gp);
            } else {
                if (!gp.dangThachDau) {
                    user.removeProperty((Object)"GAME_ROOM_SETTING");
                    user.removeProperty((Object)"ENEMY_USER");
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
        int otherChair;
        GamePlayer otherPlayer;
        Debug.trace((Object)"End game");
        this.gameMgr.gameState = 3;
        this.gameMgr.countDown = 8;
        this.gameMgr.lastWinChair = this.gameMgr.currentChair;
        UserScore score = new UserScore();
        UserScore otherScore = new UserScore();
        if (res.result == GameResult.Name.WIN_LOST || res.result == GameResult.Name.TIME_OUT || res.result == GameResult.Name.RESIGN) {
            otherScore.money = -this.getMoneyBet();
            otherChair = (this.gameMgr.currentChair + 1) % 2;
            otherScore.lostCount = 1;
            otherPlayer = this.getPlayerByGameChair(otherChair);
            try {
                otherScore.money = otherPlayer.gameMoneyInfo.chargeMoneyInGame(otherScore, this.room.getId(), this.gameMgr.game.id);
                Debug.trace((Object[])new Object[]{"Money after charge:", otherPlayer.gameMoneyInfo});
                this.dispatchAddEventScore(otherPlayer.getUser(), otherScore);
            }
            catch (MoneyException e) {
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + otherPlayer.gameMoneyInfo.toString()));
                otherPlayer.reqQuitRoom = true;
                otherScore.money = 0L;
            }
            long moneyWin = -otherScore.money;
            GamePlayer gp = this.getPlayerByGameChair(this.gameMgr.currentChair);
            long wastedMoney = (long)((double)(moneyWin * (long)this.room.setting.commisionRate) / 100.0);
            score.money = moneyWin -= wastedMoney;
            score.winCount = 1;
            score.wastedMoney = wastedMoney;
            try {
                score.money = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                Debug.trace((Object[])new Object[]{"Money after charge:", gp.gameMoneyInfo});
                this.dispatchAddEventScore(gp.getUser(), score);
            }
            catch (MoneyException e) {
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
        } else if (res.result == GameResult.Name.DRAW) {
            otherScore.wastedMoney = otherScore.money = (long)((double)(-this.getMoneyBet()) * ((double)this.room.setting.commisionRate / 100.0));
            otherChair = (this.gameMgr.currentChair + 1) % 2;
            otherPlayer = this.getPlayerByGameChair(otherChair);
            try {
                otherScore.money = otherPlayer.gameMoneyInfo.chargeMoneyInGame(otherScore, this.room.getId(), this.gameMgr.game.id);
                Debug.trace((Object[])new Object[]{"Money after charge:", otherPlayer.gameMoneyInfo});
                this.dispatchAddEventScore(otherPlayer.getUser(), otherScore);
            }
            catch (MoneyException e) {
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + otherPlayer.gameMoneyInfo.toString()));
                otherPlayer.reqQuitRoom = true;
            }
            score.wastedMoney = score.money = (long)((double)(-this.getMoneyBet()) * ((double)this.room.setting.commisionRate / 100.0));
            GamePlayer gp = this.getPlayerByGameChair(this.gameMgr.currentChair);
            try {
                score.money = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                Debug.trace((Object[])new Object[]{"Money after charge:", gp.gameMoneyInfo});
                this.dispatchAddEventScore(gp.getUser(), score);
            }
            catch (MoneyException e) {
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
        }
        SendEndGame msg = new SendEndGame();
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            msg.moneyArray[gp.gameChair] = gp.gameMoneyInfo.getCurrentMoneyFromCache();
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
        for (int i = 0; i < endMap.length; ++i) {
            for (int j = 0; j < endMap[i].length; ++j) {
                this.gameLog.append(endMap[i][j]).append(",");
            }
            this.gameLog.append("/");
        }
        this.gameLog.append(">");
        this.logEndGame();
    }

    public void dispatchAddEventScore(User user, UserScore score) {
        if (user == null) {
            return;
        }
        Debug.trace((Object[])new Object[]{"Change money user:", user.getName(), GameUtils.toJsonString((Object)score)});
        score.moneyType = this.room.setting.moneyType;
        UserScore newScore = score.clone();
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.USER, (Object)user);
        evtParams.put(GameEventParam.USER_SCORE, (Object)newScore);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.EVENT_ADD_SCORE, evtParams));
    }

    public void notifyKickRoom(GamePlayer gp, byte reason) {
        SendKickRoom msg = new SendKickRoom();
        msg.reason = reason;
        this.send((BaseMsg)msg, gp.getUser());
    }

    public boolean checkMoneyPlayer(GamePlayer gp) {
        return false;
    }

    public synchronized void pPrepareNewGame() {
        int i;
        GamePlayer gp;
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 20; ++i) {
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
                        GameRoom gameRoom = (GameRoom)gp.getUser().getProperty((Object)"GAME_ROOM");
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
                if (!gp.regToView) continue;
                gp.regToView = false;
                this.huyDangKyChoi(gp.getUser(), null);
                continue;
            }
            msg.hasInfoAtChair[i] = false;
        }
        for (i = 0; i < 20; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
    }

    public void logEndGame() {
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    public synchronized void tudongChoi() {
    }

    public void botAutoPlay() {
        if (!GameUtils.dev_mod) {
            return;
        }
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
    }

    public void pCheatCards(User user, DataCmd dataCmd) {
        if (!GameUtils.isCheat) {
            return;
        }
        RevCheatCard cmd = new RevCheatCard(dataCmd);
        if (cmd.isCheat) {
            this.configGame(cmd.cards, cmd.moneyArray, cmd.chair);
        } else {
            this.gameMgr.game.isCheat = false;
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void notifyNoHu() {
        try {
            if (this.thongTinNoHu != null) {
                for (int i = 0; i < 20; ++i) {
                    GamePlayer gp = this.getPlayerByChair(i);
                    if (!gp.gameMoneyInfo.sessionId.equalsIgnoreCase(this.thongTinNoHu.moneySessionId) || !gp.gameMoneyInfo.nickName.equalsIgnoreCase(this.thongTinNoHu.nickName)) continue;
                    gp.gameMoneyInfo.currentMoney = this.thongTinNoHu.currentMoney;
                    break;
                }
                SendNoHu msg = new SendNoHu();
                msg.info = this.thongTinNoHu;
                for (Map.Entry entry : this.room.userManager.entrySet()) {
                    User u = (User)entry.getValue();
                    if (u == null) continue;
                    this.send((BaseMsg)msg, u);
                }
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        finally {
            this.thongTinNoHu = null;
        }
    }

    public void choNoHu(String nickName) {
        for (int i = 0; i < 20; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() != null && !gp.getUser().getName().equalsIgnoreCase(nickName)) continue;
        }
    }

    public synchronized void changeTurn() {
        this.gameMgr.gameAction = 0;
        this.gameMgr.countDown = 90;
        SendChangeTurn msg = new SendChangeTurn();
        msg.curentChair = this.gameMgr.currentChair;
        GamePlayer gp = this.getPlayerByGameChair(this.gameMgr.currentChair);
        gp.spInfo.turnTime = this.gameMgr.countDown;
        this.gameMgr.countDown += 3;
        msg.gameTime = gp.spInfo.gameTime;
        msg.turnTime = gp.spInfo.turnTime;
        this.send(msg);
    }

    public synchronized void updatePlayingTime() {
        for (int i = 0; i < 20; ++i) {
            boolean res;
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.gameChair != this.gameMgr.currentChair || (res = gp.updatePlayingTime())) continue;
            GameResult result = new GameResult();
            result.result = GameResult.Name.TIME_OUT;
            this.gameMgr.currentChair = (this.gameMgr.currentChair + 1) % 2;
            this.endGame(result);
            ++gp.countToOutRoom;
        }
    }

    public synchronized void gameLoop() {
        try {
            this.gameMgr.gameLoop();
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((String)"Error in game loop");
            CommonHandle.writeErrLog((Throwable)e);
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
            if (json != null) {
                return json.toString();
            }
            return "{}";
        }
        catch (Exception e) {
            return "{}";
        }
    }

    public JSONObject toJONObject() {
        try {
            JSONObject json = new JSONObject();
            json.put("gameState", this.gameMgr.gameState);
            json.put("gameAction", this.gameMgr.gameAction);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < 20; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                arr.put((Object)gp.toJSONObject());
            }
            json.put("players", (Object)arr);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }

    public final class GameLoopTask
    implements Runnable {
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                CotuongGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

