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
 *  com.vinplay.usercore.service.MoneyInGameService
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
package game.lieng.server;

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
//import com.vinplay.usercore.service.MoneyInGameService;
//import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.lieng.server.GameManager;
import game.lieng.server.GamePlayer;
import game.lieng.server.cmd.SendStandUp;
import game.lieng.server.cmd.receive.RevBuyIn;
import game.lieng.server.cmd.receive.RevCheatCard;
import game.lieng.server.cmd.receive.RevShowCardSub;
import game.lieng.server.cmd.receive.RevTakeTurn;
import game.lieng.server.cmd.send.SendBuyIn;
import game.lieng.server.cmd.send.SendChangeTurn;
import game.lieng.server.cmd.send.SendDealPrivateCard;
import game.lieng.server.cmd.send.SendEndGame;
import game.lieng.server.cmd.send.SendGameInfo;
import game.lieng.server.cmd.send.SendJoinRoomSuccess;
import game.lieng.server.cmd.send.SendKickRoom;
import game.lieng.server.cmd.send.SendNewRound;
import game.lieng.server.cmd.send.SendNewUserJoin;
import game.lieng.server.cmd.send.SendNotifyReqQuitRoom;
import game.lieng.server.cmd.send.SendRequestBuyIn;
import game.lieng.server.cmd.send.SendSelectDealer;
import game.lieng.server.cmd.send.SendShowCard;
import game.lieng.server.cmd.send.SendShowCardSub;
import game.lieng.server.cmd.send.SendTakeTurn;
import game.lieng.server.cmd.send.SendUpdateMatch;
import game.lieng.server.cmd.send.SendUserExitRoom;
import game.lieng.server.logic.CardSuit;
import game.lieng.server.logic.Gamble;
import game.lieng.server.logic.GroupCard;
import game.lieng.server.logic.LiengGameInfo;
import game.lieng.server.logic.LiengPlayerInfo;
import game.lieng.server.logic.LiengRank;
import game.lieng.server.logic.LiengResult;
import game.lieng.server.logic.LiengRule;
import game.lieng.server.logic.Round;
import game.lieng.server.logic.Turn;
import game.lieng.server.sPlayerInfo;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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

