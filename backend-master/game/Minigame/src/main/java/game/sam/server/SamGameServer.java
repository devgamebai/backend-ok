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
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.sam.server;

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
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.sam.server.GameLogic;
import game.sam.server.GameManager;
import game.sam.server.GamePlayer;
import game.sam.server.cmd.receive.RevCheatCard;
import game.sam.server.cmd.receive.RevDanhBai;
import game.sam.server.cmd.send.SendBaoSam;
import game.sam.server.cmd.send.SendBoluot;
import game.sam.server.cmd.send.SendChangeTurn;
import game.sam.server.cmd.send.SendChatChong;
import game.sam.server.cmd.send.SendDanhBai;
import game.sam.server.cmd.send.SendDealCard;
import game.sam.server.cmd.send.SendEndGame;
import game.sam.server.cmd.send.SendFirstTurnDecision;
import game.sam.server.cmd.send.SendGameInfo;
import game.sam.server.cmd.send.SendHuyBaoSam;
import game.sam.server.cmd.send.SendJoinRoomSuccess;
import game.sam.server.cmd.send.SendKickRoom;
import game.sam.server.cmd.send.SendNewUserJoin;
import game.sam.server.cmd.send.SendNotifyReqQuitRoom;
import game.sam.server.cmd.send.SendUpdateMatch;
import game.sam.server.cmd.send.SendUpdateOwnerRoom;
import game.sam.server.cmd.send.SendUserExitRoom;
import game.sam.server.logic.Card;
import game.sam.server.logic.CardSuit;
import game.sam.server.logic.Gamble;
import game.sam.server.logic.GroupCard;
import game.sam.server.logic.Round;
import game.sam.server.logic.Turn;
import game.sam.server.sPlayerInfo;
import game.utils.GameUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    private ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int gsResult = 2;
    public static final int KHONG_CHOI = 1;
    public static final int THANG_THONG_THUONG = 2;
    public static final int THANG_SAM = 3;
    public static final int THANG_CHAN_SAM = 4;
    public static final int THANG_BAT_TREO = 5;
    public static final int THANG_TRANG_SAM_DINH = 6;
    public static final int THANG_TRANG_TU_HEO = 7;
    public static final int THANG_TRANG_NAM_DOI = 8;
    public static final int THANG_TRANG_DONG_MAU = 9;
    public static final int THANG_DEN_BAO_MOT = 10;
    public static final int THUA_DEN_BAO_MOT = 11;
    public static final int KET_QUA_HOA = 12;
    public static final int THUA_THONG_THUONG = 13;
    public static final int THUA_TREO = 14;
    public static final int THUA_TOI_TRANG = 15;
    public static final int THUA_DEN_CHAN_SAM = 16;
    public static final int PHONG_CO_KHOA = 1;
    public static final int PHONG_KHONG_CO_KHOA = 2;
    public static final String USER_CHAIR = "user_chair";
    private GameRoom room;
    private final GameManager gameMgr;
    public final Vector<GamePlayer> playerList = new Vector(5);
    public int lastWinId = -1;
    public int winChair = -1;
    public int samChair = -1;
    public int playingCount = 0;
    public int winType;
    private volatile int serverState = 0;
    public volatile int groupIndex;
    public volatile int playerCount;
    StringBuilder gameLog = new StringBuilder();
    private final Logger logger = LoggerFactory.getLogger((String)"SamLogic");
    private final Runnable gameLoopTask = new GameLoopTask();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\ngameState: ").append(this.serverState).append(" ").append(this.gameMgr.gameAction);
        for (int i = 0; i < 5; ++i) {
            sb.append(this.getPlayerByChair(i).toString());
        }
        sb.append(this.gameLog.toString()).append("\n");
        return sb.toString();
    }

    @Override
    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        this.logger.info("onGameMessage: ", dataCmd.getId(), user.getName());
        switch (dataCmd.getId()) {
            case 3109: {
                this.baoSam(user, dataCmd);
                break;
            }
            case 3111: {
                this.pOutRoom(user, dataCmd);
                break;
            }
            case 3101: {
                this.pDanhBai(user, dataCmd);
                break;
            }
            case 3102: {
                this.pBatDau(user, dataCmd);
                break;
            }
            case 3114: {
                this.pHuyBaoSam(user, dataCmd);
                break;
            }
            case 3115: {
                this.pCheatCards(user, dataCmd);
                break;
            }
            case 3116: {
                this.pDangKyChoiTiep(user, dataCmd);
            }
        }
    }

    public SamGameServer(GameRoom ro) {
        this.room = ro;
        this.gameMgr = new GameManager();
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 5) {
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
        if (i >= 0 && i < 5) {
            return this.playerList.get(i);
        }
        return null;
    }

    public long getMoneyBet() {
        return this.gameMgr.gameServer.room.setting.moneyBet;
    }

    public int isNeedRandomFirstTurn() {
        if (this.lastWinId > 0) {
            for (int i = 0; i < 5; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (!gp.isPlaying() || gp.getUser() == null || gp.getUser().getId() != this.lastWinId) continue;
                this.gameMgr.logic.firstTurn = i;
                return i;
            }
            return -1;
        }
        return -1;
    }

    public byte getPlayerCount() {
        return (byte)this.playerCount;
    }

    public boolean checkPlayerChair(int chair) {
        return chair >= 0 && chair < 5;
    }

    @Override
    public synchronized void onGameUserExit(User user) {
        Integer chair = (Integer)user.getProperty(USER_CHAIR);
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
            ++gp.tuDongChoiNhanh;
            this.gameLog.append("DIS<").append(chair).append(">");
        } else {
            boolean disconnect;
            this.removePlayerAtChair(chair, !(disconnect = user.isConnected()));
        }
        if (this.room.userManager.size() == 1) {
            this.lastWinId = -1;
        }
        if (this.room.userManager.size() == 0) {
            this.resetPlayDisconnect();
            this.destroy();
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null) continue;
            gp.pInfo.setIsHold(false);
        }
    }

    @Override
    public synchronized void onGameUserReturn(User user) {
        if (user == null) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (gp.getPlayerStatus() == 0 || gp.pInfo == null || gp.pInfo.userId != user.getId()) continue;
            this.gameLog.append("RE<").append(i).append(">");
            gp.reqQuitRoom = false;
            gp.user = user;
            user.setProperty("GAME_MONEY_INFO", gp.gameMoneyInfo);
            gp.user.setProperty(USER_CHAIR, gp.chair);
            this.sendGameInfo(gp.chair);
            break;
        }
    }

    @Override
    public synchronized void onGameUserEnter(User user) {
        int i;
        GamePlayer gp;
        if (user == null) {
            return;
        }
        PlayerInfo pInfo = PlayerInfo.getInfo(user);
        if (pInfo == null) {
            return;
        }
        GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty("GAME_MONEY_INFO");
        if (moneyInfo == null) {
            return;
        }
        if (this.room.setting.maxUserPerRoom == 5) {
            for (i = 0; i < 5; ++i) {
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
        if (this.room.setting.maxUserPerRoom == 2) {
            for (i = 0; i < 5; ++i) {
                if (i != 0 && i != 1 || (gp = this.playerList.get(i)).getPlayerStatus() != 0) continue;
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

    public void updateOwnerRoom(int chair) {
        Debug.trace((Object[])new Object[]{"UpdateOwnerRoom:", chair});
        SendUpdateOwnerRoom msg = new SendUpdateOwnerRoom();
        msg.ownerChair = chair;
        this.sendMsg(msg);
    }

    public int getNumTotalPlayer() {
        return this.playerCount;
    }

    public void sendMsgToPlayingUser(BaseMsg msg) {
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.sendMsg(msg, gp.getUser());
        }
    }

    public void sendMsg(BaseMsg msg) {
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
        }
    }

    public void chiabai() {
        this.gameLog.append("CB<");
        SendDealCard msg = new SendDealCard();
        msg.gameId = this.gameMgr.game.id;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            msg.toitrang = gp.spInfo.handCards.kiemTraToiTrang();
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
            this.gameLog.append(msg.toitrang).append(";");
            this.sendMsg(msg, user);
        }
        this.gameLog.append(">");
    }

    public void start() {
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        this.serverState = 1;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            gp.tuDongChoiNhanh = 0;
            if (!this.coTheChoiTiep(gp)) continue;
            gp.setPlayerStatus(3);
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId(gp.pInfo.userId, this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(i).append(";");
            gp.choiTiepVanSau = false;
        }
        this.gameLog.append(">");
        this.gameMgr.gameAction = 0;
        this.gameMgr.countDown = 0;
    }

    public byte getBaoSam() {
        int firstTurn;
        for (int i = firstTurn = this.gameMgr.logic.firstTurn; i < 5 + firstTurn; ++i) {
            int chair = (i + 5) % 5;
            GamePlayer gp = this.getPlayerByChair(chair);
            if (!gp.isPlaying() || !gp.baosam) continue;
            this.gameLog.append("CS<");
            this.gameMgr.game.baosam = true;
            this.samChair = chair;
            this.gameMgr.currentChair = chair;
            this.gameMgr.logic.firstTurn = chair;
            this.gameLog.append(chair).append(">");
            return (byte)chair;
        }
        this.gameMgr.game.baosam = false;
        return -1;
    }

    public byte getToiTrang() {
        int firstTurn;
        for (int i = firstTurn = this.gameMgr.logic.firstTurn; i < 5 + firstTurn; ++i) {
            int chair = (i + 5) % 5;
            GamePlayer gp = this.getPlayerByChair(chair);
            if (!gp.isPlaying() || gp.kiemTraToiTrang() <= 0) continue;
            this.gameMgr.currentChair = chair;
            this.gameMgr.logic.firstTurn = chair;
            return (byte)chair;
        }
        return -1;
    }

    public int demSoNguoiChoiTiep() {
        int count = 0;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            ++count;
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 5; ++i) {
            ++count;
        }
        return count;
    }

    public void kiemTraTuDongBatDau(int after) {
        if (this.gameMgr.gameState == 0) {
            if (this.demSoNguoiChoiTiep() < 2) {
                this.gameMgr.cancelAutoStart();
            } else {
                this.gameMgr.makeAutoStart(after);
            }
        }
    }

    private void kiemTraNguoiDiDau() {
        int from;
        for (int i = from = this.gameMgr.currentChair; i < from + 5; ++i) {
            int chair = i % 5;
            GamePlayer gp = this.getPlayerByChair(chair);
            if (!this.coTheChoiTiep(gp)) continue;
            this.gameMgr.currentChair = chair;
            this.gameMgr.roomOwnerChair = chair;
            break;
        }
    }

    private boolean coTheChoiTiep(GamePlayer gp) {
        return gp.user != null && gp.user.isConnected() && gp.canPlayNextGame();
    }

    private synchronized void removePlayerAtChair(int chair, boolean disconnect) {
        if (!this.checkPlayerChair(chair)) {
            Debug.trace((Object[])new Object[]{"removePlayerAtChair error", chair});
            return;
        }
        GamePlayer gp = this.playerList.get(chair);
        Debug.trace((Object[])new Object[]{"removePlayerAtChair", chair, "dis", disconnect, gp.pInfo.nickName});
        gp.choiTiepVanSau = true;
        this.notifyUserExit(gp, disconnect);
        if (gp.user != null) {
            gp.user.removeProperty(USER_CHAIR);
            gp.user.removeProperty("GAME_ROOM");
            gp.user.removeProperty("GAME_MONEY_INFO");
        }
        gp.user = null;
        gp.pInfo = null;
        gp.gameMoneyInfo.restoreMoney(this.room.getId());
        gp.gameMoneyInfo = null;
        gp.setPlayerStatus(0);
        --this.playerCount;
        if (chair == this.gameMgr.currentChair) {
            this.kiemTraNguoiDiDau();
            this.updateOwnerRoom(this.gameMgr.currentChair());
        }
        this.kiemTraTuDongBatDau(5);
    }

    private void notifyUserEnter(GamePlayer gamePlayer) {
        User user = gamePlayer.getUser();
        if (user == null) {
            return;
        }
        SendNewUserJoin msg = new SendNewUserJoin();
        msg.money = gamePlayer.gameMoneyInfo.currentMoney;
        msg.uStatus = gamePlayer.getPlayerStatus();
        msg.setBaseInfo(gamePlayer.pInfo);
        msg.uChair = gamePlayer.chair;
        this.sendMsgExceptMe(msg, user);
        this.notifyJoinRoomSuccess(gamePlayer);
    }

    public void notifyJoinRoomSuccess(GamePlayer gamePlayer) {
        SendJoinRoomSuccess msg = new SendJoinRoomSuccess();
        msg.uChair = gamePlayer.chair;
        msg.roomId = this.room.getId();
        msg.comission = this.gameMgr.gameServer.room.setting.commisionRate;
        msg.comissionJackpot = this.gameMgr.gameServer.room.setting.rule;
        msg.moneyType = this.gameMgr.gameServer.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.gameMgr.gameServer.room.setting.moneyBet;
        msg.roomOwner = (byte)this.gameMgr.roomOwnerChair;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
            msg.playerList[i] = gp.getPlayerInfo();
            msg.moneyInfoList[i] = gp.gameMoneyInfo;
            if (gp.getUser() == null || gp.spInfo.handCards == null) continue;
            msg.handCardSize[i] = (byte)gp.spInfo.handCards.cards.size();
        }
        msg.gameAction = (byte)this.gameMgr.gameAction;
        msg.curentChair = (byte)this.gameMgr.currentChair();
        msg.countDownTime = (byte)this.gameMgr.countDown;
        msg.lastCard = this.currentCardOnBoard();
        this.sendMsg(msg, gamePlayer.getUser());
    }

    private byte[] currentCardOnBoard() {
        Turn turn;
        Round round = this.gameMgr.game.getLastRound();
        if (round != null && (turn = round.getPrevTurn()) != null && turn.throwCard != null) {
            return turn.throwCard.toByteArray();
        }
        return new byte[0];
    }

    public void sendMsgExceptMe(BaseMsg msg, User user) {
        if (user != null && this.room != null && this.room != null) {
            for (Map.Entry<Integer, User> entry : this.room.userManager.entrySet()) {
                User curentUser = entry.getValue();
                if (curentUser == null || user.getId() == curentUser.getId()) continue;
                ExtensionUtility.getExtension().send(msg, curentUser);
            }
        }
    }

    public void sendMsg(BaseMsg cmd, User user) {
        if (user != null) {
            ExtensionUtility.getExtension().send(cmd, user);
        }
    }

    private void notifyUserExit(GamePlayer gamePlayer, boolean disconnect) {
        if (gamePlayer.pInfo != null) {
            Debug.trace((Object[])new Object[]{gamePlayer.pInfo.nickName, gamePlayer.chair, "exit room ", disconnect});
            gamePlayer.pInfo.setIsHold(false);
            SendUserExitRoom msg = new SendUserExitRoom();
            msg.nChair = (byte)gamePlayer.chair;
            msg.nickName = gamePlayer.pInfo.nickName;
            this.sendMsg(msg);
        } else {
            Debug.trace((Object[])new Object[]{gamePlayer.chair, "exit room playerInfo null"});
        }
    }

    public GamePlayer getPlayerByUser(User user) {
        Integer chair = (Integer)user.getProperty(USER_CHAIR);
        if (chair != null) {
            GamePlayer gp = this.getPlayerByChair(chair);
            if (gp != null && gp.pInfo != null && gp.pInfo.userId == user.getId()) {
                return gp;
            }
            Debug.trace((Object[])new Object[]{"getPlayerByUser NULL1", user.getName()});
            return null;
        }
        Debug.trace((Object[])new Object[]{"getPlayerByUser NULL2", user.getName()});
        return null;
    }

    private void baoSam(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameState == 1 && this.gameMgr.gameAction == 2) {
            this.gameLog.append("BS<");
            GamePlayer gp = this.getPlayerByUser(user);
            gp.baosam = true;
            this.gameMgr.game.baosam = true;
            this.notifyBaoSam(gp);
            this.gameLog.append(gp.chair).append(">");
            for (int i = 0; i < 5; ++i) {
                GamePlayer gamePlayer = this.getPlayerByChair(i);
                if (!gamePlayer.dangChoBaoSam()) continue;
                return;
            }
            this.gameMgr.countDown = 0;
        }
    }

    private void pHuyBaoSam(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameState == 1 && this.gameMgr.gameAction == 2) {
            GamePlayer gp = this.getPlayerByUser(user);
            Debug.trace((Object[])new Object[]{user.getName(), " o ghe ", gp.chair, "huy bao sam"});
            gp.huyBaoSam = true;
            this.notifyHuyBaoSam(gp);
            for (int i = 0; i < 5; ++i) {
                GamePlayer gamePlayer = this.getPlayerByChair(i);
                if (!gamePlayer.dangChoBaoSam()) continue;
                return;
            }
            this.gameMgr.countDown = 0;
        }
    }

    private void notifyBaoSam(GamePlayer gp) {
        SendBaoSam msg = new SendBaoSam();
        msg.chair = (byte)gp.chair;
        this.sendMsg(msg);
    }

    private void notifyHuyBaoSam(GamePlayer gp) {
        SendHuyBaoSam msg = new SendHuyBaoSam();
        msg.chair = (byte)gp.chair;
        this.sendMsg(msg);
    }

    private void sendGameInfo(int chair) {
        GamePlayer gamePlayer = this.getPlayerByChair(chair);
        SendGameInfo msg = new SendGameInfo();
        msg.gameState = this.gameMgr.gameState;
        msg.gameAction = this.gameMgr.gameAction;
        msg.countdownTime = this.gameMgr.countDown;
        msg.currentChair = this.gameMgr.currentChair;
        Round currentRound = this.gameMgr.game.getLastRound();
        if (currentRound != null) {
            Turn lastTurn = currentRound.getPrevTurn();
            if (lastTurn != null) {
                msg.newRound = false;
                GroupCard lastCards = lastTurn.throwCard;
                msg.lastTurnCards = lastCards.toByteArray();
            } else {
                msg.newRound = true;
                msg.lastTurnCards = new byte[0];
            }
        } else {
            msg.lastTurnCards = new byte[0];
            msg.newRound = true;
        }
        msg.chair = (byte)gamePlayer.chair;
        msg.baosam = gamePlayer.baosam;
        msg.boluot = gamePlayer.boLuot;
        msg.kieuToiTrang = gamePlayer.kiemTraToiTrang();
        msg.roomId = this.room.getId();
        msg.moneyType = this.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.roomBet = this.getMoneyBet();
        msg.initPrivateInfo(gamePlayer);
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.hasUser()) {
                msg.pInfos[i] = gp;
                msg.hasInfoAtChair[i] = true;
                continue;
            }
            msg.hasInfoAtChair[i] = false;
        }
        this.sendMsg(msg, gamePlayer.getUser());
    }

    private void pOutRoom(User user, DataCmd dataCmd) {
        Debug.trace((Object[])new Object[]{"pOutRoom", user.getName()});
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            if (gp.getPlayerStatus() == 3) {
                gp.reqQuitRoom = true;
                this.notifyRegisterOutRoom(gp);
            } else {
                this.room.group.leaveRoom(user, this.room);
            }
        }
    }

    private void notifyRegisterOutRoom(GamePlayer gp) {
        SendNotifyReqQuitRoom msg = new SendNotifyReqQuitRoom();
        msg.chair = (byte)gp.chair;
        msg.reqQuitRoom = gp.reqQuitRoom;
        this.sendMsg(msg);
    }

    private synchronized void danhBaiPlayer(GamePlayer gp, boolean boluot, byte[] cards, int auto) {
        try {
            if (gp == null || !gp.isPlaying() || gp.chair != this.gameMgr.currentChair()) {
                return;
            }
            boolean baomot = this.tinhBaoMot(gp, cards);
            if (!boluot) {
                int error = gp.takeTurn(baomot, cards, this.gameMgr.game.getCurrentRound());
                if (error == 1) {
                    return;
                }
                this.notifyDanhBai(gp.chair, cards, gp.getHandCardsSize(), auto);
                if (this.gameMgr.game.baosam && gp.chair != this.samChair || error == 3) {
                    this.endGame();
                    return;
                }
            } else {
                boolean duocBoLuot = this.kiemTraDuocBoLuot();
                if (!duocBoLuot) {
                    return;
                }
                gp.boLuot = true;
                if (baomot) {
                    gp.kiemtraDenBaoMot(this.gameMgr.game.getCurrentRound());
                }
                this.notifyBoluot(gp, auto);
            }
            this.changeTurn(baomot);
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private boolean kiemTraDuocBoLuot() {
        Round r = this.gameMgr.game.getLastRound();
        if (r == null) {
            return true;
        }
        return r.turns.size() != 0;
    }

    private void danhBaiUser(User user, boolean boluot, byte[] cards) {
        try {
            GamePlayer gp = this.getPlayerByUser(user);
            this.danhBaiPlayer(gp, boluot, cards, 0);
            gp.tuDongChoiNhanh = 0;
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    private boolean tinhBaoMot(GamePlayer gp, byte[] cards) {
        if (this.playingCount == 2 || cards != null && cards.length > 1) {
            return false;
        }
        int here = gp.chair;
        for (int i = here + 1; i < 5 + here - 1; ++i) {
            int chair = i % 5;
            GamePlayer nextGp = this.getPlayerByChair(chair);
            if (!nextGp.isPlaying()) continue;
            return !nextGp.boLuot && nextGp.baoMot;
        }
        return false;
    }

    private void pDanhBai(User user, DataCmd dataCmd) {
        RevDanhBai cmd = new RevDanhBai(dataCmd);
        this.danhBaiUser(user, cmd.boluot, cmd.cards);
    }

    private int calculateNextTurn(int from) {
        int nextTurn = -1;
        for (int i = from + 1; i < 5 + from; ++i) {
            int chair = i % 5;
            GamePlayer gp = this.getPlayerByChair(chair);
            if (!gp.isPlaying() || gp.boLuot) continue;
            nextTurn = (i + 5) % 5;
            return nextTurn;
        }
        return nextTurn;
    }

    private void changeTurn(boolean baomot) {
        this.gameMgr.prevChair = this.gameMgr.currentChair;
        this.gameMgr.currentChair = this.calculateNextTurn(this.gameMgr.currentChair);
        this.gameMgr.nextChair = this.calculateNextTurn(this.gameMgr.currentChair);
        if (this.gameMgr.nextChair == -1) {
            this.kiemTraChatChong();
            if (!baomot) {
                this.huyDenBaoMot();
            }
            this.gameMgr.game.makeRound();
            this.huyBoLuot();
            this.notifyChangeTurn(true);
            this.gameMgr.nextChair = this.calculateNextTurn(this.gameMgr.currentChair);
        } else {
            this.notifyChangeTurn(false);
        }
    }

    private void kiemTraChatChong() {
        Round round = this.gameMgr.game.getCurrentRound();
        if (round.biPhatChatChong()) {
            this.chatChong(this.gameMgr.currentChair, this.gameMgr.prevChair, round);
        }
    }

    private void huyDenBaoMot() {
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            gp.denBaoMot = false;
        }
    }

    private void notifyDanhBai(int chair, byte[] cards, int numberOfRemainCards, int auto) {
        int l1 = this.gameLog.length();
        this.gameLog.append("DB<");
        this.gameLog.append(chair).append(";");
        GroupCard card = new GroupCard(cards);
        this.gameLog.append(auto).append(";");
        this.gameLog.append(0).append(";");
        this.gameLog.append(card.toString()).append(">");
        int l2 = this.gameLog.length();
        Debug.trace(this.gameLog.substring(l1, l2));
        SendDanhBai msg = new SendDanhBai();
        msg.chair = (byte)chair;
        msg.cards = cards;
        msg.numberOfRemainCards = (byte)numberOfRemainCards;
        this.sendMsg(msg);
    }

    public void endGame() {
        this.kiemTraChatChong();
        this.gameMgr.gameState = 3;
        if (this.gameMgr.game.toitrang) {
            long[] arrl = this.pToiTrang(this.gameMgr.currentChair);
        } else if (this.gameMgr.game.baosam) {
            if (this.samChair == this.gameMgr.currentChair) {
                long[] arrl = this.pThangSam(this.samChair);
            } else {
                long[] arrl = this.pChanSam(this.samChair, this.gameMgr.currentChair);
            }
        } else {
            int denBaoMotChair = this.tinhGheDenBaoMotHetVan(this.gameMgr.currentChair);
            if (denBaoMotChair >= 0) {
                long[] arrl = this.pDenBaoMot(this.gameMgr.currentChair, denBaoMotChair);
            } else {
                long[] arrl = this.pThangThongThuong(this.gameMgr.currentChair);
            }
        }
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (gp == null || gp.pInfo != null) {
            // empty if block
        }
        this.gameMgr.countDown = this.gameMgr.game.baosam ? 17 : 15;
    }

    private int tinhGheDenBaoMotHetVan(int winChair) {
        int from = winChair;
        for (int i = from - 1; i > from - 5; --i) {
            int prevChair = (i + 5) % 5;
            GamePlayer gp = this.getPlayerByChair(prevChair);
            if (!gp.isPlaying()) continue;
            if (gp.denBaoMot) {
                return prevChair;
            }
            return -1;
        }
        return -1;
    }

    private long[] pThangSam(int samChair) {
        long[] ketQuaTinhTien = this.tinhTienThangThua(3, samChair, -1);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        this.notifyEndGame(ketQuaTinhTien, 3, samChair, -1);
        return ketQuaTinhTien;
    }

    private long[] congTruTienThangThua(long[] ketQuaTinhTien) {
        long wasterMoney;
        long res;
        long[] ketQuaThucTe = new long[5];
        UserScore score = new UserScore();
        int winChair = 0;
        long winMoney = 0L;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            score.money = ketQuaTinhTien[i];
            if (score.money > 0L) {
                winChair = i;
            } else if (score.money < 0L) {
                score.lostCount = 1;
                score.winCount = 0;
                ketQuaThucTe[i] = -gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                Debug.trace((Object[])new Object[]{"Tru tien cua", gp.pInfo.nickName, ketQuaThucTe[i]});
                winMoney -= ketQuaThucTe[i];
                score.money = ketQuaThucTe[i];
            }
            this.dispatchAddEventScore(gp.getUser(), score);
        }
        score.money = winMoney;
        long wastedTotal = wasterMoney = (long)((double)(score.money * (long)this.room.setting.commisionRate) / 100.0);
        score.money -= wastedTotal;
        score.wastedMoney = wastedTotal;
        score.winCount = 1;
        score.lostCount = 0;
        GamePlayer gp = this.getPlayerByChair(winChair);
        ketQuaThucTe[winChair] = res = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
        this.dispatchAddEventScore(gp.getUser(), score);
        Debug.trace((Object[])new Object[]{"Cong tien cho", gp.pInfo.nickName, score.money});
        return ketQuaThucTe;
    }

    private long[] tinhTienThangThua(int kieuThang, int winChair, int loseChair) {
        this.winType = kieuThang;
        long[] res = new long[5];
        GamePlayer winPlayer = this.getPlayerByChair(winChair);
        long winPlayerBean = winPlayer.gameMoneyInfo.freezeMoney;
        long winMoney = 0L;
        for (int i = 0; i < 5; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (i == winChair || !gp.isPlaying()) continue;
            res[i] = -gp.calculateMoneyLost(kieuThang, winPlayerBean, this.getMoneyBet());
            winPlayerBean -= res[i];
            winMoney -= res[i];
            if (loseChair < 0) continue;
            res[i] = 0L;
        }
        if (kieuThang == 4 || kieuThang == 10) {
            GamePlayer lostPlayer = this.getPlayerByChair(loseChair);
            long maxWin = Math.min(winPlayer.gameMoneyInfo.getFreezeMoney(), lostPlayer.gameMoneyInfo.getFreezeMoney());
            if (winMoney > maxWin) {
                winMoney = maxWin;
            }
        }
        res[winChair] = winMoney;
        if (loseChair >= 0) {
            res[loseChair] = -winMoney;
        }
        return res;
    }

    private long[] pChanSam(int samChair, int winChair) {
        long[] ketQuaTinhTien = this.tinhTienThangThua(4, winChair, samChair);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        this.notifyEndGame(ketQuaTinhTien, 4, winChair, samChair);
        return ketQuaTinhTien;
    }

    private long[] pToiTrang(int winChair) {
        GamePlayer gp = this.getPlayerByChair(winChair);
        int kieuToiTrang = gp.kiemTraToiTrang();
        int winType = 9;
        if (kieuToiTrang == 1) {
            winType = 6;
        } else if (kieuToiTrang == 3) {
            winType = 7;
        } else if (kieuToiTrang == 2) {
            winType = 8;
        }
        long[] ketQuaTinhTien = this.tinhTienThangThua(winType, winChair, -1);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        this.notifyEndGame(ketQuaTinhTien, winType, winChair, -1);
        return ketQuaTinhTien;
    }

    private long[] pDenBaoMot(int winChair, int lostChair) {
        long[] ketQuaTinhTien = this.tinhTienThangThua(10, winChair, lostChair);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        if (this.playingCount > 2) {
            this.notifyEndGame(ketQuaTinhTien, 10, winChair, lostChair);
        } else {
            this.notifyEndGame(ketQuaTinhTien, 2, winChair, -1);
        }
        return ketQuaTinhTien;
    }

    private long[] pThangThongThuong(int winChair) {
        long[] ketQuaTinhTien = this.tinhTienThangThua(2, winChair, -1);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        this.notifyEndGame(ketQuaTinhTien, 2, winChair, -1);
        return ketQuaTinhTien;
    }

    private void notifyKickRoom(GamePlayer gp, int reason) {
        SendKickRoom msg = new SendKickRoom();
        msg.reason = (byte)reason;
        this.sendMsg(msg, gp.getUser());
    }

    public synchronized void pPrepareNewGame() {
        int i;
        GamePlayer gp;
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 5; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 0) {
                if (!this.coTheChoiTiep(gp)) {
                    if (!gp.checkMoneyCanPlay()) {
                        this.notifyKickRoom(gp, 1);
                    }
                    if (gp.getUser() != null && this.room != null) {
                        GameRoom gameRoom = (GameRoom)gp.getUser().getProperty("GAME_ROOM");
                        if (gameRoom == this.room) {
                            this.room.group.leaveRoom(gp.getUser());
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
        this.kiemTraNguoiDiDau();
        msg.startChair = (byte)this.gameMgr.currentChair;
        for (i = 0; i < 5; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.sendMsg(msg, gp.getUser());
        }
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
    }

    private void chatChong(int chair1, int chair2, Round round) {
        this.gameLog.append("CC<").append(chair1).append(";").append(chair2).append(";");
        GamePlayer gpWin = this.getPlayerByChair(chair1);
        GameMoneyInfo pWin = gpWin.gameMoneyInfo;
        GamePlayer gpLost = this.getPlayerByChair(chair2);
        GameMoneyInfo pLost = this.getPlayerByChair((int)chair2).gameMoneyInfo;
        long moneyLost = this.getMoneyBet() * (long)round.soLaPhatChatChong;
        long maxWin = Math.min(pWin.freezeMoney, pLost.freezeMoney);
        if (moneyLost > maxWin) {
            moneyLost = maxWin;
        }
        this.gameLog.append(moneyLost).append(">");
        UserScore score = new UserScore();
        score.money = -moneyLost;
        long moneyWin = pLost.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
        Debug.trace((Object[])new Object[]{"Tru tien chat chong:", gpLost.pInfo.nickName, moneyWin});
        this.dispatchAddEventScore(gpLost.getUser(), score);
        long moneyWaste = moneyWin * (long)this.room.setting.commisionRate / 100L;
        score.money = moneyWin - moneyWaste;
        score.wastedMoney = moneyWaste;
        pWin.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
        Debug.trace((Object[])new Object[]{"Cong tien chat chong:", gpLost.pInfo.nickName, score.money});
        this.dispatchAddEventScore(gpWin.getUser(), score);
        long winBalance = gpWin.gameMoneyInfo.currentMoney;
        long lostBalance = gpLost.gameMoneyInfo.currentMoney;
        this.notityChatChong(chair1, chair2, moneyWin, -moneyWin, winBalance, lostBalance);
    }

    private void notityChatChong(int winChair, int lostChair, long moneyWin, long moneyLost, long winBalance, long lostBalance) {
        SendChatChong msg = new SendChatChong();
        msg.winChair = winChair;
        msg.lostChair = lostChair;
        msg.winMoney = moneyWin;
        msg.lostMoney = moneyLost;
        msg.curWinPlayerMoney = winBalance;
        msg.curLostPlayerMoney = lostBalance;
        this.sendMsg(msg);
    }

    private void huyBoLuot() {
        for (int i = 0; i < 5; ++i) {
            this.getPlayerByChair((int)i).boLuot = false;
        }
    }

    public void notifyChangeTurn(boolean newRound) {
        SendChangeTurn msg = new SendChangeTurn();
        msg.newRound = newRound;
        msg.curentChair = (byte)this.gameMgr.currentChair();
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair());
        if (gp == null) {
            CommonHandle.writeErrLog((String)"\nLoi change turn");
            CommonHandle.writeErrLog((String)("" + this.gameMgr.currentChair + ";"));
            CommonHandle.writeErrLog((String)("" + this.gameMgr.prevChair + ";"));
            CommonHandle.writeErrLog((String)("" + this.gameMgr.nextChair + "\n"));
            CommonHandle.writeErrLog((String)this.toString());
            return;
        }
        if (gp.tuDongBoLuot()) {
            msg.countDownTime = (byte)5;
            this.sendMsg(msg);
            this.gameMgr.countDown = 5;
        } else {
            msg.countDownTime = (byte)20;
            this.sendMsg(msg);
            this.gameMgr.countDown = 20;
        }
    }

    private void notifyEndGame(long[] ketQuaTinhTien, int winType, int winChair, int loseChair) {
        int i;
        GamePlayer gp;
        SendEndGame msg = new SendEndGame();
        boolean thuaden = false;
        if (loseChair >= 0) {
            thuaden = true;
        }
        for (i = 0; i < 5; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
                msg.cards[i] = gp.getHandCards();
                msg.moneyArray[i] = gp.gameMoneyInfo.currentMoney;
                if (i == winChair) {
                    msg.winType[i] = (byte)winType;
                    continue;
                }
                if (thuaden) {
                    if (i == loseChair) {
                        if (winType == 10) {
                            msg.winType[i] = 11;
                            continue;
                        }
                        msg.winType[i] = 16;
                        continue;
                    }
                    msg.winType[i] = 12;
                    continue;
                }
                if (gp.getHandCardsSize() == 10 && winType == 2) {
                    msg.winType[i] = 14;
                    msg.winType[winChair] = 5;
                    continue;
                }
                if (winType == 2) {
                    msg.winType[i] = 13;
                    continue;
                }
                msg.winType[i] = 15;
                continue;
            }
            msg.cards[i] = new byte[0];
            msg.winType[i] = 1;
        }
        this.gameLog.append("KT<").append(winType).append(";");
        for (i = 0; i < 5; ++i) {
            gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.gameLog.append(i).append("/").append(ketQuaTinhTien[i]).append(gp.spInfo.handCards.toString()).append(";");
        }
        this.gameLog.deleteCharAt(this.gameLog.length() - 1);
        this.gameLog.append(">");
        msg.ketQuaTinhTien = ketQuaTinhTien;
        msg.countdown = this.gameMgr.game.baosam ? 17 : 15;
        this.sendMsg(msg);
    }

    private void pBatDau(User user, DataCmd dataCmd) {
        int nextGamePlayerCount = this.demSoNguoiChoiTiep();
        Debug.trace((Object[])new Object[]{user.getName(), "bat dau game", "playerCount =", nextGamePlayerCount});
        if (nextGamePlayerCount >= 2) {
            this.gameMgr.makeAutoStart(0);
        }
    }

    public void tudongChoi() {
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (!this.checkPlayerChair(this.gameMgr.currentChair)) {
            CommonHandle.writeErrLog((String)"\nLoi tu dong choi");
            CommonHandle.writeErrLog((String)("" + this.gameMgr.currentChair + ";"));
            CommonHandle.writeErrLog((String)("" + this.gameMgr.prevChair + ";"));
            CommonHandle.writeErrLog((String)("" + this.gameMgr.nextChair + "\n"));
            CommonHandle.writeErrLog((String)this.toString());
            return;
        }
        if (this.gameMgr.game.getCurrentRound().turns.size() == 0) {
            this.danhBaiPlayer(gp, false, gp.getMinCardS(), 1);
        } else {
            this.danhBaiPlayer(gp, true, null, 1);
        }
        ++gp.tuDongChoiNhanh;
    }

    private void notifyBoluot(GamePlayer gamePlayer, int auto) {
        this.gameLog.append("DB<");
        this.gameLog.append(gamePlayer.chair).append(";");
        this.gameLog.append(auto).append(";");
        this.gameLog.append(1).append(";");
        this.gameLog.append("#$").append(">");
        SendBoluot msg = new SendBoluot();
        msg.chair = (byte)gamePlayer.chair;
        this.sendMsg(msg);
    }

    private void pCheatCards(User user, DataCmd dataCmd) {
        if (!GameUtils.isCheat) {
            return;
        }
        RevCheatCard cmd = new RevCheatCard(dataCmd);
        if (cmd.isCheat) {
            this.gameMgr.game.isCheat = true;
            this.gameMgr.game.suit.setOrder(cmd.cards);
            GamePlayer gp = this.getPlayerByChair(cmd.firstChair);
            if (gp.hasUser()) {
                this.lastWinId = gp.pInfo.userId;
            }
        } else {
            this.gameMgr.game.isCheat = false;
            this.lastWinId = -1;
        }
    }

    public void logQuyetDinhDiDau(SendFirstTurnDecision msg) {
        this.gameLog.append("DD<").append(msg.isRandom).append(";");
        this.gameLog.append(msg.chair).append(";");
        GroupCard card = new GroupCard(msg.cards);
        this.gameLog.append(card.toString()).append(">");
    }

    private void pDangKyChoiTiep(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.choiTiepVanSau = true;
        }
    }

    private void dispatchAddEventScore(User user, UserScore score) {
        if (user == null) {
            return;
        }
        score.moneyType = this.room.setting.moneyType;
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.USER, user);
        evtParams.put(GameEventParam.USER_SCORE, score);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.EVENT_ADD_SCORE, evtParams));
    }

    private synchronized void gameLoop() {
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

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                SamGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

