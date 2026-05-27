/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.Debug
 *  com.vinplay.gamebai.entities.XocDiaBoss
 *  com.vinplay.usercore.logger.MoneyLogger
 *  com.vinplay.usercore.service.MoneyInGameService
 *  com.vinplay.usercore.service.XocDiaService
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.usercore.service.impl.XocDiaServiceImpl
 *  com.vinplay.vbee.common.enums.FreezeInGame
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.modules.bot.BotXocDiaManager
 *  game.modules.gameRoom.entities.BanUserManager
 *  game.modules.gameRoom.entities.BossManager
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  game.modules.gameRoom.entities.GameRoom
 *  game.modules.gameRoom.entities.GameRoomGroup
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 *  game.modules.gameRoom.entities.GameRoomManager
 *  game.modules.gameRoom.entities.GameRoomSetting
 *  game.modules.gameRoom.entities.GameServer
 *  game.modules.gameRoom.entities.ListGameMoneyInfo
 *  game.modules.gameRoom.entities.ThongTinThangLon
 *  game.utils.GameUtils
 *  game.utils.NumberUtils
 *  game.xocdia.conf.XocDiaBetBotModel
 *  game.xocdia.conf.XocDiaConfig
 *  game.xocdia.conf.XocDiaForceResult
 *  org.json.JSONObject
 *  scala.collection.mutable.StringBuilder
 *  scala.util.Random
 */
package game.xocdia.server;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.gamebai.entities.XocDiaBoss;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.service.MoneyInGameService;
import com.vinplay.usercore.service.XocDiaService;
import com.vinplay.usercore.service.impl.MoneyInGameServiceImpl;
import com.vinplay.usercore.service.impl.XocDiaServiceImpl;
import com.vinplay.vbee.common.enums.FreezeInGame;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.modules.bot.BotXocDiaManager;
import game.modules.gameRoom.entities.BanUserManager;
import game.modules.gameRoom.entities.BossManager;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomIdGenerator;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ListGameMoneyInfo;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.utils.GameUtils;
import game.utils.NumberUtils;
import game.xocdia.bot.BotBettingModel;
import game.xocdia.bot.BotPurchaseModel;
import game.xocdia.bot.BotRejectModel;
import game.xocdia.bot.BotRequestBankerModel;
import game.xocdia.bot.BotSellPotModel;
import game.xocdia.cmd.rev.AllInCmd;
import game.xocdia.cmd.rev.BetCmd;
import game.xocdia.cmd.rev.BuyPotCmd;
import game.xocdia.cmd.rev.ChatCmd;
import game.xocdia.cmd.rev.CheatCmd;
import game.xocdia.cmd.rev.RegisChangeLockPotCmd;
import game.xocdia.cmd.rev.RejectCmd;
import game.xocdia.cmd.rev.ReqKickRoomCmd;
import game.xocdia.cmd.rev.SellPotCmd;
import game.xocdia.cmd.send.ActionGameMsg;
import game.xocdia.cmd.send.BetMsg;
import game.xocdia.cmd.send.BuyPotMsg;
import game.xocdia.cmd.send.ChatMsg;
import game.xocdia.cmd.send.DestroyBankerMsg;
import game.xocdia.cmd.send.GetRevenueMsg;
import game.xocdia.cmd.send.JoinRoomMsg;
import game.xocdia.cmd.send.LeaveRoomMsg;
import game.xocdia.cmd.send.OutRoomMsg;
import game.xocdia.cmd.send.PurchaseInfoMsg;
import game.xocdia.cmd.send.RefundMsg;
import game.xocdia.cmd.send.RegisChangeLockPotMsg;
import game.xocdia.cmd.send.RegisLeaveRoomMsg;
import game.xocdia.cmd.send.RejectInfoMsg;
import game.xocdia.cmd.send.RejectMsg;
import game.xocdia.cmd.send.ReqKickRoomMsg;
import game.xocdia.cmd.send.RequestBankerFailMsg;
import game.xocdia.cmd.send.RequestBankerMsg;
import game.xocdia.cmd.send.RequestDestroyRoomMsg;
import game.xocdia.cmd.send.ResultMsg;
import game.xocdia.cmd.send.RsListMsg;
import game.xocdia.cmd.send.SellPotMsg;
import game.xocdia.cmd.send.StartGameMsg;
import game.xocdia.cmd.send.StopGameMsg;
import game.xocdia.cmd.send.TimeMsg;
import game.xocdia.cmd.send.UpdateMoneyMsg;
import game.xocdia.cmd.send.UserJoinRoomMsg;
import game.xocdia.cmd.send.UserLeaveRoomMsg;
import game.xocdia.conf.XocDiaBetBotModel;
import game.xocdia.conf.XocDiaConfig;
import game.xocdia.conf.XocDiaForceResult;
import game.xocdia.entities.CauXocDia;
import game.xocdia.entities.GamePlayer;
import game.xocdia.entities.GamePot;
import game.xocdia.entities.LogBettingModel;
import game.xocdia.entities.PotType;
import game.xocdia.entities.RefundModel;
import game.xocdia.entities.RewardModel;
import game.xocdia.entities.SubBanker;
import game.xocdia.utils.MsgUtils;
import game.xocdia.utils.XocDiaResult;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import scala.collection.mutable.StringBuilder;
import scala.util.Random;

