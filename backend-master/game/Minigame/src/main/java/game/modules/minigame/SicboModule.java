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
 *  com.google.gson.Gson
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.TaiXiuService
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl
 *  com.vinplay.vbee.common.config.VBeePath
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.utils.CommonUtils
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
import com.google.gson.Gson;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.chat.cmd.send.ChatMsg;
import game.modules.chat.entities.ChatEntry;
import game.modules.minigame.cmd.rev.BetSicboCmd;
import game.modules.minigame.cmd.rev.ChangeRoomMinigameCmd;
import game.modules.minigame.cmd.rev.ForceResultTaiXiuCmd;
import game.modules.minigame.cmd.rev.SubcribeMinigameCmd;
import game.modules.minigame.cmd.rev.UnsubscribeMiniGameCmd;
import game.modules.minigame.cmd.send.BroadcastTXTimeMsg;
import game.modules.minigame.cmd.send.sicbo.BetSicboBotMsg;
import game.modules.minigame.cmd.send.sicbo.LichSuPhienSicboMsg;
import game.modules.minigame.cmd.send.sicbo.StartNewGameSicboMsg;
import game.modules.minigame.entities.BotMinigame;
import game.modules.minigame.entities.BotSicbo;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomSicbo;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SICBO — handler 28000.
 *
 * Renamed 2026-04-12 from TaiXiuSicbo* -> Sicbo* so stack traces, log greps,
 * and bug reports unambiguously distinguish Sicbo from TaiXiu (handler 2000).
 * See docs/TAIXIU-SICBO-GAME-ARCHITECTURE.md for the architecture rules,
 * especially the hardcoded-vs-config-driven round timer pitfall that caused
 * the Apr 8-12 instability window.
 */
