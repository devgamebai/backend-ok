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
package game.baicao.server;

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
import game.baicao.server.GameManager;
import game.baicao.server.GamePlayer;
import game.baicao.server.cmd.receive.RevCheatCard;
import game.baicao.server.cmd.receive.RevDanhBien;
import game.baicao.server.cmd.receive.RevDatCuoc;
import game.baicao.server.cmd.receive.RevDongYDanhBien;
import game.baicao.server.cmd.receive.RevKeCua;
import game.baicao.server.cmd.send.SendDatCuoc;
import game.baicao.server.cmd.send.SendDealCard;
import game.baicao.server.cmd.send.SendDoiChuong;
import game.baicao.server.cmd.send.SendDongYDanhBien;
import game.baicao.server.cmd.send.SendEndGame;
import game.baicao.server.cmd.send.SendGameInfo;
import game.baicao.server.cmd.send.SendJoinRoomSuccess;
import game.baicao.server.cmd.send.SendKeCua;
import game.baicao.server.cmd.send.SendKickRoom;
import game.baicao.server.cmd.send.SendMoBaiSuccess;
import game.baicao.server.cmd.send.SendMoiDatCuoc;
import game.baicao.server.cmd.send.SendNewUserJoin;
import game.baicao.server.cmd.send.SendNotifyReqQuitRoom;
import game.baicao.server.cmd.send.SendUpdateMatch;
import game.baicao.server.cmd.send.SendUpdateOwnerRoom;
import game.baicao.server.cmd.send.SendUserExitRoom;
import game.baicao.server.cmd.send.SendVaoGa;
import game.baicao.server.cmd.send.SendYeuCauDanhBien;
import game.baicao.server.logic.BaicaoRule;
import game.baicao.server.logic.CardSuit;
import game.baicao.server.logic.Gamble;
import game.baicao.server.logic.GroupCard;
import game.baicao.server.sPlayerInfo;
import game.baicao.server.sResultInfo;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.bot.Bot;
import game.modules.bot.BotManager;
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
import java.util.Calendar;
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