public class XocDiaGameServer
extends GameServer {
    private final Runnable gameLoopTask = new GameLoopTask();
    private ScheduledFuture<?> task;
    private final Runnable bossTask = new BossTask();
    private ScheduledFuture<?> taskBoss;
    private final Runnable botTask = new BotTask();
    private ScheduledFuture<?> taskBot;
    private MoneyInGameService service = new MoneyInGameServiceImpl();
    private XocDiaService xdService = new XocDiaServiceImpl();
    private Random rd = new Random();
    private static final byte GS_NOT_READY = 0;
    private static final byte GS_PREPARE = 1;
    private static final byte GS_BETTING = 2;
    private static final byte GS_PURCHASE = 3;
    private static final byte GS_REJECT = 4;
    private static final byte GS_BALANCE = 5;
    private static final byte GS_REWARD = 6;
    private static final byte PRE_NEW_GAME_TIME = 5;
    private static final byte BETTING_TIME = 30;
    private static final byte PURCHASE_TIME = 10;
    private static final byte REJECT_TIME = 5;
    private static final byte BALANCE_TIME = 5;
    private static final byte REWARD_TIME = 15;
    private static final byte START_NEW_GAME_TIME = 0;
    private static final byte START_BETTING_TIME = 5;
    private static final byte START_PURCHASE_TIME = 35;
    private static final byte START_REJECT_TIME = 45;
    private static final byte START_BALANCE_TIME = 50;
    private static final byte START_REWARD_TIME = 55;
    private static final byte START_FINISH_TIME = 70;
    private GameRoom room;
    private int roomId;
    private int roomType;
    private int maxUsers;
    private long fundMin;
    private long fundInitial;
    private int moneyBet;
    private byte moneyType;
    private double feeWin;
    private double bankerFee;
    private String sMoneyType;
    private long moneyRegisBankerMin;
    private long moneyHoldBankerMin;
    private Vector<GamePot> potList;
    private BlockingDeque<String> reqBankerList;
    private volatile Map<String, Long> subBankerList;
    private volatile int purchaseStatus;
    private volatile long moneyDif;
    private volatile boolean finishStep;
    private volatile boolean enableBetting;
    private volatile byte gameState;
    private volatile int countTime;
    private volatile int gameId;
    private Map<String, GamePlayer> playerList;
    private volatile boolean isRegisterLoop;
    private List<Byte> rsList;
    private int totalEven;
    private int totalOdd;
    private volatile String bankerName;
    private volatile StringBuilder gameLog;
    private volatile String lastBetting;
    private BlockingDeque<LogBettingModel> logBettingList;
    private boolean isBetting;
    private List<Integer> rsCheat;
    private long totalReveneu;
    private long totalFee;
    private volatile long moneyPurchaseEven;
    private volatile long moneyPurchaseOdd;
    private volatile long moneySell;
    private volatile long moneyBuy;
    private volatile int potPurchase;
    private volatile boolean isBankerReject;
    private CauXocDia cauXD;
    private int cntDate;
    private int transInDay;
    private int transInWeek;
    private volatile boolean isLock;
    private volatile boolean reqDestroyGame;
    private List<BotBettingModel> botBettingList;
    private List<BotPurchaseModel> botPurchaseList;
    private List<BotRequestBankerModel> botReqBankerList;
    private BotSellPotModel botSellPot;
    private BotRejectModel botReject;
    private boolean emty;
    private long timeSellPot;
    private boolean bCheckBalanceBot;
    private List<Integer> listCoins;

    public void init(GameRoom room) {
        try {
            this.room = room;
            this.roomId = room.getId();
            this.roomType = room.setting.rule;
            this.maxUsers = room.setting.maxUserPerRoom;
            this.moneyBet = (int)room.setting.moneyBet;
            this.fundMin = Math.round((double)this.moneyBet * XocDiaConfig.fundVipMinHold);
            this.fundInitial = 0L;
            this.moneyType = (byte)room.setting.moneyType;
            this.feeWin = (double)room.setting.commisionRate / 100.0;
            this.bankerFee = XocDiaConfig.bankerFee;
            this.sMoneyType = this.moneyType == 1 ? "vin" : "xu";
            this.moneyRegisBankerMin = Math.round(XocDiaConfig.minRegisBanker * this.moneyBet);
            this.moneyHoldBankerMin = Math.round(XocDiaConfig.minHoldBanker * this.moneyBet);
            Debug.trace((Object)("START INIT ROOM, roomId: " + this.roomId + ", roomType: " + this.roomType + ", maxUsers: " + this.maxUsers + ", moneyBet: " + this.moneyBet + ", sMoneyType: " + this.sMoneyType + ", feeWin: " + this.feeWin + ", bankerFee: " + this.bankerFee + ", moneyRegisBankerMin: " + this.moneyRegisBankerMin + ", moneyHoldBankerMin: " + this.moneyHoldBankerMin + ", fundMin: " + this.fundMin));
            this.gameId = 0;
            this.potList = new Vector(6);
            for (int i = 0; i < 6; ++i) {
                PotType pt = PotType.findPotType(i);
                this.potList.add(new GamePot(pt.getId(), pt.getRatio(), pt.getName(), Math.round(pt.getMaxRatioBet() * (double)this.moneyBet)));
            }
            this.playerList = new ConcurrentHashMap<String, GamePlayer>();
            this.purchaseStatus = 0;
            this.moneyDif = 0L;
            this.finishStep = false;
            this.enableBetting = false;
            this.gameState = 0;
            this.countTime = -1;
            this.isRegisterLoop = false;
            this.rsList = new ArrayList<Byte>();
            this.totalEven = 0;
            this.totalOdd = 0;
            this.bankerName = "";
            this.reqBankerList = new LinkedBlockingDeque<String>();
            this.subBankerList = new ConcurrentHashMap<String, Long>();
            this.gameLog = new StringBuilder("");
            this.logBettingList = new LinkedBlockingDeque<LogBettingModel>();
            this.isBetting = false;
            this.cauXD = null;
            this.cntDate = 0;
            this.transInDay = 0;
            this.transInWeek = 0;
            this.reqDestroyGame = false;
            this.isLock = false;
            this.botBettingList = new LinkedList<BotBettingModel>();
            this.botPurchaseList = new LinkedList<BotPurchaseModel>();
            this.botReqBankerList = new LinkedList<BotRequestBankerModel>();
            this.botSellPot = null;
            this.botReject = null;
            this.emty = true;
            this.timeSellPot = 0L;
            this.bCheckBalanceBot = this.bCheckBalanceBot();
            this.listCoins = this.roomType == 1 ? XocDiaConfig.listCoinsGlobal : XocDiaConfig.listCoinsNormal;
            Debug.trace((Object[])new Object[]{"INIT ROOM SUCCESS", this.roomId, this.gameId});
            if (this.bCheckBalanceBot && this.moneyBet <= XocDiaConfig.normalMaxRoom && this.roomType != 2) {
                BotXocDiaManager.instance().regJoinRoom(room);
            }
        }
        catch (Exception e) {
            Debug.trace((Object)("INIT ROOM ERROR: " + e.getMessage()));
            Debug.trace((Object)e);
        }
    }

    public synchronized void init() {
        try {
            if (!this.isRegisterLoop) {
                GamePlayer banker;
                Debug.trace((Object[])new Object[]{"START LOOP", this.roomId, this.gameId});
                if (!this.bankerName.isEmpty() && (banker = this.getPlayer(this.bankerName)) != null) {
                    banker.setPlaying(this.roomId);
                    this.setPlayer(this.bankerName, banker);
                }
                this.task = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 0, 1, TimeUnit.SECONDS);
                if (this.bCheckBalanceBot) {
                    this.taskBot = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.botTask, 0, XocDiaConfig.timeTaskBotInGame, TimeUnit.MILLISECONDS);
                }
                this.isRegisterLoop = true;
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: init() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private synchronized void prepareNewGame(boolean isNextGame) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                if (this.gameState == 0) {
                    Debug.trace((Object[])new Object[]{"PREPARE NEW GAME", this.roomId, this.gameId});
                    this.destroyLoop();
                    this.checkBanker();
                    for (Map.Entry<String, GamePlayer> entry : this.playerList.entrySet()) {
                        byte rs;
                        GamePlayer gp;
                        if (this.bankerName.equals(entry.getKey()) || (rs = (gp = entry.getValue()).isLeaveRoom(this.reqDestroyGame)) == 0) continue;
                        gp.playerStatus = 1;
                        this.setPlayer(entry.getKey(), gp);
                        this.leaveRoom(gp.user);
                        if (rs == 4) continue;
                        this.notifyOutRoom(gp, rs);
                    }
                    this.reset();
                    if (this.playerList.size() >= 2) {
                        Debug.trace((Object[])new Object[]{"\u0110\u1ee7 2 ng\u01b0\u1eddi ch\u01a1i, b\u1eaft \u0111\u1ea7u game m\u1edbi", this.roomId, this.gameId});
                        this.init();
                    } else if (isNextGame) {
                        Debug.trace((Object[])new Object[]{"Kh\u00f4ng \u0111\u1ee7 ng\u01b0\u1eddi ch\u01a1i => k\u1ebft th\u00fac game", this.roomId, this.gameId});
                        this.notifyStopGame();
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: prepareNewGame() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private void checkBanker() {
        try {
            if (this.isBoss(this.bankerName)) {
                Debug.trace((Object[])new Object[]{"CHECK BOSS", this.roomId, this.gameId});
                GamePlayer gBoss = this.getPlayer(this.bankerName);
                if (gBoss != null && (gBoss.reqDestroyRoom || gBoss.getMoneyUseInGame() < this.fundMin || this.reqDestroyGame)) {
                    this.destroyBoss(gBoss, (byte)6);
                }
            } else {
                GamePlayer gp;
                Debug.trace((Object[])new Object[]{"CHECK BANKER", this.roomId, this.gameId});
                if (!this.bankerName.isEmpty() && (gp = this.getPlayer(this.bankerName)) != null) {
                    MoneyResponse mnresBanker;
                    long moneyHoldBankerMin = this.getMoneyHoldBankerMin();
                    boolean holdBanker = false;
                    byte rs = gp.isLeaveRoom(this.reqDestroyGame);
                    if (!gp.reqDestroyBanker && rs == 0 && (mnresBanker = gp.gameMoneyInfo.addFreezeMoney(moneyHoldBankerMin, this.roomId, this.gameId, FreezeInGame.ALL_MIN)).isSuccess()) {
                        holdBanker = true;
                        this.setPlayer(this.bankerName, gp);
                    }
                    if (!holdBanker) {
                        this.rejectBanker(gp, rs, gp.reqDestroyBanker);
                    }
                }
                if (this.bankerName.isEmpty()) {
                    int qSize = this.reqBankerList.size();
                    long moneyRegisBankerMin = this.getMoneyRegisBankerMin();
                    boolean checkDone = false;
                    for (int i = 0; i < qSize; ++i) {
                        String nickname = this.reqBankerList.poll();
                        GamePlayer gp2 = this.getPlayer(nickname);
                        if (gp2 == null) continue;
                        if (!checkDone) {
                            byte rs = gp2.isLeaveRoom(this.reqDestroyGame);
                            if (rs != 0) continue;
                            MoneyResponse mnres = gp2.gameMoneyInfo.addFreezeMoney(moneyRegisBankerMin, this.roomId, this.gameId, FreezeInGame.ALL_MIN);
                            if (mnres.isSuccess()) {
                                this.bankerName = gp2.user.getName();
                                gp2.isBanker = true;
                                this.setPlayer(nickname, gp2);
                                checkDone = true;
                                if (!gp2.isBot) continue;
                                BotXocDiaManager.instance().updateBanker(this.roomId, false);
                                continue;
                            }
                            this.notifyRequestBankerFail(gp2, (byte)1);
                            continue;
                        }
                        this.notifyRequestBankerFail(gp2, (byte)2);
                    }
                }
                this.reqBankerList.clear();
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: checkBanker() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private void destroyBoss(GamePlayer gBoss, byte reason) {
        try {
            Debug.trace((Object[])new Object[]{"Destroy Boss reason: " + reason, this.roomId, this.gameId});
            this.isLock = true;
            long feeBoss = 0L;
            long revenue = gBoss.getMoneyUseInGame() - this.fundInitial;
            if (revenue > 0L && (feeBoss = Math.round((double)revenue * XocDiaConfig.bossFee)) > 0L) {
                revenue -= feeBoss;
                gBoss.gameMoneyInfo.updateMoney(-feeBoss, this.roomId, this.gameId, 0L, false);
            }
            Debug.trace((Object)("Nha cai huy ban, nickname: " + this.bankerName + ", reason: " + reason + ", revenue: " + revenue + ", feeBoss: " + feeBoss));
            this.xdService.updateRoomBoss(gBoss.gameMoneyInfo.sessionId, this.bankerName, 0L, (int)feeBoss, revenue, 2);
            BossManager.instance().removeBoss(this.roomId);
            this.reqDestroyGame = true;
            String nickname = this.bankerName;
            this.bankerName = "";
            gBoss.isBanker = false;
            gBoss.isBoss = false;
            gBoss.playerStatus = 1;
            this.setPlayer(nickname, gBoss);
            this.leaveRoom(gBoss.user);
            this.notifyOutRoom(gBoss, reason);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: destroyBoss() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private void rejectBanker(GamePlayer gp, byte rs, boolean reqDestroyBanker) {
        try {
            Debug.trace((Object[])new Object[]{"H\u1ee7y \u0111\u0103ng k\u00fd l\u00e0m c\u00e1i ho\u1eb7c kh\u00f4ng \u0111\u1ee7 ti\u1ec1n " + this.bankerName, this.roomId, this.gameId});
            String nickname = this.bankerName;
            this.bankerName = "";
            gp.isBanker = false;
            if (gp.isBot) {
                BotXocDiaManager.instance().updateBanker(this.roomId, true);
            }
            if (rs != 0 || !reqDestroyBanker) {
                gp.playerStatus = 1;
                this.setPlayer(nickname, gp);
                this.leaveRoom(gp.user);
                if (rs != 4) {
                    this.notifyOutRoom(gp, rs);
                }
            } else {
                MoneyResponse mnres = gp.gameMoneyInfo.addFreezeMoney(gp.gameMoneyInfo.requireMoney, this.roomId, this.gameId, FreezeInGame.SET);
                if (mnres.isSuccess()) {
                    this.setPlayer(nickname, gp);
                } else {
                    gp.playerStatus = 1;
                    this.setPlayer(nickname, gp);
                    this.leaveRoom(gp.user);
                    this.notifyOutRoom(gp, (byte)1);
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: rejectBanker() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private void reset() {
        try {
            Debug.trace((Object[])new Object[]{"RESET NEW GAME", this.roomId, this.gameId});
            for (int i = 0; i < 6; ++i) {
                GamePot gPot = this.getPot(i);
                gPot.reset();
                this.setPot(i, gPot);
            }
            for (Map.Entry<String, GamePlayer> entry : this.playerList.entrySet()) {
                GamePlayer gp = entry.getValue();
                gp.newGame(this.roomId);
                this.setPlayer(entry.getKey(), gp);
            }
            this.purchaseStatus = 0;
            this.moneyDif = 0L;
            this.subBankerList.clear();
            this.gameLog = new StringBuilder("");
            this.finishStep = false;
            this.enableBetting = false;
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: reset() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void gameLoop() {
        try {
            ++this.countTime;
            switch (this.countTime) {
                case 0: {
                    this.startNewGame();
                    this.finishStep = true;
                    break;
                }
                case 5: {
                    if (this.finishStep) {
                        this.finishStep = false;
                        this.startBetting();
                        break;
                    }
                    Debug.trace((Object[])new Object[]{"Waiting START NEW GAME", this.roomId, this.gameId});
                    --this.countTime;
                    break;
                }
                case 35: {
                    this.startPurchase();
                    this.finishStep = true;
                    break;
                }
                case 45: {
                    this.startReject();
                    this.finishStep = true;
                    break;
                }
                case 50: {
                    if (this.finishStep) {
                        this.finishStep = false;
                        this.startBalance();
                        this.finishStep = true;
                        break;
                    }
                    Debug.trace((Object[])new Object[]{"Waiting BETTING/PURCHASE/REJECT", this.roomId, this.gameId});
                    --this.countTime;
                    break;
                }
                case 55: {
                    if (this.finishStep) {
                        this.finishStep = false;
                        this.startReward();
                        this.finishStep = true;
                        break;
                    }
                    Debug.trace((Object[])new Object[]{"Waiting BALANCE", this.roomId, this.gameId});
                    --this.countTime;
                    break;
                }
                case 70: {
                    if (this.finishStep) {
                        this.finish();
                        break;
                    }
                    Debug.trace((Object[])new Object[]{"Waiting REWARD", this.roomId, this.gameId});
                    --this.countTime;
                    break;
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: gameLoop() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void bossLoop() {
        try {
            ++this.cntDate;
            if (this.transInDay < XocDiaConfig.transMinInDay) {
                MsgUtils.alertBoss("Ban xoc dia " + this.room.setting.roomName + " khong du " + XocDiaConfig.transMinInDay + " giao dich 1 ngay. Vui long moi nguoi vao choi", this.bankerName);
            }
            this.transInDay = 0;
            if (this.cntDate % 7 == 0) {
                int transMin = XocDiaConfig.getTransMinInWeek((int)this.moneyBet);
                if (this.transInWeek < transMin) {
                    String content = "Ban xoc dia " + this.room.setting.roomName + " da bi huy do khong du " + transMin + " giao dich 1 tuan";
                    MsgUtils.alertBoss(content, this.bankerName);
                    this.reqDestroyGame = true;
                    if (this.gameState == 0) {
                        this.prepareNewGame(true);
                    }
                }
                this.transInWeek = 0;
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: bossLoop() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void botLoop() {
        try {
            switch (this.gameState) {
                case 2: {
                    this.botBetting();
                    break;
                }
                case 3: {
                    this.botPurchase();
                    break;
                }
                case 4: {
                    this.botReject();
                    break;
                }
            }
            this.getBotReqBanker();
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: botLoop() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void destroyLoop() {
        try {
            Debug.trace((Object[])new Object[]{"DESTROY LOOP", this.roomId, this.gameId});
            this.countTime = -1;
            if (this.task != null && !this.task.isCancelled()) {
                this.task.cancel(false);
            }
            if (this.taskBot != null && !this.taskBot.isCancelled()) {
                this.taskBot.cancel(false);
            }
            this.isRegisterLoop = false;
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: destroyLoop() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void startNewGame() {
        try {
            this.gameId = GameRoomIdGenerator.instance().getId();
            if (this.moneyType == 1) {
                BotXocDiaManager.instance().startNewGame(this.roomId, this.gameId);
            }
            Debug.trace((Object[])new Object[]{"START NEW GAME", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
            this.gameLog.append("XDBD<").append(this.moneyType).append(";");
            this.botBettingList.clear();
            this.botPurchaseList.clear();
            this.botReqBankerList.clear();
            this.botSellPot = null;
            this.botReject = null;
            for (Map.Entry<String, GamePlayer> entry : this.playerList.entrySet()) {
                GamePlayer gp = entry.getValue();
                if (!gp.isSitting()) continue;
                this.gameLog.append(entry.getKey()).append("/").append(gp.isBanker ? 1 : 0).append("/").append(gp.getMoneyUseInGame()).append(";");
                if (!this.bCheckBalanceBot) continue;
                this.getBotPlayInGame(gp, this.getPot(2), this.getPot(3), this.getPot(4), this.getPot(5), this.bankerName.isEmpty() && this.roomType == 0 && BotXocDiaManager.instance().checkRequestBanker());
            }
            this.totalReveneu = 0L;
            this.totalFee = 0L;
            this.moneyPurchaseEven = 0L;
            this.moneyPurchaseOdd = 0L;
            this.moneySell = 0L;
            this.moneyBuy = 0L;
            this.potPurchase = -1;
            this.isBankerReject = false;
            this.timeSellPot = 0L;
            this.gameState = 1;
            this.notifyActionGamme((byte)1, (byte)5);
            this.notifyStartGame();
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: startNewGame() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private void getBotPlayInGame(GamePlayer gp, GamePot pot4T, GamePot pot4D, GamePot pot1T, GamePot pot1D, boolean isCheckReqBanker) {
        try {
            if (gp == null) {
                return;
            }
            long moneyUser = gp.getMoneyUseInGame();
            long totalBet = 0L;
            boolean isNext = true;
            if (gp.isBot && !gp.user.getName().equals(this.bankerName) && moneyUser >= (long)this.moneyBet) {
                int betStartTime;
                long money;
                byte potChanLe = 10;
                if (NumberUtils.isDoWithRatio((double)XocDiaConfig.normalRatioBetChanLe)) {
                    potChanLe = (byte)this.rd.nextInt(2);
                    long money2 = 0L;
                    money2 = this.moneyBet == 100 ? (long)(this.moneyBet * NumberUtils.randomIntLimit((int)XocDiaConfig._100BetChanLeMin, (int)XocDiaConfig._100BetChanLeMax)) : (long)(this.moneyBet * NumberUtils.randomIntLimit((int)XocDiaConfig.normalBetChanLeMin, (int)XocDiaConfig.normalBetChanLeMax));
                    if ((totalBet += money2) > moneyUser) {
                        money2 = moneyUser;
                        isNext = false;
                    }
                    int betStartTime2 = NumberUtils.randomIntLimit((int)6, (int)13);
                    this.botBettingList.add(new BotBettingModel(gp.user, potChanLe, money2, betStartTime2));
                }
                if (!(!isNext || this.bankerName.isEmpty() || potChanLe == PotType.EVEN.getId() || !NumberUtils.isDoWithRatio((double)XocDiaConfig.normalRatioBet1) || pot1T.isLock && pot1D.isLock)) {
                    byte potId = (byte)NumberUtils.randomIntLimit((int)4, (int)5);
                    if (pot1T.isLock) {
                        potId = 5;
                    } else if (pot1D.isLock) {
                        potId = 4;
                    }
                    money = 0L;
                    money = this.moneyBet == 100 ? (long)(this.moneyBet * NumberUtils.randomIntLimit((int)XocDiaConfig._100Bet1Min, (int)XocDiaConfig._100Bet1Max)) : (long)(this.moneyBet * NumberUtils.randomIntLimit((int)XocDiaConfig.normalBet1Min, (int)XocDiaConfig.normalBet1Max));
                    if ((totalBet += money) > moneyUser) {
                        money = moneyUser - totalBet + money;
                        isNext = false;
                    }
                    betStartTime = NumberUtils.randomIntLimit((int)6, (int)13);
                    this.botBettingList.add(new BotBettingModel(gp.user, potId, money, betStartTime));
                }
                if (!(!isNext || this.bankerName.isEmpty() || potChanLe == PotType.ODD.getId() || !NumberUtils.isDoWithRatio((double)XocDiaConfig.normalRatioBet4) || pot4T.isLock && pot4D.isLock)) {
                    byte potId = (byte)NumberUtils.randomIntLimit((int)2, (int)3);
                    if (pot4T.isLock) {
                        potId = 3;
                    } else if (pot4D.isLock) {
                        potId = 2;
                    }
                    money = 0L;
                    money = this.moneyBet == 100 ? (long)(this.moneyBet * NumberUtils.randomIntLimit((int)XocDiaConfig._100Bet4Min, (int)XocDiaConfig._100Bet4Max)) : (long)(this.moneyBet * NumberUtils.randomIntLimit((int)XocDiaConfig.normalBet4Min, (int)XocDiaConfig.normalBet4Max));
                    if ((totalBet += money) > moneyUser) {
                        money = moneyUser - totalBet + money;
                        isNext = false;
                    }
                    betStartTime = NumberUtils.randomIntLimit((int)6, (int)13);
                    this.botBettingList.add(new BotBettingModel(gp.user, potId, money, betStartTime));
                }
                if (isNext && NumberUtils.isDoWithRatio((double)XocDiaConfig.normalRatioBuyPot)) {
                    int moneyBuy = 0;
                    moneyBuy = this.moneyBet == 100 ? NumberUtils.randomIntLimit((int)XocDiaConfig._100BuyPotMin, (int)XocDiaConfig._100BuyPotMax) : NumberUtils.randomIntLimit((int)XocDiaConfig.normalBuyPotMin, (int)XocDiaConfig.normalBuyPotMax);
                    int buyStartTime = NumberUtils.randomIntLimit((int)37, (int)44);
                    this.botPurchaseList.add(new BotPurchaseModel(gp.user, moneyBuy, buyStartTime));
                }
                if (isCheckReqBanker && this.botReqBankerList.size() < XocDiaConfig.bkNumBotReqBankerMax && gp.getMoneyUseInGame() > (long)Math.round(XocDiaConfig.bkMoneyRequestBankerMin * this.moneyBet)) {
                    int reqStartTime = NumberUtils.randomIntLimit((int)6, (int)65);
                    this.botReqBankerList.add(new BotRequestBankerModel(gp.user, reqStartTime));
                }
            }
            if (this.roomType == 0 && gp.isBot && this.bankerName.equals(gp.user.getName())) {
                boolean isSellPot = NumberUtils.isDoWithRatio((double)XocDiaConfig.bkRatioSellPot);
                int sellStartTime = NumberUtils.randomIntLimit((int)36, (int)40);
                this.botSellPot = new BotSellPotModel(gp.user, isSellPot, 0.0, sellStartTime);
                byte action = NumberUtils.isDoWithRatio((double)XocDiaConfig.bkRatioReject) ? (byte)2 : 1;
                int rejectStartTime = NumberUtils.randomIntLimit((int)46, (int)49);
                this.botReject = new BotRejectModel(gp.user, action, rejectStartTime);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: getBotPlayInGame() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void botBetting() {
        try {
            for (int i = 0; i < this.botBettingList.size(); ++i) {
                BotBettingModel model;
                if (!NumberUtils.isDoWithRatio((double)XocDiaConfig.ratioBotBettingInGame) || (model = this.botBettingList.get(i)) == null || this.countTime < model.betStartTime) continue;
                if (model.money < (long)this.moneyBet) {
                    this.botBettingList.remove(i);
                    continue;
                }
                long money = this.listCoins.get(this.rd.nextInt(this.listCoins.size())) * this.moneyBet;
                boolean remove = false;
                if (money >= model.money) {
                    money = model.money;
                    remove = true;
                } else {
                    model.money -= money;
                    this.botBettingList.set(i, model);
                }
                if (!this.uBet(model.user, model.potId, money)) {
                    remove = true;
                }
                if (!remove) continue;
                this.botBettingList.remove(i);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: botBetting() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void botPurchase() {
        block8 : {
            try {
                if (this.moneySell > 0L && this.potPurchase >= 0) {
                    if ((long)this.countTime < this.timeSellPot + 2L || !NumberUtils.isDoWithRatio((double)XocDiaConfig.ratioBotBuyPotInGame)) break block8;
                    for (int i = 0; i < this.botPurchaseList.size(); ++i) {
                        BotPurchaseModel model = this.botPurchaseList.get(i);
                        if ((long)model.buyStartTime >= this.timeSellPot + 2L) {
                            GamePlayer gBot;
                            if (model == null || this.countTime < model.buyStartTime) continue;
                            long moneyBotBuy = Math.round(model.moneyBuy * (double)this.moneyBet);
                            if (moneyBotBuy >= this.moneySell) {
                                moneyBotBuy = this.moneySell;
                            }
                            moneyBotBuy = moneyBotBuy <= (gBot = this.getPlayer(model.user.getName())).getMoneyUseInGame() ? moneyBotBuy : gBot.getMoneyUseInGame();
                            this.uBuyPot(model.user, moneyBotBuy);
                            this.botPurchaseList.remove(i);
                            break block8;
                        }
                        this.botPurchaseList.remove(i);
                    }
                    break block8;
                }
                if (this.botSellPot != null && this.countTime >= this.botSellPot.sellStartTime) {
                    int action = 1;
                    long moneySell = 0L;
                    if (this.botSellPot.isSellPot) {
                        action = this.moneyPurchaseEven > 0L && this.moneyPurchaseOdd > 0L ? (NumberUtils.randomInt((int)2) == 0 ? 2 : 3) : (this.moneyPurchaseEven > 0L ? 3 : 2);
                        moneySell = action == 2 ? Math.round((double)this.moneyPurchaseOdd * ((double)NumberUtils.randomIntLimit((int)XocDiaConfig.bkSellPotMin, (int)XocDiaConfig.bkSellPotMax) / 100.0)) : Math.round((double)this.moneyPurchaseEven * ((double)NumberUtils.randomIntLimit((int)XocDiaConfig.bkSellPotMin, (int)XocDiaConfig.bkSellPotMax) / 100.0));
                    }
                    this.uSellPor(this.botSellPot.user, (byte)action, moneySell);
                    this.botSellPot = null;
                }
            }
            catch (Exception e) {
                String content = "Xoc Dia exception: " + e.getMessage() + ", function: botPurchase() " + this.roomId + " " + this.gameId;
                MsgUtils.alertServer(content, false, false);
                Debug.trace((Object)e);
            }
        }
    }

    private synchronized void botReject() {
        try {
            if (this.botReject != null && this.countTime >= this.botReject.rejectStartTime) {
                this.uReject(this.botReject.user, this.botReject.action);
                this.botReject = null;
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: botReject() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void botRequestBanker(User user) {
        try {
            GamePlayer gp = this.getPlayer(user.getName());
            if (gp != null) {
                this.uRequestBanker(user);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: botRequestBanker() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void getBotReqBanker() {
        try {
            for (int i = 0; i < this.botReqBankerList.size(); ++i) {
                BotRequestBankerModel model = this.botReqBankerList.get(i);
                if (model == null || this.countTime < model.reqStartTime) continue;
                this.botRequestBanker(model.user);
                this.botReqBankerList.remove(i);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: getBotReqBanker() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void startBetting() {
        try {
            Debug.trace((Object[])new Object[]{"START BETTING", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
            this.gameLog.append(">").append("XDDC<");
            this.lastBetting = "";
            this.logBettingList.clear();
            this.isBetting = false;
            this.gameState = (byte)2;
            this.enableBetting = true;
            this.notifyActionGamme((byte)2, (byte)30);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: startBetting() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void stopBetting() {
        try {
            Debug.trace((Object[])new Object[]{"STOP BETTING", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
            this.enableBetting = false;
            int logSize = this.logBettingList.size();
            if (logSize > 0) {
                this.isBetting = true;
                for (int i = 0; i < logSize; ++i) {
                    LogBettingModel model = this.logBettingList.poll();
                    this.gameLog.append(model.getNickname()).append("/").append(model.getPotId()).append("/").append(model.getMoney()).append("/").append(model.getType()).append(";");
                }
            }
            this.gameLog.append(">").append("XDDDC<");
            for (GamePot pot : this.potList) {
                this.gameLog.append(pot.id).append("/").append(pot.totalMoney).append(";");
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: stopBetting() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private void calculateMoneyDif(GamePlayer banker) {
        try {
            long moneyPotEven = this.getPot((int)PotType.EVEN.getId()).totalMoney;
            long moneyPotOdd = this.getPot((int)PotType.ODD.getId()).totalMoney;
            this.moneyDif = moneyPotEven - moneyPotOdd;
            if (banker != null) {
                long moneyBanker = banker.getMoneyUseInGame();
                long moneyEx = moneyBanker + Math.abs(this.moneyDif);
                if (this.moneyDif > 0L) {
                    this.moneyPurchaseEven = moneyEx >= moneyPotEven ? moneyPotEven : moneyEx;
                    this.moneyPurchaseOdd = moneyBanker >= moneyPotOdd ? moneyPotOdd : moneyBanker;
                } else {
                    this.moneyPurchaseOdd = moneyEx >= moneyPotOdd ? moneyPotOdd : moneyEx;
                    this.moneyPurchaseEven = moneyBanker >= moneyPotEven ? moneyPotEven : moneyBanker;
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: calculateMoneyDif() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void startPurchase() {
        try {
            this.stopBetting();
            GamePlayer banker = null;
            if (!this.bankerName.isEmpty()) {
                banker = this.getPlayer(this.bankerName);
            }
            this.calculateMoneyDif(banker);
            if (!this.isBetting) {
                Debug.trace((Object[])new Object[]{"kh\u00f4ng \u0111\u1eb7t c\u1eeda n\u00e0o => k\u1ebft th\u00fac v\u00e1n ch\u01a1i", this.roomId, this.gameId});
                this.countTime = 69;
            } else if (!this.bankerName.isEmpty()) {
                if (this.moneyPurchaseEven > 0L || this.moneyPurchaseOdd > 0L) {
                    Debug.trace((Object[])new Object[]{"START PURCHASE", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
                    this.gameState = (byte)3;
                    this.gameLog.append(">").append("XDMBC<");
                    this.notifyActionGamme((byte)3, (byte)10);
                    PurchaseInfoMsg msg = new PurchaseInfoMsg();
                    msg.moneyEven = this.moneyPurchaseEven;
                    msg.moneyOdd = this.moneyPurchaseOdd;
                    MsgUtils.send(msg, banker.user, banker.revMsg);
                } else {
                    Debug.trace((Object[])new Object[]{"kh\u00f4ng \u0111\u1eb7t ch\u1eb5n l\u1ebb => ho\u00e0n ti\u1ec1n", this.roomId, this.gameId});
                    this.countTime = 49;
                }
            } else if (this.moneyDif != 0L) {
                Debug.trace((Object[])new Object[]{"START PURCHASE", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
                this.gameState = (byte)3;
                this.gameLog.append(">").append("XDMBC<");
                this.notifyActionGamme((byte)3, (byte)10);
                this.moneySell = Math.abs(this.moneyDif);
                this.potPurchase = this.moneyDif > 0L ? PotType.EVEN.getId() : PotType.ODD.getId();
                SellPotMsg msg = new SellPotMsg();
                msg.action = (byte)(this.moneyDif > 0L ? 2 : 3);
                this.gameLog.append("vinplay").append("/").append(msg.action).append("/").append(this.moneySell).append(";");
                this.logGameCSV("vinplay", this.moneySell, "XDMBC<", String.valueOf(msg.action));
                this.purchaseStatus = 1;
                msg.moneySell = this.moneySell;
                MsgUtils.sendToRoom(msg, this.playerList);
            } else {
                Debug.trace((Object[])new Object[]{"2 c\u1eeda ch\u1eb5n l\u1ebb kh\u00f4ng l\u1ec7ch => tr\u1ea3 th\u01b0\u1edfng", this.roomId, this.gameId});
                this.countTime = 54;
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: startPurchase() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void startReject() {
        try {
            if (this.purchaseStatus != 0 && !this.bankerName.isEmpty()) {
                this.purchaseStatus = 2;
                if (this.moneySell > this.moneyBuy) {
                    Debug.trace((Object[])new Object[]{"START REJECT", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
                    this.gameState = (byte)4;
                    this.gameLog.append(">").append("XDTL<");
                    this.notifyActionGamme((byte)4, (byte)5);
                    RejectInfoMsg msg = new RejectInfoMsg();
                    msg.money = this.moneySell - this.moneyBuy;
                    GamePlayer banker = this.getPlayer(this.bankerName);
                    MsgUtils.send(msg, banker.user, banker.revMsg);
                } else {
                    Debug.trace((Object[])new Object[]{"\u0111\u00e3 c\u00f3 user c\u00e2n h\u1ebft c\u1eeda => ho\u00e0n ti\u1ec1n", this.roomId, this.gameId});
                    this.countTime = 49;
                }
            } else {
                Debug.trace((Object[])new Object[]{"nh\u00e0 c\u00e1i kh\u00f4ng thao t\u00e1c \u0111\u1ed3ng ngh\u0129a l\u00e0 c\u00e2n t\u1ea5t => ho\u00e0n ti\u1ec1n", this.roomId, this.gameId});
                this.countTime = 49;
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: startReject() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void startBalance() {
        try {
            this.gameLog.append(">").append("XDHT<");
            Map<String, RefundModel> refundMap = this.systemBalance();
            if (refundMap.size() > 0) {
                Debug.trace((Object[])new Object[]{"START BALANCE", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
                this.gameState = (byte)5;
                this.notifyActionGamme((byte)5, (byte)5);
                RefundMsg msg = new RefundMsg();
                msg.potList = this.potList;
                msg.refundMap = refundMap;
                MsgUtils.sendToRoom(msg, this.playerList);
                this.gameLog.append(">").append("XDDHT<");
                for (GamePot pot : this.potList) {
                    this.gameLog.append(pot.id).append("/").append(pot.totalMoney).append(";");
                }
            } else {
                Debug.trace((Object[])new Object[]{"\u0110\u1ee7 ti\u1ec1n tr\u1ea3, kh\u00f4ng c\u1ea7n ho\u00e0n => tr\u1ea3 th\u01b0\u1edfng lu\u00f4n", this.roomId, this.gameId});
                this.countTime = 54;
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: startBalance() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void startReward() {
        try {
            if (this.checkPotEmty()) {
                Debug.trace((Object[])new Object[]{"ho\u00e0n h\u1ebft ti\u1ec1n c\u00e1c c\u1eeda => k\u1ebft th\u00fac v\u00e1n ch\u01a1i", this.roomId, this.gameId});
                this.countTime = 69;
            } else {
                Debug.trace((Object[])new Object[]{"START REWARD", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
                this.gameState = (byte)6;
                this.gameLog.append(">").append("XDKQ<");
                this.notifyActionGamme((byte)6, (byte)15);
                this.reward();
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: startReward() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private synchronized void finish() {
        try {
            this.gameLog.append(">");
            if (this.isBetting) {
                long logTime = System.currentTimeMillis();
                for (Map.Entry<String, GamePlayer> entry : this.playerList.entrySet()) {
                    GamePlayer gp = entry.getValue();
                    if (!gp.isSitting()) continue;
                    GameUtils.logStartGame((int)this.gameId, (String)entry.getKey(), (long)logTime, (int)this.moneyType);
                }
                GameUtils.logEndGame((int)this.gameId, (String)this.gameLog.toString(), (long)logTime);
            }
            if (this.totalReveneu > 0L || Math.abs(this.totalFee + this.totalReveneu) > 2L) {
                String content = "Xoc Dia errot money, gameId: " + this.gameId + ", totalFee: " + this.totalFee + ", totalReveneu: " + this.totalReveneu;
                MsgUtils.alertServer(content, true, true);
            }
            if (this.totalFee > 0L && this.roomType == 2) {
                ++this.transInDay;
                ++this.transInWeek;
            }
            Debug.trace((Object[])new Object[]{"FINISH", VinPlayUtils.getCurrentDateTime(), this.roomId, this.gameId});
            this.gameState = 0;
            this.prepareNewGame(true);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: finish() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    public synchronized void onGameMessage(User user, DataCmd dataCmd) {
        try {
            switch (dataCmd.getId()) {
                case 3100: {
                    this.regisLeaveRoom(user, dataCmd);
                    break;
                }
                case 3113: {
                    this.requestBanker(user, dataCmd);
                    break;
                }
                case 3106: {
                    this.bet(user, dataCmd);
                    break;
                }
                case 3115: {
                    this.x2(user, dataCmd);
                    break;
                }
                case 3116: {
                    this.allIn(user, dataCmd);
                    break;
                }
                case 3110: {
                    this.sellPot(user, dataCmd);
                    break;
                }
                case 3111: {
                    this.buyPot(user, dataCmd);
                    break;
                }
                case 3127: {
                    this.reject(user, dataCmd);
                    break;
                }
                case 3130: {
                    this.destroyBanker(user, dataCmd);
                    break;
                }
                case 3119: {
                    this.getTime(user, dataCmd);
                    break;
                }
                case 3121: {
                    this.getRsList(user, dataCmd);
                    break;
                }
                case 3124: {
                    this.cheat(user, dataCmd);
                    break;
                }
                case 3008: {
                    this.chat(user, dataCmd);
                    break;
                }
                case 3131: {
                    this.regisChangeLockPot(user, dataCmd);
                    break;
                }
                case 3132: {
                    this.requestKickRoom(user, dataCmd);
                    break;
                }
                case 3133: {
                    this.requestDestroyRoom(user, dataCmd);
                    break;
                }
                case 3134: {
                    this.getRevenue(user, dataCmd);
                    break;
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: onGameMessage() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    public synchronized void onGameUserEnter(User user) {
        block12 : {
            try {
                GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
                Debug.trace((Object)("[DEBUG-JOIN] user=" + user.getName()
                    + " roomId=" + this.roomId
                    + " roomType=" + this.roomType
                    + " gameState=" + this.gameState
                    + " isLock=" + this.isLock
                    + " emty=" + this.emty
                    + " banker=" + this.bankerName
                    + " players=" + this.playerList.size() + "/" + this.maxUsers
                    + " moneyInfo=" + (moneyInfo != null ? "sessionId=" + moneyInfo.sessionId + ",freeze=" + moneyInfo.freezeMoney : "NULL")
                    + " isBot=" + user.isBot()));
                if (this.playerList.size() >= this.maxUsers || this.isLock) {
                    Debug.trace((Object)("[DEBUG-JOIN] BLOCKED user=" + user.getName() + " reason=" + (this.isLock ? "ROOM_LOCKED" : "ROOM_FULL")));
                    break block12;
                }
                PlayerInfo pInfo = PlayerInfo.getInfo((User)user);
                GamePlayer gp = new GamePlayer(user, pInfo, moneyInfo);
                if (this.gameState != 0) {
                    gp.playerStatus = 2;
                }
                if (this.emty && this.roomType == 2) {
                    this.emty = false;
                    this.bankerName = gp.user.getName();
                    gp.isBanker = true;
                    gp.isBoss = true;
                    gp.pInfo.setIsHold(true);
                    PlayerInfo.setRoomId((String)user.getName(), (int)this.roomId);
                    this.taskBoss = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.bossTask, 24, 24, TimeUnit.HOURS);
                    XocDiaBoss boss = BossManager.instance().getBoss(this.roomId);
                    if (boss != null) {
                        this.fundInitial = boss.getFundInitial();
                        Debug.trace((Object)("BOSS JOIN ROOM roomId: " + this.roomId + ", sessionId: " + boss.getSessionId() + ", nickname: " + boss.getNickname() + ", setting: " + boss.getRoomSetting() + ", fundInitial: " + this.fundInitial));
                        if (BossManager.instance().checkBossSysLogin(this.roomId)) {
                            gp.revMsg = false;
                        }
                    } else {
                        try {
                            String bossSessionId = (moneyInfo != null && moneyInfo.sessionId != null) ? moneyInfo.sessionId : GameUtils.gameName + System.currentTimeMillis();
                            long bossFreezeAmount = (moneyInfo != null) ? moneyInfo.freezeMoney : 0L;
                            boss = new XocDiaBoss(bossSessionId, user.getName(), this.roomId, this.room.setting.toString(), bossFreezeAmount, 1, 0, 0L, VinPlayUtils.getCurrentDateTime());
                            this.fundInitial = boss.getFundInitial();
                            BossManager.instance().putBoss(boss);
                        }
                        catch (Exception e) {
                            // Don't lock room permanently — reset so next player can become boss
                            this.emty = true;
                            this.bankerName = "";
                            gp.isBanker = false;
                            gp.isBoss = false;
                            String content = "Xoc Dia create boss exception: " + e.getMessage() + ", function: onGameUserEnter() " + this.roomId + " " + this.gameId;
                            MsgUtils.alertServer(content, false, true);
                            Debug.trace((Object)e);
                        }
                    }
                    // Notify the boss player that they are now the banker (cmd 3113, error=0)
                    if (!this.bankerName.isEmpty()) {
                        RequestBankerMsg bankerMsg = new RequestBankerMsg();
                        bankerMsg.Error = 0;
                        bankerMsg.moneyRequire = 0;
                        MsgUtils.send(bankerMsg, user, gp.revMsg);
                        Debug.trace((Object)("[DEBUG-JOIN] Auto-boss assigned: " + user.getName() + " roomId=" + this.roomId));
                    }
                }
                this.setPlayer(user.getName(), gp);
                this.notifyUserEnter(gp);
                if (this.playerList.size() == 2) {
                    this.prepareNewGame(false);
                } else if (this.playerList.size() == 1 && user.isBot() && this.bankerName.isEmpty() && this.roomType == 0 && NumberUtils.isDoWithRatio((double)XocDiaConfig.bkRatioRequestBanker) && gp.getMoneyUseInGame() >= (long)Math.round(XocDiaConfig.bkMoneyRequestBankerMin * this.moneyBet)) {
                    this.botRequestBanker(user);
                }
            }
            catch (Exception e) {
                String content = "Xoc Dia exception: " + e.getMessage() + ", function: onGameUserEnter() " + this.roomId + " " + this.gameId;
                MsgUtils.alertServer(content, false, true);
                Debug.trace((Object)e);
            }
        }
    }

    public synchronized void onGameUserDis(User user) {
        try {
            Debug.trace((Object[])new Object[]{"onGameUserDis", user.getName(), this.roomId, this.gameId});
            if (this.isBoss(user.getName())) {
                return;
            }
            GamePlayer gp = this.getPlayer(user.getName());
            if (gp != null && !gp.isPlaying()) {
                this.leaveRoom(user);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: onGameUserDis() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    public synchronized void onGameUserReturn(User user) {
        try {
            Debug.trace((Object[])new Object[]{"onGameUserReturn", user.getName(), this.roomId, this.gameId, user.getId()});
            GamePlayer gp = this.getPlayer(user.getName());
            if (gp != null) {
                gp.revMsg = true;
                gp.user = user;
                gp.reqLeaveRoom = false;
                this.setPlayer(user.getName(), gp);
                this.notifyJoinRoom(gp);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: onGameUserReturn() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    public synchronized void onGameUserExit(User user) {
        try {
            Debug.trace((Object[])new Object[]{"onGameUserExit: ", user.getName(), this.roomId, this.gameId});
            GamePlayer gp = this.getPlayer(user.getName());
            if (gp != null) {
                if (gp.isPlaying()) {
                    gp.reqLeaveRoom = true;
                    this.setPlayer(user.getName(), gp);
                } else {
                    this.removePlayerFromRoom(user);
                }
            }
            if (this.playerList.size() == 0) {
                this.destroy();
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: onGameUserExit() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void regisLeaveRoom(User user, DataCmd dataCmd) {
        try {
            if (this.isBoss(user.getName())) {
                GamePlayer gBoss = this.getPlayer(this.bankerName);
                if (gBoss != null) {
                    gBoss.revMsg = false;
                    this.setPlayer(this.bankerName, gBoss);
                }
                return;
            }
            Map<String, GamePlayer> gBoss = this.playerList;
            synchronized (gBoss) {
                GamePlayer gp = this.getPlayer(user.getName());
                if (gp != null) {
                    if (gp.isPlaying() || gp.isBanker && this.playerList.size() > 1) {
                        gp.reqLeaveRoom = !gp.reqLeaveRoom;
                        this.setPlayer(user.getName(), gp);
                        this.notifyRegisLeaveRoom(gp);
                    } else {
                        this.leaveRoom(user);
                        this.notifyOutRoom(gp, (byte)3);
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: regisLeaveRoom() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private void regisChangeLockPot(User user, DataCmd dataCmd) {
        try {
            if (this.isBoss(user.getName())) {
                RegisChangeLockPotCmd cmd = new RegisChangeLockPotCmd(dataCmd);
                byte potId = cmd.pot;
                if (potId != PotType.ODD.getId() && potId != PotType.EVEN.getId()) {
                    Debug.trace((Object[])new Object[]{"Dang ky khoa cua vi", this.roomId, this.gameId, potId});
                    GamePot gPot = this.getPot(potId);
                    if (gPot != null) {
                        gPot.regisChangeLock = !gPot.regisChangeLock;
                        RegisChangeLockPotMsg msg = new RegisChangeLockPotMsg();
                        msg.Error = 0;
                        msg.bChangeLock = gPot.regisChangeLock;
                        MsgUtils.send(msg, user, true);
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: regisChangeLockPot() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void requestKickRoom(User user, DataCmd dataCmd) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                if (this.isBoss(user.getName())) {
                    GamePlayer gp;
                    ReqKickRoomCmd cmd = new ReqKickRoomCmd(dataCmd);
                    String nickname = cmd.nickname;
                    if (!this.isBoss(nickname) && this.playerList.containsKey(nickname) && (gp = this.getPlayer(nickname)) != null) {
                        Debug.trace((Object)("Boss kick user: " + nickname));
                        int reason = 0;
                        if (gp.reqKickRoom) {
                            gp.reqKickRoom = false;
                            this.setPlayer(nickname, gp);
                            reason = 3;
                        } else if (gp.isPlaying()) {
                            gp.reqKickRoom = true;
                            this.setPlayer(nickname, gp);
                            reason = 2;
                        } else {
                            this.leaveRoom(gp.user);
                            this.notifyOutRoom(gp, (byte)7);
                            reason = 1;
                        }
                        ReqKickRoomMsg msg = new ReqKickRoomMsg();
                        msg.Error = 0;
                        msg.reason = (byte)reason;
                        MsgUtils.send(msg, user, gp.revMsg);
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: requestKickRoom() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private void requestBanker(User user, DataCmd dataCmd) {
        this.uRequestBanker(user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void uRequestBanker(User user) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                GamePlayer gp = this.getPlayer(user.getName());
                if (gp != null) {
                    boolean enable = false;
                    if (this.roomType == 0) {
                        GamePlayer gBanker;
                        if (this.bankerName.isEmpty()) {
                            enable = true;
                        } else if (!user.getName().equals(this.bankerName) && (gBanker = this.getPlayer(this.bankerName)) != null && gBanker.reqDestroyBanker) {
                            enable = true;
                        }
                    }
                    if (enable) {
                        this.reqBankerList.offer(user.getName());
                        this.notifyRequestBanker(gp, (byte)0);
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: uRequestBanker() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void destroyBanker(User user, DataCmd dataCmd) {
        block8 : {
            try {
                if (this.roomType != 0) break block8;
                Map<String, GamePlayer> map = this.playerList;
                synchronized (map) {
                    GamePlayer gPlayer;
                    if (user.getName().equalsIgnoreCase(this.bankerName) && (gPlayer = this.getPlayer(this.bankerName)) != null && gPlayer.isBanker && !gPlayer.reqDestroyBanker) {
                        if (this.gameState == 0 && this.playerList.size() == 1) {
                            this.rejectBanker(gPlayer, (byte)0, false);
                            this.notifyStopGame();
                        } else {
                            gPlayer.reqDestroyBanker = !gPlayer.reqDestroyBanker;
                            this.setPlayer(this.bankerName, gPlayer);
                            this.notifyDestroyBanker(gPlayer.reqDestroyBanker);
                        }
                    }
                }
            }
            catch (Exception e) {
                String content = "Xoc Dia exception: " + e.getMessage() + ", function: destroyBanker() " + this.roomId + " " + this.gameId;
                MsgUtils.alertServer(content, false, false);
                Debug.trace((Object)e);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void requestDestroyRoom(User user, DataCmd dataCmd) {
        block8 : {
            try {
                if (!this.isBoss(user.getName())) break block8;
                Map<String, GamePlayer> map = this.playerList;
                synchronized (map) {
                    Debug.trace((Object[])new Object[]{"Request Destroy Boss", this.roomId, this.gameId});
                    if (this.gameState == 0 && this.playerList.size() == 1) {
                        GamePlayer gBoss = this.getPlayer(this.bankerName);
                        if (gBoss != null) {
                            this.destroyBoss(gBoss, (byte)6);
                        }
                    } else {
                        GamePlayer gBoss = this.getPlayer(this.bankerName);
                        gBoss.reqDestroyRoom = !gBoss.reqDestroyRoom;
                        this.setPlayer(this.bankerName, gBoss);
                        RequestDestroyRoomMsg msg = new RequestDestroyRoomMsg();
                        msg.Error = 0;
                        msg.reqDestroyRoom = gBoss.reqDestroyRoom;
                        MsgUtils.sendToRoom(msg, this.playerList);
                    }
                }
            }
            catch (Exception e) {
                String content = "Xoc Dia exception: " + e.getMessage() + ", function: requestDestroyRoom() " + this.roomId + " " + this.gameId;
                MsgUtils.alertServer(content, false, false);
                Debug.trace((Object)e);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void getRevenue(User user, DataCmd dataCmd) {
        block9 : {
            try {
                if (!this.isBoss(user.getName()) || this.gameState != 0 && this.gameState != 1 && this.gameState != 2) break block9;
                Map<String, GamePlayer> map = this.playerList;
                synchronized (map) {
                    GamePlayer gBoss = this.getPlayer(this.bankerName);
                    byte error = -1;
                    long moneyBoss = gBoss.getMoneyUseInGame();
                    if (moneyBoss > this.fundInitial) {
                        long moneyWin = moneyBoss - this.fundInitial;
                        long feeBoss = Math.round((double)moneyWin * XocDiaConfig.bossFee);
                        long revenue = moneyWin - feeBoss;
                        Debug.trace((Object)("Nha cai chot lai, nickname: " + this.bankerName + ", feeBoss: " + feeBoss + ", revenue: " + revenue + ", moneyBoss: " + moneyBoss + ", fundInitial: " + this.fundInitial));
                        MoneyResponse mnres1 = gBoss.gameMoneyInfo.updateMoneyByFreeze(-moneyWin, this.roomId, this.gameId, 0L);
                        if (mnres1.isSuccess()) {
                            MoneyResponse mnres = this.service.updateMoneyUser(this.bankerName, revenue, this.sMoneyType, GameUtils.gameName, "X\u00f3c \u0110\u00eda - Ch\u1ed1t l\u00e3i", "Nh\u00e0 c\u00e1i ch\u1ed1t l\u00e3i", feeBoss, null, TransType.NO_VIPPOINT, true);
                            if (mnres.isSuccess()) {
                                this.xdService.updateRoomBoss(gBoss.gameMoneyInfo.sessionId, this.bankerName, 0L, (int)feeBoss, revenue, 3);
                                error = 0;
                            }
                            this.setPlayer(this.bankerName, gBoss);
                        }
                    } else {
                        error = 1;
                    }
                    GetRevenueMsg msg = new GetRevenueMsg();
                    msg.Error = error;
                    msg.moneyBoss = gBoss.getMoneyUseInGame();
                    MsgUtils.send(msg, user, gBoss.revMsg);
                }
            }
            catch (Exception e) {
                String content = "Xoc Dia exception: " + e.getMessage() + ", function: getRevenue() " + this.roomId + " " + this.gameId;
                MsgUtils.alertServer(content, false, false);
                Debug.trace((Object)e);
            }
        }
    }

    public void bet(User user, DataCmd dataCmd) {
        try {
            BetCmd cmd = new BetCmd(dataCmd);
            byte potId = cmd.pot;
            long money = cmd.money;
            Debug.trace((Object)("[DEBUG-BET] user=" + user.getName() + " pot=" + potId + " money=" + money
                + " roomId=" + this.roomId + " gameState=" + this.gameState + " enableBetting=" + this.enableBetting
                + " banker=" + this.bankerName));
            this.uBet(user, potId, money);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: bet() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean uBet(User user, byte potId, long money) {
        boolean res = false;
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                GamePot gPot = this.getPot(potId);
                GamePlayer gp = this.getPlayer(user.getName());
                boolean checkBet = this.checkBetting(gPot, gp);
                boolean enoughMoney = money >= (long)this.moneyBet;
                if (!checkBet || !enoughMoney) {
                    Debug.trace((Object)("[DEBUG-BET] REJECTED user=" + user.getName() + " pot=" + potId
                        + " checkBetting=" + checkBet + " enoughMoney=" + enoughMoney
                        + " money=" + money + " minBet=" + this.moneyBet
                        + " gp=" + (gp != null ? "exists" : "NULL")
                        + " gPot=" + (gPot != null ? "id=" + gPot.id + ",lock=" + gPot.isLock : "NULL")));
                }
                if (checkBet && enoughMoney) {
                    BetMsg msg = new BetMsg(user.getName());
                    msg.potId = potId;
                    MoneyResponse mnres = gp.gameMoneyInfo.updateMoney(-money, this.roomId, this.gameId, 0L, false);
                    if (mnres.isSuccess()) {
                        gp.setPlaying(this.roomId);
                        long moneySub = mnres.getSubtractMoney();
                        long mnBet = gPot.bet(user.getName(), moneySub, !this.bankerName.isEmpty(), this.moneyType, gp.isBot);
                        if (mnBet > 0L) {
                            this.setPot(potId, gPot);
                            if (mnBet < moneySub) {
                                mnres = gp.gameMoneyInfo.updateMoney(moneySub - mnBet, this.roomId, this.gameId, 0L, false);
                                moneySub = mnBet;
                            }
                            this.totalReveneu -= moneySub;
                            this.logBetting(user.getName(), potId, moneySub, 0);
                            msg.betMoney = moneySub;
                            msg.currentMoney = mnres.getMoneyInGame();
                            msg.potMoney = gPot.totalMoney;
                            msg.Error = 0;
                            MsgUtils.sendToRoom(msg, this.playerList);
                            res = true;
                        } else {
                            msg.currentMoney = mnres.getMoneyInGame();
                            msg.potMoney = gPot.totalMoney;
                            msg.Error = 2;
                            MsgUtils.send(msg, user, gp.revMsg);
                        }
                    } else {
                        msg.currentMoney = mnres.getMoneyInGame();
                        msg.potMoney = gPot.totalMoney;
                        msg.Error = 1;
                        MsgUtils.send(msg, user, gp.revMsg);
                    }
                    this.setPlayer(user.getName(), gp);
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: uBet() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void x2(User user, DataCmd dataCmd) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                long totalMoneyBet = 0L;
                String nickname = user.getName();
                GamePlayer gp = this.getPlayer(nickname);
                if (this.gameState == 2 && !this.bankerName.equals(user.getName()) && this.enableBetting && gp != null && gp.isPlaying()) {
                    HashMap<Byte, Long> uBet = new HashMap<Byte, Long>();
                    for (GamePot gPot : this.potList) {
                        long mPot = gPot.getMoneyX2(nickname, !this.bankerName.isEmpty());
                        if (mPot <= 0L) continue;
                        totalMoneyBet += mPot;
                        uBet.put(gPot.id, mPot);
                    }
                    if (totalMoneyBet > 0L) {
                        BetMsg msg = new BetMsg(user.getName());
                        MoneyResponse mnres = gp.gameMoneyInfo.updateMoney(-totalMoneyBet, this.roomId, this.gameId, 0L, true);
                        if (mnres.isSuccess()) {
                            this.totalReveneu -= totalMoneyBet;
                            long mnRollBack = 0L;
                            for (Map.Entry entry : uBet.entrySet()) {
                                long moneySub = (Long)entry.getValue();
                                GamePot gPot = this.getPot(((Byte)entry.getKey()).byteValue());
                                long mnBet = gPot.bet(user.getName(), moneySub, !this.bankerName.isEmpty(), this.moneyType, gp.isBot);
                                if (mnBet > 0L) {
                                    this.setPot(((Byte)entry.getKey()).byteValue(), gPot);
                                    if (mnBet < moneySub) {
                                        mnRollBack += moneySub - mnBet;
                                        moneySub = mnBet;
                                    }
                                    this.logBetting(user.getName(), gPot.id, moneySub, 0);
                                    msg.potId = gPot.id;
                                    msg.betMoney = moneySub;
                                    msg.currentMoney = mnres.getMoneyInGame();
                                    msg.potMoney = gPot.totalMoney;
                                    msg.Error = 0;
                                    MsgUtils.sendToRoom(msg, this.playerList);
                                    continue;
                                }
                                mnRollBack += moneySub;
                            }
                            if (mnRollBack > 0L) {
                                mnres = gp.gameMoneyInfo.updateMoney(mnRollBack, this.roomId, this.gameId, 0L, false);
                                this.totalReveneu += mnRollBack;
                                UpdateMoneyMsg msgM = new UpdateMoneyMsg();
                                msgM.nickname = user.getName();
                                msgM.money = mnres.getMoneyInGame();
                                MsgUtils.sendToRoom(msgM, this.playerList);
                            }
                        } else {
                            msg.currentMoney = mnres.getMoneyInGame();
                            msg.Error = 1;
                            MsgUtils.send(msg, user, gp.revMsg);
                        }
                        this.setPlayer(nickname, gp);
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: x2() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void allIn(User user, DataCmd dataCmd) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                String nickname = user.getName();
                AllInCmd cmd = new AllInCmd(dataCmd);
                byte potId = cmd.pot;
                GamePot gPot = this.getPot(potId);
                GamePlayer gp = this.getPlayer(nickname);
                if (this.checkBetting(gPot, gp)) {
                    BetMsg msg = new BetMsg(user.getName());
                    msg.potId = potId;
                    long money = gp.getMoneyUseInGame();
                    if (money > 0L) {
                        MoneyResponse mnres = gp.gameMoneyInfo.updateMoney(-money, this.roomId, this.gameId, 0L, false);
                        if (mnres.isSuccess()) {
                            gp.setPlaying(this.roomId);
                            long moneySub = mnres.getSubtractMoney();
                            this.totalReveneu -= moneySub;
                            long mnRollBack = 0L;
                            long mnBet = gPot.bet(user.getName(), moneySub, !this.bankerName.isEmpty(), this.moneyType, gp.isBot);
                            if (mnBet > 0L) {
                                this.setPot(potId, gPot);
                                if (mnBet < moneySub) {
                                    mnRollBack = moneySub - mnBet;
                                    moneySub = mnBet;
                                }
                                this.logBetting(user.getName(), potId, moneySub, 2);
                                msg.betMoney = moneySub;
                                msg.currentMoney = mnres.getMoneyInGame();
                                msg.potMoney = gPot.totalMoney;
                                msg.Error = 0;
                                MsgUtils.sendToRoom(msg, this.playerList);
                            } else {
                                mnRollBack = money;
                                msg.currentMoney = mnres.getMoneyInGame();
                                msg.potMoney = gPot.totalMoney;
                                msg.Error = 2;
                                MsgUtils.send(msg, user, gp.revMsg);
                            }
                            if (mnRollBack > 0L) {
                                this.totalReveneu += mnRollBack;
                                mnres = gp.gameMoneyInfo.updateMoney(mnRollBack, this.roomId, this.gameId, 0L, false);
                                UpdateMoneyMsg msgM = new UpdateMoneyMsg();
                                msgM.nickname = user.getName();
                                msgM.money = mnres.getMoneyInGame();
                                MsgUtils.sendToRoom(msgM, this.playerList);
                            }
                        } else {
                            msg.currentMoney = mnres.getMoneyInGame();
                            msg.potMoney = gPot.totalMoney;
                            msg.Error = 1;
                            MsgUtils.send(msg, user, gp.revMsg);
                        }
                    }
                    this.setPlayer(user.getName(), gp);
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: allIn() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    public boolean checkBetting(GamePot gPot, GamePlayer gp) {
        // Decode the complex condition for debug logging
        boolean wrongState = this.gameState != 2;
        boolean notBetting = !this.enableBetting;
        boolean noPlayer = gp == null;
        boolean notSitting = !noPlayer && !gp.isSitting();
        boolean noPot = gPot == null;
        boolean potLocked = !noPot && gPot.isLock;
        boolean isBanker = !noPlayer && this.bankerName.equals(gp.user.getName());
        boolean noBankerHighPot = this.bankerName.isEmpty() && !noPot && gPot.id != PotType.EVEN.getId() && gPot.id != PotType.ODD.getId();
        if (wrongState || notBetting || noPlayer || notSitting || noPot || potLocked || isBanker || noBankerHighPot) {
            Debug.trace((Object)("[DEBUG-CHECK-BET] FAIL"
                + " user=" + (gp != null ? gp.user.getName() : "null")
                + " pot=" + (gPot != null ? gPot.id : -1)
                + " wrongState=" + wrongState + "(gs=" + this.gameState + ")"
                + " notBetting=" + notBetting
                + " notSitting=" + notSitting
                + " potLocked=" + potLocked
                + " isBanker=" + isBanker
                + " noBankerHighPot=" + noBankerHighPot + "(banker='" + this.bankerName + "')"));
            return false;
        }
        if (!(wrongState || notBetting || noPlayer || notSitting || noPot || potLocked || isBanker || noBankerHighPot)) {
            if (gPot.checkBetLimitUser(gp.user.getName(), !this.bankerName.isEmpty())) {
                BetMsg msg = new BetMsg(gp.user.getName());
                msg.potId = gPot.id;
                msg.potMoney = gPot.totalMoney;
                msg.Error = 2;
                MsgUtils.send(msg, gp.user, gp.revMsg);
                return false;
            }
            return true;
        }
        return false;
    }

    private void sellPot(User user, DataCmd dataCmd) {
        try {
            SellPotCmd cmd = new SellPotCmd(dataCmd);
            byte action = cmd.action;
            long moneySell = cmd.moneySell;
            this.uSellPor(user, action, moneySell);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: sellPot() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void uSellPor(User user, byte action, long moneySell) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                if (this.gameState == 3 && this.bankerName.equals(user.getName()) && this.purchaseStatus == 0) {
                    SellPotMsg msg = new SellPotMsg();
                    msg.action = action;
                    if (action == 1) {
                        this.countTime = 49;
                        MsgUtils.sendExceptMe(msg, user, this.playerList);
                    } else {
                        long money = 0L;
                        int potSell = -1;
                        if (action == 2 && this.moneyPurchaseEven > 0L) {
                            money = this.moneyPurchaseEven;
                            potSell = PotType.EVEN.getId();
                        } else if (action == 3 && this.moneyPurchaseOdd > 0L) {
                            money = this.moneyPurchaseOdd;
                            potSell = PotType.ODD.getId();
                        }
                        if (money > 0L && moneySell > 0L && moneySell <= money) {
                            this.moneySell = moneySell;
                            this.potPurchase = potSell;
                            this.timeSellPot = this.countTime;
                            this.gameLog.append(user.getName()).append("/").append(action).append("/").append(moneySell).append(";");
                            this.logGameCSV(user.getName(), moneySell, "XDMBC<", String.valueOf(action));
                            this.purchaseStatus = 1;
                            msg.moneySell = moneySell;
                            MsgUtils.sendExceptMe(msg, user, this.playerList);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: uSellPor() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private void buyPot(User user, DataCmd dataCmd) {
        try {
            BuyPotCmd cmd = new BuyPotCmd(dataCmd);
            long moneyBuy = cmd.moneyBuy;
            this.uBuyPot(user, moneyBuy);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: buyPot() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean uBuyPot(User user, long moneyBuy) {
        boolean res = false;
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                GamePlayer gp;
                if (this.gameState == 3 && !this.bankerName.equals(user.getName()) && this.purchaseStatus == 1 && moneyBuy > 0L && (gp = this.getPlayer(user.getName())) != null) {
                    BuyPotMsg msg = new BuyPotMsg(user.getName());
                    if (this.moneyBuy < this.moneySell) {
                        boolean limit = false;
                        if (this.moneyBuy + moneyBuy >= this.moneySell) {
                            moneyBuy = this.moneySell - this.moneyBuy;
                            limit = true;
                        }
                        MoneyResponse mnres = null;
                        boolean buyed = false;
                        if (this.subBankerList.containsKey(user.getName())) {
                            buyed = true;
                            mnres = gp.gameMoneyInfo.addFreezeMoney(moneyBuy, this.roomId, this.gameId, FreezeInGame.MORE);
                        } else {
                            mnres = gp.gameMoneyInfo.addFreezeMoney(moneyBuy, this.roomId, this.gameId, FreezeInGame.SET);
                        }
                        if (mnres.isSuccess()) {
                            this.moneyBuy += moneyBuy;
                            if (buyed) {
                                this.subBankerList.put(user.getName(), this.subBankerList.get(user.getName()) + moneyBuy);
                            } else {
                                this.subBankerList.put(user.getName(), moneyBuy);
                            }
                            this.gameLog.append(user.getName()).append("/").append(4).append("/").append(moneyBuy).append(";");
                            this.logGameCSV(user.getName(), moneyBuy, "XDMBC<", String.valueOf(4));
                            gp.setPlaying(this.roomId);
                            gp.isSubBanker = true;
                            this.setPlayer(user.getName(), gp);
                            msg.Error = 0;
                            msg.moneyBuy = moneyBuy;
                            msg.rmMoneySell = this.moneySell - this.moneyBuy;
                            GamePot gPot = this.getPot(this.potPurchase);
                            if (gPot != null) {
                                gPot.buyPot(moneyBuy, gp.isBot);
                            }
                            this.setPot(this.potPurchase, gPot);
                            MsgUtils.sendToRoom(msg, this.playerList);
                            res = true;
                            if (limit) {
                                this.purchaseStatus = 2;
                                this.countTime = 49;
                            }
                        } else {
                            msg.Error = 1;
                            MsgUtils.send(msg, user, gp.revMsg);
                        }
                    } else {
                        msg.Error = 2;
                        MsgUtils.send(msg, user, gp.revMsg);
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: uBuyPot() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
        return res;
    }

    private void reject(User user, DataCmd dataCmd) {
        try {
            RejectCmd cmd = new RejectCmd(dataCmd);
            byte action = cmd.action;
            this.uReject(user, action);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: reject() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void uReject(User user, byte action) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                if (this.gameState == 4 && this.bankerName.equals(user.getName()) && (action == 1 || action == 2)) {
                    if (action == 2) {
                        this.isBankerReject = true;
                    }
                    RejectMsg msg = new RejectMsg();
                    msg.action = action;
                    msg.money = this.moneySell - this.moneyBuy;
                    MsgUtils.sendToRoom(msg, this.playerList);
                    this.gameLog.append(user.getName()).append("/").append(action).append("/").append(msg.money).append(";");
                    this.logGameCSV(user.getName(), 0L, "XDTL<", String.valueOf(action));
                    this.countTime = 49;
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: uReject() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private void getTime(User user, DataCmd dataCmd) {
        try {
            TimeMsg msg = new TimeMsg();
            msg.time = (byte)this.getCountTime();
            MsgUtils.send(msg, user, true);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void getRsList(User user, DataCmd dataCmd) {
        try {
            RsListMsg msg = new RsListMsg();
            msg.totalEven = this.totalEven;
            msg.totalOdd = this.totalOdd;
            msg.rsList = this.rsList;
            MsgUtils.send(msg, user, true);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void cheat(User user, DataCmd dataCmd) {
        try {
            CheatCmd cmd = new CheatCmd(dataCmd);
            this.rsCheat = new ArrayList<Integer>();
            this.rsCheat.add(Integer.valueOf(cmd.dince1));
            this.rsCheat.add(Integer.valueOf(cmd.dince2));
            this.rsCheat.add(Integer.valueOf(cmd.dince3));
            this.rsCheat.add(Integer.valueOf(cmd.dince4));
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void chat(User user, DataCmd data) {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                long now;
                GamePlayer gp = this.getPlayer(user.getName());
                if (gp != null && (now = System.currentTimeMillis()) - gp.lastChatTime >= 3000L) {
                    gp.lastChatTime = now;
                    this.setPlayer(user.getName(), gp);
                    ChatCmd cmd = new ChatCmd(data);
                    ChatMsg msg = new ChatMsg();
                    msg.nickname = user.getName();
                    msg.isIcon = cmd.isIcon;
                    try {
                        msg.content = URLDecoder.decode(cmd.content, "UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        Debug.trace((Object)e);
                        msg.content = cmd.content;
                    }
                    MsgUtils.sendToRoom(msg, this.playerList);
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Map<String, RefundModel> systemBalance() {
        Map<String, RefundModel> refundMap = new HashMap<String, RefundModel>();
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                if (this.bankerName.isEmpty()) {
                    long moneyRef;
                    if (this.moneyDif != 0L && (moneyRef = this.moneySell - this.moneyBuy) > 0L) {
                        Debug.trace((Object[])new Object[]{"L\u1ec7ch ch\u1eb5n l\u1ebb v\u00e0 ch\u01b0a b\u00e1n h\u1ebft ti\u1ec1n l\u1ec7ch => ho\u00e0n tr\u1ea3 : " + moneyRef, this.roomId, this.gameId});
                        GamePot gPot = this.getPotBigger();
                        gPot.refund(Math.abs(moneyRef), this.moneyType);
                        this.setPot(gPot.id, gPot);
                        refundMap = gPot.refundMap;
                        for (Map.Entry<String, RefundModel> entry : refundMap.entrySet()) {
                            GamePlayer gp = this.getPlayer(entry.getKey());
                            RefundModel model = entry.getValue();
                            MoneyResponse mnres = gp.gameMoneyInfo.updateMoney(model.moneyRefund, this.roomId, this.gameId, 0L, false);
                            this.totalReveneu += model.moneyRefund;
                            this.gameLog.append(gPot.id).append("/").append(entry.getKey()).append("/").append(model.moneyRefund).append(";");
                            this.logGameCSV(entry.getKey(), model.moneyRefund, "XDHT<", mnres.getErrorCode());
                            model.currentMoney = mnres.getMoneyInGame();
                            model.pots = String.valueOf(gPot.id);
                            model.moneyRfPots = String.valueOf(model.moneyRefund);
                            refundMap.put(entry.getKey(), model);
                            this.setPlayer(entry.getKey(), gp);
                        }
                    }
                } else {
                    GamePlayer banker = this.getPlayer(this.bankerName);
                    if (banker != null) {
                        long moneyBanker = banker.getMoneyUseInGame();
                        Debug.trace((Object[])new Object[]{"C\u00f3 nh\u00e0 c\u00e1i, bankerName: " + this.bankerName + ", moneyBanker: " + moneyBanker, this.roomId, this.gameId});
                        HashMap<Integer, Long> mPotRef = new HashMap<Integer, Long>();
                        int potBiggerId = this.getPotBiggerId();
                        if (this.moneySell > 0L) {
                            long moneyReject = this.isBankerReject ? this.moneySell - this.moneyBuy : 0L;
                            Debug.trace((Object[])new Object[]{"nh\u00e0 c\u00e1i c\u00f3 b\u00e1n c\u1eeda, moneyReject: " + moneyReject, this.roomId, this.gameId});
                            if (potBiggerId == this.potPurchase) {
                                long moneyDifNew = Math.abs(this.moneyDif) - (this.moneyBuy + moneyReject);
                                Debug.trace((Object[])new Object[]{"nh\u00e0 c\u00e1i b\u00e1n c\u1eeda l\u1edbn h\u01a1n, moneyDifNew: " + moneyDifNew, this.roomId, this.gameId});
                                moneyBanker -= Math.abs(moneyDifNew);
                                if (moneyDifNew > 0L) {
                                    Debug.trace((Object[])new Object[]{"ch\u01b0a b\u00e1n h\u1ebft ch\u1ed7 ti\u1ec1n l\u1ec7ch, moneyBanker: " + moneyBanker, this.roomId, this.gameId});
                                    if (moneyBanker < 0L) {
                                        mPotRef.put(potBiggerId, Math.abs(moneyBanker) + moneyReject);
                                    } else {
                                        mPotRef.put(potBiggerId, moneyReject);
                                    }
                                } else {
                                    Debug.trace((Object[])new Object[]{"b\u00e1n h\u1ebft c\u1ea3 ti\u1ec1n l\u1ec7ch, moneyBanker: " + moneyBanker, this.roomId, this.gameId});
                                    mPotRef.put(potBiggerId, moneyReject);
                                }
                            } else {
                                Debug.trace((Object[])new Object[]{"b\u00e1n c\u1eeda nh\u1ecf h\u01a1n ho\u1eb7c b\u1eb1ng nhau, moneyBanker: " + (moneyBanker -= Math.abs(this.moneyDif) + this.moneyBuy + moneyReject), this.roomId, this.gameId});
                                if (moneyBanker < 0L) {
                                    mPotRef.put(potBiggerId, Math.abs(moneyBanker));
                                }
                                mPotRef.put(this.potPurchase, moneyReject);
                            }
                        } else {
                            Debug.trace((Object[])new Object[]{"nh\u00e0 c\u00e1i c\u00e2n t\u1ea5t, moneyBanker: " + (moneyBanker -= Math.abs(this.moneyDif)), this.roomId, this.gameId});
                            if (moneyBanker < 0L) {
                                mPotRef.put(potBiggerId, Math.abs(moneyBanker));
                            }
                        }
                        HashMap<Integer, Long> moneyRefundList = new HashMap();
                        for (Map.Entry entry : mPotRef.entrySet()) {
                            if ((Long)entry.getValue() <= 0L) continue;
                            moneyRefundList.put((Integer)entry.getKey(), (Long)entry.getValue());
                        }
                        for (int i = 2; i < 6; ++i) {
                            long moneyref;
                            long moneyCanPay = 0L;
                            if (moneyBanker > 0L) {
                                moneyCanPay = (long)((double)moneyBanker / (PotType.findPotType(i).getRatio() - 1.0));
                            }
                            if ((moneyref = this.getPot((int)i).totalMoney - moneyCanPay) <= 0L) continue;
                            moneyRefundList.put(i, moneyref);
                        }
                        for (Map.Entry mr : moneyRefundList.entrySet()) {
                            GamePot gPot = this.getPot((Integer)mr.getKey());
                            gPot.refund((Long)mr.getValue(), this.moneyType);
                            this.setPot(gPot.id, gPot);
                            for (Map.Entry<String, RefundModel> rf : gPot.refundMap.entrySet()) {
                                RefundModel model = rf.getValue();
                                this.gameLog.append(gPot.id).append("/").append(rf.getKey()).append("/").append(model.moneyRefund).append(";");
                                if (refundMap.containsKey(rf.getKey())) {
                                    RefundModel model2 = refundMap.get(rf.getKey());
                                    model.pots = new StringBuilder(model2.pots).append(",").append(gPot.id).toString();
                                    model.moneyRfPots = new StringBuilder(model2.moneyRfPots).append(",").append(model.moneyRefund).toString();
                                    model.moneyRefund += model2.moneyRefund;
                                } else {
                                    model.pots = String.valueOf(gPot.id);
                                    model.moneyRfPots = String.valueOf(model.moneyRefund);
                                }
                                refundMap.put(rf.getKey(), model);
                            }
                        }
                        for (Map.Entry<String, RefundModel> entry : refundMap.entrySet()) {
                            GamePlayer gp = this.getPlayer((String)entry.getKey());
                            RefundModel model = (RefundModel)entry.getValue();
                            MoneyResponse mnres = gp.gameMoneyInfo.updateMoney(model.moneyRefund, this.roomId, this.gameId, 0L, false);
                            this.totalReveneu += model.moneyRefund;
                            this.logGameCSV((String)entry.getKey(), model.moneyRefund, "XDHT<", mnres.getErrorCode());
                            model.currentMoney = mnres.getMoneyInGame();
                            refundMap.put((String)entry.getKey(), model);
                            this.setPlayer((String)entry.getKey(), gp);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: systemBalance() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
        return refundMap;
    }

    private boolean bCheckBalanceBot() {
        return GameUtils.isBot && this.moneyType == 1;
    }

    private XocDiaForceResult checkForceResult() {
        int forceType = -1;
        XocDiaForceResult xdForce = new XocDiaForceResult(forceType, null);
        try {
            if (this.bCheckBalanceBot) {
                GamePlayer banker;
                boolean hasBanker;
                GamePot potPC;
                boolean isBankerIsBot = false;
                boolean bl = hasBanker = !this.bankerName.isEmpty();
                if (hasBanker && (banker = this.getPlayer(this.bankerName)) != null && banker.isBot) {
                    isBankerIsBot = true;
                }
                if (this.moneySell > 0L && this.potPurchase >= 0 && (potPC = this.getPot(this.potPurchase)) != null) {
                    GamePot potRM;
                    int potRMid;
                    long moneyBuyRemain = potPC.getMoneyBuyRemain(isBankerIsBot, this.roomId, this.gameId);
                    this.setPot(this.potPurchase, potPC);
                    if (moneyBuyRemain > 0L && (potRM = this.getPot(potRMid = this.potPurchase == 0 ? 1 : 0)) != null) {
                        potRM.addBetMoney(moneyBuyRemain, isBankerIsBot, this.roomId, this.gameId);
                    }
                }
                ArrayList<XocDiaBetBotModel> potBetList = new ArrayList<XocDiaBetBotModel>();
                long totalUserBet = 0L;
                long totalBotBet = 0L;
                for (GamePot gPot : this.potList) {
                    totalUserBet += gPot.totalMoneyUserBet;
                    totalBotBet += gPot.totalMoneyBotBet;
                    if (isBankerIsBot) {
                        potBetList.add(new XocDiaBetBotModel(gPot.id, gPot.totalMoneyUserBet, gPot.ratio));
                        continue;
                    }
                    potBetList.add(new XocDiaBetBotModel(gPot.id, gPot.totalMoneyBotBet, gPot.ratio));
                }
                boolean bCheckForceTypeForBot = true;
                if (hasBanker) {
                    if (isBankerIsBot) {
                        if (totalUserBet == 0L) {
                            bCheckForceTypeForBot = false;
                        }
                    } else if (totalBotBet == 0L) {
                        bCheckForceTypeForBot = false;
                    }
                } else if (totalBotBet == 0L || totalUserBet == 0L) {
                    bCheckForceTypeForBot = false;
                }
                if (bCheckForceTypeForBot) {
                    xdForce = BotXocDiaManager.instance().getForceResultForBot(this.roomId, this.gameId, isBankerIsBot, hasBanker, potBetList);
                    forceType = xdForce.forceType;
                } else {
                    Debug.trace((Object)(this.roomId + " " + this.gameId + " bCheckForceTypeForBot: " + bCheckForceTypeForBot));
                }
            }
            if (forceType == -1) {
                if (this.cauXD != null) {
                    forceType = this.cauXD.getResult();
                    if (forceType == -1) {
                        this.cauXD = null;
                    } else if (this.moneyType == 1) {
                        Debug.trace((Object)(this.roomId + " " + this.gameId + ", forceByCau: " + forceType));
                    }
                } else {
                    Random rd = new Random();
                    int n = rd.nextInt(XocDiaConfig.numRandomResult);
                    if (n == 0 && XocDiaConfig.listCau.size() > 0) {
                        int cau = rd.nextInt(XocDiaConfig.listCau.size());
                        this.cauXD = new CauXocDia();
                        String data = (String)XocDiaConfig.listCau.get(cau);
                        this.cauXD.setData(data);
                        forceType = this.cauXD.getResult();
                        if (this.moneyType == 1) {
                            Debug.trace((Object)(this.roomId + " " + this.gameId + ", cxd= " + this.cauXD.getData()));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: checkForceResult() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
        xdForce.forceType = forceType;
        if (this.moneyType == 1) {
            Debug.trace((Object)(this.roomId + " " + this.gameId + " checkForceResult: " + xdForce.toJson()));
        }
        return xdForce;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void reward() {
        try {
            Map<String, GamePlayer> map = this.playerList;
            synchronized (map) {
                MoneyResponse mnres;
                XocDiaResult xdResult = new XocDiaResult();
                xdResult.generateResult(this.rsCheat, this.checkForceResult());
                List<Integer> dinces = xdResult.getDinces();
                Iterator<Integer> iterator = dinces.iterator();
                while (iterator.hasNext()) {
                    int dc = iterator.next();
                    this.gameLog.append(dc).append(";");
                }
                this.gameLog.append(">").append("XDTT<");
                List<Byte> potsWin = xdResult.getPotsWin();
                if (this.roomType == 1 && this.rsList.size() >= 100 || this.roomType != 1 && this.rsList.size() >= 32) {
                    this.rsList.remove(0);
                }
                for (Byte bt : potsWin) {
                    GamePot gPot = this.getPot(bt.byteValue());
                    gPot.isWin = true;
                    this.setPot(bt.byteValue(), gPot);
                    if (bt.byteValue() == PotType.EVEN.getId()) {
                        ++this.totalEven;
                        this.rsList.add(bt);
                        continue;
                    }
                    if (bt.byteValue() != PotType.ODD.getId()) continue;
                    ++this.totalOdd;
                    this.rsList.add(bt);
                }
                ResultMsg msg = new ResultMsg();
                msg.dinces = dinces;
                msg.potList = this.potList;
                HashMap<String, RewardModel> rewardMap = new HashMap<String, RewardModel>();
                ArrayList<SubBanker> subList = new ArrayList<SubBanker>();
                HashMap<String, Long> mapVippoint = new HashMap<String, Long>();
                long moneyBankerExchange = 0L;
                GamePlayer banker = null;
                if (!this.bankerName.isEmpty()) {
                    banker = this.getPlayer(this.bankerName);
                }
                for (GamePot gPot : this.potList) {
                    String nickname;
                    SubBanker sb;
                    long moneyBet;
                    if (gPot.isWin) {
                        if (banker != null) {
                            moneyBankerExchange -= Math.round((double)gPot.totalMoney * (gPot.ratio - 1.0));
                        }
                        if (this.potPurchase == gPot.id && this.subBankerList.size() > 0) {
                            if (banker != null) {
                                moneyBankerExchange += this.moneyBuy;
                            }
                            for (Map.Entry<String, Long> entry : this.subBankerList.entrySet()) {
                                sb = new SubBanker((byte)this.potPurchase, entry.getKey(), -entry.getValue().longValue(), -entry.getValue().longValue(), 0L);
                                subList.add(sb);
                            }
                        }
                        for (Map.Entry<String, Long> entry : gPot.betMap.entrySet()) {
                            nickname = entry.getKey();
                            moneyBet = entry.getValue();
                            long moneyWin = Math.round((double)moneyBet * gPot.ratio);
                            long fee = Math.round((double)(moneyWin - moneyBet) * this.feeWin);
                            moneyWin -= fee;
                            RewardModel model = new RewardModel();
                            if (rewardMap.containsKey(nickname)) {
                                model = (RewardModel)rewardMap.get(nickname);
                                model.moneyWin += moneyWin;
                                model.moneyBet += moneyBet;
                                model.fee += fee;
                                model.potsWin = model.potsWin != null ? new StringBuilder(model.potsWin).append(",").append(gPot.id).toString() : String.valueOf(gPot.id);
                                model.moneyWinPots = model.moneyWinPots != null ? new StringBuilder(model.moneyWinPots).append(",").append(moneyWin).toString() : String.valueOf(moneyWin);
                            } else {
                                model.moneyWin = moneyWin;
                                model.moneyBet = moneyBet;
                                model.fee = fee;
                                model.potsWin = String.valueOf(gPot.id);
                                model.moneyWinPots = String.valueOf(moneyWin);
                            }
                            rewardMap.put(nickname, model);
                        }
                        continue;
                    }
                    if (banker != null) {
                        moneyBankerExchange += gPot.totalMoney;
                    }
                    if (this.potPurchase == gPot.id && this.subBankerList.size() > 0) {
                        if (banker != null) {
                            moneyBankerExchange -= this.moneyBuy;
                        }
                        for (Map.Entry<String, Long> entry : this.subBankerList.entrySet()) {
                            sb = new SubBanker((byte)this.potPurchase, entry.getKey(), entry.getValue(), entry.getValue(), 0L);
                            subList.add(sb);
                        }
                    }
                    for (Map.Entry<String, Long> entry : gPot.betMap.entrySet()) {
                        nickname = entry.getKey();
                        moneyBet = entry.getValue();
                        RewardModel model = new RewardModel();
                        if (rewardMap.containsKey(nickname)) {
                            model = (RewardModel)rewardMap.get(nickname);
                            model.moneyBet += moneyBet;
                        } else {
                            model.moneyBet = moneyBet;
                        }
                        rewardMap.put(nickname, model);
                    }
                }
                long totalFeeUser = 0L;
                long totalRevenueUser = 0L;
                long moneyBankerBefore = 0L;
                long moneyBankerAfter = 0L;
                if (banker != null) {
                    moneyBankerAfter = moneyBankerBefore = banker.getMoneyUseInGame();
                }
                if (moneyBankerExchange != 0L) {
                    long fee = 0L;
                    if (!banker.isBoss && moneyBankerExchange > 0L) {
                        fee = Math.round((double)moneyBankerExchange * this.bankerFee);
                        this.totalFee += fee;
                    }
                    MoneyResponse mnres2 = banker.gameMoneyInfo.updateMoneyByFreeze(moneyBankerExchange -= fee, this.roomId, this.gameId, fee);
                    this.totalReveneu += moneyBankerExchange;
                    this.gameLog.append("BK: ").append(this.bankerName).append("/").append(moneyBankerExchange).append("/").append(mnres2.getErrorCode()).append(";");
                    this.logGameCSV(this.bankerName, moneyBankerExchange, "XDTT<", mnres2.getErrorCode());
                    this.setPlayer(this.bankerName, banker);
                    moneyBankerAfter = banker.getMoneyUseInGame();
                    mapVippoint.put(this.bankerName, moneyBankerExchange);
                    if (!banker.isBot) {
                        totalFeeUser += fee;
                        totalRevenueUser += moneyBankerExchange;
                    }
                    if (moneyBankerExchange < 0L && !mnres2.isSuccess()) {
                        String content = "Xoc Dia errot money, tru tien nha cai nhung khong du tien, gameId: " + this.gameId + ", banker: " + this.bankerName + ", moneyBankerExchange: " + moneyBankerExchange + ", moneySub: " + mnres2.getSubtractMoney() + ", error: " + mnres2.getErrorCode();
                        MsgUtils.alertServer(content, false, true);
                    }
                }
                Debug.trace((Object[])new Object[]{"T\u00ednh ti\u1ec1n nh\u00e0 c\u00e1i: " + this.bankerName + " " + moneyBankerExchange, this.roomId, this.gameId});
                ArrayList<SubBanker> subListMsg = new ArrayList<SubBanker>();
                if (subList.size() > 0) {
                    for (SubBanker subBanker : subList) {
                        long fee = 0L;
                        if (subBanker.money > 0L) {
                            fee = Math.round((double)subBanker.money * this.feeWin);
                            this.totalFee += fee;
                        }
                        subBanker.money -= fee;
                        GamePlayer gSubBanker = this.getPlayer(subBanker.nickname);
                        mnres = gSubBanker.gameMoneyInfo.updateMoney(subBanker.money, this.roomId, this.gameId, fee, true);
                        this.totalReveneu += subBanker.money;
                        this.gameLog.append("SBK: ").append(subBanker.nickname).append("/").append(subBanker.money).append("/").append(mnres.getErrorCode()).append(";");
                        this.logGameCSV(subBanker.nickname, subBanker.money, "XDTT<", mnres.getErrorCode());
                        subBanker.currentMoney = mnres.getMoneyInGame();
                        subListMsg.add(subBanker);
                        this.setPlayer(subBanker.nickname, gSubBanker);
                        mapVippoint.put(subBanker.nickname, subBanker.money);
                        Debug.trace((Object[])new Object[]{"T\u00ednh ti\u1ec1n nh\u00e0 c\u00e1i ph\u1ee5: " + subBanker.nickname + " " + subBanker.money, this.roomId, this.gameId});
                        if (!gSubBanker.isBot) {
                            totalFeeUser += fee;
                            totalRevenueUser += subBanker.money;
                        }
                        if (subBanker.money >= 0L || mnres.isSuccess() && Math.abs(subBanker.money) == mnres.getSubtractMoney()) continue;
                        String content = "Xoc Dia errot money, tru tien nha cai phu nhung khong du tien, gameId: " + this.gameId + ", nickname: " + subBanker.nickname + ", money: " + subBanker.money + ", moneySub: " + mnres.getSubtractMoney() + ", error: " + mnres.getErrorCode();
                        MsgUtils.alertServer(content, false, true);
                    }
                }
                ArrayList<String> removeRW = new ArrayList();
                for (Map.Entry entry : rewardMap.entrySet()) {
                    GamePlayer gPlayer = this.getPlayer((String)entry.getKey());
                    RewardModel model = (RewardModel)entry.getValue();
                    if (model.moneyWin > 0L) {
                        mnres = gPlayer.gameMoneyInfo.updateMoney(model.moneyWin, this.roomId, this.gameId, model.fee, false);
                        this.totalFee += model.fee;
                        this.totalReveneu += model.moneyWin;
                        model.currentMoney = mnres.getMoneyInGame();
                        this.gameLog.append((String)entry.getKey()).append("/").append(model.moneyWin).append("/").append(mnres.getErrorCode()).append(";");
                        this.logGameCSV((String)entry.getKey(), model.moneyWin, "XDTT<", mnres.getErrorCode());
                        rewardMap.put((String)entry.getKey(), model);
                        this.setPlayer((String)entry.getKey(), gPlayer);
                        Debug.trace((Object[])new Object[]{"T\u00ednh ti\u1ec1n ng\u01b0\u1eddi ch\u01a1i: " + (String)entry.getKey() + " " + model.moneyWin, this.roomId, this.gameId});
                        if (!gPlayer.isBot) {
                            totalFeeUser += model.fee;
                            totalRevenueUser += model.moneyWin - model.moneyBet;
                        }
                    } else {
                        removeRW.add((String)entry.getKey());
                        if (!gPlayer.isBot) {
                            totalRevenueUser -= model.moneyBet;
                        }
                    }
                    if (mapVippoint.containsKey(entry.getKey())) {
                        mapVippoint.put((String)entry.getKey(), (Long)mapVippoint.get(entry.getKey()) + model.moneyWin - model.moneyBet);
                        continue;
                    }
                    mapVippoint.put((String)entry.getKey(), model.moneyWin - model.moneyBet);
                }
                if (this.moneyType == 1) {
                    BotXocDiaManager.instance().finishGame(this.roomId, this.gameId, totalFeeUser, totalRevenueUser);
                }
                this.gameLog.append("vinplay").append("/").append("Fee: ").append(this.totalFee).append(". Revenueu: ").append(this.totalReveneu).append("/").append("").append(";");
                for (Map.Entry entry : mapVippoint.entrySet()) {
                    this.service.addVippoint((String)entry.getKey(), Math.abs((Long)entry.getValue()), this.sMoneyType);
                    UserScore score = new UserScore();
                    if ((Long)entry.getValue() == 0L) continue;
                    score.money = Math.abs((Long)entry.getValue());
                    if ((Long)entry.getValue() > 0L) {
                        score.winCount = 1;
                        continue;
                    }
                    score.lostCount = 1;
                }
                for (String nickname : removeRW) {
                    rewardMap.remove(nickname);
                }
                msg.moneyBankerBefore = moneyBankerBefore;
                msg.moneyBankerAfter = moneyBankerAfter;
                msg.moneyBankerExchange = moneyBankerExchange;
                msg.rewardMap = rewardMap;
                msg.subListMsg = subListMsg;
                // Jackpot = boss's current money (fund) for display
                if (!this.bankerName.isEmpty()) {
                    GamePlayer bankerPlayer = this.getPlayer(this.bankerName);
                    if (bankerPlayer != null) {
                        msg.jackpot = bankerPlayer.getMoneyUseInGame();
                    }
                }
                MsgUtils.sendToRoom(msg, this.playerList);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: reward() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, true);
            Debug.trace((Object)e);
        }
    }

    private void removePlayerFromRoom(User user) {
        try {
            GamePlayer gp = this.getPlayer(user.getName());
            if (gp != null) {
                GameRoom gameRoom;
                this.playerList.remove(user.getName());
                this.notifyUserLeaveRoom(gp);
                gp.user.removeProperty((Object)"GAME_ROOM");
                gp.user.removeProperty((Object)"GAME_MONEY_INFO");
                gp.pInfo.setIsHold(false);
                if (gp.isBanker) {
                    this.bankerName = "";
                }
                this.restoreMoney(gp.gameMoneyInfo);
                int roomId = BossManager.instance().getRoomIdByBossName(user.getName());
                if (roomId > 0 && (gameRoom = GameRoomManager.instance().getGameRoomById(roomId)) != null) {
                    user.setProperty((Object)"GAME_ROOM", (Object)gameRoom);
                    gp.pInfo.setIsHold(true);
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: removePlayerFromRoom() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private void leaveRoom(User user) {
        try {
            this.removePlayerFromRoom(user);
            this.room.group.leaveRoom(user, this.room);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void restoreMoney(GameMoneyInfo info) {
        try {
            ListGameMoneyInfo.instance().removeGameMoneyInfo(info, this.roomId);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private long getMoneyRegisBankerMin() {
        long res = this.moneyRegisBankerMin * (long)(this.playerList.size() - 1);
        return res;
    }

    private long getMoneyHoldBankerMin() {
        long res = this.moneyHoldBankerMin * (long)(this.playerList.size() - 1);
        if (res == 0L) {
            res = 1L;
        }
        return res;
    }

    private GamePot getPotBigger() {
        if (this.moneyDif != 0L) {
            byte potId = this.moneyDif > 0L ? PotType.EVEN.getId() : PotType.ODD.getId();
            GamePot gPot = this.getPot(potId);
            return gPot;
        }
        return null;
    }

    private int getPotBiggerId() {
        if (this.moneyDif != 0L) {
            if (this.moneyDif > 0L) {
                return PotType.EVEN.getId();
            }
            return PotType.ODD.getId();
        }
        return 100;
    }

    private boolean checkPotEmty() {
        boolean emty = true;
        for (GamePot gPot : this.potList) {
            if (gPot.totalMoney <= 0L) continue;
            emty = false;
            break;
        }
        return emty;
    }

    private synchronized void logBetting(String nickname, int potId, long money, int type) {
        try {
            String key = new StringBuilder(nickname).append(potId).append(type).toString();
            if (key.equals(this.lastBetting)) {
                LogBettingModel model = (LogBettingModel)this.logBettingList.pollLast();
                model.setMoney(model.getMoney() + money);
                this.logBettingList.offer(model);
            } else {
                this.lastBetting = key;
                this.logBettingList.offer(new LogBettingModel(nickname, potId, money, type));
            }
            this.logGameCSV(nickname, money, "XDDC<", new StringBuilder("'").append(type).append(";").append(potId).toString());
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: logBetting() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private synchronized void logGameCSV(String nickname, long money, String action, String description) {
        try {
            MoneyLogger.logGame((String)this.sMoneyType, (String)String.valueOf(this.moneyBet), (String)String.valueOf(this.roomId), (String)String.valueOf(this.gameId), (String)nickname, (String)String.valueOf(money), (String)action, (String)description);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private boolean isBoss(String nickname) {
        return this.roomType == 2 && this.bankerName.equals(nickname);
    }

    private GamePlayer getPlayer(String nickname) {
        return this.playerList.get(nickname);
    }

    private void setPlayer(String key, GamePlayer gp) {
        this.playerList.put(key, gp);
    }

    private GamePot getPot(int potId) {
        return this.potList.get(potId);
    }

    private void setPot(int potId, GamePot gPot) {
        this.potList.set(potId, gPot);
    }

    private int getCountTime() {
        int cnt = 0;
        try {
            switch (this.gameState) {
                case 0: {
                    cnt = 0;
                    break;
                }
                case 1: {
                    cnt = 5 - this.countTime;
                    break;
                }
                case 2: {
                    cnt = 35 - this.countTime;
                    break;
                }
                case 3: {
                    cnt = 45 - this.countTime;
                    break;
                }
                case 4: {
                    cnt = 50 - this.countTime;
                    break;
                }
                case 5: {
                    cnt = 55 - this.countTime;
                    break;
                }
                case 6: {
                    cnt = 70 - this.countTime;
                    break;
                }
            }
            if (cnt < 0) {
                cnt = 0;
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
        return cnt;
    }

    private void notifyUserEnter(GamePlayer gamePlayer) {
        try {
            User user = gamePlayer.user;
            UserJoinRoomMsg msg = new UserJoinRoomMsg(gamePlayer.pInfo);
            msg.money = gamePlayer.getMoneyUseInGame();
            MsgUtils.sendExceptMe(msg, user, this.playerList);
            this.notifyJoinRoom(gamePlayer);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyJoinRoom(GamePlayer gamePlayer) {
        try {
            GamePlayer banker;
            JoinRoomMsg msg = new JoinRoomMsg();
            msg.me = gamePlayer;
            msg.moneyBet = this.moneyBet;
            msg.roomId = this.roomId;
            msg.gameId = this.gameId;
            msg.moneyType = this.moneyType;
            msg.gameState = this.gameState;
            msg.countTime = this.getCountTime();
            msg.playerList = this.playerList;
            msg.potList = this.potList;
            msg.purchaseStatus = this.purchaseStatus;
            msg.potPurchase = this.potPurchase;
            msg.moneyPurchaseEven = this.moneyPurchaseEven;
            msg.moneyPurchaseOdd = this.moneyPurchaseOdd;
            msg.moneyRemain = this.moneySell - this.moneyBuy;
            msg.subBankerList = this.subBankerList;
            boolean bankerReqDestroy = false;
            boolean bossReqDestroy = false;
            if (!this.bankerName.isEmpty() && (banker = this.getPlayer(this.bankerName)) != null) {
                bankerReqDestroy = banker.reqDestroyBanker;
                if (banker.isBoss) {
                    bossReqDestroy = banker.reqDestroyRoom;
                }
            }
            msg.bankerReqDestroy = bankerReqDestroy;
            msg.bossReqDestroy = bossReqDestroy;
            msg.roomType = this.roomType;
            MsgUtils.send(msg, gamePlayer.user, gamePlayer.revMsg);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyUserLeaveRoom(GamePlayer gamePlayer) {
        try {
            UserLeaveRoomMsg msg = new UserLeaveRoomMsg(gamePlayer.user.getName());
            MsgUtils.sendToRoom(msg, this.playerList);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyRegisLeaveRoom(GamePlayer gp) {
        try {
            RegisLeaveRoomMsg msg = new RegisLeaveRoomMsg(gp.user.getName());
            msg.reqLeaveRoom = gp.reqLeaveRoom;
            MsgUtils.sendToRoom(msg, this.playerList);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyOutRoom(GamePlayer gp, byte reason) {
        try {
            LeaveRoomMsg msg = new LeaveRoomMsg();
            msg.reason = reason;
            MsgUtils.send(msg, gp.user, gp.revMsg);
            if (reason == 2 || reason == 1 || reason == 7 || reason == 6) {
                if (reason == 7) {
                    BanUserManager.instance().banUser(this.roomId, gp.user.getName());
                }
                OutRoomMsg outMsg = new OutRoomMsg();
                outMsg.reason = reason;
                MsgUtils.send(outMsg, gp.user, gp.revMsg);
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyActionGamme(byte action, byte time) {
        try {
            ActionGameMsg msg = new ActionGameMsg();
            msg.action = action;
            msg.time = time;
            MsgUtils.sendToRoom(msg, this.playerList);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyRequestBankerFail(GamePlayer gp, byte error) {
        try {
            RequestBankerFailMsg msgFail = new RequestBankerFailMsg();
            msgFail.Error = error;
            MsgUtils.send(msgFail, gp.user, gp.revMsg);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyStartGame() {
        try {
            GamePlayer banker;
            StartGameMsg msg = new StartGameMsg(this.bankerName);
            msg.gameId = this.gameId;
            if (!this.bankerName.isEmpty() && (banker = this.getPlayer(this.bankerName)) != null) {
                msg.moneyBanker = banker.getMoneyUseInGame();
            }
            msg.potList = this.potList;
            MsgUtils.sendToRoom(msg, this.playerList);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyRequestBanker(GamePlayer gp, byte error) {
        try {
            RequestBankerMsg msg = new RequestBankerMsg();
            msg.Error = error;
            MsgUtils.send(msg, gp.user, gp.revMsg);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyDestroyBanker(boolean reqDestroyBanker) {
        try {
            DestroyBankerMsg msg = new DestroyBankerMsg(this.bankerName);
            msg.bDestroy = reqDestroyBanker;
            MsgUtils.sendToRoom(msg, this.playerList);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private void notifyStopGame() {
        try {
            StopGameMsg msg = new StopGameMsg(this.bankerName);
            MsgUtils.sendToRoom(msg, this.playerList);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    public void onNoHu(ThongTinThangLon info) {
    }

    public void choNoHu(String nickName) {
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
            json.put("gameId", this.gameId);
            json.put("gameState", (int)this.gameState);
            json.put("countTime", this.countTime);
            json.put("banker", (Object)this.bankerName);
            json.put("moneyDif", this.moneyDif);
            json.put("enableBetting", this.enableBetting);
            json.put("isBetting", this.isBetting);
            json.put("purchaseStatus", this.purchaseStatus);
            json.put("moneyPurchaseEven", this.moneyPurchaseEven);
            json.put("moneyPurchaseOdd", this.moneyPurchaseOdd);
            json.put("moneySell", this.moneySell);
            json.put("moneyBuy", this.moneyBuy);
            json.put("potPurchase", this.potPurchase);
            json.put("isBankerReject", this.isBankerReject);
            json.put("finishStep", this.finishStep);
            json.put("gameLog", (Object)this.gameLog);
            int i = 1;
            for (Map.Entry<String, GamePlayer> entry : this.playerList.entrySet()) {
                json.put("_u" + i + "_" + entry.getKey(), (Object)entry.getValue().toString());
                ++i;
            }
            for (GamePot gp : this.potList) {
                json.put("_p" + gp.id + "_" + gp.name, (Object)gp.toString());
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }

    public void destroy() {
        try {
            Debug.trace((Object[])new Object[]{"DESTROY GAME", this.roomId, this.gameId});
            this.purchaseStatus = 0;
            this.moneyDif = 0L;
            this.finishStep = false;
            this.enableBetting = false;
            this.gameState = 0;
            this.gameId = 0;
            this.rsList.clear();
            this.totalEven = 0;
            this.totalOdd = 0;
            this.bankerName = "";
            this.subBankerList.clear();
            this.reqBankerList.clear();
            this.gameLog = new StringBuilder("");
            this.cauXD = null;
            this.transInDay = 0;
            this.transInWeek = 0;
            this.cntDate = 0;
            this.destroyLoop();
            BanUserManager.instance().removeBanList(this.roomId);
            if (this.roomType == 2) {
                if (this.taskBoss != null && !this.taskBoss.isCancelled()) {
                    this.taskBoss.cancel(false);
                }
                GameRoomManager.instance().destroyGameRoom(this.roomId);
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: destroy() " + this.roomId + " " + this.gameId;
            MsgUtils.alertServer(content, false, false);
            Debug.trace((Object)e);
        }
    }

    private final class BotTask
    implements Runnable {
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                XocDiaGameServer.this.botLoop();
            }
            catch (Throwable e) {
                Debug.trace((Object)e);
            }
        }
    }

    private final class BossTask
    implements Runnable {
        @Override
        public void run() {
            try {
                XocDiaGameServer.this.bossLoop();
            }
            catch (Throwable e) {
                Debug.trace((Object)e);
            }
        }
    }

    private final class GameLoopTask
    implements Runnable {
        @Override
        public void run() {
            try {
                XocDiaGameServer.this.gameLoop();
            }
            catch (Throwable e) {
                Debug.trace((Object)e);
            }
        }
    }

}

