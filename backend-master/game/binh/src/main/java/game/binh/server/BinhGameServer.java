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
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.eventHandlers.GameEventParam
 *  game.eventHandlers.GameEventType
 *  game.modules.bot.Bot
 *  game.modules.bot.BotManager
 *  game.modules.gameRoom.cmd.send.SendNoHu
 *  game.modules.gameRoom.config.GameRoomConfig
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
 *  net.sf.json.JSONObject
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.binh.server;

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
import com.vinplay.usercore.service.UserService;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.binh.server.GameManager;
import game.binh.server.GamePlayer;
import game.binh.server.cmd.receive.RevBinhSoChi;
import game.binh.server.cmd.receive.RevCheatCard;
import game.binh.server.cmd.send.SendBinhSoChiSuccess;
import game.binh.server.cmd.send.SendDealCard;
import game.binh.server.cmd.send.SendEndGame;
import game.binh.server.cmd.send.SendGameInfo;
import game.binh.server.cmd.send.SendJoinRoomSuccess;
import game.binh.server.cmd.send.SendKickRoom;
import game.binh.server.cmd.send.SendNewUserJoin;
import game.binh.server.cmd.send.SendNotifyReqQuitRoom;
import game.binh.server.cmd.send.SendUpdateMatch;
import game.binh.server.cmd.send.SendUpdateOwnerRoom;
import game.binh.server.cmd.send.SendUserExitRoom;
import game.binh.server.cmd.send.SendXepLai;
import game.binh.server.logic.BinhRule;
import game.binh.server.logic.CardSuit;
import game.binh.server.logic.Gamble;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.KetQuaSoBai;
import game.binh.server.logic.KetQuaTinhSap;
import game.binh.server.logic.PlayerCard;
import game.binh.server.logic.SoSanhChi;
import game.binh.server.logic.ai.BinhAuto;
import game.binh.server.sPlayerInfo;
import game.binh.server.sResultInfo;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.bot.Bot;
import game.modules.bot.BotManager;
import game.modules.gameRoom.cmd.send.SendNoHu;
import game.modules.gameRoom.config.GameRoomConfig;
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

