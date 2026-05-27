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
 *  com.vinplay.usercore.service.LogGameService
 *  com.vinplay.usercore.service.impl.LogGameServiceImpl
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.eventHandlers.GameEventParam
 *  game.eventHandlers.GameEventType
 *  game.modules.bot.Bot
 *  game.modules.bot.BotManager
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  game.modules.gameRoom.entities.GameRoom
 *  game.modules.gameRoom.entities.GameRoomManager
 *  game.modules.gameRoom.entities.GameRoomSetting
 *  game.modules.gameRoom.entities.GameServer
 *  game.modules.gameRoom.entities.ListGameMoneyInfo
 *  game.modules.gameRoom.entities.MoneyException
 *  game.modules.gameRoom.entities.ThongTinThangLon
 *  game.utils.GameUtils
 *  game.utils.LoggerUtils
 *  org.json.JSONArray
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package game.xizach.server;

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
import com.vinplay.usercore.service.LogGameService;
import com.vinplay.usercore.service.impl.LogGameServiceImpl;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.bot.Bot;
import game.modules.bot.BotManager;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ListGameMoneyInfo;
import game.modules.gameRoom.entities.MoneyException;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import game.xizach.server.GameManager;
import game.xizach.server.GamePlayer;
import game.xizach.server.cmd.receive.RevCheatCard;
import game.xizach.server.cmd.receive.RevDanBai;
import game.xizach.server.cmd.receive.RevRutBai;
import game.xizach.server.cmd.receive.RevXetBaiAll;
import game.xizach.server.cmd.receive.RevXetBaiOne;
import game.xizach.server.cmd.send.SendChuyenGiaiDoan2;
import game.xizach.server.cmd.send.SendChuyenGiaiDoan3;
import game.xizach.server.cmd.send.SendDanBai;
import game.xizach.server.cmd.send.SendDealCard;
import game.xizach.server.cmd.send.SendDoiChuong;
import game.xizach.server.cmd.send.SendEndGame;
import game.xizach.server.cmd.send.SendGameInfo;
import game.xizach.server.cmd.send.SendJoinRoomSuccess;
import game.xizach.server.cmd.send.SendKetQuaSoBai;
import game.xizach.server.cmd.send.SendKetQuaXiZach;
import game.xizach.server.cmd.send.SendKickRoom;
import game.xizach.server.cmd.send.SendNewUserJoin;
import game.xizach.server.cmd.send.SendNotifyReqQuitRoom;
import game.xizach.server.cmd.send.SendRutBai;
import game.xizach.server.cmd.send.SendRutNhieuBai;
import game.xizach.server.cmd.send.SendUpdateMatch;
import game.xizach.server.cmd.send.SendUpdateOwnerRoom;
import game.xizach.server.cmd.send.SendUserExitRoom;
import game.xizach.server.logic.Card;
import game.xizach.server.logic.CardSuit;
import game.xizach.server.logic.Gamble;
import game.xizach.server.logic.GroupCard;
import game.xizach.server.logic.XiZachRule;
import game.xizach.server.sPlayerInfo;
import game.xizach.server.sResultInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XiZachGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    private ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int gsResult = 2;
    public static final String USER_CHAIR = "user_chair";
    private final GameManager gameMgr = new GameManager();
    public final Vector<GamePlayer> playerList = new Vector(6);
    public int playingCount = 0;
    private volatile int serverState = 0;
    public volatile int playerCount;
    StringBuilder gameLog = new StringBuilder();
    LogGameService dao = new LogGameServiceImpl();
    boolean hasChuong = false;
    public int chuongChair = 0;
    public int newChuongChair = -1;
    private final Logger logger = LoggerFactory.getLogger((String)"debug");
    private final Runnable gameLoopTask = new GameLoopTask();

    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 3129: {
                this.danBai(user, dataCmd);
                break;
            }
            case 3128: {
                this.rutBai(user, dataCmd);
                break;
            }
            case 3130: {
                this.xetBaiOne(user, dataCmd);
                break;
            }
            case 3131: {
                this.xetBaiAll(user, dataCmd);
                break;
            }
            case 3111: {
                this.pOutRoom(user, dataCmd);
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

    private void danBai(User user, DataCmd data) {
        RevDanBai cmd = new RevDanBai(data);
        this.danBai(user);
    }

    private void rutBai(User user, DataCmd data) {
        RevRutBai cmd = new RevRutBai(data);
        this.rutBai(user);
    }

    private void xetBaiOne(User user, DataCmd data) {
        RevXetBaiOne cmd = new RevXetBaiOne(data);
        byte chair = cmd.chair;
        this.xetBaiOne(user, chair);
    }

    private void xetBaiAll(User user, DataCmd data) {
        RevXetBaiAll cmd = new RevXetBaiAll(data);
        this.xetBaiAll(user);
    }

    private void danBai(User user) {
        GamePlayer gp = this.getPlayerByUser(user);
        GamePlayer chuong = this.getPlayerByChair(this.chuongChair);
        boolean i1 = gp.isPlaying();
        boolean i2 = gp.camChuong;
        boolean i3 = chuong.isPlaying();
        if (gp != null && gp.isPlaying() && !gp.camChuong && chuong != null && chuong.isPlaying()) {
            Boolean canDan = gp.canDanBai();
            if (canDan.booleanValue()) {
                gp.hasDanBai = true;
                gp.active = true;
                this.notifyDanBaiThanhCong(gp);
            } else {
                SendDanBai msg = new SendDanBai();
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        }
    }

    private void rutBai(User user) {
        GamePlayer gp = this.getPlayerByUser(user);
        int userChair = gp.chair;
        GamePlayer chuong = this.getPlayerByChair(this.chuongChair);
        if (gp != null && gp.isPlaying() && chuong != null && chuong.isPlaying() && (this.gameMgr.gameAction == 2 && gp.chair != this.chuongChair || this.gameMgr.gameAction == 4 && gp.chair == this.chuongChair)) {
            boolean canRutBai = gp.canRutBai();
            if (canRutBai) {
                Card card = gp.spInfo.rutCard();
                gp.active = true;
                this.gameLog.append("RB<");
                this.gameLog.append(userChair).append("/");
                this.gameLog.append("#T:");
                this.gameLog.append(card.toString()).append("$;");
                this.gameLog.append(">");
                for (int i = 0; i < 6; ++i) {
                    if (i == userChair) {
                        this.notifyRutBai(gp, card, userChair);
                        continue;
                    }
                    GamePlayer gp1 = this.getPlayerByChair(i);
                    if (gp1 == null || !gp.hasUser()) continue;
                    if (gp.chair == this.chuongChair) {
                        this.notifyRutBai(gp1, card, userChair);
                        continue;
                    }
                    Card newC = Card.createCard(52);
                    this.notifyRutBai(gp1, newC, userChair);
                }
            } else {
                SendRutBai msg = new SendRutBai();
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        }
    }

    private int botRutBai(User user) {
        int countDown = -1;
        GamePlayer gp = this.getPlayerByUser(user);
        GamePlayer chuong = this.getPlayerByChair(this.chuongChair);
        if (gp != null && gp.isPlaying() && chuong != null && chuong.isPlaying() && (this.gameMgr.gameAction == 2 && gp.chair != this.chuongChair || this.gameMgr.gameAction == 4 && gp.chair == this.chuongChair)) {
            int userChair = gp.chair;
            boolean canRutBai = gp.canRutBai();
            if (canRutBai) {
                Card card = gp.spInfo.rutCard();
                countDown = gp.kiemTraBotCanRutBai();
                gp.active = true;
                this.gameLog.append("RB<");
                this.gameLog.append(userChair).append("/");
                this.gameLog.append("#T:");
                this.gameLog.append(card.toString()).append("$;");
                this.gameLog.append(">");
                for (int i = 0; i < 6; ++i) {
                    if (i == userChair) {
                        this.notifyRutBai(gp, card, userChair);
                        continue;
                    }
                    GamePlayer gp1 = this.getPlayerByChair(i);
                    if (gp1 == null || !gp.hasUser()) continue;
                    if (gp.chair == this.chuongChair) {
                        this.notifyRutBai(gp1, card, userChair);
                        continue;
                    }
                    Card newC = Card.createCard(52);
                    this.notifyRutBai(gp1, newC, userChair);
                }
            } else {
                SendRutBai msg = new SendRutBai();
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        }
        return countDown;
    }

    private void xetBaiOne(User user, int chair) {
        GamePlayer gpChuong = this.getPlayerByUser(user);
        GamePlayer gp = this.getPlayerByChair(chair);
        if (!gpChuong.camChuong) {
            return;
        }
        boolean i1 = gp.hasSoBai;
        boolean i2 = gp.hasDanBai;
        boolean i3 = gp.camChuong;
        if (gp.hasSoBai || !gp.hasDanBai || gp.camChuong) {
            return;
        }
        gp.hasSoBai = true;
        this.soBaiVoiChuong(gp, gpChuong, true);
        if (this.hasSoBaiXongTatCa()) {
            this.endGame();
        }
    }

    public boolean needSoBaiEndGame() {
        GamePlayer gp = this.getPlayerByChair(this.chuongChair);
        return this.gameMgr.gameAction == 4 && gp.camChuong && !gp.canRutBai();
    }

    public void xetBaiAll(User user) {
        GamePlayer gpChuong = this.getPlayerByUser(user);
        if (!gpChuong.isPlaying() || !gpChuong.camChuong || !gpChuong.canXetBai() || this.gameMgr.gameAction != 4 && this.gameMgr.gameAction != 5) {
            return;
        }
        gpChuong.active = true;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.camChuong || gp.hasSoBai || !gp.hasDanBai) continue;
            gp.hasSoBai = true;
            this.soBaiVoiChuong(gp, gpChuong, false);
        }
        boolean hasSoBaiAll = this.hasSoBaiXongTatCa();
        if (hasSoBaiAll) {
            this.endGame();
        }
    }

    public void soBaiEndGame() {
        GamePlayer gpChuong = this.getPlayerByChair(this.chuongChair);
        if (!(gpChuong.isPlaying() && gpChuong.camChuong && gpChuong.canXetBai())) {
            return;
        }
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.camChuong || gp.hasSoBai) continue;
            gp.hasSoBai = true;
            this.soBaiVoiChuong(gp, gpChuong, false);
        }
        boolean hasSoBaiAll = this.hasSoBaiXongTatCa();
        if (hasSoBaiAll) {
            this.endGame();
        }
    }

    public void xetBaiXiZach() {
        int i;
        GamePlayer gp;
        GamePlayer gpChuong = this.getPlayerByChair(this.chuongChair);
        if (gpChuong == null || !gpChuong.camChuong || !gpChuong.isPlaying()) {
            return;
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp == null) continue;
            gp.needShowBai = false;
            gp.needUpdateXizach = false;
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp == null || !gp.isPlaying() || gp.hasSoBai || gp.camChuong || !gp.spInfo.handCards.isXiZach() && !gp.spInfo.handCards.isXiBang() && !gpChuong.spInfo.handCards.isXiBang() && !gpChuong.spInfo.handCards.isXiZach()) continue;
            gp.hasSoBai = true;
            gp.hasDanBai = true;
            gp.needShowBai = true;
            gp.needUpdateXizach = true;
            gpChuong.needUpdateXizach = true;
            this.soBaiVoiChuong(gp, gpChuong, false);
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp == null || !gp.isPlaying() || gp.hasSoBai || gp.camChuong || !gp.spInfo.handCards.isXiZach() && !gp.spInfo.handCards.isXiBang()) continue;
            gp.boQuaNotActive = true;
        }
        if (gpChuong.spInfo.handCards.isXiZach() || gpChuong.spInfo.handCards.isXiBang()) {
            for (i = 0; i < 6; ++i) {
                gp = this.getPlayerByChair(i);
                if (gp == null) continue;
                gp.boQuaNotActive = true;
            }
        }
        this.congTruTienXiZach();
        boolean hasSoBaiAll = this.hasSoBaiXongTatCa();
        if (hasSoBaiAll) {
            this.endGame();
        }
    }

    public boolean checkChair(int chair) {
        return chair >= 0 && chair < 6;
    }

    private boolean hasSoBaiXongTatCa() {
        boolean res = true;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.camChuong || gp.hasSoBai) continue;
            return false;
        }
        return res;
    }

    private void notifyDanBaiThanhCong(GamePlayer gp) {
        SendDanBai msg = new SendDanBai();
        msg.chair = (byte)gp.chair;
        this.send(msg);
    }

    private void notifyRutBai(GamePlayer gp, Card card, int chair) {
        SendRutBai msg = new SendRutBai();
        msg.chair = (byte)chair;
        msg.card = (byte)card.ID;
        this.send((BaseMsg)msg, gp.getUser());
    }

    private void notifyRutNhieuBai(GamePlayer gp, int chair, List<Card> cards) {
        SendRutNhieuBai msg = new SendRutNhieuBai();
        msg.chair = (byte)chair;
        msg.listCard.clear();
        for (int i = 0; i < cards.size(); ++i) {
            msg.listCard.add(cards.get((int)i).ID);
        }
        if (gp.getUser() != null) {
            this.send((BaseMsg)msg, gp.getUser());
        }
    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 6) {
            GamePlayer gp = new GamePlayer();
            gp.chair = i++;
            this.playerList.add(gp);
        }
        this.init();
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
        return this.gameMgr.gameServer.room.setting.moneyBet;
    }

    private void logEndGame() {
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    public byte getPlayerCount() {
        return (byte)this.playerCount;
    }

    public boolean checkPlayerChair(int chair) {
        return chair >= 0 && chair < 6;
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
        if (this.room.userManager.size() == 0) {
            this.resetPlayDisconnect();
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null) continue;
            gp.pInfo.setIsHold(false);
        }
    }

    public synchronized void onGameUserReturn(User user) {
        int kkk = 5;
        if (user == null) {
            return;
        }
        if (this.room.setting.maxUserPerRoom == 6) {
            for (int i = 0; i < 6; ++i) {
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
                this.sendGameInfo(gp.chair);
                break;
            }
        }
    }

    public void onGameUserDis(User user) {
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
        if (this.room.setting.maxUserPerRoom == 6) {
            for (i = 0; i < 6; ++i) {
                gp = this.playerList.get(i);
                if (gp.getPlayerStatus() == 0 || gp.pInfo == null || gp.pInfo.userId != user.getId()) continue;
                this.gameLog.append("RE<").append(i).append(">");
                if (moneyInfo != null && gp.gameMoneyInfo.sessionId != moneyInfo.sessionId) {
                    ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, -1);
                }
                user.setProperty((Object)USER_CHAIR, (Object)gp.chair);
                gp.user = user;
                gp.reqQuitRoom = false;
                gp.numGameNotActive = 0;
                user.setProperty((Object)"GAME_MONEY_INFO", (Object)gp.gameMoneyInfo);
                if (this.serverState == 1) {
                    this.sendGameInfo(gp.chair);
                } else {
                    this.notifyUserEnter(gp);
                }
                return;
            }
        }
        for (i = 0; i < 6; ++i) {
            GamePlayer newChuong;
            int kqNewChuong;
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
                kqNewChuong = this.timNewChuong();
                if (kqNewChuong >= 0) {
                    this.chuongChair = kqNewChuong;
                    newChuong = this.getPlayerByChair(this.chuongChair);
                    this.hasChuong = true;
                    this.doiChuong(newChuong, false);
                }
                this.init();
            }
            if (this.playerCount == 2) {
                this.gameMgr.roomCreatorUserId = user.getId();
                this.gameMgr.roomOwnerChair = i;
                if (!this.hasChuong && (kqNewChuong = this.timNewChuong()) >= 0) {
                    this.chuongChair = kqNewChuong;
                    newChuong = this.getPlayerByChair(this.chuongChair);
                    this.hasChuong = true;
                    this.doiChuong(newChuong, false);
                }
            }
            if (this.serverState == 1) {
                this.sendGameInfo(gp.chair);
                this.notifyUserEnterExceptMe(gp);
                break;
            }
            this.notifyUserEnter(gp);
            break;
        }
    }

    private GamePlayer timNguoiNhieuTien() {
        GamePlayer maxPlayer = null;
        long maxMoney = 0L;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.hasUser()) continue;
            if (maxPlayer == null) {
                maxPlayer = gp;
                maxMoney = gp.gameMoneyInfo.getCurrentMoneyFromCache();
                continue;
            }
            long currentMoney = gp.gameMoneyInfo.getCurrentMoneyFromCache();
            if (maxMoney >= currentMoney) continue;
            maxMoney = currentMoney;
            maxPlayer = gp;
        }
        return maxPlayer;
    }

    private void doiChuong(GamePlayer gp, boolean notify) {
        this.chuongChair = this.newChuongChair = gp.chair;
        gp.camChuong = true;
        this.hasChuong = true;
        for (int i = 0; i < 6; ++i) {
            if (i == gp.chair) continue;
            this.getPlayerByChair((int)i).camChuong = false;
        }
        if (notify) {
            SendDoiChuong msg = new SendDoiChuong();
            msg.chair = this.chuongChair;
            this.send(msg);
        }
    }

    private synchronized int timNewChuong() {
        Object gc = null;
        GamePlayer gpNew = this.getPlayerByChair(this.newChuongChair);
        if (gpNew != null && gpNew.hasUser() && this.duTienLamChuong(gpNew)) {
            return this.newChuongChair;
        }
        for (int i = 0; i < 6; ++i) {
            long currentMoney;
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp == null || !gp.hasUser() || (currentMoney = gp.gameMoneyInfo.getCurrentMoneyFromCache()) < 25L * this.getMoneyBet()) continue;
            return i;
        }
        return -1;
    }

    public boolean tuDongRutBai() {
        boolean needMoreTime = false;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp != null && gp.isPlaying() && !gp.camChuong && !gp.canDanBai()) {
                needMoreTime = true;
                ArrayList<Card> cardAdd = new ArrayList<Card>();
                while (!gp.canDanBai()) {
                    Card card = gp.spInfo.rutCard();
                    cardAdd.add(card);
                    this.gameLog.append("RB<");
                    this.gameLog.append(i).append("/");
                    this.gameLog.append("#T:");
                    this.gameLog.append(card.toString()).append("$;");
                    this.gameLog.append(">");
                }
                gp.hasDanBai = true;
                for (int j = 0; j < 5; ++j) {
                    GamePlayer gp1 = this.getPlayerByChair(j);
                    if (j == i) {
                        this.notifyRutNhieuBai(gp1, gp.chair, cardAdd);
                        continue;
                    }
                    ArrayList<Card> card52s = new ArrayList<Card>();
                    for (int kk = 0; kk < cardAdd.size(); ++kk) {
                        card52s.add(Card.createCard(52));
                    }
                    this.notifyRutNhieuBai(gp1, gp.chair, card52s);
                }
                continue;
            }
            if (gp == null || !gp.isPlaying() || gp.camChuong) continue;
            gp.hasDanBai = true;
        }
        return needMoreTime;
    }

    public boolean tuRutBaiChuong() {
        boolean needMoreTime = false;
        GamePlayer gp = this.getPlayerByChair(this.chuongChair);
        if (gp != null && gp.isPlaying() && !gp.canDanBai()) {
            needMoreTime = true;
            ArrayList<Card> cardAdd = new ArrayList<Card>();
            while (!gp.canDanBai()) {
                Card card = this.gameMgr.game.suit.rutBai(gp.spInfo.handCards);
                cardAdd.add(card);
                this.gameLog.append("RB<");
                this.gameLog.append(this.chuongChair).append("/");
                this.gameLog.append("#T:");
                this.gameLog.append(card.toString()).append("$;");
                this.gameLog.append(">");
            }
            gp.hasDanBai = true;
            for (int j = 0; j < 5; ++j) {
                GamePlayer gp1 = this.getPlayerByChair(j);
                if (gp1 == null || !gp1.hasUser()) continue;
                this.notifyRutNhieuBai(gp1, gp.chair, cardAdd);
            }
        }
        return needMoreTime;
    }

    private boolean duTienLamChuong(GamePlayer gp) {
        long currentMoney;
        return gp != null && gp.hasUser() && (currentMoney = gp.gameMoneyInfo.getCurrentMoneyFromCache()) > 25L * this.getMoneyBet();
    }

    private synchronized boolean kiemTraDoiChuong(boolean force) {
        GamePlayer newChuong = this.getPlayerByChair(this.newChuongChair);
        if (newChuong != null && newChuong.hasUser()) {
            if (this.chuongChair == newChuong.chair) {
                return true;
            }
            long newChuongCurrentMoney = newChuong.gameMoneyInfo.getCurrentMoneyFromCache();
            if (newChuongCurrentMoney >= 30L * this.getMoneyBet()) {
                this.doiChuong(newChuong, true);
                return true;
            }
        }
        if (force) {
            newChuong = this.timNguoiNhieuTien();
            if (newChuong == null || this.chuongChair == newChuong.chair) {
                return true;
            }
            this.doiChuong(newChuong, true);
            return true;
        }
        return false;
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
        SendDealCard msg = new SendDealCard();
        msg.gameId = this.gameMgr.game.id;
        msg.chuongChair = (byte)this.gameMgr.gameServer.chuongChair;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.storageCards.toString()).append("/");
            this.gameLog.append(gp.spInfo.handCards.kiemTraBo()).append(";");
            for (int j = 0; j < 6; ++j) {
                GamePlayer gp1 = this.getPlayerByChair(j);
                msg.playerStatus[j] = (byte)gp1.getPlayerStatus();
            }
            this.send((BaseMsg)msg, user);
        }
        this.gameLog.append(">");
    }

    public void chuyenGiaiDoan2() {
        SendChuyenGiaiDoan2 msg = new SendChuyenGiaiDoan2();
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.hasUser()) continue;
            User user = gp.getUser();
            this.send((BaseMsg)msg, user);
        }
    }

    public void chuyenGiaiDoan3() {
        SendChuyenGiaiDoan3 msg = new SendChuyenGiaiDoan3();
        msg.chuongGp = this.getPlayerByChair(this.chuongChair);
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (gp != null && i != this.chuongChair) {
                gp.hasDanBai = true;
            }
            if (!gp.hasUser()) continue;
            User user = gp.getUser();
            this.send((BaseMsg)msg, user);
        }
    }

    public void start() {
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        this.serverState = 1;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            gp.tuDongChoiNhanh = 0;
            gp.prepareNewGame();
            if (!this.coTheChoiTiep(gp)) continue;
            gp.setPlayerStatus(3);
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)gp.pInfo.nickName, (int)this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(i).append(";");
            gp.choiTiepVanSau = false;
        }
        this.gameLog.append(this.room.setting.moneyType).append(";");
        this.gameLog.append(this.getMoneyBet()).append(";");
        this.gameLog.append(this.chuongChair).append(";");
        this.gameLog.append(">");
        this.gameMgr.chiaBai();
        this.logStartGame();
        this.gameMgr.gameAction = 1;
        this.gameMgr.countDown = 2;
        this.botStartGame();
    }

    public void botJoinRoom() {
        if (this.room.setting.moneyType == 1 && this.playerCount < 6) {
            int x = BotManager.instance().getRandomNumber(10);
            BotManager.instance().regJoinRoom(this.room, x);
        }
    }

    private void botStartGame() {
        if (!GameUtils.isBot || this.room.setting.moneyType != 1 || this.room.setting.password.length() > 0) {
            return;
        }
        int botCount = 0;
        int userCount = 0;
        boolean flag = false;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            User user = gp.getUser();
            gp.countDownRutBai = gp.kiemTraBotCanRutBai() > 0 ? BotManager.instance().getRandomNumber(5) + 2 : -1;
            if (user != null && user.isBot()) {
                int x;
                ++botCount;
                Bot bot = BotManager.instance().getBotByName(user.getName());
                ++bot.count;
                if (bot.count < 15 || this.playerCount < 6 || (x = BotManager.instance().getRandomNumber(2)) != 0 || flag) continue;
                flag = true;
                gp.yeuCauBotRoiPhong = x = BotManager.instance().getRandomNumber(10);
                continue;
            }
            if (user == null) continue;
            ++userCount;
        }
        if (botCount >= 4) {
            GamePlayer gp;
            int random;
            int out = BotManager.instance().getRandomNumber(2);
            if ((out == 0 || botCount == 6) && (gp = this.getPlayerByChair(random = BotManager.instance().getRandomNumber(6))).getUser() != null && gp.getUser().isBot()) {
                this.pOutRoom(gp);
            }
        } else {
            int x = BotManager.instance().getRandomNumber(1);
            if (this.playerCount < 6 && x == 0) {
                int after = GameUtils.rd.nextInt(15) + 5;
                BotManager.instance().regJoinRoom(this.room, after);
            }
        }
    }

    public void botAutoPlay() {
        int random;
        int i;
        GamePlayer gp;
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (this.gameMgr.countDown <= 0 || gp.yeuCauBotRoiPhong != this.gameMgr.countDown || gp.getUser() == null || !gp.getUser().isBot()) continue;
            this.pOutRoom(gp);
            gp.yeuCauBotRoiPhong = -1;
        }
        if (this.gameMgr.gameAction == 2) {
            for (i = 0; i < 6; ++i) {
                gp = this.getPlayerByChair(i);
                if (!gp.isPlaying() || !gp.isBot() || gp.camChuong) continue;
                if (gp.countDownRutBai >= 0 && gp.countDownRutBai <= 20 - this.gameMgr.countDown) {
                    int time = this.botRutBai(gp.getUser());
                    if (time > 0) {
                        gp.countDownRutBai -= time;
                        if (gp.countDownRutBai >= 0) continue;
                        gp.countDownRutBai = 0;
                        continue;
                    }
                    gp.countDownRutBai = time;
                    continue;
                }
                if (gp.countDownRutBai >= 0 || (random = BotManager.instance().getRandomNumber(4)) != 1 && this.gameMgr.countDown > 1) continue;
                this.danBai(gp.getUser());
            }
        }
        if (this.gameMgr.gameAction == 4) {
            block2 : for (i = 0; i < 6; ++i) {
                int size;
                GamePlayer gp1;
                int j;
                gp = this.getPlayerByChair(i);
                if (!gp.isPlaying() || !gp.isBot() || !gp.camChuong) continue;
                if (gp.countDownRutBai >= 0 && gp.countDownRutBai <= 30 - this.gameMgr.countDown) {
                    random = BotManager.instance().getRandomNumber(10);
                    if (random >= 3 || gp.spInfo.handCards.isDanNon()) {
                        int time = this.botRutBai(gp.getUser());
                        if (time > 0) {
                            gp.countDownRutBai -= time;
                            if (gp.countDownRutBai >= 0) continue;
                            gp.countDownRutBai = 0;
                            continue;
                        }
                        gp.countDownRutBai = time;
                        continue;
                    }
                    if (gp.spInfo.handCards.isDanNon()) continue;
                    random = BotManager.instance().getRandomNumber(10);
                    if (random == 0) {
                        this.xetBaiAll(gp.getUser());
                    }
                    for (j = 0; j < 6; ++j) {
                        gp1 = this.getPlayerByChair(j);
                        if (!gp1.isPlaying() || gp1.camChuong || gp1.hasSoBai) continue;
                        size = gp1.spInfo.handCards.cards.size();
                        random = BotManager.instance().getRandomNumber(10);
                        if (size == 2 && random == 0) {
                            this.xetBaiOne(gp.getUser(), gp1.chair);
                            continue block2;
                        }
                        if (size == 3 && random >= 7) {
                            this.xetBaiOne(gp.getUser(), gp1.chair);
                            continue block2;
                        }
                        if (size != 4 || random < 6) continue;
                        this.xetBaiOne(gp.getUser(), gp1.chair);
                        continue block2;
                    }
                    continue;
                }
                if (gp.countDownRutBai >= 0) continue;
                random = BotManager.instance().getRandomNumber(4);
                if (random == 1 || this.gameMgr.countDown <= 1) {
                    this.xetBaiAll(gp.getUser());
                    continue;
                }
                for (j = 0; j < 6; ++j) {
                    gp1 = this.getPlayerByChair(j);
                    if (!gp1.isPlaying() || gp1.camChuong || gp1.hasSoBai) continue;
                    size = gp1.spInfo.handCards.cards.size();
                    random = BotManager.instance().getRandomNumber(10);
                    if (size == 2 && random == 0) {
                        this.xetBaiOne(gp.getUser(), gp1.chair);
                        continue block2;
                    }
                    if (size == 3 && random >= 7) {
                        this.xetBaiOne(gp.getUser(), gp1.chair);
                        continue block2;
                    }
                    if (size != 4 || random < 6) continue;
                    this.xetBaiOne(gp.getUser(), gp1.chair);
                    continue block2;
                }
            }
        }
    }

    private void logStartGame() {
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public int demSoNguoiChoiTiep() {
        int count = 0;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            ++count;
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

    public void kiemTraTuDongBatDau(int after) {
        GamePlayer chuong = this.getPlayerByChair(this.chuongChair);
        if (chuong != null && chuong.hasUser() && this.duTienLamChuong(chuong)) {
            this.hasChuong = true;
            chuong.camChuong = true;
        } else {
            int newChuong;
            this.hasChuong = false;
            if (chuong != null) {
                chuong.camChuong = false;
            }
            if ((newChuong = this.timNewChuong()) >= 0) {
                this.hasChuong = true;
                this.chuongChair = newChuong;
                chuong = this.getPlayerByChair(this.chuongChair);
                chuong.camChuong = true;
            }
        }
        if (this.gameMgr.gameState == 0) {
            if (this.demSoNguoiChoiTiep() < 2 || !this.hasChuong) {
                this.gameMgr.cancelAutoStart();
                if (this.demSoNguoiChoiTiep() >= 2 && !this.hasChuong) {
                    this.gameMgr.notifyKhongCoChuong();
                }
            } else {
                this.gameMgr.makeAutoStart(after);
            }
        }
    }

    private boolean coTheChoiTiep(GamePlayer gp) {
        return gp.hasUser() && gp.canPlayNextGame();
    }

    private synchronized void removePlayerAtChair(int chair, boolean disconnect) {
        GamePlayer gp = this.getPlayerByChair(chair);
        gp.choiTiepVanSau = true;
        gp.reqQuitRoom = false;
        gp.numGameNotActive = 0;
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
        if (gp.camChuong) {
            this.kiemTraDoiChuong(true);
        }
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
        this.sendMsgExceptMe((BaseMsg)msg, user);
        this.notifyJoinRoomSuccess(gamePlayer);
    }

    private void notifyUserEnterExceptMe(GamePlayer gamePlayer) {
        User user = gamePlayer.getUser();
        if (user == null) {
            return;
        }
        SendNewUserJoin msg = new SendNewUserJoin();
        msg.money = gamePlayer.gameMoneyInfo.currentMoney;
        msg.uStatus = gamePlayer.getPlayerStatus();
        msg.setBaseInfo(gamePlayer.pInfo);
        msg.uChair = gamePlayer.chair;
        this.sendMsgExceptMe((BaseMsg)msg, user);
    }

    public void notifyJoinRoomSuccess(GamePlayer gamePlayer) {
        SendJoinRoomSuccess msg = new SendJoinRoomSuccess();
        msg.chuongChair = this.chuongChair;
        msg.hasChuong = this.hasChuong;
        msg.uChair = gamePlayer.chair;
        msg.roomId = this.room.getId();
        msg.moneyType = this.gameMgr.gameServer.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.gameMgr.gameServer.room.setting.moneyBet;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
            msg.playerList[i] = gp.getPlayerInfo();
            msg.moneyInfoList[i] = gp.gameMoneyInfo;
        }
        msg.gameAction = (byte)this.gameMgr.gameAction;
        msg.countDownTime = (byte)this.gameMgr.countDown;
        this.send((BaseMsg)msg, gamePlayer.getUser());
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
        int i;
        GamePlayer gp;
        GamePlayer gamePlayer = this.getPlayerByChair(chair);
        SendGameInfo msg = new SendGameInfo();
        msg.gameState = this.gameMgr.gameState;
        msg.isAutoStart = this.gameMgr.isAutoStart;
        msg.gameAction = this.gameMgr.gameAction;
        msg.gameState = this.gameMgr.gameState;
        msg.countdownTime = this.gameMgr.countDown;
        msg.chair = (byte)gamePlayer.chair;
        msg.chuongChair = (byte)this.chuongChair;
        msg.moneyType = this.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.roomId = this.room.getId();
        msg.moneyBet = this.getMoneyBet();
        String sStatus = "";
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.hasUser()) {
                msg.pInfos[i] = gp;
                msg.playerStatusList[i] = gp.getPlayerStatus();
            } else {
                msg.playerStatusList[i] = 0;
            }
            sStatus = sStatus + msg.playerStatusList[i];
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp == null || !gp.hasUser() || !gp.isPlaying()) continue;
            msg.cardList[i] = i == chair || gp.hasSoBai || (this.gameMgr.gameAction == 4 || this.gameMgr.gameAction == 5 || this.gameMgr.gameState == 2) && i == this.chuongChair ? gp.spInfo.handCards.toByteArray() : gp.spInfo.handCards.toByteHiddenArray();
        }
        this.send((BaseMsg)msg, gamePlayer.getUser());
        this.resendEndGame(gamePlayer);
    }

    private void pOutRoom(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        this.pOutRoom(gp);
    }

    private void pOutRoom(GamePlayer gp) {
        if (gp != null) {
            if (gp.getPlayerStatus() == 3) {
                gp.reqQuitRoom = !gp.reqQuitRoom;
                this.notifyRegisterOutRoom(gp);
            } else {
                GameRoomManager.instance().leaveRoom(gp.getUser(), this.room);
            }
        }
    }

    private void notifyRegisterOutRoom(GamePlayer gp) {
        SendNotifyReqQuitRoom msg = new SendNotifyReqQuitRoom();
        msg.chair = (byte)gp.chair;
        msg.reqQuitRoom = gp.reqQuitRoom;
        this.send(msg);
    }

    private void dispatchAddEventScore(User user, UserScore score) {
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

    public boolean[] hasInfoAt() {
        boolean[] hasInfoAt = new boolean[6];
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            hasInfoAt[i] = gp.isPlaying();
        }
        return hasInfoAt;
    }

    private void notifyKickRoom(GamePlayer gp, int reason) {
        SendKickRoom msg = new SendKickRoom();
        msg.reason = (byte)reason;
        this.send((BaseMsg)msg, gp.getUser());
    }

    public void pPrepareNewGame() {
        int i;
        GamePlayer gp;
        LoggerUtils.debug((String)"binh", (Object[])new Object[]{"pPrepareNewGame: -----------START----------------------"});
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 0) {
                if (GameUtils.isMainTain) {
                    gp.reqQuitRoom = true;
                    this.notifyKickRoom(gp, 2);
                }
                if (!this.coTheChoiTiep(gp)) {
                    if (!gp.checkMoneyCanPlay()) {
                        this.notifyKickRoom(gp, 1);
                    }
                    if (gp.getUser() != null && this.room != null) {
                        GameRoom gameRoom = (GameRoom)gp.getUser().getProperty((Object)"GAME_ROOM");
                        if (gameRoom == this.room) {
                            GameRoomManager.instance().leaveRoom(gp.getUser());
                            if (!gp.getUser().isBot()) {
                                LoggerUtils.debug((String)"binh", (Object[])new Object[]{"leaveRoom", gp.getUser().getName()});
                            }
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
                continue;
            }
            msg.hasInfoAtChair[i] = false;
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.kiemTraDoiChuong(false);
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
        LoggerUtils.debug((String)"binh", (Object[])new Object[]{"pPrepareNewGame: -----------END----------------------"});
    }

    public void xuLiNoHu() {
        Random rd = new Random();
        int random = rd.nextInt(1000);
        if (random != 0) {
            return;
        }
        if (this.room.setting.moneyType == 1) {
            if (this.room.setting.moneyBet < 1000L) {
                int chair = Math.abs(rd.nextInt() % this.playerCount);
                GamePlayer gp = this.getPlayerByChair(chair);
                if (gp.hasUser() && gp.getUser() != null) {
                    this.choNoHu(gp.getUser().getName());
                }
            } else {
                for (int i = 0; i < 6; ++i) {
                    GamePlayer gp = this.getPlayerByChair(i);
                    if (gp.getUser() == null || !gp.getUser().isBot()) continue;
                    this.choNoHu(gp.getUser().getName());
                }
            }
        }
    }

    private void pBatDau(User user, DataCmd dataCmd) {
        int nextGamePlayerCount = this.demSoNguoiChoiTiep();
        if (nextGamePlayerCount >= 2) {
            this.gameMgr.makeAutoStart(0);
        }
    }

    public void tudongChoi() {
    }

    private void pCheatCards(User user, DataCmd dataCmd) {
        if (!GameUtils.isCheat) {
            return;
        }
        RevCheatCard cmd = new RevCheatCard(dataCmd);
        if (cmd.isCheat) {
            this.gameMgr.game.isCheat = true;
            this.gameMgr.game.suit.setOrder(cmd.cards);
        } else {
            this.gameMgr.game.isCheat = false;
            this.gameMgr.game.suit.initCard();
        }
    }

    private void pDangKyChoiTiep(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.choiTiepVanSau = true;
        }
    }

    public void endGame() {
        GamePlayer gpChuong = this.getPlayerByChair(this.chuongChair);
        if (gpChuong.isPlaying() && gpChuong.camChuong && gpChuong.canXetBai()) {
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (!gp.isPlaying() || gp.camChuong || gp.hasSoBai) continue;
                gp.hasSoBai = true;
                this.soBaiVoiChuong(gp, gpChuong, false);
            }
        }
        boolean hasSoBaiAll = this.hasSoBaiXongTatCa();
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp1 = this.getPlayerByChair(i);
            this.kiemTraKickKhoiPhongVanSauViKhongHoatDong(gp1);
        }
        if (hasSoBaiAll) {
            this.congTruTienCuoiVan();
            this.notifyEndGame();
            this.logEndGame();
            this.timNguoiDoiChuong();
            this.gameMgr.countDown = 12;
            this.gameMgr.gameState = 2;
            this.gameMgr.gameAction = 0;
        }
    }

    public void congTruTienXiZach() {
        int i;
        boolean needSend = false;
        for (int i2 = 0; i2 < 6; ++i2) {
            GamePlayer gp = this.getPlayerByChair(i2);
            if (!gp.isPlaying() || gp.spRes.currentTienThang == 0L) continue;
            needSend = true;
        }
        if (!needSend) {
            return;
        }
        long sumMoneyLost = 0L;
        for (int i3 = 0; i3 < 6; ++i3) {
            GamePlayer gp = this.getPlayerByChair(i3);
            if (!gp.isPlaying() || gp.camChuong || gp.spRes.currentTienThang >= 0L) continue;
            UserScore score = new UserScore();
            score.money = gp.spRes.currentTienThang;
            --score.lostCount;
            GameMoneyInfo pLost = gp.gameMoneyInfo;
            long moneyLost = 0L;
            try {
                moneyLost = pLost.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                sumMoneyLost += moneyLost;
                score.money = moneyLost;
                this.dispatchAddEventScore(gp.getUser(), score);
            }
            catch (MoneyException e) {
                moneyLost = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            this.gameLog.append("CT<");
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(moneyLost).append(";");
            this.gameLog.append(">");
            gp.spRes.currentTienThang = moneyLost;
        }
        long chuongThang = -sumMoneyLost;
        for (int i4 = 0; i4 < 6; ++i4) {
            GamePlayer gp = this.getPlayerByChair(i4);
            if (gp.camChuong || gp.spRes.currentTienThang < 0L) continue;
            chuongThang -= gp.spRes.currentTienThang;
        }
        UserScore score = new UserScore();
        long moneyWasted = 0L;
        if (chuongThang > 0L) {
            moneyWasted = (long)((double)(chuongThang * (long)this.room.setting.commisionRate) / 100.0);
            chuongThang -= moneyWasted;
        } else {
            moneyWasted = 0L;
        }
        score.money = chuongThang;
        score.wastedMoney = moneyWasted;
        GamePlayer gpChuong = this.getPlayerByChair(this.chuongChair);
        GameMoneyInfo gpInfo = gpChuong.gameMoneyInfo;
        if (gpChuong.spRes.tongTienCuoiVan > 0L) {
            ++score.winCount;
        } else if (gpChuong.spRes.tongTienCuoiVan < 0L) {
            ++score.lostCount;
        }
        try {
            score.money = chuongThang = gpInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            this.dispatchAddEventScore(gpChuong.getUser(), score);
            this.gameLog.append("CT<");
            this.gameLog.append(gpChuong.chair).append("/");
            this.gameLog.append(chuongThang).append(";");
            this.gameLog.append(">");
            gpChuong.spRes.currentTienThang = chuongThang;
            for (i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                GameMoneyInfo pWin = gp.gameMoneyInfo;
                if (!gp.isPlaying() || gp.spRes.currentTienThang < 0L || gp.camChuong) continue;
                UserScore gamePlayerScore = new UserScore();
                gamePlayerScore.wastedMoney = (long)((double)(gp.spRes.currentTienThang * (long)this.room.setting.commisionRate) / 100.0);
                gamePlayerScore.money = gp.spRes.currentTienThang - gamePlayerScore.wastedMoney;
                if (gp.spRes.currentTienThang > 0L) {
                    ++gamePlayerScore.winCount;
                }
                long moneyWin = 0L;
                try {
                    score.money = moneyWin = pWin.chargeMoneyInGame(gamePlayerScore, this.room.getId(), this.gameMgr.game.id);
                    this.dispatchAddEventScore(gp.getUser(), score);
                }
                catch (MoneyException e) {
                    moneyWin = 0L;
                }
                gp.spRes.currentTienThang = moneyWin;
                this.gameLog.append("CT<");
                this.gameLog.append(gp.chair).append("/");
                this.gameLog.append(moneyWin).append(";");
                this.gameLog.append(">");
            }
        }
        catch (MoneyException e) {
            chuongThang = 0L;
            gpChuong.spRes.currentTienThang = 0L;
            for (int i5 = 0; i5 < 6; ++i5) {
                GamePlayer gp = this.getPlayerByChair(i5);
                GameMoneyInfo pWin = gp.gameMoneyInfo;
                if (!gp.isPlaying() || gp.spRes.currentTienThang < 0L) continue;
                gp.spRes.currentTienThang = 0L;
            }
        }
        this.notifyKetQuaXiZach();
        for (i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            gp.spRes.currentTienThang = 0L;
            gp.needShowWinLostMoney = false;
        }
    }

    public void congTruTienCuoiVan() {
        long moneyWaste;
        long sumMoneyLost = 0L;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.camChuong || gp.spRes.currentTienThang >= 0L) continue;
            UserScore score = new UserScore();
            score.money = gp.spRes.currentTienThang;
            --score.lostCount;
            GameMoneyInfo pLost = gp.gameMoneyInfo;
            long moneyLost = 0L;
            try {
                score.money = moneyLost = pLost.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                this.dispatchAddEventScore(gp.getUser(), score);
                sumMoneyLost += moneyLost;
            }
            catch (MoneyException e) {
                moneyLost = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            this.gameLog.append("CT<");
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(moneyLost).append(";");
            this.gameLog.append(">");
            gp.spRes.currentTienThang = moneyLost;
        }
        long chuongThang = -sumMoneyLost;
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.camChuong || gp.spRes.currentTienThang < 0L) continue;
            chuongThang -= gp.spRes.currentTienThang;
        }
        UserScore score = new UserScore();
        long moneyWasted = 0L;
        if (chuongThang > 0L) {
            moneyWaste = (long)((double)(chuongThang * (long)this.room.setting.commisionRate) / 100.0);
            chuongThang -= moneyWaste;
        } else {
            moneyWaste = 0L;
        }
        score.money = chuongThang;
        score.wastedMoney = moneyWasted;
        GamePlayer gpChuong = this.getPlayerByChair(this.chuongChair);
        GameMoneyInfo gpInfo = gpChuong.gameMoneyInfo;
        if (gpChuong.spRes.tongTienCuoiVan > 0L) {
            ++score.winCount;
        } else if (gpChuong.spRes.tongTienCuoiVan < 0L) {
            ++score.lostCount;
        }
        try {
            score.money = chuongThang = gpInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            this.dispatchAddEventScore(gpChuong.getUser(), score);
            this.gameLog.append("CT<");
            this.gameLog.append(gpChuong.chair).append("/");
            this.gameLog.append(chuongThang).append(";");
            this.gameLog.append(">");
            gpChuong.spRes.currentTienThang = chuongThang;
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                GameMoneyInfo pWin = gp.gameMoneyInfo;
                if (!gp.isPlaying() || gp.spRes.currentTienThang < 0L || gp.camChuong) continue;
                UserScore gamePlayerScore = new UserScore();
                gamePlayerScore.wastedMoney = (long)((double)(gp.spRes.currentTienThang * (long)this.room.setting.commisionRate) / 100.0);
                gamePlayerScore.money = gp.spRes.currentTienThang - gamePlayerScore.wastedMoney;
                if (gp.spRes.currentTienThang > 0L) {
                    ++gamePlayerScore.winCount;
                }
                long moneyWin = 0L;
                try {
                    score.money = moneyWin = pWin.chargeMoneyInGame(gamePlayerScore, this.room.getId(), this.gameMgr.game.id);
                    this.dispatchAddEventScore(gp.getUser(), score);
                }
                catch (MoneyException e) {
                    moneyWin = 0L;
                }
                gp.spRes.currentTienThang = moneyWin;
                this.gameLog.append("CT<");
                this.gameLog.append(gp.chair).append("/");
                this.gameLog.append(moneyWin).append(";");
                this.gameLog.append(">");
            }
        }
        catch (MoneyException e) {
            chuongThang = 0L;
            gpChuong.spRes.currentTienThang = 0L;
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                GameMoneyInfo pWin = gp.gameMoneyInfo;
                if (!gp.isPlaying() || gp.spRes.currentTienThang < 0L) continue;
                gp.spRes.currentTienThang = 0L;
            }
        }
    }

    private void kiemTraKickKhoiPhongVanSauViKhongHoatDong(GamePlayer gp) {
        if (!gp.isPlaying()) {
            return;
        }
        if (!gp.active) {
            ++gp.numGameNotActive;
            if (gp.numGameNotActive >= 3 && !gp.isBot()) {
                gp.reqQuitRoom = true;
            }
        } else {
            gp.numGameNotActive = 0;
        }
    }

    private void timNguoiDoiChuong() {
        GroupCard newGc;
        int i;
        GamePlayer gp;
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || (newGc = gp.spInfo.handCards).kiemTraBo() != 7 || !this.duTienLamChuong(gp)) continue;
            this.newChuongChair = i;
        }
        for (i = 0; i < 6; ++i) {
            gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || (newGc = gp.spInfo.handCards).kiemTraBo() != 6 || !this.duTienLamChuong(gp)) continue;
            this.newChuongChair = i;
        }
    }

    private void notifyKetQuaXiZach() {
        for (int j = 0; j < 6; ++j) {
            SendKetQuaXiZach msg = new SendKetQuaXiZach();
            GamePlayer gp1 = this.getPlayerByChair(j);
            if (!gp1.hasUser()) continue;
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp;
                msg.gamePlayers[i] = gp = this.getPlayerByChair(i);
                if (gp.isPlaying()) {
                    msg.gamePlayers[i] = gp;
                    msg.winMoneyList.add(gp.spRes.currentTienThang);
                    msg.currentMoneyList.add(gp.gameMoneyInfo.getCurrentMoneyFromCache());
                    msg.needShowCard.add(gp.needShowBai);
                    msg.needUpdateXizach.add(gp.needUpdateXizach);
                    continue;
                }
                msg.winMoneyList.add(0L);
                msg.currentMoneyList.add(0L);
                msg.needShowCard.add(false);
                msg.needUpdateXizach.add(false);
                msg.gamePlayers[i] = gp;
            }
            this.send((BaseMsg)msg, gp1.getUser());
        }
    }

    private void notifyEndGame() {
        for (int j = 0; j < 6; ++j) {
            SendEndGame msg = new SendEndGame();
            GamePlayer gp1 = this.getPlayerByChair(j);
            if (!gp1.hasUser()) continue;
            for (int i = 0; i < 6; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (gp.isPlaying()) {
                    msg.tongKetThangThua.add(gp.spRes.currentTienThang);
                    msg.currentMoneyList.add(gp.gameMoneyInfo.getCurrentMoneyFromCache());
                    msg.gamePlayers[i] = gp;
                    msg.playerStatus[i] = gp.getPlayerStatus();
                    msg.winMoneyList.add(gp.spRes.tongTienCuoiVan);
                    msg.needShowWinLostMoney.add(gp.needShowWinLostMoney);
                    continue;
                }
                msg.needShowWinLostMoney.add(false);
                msg.playerStatus[i] = gp.getPlayerStatus();
                msg.tongKetThangThua.add(0L);
                msg.currentMoneyList.add(0L);
                msg.gamePlayers[i] = gp;
            }
            msg.result = !gp1.isPlaying() ? null : gp1.spRes;
            this.send((BaseMsg)msg, gp1.getUser());
        }
        this.gameLog.append("KT<");
        this.gameLog.append(">");
    }

    private void resendEndGame(GamePlayer reconnectPlayer) {
        if (!reconnectPlayer.isPlaying() || this.gameMgr.gameState != 2) {
            return;
        }
        SendEndGame msg = new SendEndGame();
        for (int i = 0; i < 6; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
                msg.tongKetThangThua.add(gp.spRes.tongTienCuoiVan);
                msg.currentMoneyList.add(gp.gameMoneyInfo.getCurrentMoneyFromCache());
                msg.gamePlayers[i] = gp;
                msg.playerStatus[i] = gp.getPlayerStatus();
                continue;
            }
            msg.playerStatus[i] = gp.getPlayerStatus();
            msg.tongKetThangThua.add(0L);
            msg.currentMoneyList.add(0L);
            msg.gamePlayers[i] = gp;
        }
        SendEndGame newMsg = new SendEndGame();
        msg.result = reconnectPlayer.spRes;
        newMsg.copyData(msg);
        this.send((BaseMsg)newMsg, reconnectPlayer.getUser());
    }

    private void soBaiVoiChuong(GamePlayer gp, GamePlayer chuong, boolean needSend) {
        GroupCard gc = gp.spInfo.handCards;
        GroupCard chuongGc = chuong.spInfo.handCards;
        sResultInfo res = gp.spRes;
        sResultInfo chuongRes = chuong.spRes;
        this.gameLog.append("SB<");
        long v = (long)XiZachRule.soSanhBai(gc, chuongGc) * this.getMoneyBet();
        this.gameLog.append(gp.chair).append("/");
        this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
        this.gameLog.append(gp.spInfo.handCards.kiemTraBo()).append(";");
        this.gameLog.append(chuong.chair).append("/");
        this.gameLog.append(chuong.spInfo.handCards.toString()).append("/");
        this.gameLog.append(chuong.spInfo.handCards.kiemTraBo()).append(";");
        this.gameLog.append(v).append(";");
        this.gameLog.append(">");
        res.thangChuong = v;
        chuongRes.thangChuong -= v;
        res.tongTienCuoiVan += v;
        chuongRes.tongTienCuoiVan -= v;
        if (needSend) {
            GameMoneyInfo pLost;
            GamePlayer gpWin;
            GameMoneyInfo pWin;
            GamePlayer gpLost;
            long moneyLost = Math.abs(v);
            UserScore score = new UserScore();
            score.money = -moneyLost;
            if (v >= 0L) {
                gpLost = chuong;
                pLost = chuong.gameMoneyInfo;
                gpWin = gp;
                pWin = gp.gameMoneyInfo;
            } else {
                gpLost = gp;
                pLost = gp.gameMoneyInfo;
                gpWin = chuong;
                pWin = chuong.gameMoneyInfo;
                --score.lostCount;
            }
            try {
                moneyLost = pLost.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            }
            catch (MoneyException e) {
                moneyLost = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gpLost.gameMoneyInfo.toString()));
                gpLost.reqQuitRoom = true;
            }
            this.gameLog.append("CT<");
            this.gameLog.append(gpLost.chair).append("/");
            this.gameLog.append(moneyLost).append(";");
            this.gameLog.append(">");
            score.money = moneyLost;
            this.gameLog.append(score.money).append(";");
            this.dispatchAddEventScore(gpLost.getUser(), score);
            long moneyWin = -score.money;
            long moneyWaste = (long)((double)(moneyWin * (long)this.room.setting.commisionRate) / 100.0);
            score.money = moneyWin - moneyWaste;
            score.wastedMoney = moneyWaste;
            if (v > 0L) {
                ++score.winCount;
            }
            try {
                moneyWin = pWin.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            }
            catch (MoneyException e) {
                moneyWin = 0L;
                gpWin.reqQuitRoom = true;
                CommonHandle.writeErrLogDebug((Object[])new Object[]{"ERROR WHEN CHARNGING MONEY:", gpWin.gameMoneyInfo.toString()});
            }
            this.gameLog.append("CT<");
            this.gameLog.append(gpWin.chair).append("/");
            this.gameLog.append(moneyWin).append(";");
            this.gameLog.append(">");
            score.money = moneyWin;
            this.dispatchAddEventScore(gpWin.getUser(), score);
            long winBalance = gpWin.gameMoneyInfo.currentMoney;
            long lostBalance = gpLost.gameMoneyInfo.currentMoney;
            int chairWin = gpWin.chair;
            int chairLost = gpLost.chair;
            gpWin.spRes.tongTienCuoiVan += moneyWin;
            gpLost.spRes.tongTienCuoiVan += moneyLost;
            this.notifyKetQuaSoBai(chairWin, chairLost, moneyWin, moneyLost, winBalance, lostBalance, true, true, false);
        } else {
            res.currentTienThang += v;
            chuongRes.currentTienThang -= v;
            gp.needShowWinLostMoney = true;
            chuong.needShowWinLostMoney = true;
        }
    }

    private void notifyKetQuaSoBai(int chair1, int chair2, long chair1Money, long chair2Money, long chair1Balance, long chair2Balance, boolean hasCard1, boolean hasCard2, boolean isXiZach) {
        SendKetQuaSoBai msg = new SendKetQuaSoBai();
        msg.gp1 = this.getPlayerByChair(chair1);
        msg.gp2 = this.getPlayerByChair(chair2);
        msg.chair1Money = chair1Money;
        msg.chair2Money = chair2Money;
        msg.chair1Balance = chair1Balance;
        msg.chair2Balance = chair2Balance;
        msg.hasCard1 = hasCard1;
        msg.hasCard2 = hasCard2;
        msg.isXiZach = isXiZach;
        this.send(msg);
    }

    public void onNoHu(ThongTinThangLon p1) {
    }

    public void choNoHu(String nickName) {
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
            json.put("players", (Object)arr);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }

    private final class GameLoopTask
    implements Runnable {
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                XiZachGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

