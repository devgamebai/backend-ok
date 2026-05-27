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
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.eventHandlers.GameEventParam
 *  game.eventHandlers.GameEventType
 *  game.modules.gameRoom.entities.GameRoom
 *  game.modules.gameRoom.entities.GameRoomManager
 *  game.modules.gameRoom.entities.GameRoomSetting
 *  game.modules.gameRoom.entities.GameServer
 *  game.modules.gameRoom.entities.ThongTinThangLon
 *  game.modules.tour.control.RoomTourGroup
 *  game.modules.tour.control.Tour
 *  game.modules.tour.control.TourManager
 *  game.modules.tour.control.TourUserInfo
 *  game.modules.tour.control.cmd.send.SendCountDownToPlay
 *  game.utils.GameUtils
 *  game.utils.LoggerUtils
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.tour.server;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEvent;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.modules.tour.control.Tour;
import game.modules.tour.control.TourManager;
import game.modules.tour.control.TourUserInfo;
import game.modules.tour.control.cmd.send.SendCountDownToPlay;
import game.tour.server.cmd.receive.RevCheatCard;
import game.tour.server.cmd.receive.RevTakeTurn;
import game.tour.server.cmd.send.SendChangeTurn;
import game.tour.server.cmd.send.SendDealPrivateCard;
import game.tour.server.cmd.send.SendEndGame;
import game.tour.server.cmd.send.SendGameInfo;
import game.tour.server.cmd.send.SendJoinRoomSuccess;
import game.tour.server.cmd.send.SendKickRoom;
import game.tour.server.cmd.send.SendNewRound;
import game.tour.server.cmd.send.SendNewUserJoin;
import game.tour.server.cmd.send.SendNotifyReqQuitRoom;
import game.tour.server.cmd.send.SendSelectDealer;
import game.tour.server.cmd.send.SendShowCard;
import game.tour.server.cmd.send.SendTakeTurn;
import game.tour.server.cmd.send.SendUpdateMatch;
import game.tour.server.cmd.send.SendUserExitRoom;
import game.tour.server.logic.Card;
import game.tour.server.logic.GroupCard;
import game.tour.server.logic.PokerGameInfo;
import game.tour.server.logic.PokerPlayerInfo;
import game.tour.server.logic.PokerRank;
import game.tour.server.logic.PokerResult;
import game.tour.server.logic.PokerRule;
import game.tour.server.logic.Round;
import game.tour.server.logic.Turn;
import game.tour.server.logic.ai.BotAI;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class PokerTourGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    public ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int PHONG_CO_KHOA = 1;
    public static final int PHONG_KHONG_CO_KHOA = 2;
    public static final String USER_CHAIR = "user_chair";
    public final GameManager gameMgr = new GameManager();
    public final Vector<GamePlayer> playerList = new Vector(6);
    public int playingCount = 0;
    public int winType;
    public volatile int serverState = 0;
    public volatile int groupIndex;
    public volatile int playerCount;
    StringBuilder gameLog = new StringBuilder();
    public final Runnable gameLoopTask = new GameLoopTask();
    public long totalChip = 0L;
    public long totalMoney = 0L;
    public List<Turn> cheatTurns = new LinkedList<Turn>();
    public int turnIndex = 0;

    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        boolean x = false;
        switch (dataCmd.getId()) {
            case 3111: {
                this.pOutRoom(user, dataCmd);
                break;
            }
            case 3101: {
                this.registerTakeTurn(user, dataCmd);
                break;
            }
            case 3115: {
                this.pCheatCards(user, dataCmd);
                break;
            }
            case 3116: {
                this.pDangKyChoiTiep(user, dataCmd);
                break;
            }
            case 3108: {
                this.pShowCard(user, dataCmd);
            }
        }
    }

    public void pOutRoom(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        this.pOutRoom(gp);
    }

    public void pOutRoom(GamePlayer gp) {
        if (gp != null) {
            if (gp.isPlaying()) {
                gp.reqQuitRoom = !gp.reqQuitRoom;
                this.notifyRegisterOutRoom(gp);
            } else {
                GameRoomManager.instance().leaveRoom(gp.getUser(), this.room);
                LoggerUtils.debug((String)"tour", (Object[])new Object[]{"pOutRoom", gp.getUser().getName(), this.room.getId()});
                this.notifyKickRoom(gp.getUser(), (byte)4);
            }
        }
    }

    private void notifyRegisterOutRoom(GamePlayer gp) {
        SendNotifyReqQuitRoom msg = new SendNotifyReqQuitRoom();
        msg.chair = (byte)gp.chair;
        msg.reqQuitRoom = gp.reqQuitRoom;
        this.send(msg);
    }

    public void pShowCard(User user, DataCmd dataCmd) {
        SendShowCard msg = new SendShowCard();
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && gp.isPlaying() && this.gameMgr.gameState == 3 && !gp.spInfo.pokerInfo.fold) {
            msg.chair = (byte)gp.chair;
            this.send(msg);
        }
    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 6) {
            GamePlayer gp = new GamePlayer();
            gp.gameServer = this;
            gp.spInfo.pokerInfo = new PokerPlayerInfo(gp);
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
        if (i >= 0 && i < 6) {
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
        return chair >= 0 && chair < 6;
    }

    public synchronized void onGameUserExit(User user) {
        Tour tour;
        TourUserInfo info;
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp == null) {
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserExit", "chair null", user.getName(), "room", this.room.getId()});
            return;
        }
        if (gp.isPlaying()) {
            gp.reqQuitRoom = true;
            this.gameLog.append("DIS<").append(gp.chair).append(">");
        } else {
            boolean disconnect = user.isConnected();
            this.removePlayerAtChair(gp.chair, !disconnect);
        }
        if (this.room.userManager.size() == 0) {
            this.resetPlayDisconnect();
        }
        if ((info = (tour = (Tour)this.room.getProperty("TOUR_INFO")).findTourInfoByUser(user)) != null) {
            info.roomId = 0;
            info.outRoomTimeStamp = System.currentTimeMillis();
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null) continue;
            gp.pInfo.setIsHold(false);
        }
    }

    public void onGameUserDis(User user) {
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserDis", user.getName(), "room", this.room.getId()});
    }

    public synchronized void onGameUserReturn(User user) {
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserReturn", user.getName(), "room", this.room.getId()});
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (gp.getPlayerStatus() == 0 || gp.pInfo == null || !gp.pInfo.nickName.equalsIgnoreCase(user.getName())) continue;
            this.gameLog.append("RE<").append(i).append(">");
            gp.user = user;
            gp.pInfo = PlayerInfo.getInfo((User)user);
            gp.reqQuitRoom = false;
            this.sendGameInfo(gp.chair);
            return;
        }
        this.onGameUserEnter(user);
    }

    public synchronized void onGameUserEnter(User user) {
        GamePlayer gp;
        int i;
        this.init();
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserEnter", user.getName(), "room", this.room.getId()});
        PlayerInfo pInfo = PlayerInfo.getInfo((User)user);
        if (pInfo == null) {
            return;
        }
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserEnter pInfo OK", user.getName(), "room", this.room.getId()});
        for (i = 0; i < 6; ++i) {
            gp = this.playerList.get(i);
            if (gp.getPlayerStatus() == 0 || gp.pInfo == null || !gp.pInfo.nickName.equalsIgnoreCase(user.getName())) continue;
            this.onGameUserReturn(user);
            return;
        }
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserEnter", " not return", pInfo.nickName, "room", this.room.getId()});
        for (i = 0; i < 6; ++i) {
            TourUserInfo info;
            gp = this.playerList.get(i);
            if (gp.getPlayerStatus() != 0) continue;
            if (this.serverState == 0) {
                gp.setPlayerStatus(2);
            } else {
                gp.setPlayerStatus(1);
            }
            Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
            tour.notifyTourInfo();
            gp.tourUserInfo = info = tour.findTourInfoByUser(user);
            gp.takeChair(user, pInfo);
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"onGameUserEnter", user.getName(), "at chair", gp.chair, "room", this.room.getId()});
            ++this.playerCount;
            if (this.playerCount == 1) {
                this.gameMgr.roomCreatorUserId = user.getId();
                this.gameMgr.roomOwnerChair = i;
            }
            this.notifyUserEnter(gp);
            this.kiemTraTuDongBatDau(2);
            return;
        }
        GameRoomManager.instance().leaveRoom(user, this.room);
    }

    public int getNumTotalPlayer() {
        return this.playerCount;
    }

    public void sendMsgToPlayingUser(BaseMsg msg) {
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.send(msg, gp.getUser());
        }
    }

    public void send(BaseMsg msg) {
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
        }
    }

    public void chiabai() {
        this.gameLog.append("CB<");
        SendDealPrivateCard msg = new SendDealPrivateCard();
        msg.gameId = this.gameMgr.game.id;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            msg.card_name = gp.spInfo.handCards.kiemtraBo();
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
            this.gameLog.append(gp.spInfo.handCards.kiemtraBo()).append(";");
            this.send((BaseMsg)msg, user);
        }
        this.gameLog.append(">");
        this.gameLog.append("PC<");
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        this.gameLog.append(gameInfo.publicCard);
        this.gameLog.append(">");
    }

    public synchronized void start() {
        if (this.serverState == 1) {
            return;
        }
        this.serverState = 1;
        this.gameMgr.isAutoStart = false;
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            gp.setPlayerStatus(3);
            gp.tourUserInfo.roomId = this.room.getId();
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)gp.pInfo.nickName, (int)this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(i).append(";");
            gp.choiTiepVanSau = false;
            if (this.gameMgr.game.previousId > 0 && gp.lastGameId != this.gameMgr.game.previousId) {
                gp.requireBigBlind = false;
            }
            gp.lastGameId = this.gameMgr.game.id;
            gp.tourUserInfo.lastChip = gp.tourUserInfo.chip;
        }
        this.gameLog.append(this.room.setting.moneyType).append(";");
        this.gameLog.append(">");
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"start", this.gameLog, "room", this.room.getId(), "game", this.gameMgr.game.id});
        this.clearInfoNewGame();
        this.gameMgr.gameAction = 0;
        this.gameMgr.countDown = 0;
        this.logStartGame();
    }

    private void clearInfoNewGame() {
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        gameInfo.clearNewGame();
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            PokerPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            pokerInfo.clearNewGame();
        }
    }

    public void logStartGame() {
        this.totalChip = 0L;
        this.totalMoney = 0L;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            PokerPlayerInfo playerInfo = gp.spInfo.pokerInfo;
            this.totalMoney += playerInfo.currentMoney;
            TourUserInfo tourUserInfo = gp.tourUserInfo;
            this.totalChip += tourUserInfo.chip;
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"logStartGame", gp.getUser().getName(), "gameId", this.gameMgr.game.id, "money:", gp.spInfo.pokerInfo.currentMoney, "chip:", gp.tourUserInfo.chip});
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public int demSoNguoiChoiTiep() {
        int count = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (this.coTheChoiTiep(gp)) {
                ++count;
                continue;
            }
            if (gp.getUser() == null) continue;
            this.pOutRoom(gp);
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            ++count;
        }
        return count;
    }

    public synchronized void kiemTraGhepPhong() {
        if (this.gameMgr.gameState == 0) {
            int count = this.demSoNguoiChoiTiep();
            boolean ghepPhong = false;
            Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
            if (count > 0 && count <= 5) {
                ghepPhong = tour.roomGroup.combineRoom(this.room);
            }
        }
    }

    public synchronized void kiemTraTuDongBatDau(int after) {
        Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
        if (!tour.canPlayGame) {
            SendCountDownToPlay msg = new SendCountDownToPlay();
            msg.coundDown = tour.countDownToStart;
            this.send((BaseMsg)msg);
            return;
        }
        if (this.gameMgr.gameState == 0) {
            int count = this.demSoNguoiChoiTiep();
            if (count < 2) {
                this.gameMgr.cancelAutoStart();
            } else {
                this.gameMgr.makeAutoStart(after);
            }
        }
    }

    public boolean coTheChoiTiep(GamePlayer gp) {
        return gp.spInfo.pokerInfo.currentMoney > 0L;
    }

    public boolean coTheOLaiBan(GamePlayer gp) {
        return !gp.reqQuitRoom && this.coTheChoiTiep(gp);
    }

    public synchronized void removePlayerAtChair(int chair, boolean disconnect) {
        GamePlayer gp = this.playerList.get(chair);
        gp.standUp = false;
        gp.choiTiepVanSau = true;
        gp.countToOutRoom = 0;
        gp.lastGameId = -1;
        this.notifyUserExit(gp, disconnect);
        gp.spInfo.pokerInfo.clearOutGame();
        gp.user = null;
        gp.pInfo = null;
        gp.tourUserInfo = null;
        gp.setPlayerStatus(0);
        --this.playerCount;
        this.kiemTraTuDongBatDau(2);
    }

    public void notifyUserEnter(GamePlayer gamePlayer) {
        User user = gamePlayer.getUser();
        if (user == null) {
            return;
        }
        gamePlayer.timeJoinRoom = System.currentTimeMillis();
        SendNewUserJoin msg = new SendNewUserJoin();
        msg.money = gamePlayer.spInfo.pokerInfo.currentMoney;
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
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
            msg.playerList[i] = gp.getPlayerInfo();
            msg.moneyInfoList[i] = gp.spInfo.pokerInfo;
            if (gp.getUser() == null || gp.spInfo.handCards == null) continue;
            msg.handCardSize[i] = 2;
        }
        msg.gameAction = (byte)this.gameMgr.gameState;
        msg.gameAction = (byte)this.gameMgr.gameAction;
        msg.curentChair = (byte)this.gameMgr.currentChair();
        msg.countDownTime = (byte)this.gameMgr.countDown;
        Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
        if (tour != null) {
            msg.tourId = tour.tourId;
        }
        this.send((BaseMsg)msg, gamePlayer.getUser());
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
        if (user == null) {
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"user null"});
            return null;
        }
        for (int i = 0; i < this.playerList.size(); ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null || !gp.getUser().getName().equalsIgnoreCase(user.getName())) continue;
            return gp;
        }
        return null;
    }

    public void sendGameInfo(int chair) {
        GamePlayer me = this.getPlayerByChair(chair);
        if (me != null) {
            SendGameInfo msg = new SendGameInfo();
            msg.pokerGameInfo = this.gameMgr.game.pokerGameInfo;
            msg.currentChair = this.gameMgr.currentChair;
            msg.gameState = this.gameMgr.gameState;
            msg.gameAction = this.gameMgr.gameAction;
            msg.countdownTime = this.gameMgr.countDown;
            msg.maxUserPerRoom = this.room.setting.maxUserPerRoom;
            msg.moneyType = this.room.setting.moneyType;
            msg.roomBet = this.room.setting.moneyBet;
            msg.gameId = this.gameMgr.game.id;
            msg.roomId = this.room.getId();
            Round round = this.gameMgr.game.getLastRound();
            if (round != null) {
                msg.roundId = round.roundId;
            }
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                msg.hasInfoAtChair[i] = gp.hasUser();
                msg.pInfos[i] = gp;
            }
            msg.initPrivateInfo(me);
            Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
            if (tour != null) {
                msg.tourId = tour.tourId;
            }
            this.send((BaseMsg)msg, me.getUser());
        }
    }

    public void registerTakeTurn(User user, DataCmd dataCmd) {
        RevTakeTurn cmd = new RevTakeTurn(dataCmd);
        this.registerTakeTurn(user, cmd);
    }

    public synchronized boolean registerTakeTurn(User user, RevTakeTurn cmd) {
        if (this.gameMgr.gameState != 1) {
            return false;
        }
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && this.gameMgr.gameAction == 4 && gp.chair == this.gameMgr.currentChair && this.gameMgr.countDown >= 1) {
            PokerPlayerInfo info = gp.spInfo.pokerInfo;
            PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
            boolean res = false;
            res = info.register(cmd, info, gameInfo);
            if (res) {
                gp.countToOutRoom = 0;
                this.takeTurn();
                return true;
            }
            return false;
        }
        return false;
    }

    public void endGame() {
        this.gameMgr.gameState = 3;
        this.gameMgr.countDown = (int)(10.0 + Math.ceil(1.5 * (double)this.playingCount));
        this.gomTienVaoHu();
        this.sortUserRanking();
        this.tinhTraTien();
        this.traTien();
        this.notifyEndGame();
        Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.hasUser() || gp.tourUserInfo == null || gp.tourUserInfo == null) continue;
            TourManager.instance().updatePlayerTour(tour, gp.tourUserInfo);
        }
    }

    public void notifyEndGame() {
        int i;
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        PokerResult result = gameInfo.resultPoker;
        List<PokerRank> listRank = result.ranking;
        SendEndGame msg = new SendEndGame();
        msg.moneyPot = gameInfo.potMoney;
        msg.publicCard = gameInfo.publicCard.toByteArray();
        for (i = 0; i < listRank.size(); ++i) {
            PokerRank rank = listRank.get(i);
            msg.ketQuaTinhTien[rank.chair] = rank.finalMoney;
            msg.winLost[rank.chair] = rank.win;
            msg.rank[rank.chair] = rank.fold ? 7L : (long)rank.rank;
            msg.maxCards[rank.chair] = rank.cards.toByteArray();
            msg.groupCardName[rank.chair] = (byte)rank.cards.kiemtraBo();
        }
        for (i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
                PokerPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
                msg.hasInfoAtChair[gp.chair] = pokerInfo.getStatus();
                msg.moneyArray[gp.chair] = pokerInfo.currentMoney;
                msg.balanceMoney[gp.chair] = pokerInfo.currentMoney;
                msg.cards[gp.chair] = !pokerInfo.fold ? gp.spInfo.handCards.toByteArray() : new byte[0];
                if (msg.ketQuaTinhTien[i] != 0L) continue;
            }
            msg.hasInfoAtChair[gp.chair] = 0;
        }
        msg.countdown = this.gameMgr.countDown;
        this.send(msg);
        this.logKetQua(msg);
    }

    public void logKetQua(SendEndGame msg) {
        this.gameLog.append("KT<");
        this.gameLog.append(0).append(";");
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.gameLog.append(gp.chair).append("/").append(msg.ketQuaTinhTien[gp.chair]).append("/").append(gp.spInfo.maxCards).append(";");
        }
        this.gameLog.append(">");
        this.logEndGame();
    }

    public void traTien() {
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        PokerResult result = gameInfo.resultPoker;
        List<PokerRank> listRank = result.ranking;
        for (int i = 0; i < listRank.size(); ++i) {
            PokerRank rank = listRank.get(i);
            GamePlayer gp = this.getPlayerByChair(rank.chair);
            PokerPlayerInfo info = gp.spInfo.pokerInfo;
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"tra tien gameId:", this.gameMgr.game.id, "user", gp.getUser().getName(), GameUtils.toJSONObject((Object)rank)});
            rank.finalMoney = rank.totalMoneyWin;
            boolean bl = rank.win = rank.finalMoney > info.totalBet;
            if (rank.finalMoney <= 0L) continue;
            UserScore score = new UserScore();
            score.money = rank.finalMoney;
            if (score.money > info.totalBet) {
                score.wastedMoney = 0L;
                score.money -= score.wastedMoney;
                score.winCount = 1;
            } else {
                score.lostCount = 1;
            }
            gp.spInfo.pokerInfo.currentMoney += rank.finalMoney;
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"traTien", gp.getUser().getName(), "gameId", this.gameMgr.game.id, "money:", gp.spInfo.pokerInfo.currentMoney, "chip:", gp.tourUserInfo.chip});
        }
        this.capNhatLaiChip();
    }

    private void capNhatLaiChip() {
        long totalCurrentChip = 0L;
        long totalCurrentMoney = 0L;
        for (int i = 0; i < this.room.setting.limitPlayer; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp == null || !gp.isPlaying() || gp.spInfo.pokerInfo == null || gp.tourUserInfo == null) continue;
            totalCurrentChip += gp.tourUserInfo.chip;
            totalCurrentMoney += gp.spInfo.pokerInfo.currentMoney;
            gp.tourUserInfo.chip = gp.spInfo.pokerInfo.currentMoney;
        }
        if (totalCurrentChip != totalCurrentMoney || this.totalChip != this.totalMoney || this.totalChip != totalCurrentChip) {
            LoggerUtils.error((String)"tour", (Object[])new Object[]{"ERROR CHIP MONEY: room", this.room.getId(), "game", this.gameMgr.game.id});
        }
    }

    public void tinhTraTien() {
        int i;
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        PokerResult result = gameInfo.resultPoker;
        List<PokerRank> listRank = result.ranking;
        int index = 0;
        int size = listRank.size();
        LinkedList<PokerRank> winList = new LinkedList<PokerRank>();
        LinkedList<PokerRank> lostList = new LinkedList<PokerRank>();
        for (i = 0; i < listRank.size(); ++i) {
            PokerRank pokerRank = listRank.get(i);
        }
        while (index < size) {
            try {
                winList.clear();
                lostList.clear();
                PokerRank myRank = listRank.get(index);
                for (int i2 = index; i2 < size; ++i2) {
                    PokerRank otherRank = listRank.get(i2);
                    if (myRank.rank < otherRank.rank || otherRank.fold) {
                        lostList.add(otherRank);
                        continue;
                    }
                    winList.add(otherRank);
                }
                long winUnit = myRank.totalBet;
                int winSize = winList.size();
                long totalLost = 0L;
                for (int i3 = index; i3 < listRank.size(); ++i3) {
                    PokerRank rank = listRank.get(i3);
                    long lostMoney = rank.totalBet;
                    if (rank.totalBet > winUnit) {
                        lostMoney = winUnit;
                    }
                    rank.totalBet -= lostMoney;
                    totalLost += lostMoney;
                }
                long winShare = totalLost;
                if (winSize != 0) {
                    winShare = (long)Math.floor((double)totalLost * 1.0 / (double)winSize);
                    for (int i4 = 0; i4 < winList.size(); ++i4) {
                        PokerRank winRank = (PokerRank)winList.get(i4);
                        winRank.totalMoneyWin += winShare;
                    }
                }
                ++index;
            }
            catch (Exception e) {
                CommonHandle.writeErrLog((Throwable)e);
                CommonHandle.writeErrLog((String)this.gameLog.toString());
                break;
            }
        }
        for (i = 0; i < listRank.size(); ++i) {
            PokerRank winUnit = listRank.get(i);
        }
    }

    public void dispatchAddEventScore(User user, UserScore score) {
        if (user == null) {
            return;
        }
        score.moneyType = this.room.setting.moneyType;
        UserScore newScore = score.clone();
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.USER, (Object)user);
        evtParams.put(GameEventParam.USER_SCORE, (Object)newScore);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.EVENT_ADD_SCORE, evtParams));
    }

    public boolean checkJackpot(GroupCard gc) {
        if (gc.kiemtraBo() == 0) {
            return true;
        }
        if (gc.kiemtraBo() == 1) {
            for (int i = 0; i < gc.cards.size(); ++i) {
                Card c = gc.cards.get(i);
                if (c.SO != 11) continue;
                return true;
            }
        }
        return false;
    }

    public void sortUserRanking() {
        ArrayList<PokerRank> listRank = new ArrayList<PokerRank>();
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        PokerResult result = gameInfo.resultPoker;
        LinkedList<String> nickNames = new LinkedList<String>();
        String card = "";
        boolean addTime = false;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            PokerPlayerInfo playerInfo = gp.spInfo.pokerInfo;
            PokerRank rank = new PokerRank();
            rank.fold = playerInfo.fold;
            rank.chair = gp.chair;
            rank.cards = PokerRule.findMaxGroup(gp.spInfo.handCards, gameInfo.publicCard);
            if (!rank.fold && this.checkJackpot(rank.cards)) {
                nickNames.add(gp.getUser().getName());
                card = rank.cards.toString();
                addTime = true;
            }
            gp.spInfo.maxCards = rank.cards;
            rank.totalBet = playerInfo.totalBet;
            listRank.add(rank);
        }
        if (addTime) {
            this.gameMgr.countDown += 5;
        }
        if (nickNames.size() >= 1) {
            TourManager.instance().giveJackpot(nickNames, this.room, card, this.gameMgr.game.id);
        }
        Collections.sort(listRank);
        int rank = 1;
        PokerRank prev = null;
        for (int i = 0; i < listRank.size(); ++i) {
            PokerRank current = (PokerRank)listRank.get(i);
            if (prev == null || PokerRule.soSanhBoBai(prev.cards, current.cards) != 0) {
                // empty if block
            }
            prev = current;
            prev.rank = ++rank;
        }
        result.ranking = listRank;
    }

    public void notifyKickRoom(GamePlayer gp, byte reason) {
        this.notifyKickRoom(gp.getUser(), reason);
    }

    public void notifyKickRoom(User user, byte reason) {
        Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
        if (tour.tourState != 3 && tour.tourState != 4) {
            SendKickRoom msg = new SendKickRoom();
            msg.reason = reason;
            this.send((BaseMsg)msg, user);
        }
        HashMap<GameEventParam, User> evtParams = new HashMap<GameEventParam, User>();
        evtParams.put(GameEventParam.USER, user);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.OUT_TOUR, evtParams));
    }

    public boolean checkMoneyPlayer(GamePlayer gp) {
        gp.tourUserInfo.chip = gp.spInfo.pokerInfo.currentMoney;
        return gp.spInfo.pokerInfo.currentMoney > 0L;
    }

    public boolean isPlaying() {
        return this.serverState == 1;
    }

    public synchronized void pPrepareNewGame() {
        GamePlayer gp;
        int i;
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"pPrepareNewGame", "room", this.room.getId(), "game:", this.gameMgr.game.id});
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 0) {
                LoggerUtils.debug((String)"tour", (Object[])new Object[]{"pPrepareNewGame", gp.getUser().getName(), "gameId", this.gameMgr.game.id, "money:", gp.spInfo.pokerInfo.currentMoney, "chip:", gp.tourUserInfo.chip});
                gp.setPlayerStatus(2);
                if (GameUtils.isMainTain || !this.coTheOLaiBan(gp)) {
                    if (GameUtils.isMainTain) {
                        this.notifyKickRoom(gp, (byte)2);
                    } else {
                        this.notifyKickRoom(gp, (byte)4);
                    }
                    if (gp.getUser() != null && this.room != null) {
                        this.pOutRoom(gp);
                    } else {
                        this.removePlayerAtChair(i, false);
                    }
                    msg.hasInfoAtChair[i] = false;
                } else {
                    msg.hasInfoAtChair[i] = true;
                    msg.pInfos[i] = gp;
                }
            } else {
                msg.hasInfoAtChair[i] = false;
            }
            gp.prepareNewGame();
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.gameMgr.prepareNewGame();
        this.kiemTraGhepPhong();
        this.serverState = 0;
    }

    public void logEndGame() {
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    public synchronized void tudongChoi() {
        if (this.gameMgr.gameState != 1) {
            return;
        }
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (gp != null) {
            if (gp.getUser() != null && gp.getUser().isBot()) {
                this.botAutoPlay();
            } else {
                PokerPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
                pokerInfo.registerCheck = true;
            }
            this.changeTurn();
        }
    }

    public void botAutoPlay() {
        if (this.gameMgr.gameState != 1) {
            return;
        }
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (gp != null && gp.getUser() != null && gp.getUser().isBot() && gp.isPlaying()) {
            PokerGameInfo game = this.gameMgr.game.pokerGameInfo;
            Round round = this.gameMgr.game.getLastRound();
            BotAI.decideAction(game, gp, round);
            this.changeTurn();
        }
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
            this.gameMgr.game.suit.initCard();
        }
    }

    public void configGame(byte[] cards, long[] moneyArray, int dealer) {
        this.gameMgr.game.isCheat = true;
        this.gameMgr.game.suit.setOrder(cards);
        this.gameMgr.game.moneyArray = moneyArray;
        this.gameMgr.game.dealer = dealer;
    }

    public void pDangKyChoiTiep(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.choiTiepVanSau = true;
        }
    }

    public void choNoHu(String nickName) {
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null || !gp.getUser().getName().equalsIgnoreCase(nickName)) continue;
            this.gameMgr.game.suit.noHuAt(gp.chair);
        }
    }

    public void selectDealer() {
        SendSelectDealer msg = new SendSelectDealer();
        PokerGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        int from = (pokerGameInfo.dealer + 1) % 6;
        if (this.gameMgr.game.isCheat) {
            from = this.gameMgr.game.dealer < 0 ? from : this.gameMgr.game.dealer;
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (!gp.isPlaying()) continue;
                this.gameMgr.game.moneyArray[i] = gp.spInfo.pokerInfo.currentMoney = this.gameMgr.game.moneyArray[i] == 0L ? gp.spInfo.pokerInfo.currentMoney : this.gameMgr.game.moneyArray[i];
            }
        }
        int newDealer = -1;
        int newSmallBlind = -1;
        int newBigBlind = -1;
        int startChair = -1;
        int i = from;
        while (startChair == -1) {
            int currentChair = i % 6;
            GamePlayer gp = this.getPlayerByChair(currentChair);
            if (gp.isPlaying()) {
                if (newDealer == -1) {
                    newDealer = currentChair;
                } else if (newSmallBlind == -1) {
                    newSmallBlind = currentChair;
                } else if (newBigBlind == -1) {
                    newBigBlind = currentChair;
                } else if (startChair == -1) {
                    startChair = currentChair;
                }
            }
            ++i;
        }
        this.gameMgr.currentChair = startChair;
        pokerGameInfo.dealer = newDealer;
        pokerGameInfo.smallBlind = newSmallBlind;
        pokerGameInfo.bigBlind = newBigBlind;
        pokerGameInfo.bigBlindMoney = 2L * this.room.setting.moneyBet;
        pokerGameInfo.smallBlindMoney = 1L * this.room.setting.moneyBet;
        this.gameLog.append("SD<");
        this.gameLog.append("dl/").append(pokerGameInfo.dealer).append(";");
        this.gameLog.append("sb/").append(pokerGameInfo.smallBlind).append(";");
        this.gameLog.append("bb/").append(pokerGameInfo.bigBlind).append(">");
        this.initSmallAndBigBlind(msg);
        this.notifyDealder(msg);
    }

    public void initSmallAndBigBlind(SendSelectDealer msg) {
        PokerGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        GamePlayer sPlayer = this.getPlayerByChair(pokerGameInfo.smallBlind);
        PokerPlayerInfo smallBlindInfo = sPlayer.spInfo.pokerInfo;
        if (!sPlayer.requireBigBlind) {
            smallBlindInfo.registerBlindMoney(1L * this.room.setting.moneyBet);
        } else {
            smallBlindInfo.registerBlindMoney(2L * this.room.setting.moneyBet);
            sPlayer.requireBigBlind = false;
            msg.requireBigBlinds[sPlayer.chair] = true;
        }
        Round round = this.gameMgr.game.getCurrentRound();
        Turn turn = smallBlindInfo.take(pokerGameInfo, round, true);
        this.logTakeTurn(sPlayer, turn);
        GamePlayer bPlayer = this.getPlayerByChair(pokerGameInfo.bigBlind);
        PokerPlayerInfo bigBlindInfo = bPlayer.spInfo.pokerInfo;
        bigBlindInfo.registerBlindMoney(2L * this.room.setting.moneyBet);
        if (bPlayer.requireBigBlind) {
            bPlayer.requireBigBlind = false;
            msg.requireBigBlinds[bPlayer.chair] = true;
        }
        turn = bigBlindInfo.take(pokerGameInfo, round, true);
        this.logTakeTurn(bPlayer, turn);
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || !gp.requireBigBlind) continue;
            PokerPlayerInfo info = gp.spInfo.pokerInfo;
            info.registerMoney(2L * this.room.setting.moneyBet);
            gp.requireBigBlind = false;
            msg.requireBigBlinds[gp.chair] = true;
            turn = info.take(pokerGameInfo, round, true);
            ++round.requireBigBlindCount;
            this.logTakeTurn(gp, turn);
        }
    }

    public void notifyDealder(SendSelectDealer msg) {
        PokerGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        msg.smallBlind = pokerGameInfo.smallBlind;
        msg.bigBlind = pokerGameInfo.bigBlind;
        msg.dealer = pokerGameInfo.dealer;
        msg.gameId = this.gameMgr.game.id;
        if (this.gameMgr.game.isCheat) {
            msg.isCheat = true;
        }
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.hasUser()) continue;
            msg.hasInfoAtChair[i] = true;
            msg.gamePlayers[i] = gp;
            msg.moneyArray[i] = gp.spInfo.pokerInfo.currentMoney;
        }
        msg.smallBlindMoney = (int)pokerGameInfo.smallBlindMoney;
        this.send(msg);
        this.gameMgr.gameAction = 1;
        this.gameMgr.countDown = 3;
    }

    public void newRound() {
        Round round = this.gameMgr.game.getCurrentRound();
        if (round.roundId == 3) {
            this.endGame();
        } else {
            round = this.gameMgr.game.makeRound();
            if (round.roundId != 0) {
                this.gameMgr.currentChair = this.findFirstChairNewRound();
            }
            PokerGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
            this.gameLog.append("NR<");
            this.gameLog.append(round.roundId).append(";");
            this.gameLog.append(pokerGameInfo.getGroupCardPublic(round.roundId)).append(">");
            this.gomTienVaoHu();
            this.notifyNewRound(round);
        }
    }

    public int findFirstChairNewRound() {
        PokerGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        int from = pokerGameInfo.smallBlind;
        int startChair = -1;
        int i = from;
        while (startChair == -1) {
            int currentChair = i % 6;
            GamePlayer gp = this.getPlayerByChair(currentChair);
            if (gp.isPlaying()) {
                PokerPlayerInfo info = gp.spInfo.pokerInfo;
                if (startChair == -1 && !info.fold && !info.allIn) {
                    startChair = currentChair;
                    break;
                }
            }
            ++i;
        }
        return startChair;
    }

    public void gomTienVaoHu() {
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        long totalBet = 0L;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            PokerPlayerInfo info = gp.spInfo.pokerInfo;
            totalBet += info.moneyBet;
            info.totalBet += info.moneyBet;
            info.clearNewRound();
        }
        gameInfo.clearNewRound();
    }

    public void notifyNewRound(Round round) {
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.hasUser()) continue;
            SendNewRound msg = new SendNewRound();
            msg.potMoney = gameInfo.potMoney;
            msg.roundId = round.roundId;
            msg.cards = gameInfo.getPublicCard(round.roundId);
            byte[] publicCard = gameInfo.getCurrentPublicCard(round.roundId);
            GroupCard gc = PokerRule.findMaxGroup(gp.spInfo.handCards, new GroupCard(publicCard));
            msg.cards_name = (byte)gc.kiemtraBo();
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.gameMgr.gameAction = 5;
        this.gameMgr.countDown = 1;
    }

    public synchronized void changeTurn() {
        Turn turn = this.takeTurn();
        if (turn == null) {
            this.notifyChangeTurn();
        }
    }

    public void notifyChangeTurn() {
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        this.gameMgr.countDown = 10;
        this.gameMgr.gameAction = 4;
        SendChangeTurn msg = new SendChangeTurn();
        Round round = this.gameMgr.game.getCurrentRound();
        msg.roundId = round.roundId;
        msg.curentChair = this.gameMgr.currentChair;
        msg.countDownTime = this.gameMgr.countDown;
        this.send(msg);
        if (gp.getUser() != null && gp.getUser().isBot()) {
            int random = GameUtils.rd.nextInt(5);
            if (random < 5) {
                random = 5;
            }
            this.gameMgr.botCountDown = random;
        }
    }

    public synchronized Turn takeTurn() {
        PokerGameInfo potInfo = this.gameMgr.game.pokerGameInfo;
        Round round = this.gameMgr.game.getCurrentRound();
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        PokerPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"takeTurn 1 user", gp.getUser().getName(), "currentMoney", pokerInfo.currentMoney, "totalBet", pokerInfo.totalBet, "registerMoney", pokerInfo.registerMoney, "game", this.gameMgr.game.id});
        Turn turn = pokerInfo.takeTurn(potInfo, round);
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"takeTurn 2 user", gp.getUser().getName(), "currentMoney", pokerInfo.currentMoney, "totalBet", pokerInfo.totalBet, "registerMoney", pokerInfo.registerMoney, this.gameMgr.game.id});
        if (turn != null) {
            pokerInfo.clearNewTurn();
            this.notifyTakeTurn(turn);
            if (this.checkEndGameByFoldAndAllIn()) {
                this.endGame();
            } else if (!this.checkNewRound()) {
                this.gameMgr.gameAction = 5;
                this.gameMgr.countDown = 1;
            }
        }
        return turn;
    }

    public void notifyTakeTurn(Turn turn) {
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (turn.action == 0) {
            gp.spInfo.pokerInfo.fold = true;
        }
        PokerGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        PokerPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
        SendTakeTurn msg = new SendTakeTurn();
        msg.action = turn.action;
        msg.chair = this.gameMgr.currentChair;
        msg.currentBet = pokerInfo.moneyBet;
        msg.currentMoney = pokerInfo.currentMoney;
        msg.maxBet = gameInfo.maxBetMoney;
        msg.raiseAmount = turn.raiseAmount;
        msg.raiseStep = gameInfo.lastRaise > gameInfo.bigBlindMoney ? gameInfo.lastRaise : gameInfo.bigBlindMoney;
        msg.raiseBlock = gameInfo.raiseBlock;
        this.findNextChair();
        this.logTakeTurn(gp, turn);
        this.send(msg);
    }

    private void logTakeTurn(GamePlayer gp, Turn turn) {
        gp.spInfo.turns.add(turn);
        this.gameLog.append("TT<");
        this.gameLog.append(gp.chair).append(";");
        this.gameLog.append(turn.action).append(";-");
        this.gameLog.append(turn.raiseAmount);
        this.gameLog.append(">");
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"logTakeTurn", "user", gp.getUser().getName(), "action", turn.action, "mount", turn.raiseAmount, "game", this.gameMgr.game.id});
    }

    public boolean checkEndGameByFoldAndAllIn() {
        PokerGameInfo potInfo = this.gameMgr.game.pokerGameInfo;
        int foldCount = 0;
        int allInCount = 0;
        int followCount = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (gp.spInfo.pokerInfo.allIn) {
                ++allInCount;
                continue;
            }
            if (gp.spInfo.pokerInfo.fold) {
                ++foldCount;
                continue;
            }
            if (gp.spInfo.pokerInfo.moneyBet != potInfo.maxBetMoney) continue;
            ++followCount;
        }
        if (allInCount == this.playingCount - foldCount || allInCount == this.playingCount - foldCount - 1 && followCount == 1) {
            return true;
        }
        return foldCount == this.playingCount - 1;
    }

    public boolean checkNewRound() {
        Round round = this.gameMgr.game.getCurrentRound();
        PokerGameInfo potInfo = this.gameMgr.game.pokerGameInfo;
        int allInCount = 0;
        int foldCount = 0;
        int callCount = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            PokerPlayerInfo info = gp.spInfo.pokerInfo;
            if (info.fold) {
                ++foldCount;
                continue;
            }
            if (info.allIn) {
                ++allInCount;
                continue;
            }
            if (info.moneyBet != potInfo.maxBetMoney) continue;
            ++callCount;
        }
        boolean flag = false;
        int size = round.turns.size();
        if (round.roundId == 0) {
            flag = size >= this.playingCount + 2 + round.requireBigBlindCount;
        } else {
            boolean bl = flag = size >= this.playingCount - this.gameMgr.game.stopInRound;
        }
        if (flag && allInCount + foldCount + callCount == this.playingCount) {
            this.gameMgr.gameAction = 3;
            this.gameMgr.countDown = 1;
            this.gameMgr.game.stopInRound = foldCount + allInCount;
            return true;
        }
        return false;
    }

    public void findNextChair() {
        int from;
        for (int i = from = this.gameMgr.currentChair + 1; i < 6 + from; ++i) {
            int currentChair = i % 6;
            GamePlayer gp = this.getPlayerByChair(currentChair);
            if (!gp.isPlaying() || gp.spInfo.pokerInfo.fold || gp.spInfo.pokerInfo.allIn) continue;
            this.gameMgr.currentChair = currentChair;
            break;
        }
    }

    public int getTicket() {
        Tour tour = (Tour)this.room.getProperty("TOUR_INFO");
        if (tour != null) {
            return tour.ticket;
        }
        return 0;
    }

    public void onNoHu(ThongTinThangLon info) {
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
            this.task = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 1, 1, TimeUnit.SECONDS);
            this.isRegisterLoop = true;
        }
    }

    public synchronized int countPlayers() {
        return this.playerCount;
    }

    public void destroy() {
        if (this.isRegisterLoop) {
            this.task.cancel(false);
            this.isRegisterLoop = false;
        }
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
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                arr.put((Object)gp.toJSONObject());
            }
            if (this.gameMgr.game.pokerGameInfo.publicCard != null) {
                json.put("pokerGameInfo", (Object)this.gameMgr.game.pokerGameInfo.publicCard.toString());
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
                PokerTourGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