public class BinhGameServer
extends GameServer {
    public volatile boolean isRegisterLoop = false;
    private ScheduledFuture<?> task;
    public static final int gsNoPlay = 0;
    public static final int gsPlay = 1;
    public static final int gsResult = 2;
    private final GameManager gameMgr = new GameManager();
    public final Vector<GamePlayer> playerList = new Vector(4);
    public int playingCount = 0;
    private volatile int serverState = 0;
    public volatile int playerCount;
    public ThongTinThangLon thongTinNoHu = null;
    StringBuilder gameLog = new StringBuilder();
    private final Runnable gameLoopTask = new GameLoopTask();

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
                arr.put((Map)gp.toJSONObject());
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
            case 3101: {
                this.soChi(user, dataCmd);
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
                break;
            }
            case 3104: {
                this.binhSoChiTuDong(user, dataCmd);
                break;
            }
            case 3106: {
                this.baoBinh(user, dataCmd);
                break;
            }
            case 3108: {
                this.xepLai(user, dataCmd);
            }
        }
    }

    private void xepLai(User user, DataCmd cmd) {
        if (this.gameMgr.gameAction != 2) {
            return;
        }
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.sochi = false;
            SendXepLai msg = new SendXepLai();
            msg.chair = gp.chair;
            this.send(msg);
        }
    }

    private void baoBinh(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameAction != 2) {
            return;
        }
        GamePlayer gp = this.getPlayerByUser(user);
        SendBinhSoChiSuccess msg = new SendBinhSoChiSuccess();
        if (gp != null) {
            msg.chair = gp.chair;
            int kind = gp.spInfo.autoSort(this.room.setting.rule);
            if (BinhRule.isMauBinh(kind)) {
                this.send(msg);
                gp.sochi = true;
                this.kiemTraHoanThanhSoChi();
                this.logSoChi(gp, true);
            } else {
                msg.Error = 1;
                this.send((BaseMsg)msg, user);
            }
        } else {
            msg.Error = 2;
            this.send((BaseMsg)msg, user);
        }
    }

    private void soChi(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameAction != 2) {
            return;
        }
        RevBinhSoChi cmd = new RevBinhSoChi(dataCmd);
        GamePlayer gp = this.getPlayerByUser(user);
        SendBinhSoChiSuccess msg = new SendBinhSoChiSuccess();
        if (gp == null) {
            msg.Error = 2;
            this.send((BaseMsg)msg, user);
            return;
        }
        msg.chair = gp.chair;
        GroupCard chi1 = new GroupCard(cmd.chi1);
        chi1.kiemtraBo(this.room.setting.rule);
        GroupCard chi2 = new GroupCard(cmd.chi2);
        chi2.kiemtraBo(this.room.setting.rule);
        GroupCard chi3 = new GroupCard(cmd.chi3);
        chi3.kiemtraBo(this.room.setting.rule);
        boolean checkValidCard = gp.spInfo.checkCardValid(chi1, chi2, chi3);
        if (checkValidCard) {
            gp.spInfo.sorttedCard.ApplyNew3GroupCards(chi1, chi2, chi3, this.room.setting.rule);
            gp.kiemTraMauBinh(this.room.setting.rule);
            this.send(msg);
            gp.sochi = true;
            this.kiemTraHoanThanhSoChi();
            this.logSoChi(gp, false);
        } else {
            msg.Error = 1;
            LoggerUtils.error((String)"binh", (Object[])new Object[]{"so chi ERROR", chi1, chi2, chi3});
            this.send((BaseMsg)msg, user);
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
        BinhAuto.instance();
        this.init();
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

    public byte getPlayerCount() {
        return (byte)this.playerCount;
    }

    public boolean checkPlayerChair(int chair) {
        return chair >= 0 && chair < 4;
    }

    public synchronized void onGameUserDis(User user) {
        Integer chair = (Integer)user.getProperty((Object)"user_chair");
        if (chair == null) {
            return;
        }
        GamePlayer gp = this.getPlayerByChair(chair);
        if (gp == null) {
            return;
        }
        if (gp.isPlaying()) {
            gp.reqQuitRoom = true;
            ++gp.tuDongChoi;
            this.gameLog.append("DIS<").append(chair).append(">");
        } else {
            GameRoomManager.instance().leaveRoom(user, this.room);
        }
    }

    public synchronized void onGameUserExit(User user) {
        Integer chair = (Integer)user.getProperty((Object)"user_chair");
        if (chair == null) {
            return;
        }
        GamePlayer gp = this.getPlayerByChair(chair);
        if (gp == null) {
            return;
        }
        if (gp.isPlaying()) {
            gp.reqQuitRoom = true;
            ++gp.tuDongChoi;
            this.gameLog.append("DIS<").append(chair).append(">");
        } else {
            boolean disconnect;
            this.removePlayerAtChair(chair, !(disconnect = user.isConnected()));
        }
        if (this.room.userManager.size() == 0) {
            this.resetPlayDisconnect();
            this.destroy();
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
        if (this.room.setting.maxUserPerRoom == 4) {
            for (int i = 0; i < 4; ++i) {
                GamePlayer gp = this.playerList.get(i);
                if (gp.getPlayerStatus() == 0 || gp.pInfo == null || !gp.pInfo.nickName.equalsIgnoreCase(user.getName())) continue;
                this.gameLog.append("RE<").append(i).append(">");
                GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
                if (moneyInfo != null && gp.gameMoneyInfo.sessionId != moneyInfo.sessionId) {
                    ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, -1);
                }
                user.setProperty((Object)"user_chair", (Object)gp.chair);
                gp.user = user;
                gp.tuDongChoi = 0;
                gp.reqQuitRoom = false;
                user.setProperty((Object)"GAME_MONEY_INFO", (Object)gp.gameMoneyInfo);
                this.sendGameInfo(gp.chair);
                return;
            }
        }
        user.removeProperty((Object)"GAME_ROOM");
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
            user.setProperty((Object)"user_chair", (Object)gp.chair);
            gp.user = user;
            gp.tuDongChoi = 0;
            gp.reqQuitRoom = false;
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)gp.gameMoneyInfo);
            if (this.serverState == 1) {
                this.sendGameInfo(gp.chair);
            } else {
                this.notifyUserEnter(gp);
            }
            return;
        }
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
        this.kiemTraTuDongBatDau(5);
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
                    if (gp.gameMoneyInfo == null || !gp.gameMoneyInfo.sessionId.equalsIgnoreCase(this.thongTinNoHu.moneySessionId) || !gp.gameMoneyInfo.nickName.equalsIgnoreCase(this.thongTinNoHu.nickName)) continue;
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
        this.gameLog.append("CB<");
        SendDealCard msg = new SendDealCard();
        msg.gameId = this.gameMgr.game.id;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            if (!user.isBot()) {
                msg.maubinh = gp.kiemTraMauBinh(this.room.setting.rule);
            } else {
                GroupCard gc = new GroupCard(gp.getHandCards());
                gp.spInfo.handCards.BO = msg.maubinh = gc.kiemtraBo(this.room.setting.rule);
            }
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
            this.gameLog.append(msg.maubinh).append(";");
            this.send((BaseMsg)msg, user);
        }
        this.gameLog.append(">");
    }

    public void start() {
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        this.serverState = 1;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            gp.tuDongChoi = 0;
            if (!this.coTheChoiTiep(gp)) continue;
            gp.setPlayerStatus(3);
            ++this.playingCount;
            gp.pInfo.setIsHold(true);
            PlayerInfo.setRoomId((String)gp.pInfo.nickName, (int)this.room.getId());
            this.gameLog.append(gp.pInfo.nickName).append("/");
            this.gameLog.append(i).append(";");
            gp.choiTiepVanSau = false;
        }
        this.gameLog.append(this.room.setting.moneyType);
        this.gameLog.append(">");
        this.logStartGame();
        this.gameMgr.gameAction = 1;
        this.gameMgr.countDown = 0;
        this.botStartGame();
    }

    private void logStartGame() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    private void logEndGame() {
        this.gameLog.append("KT<");
        this.gameLog.append(0).append(";");
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
            this.gameLog.append(gp.chair).append("/").append(kq.moneyCommon).append("/").append(gp.spInfo.sorttedCard.fullCard).append(";");
        }
        this.gameLog.append(">");
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
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
            gp.user.removeProperty((Object)"user_chair");
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
        gp.boSoChi = 0;
        --this.playerCount;
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
        msg.uChair = gamePlayer.chair;
        msg.roomId = this.room.getId();
        msg.moneyType = this.gameMgr.gameServer.room.setting.moneyType;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.gameMgr.gameServer.room.setting.moneyBet;
        msg.rule = this.room.setting.rule;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
            msg.playerList[i] = gp.getPlayerInfo();
            msg.moneyInfoList[i] = gp.gameMoneyInfo;
        }
        msg.gameState = (byte)this.gameMgr.gameState;
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
        Integer chair = (Integer)user.getProperty((Object)"user_chair");
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
        msg.gameAction = this.gameMgr.gameAction;
        msg.countdownTime = this.gameMgr.countDown;
        msg.chair = (byte)gamePlayer.chair;
        msg.roomId = this.room.getId();
        msg.rule = this.room.setting.rule;
        msg.gameId = this.gameMgr.game.id;
        msg.moneyBet = this.getMoneyBet();
        msg.moneyType = this.room.setting.moneyType;
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

    private void logSoChi(GamePlayer gp, boolean baoBinh) {
        if (baoBinh) {
            this.gameLog.append("BB<");
        } else {
            this.gameLog.append("SC<");
        }
        this.gameLog.append(gp.chair).append(";");
        this.gameLog.append(gp.spInfo.getKind(this.room.setting.rule)).append(";");
        this.gameLog.append(gp.spInfo.sorttedCard.ChiMot()).append(";");
        this.gameLog.append(gp.spInfo.sorttedCard.ChiHai()).append(";");
        this.gameLog.append(gp.spInfo.sorttedCard.ChiBa()).append(">");
    }

    private void binhSoChiTuDong(User user, DataCmd dataCmd) {
        if (this.gameMgr.gameAction != 2) {
            return;
        }
        RevBinhSoChi cmd = new RevBinhSoChi(dataCmd);
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp == null) {
            return;
        }
        GroupCard chi1 = new GroupCard(cmd.chi1);
        chi1.kiemtraBo(this.room.setting.rule);
        GroupCard chi2 = new GroupCard(cmd.chi2);
        chi2.kiemtraBo(this.room.setting.rule);
        GroupCard chi3 = new GroupCard(cmd.chi3);
        chi3.kiemtraBo(this.room.setting.rule);
        boolean checkValidCard = gp.spInfo.checkCardValid(chi1, chi2, chi3);
        if (checkValidCard) {
            gp.spInfo.sorttedCard.ApplyNew3GroupCards(chi1, chi2, chi3, this.room.setting.rule);
            gp.kiemTraMauBinh(this.room.setting.rule);
            if (gp.spInfo.sorttedCard.kiemTraBinhLung(this.room.setting.rule)) {
                gp.reqQuitRoom = true;
            }
        }
    }

    public void kiemTraHoanThanhSoChi() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gamePlayer = this.getPlayerByChair(i);
            if (!gamePlayer.dangChoSoChi()) continue;
            return;
        }
        this.gameMgr.countDown = 0;
    }

    public void endGame() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp1 = this.getPlayerByChair(i);
            if (!gp1.isPlaying()) continue;
            this.kiemTraKickKhoiPhongVanSauViKhongSoChi(gp1);
            int kind1 = gp1.kiemTraMauBinh(this.room.setting.rule);
            for (int j = i + 1; j < 4; ++j) {
                GamePlayer gp2 = this.getPlayerByChair(j);
                if (!gp2.isPlaying()) continue;
                int kind2 = gp2.kiemTraMauBinh(this.room.setting.rule);
                if (kind1 == 6 && kind2 == 6) {
                    this.soChiThongThuong(gp1, gp2);
                    continue;
                }
                this.soChiMauBinh(gp1, gp2);
            }
        }
        this.tinhThangThuaSapLang();
        if (this.room.setting.rule == 1) {
            this.tinhThangThuaAt();
        }
        this.tinhTienThucSu();
        this.notifyEndGame();
        this.logEndGame();
    }

    private void tinhThangThuaAt() {
        if (this.playingCount == 4) {
            int i;
            GamePlayer gp;
            for (i = 0; i < 4; ++i) {
                gp = this.getPlayerByChair(i);
                if (!gp.spInfo.hasTuQuyAt(this.room.setting.rule)) continue;
                return;
            }
            for (i = 0; i < 4; ++i) {
                gp = this.getPlayerByChair(i);
                long tienAt = BinhRule.getSoLaThangAt(gp.spInfo.demSoAt());
                for (int j = 0; j < 4; ++j) {
                    GamePlayer gp1 = this.getPlayerByChair(j);
                    KetQuaSoBai kq = gp1.spRes.getResultWithPlayer(i);
                    kq.moneyAt = tienAt;
                }
            }
        }
    }

    private void tinhThangThuaSapLang() {
        if (this.playingCount == 4) {
            for (int i = 0; i < 4; ++i) {
                int soNhaThangSap = 0;
                int soNhaThuaSap = 0;
                GamePlayer gp = this.getPlayerByChair(i);
                for (int j = 0; j < 4; ++j) {
                    if (j == gp.chair) continue;
                    KetQuaSoBai kq = gp.spRes.getResultWithPlayer(j);
                    if (kq.moneySap > 0L) {
                        ++soNhaThangSap;
                    }
                    if (kq.moneySap >= 0L) continue;
                    ++soNhaThuaSap;
                }
                if (soNhaThangSap == 3) {
                    this.thangThuaSapLang(gp);
                }
                if (soNhaThuaSap != 3) continue;
                this.thangThuaSapLang(gp);
            }
        }
    }

    private void thangThuaSapLang(GamePlayer gp) {
        gp.sapLang = true;
        for (int j = 0; j < 4; ++j) {
            if (j == gp.chair) continue;
            GamePlayer gpThangThua = this.getPlayerByChair(j);
            if (gpThangThua.sapLang) continue;
            gp.thangThuaSapLang(gpThangThua);
        }
    }

    private void kiemTraKickKhoiPhongVanSauViKhongSoChi(GamePlayer gp) {
        if (!gp.sochi) {
            if (gp.tuDongChoi > 0) {
                if (gp.getUser() != null && !gp.getUser().isBot()) {
                    gp.autoSort(this.room.setting.rule);
                }
            } else {
                ++gp.boSoChi;
                if (gp.boSoChi >= 2) {
                    gp.reqQuitRoom = true;
                }
            }
        } else {
            gp.boSoChi = 0;
        }
    }

    public void tinhTienThucSu() {
        UserScore score = new UserScore();
        long moneyLostTotal = 0L;
        long soChiThang = 0L;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
            kq.calculateMoneyCommon();
            if (kq.moneyCommon < 0L) {
                score.money = kq.moneyCommon * this.getMoneyBet();
                try {
                    score.money = kq.moneyCommon = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                }
                catch (MoneyException e) {
                    kq.moneyCommon = 0L;
                    CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                    gp.reqQuitRoom = true;
                }
                moneyLostTotal -= kq.moneyCommon;
                score.winCount = 0;
                score.lostCount = 1;
                this.capNhatKetQuaTinhTienChung(gp, kq);
                this.dispatchAddEventScore(gp.getUser(), score);
                continue;
            }
            soChiThang += kq.moneyCommon;
        }
        long moneyWinTotal = 0L;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
            if (kq.moneyCommon < 0L) continue;
            kq.moneyCommon = Math.round(1.0 * (double)kq.moneyCommon / (double)soChiThang * (double)moneyLostTotal);
            String moneyTypeName = this.room.setting.moneyType == 1 ? "vin" : "xu";
            long currentMoney = GameMoneyInfo.userService.getCurrentMoneyUserCache(gp.user.getName(), moneyTypeName);
            if (kq.moneyCommon > currentMoney) {
                kq.moneyCommon = currentMoney;
            }
            score.money = kq.moneyCommon;
            score.wastedMoney = (long)((double)(score.money * (long)this.room.setting.commisionRate) / 100.0);
            score.money -= score.wastedMoney;
            score.winCount = 1;
            score.lostCount = 0;
            try {
                score.money = kq.moneyCommon = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                moneyWinTotal += score.money + score.wastedMoney;
            }
            catch (MoneyException e) {
                kq.moneyCommon = 0L;
                score.money = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            this.capNhatKetQuaTinhTienChung(gp, kq);
            this.dispatchAddEventScore(gp.getUser(), score);
        }
        long remain = moneyLostTotal - moneyWinTotal;
        if (remain > 0L) {
            for (int i = 0; i < 4; ++i) {
                long moneyBack;
                GamePlayer gp = this.getPlayerByChair(i);
                KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
                if (kq.moneyCommon >= 0L) continue;
                score.money = moneyBack = -Math.round(1.0 * (double)kq.moneyCommon / (double)moneyLostTotal * (double)remain);
                try {
                    kq.moneyCommon += gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                }
                catch (MoneyException e) {
                    score.money = 0L;
                    CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                    gp.reqQuitRoom = true;
                }
                this.capNhatKetQuaTinhTienChung(gp, kq);
                this.dispatchAddEventScore(gp.getUser(), score);
            }
        }
        this.truTienHoa();
    }

    private void truTienHoa() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
            if (kq.moneyCommon == 0L) continue;
            return;
        }
        UserScore score = new UserScore();
        score.money = (long)(-Math.floor((double)this.room.setting.moneyBet * ((double)this.room.setting.commisionRate / 100.0)));
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
            try {
                kq.moneyCommon = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                if (kq.moneyCommon != 0L) {
                    String mamaName = "simacula";
                    BotManager.instance().userService.updateMoney(mamaName, -kq.moneyCommon, "vin", GameUtils.gameName, "Binh_Hoa", "Binh_Hoa", -kq.moneyCommon, null, TransType.NO_VIPPOINT);
                }
            }
            catch (MoneyException e) {
                kq.moneyCommon = 0L;
                CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                gp.reqQuitRoom = true;
            }
            this.capNhatKetQuaTinhTienChung(gp, kq);
        }
    }

    private void capNhatKetQuaTinhTienChung(GamePlayer me, KetQuaSoBai kq) {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying() || me.chair == i) continue;
            KetQuaSoBai kq1 = gp.spRes.getResultWithPlayer(me.chair);
            kq1.moneyCommon = kq.moneyCommon;
        }
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

    public void notifyEndGame() {
        GamePlayer gp;
        int i;
        SendEndGame msgView = new SendEndGame();
        int soChi = 0;
        int sapCaBaChi = 0;
        int count = 5;
        if (this.room.setting.rule == 1) {
            count += 3;
        }
        int c = 0;
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (gp.spInfo.getKind(this.room.setting.rule) == 6 && ++c == 2) {
                soChi = 7;
            }
            if (gp.spRes.getResultWithPlayer((int)gp.chair).moneySap == 0L) continue;
            sapCaBaChi = 3;
        }
        count += soChi + sapCaBaChi;
        for (i = 0; i < 4; ++i) {
            SendEndGame msg = new SendEndGame();
            GamePlayer gp1 = this.getPlayerByChair(i);
            if (!gp1.isPlaying()) continue;
            msg.moneyArray[i] = gp1.gameMoneyInfo.currentMoney;
            msgView.moneyArray[i] = gp1.gameMoneyInfo.currentMoney;
            msgView.ketqua.add(gp1.spRes.getResultWithPlayer(gp1.chair));
            for (int j = 0; j < 4; ++j) {
                GamePlayer gp2 = this.getPlayerByChair(j);
                if (!gp2.isPlaying()) continue;
                msg.ketqua.add(gp1.spRes.getResultWithPlayer(j));
            }
            msg.countdownsochi = count;
            for (KetQuaSoBai gp2 : msg.ketqua) {
            }
            this.send((BaseMsg)msg, gp1.getUser());
        }
        msgView.countdownsochi = count;
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 1) continue;
            this.send((BaseMsg)msgView, gp.getUser());
        }
        this.gameMgr.gameState = 2;
        this.gameMgr.countDown = count;
        this.kiemTraNoHuThangLon();
    }

    private boolean dispatchEventThangLon(GamePlayer gp, boolean isNoHu) {
        return GameUtils.dispatchEventThangLon((User)gp.getUser(), (GameRoom)this.room, (int)this.gameMgr.game.id, (GameMoneyInfo)gp.gameMoneyInfo, (long)this.getMoneyBet(), (boolean)isNoHu, (byte[])gp.getHandCards());
    }

    private void kiemTraNoHuThangLon() {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (gp.spInfo.handCards.isNoHu(this.room.setting.rule)) {
                boolean result = this.dispatchEventThangLon(gp, true);
                if (!result) continue;
                this.gameMgr.countDown += 5;
                continue;
            }
            KetQuaSoBai kq = gp.spRes.getResultWithPlayer(gp.chair);
            if (kq.moneyCommon < GameRoomConfig.instance().getBigWin()) continue;
            this.dispatchEventThangLon(gp, false);
        }
    }

    public void soSanhTungChi(GamePlayer gp1, GamePlayer gp2, KetQuaSoBai kq11, KetQuaSoBai kq12, KetQuaSoBai kq22, KetQuaSoBai kq21, KetQuaTinhSap kqSap, int chi) {
        SoSanhChi sc1 = null;
        sc1 = this.room.setting.rule == 0 ? BinhRule.BinhChiMode1(gp1.spInfo.sorttedCard.getChi(chi), gp2.spInfo.sorttedCard.getChi(chi), chi) : BinhRule.BinhChiMode2(gp1.spInfo.sorttedCard.getChi(chi), gp2.spInfo.sorttedCard.getChi(chi), chi);
        kq12.moneyInChi[chi - 1] = sc1.chiCount2;
        kq21.moneyInChi[chi - 1] = sc1.chiCount1;
        long[] arrl = kq11.moneyInChi;
        int n = chi - 1;
        arrl[n] = arrl[n] + kq21.moneyInChi[chi - 1];
        long[] arrl2 = kq22.moneyInChi;
        int n2 = chi - 1;
        arrl2[n2] = arrl2[n2] + kq12.moneyInChi[chi - 1];
        if (sc1.motSapHai()) {
            ++kqSap.tinhSap1;
        } else if (sc1.haiSapMot()) {
            ++kqSap.tinhSap2;
        }
        kqSap.tongChiThang += sc1.chiCount1;
    }

    public void soChiThongThuong(GamePlayer gp1, GamePlayer gp2) {
        KetQuaSoBai kq11 = gp1.spRes.getResultWithPlayer(gp1.chair);
        kq11.initCard(gp1, this.room.setting.rule);
        KetQuaSoBai kq12 = gp1.spRes.getResultWithPlayer(gp2.chair);
        kq12.initCard(gp2, this.room.setting.rule);
        KetQuaSoBai kq22 = gp2.spRes.getResultWithPlayer(gp2.chair);
        kq22.initCard(gp2, this.room.setting.rule);
        KetQuaSoBai kq21 = gp2.spRes.getResultWithPlayer(gp1.chair);
        kq21.initCard(gp1, this.room.setting.rule);
        KetQuaTinhSap kqSap = new KetQuaTinhSap();
        this.soSanhTungChi(gp1, gp2, kq11, kq12, kq22, kq21, kqSap, 1);
        this.soSanhTungChi(gp1, gp2, kq11, kq12, kq22, kq21, kqSap, 2);
        this.soSanhTungChi(gp1, gp2, kq11, kq12, kq22, kq21, kqSap, 3);
        if (kqSap.tinhSap1 == 3) {
            kq11.moneySap += (long)kqSap.tongChiThang;
            kq22.moneySap -= (long)kqSap.tongChiThang;
            kq12.moneySap = -kqSap.tongChiThang;
            kq21.moneySap = kqSap.tongChiThang;
        }
        if (kqSap.tinhSap2 == 3) {
            int tongChiThang2 = -kqSap.tongChiThang;
            kq22.moneySap += (long)tongChiThang2;
            kq11.moneySap -= (long)tongChiThang2;
            kq21.moneySap = -tongChiThang2;
            kq12.moneySap = tongChiThang2;
        }
    }

    public void soChiMauBinh(GamePlayer gp1, GamePlayer gp2) {
        KetQuaSoBai kq11 = gp1.spRes.getResultWithPlayer(gp1.chair);
        kq11.initCard(gp1, this.room.setting.rule);
        KetQuaSoBai kq12 = gp1.spRes.getResultWithPlayer(gp2.chair);
        kq12.initCard(gp2, this.room.setting.rule);
        KetQuaSoBai kq22 = gp2.spRes.getResultWithPlayer(gp2.chair);
        kq22.initCard(gp2, this.room.setting.rule);
        KetQuaSoBai kq21 = gp2.spRes.getResultWithPlayer(gp1.chair);
        kq21.initCard(gp1, this.room.setting.rule);
        int kind1 = gp1.kiemTraMauBinh(this.room.setting.rule);
        int kind2 = gp2.kiemTraMauBinh(this.room.setting.rule);
        PlayerCard pc1 = gp1.spInfo.sorttedCard;
        PlayerCard pc2 = gp2.spInfo.sorttedCard;
        if (kind1 == 7 && kind2 == 6 || kind1 == 6 && kind2 == 7) {
            SoSanhChi sc = null;
            sc = this.room.setting.rule == 0 ? BinhRule.BinhLungMode1(pc1, pc2) : BinhRule.BinhLungMode2(pc1, pc2);
            kq11.moneyCommon += (long)sc.chiCount1;
            kq22.moneyCommon += (long)sc.chiCount2;
            kq21.moneyCommon = sc.chiCount1;
            kq12.moneyCommon = sc.chiCount2;
        }
        if (BinhRule.isMauBinh(kind1)) {
            long moneyWin = BinhRule.GetPlayerCardMauBinhRate(kind1);
            kq12.moneyCommon = -moneyWin;
            kq21.moneyCommon = moneyWin;
            kq11.moneyCommon -= kq12.moneyCommon;
            kq22.moneyCommon -= kq21.moneyCommon;
        }
        if (BinhRule.isMauBinh(kind2)) {
            long moneyWin;
            kq12.moneyCommon = moneyWin = BinhRule.GetPlayerCardMauBinhRate(kind2);
            kq21.moneyCommon = -moneyWin;
            kq11.moneyCommon -= kq12.moneyCommon;
            kq22.moneyCommon -= kq21.moneyCommon;
        }
    }

    public byte[] kiemTraMauBinh() {
        byte[] ketQuaMauBinh = new byte[4];
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            ketQuaMauBinh[i] = (gp.isPlaying()? (byte)gp.spInfo.getKind(this.room.setting.rule) : 6);
        }
        return ketQuaMauBinh;
    }

    public boolean[] hasInfoAt() {
        boolean[] hasInfoAt = new boolean[4];
        for (int i = 0; i < 4; ++i) {
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
        this.gameMgr.gameState = 0;
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 4; ++i) {
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
        for (i = 0; i < 4; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
    }

    private void pBatDau(User user, DataCmd dataCmd) {
        int nextGamePlayerCount = this.demSoNguoiChoiTiep();
        if (nextGamePlayerCount >= 2) {
            this.gameMgr.makeAutoStart(0);
        }
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
            this.gameMgr.game.suit.initCard();
            this.gameMgr.game.isCheat = false;
        }
    }

    private void pDangKyChoiTiep(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.choiTiepVanSau = true;
        }
    }

    public void botAutoPlay() {
        for (int i = 0; i < 4; ++i) {
            int c;
            int random;
            GamePlayer gp = this.getPlayerByChair(i);
            if (this.gameMgr.countDown > 0 && gp.yeuCauBotRoiPhong == this.gameMgr.countDown && gp.getUser() != null && gp.getUser().isBot()) {
                this.pOutRoom(gp);
                gp.yeuCauBotRoiPhong = -1;
            }
            if (this.gameMgr.countDown >= (c = 60) - 10) {
                return;
            }
            if (!gp.isPlaying() || gp.getUser() == null || !gp.getUser().isBot() || gp.sochi || (random = BotManager.instance().getRandomNumber(10)) != 1 && this.gameMgr.countDown > 25) continue;
            gp.sochi = true;
            SendBinhSoChiSuccess msg = new SendBinhSoChiSuccess();
            msg.chair = gp.chair;
            this.send(msg);
            this.kiemTraHoanThanhSoChi();
        }
    }

    public void choNoHu(String nickName) {
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null || !gp.getUser().getName().equalsIgnoreCase(nickName)) continue;
            this.gameMgr.game.suit.noHuAt(gp.chair);
        }
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
        boolean flag = false;
        for (int i = 0; i < 4; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            User user = gp.getUser();
            if (user != null && user.isBot()) {
                int x;
                ++botCount;
                Bot bot = BotManager.instance().getBotByName(user.getName());
                ++bot.count;
                if (bot.count < 5 || this.playerCount < 4 || (x = BotManager.instance().getRandomNumber(2)) != 0 || flag) continue;
                flag = true;
                gp.yeuCauBotRoiPhong = x = BotManager.instance().getRandomNumber(20);
                continue;
            }
            if (user == null) continue;
            ++userCount;
        }
        if (botCount >= 3) {
            int random;
            GamePlayer gp;
            int out = BotManager.instance().getRandomNumber(2);
            if ((out == 0 || botCount == 4 || userCount == 3) && (gp = this.getPlayerByChair(random = BotManager.instance().getRandomNumber(4))).getUser() != null && gp.getUser().isBot()) {
                this.pOutRoom(gp);
            }
        } else {
            int x = BotManager.instance().getRandomNumber(2);
            if (this.playerCount < 3 && x == 0) {
                int after = GameUtils.rd.nextInt(15) + 15;
                BotManager.instance().regJoinRoom(this.room, after);
            }
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
                BinhGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