public class BaicaoGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    private ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int gsResult = 2;
    public static final String USER_CHAIR = "user_chair";
    private final GameManager gameMgr = new GameManager();
    public final Vector<GamePlayer> playerList = new Vector(8);
    public int playingCount = 0;
    private volatile int serverState = 0;
    public volatile int playerCount;
    StringBuilder gameLog = new StringBuilder();
    LogGameService dao = new LogGameServiceImpl();
    public int chuongChair = 0;
    public int newChuongChair = -1;
    private final Logger logger = LoggerFactory.getLogger((String)"SamLogic");
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
            for (int i = 0; i < 8; ++i) {
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
        this.logger.info("onGameMessage: ", (Object)dataCmd.getId(), (Object)user.getName());
        switch (dataCmd.getId()) {
            case 3101: {
                this.moBai(user, dataCmd);
                break;
            }
            case 3106: {
                this.keCua(user, dataCmd);
                break;
            }
            case 3104: {
                this.yeuCauDanhBien(user, dataCmd);
                break;
            }
            case 3108: {
                this.dongYDanhBien(user, dataCmd);
                break;
            }
            case 3109: {
                this.datCuoc(user, dataCmd);
                break;
            }
            case 3112: {
                this.vaoGa(user, dataCmd);
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

    private void datCuoc(User user, DataCmd data) {
        RevDatCuoc cmd = new RevDatCuoc(data);
        this.datCuoc(user, cmd.rate);
    }

    private void datCuoc(User user, int rate) {
        GamePlayer gp = this.getPlayerByUser(user);
        GamePlayer chuong = this.getPlayerByChair(this.chuongChair);
        if (gp != null && gp.isPlaying() && !gp.camChuong && chuong != null && chuong.isPlaying() && chuong.camChuong) {
            int e = this.kiemTraDatCuoc(gp, rate);
            if (e == 0) {
                this.datCuocThanhCong(gp, chuong, rate);
                this.notifyDatCuocThanhCong(gp, rate);
            } else {
                SendDatCuoc msg = new SendDatCuoc();
                msg.Error = (byte)e;
                this.send((BaseMsg)msg, user);
            }
        }
    }

    public void datCuocThanhCong(GamePlayer gp, GamePlayer chuong, int rate) {
        gp.spRes.cuocChuong = rate;
    }

    public void notifyDatCuocThanhCong(GamePlayer gp, int rate) {
        this.gameLog.append("DC<");
        this.gameLog.append(gp.chair).append("/");
        this.gameLog.append(rate);
        this.gameLog.append(">");
        SendDatCuoc msg = new SendDatCuoc();
        msg.chair = gp.chair;
        msg.rate = rate;
        this.send(msg);
    }

    private int kiemTraDatCuoc(GamePlayer gp, int rate) {
        if (rate < 1 || rate > 4) {
            return 3;
        }
        if (gp.spRes.daCuocChuong()) {
            return 1;
        }
        if (!this.kiemTraDieuKienSoDu(gp, rate * 4)) {
            return 2;
        }
        return 0;
    }

    private boolean kiemTraDieuKienSoDu(GamePlayer gp, int rate) {
        long require;
        long maxLost;
        long currentMoney = gp.gameMoneyInfo.getCurrentMoneyFromCache();
        long remain = currentMoney - (maxLost = gp.spRes.tongTienDatCuocTreo() * this.getMoneyBet());
        return remain >= (require = (long)rate * this.getMoneyBet());
    }

    private void vaoGa(User user, DataCmd data) {
        if (this.playingCount <= 2) {
            return;
        }
        SendVaoGa msg = new SendVaoGa();
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && gp.isPlaying() && !gp.camChuong) {
            if (gp.spRes.cuocGa == 0) {
                if (this.kiemTraVaoGa(gp)) {
                    msg.chair = gp.chair;
                    msg.tienVaoGa = 3L * this.getMoneyBet();
                    this.gameLog.append("VG<");
                    this.gameLog.append(gp.chair).append("/").append(3);
                    this.gameLog.append(">");
                    this.send(msg);
                } else {
                    msg.Error = 2;
                    this.send((BaseMsg)msg, user);
                }
            } else {
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        }
    }

    private boolean kiemTraVaoGa(GamePlayer gp) {
        if (this.kiemTraDieuKienSoDu(gp, 3)) {
            gp.spRes.cuocGa = 3;
            return true;
        }
        return false;
    }

    private void dongYDanhBien(User user, DataCmd data) {
        RevDongYDanhBien cmd = new RevDongYDanhBien(data);
        this.dongYDanhBien(user, cmd.chair);
    }

    private void dongYDanhBien(User user, int chair) {
        GamePlayer me = this.getPlayerByUser(user);
        GamePlayer enemy = this.getPlayerByChair(chair);
        int rate = me.spRes.duocYeuCauDanhBien[chair];
        if (me != null && me.isPlaying() && enemy.isPlaying() && !me.camChuong && !enemy.camChuong && this.kiemTraQuyenDongYDanhBien(me, rate) && this.kiemTraQuyenDongYDanhBien(enemy, rate) && rate != 0) {
            enemy.spRes.cuocDanhBien[me.chair] = rate;
            me.spRes.cuocDanhBien[enemy.chair] = rate;
            this.gameLog.append("SL<");
            this.gameLog.append(enemy.chair).append(";");
            this.gameLog.append(me.chair).append(";");
            this.gameLog.append(rate);
            this.gameLog.append(">");
            SendDongYDanhBien msg1 = new SendDongYDanhBien();
            msg1.chair = me.chair;
            this.send((BaseMsg)msg1, enemy.getUser());
            SendDongYDanhBien msg2 = new SendDongYDanhBien();
            msg2.chair = enemy.chair;
            this.send((BaseMsg)msg2, me.getUser());
        }
    }

    private boolean kiemTraQuyenYeuCauDanhBien(GamePlayer gp, int rate) {
        if (rate < 1 || rate > 2) {
            return false;
        }
        long tongTienCuoc = gp.spRes.tongTienDatCuocTreo();
        return gp.gameMoneyInfo.getCurrentMoneyFromCache() >= tongTienCuoc + (long)(rate * 4) * this.getMoneyBet();
    }

    private boolean kiemTraQuyenDongYDanhBien(GamePlayer gp, int rate) {
        if (gp.spRes.cuocChuong == 0) {
            return false;
        }
        return this.kiemTraDieuKienSoDu(gp, rate * 4);
    }

    private void yeuCauDanhBien(User user, DataCmd data) {
        RevDanhBien cmd = new RevDanhBien(data);
        this.yeuCauDanhBien(user, cmd.chair, cmd.rate);
    }

    private void yeuCauDanhBien(User user, int chair, int rate) {
        GamePlayer enemy = this.getPlayerByChair(chair);
        GamePlayer me = this.getPlayerByUser(user);
        if (me != null && enemy != null && me.isPlaying() && enemy.isPlaying() && !me.camChuong && !enemy.camChuong && this.kiemTraQuyenYeuCauDanhBien(me, rate) && this.kiemTraQuyenYeuCauDanhBien(enemy, rate)) {
            SendYeuCauDanhBien msg = new SendYeuCauDanhBien();
            msg.chair = me.chair;
            msg.rate = rate;
            if (enemy.spRes.duocYeuCauDanhBien[enemy.chair] == 0 && me.spRes.duocYeuCauDanhBien[enemy.chair] == 0) {
                enemy.spRes.duocYeuCauDanhBien[me.chair] = rate;
                if (enemy.getUser().isBot()) {
                    enemy.yeuCauBotDanhBien = msg.chair;
                } else {
                    this.send((BaseMsg)msg, enemy.getUser());
                }
            } else {
                msg.Error = 1;
                msg.chair = enemy.chair;
                msg.rate = rate;
                this.send((BaseMsg)msg, me.getUser());
            }
        } else {
            SendYeuCauDanhBien msg = new SendYeuCauDanhBien();
            msg.chair = chair;
            msg.rate = rate;
            msg.Error = 2;
            this.send((BaseMsg)msg, me.getUser());
        }
    }

    private void keCua(User user, DataCmd data) {
        RevKeCua cmd = new RevKeCua(data);
        this.keCua(user, cmd.chair, cmd.rate);
    }

    private void keCua(User user, int chair, int rate) {
        if (this.checkChair(chair)) {
            GamePlayer me = this.getPlayerByUser(user);
            GamePlayer ally = this.getPlayerByChair(chair);
            if (me != null && ally != null && me.isPlaying() && ally.isPlaying() && !me.camChuong && !ally.camChuong) {
                int e = this.kiemTraDieuKienKeCua(me, chair, rate);
                if (e == 0) {
                    this.gameLog.append("KC<");
                    this.gameLog.append(me.chair).append(";");
                    this.gameLog.append(ally.chair).append(";");
                    this.gameLog.append(rate);
                    this.gameLog.append(">");
                    me.spRes.cuocKeCua[chair] = rate;
                }
                this.notifyKeCua(me, chair, rate, e);
            }
        }
    }

    public int kiemTraDieuKienKeCua(GamePlayer gp, int chair, int rate) {
        if (gp.spRes.cuocKeCua[chair] != 0) {
            return 1;
        }
        long totalBet = gp.spRes.tongTienDatCuocTreo();
        long remain = gp.gameMoneyInfo.getCurrentMoneyFromCache() - totalBet * this.getMoneyBet();
        if (remain < (long)(rate * 4) * this.getMoneyBet()) {
            return 2;
        }
        return 0;
    }

    public void notifyKeCua(GamePlayer gp, int chair, int rate, int e) {
        SendKeCua msg = new SendKeCua();
        msg.fromChair = gp.chair;
        msg.toChair = chair;
        msg.rate = rate;
        msg.Error = (byte)e;
        if (e == 0) {
            this.send(msg);
        } else {
            this.send((BaseMsg)msg, gp.getUser());
        }
    }

    public boolean checkChair(int chair) {
        return chair >= 0 && chair < 8;
    }

    private void moBai(User user, DataCmd data) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.moBai = true;
            this.notifyMoBai(gp);
        }
        this.hoanThanhMoBai();
    }

    private void hoanThanhMoBai() {
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || !gp.dangChoMoBai()) continue;
            return;
        }
        this.gameMgr.countDown = 0;
    }

    private void notifyMoBai(GamePlayer gp) {
        if (gp.isPlaying() && gp.spInfo.handCards != null) {
            SendMoBaiSuccess msg = new SendMoBaiSuccess();
            msg.chair = gp.chair;
            msg.cards = gp.spInfo.handCards.toByteArray();
            msg.bo = gp.spInfo.handCards.kiemTraBo();
            this.send(msg);
        }
    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 8) {
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
        if (i >= 0 && i < 8) {
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
        return chair >= 0 && chair < 8;
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

    public void checkRemoveBot() {
        int i;
        GamePlayer gp;
        int count = 0;
        for (i = 0; i < 8; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getUser() == null || gp.getUser().isBot() || gp.reqQuitRoom) continue;
            ++count;
        }
        if (count == 0) {
            for (i = 0; i < 8; ++i) {
                gp = this.getPlayerByChair(i);
                if (gp.getUser() == null) continue;
                gp.reqQuitRoom = true;
            }
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.pInfo == null) continue;
            gp.pInfo.setIsHold(false);
        }
    }

    public synchronized void onGameUserReturn(User user) {
        if (user == null) {
            return;
        }
        if (this.room.setting.maxUserPerRoom == 8) {
            for (int i = 0; i < 8; ++i) {
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
        for (i = 0; i < 8; ++i) {
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
            if (this.serverState == 1) {
                this.sendGameInfo(gp.chair);
            } else {
                this.notifyUserEnter(gp);
            }
            return;
        }
        for (i = 0; i < 8; ++i) {
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
                this.doiChuong(gp, false);
                this.chuongChair = gp.chair;
            }
            if (this.playerCount == 2) {
                this.gameMgr.roomCreatorUserId = user.getId();
                this.gameMgr.roomOwnerChair = i;
            }
            this.notifyUserEnter(gp);
            break;
        }
        this.kiemTraTuDongBatDau(5);
    }

    private GamePlayer timNguoiNhieuTien() {
        GamePlayer maxPlayer = null;
        long maxMoney = 0L;
        for (int i = 0; i < 8; ++i) {
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
        for (int i = 0; i < 8; ++i) {
            if (i == gp.chair) continue;
            this.getPlayerByChair((int)i).camChuong = false;
        }
        if (notify) {
            SendDoiChuong msg = new SendDoiChuong();
            msg.chair = this.chuongChair;
            this.send(msg);
        }
    }

    private synchronized boolean kiemTraDoiChuong(boolean force) {
        GamePlayer newChuong = this.getPlayerByChair(this.newChuongChair);
        if (newChuong != null && newChuong.hasUser()) {
            if (this.chuongChair == newChuong.chair) {
                return true;
            }
            long newChuongCurrentMoney = newChuong.gameMoneyInfo.getCurrentMoneyFromCache();
            if (newChuongCurrentMoney >= 100L * this.getMoneyBet()) {
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
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.send(msg, gp.getUser());
        }
    }

    public void send(BaseMsg msg) {
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
        }
    }

    public void chiabai() {
        this.gameLog.append("CB<");
        SendDealCard msg = new SendDealCard();
        msg.gameId = this.gameMgr.game.id;
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
            this.gameLog.append(gp.spInfo.handCards.kiemTraBo()).append(";");
            this.send((BaseMsg)msg, user);
        }
        this.gameLog.append(">");
    }

    public void start() {
        this.gameLog.setLength(0);
        this.gameLog.append("MC<");
        this.playingCount = 0;
        this.serverState = 1;
        for (int i = 0; i < 8; ++i) {
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
        this.gameLog.append(this.getMoneyBet()).append(";");
        this.gameLog.append(this.chuongChair).append(";");
        this.gameLog.append(this.room.setting.moneyType);
        this.gameLog.append(">");
        this.moiVaoCuoc();
        this.logStartGame();
        this.gameMgr.gameAction = 1;
        this.gameMgr.countDown = 4 * this.playingCount;
        this.botStartGame();
    }

    public void botJoinRoom() {
        if (this.room.setting.moneyType == 1 && this.playerCount < 2) {
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
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(11);
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            User user = gp.getUser();
            if (user != null &&   user.isBot()) {
                int x;
                ++botCount;
                Bot bot = BotManager.instance().getBotByName(user.getName());
                if (bot == null) continue;
                ++bot.count;
                int num = 7;
                if (hour < 11 || hour > 23) {
                    num = 7;
                }
                if (bot.count < 5 && this.playerCount < num || (x = BotManager.instance().getRandomNumber(5)) != 0) continue;
                gp.yeuCauBotRoiPhong = x = BotManager.instance().getRandomNumber(5);
                --botCount;
                continue;
            }
            if (user == null) continue;
            ++userCount;
        }
        int num = 5;
        if (hour < 11 || hour > 23) {
            num = 5;
        }
        int x = BotManager.instance().getRandomNumber(2);
        if (this.playerCount <= num && x == 0) {
            int after = GameUtils.rd.nextInt(15) + 15;
            BotManager.instance().regJoinRoom(this.room, after);
        }
    }

    private void logStartGame() {
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public void moiVaoCuoc() {
        SendMoiDatCuoc msg = new SendMoiDatCuoc();
        msg.countDownTime = (byte)(4 * this.playingCount);
        this.sendMsgToPlayingUser(msg);
    }

    public int demSoNguoiChoiTiep() {
        int count = 0;
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            ++count;
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
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

    private boolean coTheChoiTiep(GamePlayer gp) {
        return gp.hasUser() && gp.canPlayNextGame();
    }

    private synchronized void removePlayerAtChair(int chair, boolean disconnect) {
        if (!this.checkPlayerChair(chair)) {
            return;
        }
        GamePlayer gp = this.playerList.get(chair);
        gp.choiTiepVanSau = true;
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
        this.sendMsgExceptMe((BaseMsg)msg, user);
        this.notifyJoinRoomSuccess(gamePlayer);
    }

    public void notifyJoinRoomSuccess(GamePlayer gamePlayer) {
        SendJoinRoomSuccess msg = new SendJoinRoomSuccess();
        msg.chuongChair = this.chuongChair;
        msg.uChair = gamePlayer.chair;
        msg.roomId = this.room.getId();
        msg.moneyType = this.gameMgr.gameServer.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.gameMgr.gameServer.room.setting.moneyBet;
        for (int i = 0; i < 8; ++i) {
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
        GamePlayer gamePlayer = this.getPlayerByChair(chair);
        SendGameInfo msg = new SendGameInfo();
        msg.gameState = this.gameMgr.gameState;
        msg.isAutoStart = this.gameMgr.isAutoStart;
        msg.gameAction = this.gameMgr.gameAction;
        msg.countdownTime = this.gameMgr.countDown;
        msg.chair = (byte)gamePlayer.chair;
        msg.chuongChair = (byte)this.chuongChair;
        msg.roomId = this.room.getId();
        msg.comissionRate = this.room.setting.commisionRate;
        msg.jackpotRate = this.room.setting.rule;
        msg.moneyType = this.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.getMoneyBet();
        msg.initPrivateInfo(gamePlayer);
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.hasUser()) {
                msg.pInfos[i] = gp;
                msg.hasInfoAtChair[i] = true;
                continue;
            }
            msg.hasInfoAtChair[i] = false;
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
        boolean[] hasInfoAt = new boolean[8];
        for (int i = 0; i < 8; ++i) {
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
        this.checkRemoveBot();
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 8; ++i) {
            gp = this.getPlayerByChair(i);
            if (GameUtils.isMainTain) {
                gp.reqQuitRoom = true;
                this.notifyKickRoom(gp, 2);
            }
            if (gp.getPlayerStatus() != 0) {
                if (!this.coTheChoiTiep(gp)) {
                    if (!gp.checkMoneyCanPlay()) {
                        this.notifyKickRoom(gp, 1);
                    }
                    if (gp.getUser() != null && this.room != null) {
                        GameRoomManager.instance().leaveRoom(gp.getUser());
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
        for (i = 0; i < 8; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.kiemTraDoiChuong(false);
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
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
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp1 = this.getPlayerByChair(i);
            if (!gp1.isPlaying()) continue;
            for (int j = i + 1; j < 8; ++j) {
                GamePlayer gp2 = this.getPlayerByChair(j);
                if (!gp2.isPlaying()) continue;
                this.soBaiHaiNguoiChoi(gp1, gp2);
            }
        }
        this.soGa();
        this.timNguoiDoiChuong();
        this.tinhTienKetThuc();
        this.notifyEndGame();
        this.logEndGame();
        this.gameMgr.countDown = 12;
        this.gameMgr.gameState = 2;
        this.gameMgr.gameAction = 0;
        this.kiemTraNoHuThangLon();
    }

    public void soGa() {
        int maxChair = -1;
        GroupCard maxCard = null;
        int countGa = 0;
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.spRes.cuocGa <= 0) continue;
            ++countGa;
            gp.spRes.thangGa = -3;
            if (maxChair == -1) {
                maxChair = i;
                maxCard = gp.spInfo.handCards;
                continue;
            }
            GroupCard currentCard = gp.spInfo.handCards;
            int v = BaicaoRule.soSanhBai(currentCard, maxCard);
            if (v <= 0) continue;
            maxCard = currentCard;
            maxChair = i;
        }
        if (maxChair != -1) {
            GamePlayer winGa = this.getPlayerByChair(maxChair);
            winGa.spRes.thangGa = (countGa - 1) * 3;
        }
    }

    private void timNguoiDoiChuong() {
        GroupCard gc = null;
        for (int i = 0; i < 8; ++i) {
            GroupCard newGc;
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || (newGc = gp.spInfo.handCards).kiemTraBo() != 1 || gc != null && BaicaoRule.soSanhBai(newGc, gc) <= 0) continue;
            gc = newGc;
            this.newChuongChair = i;
        }
    }

    private void tinhTienKetThuc() {
        long tienChuong = 0L;
        long chuongRate = this.getMoneyBet();
        GamePlayer gpChuong = this.getPlayerByChair(this.chuongChair);
        if (gpChuong != null && gpChuong.isPlaying()) {
            tienChuong = this.tinhTienChuong(gpChuong);
            if (tienChuong < 0L) {
                chuongRate = -this.hieuChinhTiLeTienChuong(tienChuong);
            }
            this.hieuChinhTienThangThuaChuong(chuongRate);
        }
        this.tinhTraTien();
    }

    private long tinhTienChuong(GamePlayer gp) {
        long tienChuong = gp.spRes.calculateThangChuong(true);
        if (tienChuong < 0L) {
            UserScore score = new UserScore();
            score.money = tienChuong * this.getMoneyBet();
            score.winCount = 0;
            score.lostCount += 0;
            try {
                tienChuong = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            }
            catch (MoneyException e) {
                tienChuong = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            gp.spRes.tongTienCuoiVan = gp.spRes.tienThangChuong = tienChuong;
            score.money = tienChuong;
            this.dispatchAddEventScore(gp.getUser(), score);
        }
        return tienChuong;
    }

    private void hieuChinhTienThangThuaChuong(long chuongRate) {
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (!gp.camChuong) {
                gp.spRes.tinhTienThangTongNguoiChoi(chuongRate, this.getMoneyBet());
                continue;
            }
            gp.spRes.tinhTienThangTongCuaChuong(chuongRate);
        }
    }

    private long hieuChinhTiLeTienChuong(long tienChuong) {
        long sum = 0L;
        long rate = 0L;
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || gp.camChuong) continue;
            sum += gp.spRes.calculateThangChuong(false);
        }
        if (sum == 0L) {
            return 0L;
        }
        double doubleRate = 1.0 * (double)tienChuong / (double)sum;
        rate = Math.round(doubleRate);
        if ((double)rate > doubleRate) {
            --rate;
        }
        return rate;
    }

    private void tinhTraTien() {
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            UserScore score = new UserScore();
            score.money = gp.spRes.tongTienCuoiVan;
            if (gp.camChuong && score.money < 0L) continue;
            if (score.money >= 0L) {
                score.wastedMoney = (long)((double)(score.money * (long)this.room.setting.commisionRate) / 100.0);
                score.money -= score.wastedMoney;
                ++score.winCount;
            } else {
                score.wastedMoney = 0L;
                ++score.lostCount;
            }
            try {
                score.money = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
            }
            catch (MoneyException e) {
                score.money = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            gp.spRes.tongTienCuoiVan = score.money;
            this.dispatchAddEventScore(gp.getUser(), score);
        }
    }

    private void notifyEndGame() {
        int i;
        GamePlayer gp;
        SendEndGame msg = new SendEndGame();
        for (i = 0; i < 8; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
                msg.tongCuocGa.add(gp.spRes.tienThangGa);
                msg.tongThangDatCuoc.add(gp.spRes.tienThangChuong);
                msg.tongThangDanhBien.add(gp.spRes.tongTienDanhBien());
                msg.tongThangKeCua.add(gp.spRes.tongTienKeCua());
                msg.tongKetThangThua.add(gp.spRes.tongTienCuoiVan);
                msg.currentMoneyList.add(gp.gameMoneyInfo.getCurrentMoneyFromCache());
                msg.gamePlayers[i] = gp;
                msg.playerStatus[i] = (byte)gp.getPlayerStatus();
                continue;
            }
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
        }
        this.gameLog.append("KT<");
        this.gameLog.append(0).append(";");
        for (i = 0; i < 8; ++i) {
            gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            msg.result = gp.spRes;
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spRes.tongTienCuoiVan).append("/");
            this.gameLog.append(gp.spInfo.handCards).append(";");
            SendEndGame newMsg = new SendEndGame();
            newMsg.copyData(msg);
            this.send((BaseMsg)newMsg, gp.getUser());
        }
        this.gameLog.append(">");
    }

    private boolean dispatchEventThangLon(GamePlayer gp, boolean isNoHu) {
        boolean result = GameUtils.dispatchEventThangLon((User)gp.getUser(), (GameRoom)this.room, (int)this.gameMgr.game.id, (GameMoneyInfo)gp.gameMoneyInfo, (long)this.getMoneyBet(), (boolean)isNoHu, (byte[])gp.getHandCards());
        return result;
    }

    private void kiemTraNoHuThangLon() {
        for (int i = 0; i < 8; ++i) {
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

    private void resendEndGame(GamePlayer reconnectPlayer) {
        if (!reconnectPlayer.isPlaying() || this.gameMgr.gameState != 2) {
            return;
        }
        SendEndGame msg = new SendEndGame();
        for (int i = 0; i < 8; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
                msg.tongCuocGa.add(gp.spRes.tienThangGa);
                msg.tongThangDatCuoc.add(gp.spRes.tienThangChuong);
                msg.tongThangDanhBien.add(gp.spRes.tongTienDanhBien());
                msg.tongThangKeCua.add(gp.spRes.tongTienKeCua());
                msg.tongKetThangThua.add(gp.spRes.tongTienCuoiVan);
                msg.currentMoneyList.add(gp.gameMoneyInfo.getCurrentMoneyFromCache());
                msg.gamePlayers[i] = gp;
                msg.playerStatus[i] = (byte)gp.getPlayerStatus();
                continue;
            }
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
        }
        SendEndGame newMsg = new SendEndGame();
        msg.result = reconnectPlayer.spRes;
        newMsg.copyData(msg);
        this.send((BaseMsg)newMsg, reconnectPlayer.getUser());
    }

    private void soBaiHaiNguoiChoi(GamePlayer gp1, GamePlayer gp2) {
        if (gp1.camChuong || gp2.camChuong) {
            this.soBaiVoiChuong(gp1, gp2);
        } else {
            this.soBaiDanhBien(gp1, gp2);
        }
    }

    private void soBaiVoiChuong(GamePlayer gp1, GamePlayer gp2) {
        if (gp1.camChuong) {
            this.soChuong(gp2, gp1);
        } else {
            this.soChuong(gp1, gp2);
        }
    }

    private void soChuong(GamePlayer gp, GamePlayer chuong) {
        GroupCard gc = gp.spInfo.handCards;
        GroupCard chuongGc = chuong.spInfo.handCards;
        sResultInfo res = gp.spRes;
        sResultInfo chuongRes = chuong.spRes;
        int v = BaicaoRule.soSanhBai(gc, chuongGc);
        if (v >= 0 && gc.kiemTraBo() == 1) {
            this.newChuongChair = gp.chair;
        }
        if (res.cuocChuong == 0) {
            this.datCuocThanhCong(gp, chuong, 1);
        }
        res.thangChuong = v * res.cuocChuong;
        chuongRes.thangChuong -= v * res.cuocChuong;
        for (int i = 0; i < 8; ++i) {
            GamePlayer gpKe;
            if (i == gp.chair || i == chuong.chair || !(gpKe = this.getPlayerByChair(i)).isPlaying() || gpKe.spRes.cuocKeCua[gp.chair] == 0) continue;
            gpKe.spRes.thangKeCua[gp.chair] = v * gpKe.spRes.cuocKeCua[gp.chair];
            chuong.spRes.thangChuong -= gpKe.spRes.thangKeCua[gp.chair];
        }
    }

    private void soBaiDanhBien(GamePlayer gp1, GamePlayer gp2) {
        GroupCard gc1 = gp1.spInfo.handCards;
        GroupCard gc2 = gp2.spInfo.handCards;
        sResultInfo res1 = gp1.spRes;
        sResultInfo res2 = gp2.spRes;
        int v = BaicaoRule.soSanhBai(gc1, gc2);
        if (gp1.spRes.cuocDanhBien[gp2.chair] > 0 && gp2.spRes.cuocDanhBien[gp1.chair] > 0) {
            res1.thangBien[gp2.chair] = v * res1.cuocDanhBien[gp2.chair];
            res2.thangBien[gp1.chair] = -v * res2.cuocDanhBien[gp1.chair];
        }
    }

    public void botAutoPlay() {
        for (int i = 0; i < 8; ++i) {
            int ran;
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.isPlaying() && gp.getUser() != null && gp.getUser().isBot() && !gp.camChuong && this.gameMgr.gameAction == 1) {
                if (gp.yeuCauBotRoiPhong == this.gameMgr.countDown) {
                    this.pOutRoom(gp);
                }
                if (!gp.vaoCuoc) {
                    ran = BotManager.instance().getRandomNumber(2);
                    if (ran == 1) {
                        gp.vaoCuoc = true;
                        int rate = BotManager.instance().getRandomNumber(4) + 1;
                        this.datCuoc(gp.getUser(), rate);
                    }
                } else if (!gp.vaoGa) {
                    ran = BotManager.instance().getRandomNumber(2);
                    if (ran == 1) {
                        gp.vaoGa = true;
                        this.vaoGa(gp.getUser(), null);
                    }
                } else if (this.playingCount > 2) {
                    int chair = gp.lastChair++ % 8;
                    GamePlayer gamePlayer = this.getPlayerByChair(chair);
                    int ran2 = BotManager.instance().getRandomNumber(5);
                    int rate = BotManager.instance().getRandomNumber(2) + 1;
                    if (gamePlayer != null && gamePlayer.isPlaying() && gamePlayer.chair != gp.chair && !gamePlayer.camChuong && ran2 == 1) {
                        this.keCua(gp.getUser(), gamePlayer.chair, rate);
                    }
                    if (gp.yeuCauBotDanhBien >= 0 && gp.yeuCauBotDanhBien < 8) {
                        int x = BotManager.instance().getRandomNumber(5);
                        if (x == 0) {
                            this.dongYDanhBien(gp.getUser(), gp.yeuCauBotDanhBien);
                            gp.yeuCauBotDanhBien = -1;
                        } else if (x == 1) {
                            gp.yeuCauBotDanhBien = -1;
                        }
                    }
                }
            }
            if (!gp.isPlaying() || gp.getUser() == null || !gp.getUser().isBot() || this.gameMgr.gameAction != 2 || (ran = BotManager.instance().getRandomNumber(5)) != 1 || this.gameMgr.countDown > 14) continue;
            this.moBai(gp.getUser(), null);
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
                for (int i = 0; i < 8; ++i) {
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
        for (int i = 0; i < 8; ++i) {
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
                BaicaoGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