public class SicboModule
extends BaseClientRequestHandler {
    public Map<String, MGRoom> rooms = new HashMap<String, MGRoom>();
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable serverReadyTask = new ServerReadyTask();
    private final Runnable botChatTask = new ScheduleBotChatTask();
    private final Runnable calculatingTXVinTask = new CalculatingTaiXiuPrize((short)1);
    // SUN-1xxx watchdog: same defense as TaiXiuModule. Independent single-thread
    // scheduler that resurrects gameLoop if it stops ticking for > 90s.
    private volatile long lastGameLoopTickMs = System.currentTimeMillis();
    private volatile java.util.concurrent.ScheduledFuture<?> gameLoopFuture;
    private final java.util.concurrent.ScheduledExecutorService gameLoopWatchdogExec =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "sicbo-gameloop-watchdog");
                t.setDaemon(true);
                return t;
            }
        });
    private final CacheService cacheService = new CacheServiceImpl();
    private int count = 0;
    private boolean serverReady = false;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(8);
    private long referenceTaiXiuId;
    private TaiXiuService txService = new TaiXiuSicboServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private List<ResultTaiXiu> lichSuPhienTX = new ArrayList<ResultTaiXiu>();
    private short result = (short)-1;
    private List<BotSicbo> botsVin = new ArrayList<BotSicbo>();
    private List<String> listChat = new ArrayList<String>();
    private List<String> listChatUsers = new ArrayList<String>();
    private static String CacheCurrentReference = "Tai_xiu_current_reference";
    private final SplittableRandom rand = new SplittableRandom();
    private static SicboModule _instance;
    private Gson gson = new Gson();
    private String topBots;
    public static final String SICBO_FUND_NAME = "SICBO_FUND_VIN";

    public static SicboModule getInstance() {
        return _instance;
    }

    public void init() {
        long[] funds = new long[4];
        try {
            funds = this.mgService.getFunds(SICBO_FUND_NAME);
            Debug.trace((Object[])new Object[]{"TAI_XIU_SICBO_FUNDS: " + CommonUtils.arrayLongToString((long[])funds)});
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Get TAI_XIU pot error ", e.getMessage()});
        }
        long jpfakeT = 5000000L;
        long jpfakeX = 5000000L;
        try {
            jpfakeT = this.cacheService.getValueJP("TaiXiuSicbo_Fund_JP_FakeTai");
            jpfakeX = this.cacheService.getValueJP("TaiXiuSicbo_Fund_JP_FakeXiu");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.rooms.put(MGRoomSicbo.getKeyRoom((short)1), new MGRoomSicbo("TaiXiuSicbo_1", this.referenceTaiXiuId, (short)1, funds[0], 0L, 0L, jpfakeT, jpfakeX));
        this.rooms.put(MGRoomSicbo.getKeyRoom((short)0), new MGRoomSicbo("TaiXiuSicbo_0", this.referenceTaiXiuId, (short)0, funds[0], 0L, 0L, jpfakeT, jpfakeX));
        this.loadData();
        this.loadChatData();
        this.loadChatUsers();
        this.gameLoopFuture = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.botChatTask, 1000, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.serverReadyTask, 10, TimeUnit.SECONDS);
        // SUN-1xxx watchdog: 60s grace, 30s tick, 90s death threshold.
        this.gameLoopWatchdogExec.scheduleAtFixedRate(new GameLoopWatchdog(), 60L, 30L, TimeUnit.SECONDS);
        Debug.trace((Object[])new Object[]{"SERVER SICBO READY TASK RUNNING..."});
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        _instance = this;
    }

    public void loadChatData() {
        try (BufferedReader br2 = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(VBeePath.basePath.concat("config/list_chat.txt")), "UTF8"));){
            String entry;
            while ((entry = br2.readLine()) != null) {
                this.listChat.add(entry);
            }
            Debug.trace((Object[])new Object[]{"SICBO BOT CHAT :" + this.listChat.size()});
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void loadChatUsers() {
        try (BufferedReader br2 = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(VBeePath.basePath.concat("config/bots.txt")), "UTF8"));){
            String entry;
            while ((entry = br2.readLine()) != null) {
                this.listChatUsers.add(entry);
            }
            br2.close();
            Debug.trace((Object[])new Object[]{"SICBO BOT CHAT USERS :" + this.listChatUsers.size()});
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
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
            this.referenceTaiXiuId = this.mgService.getReferenceId(Games.TAI_XIU_SICBO.getId());
            this.lichSuPhienTX = this.txService.getListLichSuPhien(30, 1);
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"SICBO Load reference error ", e.getMessage()});
        }
    }

    private void saveReferences() {
        try {
            this.mgService.saveReferenceId(this.referenceTaiXiuId, 31);
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"SICBO Save reference error " + e.getMessage()});
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        // PR-4/4 — thin dispatch through SicboModuleBridge when
        // MINIGAME_API_BRIDGE_ENABLED=1. Default OFF; the legacy switch
        // below remains the authoritative path until cutover (mirrors
        // TaiXiuModule PR-4/2).
        if (isBridgeEnabled()) {
            try {
                Class<?> dispatchCls = Class.forName(
                    "com.sunwinkr.minigame.api.wire.LegacySicboBridgeDispatcher");
                java.lang.reflect.Method m = dispatchCls.getMethod("dispatch",
                    Object.class, Object.class, Object.class);
                m.invoke(null, this, user, dataCmd);
                return;
            } catch (Throwable t) {
                // Fall through to legacy path on any wiring failure.
                Debug.trace((Object[]) new Object[] {
                    "SICBO bridge dispatch failed (falling back to legacy): "
                    + t.getMessage() });
            }
        }
        switch (dataCmd.getId()) {
            case 28000: {
                this.subcribeMiniGame(user, dataCmd);
                break;
            }
            case 28001: {
                this.unsubscribeMiniGame(user, dataCmd);
                break;
            }
            case 28002: {
                this.changeRoom(user, dataCmd);
                break;
            }
            case 28110: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.betTaiXiu(user, dataCmd);
                break;
            }
            case 28116: {
                this.getLichSuPhienTX(user);
                break;
            }
            case 28003: {
                this.forceResultTaiXiu(user, dataCmd);
            }
        }
    }

    /** Feature flag — flips the dispatcher through {@code SicboModuleBridge}. */
    private static boolean isBridgeEnabled() {
        String v = System.getenv("MINIGAME_API_BRIDGE_ENABLED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
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
            // that MGRoomSicbo.getResult() reads via suaKetQuaTaiXiu()
            java.util.concurrent.ThreadLocalRandom rd = java.util.concurrent.ThreadLocalRandom.current();
            short[] dices;
            int total;
            do {
                dices = new short[]{(short)(rd.nextInt(6)+1), (short)(rd.nextInt(6)+1), (short)(rd.nextInt(6)+1)};
                total = dices[0] + dices[1] + dices[2];
            } while ((total > 10 ? 1 : 0) != resultSide);

            com.hazelcast.core.HazelcastInstance hz = com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
            com.hazelcast.core.IMap<String, short[]> map = hz.getMap("ketquataixiusicbo");
            map.put("ketquataixiusicbo", dices);
            System.out.println("ForceResultSicbo: user=" + user.getName() + " side=" + (resultSide == 1 ? "TAI" : "XIU")
                    + " dice=[" + dices[0] + "," + dices[1] + "," + dices[2] + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void subcribeMiniGame(User user, DataCmd dataCmd) {
        SubcribeMinigameCmd cmd = new SubcribeMinigameCmd(dataCmd);
        this.doSubcribeMiniGame(user, cmd.gameId, cmd.roomId);
        this.getLichSuPhienTX(user);
    }

    private void doSubcribeMiniGame(User user, short gameId, short roomId) {
        switch (gameId) {
            case 2: {
                short moneyType = MGRoomSicbo.getMoneyType(roomId);
                String keyRoom = MGRoomSicbo.getKeyRoom(moneyType);
                MGRoomSicbo roomTX = (MGRoomSicbo)this.getGame(keyRoom);
                if (roomTX != null) {
                    roomTX.joinRoom(user);
                    roomTX.updateTaiXiuInfo(user);
                    break;
                }
                CommonHandle.writeErrLog((String)"Game TAI XIU SicBo not found");
                break;
            }
            default: {
                Debug.trace((Object[])new Object[]{"Game id not found SicBo"});
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
                short moneyType = MGRoomSicbo.getMoneyType(roomId);
                String keyRoom = MGRoomSicbo.getKeyRoom(moneyType);
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
        MGRoomSicbo roomTXVin = this.getRoomTX((short)1);
        MGRoomSicbo roomTXXu = this.getRoomTX((short)0);
        ++this.referenceTaiXiuId;
        roomTXVin.startNewGame(this.referenceTaiXiuId);
        roomTXXu.startNewGame(this.referenceTaiXiuId);
        StartNewGameSicboMsg msg = new StartNewGameSicboMsg();
        msg.referenceId = this.referenceTaiXiuId;
        try {
            msg.jpTai = this.cacheService.getValueJP("TaiXiu_Fund_JP_FakeTai");
            msg.jpXiu = this.cacheService.getValueJP("TaiXiu_Fund_JP_FakeXiu");
        }
        catch (Exception e) {
            msg.jpTai = 0L;
            msg.jpXiu = 0L;
            e.printStackTrace();
        }
        this.sendMessageToTaiXiuNewThread(msg);
        this.saveReferences();
        this.cacheService.setValue("allow_betting_" + this.referenceTaiXiuId, 1);
        this.cacheService.setValue(CacheCurrentReference, Long.toString(this.referenceTaiXiuId));
    }

    private void scheduleBot() {
        try {
            this.botsVin.clear();
            this.botsVin = BotMinigame.getBotSicbo("vin");
            Debug.trace((Object[])new Object[]{"SicBo BOTS VIN: " + this.botsVin.size()});
            List<BotSicbo> botsVip = BotMinigame.getVipBotSicbo();
            this.botsVin.addAll(botsVip);
            Collections.sort(this.botsVin);
            MGRoomSicbo roomVin = this.getRoomTX((short)1);
            this.topBots = this.gson.toJson(this.botsVin.subList(0, Math.min(8, this.botsVin.size())));
            Debug.trace((Object[])new Object[]{"SicBo TX BOTS VIP: " + botsVip.size()});
        }
        catch (Exception e) {
            GameUtils.sendAlert("Bot tai xiu start error: " + e.getMessage() + ", time= " + DateTimeUtils.getCurrentTime());
        }
    }

    private void botBet(int count) {
        try {
            MGRoomSicbo roomVin = this.getRoomTX((short)1);
            for (BotSicbo b : this.botsVin) {
                if (b.getTimeBetting() != 37 - count) continue;
                roomVin.updateInfoBotBet(b.getNickname(), b.getBetValue(), b.getBetSide(), b.getTimeBetting(), (short)1);
                BetSicboBotMsg msg = roomVin.betTaiXiu(b.getNickname(), 0, b.getBetValue(), b.getTimeBetting(), (short)1, b.getBetSide(), true);
                roomVin.sendMessageToRoom(msg);
            }
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"Debug SicBo 15 count: " + count});
        }
    }

    public void betTaiXiu(User user, DataCmd dataCmd) {
        BetSicboCmd cmd = new BetSicboCmd(dataCmd);
        MGRoomSicbo roomTX = this.getRoomTX(cmd.moneyType);
        if (roomTX != null && roomTX.isBetting()) {
            roomTX.betTaiXiu(user, cmd);
        }
    }

    private synchronized void gameLoop() {
        this.lastGameLoopTickMs = System.currentTimeMillis();
        try {
            ++this.count;
            this.botBet(this.count);
            MGRoomSicbo roomTXVin = this.getRoomTX((short)1);
            MGRoomSicbo roomTXXu = this.getRoomTX((short)0);
            // 2026-05-08 (final): 40-sec betting phase. disableBetting fires
            // BEFORE the per-second push so count=40 ships
            // {remainTime=8, bettingState=false} (resultTime=8) — eliminating
            // the 1-tick race that previously displayed a "→20 jump" right
            // after the betting countdown hit 1. Subsequent gameLoop cases
            // shifted by +6 to align with the longer betting window:
            //   40 disableBetting   (was 34)
            //   43 finish           (was 37)
            //   44 generate dices   (was 38)
            //   48 reward           (was 42)
            //   53 schedule bots    (was 47)
            //   55 startNewRound    (unchanged — round total stays 55 sec)
            if (this.count == 40) {
                roomTXVin.disableBetting();
                roomTXXu.disableBetting();
            }
            roomTXVin.updateTaiXiuPerSecond(this.botsVin.size(), this.topBots);
            roomTXXu.updateTaiXiuPerSecond(this.botsVin.size(), this.topBots);
            switch (this.count) {
                case 25: {
                    break;
                }
                case 40: {
                    // disableBetting already fired above; case kept for
                    // symmetry with the rest of the schedule.
                    break;
                }
                case 41: {
                    break;
                }
                case 43: {
                    roomTXVin.finish();
                    break;
                }
                case 44: {
                    this.generateTaiXiuDices(roomTXVin, roomTXXu);
                    break;
                }
                case 48: {
                    roomTXVin.reward();
                    this.txService.updateAllTop();
                    break;
                }
                case 53: {
                    ScheduleBotTask t = new ScheduleBotTask();
                    this.executor.execute(t);
                    break;
                }
                case 55: {
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

    private void generateTaiXiuDices(MGRoomSicbo roomTXVin, MGRoomSicbo roomTXXu) {
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
        Debug.trace((Object[])new Object[]{"SicBo GENERATE RESULT DICES: " + dices[0] + " - " + dices[1] + " - " + dices[2] + "   " + this.result});
        this.lichSuPhienTX.add(resultTX);
        if (this.lichSuPhienTX.size() > 120) {
            this.lichSuPhienTX.remove(0);
        }
    }

    private void getLichSuPhienTX(User user) {
        LichSuPhienSicboMsg msg = new LichSuPhienSicboMsg();
        msg.data = this.gson.toJson(TaiXiuUtils.buildLichSuPhienSicbo(this.lichSuPhienTX, 100));
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
        MGRoomSicbo roomTXVin = this.getRoomTX((short)1);
        roomTXVin.sendMessageToRoom(msg);
    }

    public MGRoom getGame(String key) {
        return this.rooms.get(key);
    }

    public MGRoomSicbo getRoomTX(short moneyType) {
        String keyRoom = MGRoomSicbo.getKeyRoom(moneyType);
        return (MGRoomSicbo)this.getGame(keyRoom);
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
                    MGRoomSicbo roomTXVin = this.getRoomTX((short)1);
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
                SicboModule.this.gameLoop();
            }
            catch (Throwable e) {
                // SUN-1xxx: defensive — see TaiXiuModule.GameLoopTask.run().
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
                long age = now - SicboModule.this.lastGameLoopTickMs;
                java.util.concurrent.ScheduledFuture<?> f = SicboModule.this.gameLoopFuture;
                boolean cancelled = f != null && f.isCancelled();
                boolean done = f != null && f.isDone();
                if (age > 90000L || cancelled || done) {
                    try {
                        Debug.trace((Object[])new Object[]{
                            "!!! Sicbo gameLoop WATCHDOG resurrecting task — age=" + age
                                + "ms cancelled=" + cancelled + " done=" + done});
                    } catch (Throwable ignore) {}
                    try { if (f != null) f.cancel(false); } catch (Throwable ignore) {}
                    SicboModule.this.gameLoopFuture =
                        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(
                            SicboModule.this.gameLoopTask, 1, 1, TimeUnit.SECONDS);
                    SicboModule.this.lastGameLoopTickMs = now;
                }
            } catch (Throwable t) {
                try { Debug.trace((Object[])new Object[]{"Sicbo watchdog error (continuing): " + t.getMessage()}); }
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
            if (!SicboModule.this.serverReady) {
                Debug.trace((Object[])new Object[]{"START MINI GAME - SicBo"});
                SicboModule.this.serverReady = true;
                ScheduleBotTask t = new ScheduleBotTask();
                SicboModule.this.executor.execute(t);
                SicboModule.this.startNewRoundTX();
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
                MGRoomSicbo mGRoomTaiXiuSicbo = SicboModule.this.getRoomTX(this.roomId);
            }
            catch (Throwable e) {
                Debug.trace((Object[])new Object[]{"SicBo Calculate TX " + this.roomId + ", phien= " + SicboModule.this.referenceTaiXiuId + " error: " + e.getMessage()});
            }
            long endTime = System.currentTimeMillis();
            Debug.trace((Object[])new Object[]{"SicBo CALCUALTE PRIZE, time handle= " + (endTime - startTime) + " (ms)"});
            SicboModule.this.txService.updateAllTop();
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
                SicboModule.this.sendMessageToAllUsers(this.msg);
            } else {
                SicboModule.this.sendMessageToTaiXiu(this.msg);
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
                Debug.trace((Object[])new Object[]{"SicBo Schedule bot running ..."});
                SicboModule.this.scheduleBot();
                Debug.trace((Object[])new Object[]{"SicBo Schedule bot finished ..."});
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
                Debug.trace((Object[])new Object[]{"Schedule SicBo bot chat running ..."});
                SicboModule.this.scheduleBotChat();
                Debug.trace((Object[])new Object[]{"Schedule SicBo bot chat finished ..."});
            }
            catch (Exception ex) {
                Debug.trace((Object[])new Object[]{ex.getMessage()});
            }
        }
    }
}

