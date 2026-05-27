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
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuMD5ServiceImpl
 *  com.vinplay.usercore.dao.impl.GameConfigDaoImpl
 *  com.vinplay.utils.TelegramUtil
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.apache.commons.lang.RandomStringUtils
 *  org.json.JSONException
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
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
import bitzero.server.util.TaskScheduler;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuMD5ServiceImpl;
import com.vinplay.usercore.dao.impl.GameConfigDaoImpl;
import com.vinplay.utils.TelegramUtil;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.TaiXiu.TaiXiuUtil;
import game.modules.chat.ChatMD5Module;
import game.modules.minigame.cmd.rev.BetTaiXiuCmd;
import game.modules.minigame.cmd.rev.ChangeRoomMinigameCmd;
import game.modules.minigame.cmd.rev.SubcribeMinigameCmd;
import game.modules.minigame.cmd.rev.UnsubscribeMiniGameCmd;
import game.modules.minigame.cmd.send.txmini_md5.LichSuPhienMsg;
import game.modules.minigame.cmd.send.txmini_md5.StartNewGameTaiXiuMsg;
import game.modules.minigame.entities.BotMinigame;
import game.modules.minigame.entities.BotTaiXiu;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.room.MGRoomTaiXiuMD5;
import game.modules.minigame.utils.GenerationTaiXiu;
import game.modules.minigame.utils.TaiXiuUtils;
import game.utils.GameUtils;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaiXiuMD5Module
extends BaseClientRequestHandler {
    int win = 0;
    int lose = 0;
    int sMD5 = 0;
    int ssMD5 = 0;
    int rswl = 0;
    String md5;
    Timer timer = new Timer();
    private Map<String, MGRoom> rooms = new HashMap<String, MGRoom>();
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable serverReadyTask = new ServerReadyTask();
    private final Runnable calculatingTXVinTask = new CalculatingTaiXiuPrize((short)1);
    private int count = 0;
    private boolean serverReady = false;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(8);
    private long referenceTaiXiuId;
    private TaiXiuMD5ServiceImpl txService = new TaiXiuMD5ServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private List<ResultTaiXiu> lichSuPhienTX = new ArrayList<ResultTaiXiu>();
    private GenerationTaiXiu generationTX = new GenerationTaiXiu();
    private short result = (short)-1;
    private long fundRutLoc = 0L;
    private int countRutLoc = 0;
    private int countReqRutLoc = 0;
    private int[] rutLocPrizes;
    private int[] phanBoGiaiThuong;
    private boolean enableRutLoc = false;
    private int tongSoNguoiRutLocLanTruoc = 30;
    private List<BotTaiXiu> botsVin = new ArrayList<BotTaiXiu>();
    private short forceBetSide = (short)-1;
    private String session_md5_string = "";
    private String session_before_md5_string = "";
    private short[] currDices = new short[]{1, 2, 3};
    public final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);
    private static AtomicInteger winCount = new AtomicInteger(0);
    private long MinCtrl = 100000L;
    private ChatMD5Module chatModule;
    int a;
    int b;
    int c;
    int d;
    int e;

    public TaiXiuMD5Module() {
        try {
            GameConfigDaoImpl dao = new GameConfigDaoImpl();
            String commons = dao.getGameCommon("taixiu");
            try {
                JSONObject commonObj = new JSONObject(commons);
                this.MinCtrl = commonObj.getLong("min_ctrl");
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        this.chatModule = new ChatMD5Module();
        this.a = 0;
        this.b = 0;
        this.c = 0;
        this.d = 0;
        this.e = 0;
    }

    public void init() {
        this.rooms.put(MGRoomTaiXiuMD5.getKeyRoom((short)1), new MGRoomTaiXiuMD5("TaiXiuMD5_1", this.referenceTaiXiuId, (short)1));
        this.loadData();
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().schedule(this.serverReadyTask, 10, TimeUnit.SECONDS);
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
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
        this.session_md5_string = "";
        this.session_before_md5_string = "";
        this.currDices = new short[]{1, 2, 3};
        try {
            this.referenceTaiXiuId = this.mgService.getReferenceId(4);
            this.logger.debug("referenceTaiXiuId md5: " + this.referenceTaiXiuId);
            this.lichSuPhienTX = this.txService.getListLichSuPhien(120, 1);
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Load reference error ", e.getMessage()});
            this.logger.error("error:", (Throwable)e);
        }
        try {
            this.generationTX.readConfig();
        }
        catch (IOException e) {
            Debug.trace((Object[])new Object[]{"Load cau tai xiu MD5 error ", e.getMessage()});
        }
        Debug.trace((Object[])new Object[]{"Phien TX MD5: " + this.referenceTaiXiuId});
        Debug.trace((Object[])new Object[]{"SIZE LSDG MD5: " + this.lichSuPhienTX.size()});
        Debug.trace((Object[])new Object[]{"LSDG MD5: " + TaiXiuUtils.logLichSuPhien(this.lichSuPhienTX, 100)});
    }

    private void saveReferences() {
        try {
            this.mgService.saveReferenceId(this.referenceTaiXiuId, 4);
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{"Save reference error " + e.getMessage()});
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 22000: {
                this.subcribeMiniGame(user, dataCmd);
                break;
            }
            case 22001: {
                this.unsubscribeMiniGame(user, dataCmd);
                break;
            }
            case 22110: {
                if (GameUtils.disablePlayMiniGame(user)) {
                    return;
                }
                this.betTaiXiu(user, dataCmd);
                break;
            }
            case 22116: {
                this.getLichSuPhienTX(user);
            }
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
        this.logger.debug("doSubcribeMiniGame gameId:" + gameId + " roomId:" + roomId);
        switch (gameId) {
            case 22000: {
                short moneyType = MGRoomTaiXiuMD5.getMoneyType(roomId);
                String keyRoom = MGRoomTaiXiuMD5.getKeyRoom(moneyType);
                MGRoomTaiXiuMD5 roomTX = (MGRoomTaiXiuMD5)this.getGame(keyRoom);
                if (roomTX != null) {
                    roomTX.joinRoom(user);
                    roomTX.updateTaiXiuInfo(user, moneyType, this.session_md5_string, this.session_before_md5_string);
                    break;
                }
                CommonHandle.writeErrLog((String)"Game TAI XIU MD5 not found");
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
            case 22000: {
                short moneyType = MGRoomTaiXiuMD5.getMoneyType(roomId);
                String keyRoom = MGRoomTaiXiuMD5.getKeyRoom(moneyType);
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

    public String genRandomCode() {
        int length = 10;
        boolean useLetters = true;
        boolean useNumbers = true;
        String generatedString = RandomStringUtils.random((int)length, (boolean)useLetters, (boolean)useNumbers);
        return generatedString;
    }

    public String MD5(String md5) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString(array[i] & 0xFF | 0x100).substring(1, 3));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            return null;
        }
    }

    private void startBotMd55() {
        Random rd = new Random();
        boolean bool = rd.nextBoolean();
        if (bool) {
            this.c = 1;
        } else if (!bool) {
            this.c = 0;
        }
    }

    private void startNewRoundTX() {
        String md5;
        MGRoomTaiXiuMD5 roomTXVin = this.getRoomTX((short)1);
        ++this.referenceTaiXiuId;
        this.generateTaiXiuDices();
        this.session_before_md5_string = md5 = this.genRandomCode() + "#" + this.referenceTaiXiuId + ":[" + this.currDices[0] + "," + this.currDices[1] + "," + this.currDices[2] + "]";
        this.session_md5_string = md5 = this.MD5(md5);
        roomTXVin.startNewGame(this.referenceTaiXiuId);
        StartNewGameTaiXiuMsg msg = new StartNewGameTaiXiuMsg();
        msg.referenceId = this.referenceTaiXiuId;
        msg.md5 = md5;
        this.sendMessageToTaiXiuNewThread(msg);
        this.saveReferences();
        if (this.sMD5 == 1) {
            TelegramUtil.BotMD5((String)"<b>\ud83d\udc49 BOT \u0111ang ph\u00e2n t\u00edch m\u00e3 MD5 phi\u00ean ti\u1ebfp theo</b>");
            this.startBotdoclenh();
            if (this.rswl == 1) {
                this.win = 0;
                this.lose = 0;
                this.rswl = 0;
            }
        }
    }

    private void scheduleBot() {
        try {
            this.botsVin.clear();
            this.botsVin = BotMinigame.getBotTaiXiu("vin");
            this.logger.debug("BOTS VIN MD5: " + this.botsVin.size());
            LoggerFactory.getLogger((String)"TaiXiuMD5Module").debug("BOTS VIN MD5: " + this.botsVin.size());
            List<BotTaiXiu> botsVip = BotMinigame.getVipBotTaiXiu();
            this.botsVin.addAll(botsVip);
            Debug.trace((Object[])new Object[]{"TX BOTS VIP MD5: " + botsVip.size()});
            this.logger.debug("TX BOTS VIP MD5: " + botsVip.size());
        }
        catch (Exception e) {
            GameUtils.sendAlert("Bot tai xiu MD5 start error: " + e.getMessage() + ", time= " + DateTimeUtils.getCurrentTime());
            LoggerFactory.getLogger((String)"TaiXiuMD5Module").error("error:", (Throwable)e);
        }
    }

    private void botBet(int count) {
        MGRoomTaiXiuMD5 roomVin = this.getRoomTX((short)1);
        for (BotTaiXiu b : this.botsVin) {
            if (b.getTimeBetting() != 60 - count) continue;
            roomVin.betTaiXiu(b.getNickname(), 0, b.getBetValue(), b.getTimeBetting(), (short)1, b.getBetSide(), true);
        }
    }

    public void betTaiXiu(User user, DataCmd dataCmd) {
        BetTaiXiuCmd cmd = new BetTaiXiuCmd(dataCmd);
        MGRoomTaiXiuMD5 roomTX = this.getRoomTX(cmd.moneyType);
        if (roomTX != null) {
            roomTX.betTaiXiu(user, cmd);
        }
    }

    private synchronized void gameLoop() {
        try {
            ++this.count;
            this.botBet(this.count);
            if (this.countRutLoc > -1) {
                ++this.countRutLoc;
            }
            MGRoomTaiXiuMD5 roomTXVin = this.getRoomTX((short)1);
            roomTXVin.updateTaiXiuPerSecond();
            this.sendTXTime(roomTXVin.getRemainTime(), roomTXVin.isBetting());
            switch (this.count) {
                case 45: {
                    roomTXVin.disableBetting();
                    break;
                }
                case 50: {
                    roomTXVin.finish();
                    break;
                }
                case 48: {
                    this.forceBalanceLateGame(roomTXVin);
                    break;
                }
                case 51: {
                    this.endGame(roomTXVin);
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
                case 3: {
                    this.startBotMd55();
                    break;
                }
                case 65: {
                    roomTXVin.getBalanceTX().startNewRound();
                    this.startNewRoundTX();
                    this.count = 0;
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"Exception MD5: " + e.getMessage(), e});
        }
    }

    private void forceBalanceLateGame(MGRoomTaiXiuMD5 roomTXVin) {
    }

    private void resetForceBalance() {
        this.forceBetSide = (short)-1;
    }

    private void endGame(MGRoomTaiXiuMD5 roomTXVin) {
        long moneyBetXiu = roomTXVin.nguoichoidatXiu;
        long moneyBetTai = roomTXVin.nguoichoidatTai;
        Debug.trace((Object[])new Object[]{"TAIXIUMD5DEBUG Result1 nguoichoidatXiu:" + moneyBetXiu + " nguoichoidatTai" + moneyBetTai + " this.referenceTaiXiuId: " + this.referenceTaiXiuId});
        if (roomTXVin.nguoichoidatXiu - roomTXVin.nguoichoidatTai != 0L) {
            short[] result = new short[]{};
            int Xtimes = 3;
            short[] dataCache = roomTXVin.api.suaKetQuaTaiXiu();
            if (dataCache != null) {
                result = dataCache;
                Debug.trace((Object[])new Object[]{"TAIXIUMD5DEBUG Result data cache :" + result[0] + " " + result[1] + " " + result[2]});
            } else {
                result = TaiXiuUtil.genarateRandomResult();
                int totalDiceTemp = result[0] + result[1] + result[2];
                while (totalDiceTemp == 3 || totalDiceTemp == 18) {
                    result = TaiXiuUtil.genarateRandomResult();
                    totalDiceTemp = result[0] + result[1] + result[2];
                }
                Debug.trace((Object[])new Object[]{"TAIXIUMD5DEBUG Result :" + result[0] + " " + result[1] + " " + result[2]});
                if (winCount.get() > 3) {
                    winCount.set(0);
                }
                if (moneyBetTai + moneyBetXiu > 0L) {
                    Debug.trace((Object[])new Object[]{"MGRoomTaiXiuMd5 debug moneyBetTai" + moneyBetTai + " moneyBetXiu= " + moneyBetXiu});
                    if (winCount.get() < 3) {
                        if (TaiXiuUtil.isXiu(result)) {
                            Debug.trace((Object[])new Object[]{"MGRoomTaiXiuMd5 debug TaiXiuUtil.isXiu(result)" + moneyBetTai + " moneyBetXiu= " + moneyBetXiu});
                            if (moneyBetXiu > moneyBetTai) {
                                winCount.getAndIncrement();
                                result = TaiXiuUtil.genarateResult(true);
                                Debug.trace((Object[])new Object[]{"MGRoomTaiXiuMd5 debug this.fundTaiXiu - moneyMinusFund < 0 " + moneyBetTai + " moneyBetXiu= " + moneyBetXiu});
                            }
                        } else {
                            Debug.trace((Object[])new Object[]{"MGRoomTaiXiuMd5 debug false TaiXiuUtil.isXiu(result)" + moneyBetTai + " moneyBetXiu= " + moneyBetXiu});
                            if (moneyBetTai > moneyBetXiu) {
                                winCount.getAndIncrement();
                                result = TaiXiuUtil.genarateResult(false);
                                Debug.trace((Object[])new Object[]{"MGRoomTaiXiuMd5 debug this.fundTaiXiu - moneyMinusFund < 0" + moneyBetTai + " moneyBetXiu= " + moneyBetXiu});
                            }
                        }
                    } else {
                        result = moneyBetTai > moneyBetXiu ? TaiXiuUtil.genarateResult(false) : TaiXiuUtil.genarateResult(true);
                        winCount.set(0);
                        Debug.trace((Object[])new Object[]{"MGRoomTaiXiuMd5 debug winCount.get() > Xtimes" + moneyBetTai + " moneyBetXiu= " + moneyBetXiu});
                    }
                }
            }
            short totalGen = (short)(result[0] + result[1] + result[2]);
            short resultGen = totalGen > 10 ? (short)1 : 0;
            Debug.trace((Object[])new Object[]{"TAIXIUMD5DEBUG Result data old :" + this.currDices[0] + " " + this.currDices[1] + " " + this.currDices[2] + "   ketqua: " + (resultGen != this.result)});
            if (resultGen != this.result) {
                String md5;
                this.currDices = result;
                short total = (short)(this.currDices[0] + this.currDices[1] + this.currDices[2]);
                this.result = total > 10 ? (short)1 : 0;
                this.session_before_md5_string = md5 = this.genRandomCode() + "#" + this.referenceTaiXiuId + ":[" + this.currDices[0] + "," + this.currDices[1] + "," + this.currDices[2] + "]";
                this.session_md5_string = md5 = this.MD5(md5);
            }
        }
        roomTXVin.updateResultDices(this.currDices, this.result, this.session_before_md5_string, this.session_md5_string);
        ResultTaiXiu resultTX = new ResultTaiXiu();
        resultTX.referenceId = this.referenceTaiXiuId;
        resultTX.result = this.result;
        resultTX.dice1 = this.currDices[0];
        resultTX.dice2 = this.currDices[1];
        resultTX.dice3 = this.currDices[2];
        resultTX.before_md5 = this.session_before_md5_string;
        resultTX.md5 = this.session_md5_string;
        Debug.trace((Object[])new Object[]{"RESULT DICES MD5: " + this.currDices[0] + " - " + this.currDices[1] + " - " + this.currDices[2] + "   " + this.result});
        this.lichSuPhienTX.add(resultTX);
        if (this.lichSuPhienTX.size() > 120) {
            this.lichSuPhienTX.remove(0);
        }
        if (this.ssMD5 == 1) {
            this.startBotdoclenhend();
        }
    }

    private void generateTaiXiuDices() {
        // Phase 4 — pct-aware force-side via CanCuaRtpBalancer (subtract bot bets to
        // target real-player house edge). Returns -1 when flag off or pot too small.
        short forceSide = (short) -1;
        try {
            MGRoomTaiXiuMD5 roomVin = this.getRoomTX((short) 1);
            if (roomVin != null) {
                long betXiuUsers = roomVin.getUserBetXiu();  // side 0
                long betTaiUsers = roomVin.getUserBetTai();  // side 1
                forceSide = game.modules.minigame.utils.CanCuaRtpBalancer.chooseWinningSide(
                        betXiuUsers, betTaiUsers, "taixiu");
            }
        } catch (Throwable t) {
            forceSide = (short) -1;
        }
        this.currDices = this.generationTX.generateResult(forceSide);
        Debug.trace((Object[])new Object[]{"GENERATE RESULT DICES MD5: " + this.currDices[0] + " - " + this.currDices[1] + " - " + this.currDices[2] + "   " + this.result});
        this.resetForceBalance();
        short total = (short)(this.currDices[0] + this.currDices[1] + this.currDices[2]);
        this.result = total > 10 ? (short)1 : 0;
    }

    private void getLichSuPhienTX(User user) {
        LichSuPhienMsg msg = new LichSuPhienMsg();
        msg.data = TaiXiuUtils.buildLichSuPhien(this.lichSuPhienTX, 100);
        Debug.trace((Object[])new Object[]{"LSDG MD5: " + TaiXiuUtils.logLichSuPhien(this.lichSuPhienTX, 100)});
        this.send(msg, user);
    }

    private void sendMessageToTaiXiuNewThread(BaseMsg msg) {
        SendMessageToTXThread t = new SendMessageToTXThread(false, msg);
        this.executor.execute(t);
    }

    private void sendTXTime(short remainTime, boolean betting) {
    }

    private void sendMessageToAllUsers(BaseMsg msg) {
        List users = ExtensionUtility.globalUserManager.getAllUsers();
        if (users != null) {
            this.send(msg, users);
        }
    }

    private void sendMessageToTaiXiu(BaseMsg msg) {
        MGRoomTaiXiuMD5 roomTXVin = this.getRoomTX((short)1);
        roomTXVin.sendMessageToRoom(msg);
    }

    public MGRoom getGame(String key) {
        return this.rooms.get(key);
    }

    private MGRoomTaiXiuMD5 getRoomTX(short moneyType) {
        String keyRoom = MGRoomTaiXiuMD5.getKeyRoom(moneyType);
        return (MGRoomTaiXiuMD5)this.getGame(keyRoom);
    }

    public void startBotdoclenh() {
        this.timer.schedule(new TimerTask(){

            @Override
            public void run() {
                TaiXiuMD5Module.this.Botdoclenh();
            }
        }, 5000L);
    }

    public void startBotdoclenhend() {
        this.timer.schedule(new TimerTask(){

            @Override
            public void run() {
                TaiXiuMD5Module.this.Botdoclenhend();
            }
        }, 3000L);
    }

    public void Botdoclenh() {
        TaiXiuMD5Module txmd = new TaiXiuMD5Module();
        Random random = new Random();
        int percentage = 50 + random.nextInt(46);
        if (this.c == 1) {
            TelegramUtil.BotMD5((String)("\ud83d\udd08 M\u1ecdi ng\u01b0\u1eddi! H\u00e3y ch\u1ecdn: <b>T\u00c0I: </b>" + percentage + "%"));
            TelegramUtil.BotMD5((String)"\ud83d\udd51  Ch\u1edd k\u1ebft  qu\u1ea3...");
            TelegramUtil.BotMD5((String)"\u23f3");
        } else {
            TelegramUtil.BotMD5((String)("\ud83d\udd08 M\u1ecdi ng\u01b0\u1eddi! H\u00e3y ch\u1ecdn: <b>X\u1ec8U: </b>" + percentage + "%"));
            TelegramUtil.BotMD5((String)"\ud83d\udd51  Ch\u1edd k\u1ebft  qu\u1ea3...");
            TelegramUtil.BotMD5((String)"\u23f3");
        }
        this.ssMD5 = 1;
    }

    public void Botdoclenhend() {
        TaiXiuMD5Module txmd = new TaiXiuMD5Module();
        int kq = this.currDices[0] + this.currDices[1] + this.currDices[2];
        String xxa = "";
        String xxb = "";
        String xxc = "";
        if (this.currDices[0] == 1) {
            xxa = "1\ufe0f\u20e3";
        }
        if (this.currDices[0] == 2) {
            xxa = "2\ufe0f\u20e3";
        }
        if (this.currDices[0] == 3) {
            xxa = "3\ufe0f\u20e3";
        }
        if (this.currDices[0] == 4) {
            xxa = "4\ufe0f\u20e3";
        }
        if (this.currDices[0] == 5) {
            xxa = "5\ufe0f\u20e3";
        }
        if (this.currDices[0] == 6) {
            xxa = "6\ufe0f\u20e3";
        }
        if (this.currDices[1] == 1) {
            xxb = "1\ufe0f\u20e3";
        }
        if (this.currDices[1] == 2) {
            xxb = "2\ufe0f\u20e3";
        }
        if (this.currDices[1] == 3) {
            xxb = "3\ufe0f\u20e3";
        }
        if (this.currDices[1] == 4) {
            xxb = "4\ufe0f\u20e3";
        }
        if (this.currDices[1] == 5) {
            xxb = "5\ufe0f\u20e3";
        }
        if (this.currDices[1] == 6) {
            xxb = "6\ufe0f\u20e3";
        }
        if (this.currDices[2] == 1) {
            xxc = "1\ufe0f\u20e3";
        }
        if (this.currDices[2] == 2) {
            xxc = "2\ufe0f\u20e3";
        }
        if (this.currDices[2] == 3) {
            xxc = "3\ufe0f\u20e3";
        }
        if (this.currDices[2] == 4) {
            xxc = "4\ufe0f\u20e3";
        }
        if (this.currDices[2] == 5) {
            xxc = "5\ufe0f\u20e3";
        }
        if (this.currDices[2] == 6) {
            xxc = "6\ufe0f\u20e3";
        }
        if (kq < 11) {
            TelegramUtil.BotMD5((String)("\ud83d\udea6Phi\u00ean v\u1eeba xong :  <b> X\u1ec8U</b> - " + xxa + xxb + xxc + "-" + kq));
            this.d = 0;
        } else {
            TelegramUtil.BotMD5((String)("\ud83d\udea6Phi\u00ean v\u1eeba xong :  <b> T\u00c0I</b> - " + xxa + xxb + xxc + "-" + kq));
            this.d = 1;
        }
        if (this.c == this.d) {
            ++this.win;
            TelegramUtil.BotMD5((String)"\ud83d\udd39K\u1ebft Qu\u1ea3: <b>\u2705 Th\u1eafng</b>");
            TelegramUtil.BotMD5((String)("T\u1ed5ng th\u1eafng: <b>" + this.win + "</b>\nT\u1ed5ng thua: <b>" + this.lose + "</b>"));
        } else {
            ++this.lose;
            TelegramUtil.BotMD5((String)"\ud83d\udd39K\u1ebft Qu\u1ea3: <b>\u274c Thua</b>");
            TelegramUtil.BotMD5((String)("T\u1ed5ng th\u1eafng: <b>" + this.win + "</b>\nT\u1ed5ng thua: <b>" + this.lose + "</b>"));
        }
        TelegramUtil.BotMD5((String)"https://HITCLUB/0915%20(1).gif");
        if (this.win + this.lose == 50) {
            this.win = 0;
            this.lose = 0;
            this.sMD5 = 0;
            this.ssMD5 = 0;
            this.rswl = 0;
            TelegramUtil.BotMD5((String)"--- K\u1ebeT TH\u00daC CA ---");
            TelegramUtil.BotMD5((String)"=> T\u1ea2I GAME T\u1ea0I: https://OGK.VIN \n ");
        }
    }

    public class ScheduledTask {
        public ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public ScheduledTask() {
            ArrayList<Calendar> scheduledTimes = new ArrayList<Calendar>();
            Calendar scheduledTime1 = Calendar.getInstance();
            scheduledTime1.set(11, 14);
            scheduledTime1.set(12, 0);
            scheduledTime1.set(13, 0);
            scheduledTime1.set(14, 0);
            scheduledTimes.add(scheduledTime1);
            Calendar scheduledTime2 = Calendar.getInstance();
            scheduledTime2.set(11, 16);
            scheduledTime2.set(12, 0);
            scheduledTime2.set(13, 0);
            scheduledTime2.set(14, 0);
            scheduledTimes.add(scheduledTime2);
            Calendar scheduledTime3 = Calendar.getInstance();
            scheduledTime3.set(11, 19);
            scheduledTime3.set(12, 0);
            scheduledTime3.set(13, 0);
            scheduledTime3.set(14, 0);
            scheduledTimes.add(scheduledTime3);
            for (Calendar scheduledTime : scheduledTimes) {
                if (scheduledTime.before(Calendar.getInstance())) {
                    scheduledTime.add(5, 1);
                }
                long initialDelay = scheduledTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                this.scheduler.scheduleAtFixedRate(new IncrementTask(), initialDelay, 86400000L, TimeUnit.MILLISECONDS);
            }
        }

        public class IncrementTask
        implements Runnable {
            TaiXiuMD5Module txmd = new TaiXiuMD5Module();

            @Override
            public void run() {
                TaiXiuMD5Module.this.rswl = 1;
                TaiXiuMD5Module.this.sMD5 = 1;
            }
        }
    }

    private final class ScheduleBotTask
    extends Thread {
        private ScheduleBotTask() {
        }

        @Override
        public void run() {
            TaiXiuMD5Module.this.scheduleBot();
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
                TaiXiuMD5Module.this.sendMessageToAllUsers(this.msg);
            } else {
                TaiXiuMD5Module.this.sendMessageToTaiXiu(this.msg);
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
                MGRoomTaiXiuMD5 room = TaiXiuMD5Module.this.getRoomTX(this.roomId);
                room.calculatePrize(TaiXiuMD5Module.this.referenceTaiXiuId);
            }
            catch (Throwable e) {
                Debug.trace((Object[])new Object[]{"Calculate TX MD5 " + this.roomId + ", phien= " + TaiXiuMD5Module.this.referenceTaiXiuId + " error: " + e.getMessage()});
            }
            long endTime = System.currentTimeMillis();
            Debug.trace((Object[])new Object[]{"CALCUALTE PRIZE MD5, time handle= " + (endTime - startTime) + " (ms)"});
            TaiXiuMD5Module.this.txService.updateAllTop();
        }
    }

    private final class ServerReadyTask
    implements Runnable {
        private ServerReadyTask() {
        }

        @Override
        public void run() {
            if (!TaiXiuMD5Module.this.serverReady) {
                Debug.trace((Object[])new Object[]{"START TXMINI_MD5 GAME"});
                TaiXiuMD5Module.this.serverReady = true;
                ScheduleBotTask t = new ScheduleBotTask();
                TaiXiuMD5Module.this.executor.execute(t);
                TaiXiuMD5Module.this.startNewRoundTX();
                ScheduledTask scheduledTask = new ScheduledTask();
            }
        }
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                TaiXiuMD5Module.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}

