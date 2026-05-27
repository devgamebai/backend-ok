/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.modules.admin.entities.JackpotController
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
 */
package game.tienlen.server;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.modules.admin.entities.JackpotController;
import game.modules.gameRoom.cmd.send.SendNoHu;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ListGameMoneyInfo;
import game.modules.gameRoom.entities.MoneyException;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.tienlen.server.cmd.receive.RevCheatCard;
import game.tienlen.server.cmd.receive.RevDanhBai;
import game.tienlen.server.cmd.send.SendBaoChoBonDoiThong;
import game.tienlen.server.cmd.send.SendBoluot;
import game.tienlen.server.cmd.send.SendChangeTurn;
import game.tienlen.server.cmd.send.SendChatChong;
import game.tienlen.server.cmd.send.SendDanhBai;
import game.tienlen.server.cmd.send.SendDealCard;
import game.tienlen.server.cmd.send.SendEndGame;
import game.tienlen.server.cmd.send.SendFirstTurnDecision;
import game.tienlen.server.cmd.send.SendGameInfo;
import game.tienlen.server.cmd.send.SendJoinRoomSuccess;
import game.tienlen.server.cmd.send.SendKickRoom;
import game.tienlen.server.cmd.send.SendNewUserJoin;
import game.tienlen.server.cmd.send.SendNotifyReqQuitRoom;
import game.tienlen.server.cmd.send.SendUpdateMatch;
import game.tienlen.server.cmd.send.SendUpdateOwnerRoom;
import game.tienlen.server.cmd.send.SendUserExitRoom;
import game.tienlen.server.logic.GroupCard;
import game.tienlen.server.logic.Round;
import game.tienlen.server.logic.Turn;
import game.utils.GameUtils;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class TienlenGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    private ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int gsResult = 2;
    public static final int KHONG_CHOI = 1;
    public static final int THANG_THONG_THUONG = 2;
    public static final int THANG_BAT_TREO = 3;
    public static final int THANG_TRANG_SANH_RONG = 4;
    public static final int THANG_TRANG_TU_HEO = 5;
    public static final int THANG_TRANG_NAM_DOI_THONG = 6;
    public static final int THANG_TRANG_SAU_DOI = 7;
    public static final int THANG_TRANG_DONG_MAU_13 = 8;
    public static final int THANG_TRANG_DONG_MAU_12 = 9;
    public static final int KET_QUA_HOA = 10;
    public static final int THUA_THONG_THUONG = 11;
    public static final int THUA_TREO = 12;
    public static final int THUA_TOI_TRANG = 13;
    public static final int PHONG_CO_KHOA = 1;
    public static final int PHONG_KHONG_CO_KHOA = 2;
    public static final String USER_CHAIR = "user_chair";
    private final GameManager gameMgr = new GameManager();
    public final Vector<GamePlayer> playerList = new Vector(4);
    public int lastWinId = -1;
    public int winChair = -1;
    public int samChair = -1;
    public int playingCount = 0;
    public int winType;
    private volatile int serverState = 0;
    public volatile int groupIndex;
    public volatile int playerCount;
    StringBuilder gameLog = new StringBuilder();
    private final Runnable gameLoopTask = new GameLoopTask();
    public ThongTinThangLon thongTinNoHu = null;

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
            for (int i = 0; i < 4; ++i) {
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

    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
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
            case 3115: {
                this.pCheatCards(user, dataCmd);
                break;
            }
            case 3116: {
                this.pDangKyChoiTiep(user, dataCmd);
            }
        }
    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 4) {
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
        if (i >= 0 && i < 4) {
            return this.playerList.get(i);
        }
        return null;
    }

    public long getMoneyBet() {
        return this.gameMgr.gameServer.room.setting.moneyBet;
    }

    public int isNeedRandomFirstTurn() {
        if (this.lastWinId > 0) {
            for (int i = 0; i < 4; ++i) {
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
        return chair >= 0 && chair < 4;
    }

    public synchronized void onGameUserExit(User user) {
        Integer chair = (Integer)user.getProperty((Object)USER_CHAIR);
        if (chair == null) {
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
        if (this.playerCount == 0) {
            this.resetPlayDisconnect();
            this.destroy();
        }
    }

    public synchronized void onGameUserDis(User user) {
        Integer chair = (Integer)user.getProperty((Object)USER_CHAIR);
        if (chair == null) {
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
            GameRoomManager.instance().leaveRoom(user, this.room);
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null) continue;
            gp.pInfo.setIsHold(false);
        }
    }

    public synchronized void onGameUserReturn(User user) {
        if (user == null) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
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
        GamePlayer gp;
        int i;
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
        for (i = 0; i < 4; ++i) {
            gp = this.playerList.get(i);
            if (gp.getPlayerStatus() == 0 || gp.pInfo == null || !gp.pInfo.nickName.equalsIgnoreCase(user.getName())) continue;
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
        if (this.room.setting.maxUserPerRoom == 4) {
            for (i = 0; i < 4; ++i) {
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
            for (i = 0; i < 4; ++i) {
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
        SendUpdateOwnerRoom msg = new SendUpdateOwnerRoom();
        msg.ownerChair = chair;
        this.send(msg);
    }

    public int getNumTotalPlayer() {
        return this.playerCount;
    }

    public void sendMsgToPlayingUser(BaseMsg msg) {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.send(msg, gp.getUser());
        }
    }

    public void send(BaseMsg msg) {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
        }
    }

    public void chiabai() {
        int from = this.gameLog.length();
        this.gameLog.append("CB<");
        SendDealCard msg = new SendDealCard();
        msg.gameId = this.gameMgr.game.id;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            msg.toitrang = gp.spInfo.handCards.kiemTraToiTrang();
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
            this.gameLog.append(msg.toitrang).append(";");
            this.send((BaseMsg)msg, user);
        }
        this.gameLog.append(">");
        int to = this.gameLog.length();
    }

    public void start() {
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        this.serverState = 1;
        boolean checkJackpot = false;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            gp.tuDongChoiNhanh = 0;
            if (!this.coTheChoiTiep(gp)) continue;
            gp.setPlayerStatus(3);
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)gp.pInfo.nickName, (int)this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(i).append(";");
            gp.choiTiepVanSau = false;
            if (checkJackpot) continue;
            checkJackpot = JackpotController.instance().checkJackpotPlayers(gp.getUser(), this.room);
        }
        this.gameLog.append(this.room.setting.moneyType).append(";");
        this.gameLog.append(">");
        this.gameMgr.gameAction = 0;
        this.gameMgr.countDown = 0;
        this.logStartGame();
    }

    private void logStartGame() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public byte getToiTrang() {
        int firstTurn;
        for (int i = firstTurn = this.gameMgr.logic.firstTurn; i < 4 + firstTurn; ++i) {
            int chair = (i + 4) % 4;
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
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            ++count;
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            ++count;
        }
        return count;
    }

    private void xuLiDanhCap(int count) {
        GamePlayer gp2;
        GamePlayer gp1;
        int j;
        int i;
        boolean checkIp = false;
        for (i = 0; i < 4; ++i) {
            gp1 = this.getPlayerByChair(i);
            if (gp1.getUser() == null) continue;
            for (j = i + 1; j < 4; ++j) {
                gp2 = this.getPlayerByChair(j);
                if (gp2.getUser() == null || gp1.getUser().getIpAddress().equalsIgnoreCase(gp2.getUser().getIpAddress())) continue;
                checkIp = true;
            }
        }
        if (!checkIp) {
            return;
        }
        for (i = 0; i < 4; ++i) {
            gp1 = this.getPlayerByChair(i);
            if (gp1.getUser() == null) continue;
            for (j = i + 1; j < 4; ++j) {
                gp2 = this.getPlayerByChair(j);
                if (gp2.getUser() == null || this.kiemTraDuocDanhCungNhau(gp1, gp2, checkIp) || --count != 2) continue;
                return;
            }
        }
    }

    private boolean kiemTraDuocDanhCungNhau(GamePlayer gp1, GamePlayer gp2, boolean checkIp) {
        if (gp1.reqQuitRoom || gp2.reqQuitRoom) {
            return false;
        }
        if (checkIp && gp1.getUser().getIpAddress().equalsIgnoreCase(gp2.getUser().getIpAddress())) {
            if (gp1.timeJoinRoom > gp2.timeJoinRoom) {
                gp1.reqQuitRoom = true;
                GameRoomManager.instance().leaveRoom(gp1.getUser(), this.room);
            } else {
                gp2.reqQuitRoom = true;
                GameRoomManager.instance().leaveRoom(gp2.getUser(), this.room);
            }
            return false;
        }
        long delta = Math.abs(gp1.timeJoinRoom - gp2.timeJoinRoom);
        if ((double)delta < 4500.0) {
            if (gp1.timeJoinRoom > gp2.timeJoinRoom) {
                GameRoomManager.instance().leaveRoom(gp1.getUser(), this.room);
                gp1.reqQuitRoom = true;
            } else {
                GameRoomManager.instance().leaveRoom(gp2.getUser(), this.room);
                gp2.reqQuitRoom = true;
            }
            return false;
        }
        return true;
    }

    public void kiemTraTuDongBatDau(int after) {
        if (this.gameMgr.gameState == 0) {
            int count = this.demSoNguoiChoiTiep();
            if (count < 2) {
                this.gameMgr.cancelAutoStart();
            } else {
                this.gameMgr.makeAutoStart(after);
                if (count > 2) {
                    this.xuLiDanhCap(count);
                }
            }
        }
    }

    private void kiemTraNguoiDiDau() {
        int from;
        for (int i = from = this.gameMgr.currentChair; i < from + 4; ++i) {
            int chair = i % 4;
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
            return;
        }
        GamePlayer gp = this.playerList.get(chair);
        gp.choiTiepVanSau = true;
        gp.tuDongChoiNhanh = 0;
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
        msg.comission = this.gameMgr.gameServer.room.setting.commisionRate;
        msg.comissionJackpot = this.gameMgr.gameServer.room.setting.rule;
        msg.moneyType = this.gameMgr.gameServer.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.gameMgr.gameServer.room.setting.moneyBet;
        msg.roomOwner = (byte)this.gameMgr.roomOwnerChair;
        for (int i = 0; i < 4; ++i) {
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
        this.send((BaseMsg)msg, gamePlayer.getUser());
    }

    private byte[] currentCardOnBoard() {
        Turn turn;
        Round round = this.gameMgr.game.getLastRound();
        if (round != null && (turn = round.getPrevTurn()) != null && turn.throwCard != null) {
            return turn.throwCard.toByteArray();
        }
        return new byte[0];
    }

    private void notifyUserExit(GamePlayer gamePlayer, boolean disconnect) {
        if (gamePlayer.pInfo != null) {
            gamePlayer.pInfo.setIsHold(false);
            SendUserExitRoom msg = new SendUserExitRoom();
            msg.nChair = (byte)gamePlayer.chair;
            msg.nickName = gamePlayer.pInfo.nickName;
            this.send(msg);
        }
    }

    public GamePlayer getPlayerByUser(User user) {
        Integer chair = (Integer)user.getProperty((Object)USER_CHAIR);
        if (chair != null) {
            GamePlayer gp = this.getPlayerByChair(chair);
            if (gp != null && gp.pInfo != null && gp.pInfo.nickName.equalsIgnoreCase(user.getName())) {
                return gp;
            }
            return null;
        }
        return null;
    }

    private void sendGameInfo(int chair) {
        GamePlayer gamePlayer = this.getPlayerByChair(chair);
        SendGameInfo msg = new SendGameInfo();
        msg.maxUserPerRoom = this.room.setting.maxUserPerRoom;
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
        msg.boluot = gamePlayer.boLuot;
        msg.kieuToiTrang = gamePlayer.kiemTraToiTrang();
        msg.roomId = this.room.getId();
        msg.moneyType = this.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.roomBet = this.getMoneyBet();
        msg.initPrivateInfo(gamePlayer);
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.hasUser()) {
                msg.pInfos[i] = gp;
                msg.hasInfoAtChair[i] = true;
                continue;
            }
            msg.hasInfoAtChair[i] = false;
        }
        this.send((BaseMsg)msg, gamePlayer.getUser());
    }

    private void pOutRoom(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            if (gp.getPlayerStatus() == 3) {
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

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private synchronized void danhBaiPlayer(GamePlayer gp, boolean boluot, byte[] cards, int auto) {
        try {
            GroupCard playCard = null;
            if (cards != null) {
                playCard = new GroupCard(cards);
            }
            if (gp == null || !gp.isPlaying() || gp.chair != this.gameMgr.currentChair() && (playCard == null || playCard.BO != 5)) return;
            if (!boluot) {
                Round round;
                if (!(playCard == null || playCard.BO != 5 || (round = this.gameMgr.game.getCurrentRound()).isNewRound() || round.soLaChatChong != 0 && gp.chair != this.gameMgr.prevChairThrowCard)) {
                    return;
                }
                int error = gp.takeTurn(playCard, this.gameMgr.game.getCurrentRound());
                if (error == 1) return;
                if (playCard.BO == 5) {
                    this.gameMgr.prevChair = this.gameMgr.prevChairThrowCard;
                }
                this.gameMgr.prevChairThrowCard = gp.chair;
                this.notifyDanhBai(gp.chair, cards, gp.getHandCardsSize(), auto);
                if (playCard.BO == 5) {
                    this.gameMgr.currentChair = gp.chair;
                    this.gameMgr.gameAction = 6;
                    if (this.kiemTraDuBaDanhBonDoiThong()) {
                        this.notifyChoBonDoiThong();
                        this.gameMgr.countDown = 10;
                    } else {
                        this.gameMgr.countDown = 0;
                    }
                    if (error != 3) {
                        return;
                    }
                }
                if (error == 3) {
                    this.endGame();
                    return;
                }
            } else {
                boolean duocBoLuot = this.kiemTraDuocBoLuot();
                if (!duocBoLuot) {
                    return;
                }
                gp.boLuot = true;
                this.notifyBoluot(gp, auto);
            }
            this.changeTurn();
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void ketThucDanhBonDoiThong() {
        this.kiemTraChatChong();
        this.huyBoLuot();
        this.gameMgr.game.makeRound();
        this.gameMgr.nextChair = this.calculateNextTurn(this.gameMgr.currentChair);
        this.gameMgr.countDown = 20;
        this.gameMgr.gameAction = 3;
        this.notifyChangeTurn(true);
    }

    public synchronized void changeTurn() {
        this.gameMgr.prevChair = this.gameMgr.currentChair;
        this.gameMgr.currentChair = this.calculateNextTurn(this.gameMgr.currentChair);
        this.gameMgr.nextChair = this.calculateNextTurn(this.gameMgr.currentChair);
        if (this.gameMgr.nextChair == -1) {
            if (this.kiemTraChoBonDoiThong()) {
                this.notifyChoBonDoiThong();
                this.gameMgr.gameAction = 5;
                this.gameMgr.countDown = 5;
            } else {
                this.changeTurnNewRound();
            }
        } else {
            this.notifyChangeTurn(false);
        }
    }

    private boolean kiemTraDuBaDanhBonDoiThong() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.chair == this.gameMgr.prevChairThrowCard) continue;
            GroupCard handCard = gp.spInfo.handCards;
            if (handCard.cards.size() < 8) continue;
            return true;
        }
        return false;
    }

    private boolean kiemTraChoBonDoiThong() {
        if (this.playingCount == 2) {
            return false;
        }
        Round round = this.gameMgr.game.getCurrentRound();
        if (round.soLaChatChong == 0) {
            return false;
        }
        return this.kiemTraDuBaDanhBonDoiThong();
    }

    public synchronized void changeTurnNewRound() {
        this.kiemTraChatChong();
        this.gameMgr.game.makeRound();
        this.huyBoLuot();
        this.gameMgr.countDown = 20;
        this.gameMgr.gameAction = 3;
        this.notifyChangeTurn(true);
        this.gameMgr.nextChair = this.calculateNextTurn(this.gameMgr.currentChair);
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

    private void pDanhBai(User user, DataCmd dataCmd) {
        RevDanhBai cmd = new RevDanhBai(dataCmd);
        this.danhBaiUser(user, cmd.boluot, cmd.cards);
    }

    private int calculateNextTurn(int from) {
        int nextTurn = -1;
        for (int i = from + 1; i < 4 + from; ++i) {
            int chair = i % 4;
            GamePlayer gp = this.getPlayerByChair(chair);
            if (!gp.isPlaying() || gp.boLuot) continue;
            nextTurn = (i + 4) % 4;
            return nextTurn;
        }
        return nextTurn;
    }

    private void notifyChoBonDoiThong() {
        SendBaoChoBonDoiThong msg = new SendBaoChoBonDoiThong();
        msg.chair = this.gameMgr.currentChair;
        this.send(msg);
    }

    private void kiemTraChatChong() {
        Round round = this.gameMgr.game.getCurrentRound();
        if (round.biPhatChatChong()) {
            this.chatChong(this.gameMgr.currentChair, this.gameMgr.prevChair, round);
        }
    }

    private void notifyDanhBai(int chair, byte[] cards, int numberOfRemainCards, int auto) {
        this.gameLog.append("DB<");
        this.gameLog.append(chair).append(";");
        GroupCard card = new GroupCard(cards);
        this.gameLog.append(auto).append(";");
        this.gameLog.append(0).append(";");
        this.gameLog.append(card.toString()).append(">");
        SendDanhBai msg = new SendDanhBai();
        msg.chair = (byte)chair;
        msg.cards = cards;
        msg.numberOfRemainCards = (byte)numberOfRemainCards;
        this.send(msg);
    }

    public void endGame() {
        this.kiemTraChatChong();
        this.gameMgr.gameState = 3;
        if (this.gameMgr.game.toitrang) {
            long[] ketQuaTinhTien = this.pToiTrang(this.gameMgr.currentChair);
        } else {
            long[] ketQuaTinhTien = this.pThangThongThuong(this.gameMgr.currentChair);
        }
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (gp == null || gp.pInfo != null) {
            // empty if block
        }
        this.gameMgr.countDown = 15;
        this.kiemTraNoHuThangLon();
    }

    private boolean dispatchEventThangLon(GamePlayer gp, boolean isNoHu) {
        boolean result = GameUtils.dispatchEventThangLon((User)gp.getUser(), (GameRoom)this.room, (int)this.gameMgr.game.id, (GameMoneyInfo)gp.gameMoneyInfo, (long)this.getMoneyBet(), (boolean)isNoHu, (byte[])gp.getHandCards());
        return result;
    }

    private void kiemTraNoHuThangLon() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (gp.spInfo.kiemTraNoHu()) {
                if (!this.dispatchEventThangLon(gp, true)) continue;
                this.gameMgr.countDown += 5;
                continue;
            }
            this.dispatchEventThangLon(gp, false);
        }
    }

    private long[] congTruTienThangThua(long[] ketQuaTinhTien) {
        long res;
        long[] ketQuaThucTe = new long[5];
        UserScore score = new UserScore();
        int winChair = 0;
        long winMoney = 0L;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            score.money = ketQuaTinhTien[i];
            if (score.money > 0L) {
                winChair = i;
                continue;
            }
            if (score.money >= 0L) continue;
            score.lostCount = 1;
            score.winCount = 0;
            try {
                ketQuaThucTe[i] = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            }
            catch (MoneyException e) {
                ketQuaThucTe[i] = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            winMoney -= ketQuaThucTe[i];
            score.money = ketQuaThucTe[i];
            GameUtils.dispatchAddEventScore((User)gp.getUser(), (UserScore)score, (GameRoom)this.room);
        }
        score.money = winMoney;
        long wasterMoney = (long)((double)(score.money * (long)this.room.setting.commisionRate) / 100.0);
        score.money -= wasterMoney;
        score.wastedMoney = wasterMoney;
        score.winCount = 1;
        score.lostCount = 0;
        GamePlayer gp = this.getPlayerByChair(winChair);
        try {
            res = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
        }
        catch (MoneyException e) {
            res = 0L;
            CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
            gp.reqQuitRoom = true;
        }
        ketQuaThucTe[winChair] = res;
        GameUtils.dispatchAddEventScore((User)gp.getUser(), (UserScore)score, (GameRoom)this.room);
        return ketQuaThucTe;
    }

    private long[] tinhTienThangThua(int kieuThang, int winChair, int loseChair) {
        this.winType = kieuThang;
        long[] res = new long[4];
        GamePlayer winPlayer = this.getPlayerByChair(winChair);
        long winPlayerBean = winPlayer.gameMoneyInfo.getCurrentMoneyFromCache();
        long winMoney = 0L;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (i == winChair || !gp.isPlaying()) continue;
            res[i] = -gp.calculateMoneyLost(kieuThang, winPlayerBean, this.getMoneyBet());
            winPlayerBean -= res[i];
            winMoney -= res[i];
            if (loseChair < 0) continue;
            res[i] = 0L;
        }
        res[winChair] = winMoney;
        if (loseChair >= 0) {
            res[loseChair] = -winMoney;
        }
        return res;
    }

    private long[] pToiTrang(int winChair) {
        GamePlayer gp = this.getPlayerByChair(winChair);
        int kieuToiTrang = gp.kiemTraToiTrang();
        int winType = 8;
        if (kieuToiTrang == 1) {
            winType = 4;
        } else if (kieuToiTrang == 4) {
            winType = 5;
        } else if (kieuToiTrang == 3) {
            winType = 7;
        } else if (kieuToiTrang == 2) {
            winType = 6;
        } else if (kieuToiTrang == 6) {
            winType = 9;
        }
        long[] ketQuaTinhTien = this.tinhTienThangThua(winType, winChair, -1);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        this.notifyEndGame(ketQuaTinhTien, winType, winChair, -1);
        return ketQuaTinhTien;
    }

    private long[] pThangThongThuong(int winChair) {
        long[] ketQuaTinhTien = this.tinhTienThangThua(2, winChair, -1);
        ketQuaTinhTien = this.congTruTienThangThua(ketQuaTinhTien);
        this.notifyEndGame(ketQuaTinhTien, 2, winChair, -1);
        return ketQuaTinhTien;
    }

    private void notifyKickRoom(GamePlayer gp, byte reason) {
        SendKickRoom msg = new SendKickRoom();
        msg.reason = reason;
        this.send((BaseMsg)msg, gp.getUser());
    }

    public synchronized void pPrepareNewGame() {
        GamePlayer gp;
        int i;
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 0) {
                if (GameUtils.isMainTain || !this.coTheChoiTiep(gp)) {
                    if (!gp.checkMoneyCanPlay()) {
                        this.notifyKickRoom(gp, (byte)1);
                    } else if (GameUtils.isMainTain) {
                        this.notifyKickRoom(gp, (byte)2);
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
        this.kiemTraNguoiDiDau();
        msg.startChair = (byte)this.gameMgr.currentChair;
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
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
        long maxWin = Math.min(pWin.getCurrentMoneyFromCache(), pLost.getCurrentMoneyFromCache());
        if (moneyLost > maxWin) {
            moneyLost = maxWin;
        }
        UserScore score = new UserScore();
        score.money = -moneyLost;
        try {
            moneyLost = pLost.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
        }
        catch (MoneyException e) {
            moneyLost = 0L;
            CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gpLost.gameMoneyInfo.toString()));
            gpLost.reqQuitRoom = true;
        }
        score.money = moneyLost;
        this.gameLog.append(score.money).append(";");
        GameUtils.dispatchAddEventScore((User)gpLost.getUser(), (UserScore)score, (GameRoom)this.room);
        long moneyWin = -score.money;
        long moneyWaste = (long)((double)(moneyWin * (long)this.room.setting.commisionRate) / 100.0);
        score.money = moneyWin - moneyWaste;
        score.wastedMoney = moneyWaste;
        try {
            moneyWin = pWin.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
        }
        catch (MoneyException e) {
            moneyWin = 0L;
            gpWin.reqQuitRoom = true;
            CommonHandle.writeErrLogDebug((Object[])new Object[]{"ERROR WHEN CHARNGING MONEY:", gpWin.gameMoneyInfo.toString()});
        }
        score.money = moneyWin;
        this.gameLog.append(score.money).append(">");
        GameUtils.dispatchAddEventScore((User)gpWin.getUser(), (UserScore)score, (GameRoom)this.room);
        long winBalance = gpWin.gameMoneyInfo.currentMoney;
        long lostBalance = gpLost.gameMoneyInfo.currentMoney;
        this.notityChatChong(chair1, chair2, moneyWin, moneyLost, winBalance, lostBalance);
    }

    private void notityChatChong(int winChair, int lostChair, long moneyWin, long moneyLost, long winBalance, long lostBalance) {
        SendChatChong msg = new SendChatChong();
        msg.winChair = winChair;
        msg.lostChair = lostChair;
        msg.winMoney = moneyWin;
        msg.lostMoney = moneyLost;
        msg.curWinPlayerMoney = winBalance;
        msg.curLostPlayerMoney = lostBalance;
        this.send(msg);
    }

    private void huyBoLuot() {
        for (int i = 0; i < 4; ++i) {
            this.getPlayerByChair((int)i).boLuot = false;
        }
    }

    public void notifyChangeTurn(boolean newRound) {
        SendChangeTurn msg = new SendChangeTurn();
        msg.newRound = newRound;
        msg.curentChair = (byte)this.gameMgr.currentChair();
        msg.prevChairThrowCard = (byte)this.gameMgr.prevChairThrowCard;
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair());
        if (gp == null) {
            CommonHandle.writeErrLog((String)"\nLoi change turn");
            CommonHandle.writeErrLog((String)(this.gameMgr.currentChair + ";"));
            CommonHandle.writeErrLog((String)(this.gameMgr.prevChair + ";"));
            CommonHandle.writeErrLog((String)(this.gameMgr.nextChair + "\n"));
            CommonHandle.writeErrLog((String)this.toString());
            return;
        }
        if (gp.tuDongBoLuot()) {
            msg.countDownTime = (byte)5;
            this.send(msg);
            this.gameMgr.countDown = 5;
        } else {
            msg.countDownTime = (byte)20;
            this.send(msg);
            this.gameMgr.countDown = 20;
        }
    }

    private void notifyEndGame(long[] ketQuaTinhTien, int winType, int winChair, int loseChair) {
        GamePlayer gp;
        int i;
        SendEndGame msg = new SendEndGame();
        boolean thuaden = false;
        if (loseChair >= 0) {
            thuaden = true;
        }
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.hasUser() && gp.gameMoneyInfo != null) {
                msg.moneyArray[i] = gp.gameMoneyInfo.currentMoney;
            }
            if (gp.isPlaying()) {
                msg.cards[i] = gp.getHandCards();
                if (i == winChair) {
                    msg.winType[i] = (byte)winType;
                    continue;
                }
                if (gp.getHandCardsSize() == 13 && winType == 2) {
                    msg.winType[i] = 12;
                    msg.winType[winChair] = 3;
                    continue;
                }
                if (winType == 2) {
                    msg.winType[i] = 11;
                    continue;
                }
                msg.winType[i] = 13;
                continue;
            }
            msg.cards[i] = new byte[0];
            msg.winType[i] = 1;
        }
        this.gameLog.append("KT<").append(winType).append(";");
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.gameLog.append(i).append("/").append(ketQuaTinhTien[i]).append("/").append(gp.spInfo.handCards.toString()).append(";");
            gp.spInfo.moneyWin = ketQuaTinhTien[i];
        }
        this.gameLog.deleteCharAt(this.gameLog.length() - 1);
        this.gameLog.append(">");
        msg.ketQuaTinhTien = ketQuaTinhTien;
        msg.countdown = 15;
        this.send(msg);
        this.logEndGame();
    }

    private void logEndGame() {
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    private void pBatDau(User user, DataCmd dataCmd) {
        int nextGamePlayerCount = this.demSoNguoiChoiTiep();
        if (nextGamePlayerCount >= 2) {
            this.gameMgr.makeAutoStart(0);
        }
    }

    public synchronized void tudongChoi() {
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (!this.checkPlayerChair(this.gameMgr.currentChair)) {
            CommonHandle.writeErrLog((String)"\nLoi tu dong choi");
            CommonHandle.writeErrLog((String)(this.gameMgr.currentChair + ";"));
            CommonHandle.writeErrLog((String)(this.gameMgr.prevChair + ";"));
            CommonHandle.writeErrLog((String)(this.gameMgr.nextChair + "\n"));
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
        this.send(msg);
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
            this.gameMgr.game.suit.initCard();
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

    public synchronized void onNoHu(ThongTinThangLon info) {
        this.thongTinNoHu = info;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void notifyNoHu() {
        try {
            if (this.thongTinNoHu != null) {
                for (int i = 0; i < 4; ++i) {
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
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null || !gp.getUser().getName().equalsIgnoreCase(nickName)) continue;
            this.gameMgr.game.suit.noHuAt(gp.chair);
        }
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
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                TienlenGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

