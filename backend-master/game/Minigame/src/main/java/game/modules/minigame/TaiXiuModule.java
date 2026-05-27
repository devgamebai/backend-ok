/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.TaiXiuService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuServiceImpl
 *  com.vinplay.vbee.common.config.VBeePath
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game.modules.minigame;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuServiceImpl;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.chat.cmd.send.ChatMsg;
import game.modules.chat.entities.ChatEntry;
import game.modules.minigame.cmd.rev.BetTaiXiuCmd;
import game.modules.minigame.cmd.rev.ChangeRoomMinigameCmd;
import game.modules.minigame.cmd.rev.ForceResultTaiXiuCmd;
import game.modules.minigame.cmd.rev.SubcribeMinigameCmd;
import game.modules.minigame.cmd.rev.UnsubscribeMiniGameCmd;
import game.modules.minigame.cmd.send.BroadcastTXTimeMsg;
import game.modules.minigame.cmd.send.LichSuPhienMsg;
import game.modules.minigame.cmd.send.StartNewGameTaiXiuMsg;
import game.modules.minigame.entities.BotMinigame;
import game.modules.minigame.entities.BotTaiXiu;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomTaiXiu;
import game.modules.minigame.utils.TaiXiuUtils;
import game.utils.GameUtil;
import game.utils.GameUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaiXiuModule
extends BaseClientRequestHandler {
    public Map<String, MGRoom> rooms = new HashMap<String, MGRoom>();
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable serverReadyTask = new ServerReadyTask();
    private final Runnable botChatTask = new ScheduleBotChatTask();
    private final Runnable calculatingTXVinTask = new CalculatingTaiXiuPrize((short)1);
    // SUN-1xxx watchdog: detect & resurrect a dead gameLoop. Lives on its own
    // single-thread scheduler so it can't be starved by the same RMQ/MySQL
    // hang that wedged the main game scheduler on 2026-05-11 ~10:42 KST.
    private volatile long lastGameLoopTickMs = System.currentTimeMillis();
    private volatile java.util.concurrent.ScheduledFuture<?> gameLoopFuture;
    private final java.util.concurrent.ScheduledExecutorService gameLoopWatchdogExec =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "taixiu-gameloop-watchdog");
                t.setDaemon(true);
                return t;
            }
        });
    private final CacheService cacheService = new CacheServiceImpl();
    private int count = 0;
    private boolean serverReady = false;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(8);
    private long referenceTaiXiuId;
    private TaiXiuService txService = new TaiXiuServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private List<ResultTaiXiu> lichSuPhienTX = new ArrayList<ResultTaiXiu>();
    private short result = (short)-1;
    private List<BotTaiXiu> botsVin = new ArrayList<BotTaiXiu>();
    private List<String> listChat = new ArrayList<String>();
    private List<String> listChatUsers = new ArrayList<String>();
    private static String CacheCurrentReference = "Tai_xiu_current_reference";
    private final SplittableRandom rand = new SplittableRandom();
    private static TaiXiuModule _instance;

    public static TaiXiuModule getInstance() {
        return _instance;
    }

    public void init() {
        long[] funds = new long[4];
        try {
            funds = this.mgService.getFunds(Games.TAI_XIU.getName());
            Debug.trace((Object[])new Object[]{"TAI_XIU_FUNDS: " + funds});
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Get TAI_XIU pot error ", e.getMessage()});
        }
        long jpfakeT = 5000000L;
        long jpfakeX = 5000000L;
        try {
            jpfakeT = this.cacheService.getValueJP("TaiXiu_Fund_JP_FakeTai");
            jpfakeX = this.cacheService.getValueJP("TaiXiu_Fund_JP_FakeXiu");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.rooms.put(MGRoomTaiXiu.getKeyRoom((short)1), new MGRoomTaiXiu("TaiXiu_1", this.referenceTaiXiuId, (short)1, funds[1], funds[2], funds[3], jpfakeT, jpfakeX));
        this.rooms.put(MGRoomTaiXiu.getKeyRoom((short)0), new MGRoomTaiXiu("TaiXiu_0", this.referenceTaiXiuId, (short)0, funds[0], funds[2], funds[3], jpfakeT, jpfakeX));
        this.loadData();
        this.loadChatData();
        this.loadChatUsers();
        this.gameLoopFuture = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.botChatTask, 1000, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.serverReadyTask, 10, TimeUnit.SECONDS);
        // SUN-1xxx: start watchdog after 60s grace, then check every 30s.
        // Threshold 90s — gameLoop ticks every 1s, so 90s of silence = clearly dead.
        this.gameLoopWatchdogExec.scheduleAtFixedRate(new GameLoopWatchdog(), 60L, 30L, TimeUnit.SECONDS);
        Debug.trace((Object[])new Object[]{"SERVER READY TASK RUNNING..."});
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        _instance = this;
    }

    public void loadChatData() {
        try (BufferedReader br2 = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(VBeePath.basePath.concat("config/list_chat.txt")), "UTF8"));){
            String entry;
            while ((entry = br2.readLine()) != null) {
                this.listChat.add(entry);
            }
            Debug.trace((Object[])new Object[]{"BOT CHAT :" + this.listChat.size()});
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void loadChatUsers() {
        try (BufferedReader br2 = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(VBeePath.basePath.concat("config/bots.txt")), "UTF8"));){
            String entry;
            while ((entry = br2.readLine()) != null) {
                this.listChatUsers.add(entry);
            }
            br2.close();
            Debug.trace((Object[])new Object[]{"BOT CHAT USERS :" + this.listChatUsers.size()});
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        MGRoom room = (MGRoom)user.getProperty("MGROOM_TAI_XIU_INFO");
        if (room != null) {
            room.quitRoom(user);
        }
    }

    private void loadData() {
        this.referenceTaiXiuId = 1L;
        try {
            this.referenceTaiXiuId = this.mgService.getReferenceId(2);
            this.lichSuPhienTX = this.txService.getListLichSuPhien(120, 1);
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Load reference error ", e.getMessage()});
        }
    }

    private void saveReferences() {
        try {
            this.mgService.saveReferenceId(this.referenceTaiXiuId, 2);
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Save reference error " + e.getMessage()});
        }
    }

    /** Feature flag — flips the dispatcher through {@code TaiXiuModuleBridge}. */
    private static boolean isBridgeEnabled() {
        String v = System.getenv("MINIGAME_ENGINE_ENABLED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        // SUN-13xx PR-4: when MINIGAME_ENGINE_ENABLED=1, dispatch to the
        // Spring-based TaiXiuModuleBridge instead of the legacy room
        // path. The bridge translates engine results back into the same
        // BitZero BaseMsg wire shapes so Cocos clients see no diff.
        //
        // Default OFF — legacy code runs until cutover gate clears.
        // See docs/plans/taixiu-extraction-plan.md §6.
        if (isBridgeEnabled()) {
            try {
                Class<?> dispatchCls = Class.forName(
                    "com.sunwinkr.minigame.api.wire.LegacyBridgeDispatcher");
                java.lang.reflect.Method m = dispatchCls.getMethod("dispatch",
                    Object.class, Object.class, Object.class);
                m.invoke(null, this, user, dataCmd);
                return;
            } catch (Throwable t) {
                // Any bridge failure → fall through to legacy. The bridge
                // is allowed to fail closed without breaking the room.
                Debug.trace((Object[])new Object[]{
                    "TaiXiu bridge dispatch failed, falling back to legacy: " + t.getMessage()});
            }
        }
        switch (dataCmd.getId()) {
            case 2000: {
                this.subcribeMiniGame(user, dataCmd);
                break;
            }
            case 2001: {
                this.unsubscribeMiniGame(user, dataCmd);
                break;
            }
            case 2002: {
                this.changeRoom(user, dataCmd);
                break;
            }
            case 2110: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.betTaiXiu(user, dataCmd);
                break;
            }
            case 2116: {
                this.getLichSuPhienTX(user);
            }
            case 2003: {
                this.forceResultTaiXiu(user, dataCmd);
            }
        }
    }

    private void forceResultTaiXiu(User user, DataCmd dataCmd) {
        if (!user.getName().contains("superadmin")) {
            return;
        }
        ForceResultTaiXiuCmd cmd = new ForceResultTaiXiuCmd(dataCmd);
        int resultSide = cmd.betSide;
        if (resultSide != 0 && resultSide != 1) {
            return;
        }
        try {
            // Generate dice matching forced side and write to Hazelcast map
            // that MGRoomTaiXiu.getResult() actually reads via suaKetQuaTaiXiu()
            java.util.concurrent.ThreadLocalRandom rd = java.util.concurrent.ThreadLocalRandom.current();
            short[] dices;
            int total;
            do {
                dices = new short[]{(short)(rd.nextInt(6)+1), (short)(rd.nextInt(6)+1), (short)(rd.nextInt(6)+1)};
                total = dices[0] + dices[1] + dices[2];
            } while ((total > 10 ? 1 : 0) != resultSide);

            com.hazelcast.core.HazelcastInstance hz = com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
            com.hazelcast.core.IMap<String, short[]> map = hz.getMap("ketquataixiu");
            map.put("ketquataixiu", dices);
            System.out.println("ForceResultTaiXiu: user=" + user.getName() + " side=" + (resultSide == 1 ? "TAI" : "XIU")
                    + " dice=[" + dices[0] + "," + dices[1] + "," + dices[2] + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void subcribeMiniGame(User user, DataCmd dataCmd) {
        SubcribeMinigameCmd cmd = new SubcribeMinigameCmd(dataCmd);
        this.doSubcribeMiniGame(user, cmd.gameId, cmd.roomId);
        LichSuPhienMsg msgLSGD = new LichSuPhienMsg();
        msgLSGD.data = TaiXiuUtils.buildLichSuPhien(this.lichSuPhienTX, 100);
        this.send(msgLSGD, user);
    }

    private void doSubcribeMiniGame(User user, short gameId, short roomId) {
        switch (gameId) {
            case 2: {
                short moneyType = MGRoomTaiXiu.getMoneyType(roomId);
                String keyRoom = MGRoomTaiXiu.getKeyRoom(moneyType);
                MGRoomTaiXiu roomTX = (MGRoomTaiXiu)this.getGame(keyRoom);
                if (roomTX != null) {
                    roomTX.joinRoom(user);
                    roomTX.updateTaiXiuInfo(user);
                    break;
                }
                CommonHandle.writeErrLog((String)"Game TAI XIU not found");
                break;
            }
            default: {
                Debug.trace((Object[])new Object[]{"Game id not found"});
            }
        }
    }

    private void unsubscribeMiniGame(User user, DataCmd dataCmd) {
        UnsubscribeMiniGameCmd cmd = new UnsubscribeMiniGameCmd(dataCmd);
        this.doUnsubscribeMiniGame(user, cmd.gameId, cmd.roomId);
    }

    private void doUnsubscribeMiniGame(User user, short gameId, short roomId) {
        switch (gameId) {
            case 2: {
                short moneyType = MGRoomTaiXiu.getMoneyType(roomId);
                String keyRoom = MGRoomTaiXiu.getKeyRoom(moneyType);
                MGRoom room = this.getGame(keyRoom);
                if (room == null) break;
                room.quitRoom(user);
            }
        }
    }

    private void changeRoom(User user, DataCmd dataCmd) {
        ChangeRoomMinigameCmd cmd = new ChangeRoomMinigameCmd(dataCmd);
        this.doUnsubscribeMiniGame(user, cmd.gameId, cmd.lastRoomId);
        this.doSubcribeMiniGame(user, cmd.gameId, cmd.newRoomId);
    }

    private void startNewRoundTX() {
        MGRoomTaiXiu roomTXVin = this.getRoomTX((short)1);
        MGRoomTaiXiu roomTXXu = this.getRoomTX((short)0);
        ++this.referenceTaiXiuId;
        roomTXVin.startNewGame(this.referenceTaiXiuId);
        roomTXXu.startNewGame(this.referenceTaiXiuId);
        StartNewGameTaiXiuMsg msg = new StartNewGameTaiXiuMsg();
        msg.referenceId = this.referenceTaiXiuId;
        msg.jpTai = roomTXVin.getJackpotAccumulate();
        msg.jpXiu = roomTXVin.getJackpotAccumulate();
        this.sendMessageToTaiXiuNewThread(msg);
        this.saveReferences();
        // SUN-1xxx (2026-05-11): isolate the Hazelcast cache writes so a
        // transient HZ disconnect can't kill the scheduled GameLoopTask.
        // The 12-minute freeze on 2026-05-10 was caused by setValue throwing
        // "Connection reset by peer" during a HZ rolling restart, which
        // escaped to ScheduledThreadPoolExecutor and silently cancelled the
        // task. CacheServiceImpl now retries once internally; if both attempts
        // fail we log + continue. The round is already started in memory and
        // saveReferences() persisted the refid, so the cache flags are
        // derived state. Worst case: brief betting-disabled window until HZ
        // recovers — next tick can reassert if needed. Outer gameLoop catch
        // then sees a clean return and advances count → no freeze.
        try {
            this.cacheService.setValue("allow_betting_" + this.referenceTaiXiuId, 1);
        } catch (Exception cacheErr) {
            Debug.trace((Object[])new Object[]{"TaiXiu cache.setValue allow_betting FAILED (continuing) ref="
                    + this.referenceTaiXiuId + " err=" + cacheErr.getMessage()});
        }
        try {
            this.cacheService.setValue(CacheCurrentReference, Long.toString(this.referenceTaiXiuId));
        } catch (Exception cacheErr) {
            Debug.trace((Object[])new Object[]{"TaiXiu cache.setValue CacheCurrentReference FAILED (continuing) ref="
                    + this.referenceTaiXiuId + " err=" + cacheErr.getMessage()});
        }
    }

    private void scheduleBot() {
        try {
            this.botsVin.clear();
            this.botsVin = BotMinigame.getBotTaiXiu("vin");
            Debug.trace((Object[])new Object[]{"BOTS VIN: " + this.botsVin.size()});
            List<BotTaiXiu> botsVip = BotMinigame.getVipBotTaiXiu();
            this.botsVin.addAll(botsVip);
            Debug.trace((Object[])new Object[]{"TX BOTS VIP: " + botsVip.size()});
        }
        catch (Exception e) {
            GameUtils.sendAlert("Bot tai xiu start error: " + e.getMessage() + ", time= " + DateTimeUtils.getCurrentTime());
        }
    }

    private void botBet(int count) {
        try {
            MGRoomTaiXiu roomVin = this.getRoomTX((short)1);
            if (roomVin == null) {
                return;
            }
            int remainTime = 60 - count;
            short jackpotSide = 0;
            if (remainTime <= 15) {
                jackpotSide = roomVin.getJackPotApi();
            }
            int botBetSide = 0;
            for (BotTaiXiu b : this.botsVin) {
                if (remainTime <= 15 && jackpotSide > 0 && (jackpotSide != 6 || botBetSide != 1 ? jackpotSide == 1 && botBetSide == 0 && roomVin.getNumBetXiu() % 5 == 0 : roomVin.getNumBetTai() % 5 == 0)) continue;
                if (b.getTimeBetting() != remainTime) continue;
                roomVin.betTaiXiu(b.getNickname(), 0, b.getBetValue(), b.getTimeBetting(), (short)1, (short)botBetSide, true);
                botBetSide = 1 - botBetSide;
            }
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"Debug taixiu 15 count: " + count});
        }
    }

    public void betTaiXiu(User user, DataCmd dataCmd) {
        BetTaiXiuCmd cmd = new BetTaiXiuCmd(dataCmd);
        MGRoomTaiXiu roomTX = this.getRoomTX(cmd.moneyType);
        if (roomTX != null) {
            roomTX.betTaiXiu(user, cmd);
        }
    }

    private synchronized void gameLoop() {
        try {
            this.lastGameLoopTickMs = System.currentTimeMillis();
            ++this.count;
            this.botBet(this.count);
            MGRoomTaiXiu roomTXVin = this.getRoomTX((short)1);
            roomTXVin.updateTaiXiuPerSecond();
            MGRoomTaiXiu roomTXXu = this.getRoomTX((short)0);
            roomTXXu.updateTaiXiuPerSecond();
            this.sendTXTime(roomTXVin.getRemainTime(), roomTXVin.isBetting());
            switch (this.count) {
                case 38: {
                    break;
                }
                case 45: {
                    roomTXVin.disableBetting();
                    roomTXXu.disableBetting();
                    break;
                }
                case 48: {
                    roomTXVin.calculateMoneyReturn();
                    break;
                }
                case 50: {
                    roomTXVin.finish();
                    break;
                }
                case 51: {
                    this.generateTaiXiuDices(roomTXVin, roomTXXu);
                    roomTXVin.calculateJackpot();
                    break;
                }
                case 55: {
                    BitZeroServer.getInstance().getTaskScheduler().schedule(this.calculatingTXVinTask, 1, TimeUnit.SECONDS);
                    break;
                }
                case 60: {
                    ScheduleBotTask t = new ScheduleBotTask();
                    this.executor.execute(t);
                    break;
                }
                case 68: {
                    roomTXVin.getBalanceTX().startNewRound();
                    this.startNewRoundTX();
                    this.count = 0;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Debug.trace((Object[])new Object[]{"Exception: " + e.getMessage(), e});
        }
    }

    private void generateTaiXiuDices(MGRoomTaiXiu roomTXVin, MGRoomTaiXiu roomTXXu) {
        short[] dices = roomTXVin.getResult(this.referenceTaiXiuId);
        int total = dices[0] + dices[1] + dices[2];
        this.result = total > 10 ? (short)1 : 0;
        roomTXXu.updateResultDices(dices, this.result);
        ResultTaiXiu resultTX = new ResultTaiXiu();
        resultTX.referenceId = this.referenceTaiXiuId;
        resultTX.result = this.result;
        resultTX.dice1 = dices[0];
        resultTX.dice2 = dices[1];
        resultTX.dice3 = dices[2];
        Debug.trace((Object[])new Object[]{"GENERATE RESULT DICES: " + dices[0] + " - " + dices[1] + " - " + dices[2] + "   " + this.result});
        this.lichSuPhienTX.add(resultTX);
        if (this.lichSuPhienTX.size() > 120) {
            this.lichSuPhienTX.remove(0);
        }
    }

    private void getLichSuPhienTX(User user) {
        LichSuPhienMsg msg = new LichSuPhienMsg();
        msg.data = TaiXiuUtils.buildLichSuPhien(this.lichSuPhienTX, 100);
        Debug.trace((Object[])new Object[]{"LSDG: " + TaiXiuUtils.logLichSuPhien(this.lichSuPhienTX, 100)});
        this.send(msg, user);
    }

    private void sendMessageToTaiXiuNewThread(BaseMsg msg) {
        SendMessageToTXThread t = new SendMessageToTXThread(false, msg);
        this.executor.execute(t);
    }

    private void sendTXTime(short remainTime, boolean betting) {
        BroadcastTXTimeMsg msg = new BroadcastTXTimeMsg();
        msg.remainTime = (byte)remainTime;
        msg.betting = betting;
        SendMessageToTXThread t = new SendMessageToTXThread(true, msg);
        this.executor.execute(t);
    }

    private void sendMessageToAllUsers(BaseMsg msg) {
        List users = ExtensionUtility.globalUserManager.getAllUsers();
        if (users != null) {
            this.send(msg, users);
        }
    }

    private void sendMessageToTaiXiu(BaseMsg msg) {
        MGRoomTaiXiu roomTXVin = this.getRoomTX((short)1);
        roomTXVin.sendMessageToRoom(msg);
    }

    public MGRoom getGame(String key) {
        return this.rooms.get(key);
    }

    public MGRoomTaiXiu getRoomTX(short moneyType) {
        String keyRoom = MGRoomTaiXiu.getKeyRoom(moneyType);
        return (MGRoomTaiXiu)this.getGame(keyRoom);
    }

    private void scheduleBotChat() {
        try {
            while (true) {
                if (this.listChatUsers.size() > 0) {
                    String randMessage;
                    String user;
                    Thread.sleep(2000000L);
                    long ltime = System.currentTimeMillis();
                    if ((ltime - GameUtil.lastTimeTx.get()) / 10000L < 60L) {
                        return;
                    }
                    MGRoomTaiXiu roomTXVin = this.getRoomTX((short)1);
                    ChatMsg msg = new ChatMsg();
                    msg.nickname = user = this.listChatUsers.get(this.rand.nextInt(0, 0));
                    msg.mesasge = randMessage = this.listChat.get(this.rand.nextInt(this.listChat.size()));
                    roomTXVin.sendMessageToRoom(msg);
                    ChatEntry chatEntry = new ChatEntry(user, randMessage);
                    continue;
                }
                Thread.sleep(1000000L);
            }
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e.getMessage()});
            return;
        }
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                TaiXiuModule.this.gameLoop();
            }
            catch (Throwable e) {
                // SUN-1xxx: defensive — printStackTrace can throw on a broken
                // stdout (full disk / broken pipe). If it does, the Throwable
                // escapes run(), and scheduleAtFixedRate cancels this task
                // permanently. Suppress so the scheduled future stays alive.
                try { e.printStackTrace(); } catch (Throwable ignore) {}
            }
        }
    }

    private final class GameLoopWatchdog
    implements Runnable {
        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();
                long age = now - TaiXiuModule.this.lastGameLoopTickMs;
                java.util.concurrent.ScheduledFuture<?> f = TaiXiuModule.this.gameLoopFuture;
                boolean cancelled = f != null && f.isCancelled();
                boolean done = f != null && f.isDone();
                if (age > 90000L || cancelled || done) {
                    try {
                        Debug.trace((Object[])new Object[]{
                            "!!! TaiXiu gameLoop WATCHDOG resurrecting task — age=" + age
                                + "ms cancelled=" + cancelled + " done=" + done
                                + " (the prior task is presumed dead/wedged)"});
                    } catch (Throwable ignore) {}
                    try { if (f != null) f.cancel(false); } catch (Throwable ignore) {}
                    TaiXiuModule.this.gameLoopFuture =
                        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(
                            TaiXiuModule.this.gameLoopTask, 1, 1, TimeUnit.SECONDS);
                    TaiXiuModule.this.lastGameLoopTickMs = now;
                }
            } catch (Throwable t) {
                // Never let the watchdog itself die.
                try { Debug.trace((Object[])new Object[]{"TaiXiu watchdog error (continuing): " + t.getMessage()}); }
                catch (Throwable ignore) {}
            }
        }
    }

    private final class ServerReadyTask
    implements Runnable {
        private ServerReadyTask() {
        }

        @Override
        public void run() {
            if (!TaiXiuModule.this.serverReady) {
                Debug.trace((Object[])new Object[]{"START MINI GAME"});
                TaiXiuModule.this.serverReady = true;
                ScheduleBotTask t = new ScheduleBotTask();
                TaiXiuModule.this.executor.execute(t);
                TaiXiuModule.this.startNewRoundTX();
            }
        }
    }

    private final class CalculatingTaiXiuPrize
    implements Runnable {
        private short roomId;

        public CalculatingTaiXiuPrize(short roomId) {
            this.roomId = roomId;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                MGRoomTaiXiu room = TaiXiuModule.this.getRoomTX(this.roomId);
                room.calculatePrize(TaiXiuModule.this.referenceTaiXiuId);
            }
            catch (Throwable e) {
                Debug.trace((Object[])new Object[]{"Calculate TX " + this.roomId + ", phien= " + TaiXiuModule.this.referenceTaiXiuId + " error: " + e.getMessage()});
            }
            long endTime = System.currentTimeMillis();
            Debug.trace((Object[])new Object[]{"CALCUALTE PRIZE, time handle= " + (endTime - startTime) + " (ms)"});
            TaiXiuModule.this.txService.updateAllTop();
        }
    }

    private final class SendMessageToTXThread
    extends Thread {
        private BaseMsg msg;
        private boolean all;

        private SendMessageToTXThread(boolean all, BaseMsg msg) {
            this.msg = msg;
            this.all = all;
        }

        @Override
        public void run() {
            if (this.all) {
                TaiXiuModule.this.sendMessageToAllUsers(this.msg);
            } else {
                TaiXiuModule.this.sendMessageToTaiXiu(this.msg);
            }
        }
    }

    private final class ScheduleBotTask
    extends Thread {
        private ScheduleBotTask() {
        }

        @Override
        public void run() {
            try {
                Debug.trace((Object[])new Object[]{"Schedule bot running ..."});
                TaiXiuModule.this.scheduleBot();
                Debug.trace((Object[])new Object[]{"Schedule bot finished ..."});
            }
            catch (Exception ex) {
                Debug.trace((Object[])new Object[]{ex.getMessage()});
            }
        }
    }

    private final class ScheduleBotChatTask
    extends Thread {
        private ScheduleBotChatTask() {
        }

        @Override
        public void run() {
            try {
                Debug.trace((Object[])new Object[]{"Schedule bot chat running ..."});
                TaiXiuModule.this.scheduleBotChat();
                Debug.trace((Object[])new Object[]{"Schedule bot chat finished ..."});
            }
            catch (Exception ex) {
                Debug.trace((Object[])new Object[]{ex.getMessage()});
            }
        }
    }
}