public class LiengGameServer
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
    public final Vector<GamePlayer> playerList = new Vector(9);
    public int playingCount = 0;
    public int winType;
    public volatile int serverState = 0;
    public volatile int groupIndex;
    public volatile int playerCount;
    StringBuilder gameLog = new StringBuilder();
    public final Logger logger = LoggerFactory.getLogger((String)"debug");
    public final Runnable gameLoopTask = new GameLoopTask();
    public List<Turn> cheatTurns = new LinkedList<Turn>();
    public int turnIndex = 0;
    public ThongTinThangLon thongTinNoHu = null;

    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        this.logger.info("onGameMessage: ", (Object)dataCmd.getId(), (Object)user.getName());
        switch (dataCmd.getId()) {
            case 3111: {
                this.pOutRoom(user, dataCmd);
                break;
            }
            case 3102: {
                this.buyIn(user, dataCmd);
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
                break;
            }
            case 3112: {
                this.pShowCardSub(user, dataCmd);
                break;
            }
            case 3113: {
                this.standUp(user, dataCmd);
            }
        }
    }

    public void standUp(User user, DataCmd dataCmd) {
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null) {
            gp.standUp = !gp.standUp;
            SendStandUp msg = new SendStandUp();
            msg.standUp = gp.standUp;
            this.send((BaseMsg)msg, user);
            LiengPlayerInfo info = gp.spInfo.pokerInfo;
            if (!(!info.fold && gp.isPlaying() || this.gameMgr.isAutoStart)) {
                this.requestBuyIn(gp);
            }
        }
    }

    public void pShowCard(User user, DataCmd dataCmd) {
        SendShowCard msg = new SendShowCard();
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && gp.isPlaying() && this.gameMgr.gameState == 3 && !gp.spInfo.pokerInfo.fold) {
            msg.chair = (byte)gp.chair;
            this.send(msg);
        }
    }

    public void pShowCardSub(User user, DataCmd dataCmd) {
        RevShowCardSub cmd = new RevShowCardSub(dataCmd);
        SendShowCardSub msg = new SendShowCardSub();
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && gp.isPlaying() && !gp.spInfo.pokerInfo.fold) {
            msg.chair = (byte)gp.chair;
            this.checkCard(gp, cmd.cards);
            msg.cards = this.checkCard(gp, cmd.cards);
            if (msg.cards != null) {
                msg.cards = this.shuffleCard(msg.cards);
                this.send(msg);
            }
        }
    }

    public byte[] checkCard(GamePlayer gp, byte[] cards) {
        int i;
        byte[] handCard = gp.spInfo.handCards.toByteArray();
        if (cards.length >= handCard.length) {
            return null;
        }
        byte[] card = new byte[3];
        for (i = 0; i < card.length; ++i) {
            card[i] = -1;
        }
        block1 : for (i = 0; i < cards.length; ++i) {
            byte b1 = cards[i];
            for (int j = 0; j < handCard.length; ++j) {
                byte b2 = handCard[j];
                if (b1 != b2) continue;
                card[j] = b1;
                continue block1;
            }
        }
        return card;
    }

    public void buyIn(User user, DataCmd data) {
        RevBuyIn cmd = new RevBuyIn(data);
        this.buyIn(user, cmd.moneyBuyIn, cmd.autoBuyIn);
    }

    public void buyIn(User user, long moneyBuyIn, boolean autoBuyIn) {
        GamePlayer gp = this.getPlayerByUser(user);
        this.buyIn(gp, moneyBuyIn, autoBuyIn);
    }

    private boolean checkBuyInMoney(long moneyBuyIn) {
        return moneyBuyIn >= 5L * this.getMoneyBet() && moneyBuyIn <= 200L * this.getMoneyBet();
    }

    public boolean buyIn(GamePlayer gp, long moneyBuyIn, boolean autoBuyIn) {
        if (this.canBuyIn(gp)) {
            if (!this.checkBuyInMoney(moneyBuyIn)) {
                GameRoomManager.instance().leaveRoom(gp.getUser(), this.room);
                this.notifyKickRoom(gp, (byte)3);
                return false;
            }
            SendBuyIn msg = new SendBuyIn();
            ListGameMoneyInfo.instance().removeGameMoneyInfo(gp.gameMoneyInfo, this.room.getId());
            boolean result = gp.gameMoneyInfo.freezeMoneyBegining(moneyBuyIn);
            if (result) {
                msg.buyInMoney = gp.gameMoneyInfo.freezeMoney;
                LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
                pokerInfo.currentMoney = msg.buyInMoney;
                pokerInfo.lastBuyInMoney = msg.buyInMoney;
                Debug.trace((Object[])new Object[]{"buyIn: Freeze=;Current=;", gp.pInfo.nickName, gp.gameMoneyInfo.freezeMoney, pokerInfo.currentMoney});
                msg.chair = gp.chair;
                gp.autoBuyIn = autoBuyIn;
                this.send(msg);
                gp.standUp = false;
                gp.lastTimeBuyIn = System.currentTimeMillis();
                return true;
            }
            GameRoomManager.instance().leaveRoom(gp.getUser(), this.room);
            this.notifyKickRoom(gp, (byte)3);
            return false;
        }
        return false;
    }

    public boolean canBuyIn(GamePlayer gp) {
        boolean flag;
        if (gp == null) {
            return false;
        }
        long delta = System.currentTimeMillis() - gp.lastTimeBuyIn;
        if (delta < 1000L) {
            return false;
        }
        LiengPlayerInfo info = gp.spInfo.pokerInfo;
        if (info == null) {
            return false;
        }
        boolean bl = flag = gp.standUp && (!gp.isPlaying() || info.fold || this.gameMgr.gameState == 3);
        return info.currentMoney < 1L * this.getMoneyBet() || flag;
    }

    public boolean buyInAuTo(GamePlayer gp) {
        if (this.canBuyIn(gp)) {
            if (!this.checkBuyInMoney(gp.spInfo.pokerInfo.lastBuyInMoney) || !gp.autoBuyIn) {
                Debug.trace((Object[])new Object[]{"buyInAuTo failed", gp.gameMoneyInfo.freezeMoney, gp.autoBuyIn});
                return false;
            }
            LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            this.buyIn(gp, pokerInfo.lastBuyInMoney, true);
            return true;
        }
        Debug.trace((Object)"buyInAuTo failed gameplayer null");
        return false;
    }

    public void init(GameRoom ro) {
        this.room = ro;
        this.gameMgr.gameServer = this;
        int i = 0;
        while (i < 9) {
            GamePlayer gp = new GamePlayer();
            gp.gameServer = this;
            gp.spInfo.pokerInfo = new LiengPlayerInfo(gp);
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
        if (i >= 0 && i < 9) {
            return this.playerList.get(i);
        }
        Debug.trace((Object)("Get player null: " + i));
        return null;
    }

    public long getMoneyBet() {
        return this.room.setting.moneyBet;
    }

    public byte getPlayerCount() {
        return (byte)this.playerCount;
    }

    public boolean checkPlayerChair(int chair) {
        return chair >= 0 && chair < 9;
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
            boolean disconnect;
            this.removePlayerAtChair(chair, !(disconnect = user.isConnected()));
        }
        if (this.room.userManager.size() == 0) {
            this.resetPlayDisconnect();
            this.destroy();
        }
    }

    public void resetPlayDisconnect() {
        for (int i = 0; i < 9; ++i) {
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
        for (int i = 0; i < 9; ++i) {
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
        for (i = 0; i < 9; ++i) {
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
            for (i = 0; i < 9; ++i) {
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
                this.kiemTraTuDongBatDau(2);
                return;
            }
        }
        for (i = 0; i < 9; ++i) {
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
            this.kiemTraTuDongBatDau(2);
            return;
        }
    }

    public int getNumTotalPlayer() {
        return this.playerCount;
    }

    public void sendMsgToPlayingUser(BaseMsg msg) {
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.send(msg, gp.getUser());
        }
    }

    public void send(BaseMsg msg) {
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null) continue;
            ExtensionUtility.getExtension().send(msg, gp.getUser());
        }
    }

    public void chiabai() {
        this.gameLog.append("CB<");
        SendDealPrivateCard msg = new SendDealPrivateCard();
        msg.gameId = this.gameMgr.game.id;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.playerList.get(i);
            if (!gp.isPlaying()) continue;
            User user = gp.getUser();
            msg.cards = gp.spInfo.handCards.toByteArray();
            msg.cards = this.shuffleCard(msg.cards);
            msg.card_name = gp.spInfo.handCards.tinhChoDiemBoChoClient();
            this.gameLog.append(gp.chair).append("/");
            this.gameLog.append(gp.spInfo.handCards.toString()).append("/");
            this.gameLog.append(gp.spInfo.handCards.kiemTraBo()).append(";");
            Debug.trace((Object[])new Object[]{"chiabai:", gp.pInfo.nickName, gp.spInfo.handCards});
            msg.countDown = this.gameMgr.countDown;
            this.send((BaseMsg)msg, user);
        }
        this.gameLog.append(">");
        this.gameLog.append("PC<");
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        this.gameLog.append(gameInfo.publicCard);
        this.gameLog.append(">");
    }

    private byte[] shuffleCard(byte[] cards) {
        int i;
        LinkedList<Byte> x = new LinkedList<Byte>();
        for (i = 0; i < cards.length; ++i) {
            x.add(cards[i]);
        }
        Collections.shuffle(x);
        for (i = 0; i < cards.length; ++i) {
            cards[i] = (Byte)x.get(i);
        }
        return cards;
    }

    public synchronized void start() {
        Debug.trace((Object)"Start game");
        this.gameMgr.isAutoStart = false;
        this.gameLog.setLength(0);
        this.gameLog.append("BD<");
        this.playingCount = 0;
        this.serverState = 1;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
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
        this.gameLog.append(">");
        this.clearInfoNewGame();
        this.gameMgr.gameAction = 0;
        this.gameMgr.countDown = 0;
        this.logStartGame();
    }

    public void clearInfoNewGame() {
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        gameInfo.clearNewGame();
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            pokerInfo.clearNewGame();
        }
    }

    public void logStartGame() {
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            Debug.trace((Object[])new Object[]{"logStartGame", gp.chair, gp.pInfo.nickName});
            GameUtils.logStartGame((int)this.gameMgr.game.id, (String)gp.pInfo.nickName, (long)this.gameMgr.game.logTime, (int)this.room.setting.moneyType);
        }
    }

    public int demSoNguoiChoiTiep() {
        int count = 0;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!this.coTheChoiTiep(gp)) continue;
            ++count;
        }
        return count;
    }

    public int demSoNguoiDangChoi() {
        int count = 0;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            ++count;
        }
        return count;
    }

    public synchronized void kiemTraTuDongBatDau(int after) {
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

    private void xuLiDanhCap(int count) {
        GamePlayer gp2;
        GamePlayer gp1;
        int j;
        int i;
        boolean checkIp = false;
        for (i = 0; i < 9; ++i) {
            gp1 = this.getPlayerByChair(i);
            if (gp1.getUser() == null) continue;
            for (j = i + 1; j < 9; ++j) {
                gp2 = this.getPlayerByChair(j);
                if (gp2.getUser() == null || gp1.getUser().getIpAddress().equalsIgnoreCase(gp2.getUser().getIpAddress())) continue;
                checkIp = true;
            }
        }
        if (!checkIp) {
            return;
        }
        for (i = 0; i < 9; ++i) {
            gp1 = this.getPlayerByChair(i);
            if (gp1.getUser() == null) continue;
            for (j = i + 1; j < 9; ++j) {
                gp2 = this.getPlayerByChair(j);
                if (gp2.getUser() == null || this.kiemTraDuocDanhCungNhau(gp1, gp2, checkIp) || --count != 2) continue;
                return;
            }
        }
    }

    private boolean kiemTraDuocDanhCungNhau(GamePlayer gp1, GamePlayer gp2, boolean checkIp) {
        Debug.trace((Object[])new Object[]{"kiemTraDuocDanhCungNhau1", gp1.getUser().getName(), gp1.reqQuitRoom, gp1.timeJoinRoom, checkIp, gp1.getUser().getIpAddress()});
        Debug.trace((Object[])new Object[]{"kiemTraDuocDanhCungNhau2", gp2.getUser().getName(), gp2.reqQuitRoom, gp2.timeJoinRoom, checkIp, gp2.getUser().getIpAddress()});
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
        if ((double)delta < 1500.0) {
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

    public boolean coTheChoiTiep(GamePlayer gp) {
        return gp.user != null && gp.user.isConnected() && gp.canPlayNextGame() && gp.spInfo.pokerInfo.currentMoney >= 1L * this.room.setting.moneyBet;
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
        gp.spInfo.pokerInfo.clearOutGame();
        gp.user = null;
        gp.pInfo = null;
        if (gp.gameMoneyInfo != null) {
            ListGameMoneyInfo.instance().removeGameMoneyInfo(gp.gameMoneyInfo, this.room.getId());
        }
        gp.gameMoneyInfo = null;
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
        msg.money = gamePlayer.gameMoneyInfo.currentMoney;
        msg.uStatus = gamePlayer.getPlayerStatus();
        msg.setBaseInfo(gamePlayer.pInfo);
        msg.uChair = gamePlayer.chair;
        this.sendMsgExceptMe((BaseMsg)msg, user);
        this.notifyJoinRoomSuccess(gamePlayer);
        if (GameUtils.isBot && user.isBot()) {
            this.buyIn(gamePlayer, 5L * this.getMoneyBet(), true);
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
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            msg.playerStatus[i] = (byte)gp.getPlayerStatus();
            msg.playerList[i] = gp.getPlayerInfo();
            msg.moneyInfoList[i] = gp.spInfo.pokerInfo;
            if (gp.getUser() == null || gp.spInfo.handCards == null) continue;
            msg.handCardSize[i] = 2;
        }
        msg.gameAction = (byte)this.gameMgr.gameAction;
        msg.curentChair = (byte)this.gameMgr.currentChair();
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
            msg.initPrivateInfo(me);
            for (int i = 0; i < 9; ++i) {
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

    public void registerTakeTurn(User user, DataCmd dataCmd) {
        RevTakeTurn cmd = new RevTakeTurn(dataCmd);
        this.registerTakeTurn(user, cmd);
    }

    public boolean registerTakeTurn(User user, RevTakeTurn cmd) {
        if (this.gameMgr.gameState != 1) {
            return false;
        }
        Debug.trace((Object[])new Object[]{"registerTakeTurn", user.getName()});
        GamePlayer gp = this.getPlayerByUser(user);
        if (gp != null && this.gameMgr.gameAction == 4 && gp.chair == this.gameMgr.currentChair && this.gameMgr.countDown >= 1) {
            LiengPlayerInfo info = gp.spInfo.pokerInfo;
            LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
            boolean res = false;
            res = info.register(cmd, info, gameInfo);
            if (res) {
                this.takeTurn();
                return true;
            }
            Debug.trace((Object)"register take turn failed");
            return false;
        }
        Debug.trace((Object)"Take turn invalid order");
        return false;
    }

    public void endGame() {
        this.gameMgr.gameState = 3;
        this.gameMgr.countDown = (int)(10.0 + Math.ceil(1.5 * (double)this.playingCount));
        Debug.trace((Object)"endGame");
        this.gomTienVaoHu();
        this.sortUserRanking();
        this.tinhTraTien();
        this.traTien();
        this.notifyEndGame();
        this.kiemTraNoHuThangLon();
    }

    public void notifyEndGame() {
        int i;
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        LiengResult result = gameInfo.resultPoker;
        List<LiengRank> listRank = result.ranking;
        SendEndGame msg = new SendEndGame();
        msg.moneyPot = gameInfo.potMoney;
        int count = 0;
        for (i = 0; i < listRank.size(); ++i) {
            LiengRank rank = listRank.get(i);
            msg.ketQuaTinhTien[rank.chair] = rank.finalMoney;
            msg.winLost[rank.chair] = rank.win;
            msg.rank[rank.chair] = rank.fold ? 10L : (long)rank.rank;
            if (rank.fold) count++;
        }
        boolean isShowCard = false;
        if (count < listRank.size() - 1) isShowCard = true;
        for (i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.isPlaying()) {
                LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
                msg.hasInfoAtChair[gp.chair] = pokerInfo.getStatus();
                msg.moneyArray[gp.chair] = pokerInfo.currentMoney;
                msg.balanceMoney[gp.chair] = gp.gameMoneyInfo.getCurrentMoneyFromCache();
                if (!pokerInfo.fold) {
                    msg.cards[gp.chair] = gp.spInfo.handCards.toByteArray();
                    msg.groupCardName[gp.chair] = (byte)gp.spInfo.handCards.tinhChoDiemBoChoClient();
                    if (isShowCard) msg.gpShowCard[gp.chair] = 1;
                } else {
                    msg.cards[gp.chair] = new byte[0];
                }
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
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            this.gameLog.append(gp.chair).append("/").append(msg.ketQuaTinhTien[gp.chair]).append("/").append(gp.spInfo.maxCards).append(";");
        }
        this.gameLog.append(">");
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    public void traTien() {
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        LiengResult result = gameInfo.resultPoker;
        List<LiengRank> listRank = result.ranking;
        for (int i = 0; i < listRank.size(); ++i) {
            LiengRank rank = listRank.get(i);
            GamePlayer gp = this.getPlayerByChair(rank.chair);
            LiengPlayerInfo info = gp.spInfo.pokerInfo;
            rank.finalMoney = rank.totalMoneyWin;
            boolean bl = rank.win = rank.finalMoney > info.totalBet;
            if (rank.finalMoney > 0L) {
                long moneyWin;
                UserScore score = new UserScore();
                score.money = rank.finalMoney;
                if (score.money > info.totalBet) {
                    moneyWin = score.money - info.totalBet;
                    score.wastedMoney = (long)((double)(moneyWin * (long)this.room.setting.commisionRate) / 100.0);
                    score.money -= score.wastedMoney;
                    score.winCount = 1;
                    gameInfo.dealer = gp.chair;
                    if (this.room.setting.moneyType == 1) {
                        GameMoneyInfo.moneyService.addVippoint(gp.gameMoneyInfo.nickName, moneyWin, "vin");
                    }
                } else {
                    moneyWin = info.totalBet - score.money;
                    score.lostCount = 1;
                    if (this.room.setting.moneyType == 1) {
                        GameMoneyInfo.moneyService.addVippoint(gp.gameMoneyInfo.nickName, moneyWin, "vin");
                    }
                }
                try {
                    rank.finalMoney = gp.gameMoneyInfo.chargeMoneyInGame(score, this.room.getId(), this.gameMgr.game.id);
                }
                catch (MoneyException e) {
                    CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + gp.gameMoneyInfo.toString()));
                    gp.reqQuitRoom = true;
                }
                score.money = rank.finalMoney;
                gp.spInfo.pokerInfo.currentMoney += rank.finalMoney;
                this.dispatchAddEventScore(gp.getUser(), score);
                continue;
            }
            if (this.room.setting.moneyType != 1) continue;
            GameMoneyInfo.moneyService.addVippoint(gp.gameMoneyInfo.nickName, info.totalBet, "vin");
        }
    }

    public void tinhTraTien() {
        int i;
        LiengRank rank;
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        LiengResult result = gameInfo.resultPoker;
        List<LiengRank> listRank = result.ranking;
        int index = 0;
        int size = listRank.size();
        LinkedList<LiengRank> winList = new LinkedList<LiengRank>();
        LinkedList<LiengRank> lostList = new LinkedList<LiengRank>();
        Debug.trace((Object)"=================================================tinhTraTien1==================================================");
        for (i = 0; i < listRank.size(); ++i) {
            rank = listRank.get(i);
            Debug.trace((Object)rank);
        }
        Debug.trace((Object)"=================================================tinhTraTien2==================================================");
        while (index < size) {
            try {
                winList.clear();
                lostList.clear();
                LiengRank myRank = listRank.get(index);
                for (int i2 = index; i2 < size; ++i2) {
                    LiengRank otherRank = listRank.get(i2);
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
                    LiengRank rank2 = listRank.get(i3);
                    long lostMoney = rank2.totalBet;
                    if (rank2.totalBet > winUnit) {
                        lostMoney = winUnit;
                    }
                    rank2.totalBet -= lostMoney;
                    totalLost += lostMoney;
                }
                long winShare = totalLost;
                if (winSize != 0) {
                    winShare = (long)Math.floor((double)totalLost * 1.0 / (double)winSize);
                    for (int i4 = 0; i4 < winList.size(); ++i4) {
                        LiengRank winRank = (LiengRank)winList.get(i4);
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
            rank = listRank.get(i);
            Debug.trace((Object)rank);
        }
        Debug.trace((Object)"============================================tinhTraTien3=======================================================");
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

    public void sortUserRanking() {
        int i;
        ArrayList<LiengRank> listRank = new ArrayList<LiengRank>();
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        LiengResult result = gameInfo.resultPoker;
        for (int i2 = 0; i2 < 9; ++i2) {
            GamePlayer gp = this.getPlayerByChair(i2);
            if (!gp.isPlaying()) continue;
            LiengPlayerInfo playerInfo = gp.spInfo.pokerInfo;
            LiengRank rank = new LiengRank();
            rank.fold = playerInfo.fold;
            rank.chair = gp.chair;
            gp.spInfo.maxCards = rank.cards = gp.spInfo.handCards;
            rank.totalBet = playerInfo.totalBet;
            listRank.add(rank);
        }
        Collections.sort(listRank);
        int rank = 1;
        LiengRank prev = null;
        for (i = 0; i < listRank.size(); ++i) {
            LiengRank current = (LiengRank)listRank.get(i);
            if (prev == null || LiengRule.soSanhBoBai(prev.cards, current.cards) != 0) {
                // empty if block
            }
            prev = current;
            prev.rank = ++rank;
        }
        result.ranking = listRank;
        for (i = 0; i < result.ranking.size(); ++i) {
            LiengRank r = result.ranking.get(i);
            Debug.trace((Object[])new Object[]{"sortUserRanking:", r});
        }
    }

    public boolean dispatchEventThangLon(GamePlayer gp, boolean isNoHu) {
        boolean result = GameUtils.dispatchEventThangLon((User)gp.getUser(), (GameRoom)this.room, (int)this.gameMgr.game.id, (GameMoneyInfo)gp.gameMoneyInfo, (long)this.getMoneyBet(), (boolean)isNoHu, (byte[])gp.getHandCards());
        return result;
    }

    public void kiemTraNoHuThangLon() {
        for (int i = 0; i < 9; ++i) {
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

    public void notifyKickRoom(GamePlayer gp, byte reason) {
        SendKickRoom msg = new SendKickRoom();
        msg.reason = reason;
        this.send((BaseMsg)msg, gp.getUser());
    }

    public boolean checkMoneyPlayer(GamePlayer gp) {
        return gp.spInfo.pokerInfo.currentMoney >= 1L * this.room.setting.moneyBet;
    }

    public boolean coTheOLaiBan(GamePlayer gp) {
        return gp.user != null && gp.user.isConnected() && !gp.reqQuitRoom;
    }

    public synchronized void pPrepareNewGame() {
        GamePlayer gp;
        int i;
        this.gameMgr.gameState = 0;
        this.tuDongBuyIn();
        SendUpdateMatch msg = new SendUpdateMatch();
        for (i = 0; i < 9; ++i) {
            gp = this.getPlayerByChair(i);
            if (gp.getPlayerStatus() != 0) {
                if (GameUtils.isMainTain || !this.coTheOLaiBan(gp) && gp.isPlaying()) {
                    if (GameUtils.isMainTain) {
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
                    if (!this.checkMoneyPlayer(gp)) {
                        this.requestBuyIn(gp);
                    }
                    msg.hasInfoAtChair[i] = true;
                    msg.pInfos[i] = gp;
                }
                gp.setPlayerStatus(2);
            } else {
                msg.hasInfoAtChair[i] = false;
            }
            gp.prepareNewGame();
        }
        for (i = 0; i < 9; ++i) {
            gp = this.getPlayerByChair(i);
            if (!msg.hasInfoAtChair[i]) continue;
            msg.chair = (byte)i;
            this.send((BaseMsg)msg, gp.getUser());
        }
        this.gameMgr.prepareNewGame();
        this.serverState = 0;
    }

    public void requestBuyIn(GamePlayer gp) {
        if (this.canBuyIn(gp)) {
            if (!gp.standUp) {
                ++gp.countToOutRoom;
            }

            if(gp.gameMoneyInfo.currentMoney < 1L * this.room.setting.requiredMoney){
                GameRoomManager.instance().leaveRoom(gp.getUser(), this.room);
                this.notifyKickRoom(gp, (byte)3);
            }

            if (this.gameMgr.gameState == 3) {
                this.kiemTraTuDongBatDau(2);
            }
            gp.spInfo.pokerInfo.currentMoney = 0L;
            SendRequestBuyIn msg = new SendRequestBuyIn();
            this.send((BaseMsg)msg, gp.getUser());
            Debug.trace((Object[])new Object[]{"Request buy in", gp.pInfo.nickName});
        }
    }

    public void tuDongBuyIn() {
        for (int i = 0; i < 9; ++i) {
            boolean flag2;
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.hasUser()) continue;
            if (gp.countToOutRoom >= 2) {
                gp.reqQuitRoom = true;
                continue;
            }
            LiengPlayerInfo pokerPlayerInfo = gp.spInfo.pokerInfo;
            if (gp.standUp) {
                pokerPlayerInfo.currentMoney = 0L;
                continue;
            }
            GameMoneyInfo moneyInfo = gp.gameMoneyInfo;
            boolean flag1 = moneyInfo.currentMoney >= 5L * this.room.setting.moneyBet;
            boolean bl = flag2 = pokerPlayerInfo.currentMoney < 1L * this.room.setting.moneyBet;
            if (!flag1 || !flag2) continue;
            this.buyInAuTo(gp);
        }
    }

    public void logEndGame() {
        GameUtils.logEndGame((int)this.gameMgr.game.id, (String)this.gameLog.toString(), (long)this.gameMgr.game.logTime);
    }

    public void tudongChoi1() {
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        if (gp != null && gp.getUser().isBot()) {
            Debug.trace((Object[])new Object[]{"TU DONG CHOI =========================", gp.pInfo.nickName, " BEGIN ====================="});
            LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            Round round = this.gameMgr.game.getCurrentRound();
            LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
            Random rd = new Random();
            boolean res = false;
            while (!res) {
                int action = Math.abs(rd.nextInt() % 5);
                RevTakeTurn cmd = new RevTakeTurn(new DataCmd(new byte[20]));
                if (action == 0) {
                    cmd.fold = true;
                }
                if (action == 1) {
                    cmd.check = true;
                }
                if (action == 2) {
                    cmd.callAll = true;
                }
                if (action == 4) {
                    cmd.allIn = true;
                }
                if (action == 3) {
                    cmd.raise = gameInfo.lastRaise + gameInfo.bigBlindMoney;
                }
                res = this.registerTakeTurn(gp.getUser(), cmd);
            }
            Debug.trace((Object[])new Object[]{"TU DONG CHOI **********************", gp.pInfo.nickName, " END **********************"});
        }
    }

    public void tudongChoi() {
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        Debug.trace((Object[])new Object[]{"TU DONG CHOI =========================", gp.pInfo.nickName, " BEGIN ====================="});
        if (gp != null) {
            ++gp.countToOutRoom;
            LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            pokerInfo.registerFold = true;
            this.changeTurn();
            Debug.trace((Object[])new Object[]{"TU DONG CHOI **********************", gp.pInfo.nickName, " END **********************"});
        }
    }

    public void botAutoPlay() {
        if (!GameUtils.dev_mod) {
            return;
        }
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        Debug.trace((Object[])new Object[]{"TU DONG CHOI =========================", gp.pInfo.nickName, " BEGIN ====================="});
        if (gp != null && this.cheatTurns.size() == 0) {
            LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            pokerInfo.registerFold = true;
            this.changeTurn();
            Debug.trace((Object[])new Object[]{"TU DONG CHOI **********************", gp.pInfo.nickName, " END **********************"});
        } else {
            Turn turn = this.cheatTurns.get(this.turnIndex % this.cheatTurns.size());
            ++this.turnIndex;
            LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
            pokerInfo.register(turn);
            this.changeTurn();
            Debug.trace((Object[])new Object[]{"TU DONG CHOI ********************************************", "**"});
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
        if (dealer >= 0 && dealer < 9) {
            this.gameMgr.game.dealer = dealer;
        }
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
                for (int i = 0; i < 9; ++i) {
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
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (gp.getUser() == null || !gp.getUser().getName().equalsIgnoreCase(nickName)) continue;
            this.gameMgr.game.suit.noHuAt(gp.chair);
        }
    }

    public void selectDealer() {
        LiengGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        int from = pokerGameInfo.dealer % 9;
        if (this.gameMgr.game.isCheat) {
            from = this.gameMgr.game.dealer < 0 ? from : this.gameMgr.game.dealer;
            for (int i = 0; i < 9; ++i) {
                GamePlayer gp = this.getPlayerByChair(i);
                if (!gp.isPlaying() || this.gameMgr.game.moneyArray[i] == 0L) continue;
                gp.spInfo.pokerInfo.currentMoney = this.gameMgr.game.moneyArray[i];
            }
        }
        int newDealer = -1;
        int newSmallBlind = -1;
        int newBigBlind = -1;
        int startChair = -1;
        int i = from;
        while (newDealer == -1) {
            int currentChair = i % 9;
            GamePlayer gp = this.getPlayerByChair(currentChair);
            if (gp.isPlaying() && newDealer == -1) {
                newDealer = currentChair;
                newSmallBlind = currentChair;
                newBigBlind = currentChair;
                startChair = currentChair;
                break;
            }
            ++i;
        }
        this.gameMgr.currentChair = newDealer;
        pokerGameInfo.dealer = newDealer;
        pokerGameInfo.smallBlind = newSmallBlind;
        pokerGameInfo.bigBlind = newBigBlind;
        pokerGameInfo.bigBlindMoney = 1L * this.room.setting.moneyBet;
        this.gameLog.append("SD<");
        this.gameLog.append("dl/").append(pokerGameInfo.dealer).append(";");
        this.gameLog.append("sb/").append(pokerGameInfo.smallBlind).append(";");
        this.gameLog.append("bb/").append(pokerGameInfo.bigBlind).append(">");
        this.initSmallAndBigBlind();
        this.notifyDealder();
    }

    public void initSmallAndBigBlind() {
        int from;
        Debug.trace((Object[])new Object[]{"============VAO CUOC DAU GAME:", "=================================="});
        LiengGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        Round round = this.gameMgr.game.getCurrentRound();
        for (int i = from = pokerGameInfo.dealer; i < 9 + from; ++i) {
            int chair = i % 9;
            GamePlayer gp = this.getPlayerByChair(chair);
            if (!gp.isPlaying()) continue;
            LiengPlayerInfo info = gp.spInfo.pokerInfo;
            info.registerMoney(1L * this.room.setting.moneyBet);
            Turn turn = info.take(pokerGameInfo, round, true);
            this.logTakeTurn(gp, turn);
        }
        Debug.trace((Object[])new Object[]{"===============================", "=================================="});
    }

    public void notifyDealder() {
        SendSelectDealer msg = new SendSelectDealer();
        LiengGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        msg.smallBlind = pokerGameInfo.smallBlind;
        msg.bigBlind = pokerGameInfo.bigBlind;
        msg.dealer = pokerGameInfo.dealer;
        msg.gameId = this.gameMgr.game.id;
        if (this.gameMgr.game.isCheat) {
            msg.isCheat = true;
        }
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.hasUser()) continue;
            msg.hasInfoAtChair[i] = true;
            msg.gamePlayers[i] = gp;
            msg.moneyArray[i] = gp.spInfo.pokerInfo.currentMoney;
        }
        this.send(msg);
        this.gameMgr.gameAction = 1;
        this.gameMgr.countDown = 3;
    }

    public void newRound() {
        Round round = this.gameMgr.game.getCurrentRound();
        if (round.roundId == 0) {
            this.endGame();
        }
    }

    public int findFirstChairNewRound() {
        LiengGameInfo pokerGameInfo = this.gameMgr.game.pokerGameInfo;
        int from = pokerGameInfo.smallBlind;
        int startChair = -1;
        int i = from;
        while (startChair == -1) {
            int currentChair = i % 9;
            GamePlayer gp = this.getPlayerByChair(currentChair);
            if (gp.isPlaying()) {
                LiengPlayerInfo info = gp.spInfo.pokerInfo;
                if (startChair == -1 && !info.fold && !info.allIn) {
                    startChair = currentChair;
                    break;
                }
            }
            ++i;
        }
        Debug.trace((Object[])new Object[]{"findFirstChairNewRound", startChair});
        return startChair;
    }

    public void gomTienVaoHu() {
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        long totalBet = 0L;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            LiengPlayerInfo info = gp.spInfo.pokerInfo;
            totalBet += info.moneyBet;
            info.totalBet += info.moneyBet;
            info.clearNewRound();
        }
        gameInfo.clearNewRound();
    }

    public void notifyNewRound(Round round) {
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            SendNewRound msg = new SendNewRound();
            msg.potMoney = gameInfo.potMoney;
            msg.roundId = round.roundId;
            msg.cards_name = (byte)gp.spInfo.handCards.kiemTraBo();
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
        this.gameMgr.countDown = 30;
        this.gameMgr.gameAction = 4;
        SendChangeTurn msg = new SendChangeTurn();
        Round round = this.gameMgr.game.getCurrentRound();
        msg.roundId = round.roundId;
        msg.curentChair = this.gameMgr.currentChair;
        msg.countDownTime = this.gameMgr.countDown;
        Debug.trace((Object[])new Object[]{"notifyChangeTurn", GameUtils.toJsonString((Object)((Object)msg))});
        this.send(msg);
    }

    public Turn takeTurn() {
        LiengGameInfo potInfo = this.gameMgr.game.pokerGameInfo;
        Round round = this.gameMgr.game.getCurrentRound();
        GamePlayer gp = this.getPlayerByChair(this.gameMgr.currentChair);
        LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
        Turn turn = pokerInfo.takeTurn(potInfo, round);
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
        LiengGameInfo gameInfo = this.gameMgr.game.pokerGameInfo;
        LiengPlayerInfo pokerInfo = gp.spInfo.pokerInfo;
        SendTakeTurn msg = new SendTakeTurn();
        msg.action = turn.action;
        msg.chair = this.gameMgr.currentChair;
        msg.currentBet = pokerInfo.moneyBet;
        msg.currentMoney = pokerInfo.currentMoney;
        msg.maxBet = gameInfo.maxBetMoney;
        msg.raiseAmount = turn.raiseAmount;
        msg.raiseStep = gameInfo.bigBlindMoney;
        msg.raiseBlock = gameInfo.raiseBlock = this.checkRaiseBlock();
        msg.potMoney = gameInfo.potMoney;
        Debug.trace((Object[])new Object[]{"notifyTakeTurn:", GameUtils.toJsonString((Object)((Object)msg))});
        this.findNextChair();
        this.logTakeTurn(gp, turn);
        this.send(msg);
    }

    private boolean checkRaiseBlock() {
        int foldCount = 0;
        int allInCount = 0;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            if (gp.spInfo.pokerInfo.allIn) {
                ++allInCount;
                continue;
            }
            if (!gp.spInfo.pokerInfo.fold) continue;
            ++foldCount;
        }
        return allInCount + foldCount == this.playingCount - 1;
    }

    private void logTakeTurn(GamePlayer gp, Turn turn) {
        this.gameLog.append("TT<");
        this.gameLog.append(gp.chair).append(";");
        this.gameLog.append(turn.action).append(";-");
        this.gameLog.append(turn.raiseAmount);
        this.gameLog.append(">");
    }

    public boolean checkEndGameByFoldAndAllIn() {
        Debug.trace((Object)"checkEndGameByFoldAndAllIn");
        LiengGameInfo potInfo = this.gameMgr.game.pokerGameInfo;
        int foldCount = 0;
        int allInCount = 0;
        int callCount = 0;
        for (int i = 0; i < 9; ++i) {
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
            ++callCount;
        }
        Round round = this.gameMgr.game.getCurrentRound();
        if (allInCount == this.playingCount - foldCount || allInCount == this.playingCount - foldCount - 1 && callCount == 1) {
            Debug.trace((Object[])new Object[]{"Tat ca all in", "allInCount=", allInCount, "playingCount=", this.playingCount, "foldCount=", foldCount});
            return round.turns.size() >= 2 * this.playingCount - 1;
        }
        if (foldCount == this.playingCount - 1) {
            Debug.trace((Object[])new Object[]{"Tat ca up bai", "allInCount=", allInCount, "playingCount=", this.playingCount, "foldCount=", foldCount});
            return round.turns.size() >= 2 * this.playingCount - 1;
        }
        return false;
    }

    public boolean checkNewRound() {
        boolean flag;
        Round round = this.gameMgr.game.getCurrentRound();
        LiengGameInfo potInfo = this.gameMgr.game.pokerGameInfo;
        int allInCount = 0;
        int foldCount = 0;
        int callCount = 0;
        for (int i = 0; i < 9; ++i) {
            GamePlayer gp = this.getPlayerByChair(i);
            if (!gp.isPlaying()) continue;
            LiengPlayerInfo info = gp.spInfo.pokerInfo;
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
        int size = round.turns.size();
        boolean bl = flag = size >= 2 * this.playingCount;
        if (flag && allInCount + foldCount + callCount == this.playingCount) {
            this.gameMgr.gameAction = 3;
            this.gameMgr.countDown = 1;
            return true;
        }
        return false;
    }

    public void findNextChair() {
        int from;
        for (int i = from = this.gameMgr.currentChair + 1; i < 9 + from; ++i) {
            int currentChair = i % 9;
            GamePlayer gp = this.getPlayerByChair(currentChair);
            if (!gp.isPlaying() || gp.spInfo.pokerInfo.fold || gp.spInfo.pokerInfo.allIn) continue;
            this.gameMgr.currentChair = currentChair;
            Debug.trace((Object[])new Object[]{"Find next chair", this.gameMgr.currentChair});
            break;
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
            for (int i = 0; i < 9; ++i) {
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
                LiengGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

