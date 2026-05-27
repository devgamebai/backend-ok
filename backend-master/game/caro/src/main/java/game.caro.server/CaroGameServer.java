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
package game.caro.server;

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
import game.caro.server.GameManager;
import game.caro.server.GamePlayer;
import game.caro.server.cmd.receive.RevCheatCard;
import game.caro.server.cmd.receive.RevTakeTurn;
import game.caro.server.cmd.send.SendChangeTurn;
import game.caro.server.cmd.send.SendEndGame;
import game.caro.server.cmd.send.SendGameInfo;
import game.caro.server.cmd.send.SendJoinRoomSuccess;
import game.caro.server.cmd.send.SendKickRoom;
import game.caro.server.cmd.send.SendNewUserJoin;
import game.caro.server.cmd.send.SendNotifyReqQuitRoom;
import game.caro.server.cmd.send.SendStartGame;
import game.caro.server.cmd.send.SendTakeTurn;
import game.caro.server.cmd.send.SendUpdateMatch;
import game.caro.server.cmd.send.SendUserExitRoom;
import game.caro.server.logic.CaroCell;
import game.caro.server.logic.CaroTable;
import game.caro.server.logic.Gamble;
import game.caro.server.logic.GameResult;
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
import java.util.ArrayList;
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

public class CaroGameServer
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
    public final Vector<GamePlayer> playerList = new Vector(2);
    public int playingCount = 0;
    public int winType;
    public volatile int serverState = 0;
    public volatile int groupIndex;
    public volatile int playerCount;
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
            }
        }
    }

    public void takeTurn(User user, DataCmd dataCmd) {
        RevTakeTurn cmd = new RevTakeTurn(dataCmd);
        GamePlayer gp = this.getPlayerByUser(user);
        if (this.gameMgr.gameState == 1 && this.gameMgr.gameAction == 0 && gp != null && gp.isPlaying() && gp.chair == this.gameMgr.currentChair) {
            this.takeTurn(gp, cmd.x, cmd.y);
        } else {
            Debug.trace((Object[])new Object[]{"Cannot play:", gp.chair == this.gameMgr.currentChair, this.gameMgr.gameAction == 0});
        }
    }

    public void takeTurn(GamePlayer gp, int x, int y) {
        CaroTable table = this.gameMgr.game.table;
        GameResult res = table.play(x, y, gp.tickType);
        if (res.result != GameResult.Name.ERROR) {
            SendTakeTurn msg = new SendTakeTurn();
            msg.chair = this.gameMgr.currentChair;
            msg.x = x;
            msg.y = y;
            msg.type = gp.tickType;
            this.send(msg);
            if (res.result == GameResult.Name.CONTINUE) {
                this.nextChair();
            } else {
                this.endGame(res);
            }
        }
    }

    public void nextChair() {
        this.gameMgr.currentChair = (this.gameMgr.currentChair + 1) % 2;
        this.changeTurn();
    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 2) {
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
        if (i >= 0 && i < 2) {
            return this.playerList.get(i);
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
        return chair >= 0 && chair < 2;
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
            if (this.gameMgr.isAutoStart) {
                this.kiemTraTuDongBatDau(5);
            }
            this.removePlayerAtChair(chair, !disconnect);
        }
        if (this.room.userManager.size() == 0) {
            this.resetPlayDisconnect();
            this.destroy();
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 2; ++i) {
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
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (gp.getPlayerStatus() == 0 || gp.pInfo == null || gp.pInfo.userId != user.getId()) continue;
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
            this.sendGameInfo(gp.chair);
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
        for (i = 0; i < 2; ++i) {
            gp = this.playerList.get(i);
            if (gp.getPlayerStatus() == 0 || gp.pInfo == null || gp.pInfo.userId != user.getId()) continue;
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
        if (this.room.setting.maxUserPerRoom == 2) {
            for (i = 0; i < 2; ++i) {
                gp = this.playerList.get(i);
                if (gp.getPlayerStatus() != 0) continue;
                if (this.serverState == 0) {
                    gp.setPlayerStatus(2);
                } else {
                    gp.setPlayerStatus(1);
                }
                gp.takeChair(user, pInfo, moneyInfo);
                ++this.playerCount;
                if (this.playerCount == 1) {
                    this.gameMgr.roomCreatorUserId = user.getId();
                    this.gameMgr.roomOwnerChair = i;
                    this.init();
                }
                this.notifyUserEnter(gp);
                break;
            }
        }
        this.kiemTraTuDongBatDau(5);
    }

    public int getNumTotalPlayer() {
        return this.playerCount;
    }

    public void sendMsgToPlayingUser(BaseMsg msg) {
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.send(msg, gp.getUser());
        }
    }

    public void send(BaseMsg msg) {
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
        }
    }

    public boolean coTheChoiTiep(GamePlayer gp) {
        return gp.hasUser() && gp.canPlayNextGame();
    }

    public synchronized void start() {
        this.gameMgr.isAutoStart = false;
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        this.serverState = 1;
        int count = 0;
        ArrayList players = new ArrayList();
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            gp.setPlayerStatus(3);
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)gp.pInfo.nickName, (int)this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(i).append(";");
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
            this.resetPlayDisconnect();
            this.kiemTraTuDongBatDau(5);
        }
    }

    public void xacDinhLuotDi() {
        Random rd = new Random();
        int random = rd.nextInt(2);
        int starter = random % 2;
        int later = (random + 1) % 2;
        GamePlayer start = this.getPlayerByChair(starter);
        start.tickType = 1;
        GamePlayer late = this.getPlayerByChair(later);
        late.tickType = 2;
        SendStartGame msg = new SendStartGame();
        msg.starter = starter;
        this.gameMgr.currentChair = starter;
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            msg.hasInfoAtChair[i] = true;
            msg.gamePlayers[i] = gp;
        }
        this.gameLog.append("DD<");
        this.gameLog.append(random).append(";");
        this.gameLog.append(this.gameMgr.currentChair).append(";");
        this.gameLog.append("").append(";");
        this.gameLog.append(">");
        this.send(msg);
    }

    public void logStartGame() {
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            Debug.trace((Object[])new Object[]{"logStartGame", gp.chair, gp.pInfo.nickName});
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public int demSoNguoiChoiTiep() {
        int count = 0;
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            ++count;
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 2; ++i) {
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

    public boolean coTheOLaiBan(GamePlayer gp) {
        return gp.user != null && gp.user.isConnected() && !gp.reqQuitRoom;
    }

    public synchronized void removePlayerAtChair(int chair, boolean disconnect) {
        if (!this.checkPlayerChair(chair)) {
            Debug.trace((Object[])new Object[]{"removePlayerAtChair error", chair});
            return;
        }
        GamePlayer gp = this.playerList.get(chair);
        gp.standUp = false;
        gp.choiTiepVanSau = true;
        gp.countToOutRoom = 0;
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
        gp.setPlayerStatus(0);
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
        msg.uStatus = gamePlayer.getPlayerStatus();
        msg.setBaseInfo(gamePlayer.pInfo);
        msg.uChair = gamePlayer.chair;
        this.sendMsgExceptMe((BaseMsg)msg, user);
        this.notifyJoinRoomSuccess(gamePlayer);
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
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
            msg.playerList[i] = gp.getPlayerInfo();
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
            msg.map = this.gameMgr.game.table.getMap();
            msg.lastX = this.gameMgr.game.table.lastX;
            msg.lastY = this.gameMgr.game.table.lastY;
            msg.roomId = this.room.getId();
            msg.initPrivateInfo(me);
            for (int i = 0; i < 2; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (gp.isPlaying()) {
                    msg.hasInfoAtChair[i] = true;
                    msg.pInfos[i] = gp;
                    continue;
                }
                msg.hasInfoAtChair[i] = false;
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
                GameRoomManager.instance().leaveRoom(user, this.room);
            }
        }
    }

    private void notifyRegisterOutRoom(GamePlayer gp) {
        SendNotifyReqQuitRoom msg = new SendNotifyReqQuitRoom();
        msg.chair = (byte)gp.chair;
        msg.reqQuitRoom = gp.reqQuitRoom;
        this.send(msg);
    }

    public void endGame(GameResult res) {
        Debug.trace((Object)"End game");
        this.gameMgr.gameState = 3;
        this.gameMgr.countDown = 5;
        this.gameMgr.lastWinChair = this.gameMgr.currentChair;
        UserScore score = new UserScore();
        UserScore otherScore = new UserScore();
        if (res.result == GameResult.Name.WIN_LOST) {
            otherScore.money = -this.getMoneyBet();
            int otherChair = (this.gameMgr.currentChair + 1) % 2;
            otherScore.lostCount = 1;
            GamePlayer otherPlayer = this.getPlayerByChair(otherChair);
            try {
                otherScore.money = otherPlayer.gameMoneyInfo.chargeMoneyInGame(otherScore, this.room.getId(), this.gameMgr.game.id);
                this.dispatchAddEventScore(otherPlayer.getUser(), otherScore);
            }
            catch (MoneyException e) {
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + otherPlayer.gameMoneyInfo.toString()));
                otherPlayer.reqQuitRoom = true;
                otherScore.money = 0L;
            }
            long moneyWin = -otherScore.money;
            GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
            long wastedMoney = (long)((double)(moneyWin * (long)this.room.setting.commisionRate) / 100.0);
            score.money = moneyWin -= wastedMoney;
            score.winCount = 1;
            score.wastedMoney = wastedMoney;
            try {
                score.money = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                this.dispatchAddEventScore(gp.getUser(), score);
            }
            catch (MoneyException e) {
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
        }
        SendEndGame msg = new SendEndGame();
        msg.result = (byte)res.result.ordinal();
        msg.countdown = this.gameMgr.countDown;
        msg.winChair = (byte)this.gameMgr.currentChair;
        msg.moneyWin = score.money;
        msg.moneyLost = -this.getMoneyBet();
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            msg.moneyArray[i] = gp.gameMoneyInfo.currentMoney;
        }
        if (res.listX != null && res.listY != null) {
            int size = res.listX.size();
            msg.listX = new byte[size];
            msg.listY = new byte[size];
            for (int i = 0; i < size; ++i) {
                msg.listX[i] = res.listX.get(i);
                msg.listY[i] = res.listY.get(i);
            }
        }
        this.send(msg);
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
        for (i = 0; i < 2; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 0) {
                if (GameUtils.isMainTain) {
                    gp.reqQuitRoom = true;
                    this.notifyKickRoom(gp, (byte)2);
                }
                if (!this.coTheChoiTiep(gp)) {
                    if (!gp.checkMoneyCanPlay()) {
                        this.notifyKickRoom(gp, (byte)1);
                    }
                    if (gp.getUser() != null && this.room != null) {
                        GameRoom gameRoom = (GameRoom)gp.getUser().getProperty((Object)"GAME_ROOM");
                        if (gameRoom == this.room) {
                            GameRoomManager.instance().leaveRoom(gp.getUser());
                        }
                    } else {
                        this.removePlayerAtChair(i, false);
                    }
                    msg.hasInfoAtChair[i] = false;
                } else {
                    msg.hasInfoAtChair[i] = true;
                    msg.pInfos[i] = gp;
                }
                gp.setPlayerStatus(2);
            } else {
                msg.hasInfoAtChair[i] = false;
            }
            gp.prepareNewGame();
        }
        for (i = 0; i < 2; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
    }

    public void logEndGame() {
        this.gameLog.append("EG<");
        int[][] map = this.gameMgr.game.table.map;
        this.gameLog.append(this.gameMgr.currentChair).append(";").append(this.getMoneyBet()).append(";");
        for (int i = 0; i < 15; ++i) {
            for (int j = 0; j < 15; ++j) {
                this.gameLog.append(map[i][j]).append(",");
            }
            this.gameLog.append("|");
        }
        this.gameLog.append(">");
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    public synchronized void tudongChoi() {
        CaroCell p = this.gameMgr.game.table.findFreePosition();
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        this.takeTurn(gp, p.x, p.y);
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
                for (int i = 0; i < 2; ++i) {
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
        for (int i = 0; i < 2; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() != null && !gp.getUser().getName().equalsIgnoreCase(nickName)) continue;
        }
    }

    public synchronized void changeTurn() {
        this.gameMgr.gameAction = 0;
        this.gameMgr.countDown = 15;
        SendChangeTurn msg = new SendChangeTurn();
        msg.curentChair = this.gameMgr.currentChair;
        msg.countDownTime = this.gameMgr.countDown;
        this.send(msg);
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
            for (int i = 0; i < 2; ++i) {
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
                CaroGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

