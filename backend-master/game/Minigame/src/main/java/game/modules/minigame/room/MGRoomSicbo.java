/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.common.business.Debug
 *  com.google.gson.Gson
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.MongoIterable
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.TaiXiuService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  org.bson.Document
 */
package game.modules.minigame.room;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuSicboServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.TaiXiu.TaiXiuUtil;
import game.modules.description.TaiXiuDescription.TaiXiuDescriptionUtils;
import game.modules.minigame.cmd.rev.BetSicboCmd;
import game.modules.minigame.cmd.send.TaiXiuJackpotMsg;
import game.modules.minigame.cmd.send.TaiXiuRefundMsg;
import game.modules.minigame.cmd.send.SicboInfoMsg;
import game.modules.minigame.cmd.send.UpdatePrizeTaiXiuMsg;
import game.modules.minigame.cmd.send.sicbo.BetSicboBotMsg;
import game.modules.minigame.cmd.send.sicbo.BetSicboMsg;
import game.modules.minigame.cmd.send.sicbo.UpdateFinalSicboMsg;
import game.modules.minigame.cmd.send.sicbo.UpdatePrizeSicboMsg;
import game.modules.minigame.cmd.send.sicbo.UpdateResultSicboDicesMsg;
import game.modules.minigame.cmd.send.sicbo.UpdateSicboPerSecondMsg;
import game.modules.minigame.entities.BalanceMoneyTX;
import game.modules.minigame.entities.MinigameConstant;
import game.modules.minigame.entities.PotSicbo;
import game.modules.minigame.entities.PotTaiXiu;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.utils.TaiXiuUtils;
import game.utils.ConfigGame;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.bson.Document;

/**
 * SICBO — handler 28000.
 *
 * Renamed 2026-04-12 from TaiXiuSicbo* -> Sicbo* so stack traces, log greps,
 * and bug reports unambiguously distinguish Sicbo from TaiXiu (handler 2000).
 * See docs/TAIXIU-SICBO-GAME-ARCHITECTURE.md for the architecture rules,
 * especially the hardcoded-vs-config-driven round timer pitfall that caused
 * the Apr 8-12 instability window.
 */
public class MGRoomSicbo
extends MGRoom {
    public long referenceId;
    private short moneyType;
    private String moneyTypeStr;
    private PotTaiXiu potTai;
    private PotTaiXiu potXiu;
    private long startTime = 0L;
    private short result = (short)-1;
    private boolean bettingRound = false;
    private boolean enableBetting = false;
    private ResultTaiXiu resultTX;
    private TaiXiuService api = new TaiXiuSicboServiceImpl();
    private UserService userService = new UserServiceImpl();
    private CacheService cacheService = new CacheServiceImpl();
    private BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    private float tax = 0.0f;
    private float taxJp = MinigameConstant.MINIGAME_TAX_TX_JACKPOT;
    private BalanceMoneyTX balance = new BalanceMoneyTX();
    private long blackListBetTai = 0L;
    private long blackListBetXiu = 0L;
    private long whiteListBetTai = 0L;
    private long whiteListBetXiu = 0L;
    private boolean flagJpTai = false;
    private boolean flagJpXiu = false;
    private static boolean isJpTai = false;
    private static boolean isJpXiu = false;
    private long fundTaiXiu = 0L;
    private static volatile long fundJpTai = 0L;
    private static volatile long fundJpXiu = 0L;
    private static long fundJpFakeTai = 0L;
    private static long fundJpFakeXiu = 0L;
    private static long nextJpTai = 5L;
    private static long nextJpXiu = 5L;
    private static long fundJpAll = 0L;
    private long minMoneyJp = 10000000L;
    private static AtomicInteger winCount = new AtomicInteger(0);
    /**
     * Per-room monotonic counter for the txId we hand to MoneyGateway. Each
     * call to {@code userService.updateMoney} consumes one tick. Combined
     * with the round's referenceId, the resulting transId is unique per
     * bet attempt yet stable enough for idempotent retries within a JVM
     * lifetime (the SQL UNIQUE on tx_id + user_id is the authoritative
     * dedup; this is the per-bet uniqueness side of that pair).
     */
    private final java.util.concurrent.atomic.AtomicLong txnSequence =
            new java.util.concurrent.atomic.AtomicLong(0L);
    private static AtomicInteger countJpTai = new AtomicInteger(0);
    private static AtomicInteger countJpXiu = new AtomicInteger(0);
    private MiniGameService mgService = new MiniGameServiceImpl();
    public long realPotTai = 0L;
    public long realPotXiu = 0L;
    public short realNumBetTai = 0;
    public short realNumBetXiu = 0;
    public List<TransactionTaiXiuDetail> listUserBet = new ArrayList<TransactionTaiXiuDetail>();
    public List<String> listResult = new ArrayList<String>();
    public int[] diceRs;
    public long betIndex = 0L;
    private Gson gson = new Gson();
    public long totalValueBetUser = 0L;

    public MGRoomSicbo(String name, long referenceId, short moneyType, long fundTaiXiu, long fundJpTais, long fundJpXius, long jpFkTais, long jpFkXius) {
        super(name);
        this.moneyType = moneyType;
        this.moneyTypeStr = "xu";
        if (moneyType == 1) {
            this.moneyTypeStr = "vin";
            this.tax = MinigameConstant.MINIGAME_TAX_TX;
            this.taxJp = MinigameConstant.MINIGAME_TAX_TX_JACKPOT;
            ReportMoneySystemModel model = this.api.getReportTX(ConfigGame.getIntValue("interval_reset_balance", 10));
            if (model != null) {
                this.balance = new BalanceMoneyTX(model.moneyWin, model.moneyLost, model.fee, model.dateReset);
                Debug.trace((Object[])new Object[]{"TAI XIU VIN, win=" + model.moneyWin + ", loss=" + model.moneyLost + ", fee= " + model.fee + ", date reset= " + model.dateReset});
            }
        } else {
            this.tax = MinigameConstant.MINIGAME_TAX_TX;
        }
        this.potTai = new PotTaiXiu();
        this.potXiu = new PotTaiXiu();
        this.referenceId = referenceId;
        this.fundTaiXiu = fundTaiXiu;
        fundJpTai = fundJpTais;
        fundJpXiu = fundJpXius;
        fundJpFakeTai = jpFkTais;
        fundJpFakeXiu = jpFkXius;
        if (fundJpFakeTai + fundJpFakeXiu < 50000000L) {
            fundJpFakeTai = 25000000L;
            fundJpFakeXiu = 25000000L;
        }
        String collectionName = "user_bet_tai_xiu_sicbo";
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        boolean collectionExists = false;
        MongoIterable<String> collectionNames = db.listCollectionNames();
        for (String x : collectionNames) {
            if (!x.equalsIgnoreCase(collectionName)) continue;
            collectionExists = true;
            break;
        }
        if (!collectionExists) {
            db.createCollection(collectionName);
            System.out.println("Collection created: " + collectionName);
        } else {
            System.out.println("Collection already exists: " + collectionName);
        }
        Debug.trace((Object[])new Object[]{"TAI XIU VIN: moneyTypeStr = " + this.moneyTypeStr + " ,fundJpTai= " + fundJpTai + ",fundJpXiu=" + fundJpXiu});
    }

    public void startNewGame(long newReferenceId) {
        this.diceRs = null;
        this.listResult = new ArrayList<String>();
        this.listUserBet = new ArrayList<TransactionTaiXiuDetail>();
        this.referenceId = newReferenceId;
        this.bettingRound = true;
        this.enableBetting = true;
        this.blackListBetTai = 0L;
        this.blackListBetXiu = 0L;
        this.whiteListBetTai = 0L;
        this.whiteListBetXiu = 0L;
        this.betIndex = 0L;
        this.realPotTai = 0L;
        this.realPotXiu = 0L;
        this.realNumBetTai = 0;
        this.realNumBetXiu = 0;
        this.potTai.renew();
        this.potXiu.renew();
        this.startTime = System.currentTimeMillis();
        this.totalValueBetUser = 0L;
        Debug.trace((Object[])new Object[]{"START NEW ROUND " + this.referenceId});
        this.clearUserBetToDb();
        if (fundJpFakeTai + fundJpFakeXiu < 50000000L) {
            fundJpFakeTai = 25000000L;
            fundJpFakeXiu = 25000000L;
        }
    }

    public void finish() {
        // 2026-05-08 (final): NO startTime reset. With the reset, server
        // emitted a second monotonic countdown (34→1) after the result
        // phase, which the operator saw as "countdown doesn't stop at 0"
        // / "messy". Without it, getRemainTime stays in the result-phase
        // branch and clamps cleanly to 0 once resultTime elapses, then
        // startNewGame at gameLoop count=55 fires its own startTime
        // reset for the next round.
        //
        // Animation dependency note: client-side handlers (DICES_RESULT
        // cmd 28113, BetSicboMsg cmd 28110, SicboInfoMsg cmd 28111) are
        // driven by their own messages, not by startTime mutation. The
        // earlier "everything frozen" report was a stale browser session
        // after a rebuild cycle — a hard reload (Ctrl+Shift+R) recovered.
        this.resultTX = null;
        this.bettingRound = false;
        try {
            this.cacheService.removeKey("allow_betting_" + this.referenceId);
            this.cacheService.removeKey("force_result_" + this.referenceId);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public boolean isFlagJpTai() {
        return this.flagJpTai;
    }

    public void setFlagJpTai(boolean flagJpTai) {
        this.flagJpTai = flagJpTai;
    }

    public boolean isFlagJpXiu() {
        return this.flagJpXiu;
    }

    public void setFlagJpXiu(boolean flagJpXiu) {
        this.flagJpXiu = flagJpXiu;
    }

    public long getFundTaiXiu() {
        return this.fundTaiXiu;
    }

    public void setFundTaiXiu(long fundTaiXiu) {
        this.fundTaiXiu = fundTaiXiu;
    }

    public long getFundJpTai() {
        return fundJpTai;
    }

    public void setFundJpTai(long fundJpTais) {
        fundJpTai = fundJpTais;
    }

    public long getFundJpXiu() {
        return fundJpXiu;
    }

    public void setFundJpXiu(long fundJpXius) {
        fundJpXiu = fundJpXius;
    }

    public void disableBetting() {
        this.enableBetting = false;
        // 2026-05-08: also flip the wire-level `bettingRound` flag here.
        // updateTaiXiuPerSecond stamps msg.bettingState from bettingRound,
        // and bettingRound used to stay true until finish() ran 3 ticks
        // later (gameLoop case 37). That gap caused the SicBo client to
        // see {remainTime=20, bettingState=true} for the first 3 frames of
        // the result phase, producing the visible "1 → 20" jump in the
        // Cocos UI. Flipping it in lockstep with disableBetting (gameLoop
        // case 34) keeps the wire state self-consistent.
        this.bettingRound = false;
        this.cacheService.setValue("allow_betting_" + this.referenceId, 0);
    }

    public void updateResultDices(short[] dices, short result) {
        this.result = result;
        UpdateResultSicboDicesMsg msg = new UpdateResultSicboDicesMsg();
        msg.result = result;
        msg.dice1 = dices[0];
        msg.dice2 = dices[1];
        msg.dice3 = dices[2];
        this.resultTX = new ResultTaiXiu();
        this.resultTX.referenceId = this.referenceId;
        this.resultTX.dice1 = msg.dice1;
        this.resultTX.dice2 = msg.dice2;
        this.resultTX.dice3 = msg.dice3;
        this.resultTX.result = msg.result;
        this.resultTX.moneyType = this.moneyType;
        this.sendMessageToRoom(msg);
    }

    public short getRemainTime() {
        // SUN-769 (re-applied 2026-04-29): see MGRoomTaiXiu.getRemainTime
        // for full rationale. Same regression reintroduced by a60816b0 on
        // 2026-04-12, same fix applied here.
        //
        // SUN-EXPLOIT-GUARD (2026-05-03): bettingTime aligned to gameLoop's
        // actual disableBetting tick (count=34 in SicboModule.gameLoop —
        // unlike MGRoomTaiXiu where it's 50). Previous value 37 caused the
        // countdown to jump from 4 → 20 between count=33 and count=34
        // because enableBetting flipped before the formula expected it.
        // Client UI saw the timer freeze near "5/4" then leap to result.
        // resultTime widened to 21 to cover count 34→55 reveal window.
        //
        // SUN-1245 (defensive): branch on the time-based reveal-start
        // instead of the `enableBetting` flag. Aligns with the same fix
        // in MGRoomTaiXiu — eliminates any flag-vs-formula skew if the
        // gameLoop ticks ever drift again.
        //
        // 2026-05-08: resultTime reverted 21→20 to match the deployed
        // Cocos client's hard-coded subtract-15 in TaiXiuFull.ts (display
        // collapses to 5,4,3,2,1,0 over the result phase). afe760c0
        // widened it to 21, making the client show "22→20" jumps because
        // the server emitted remainTime=21 while the client subtracted 15
        // expecting 20. Reverting unblocks the staging UI without needing
        // a Cocos client rebuild. The 1-sec gap vs gameLoop count=55 is
        // harmless — round ends naturally at the next gameLoop transition.
        // 2026-05-08 (final): 40-sec betting phase per ops request
        // ("countdown must start at 40, not 34→0 then back to 5").
        // Aligned with SicboModule.gameLoop case shifts so disableBetting
        // fires at count=40, finish at count=43, reward at count=48,
        // startNewGame at count=55. Round total stays 55 sec; idle gap
        // between dice reveal and new round shrinks from 13s → 7s.
        //
        // resultTime=8 keeps the result phase shorter than the client's
        // hard-coded -15 subtract in TaiXiuFull.ts:403, so the post-betting
        // display collapses straight to 0 instead of showing a second
        // "5,4,3,2,1" mini-countdown after the main one.
        final int bettingTime = 40;
        final int resultTime = 8;
        long currentTime = System.currentTimeMillis();
        long revealStart = this.startTime + (long) bettingTime * 1000L;
        if (currentTime < revealStart) {
            int elapsed = (int)((currentTime - this.startTime) / 1000L);
            if (elapsed < 0) elapsed = 0;
            if (elapsed > bettingTime) elapsed = bettingTime;
            return (short)(bettingTime - elapsed);
        }
        int elapsedSinceReveal = (int)((currentTime - revealStart) / 1000L);
        if (elapsedSinceReveal < 0) elapsedSinceReveal = 0;
        if (elapsedSinceReveal > resultTime) elapsedSinceReveal = resultTime;
        return (short)(resultTime - elapsedSinceReveal);
    }

    private long getPotOfSide(String betSideStr) {
        int betSide = PotSicbo.getEnumByName(betSideStr).getId();
        AtomicReference<Long> total = new AtomicReference<Long>(0L);
        this.listUserBet.stream().filter(item -> item.betSide == betSide).forEach(item -> total.updateAndGet(v -> v + item.betValue));
        return total.get();
    }

    public void betTaiXiu(User user, BetSicboCmd cmd) {
        if (!this.bettingRound) {
            return;
        }
        Debug.trace((Object[])new Object[]{"Socbo Send : cmd.betSide : " + cmd.betSide + " getEnumByName: " + (short)PotSicbo.getEnumByName(cmd.betSide).getId()});
        BetSicboBotMsg msg = this.betTaiXiu(user.getName(), cmd.userId, cmd.betValue, cmd.inputTime, cmd.moneyType, (short)PotSicbo.getEnumByName(cmd.betSide).getId(), false);
        Debug.trace((Object[])new Object[]{"betTaiXiu -> error = " + msg.Error + "; userName -> " + msg.userName + "; currentMoney -> " + msg.currentMoney + "; moneyBet -> " + msg.moneyBet});
        if (msg.Error == 0) {
            Debug.info((Object[])new Object[]{"SicBo User " + user.getName() + " bet=" + cmd.betValue + " side=" + cmd.betSide + " index=" + this.betIndex});
            ++this.betIndex;
            TransactionTaiXiuDetail transTX = new TransactionTaiXiuDetail(this.referenceId, user.getId(), user.getName(), cmd.betValue, PotSicbo.getEnumByName(cmd.betSide).getId(), (int)cmd.inputTime, (int)cmd.moneyType);
            transTX.currentMoney = msg.currentMoney;
            transTX.transactionCode = this.referenceId + "-" + this.betIndex;
            this.listUserBet.add(transTX);
            try {
                this.api.saveTransactionTaiXiuDetail(transTX);
            }
            catch (Exception e) {
                e.printStackTrace();
                Debug.warn((Object[])new Object[]{"Error save transaction tai xiu detail: " + e.getMessage()});
            }
        }
        BetSicboMsg msg2 = new BetSicboMsg();
        msg2.currentMoney = msg.currentMoney;
        msg2.referenceId = cmd.referenceId;
        msg2.betValue = cmd.betValue;
        msg2.inputTime = cmd.inputTime;
        msg2.moneyType = cmd.moneyType;
        msg2.betSide = cmd.betSide;
        msg2.pot = this.getPotOfSide(cmd.betSide);
        msg2.Error = msg.Error;
        this.sendMessageToUser((BaseMsg)msg2, user);
    }

    public void insertUserBetToDb(String nickname, long betValue, int inputTime, int betSide) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_bet_tai_xiu_sicbo");
        Document doc = new Document();
        doc.append("referentId", this.referenceId);
        doc.append("nick_name", nickname);
        doc.append("inputTime", inputTime);
        doc.append("betSide", betSide);
        doc.append("betValue", betValue);
        doc.append("money_type", this.moneyType == 1 ? 1 : 2);
        col.insertOne(doc);
    }

    public void insertUserJackpotDetailToDb(String time, String countBet, String moneyJackpotAll, String nickName, long money) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_jackpot_tai_xiu_sicbo_details");
        Document doc = new Document();
        doc.append("referentId", this.referenceId);
        doc.append("result", this.result);
        doc.append("time", time);
        doc.append("countBet", countBet);
        doc.append("moneyJackpotAll", moneyJackpotAll);
        doc.append("nickName", nickName);
        doc.append("money", money);
        col.insertOne(doc);
    }

    public void insertUserJackpotToDb(String time, String countBet, String moneyJackpotAll, String data) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_jackpot_tai_xiu_sicbo");
        Document doc = new Document();
        doc.append("referentId", this.referenceId);
        doc.append("result", this.result);
        doc.append("time", time);
        doc.append("countBet", countBet);
        doc.append("moneyJackpotAll", moneyJackpotAll);
        doc.append("data", data);
        col.insertOne(doc);
    }

    public void clearUserBetToDb() {
        // GitLab infra issue #1: deleteMany is the first Mongo call in
        // startNewGame(); when the driver pool is stale (Mongo bounce) the
        // uncaught MongoSocketOpenException kills the round loop and the
        // player countdown never broadcasts. Retry a few times, then swallow —
        // stale docs will be cleared on the next successful round. Do NOT
        // propagate: the round loop is more important than the cleanup.
        try {
            com.vinplay.vbee.common.mongodb.MongoRetryHelper.run(() -> {
                MongoDatabase db = MongoDBConnectionFactory.getDB();
                MongoCollection col = db.getCollection("user_bet_tai_xiu_sicbo");
                col.deleteMany(new org.bson.Document());
            }, "sicbo.clearUserBetToDb");
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger("api")
                    .warn("MGRoomSicbo.clearUserBetToDb: swallowed after retries, will retry next round — "
                            + e.getMessage());
        }
    }

    private boolean isUserBot(String nickName) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("usersSetWin");
        return userMap.containsKey(nickName) && (Boolean)userMap.get(nickName) != false;
    }

    public void updateInfoBotBet(String userName, long betValue, short betSide, short inputTime, short moneyType) {
        ++this.betIndex;
        TransactionTaiXiuDetail transTX = new TransactionTaiXiuDetail(this.referenceId, 0, userName, betValue, (int)betSide, (int)inputTime, (int)moneyType);
        // SUN-1296: stamp current_money on bot bets too. Without this, the
        // long default (0) was written to log_sicbo.current_money and any
        // aggregate that didn't filter user_id=0 (bots) saw a sea of zeros
        // — 1819 / 1822 of recent log_sicbo docs were affected (bot rows).
        // Bots' wallet lives on the same `users` cache after SUN-880's
        // bot-debit unification, so getMoneyUserCache returns the bot's
        // actual pre-debit balance. Failure → leave the field unset (no
        // worse than the legacy 0); never raise into the bet path.
        try {
            long bal = this.userService.getMoneyUserCache(userName, this.moneyTypeStr);
            if (bal > 0L) transTX.currentMoney = bal;
        } catch (Throwable ignore) { /* keep legacy 0; logging would spam */ }
        transTX.transactionCode = this.referenceId + "-" + this.betIndex;
        this.listUserBet.add(transTX);
        try {
            this.api.saveTransactionTaiXiuDetail(transTX);
        }
        catch (Exception e) {
            Debug.warn((Object[])new Object[]{"Error save transaction tai xiu detail: " + e.getMessage()});
        }
    }

    public BetSicboBotMsg betTaiXiu(String nickname, int userId, long betValue, short inputTime, short moneyType, short betSide, boolean isBot) {
        // SUN-1373 — reject bet when Sicbo is administratively disabled.
        // Result code 11 = GAME_INACTIVE (same as TaiXiu, distinct from all
        // existing codes: 0=success, 1=betting-closed, 2=not-in-room,
        // 3=insufficient, 4=bet-too-small, 5=duplicate-side). Bots skip gate.
        if (!isBot && isSicboDisabled()) {
            BetSicboBotMsg inactiveMsg = new BetSicboBotMsg();
            inactiveMsg.Error = (byte)11; // GAME_INACTIVE
            inactiveMsg.userName = nickname;
            inactiveMsg.moneyBet = betValue;
            inactiveMsg.betSide = PotSicbo.getEnumById(betSide).getName();
            inactiveMsg.currentMoney = this.userService.getMoneyUserCache(nickname, this.moneyTypeStr);
            inactiveMsg.pot = this.getPotOfSide(inactiveMsg.betSide);
            return inactiveMsg;
        }
        boolean isLivestream = this.isUserBot(nickname);
        if (!isBot) {
            this.totalValueBetUser += betValue;
            if (betSide == 1) {
                this.realPotTai += betValue;
                if (!this.potTai.hasBet(nickname)) {
                    this.realNumBetTai = (short)(this.realNumBetTai + 1);
                }
            } else {
                this.realPotXiu += betValue;
                if (!this.potXiu.hasBet(nickname)) {
                    this.realNumBetXiu = (short)(this.realNumBetXiu + 1);
                }
            }
        }
        inputTime = this.getRemainTime();
        long currentMoney = this.userService.getMoneyUserCache(nickname, this.moneyTypeStr);
        int result = 2;
        if (betValue >= 100L) {
            if (betValue > currentMoney) {
                result = 3;
            } else {
                TransactionTaiXiuDetail transTX = new TransactionTaiXiuDetail(this.referenceId, userId, nickname, betValue, (int)betSide, (int)inputTime, (int)moneyType);
                transTX.currentMoney = currentMoney;
                if (betSide == 1 && this.potXiu.getTotalBetByUsername(nickname) > 0L || betSide == 0 && this.potTai.getTotalBetByUsername(nickname) > 0L) {
                    result = 5;
                } else {
                    // 2026-05-08: bots now debit real money too (no more
                    // virtual-only bot wallet). Operator wants the bots'
                    // P&L to roll into the same money_gateway_log + cache
                    // path so analytics, agency reports and house-edge
                    // tracking see consistent data. BotMinigame.pushMoney
                    // ToBot tops up bot balances every round (10M floor)
                    // so the real debit doesn't drain the bot pool.
                    //
                    // Idempotency: transId must be UNIQUE per bet attempt
                    // — multiple bets in the same round (multi-side, or
                    // bot+player sharing the round) used to collide on
                    // (txId,source,user_id) UK. Combine referenceId with
                    // a strictly monotonic per-room sequence so retries
                    // are still dedup-safe but distinct bets never clash.
                    long perBetTxId = this.referenceId * 1_000_000L
                            + this.txnSequence.incrementAndGet();
                    MoneyResponse res = this.userService.updateMoney(
                            nickname, -betValue, this.moneyTypeStr,
                            "TaiXiuSicbo", Games.TAI_XIU_SICBO.getId() + "",
                            TaiXiuDescriptionUtils.getTaiXiuBetDescription(
                                    Games.TAI_XIU_SICBO.getId() + "",
                                    this.referenceId, inputTime + "", betSide),
                            0L, Long.valueOf(perBetTxId),
                            TransType.START_TRANS);
                    if (res.isSuccess()) {
                        isBot = isLivestream ? true : this.isBot(nickname);
                        if (moneyType == 1 && !isBot) {
                            int n;
                            SplittableRandom rd = new SplittableRandom();
                            this.balance.addBet(betValue);
                            if (betValue >= (long)ConfigGame.getIntValue("tx_min_money_black_list", 100000) && ConfigGame.inBlackList(nickname) && (n = rd.nextInt(100)) <= ConfigGame.getIntValue("tx_black_list_percent", 50)) {
                                Debug.trace((Object[])new Object[]{"Black list " + nickname + " money= " + betValue + ", bet side= " + betSide});
                                if (betSide == 1) {
                                    this.blackListBetTai += betValue;
                                } else {
                                    this.blackListBetXiu += betValue;
                                }
                            }
                            if (betValue >= (long)ConfigGame.getIntValue("tx_min_money_white_list", 100000) && ConfigGame.inWhiteList(nickname) && (n = rd.nextInt(100)) <= ConfigGame.getIntValue("tx_white_list_percent", 50)) {
                                Debug.trace((Object[])new Object[]{"White list " + nickname + " money= " + betValue + ", bet side= " + betSide});
                                if (betSide == 1) {
                                    this.whiteListBetTai += betValue;
                                } else {
                                    this.whiteListBetXiu += betValue;
                                }
                            }
                        }
                        if (betSide == 1) {
                            this.potTai.bet(transTX, isBot);
                        } else {
                            this.potXiu.bet(transTX, isBot);
                        }
                        currentMoney = res.getCurrentMoney();
                        transTX.genTransactionCode();
                        if (!isBot) {
                            TaiXiuUtils.logBetTaiXiu(transTX);
                        }
                        result = 0;
                        try {
                            this.insertUserBetToDb(nickname, betValue, inputTime, betSide);
                        }
                        catch (Exception exception) {
                            // SUN-1xxx (2026-05-11): see MGRoomTaiXiu for rationale.
                            Debug.trace((Object[])new Object[]{
                                "MGRoomSicbo.insertUserBetToDb FAILED — bet history NOT logged for user="
                                + nickname + " ref=" + this.referenceId + " betValue=" + betValue
                                + " betSide=" + betSide + " err=" + exception.getMessage()});
                        }
                    } else {
                        result = 1;
                    }
                }
            }
        } else {
            result = 4;
        }
        BetSicboBotMsg msg = new BetSicboBotMsg();
        msg.Error = (byte)result;
        msg.userName = nickname;
        msg.moneyBet = betValue;
        msg.betSide = PotSicbo.getEnumById(betSide).getName();
        msg.currentMoney = currentMoney;
        msg.pot = this.getPotOfSide(msg.betSide);
        return msg;
    }

    public void initJackpot() {
        SplittableRandom rand = new SplittableRandom();
        nextJpTai = rand.nextLong(4L) + 4L;
        nextJpXiu = rand.nextLong(4L) + 4L;
        if (isJpTai) {
            fundJpTai = 0L;
            isJpTai = false;
        }
        if (isJpXiu) {
            fundJpXiu = 0L;
            isJpXiu = false;
        }
    }

    // SUN-807: per-round stable virtual-player pad. Re-rolled once at round
    // boundary (when referenceId changes) so the displayed count doesn't
    // jump each second. Each side draws independently in [fakeMin,fakeMax]
    // and exact-equal collisions are jittered so both sides are never the
    // same count (QC's original complaint: "số ng chơi giống nhau 2 bên là
    // fake lộ liễu"). Pad cleared to 0 when round is NOT in betting phase
    // so the between-rounds UI shows 0 (not an idle 30-60).
    private long padReferenceId = -1L;
    private int cachedPadTai = 0;
    private int cachedPadXiu = 0;

    private void refreshPadIfNeeded() {
        if (this.padReferenceId == this.referenceId) return;
        this.padReferenceId = this.referenceId;
        int fakeMin = game.utils.ConfigGame.getIntValue("tx_fake_player_min", 30);
        int fakeMax = game.utils.ConfigGame.getIntValue("tx_fake_player_max", 60);
        if (fakeMax <= 0 || fakeMax < fakeMin) {
            this.cachedPadTai = 0; this.cachedPadXiu = 0; return;
        }
        int span = fakeMax - fakeMin + 1;
        int a = fakeMin + (int)(Math.random() * span);
        int b = fakeMin + (int)(Math.random() * span);
        if (a == b) { b += (b > fakeMin) ? -1 : 1; }
        this.cachedPadTai = a;
        this.cachedPadXiu = b;
    }

    public synchronized void updateTaiXiuPerSecond(long totalPlayer, String topBots) {
        UpdateSicboPerSecondMsg msg = new UpdateSicboPerSecondMsg();
        msg.remainTime = this.getRemainTime();
        msg.bettingState = this.bettingRound;
        msg.potTai = this.getPotTai();
        msg.potXiu = this.getPotXiu();
        refreshPadIfNeeded();
        int padTai = this.bettingRound ? this.cachedPadTai : 0;
        int padXiu = this.bettingRound ? this.cachedPadXiu : 0;
        msg.numBetTai = (short)(this.potTai.getNumBet() + padTai);
        msg.numBetXiu = (short)(this.potXiu.getNumBet() + padXiu);
        msg.totalPlayer = totalPlayer;
        msg.msg = topBots;
        this.sendMessageToRoom(msg);
        this.updateGameInfoToCache();
    }

    public short[] getResult(long id) {
        short[] result = new short[]{};
        short[] dataCache = this.api.suaKetQuaTaiXiu();
        
        if (dataCache != null) {
            result = dataCache;
        } else {
            result = this.generateResultWithHouseEdge();
            
            if (this.totalValueBetUser > 0L) {
                long fundSB;
                long sotientra = this.sotienphaitra(result[0], result[1], result[2]);
                long tienloi = this.totalValueBetUser - sotientra;
                Debug.trace((Object[])new Object[]{"SICBO Result totalValueBetUser:" + this.totalValueBetUser + " fundTaiXiu: " + this.fundTaiXiu + " sotientra: " + sotientra + " tienloi:" + tienloi + " DICE:" + result[0] + "," + result[1] + "," + result[2]});
                
                // Fallback to strict fund protection if somehow RTP target still causes bankruptcy
                if (tienloi < 0L && (fundSB = this.fundTaiXiu + tienloi) < 0L) {
                    while (tienloi < 0L && this.fundTaiXiu + (tienloi = this.totalValueBetUser - (sotientra = this.sotienphaitra((result = TaiXiuUtil.genarateRandomResult())[0], result[1], result[2]))) <= 0L) {
                        Debug.trace((Object[])new Object[]{"SICBO Result vong lap totalValueBetUser:" + this.totalValueBetUser + " sotientra: " + sotientra + " tienloi:" + tienloi + " DICE:" + result[0] + "," + result[1] + "," + result[2]});
                    }
                }
                this.fundTaiXiu += tienloi;
                Debug.trace((Object[])new Object[]{"SICBO Result fundTaiXiu " + this.fundTaiXiu});
            }
        }
        int[] diceValues = new int[]{result[0], result[1], result[2]};
        this.diceRs = diceValues;
        this.listResult = MGRoomSicbo.getWinningStatuses(diceValues);
        this.saveFund(0);
        Debug.trace((Object[])new Object[]{"TAIXIUDEBUG Result End:" + result[0] + " " + result[1] + " " + result[2]});
        this.updateResultDices(result, (short)(!TaiXiuUtil.isXiu(result) ? 1 : 0));
        return result;
    }

    private short[] generateResultWithHouseEdge() {
        if (!game.modules.minigame.utils.CanCuaRtpBalancer.isEnabled()) {
            return TaiXiuUtil.genarateRandomResult();
        }
        double winRatePct = com.vinplay.vbee.common.rtp.RtpResolver.effectivePct(0L, "sicbo");
        double targetEdgePercent = (winRatePct >= 92.0 && com.vinplay.vbee.common.rtp.RtpResolver.effectivePct("sicbo") >= 92.0) ? 0.0 : 100.0 - winRatePct;

        if (targetEdgePercent <= 0.0 || this.totalValueBetUser <= 0) {
            return TaiXiuUtil.genarateRandomResult();
        }

        // Kiem tra lech cua: De giam tai, neu cuoc qua nho (< 100k) khong can tinh
        if (this.totalValueBetUser < 100000) {
            return TaiXiuUtil.genarateRandomResult();
        }

        double targetProfit = this.totalValueBetUser * (targetEdgePercent / 100.0);
        double minDiff = Double.MAX_VALUE;
        List<short[]> bestCombinations = new ArrayList<>();

        for (short d1 = 1; d1 <= 6; d1++) {
            for (short d2 = 1; d2 <= 6; d2++) {
                for (short d3 = 1; d3 <= 6; d3++) {
                    long totalPayout = this.sotienphaitra(d1, d2, d3);
                    double profit = this.totalValueBetUser - totalPayout;
                    double diff = Math.abs(profit - targetProfit);

                    if (diff < minDiff) {
                        minDiff = diff;
                        bestCombinations.clear();
                        bestCombinations.add(new short[]{d1, d2, d3});
                    } else if (diff == minDiff) {
                        bestCombinations.add(new short[]{d1, d2, d3});
                    }
                }
            }
        }

        if (bestCombinations.isEmpty()) return TaiXiuUtil.genarateRandomResult();

        int randIdx = java.util.concurrent.ThreadLocalRandom.current().nextInt(bestCombinations.size());
        short[] res = bestCombinations.get(randIdx);
        Debug.trace("SICBO RTP targetEdge=" + targetEdgePercent + "% targetProfit=" + targetProfit + " minDiff=" + minDiff + " bestSize=" + bestCombinations.size());
        return res;
    }

    private void sendNotifyJp(long id, long amount) {
        TaiXiuJackpotMsg msg = new TaiXiuJackpotMsg();
        msg.amount = amount;
        msg.id = id;
        this.sendMessageToRoom(msg);
    }

    public void saveFund(int result) {
        String keyBot = this.moneyType == 1 ? "SICBO_FUND_VIN" : "TaiXiu_Fund_xu";
        try {
            this.mgService.saveFund(keyBot, this.fundTaiXiu);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private long getRanFakeInit() {
        SplittableRandom rdom = new SplittableRandom();
        return rdom.nextLong(4000000L) + 1000000L;
    }

    private void saveJPFakeTai(long amount) {
        this.cacheService.setValueJp("TaiXiu_Fund_JP_FakeTai", Long.valueOf(amount));
    }

    private void saveJPFakeXiu(long amount) {
        this.cacheService.setValueJp("TaiXiu_Fund_JP_FakeXiu", Long.valueOf(amount));
    }

    public void calculateMoneyReturn() {
        TaiXiuRefundMsg taiXiuRefundMsg;
        long refund;
        Debug.trace((Object[])new Object[]{"calculateMoneyReturn  "});
        PotTaiXiu potX = this.potXiu;
        PotTaiXiu potT = this.potTai;
        long tongTienHopLe = potT == null || potX == null ? 0L : (potT.getTotalValue() > potX.getTotalValue() ? potX.getTotalValue() : potT.getTotalValue());
        long tongTienTaiDaTinh = 0L;
        long tongTienXiuDaTinh = 0L;
        if (potT != null && potT.contributors != null) {
            for (TransactionTaiXiuDetail tran : potT.contributors) {
                try {
                    long tienDuocTinh = tran.betValue;
                    if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
                        tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
                    }
                    tongTienTaiDaTinh += tienDuocTinh;
                    refund = tran.betValue - tienDuocTinh;
                    if (tran.userId == 0 || refund <= 0L) continue;
                    taiXiuRefundMsg = new TaiXiuRefundMsg(refund);
                    Debug.trace((Object[])new Object[]{"Tra lai tien " + tran.username + "    " + refund});
                    this.sendMessageToUser((BaseMsg)taiXiuRefundMsg, tran.username);
                    // SUN-809: publish TaiXiuHoanTien log over RMQ so
                    // LogSumReportUserSQLProcessor subtracts the refunded
                    // portion from taixiu_sicbo column → rolling (cược hợp lệ)
                    // no longer counts cân-cửa bets.
                    game.modules.minigame.utils.TaiXiuUtils.publishLogReportCuaRefund(
                            this.referenceId, tran.userId, tran.username, refund,
                            this.moneyTypeStr, Games.TAI_XIU_SICBO.getId(),
                            this.isBot(tran.username));
                }
                catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"Error calculate prize user1 " + tran.username + " error: " + e.getMessage()});
                }
            }
        }
        if (potX != null && potX.contributors != null) {
            for (TransactionTaiXiuDetail tran : potX.contributors) {
                try {
                    long tienDuocTinh = tran.betValue;
                    if (tongTienXiuDaTinh + tran.betValue > tongTienHopLe) {
                        tienDuocTinh = tongTienHopLe - tongTienXiuDaTinh;
                    }
                    tongTienXiuDaTinh += tienDuocTinh;
                    refund = tran.betValue - tienDuocTinh;
                    if (tran.userId == 0 || refund <= 0L) continue;
                    taiXiuRefundMsg = new TaiXiuRefundMsg(refund);
                    Debug.trace((Object[])new Object[]{"Tra lai tien " + tran.username + "    " + refund});
                    this.sendMessageToUser((BaseMsg)taiXiuRefundMsg, tran.username);
                    // SUN-809: see comment above.
                    game.modules.minigame.utils.TaiXiuUtils.publishLogReportCuaRefund(
                            this.referenceId, tran.userId, tran.username, refund,
                            this.moneyTypeStr, Games.TAI_XIU_SICBO.getId(),
                            this.isBot(tran.username));
                }
                catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"Error calculate prize user2 " + tran.username + " error: " + e.getMessage()});
                }
            }
        }
    }

    private void updateSumTran(Map<String, TransactionTaiXiu> map, TransactionTaiXiuDetail tranDetail) {
        if (map.containsKey(tranDetail.username)) {
            TransactionTaiXiu txt = map.get(tranDetail.username);
            txt.betValue += tranDetail.betValue;
            txt.totalPrize += tranDetail.prize;
            txt.totalRefund += tranDetail.refund;
            txt.totalJp += tranDetail.jpAmount;
            map.put(tranDetail.username, txt);
        } else {
            TransactionTaiXiu tran = new TransactionTaiXiu();
            tran.referenceId = tranDetail.referenceId;
            tran.userId = tranDetail.userId;
            tran.username = tranDetail.username;
            tran.moneyType = tranDetail.moneyType;
            tran.betSide = tranDetail.betSide;
            tran.betValue = tranDetail.betValue;
            tran.totalPrize = tranDetail.prize;
            tran.totalRefund = tranDetail.refund;
            tran.totalJp = tranDetail.jpAmount;
            map.put(tranDetail.username, tran);
        }
    }

    public short calculateBalanceTX(int type) {
        long tienDuocTinh;
        long totalPrizeBotTai = 0L;
        long totalPrizeBotXiu = 0L;
        long totalPrizeUserTai = 0L;
        long totalPrizeUserXiu = 0L;
        long tongTienHopLe = this.potTai.getTotalValue() > this.potXiu.getTotalValue() ? this.potXiu.getTotalValue() : this.potTai.getTotalBotBet();
        long tongTienXiuDaTinh = 0L;
        long tongTienTaiDaTinh = 0L;
        for (TransactionTaiXiuDetail tran : this.potXiu.contributors) {
            tienDuocTinh = tran.betValue;
            if (tongTienXiuDaTinh + tran.betValue > tongTienHopLe) {
                tienDuocTinh = tongTienHopLe - tongTienXiuDaTinh;
            }
            tongTienXiuDaTinh += tienDuocTinh;
            if (this.isBot(tran.username)) {
                totalPrizeBotXiu += tienDuocTinh;
                continue;
            }
            totalPrizeUserXiu += tienDuocTinh;
        }
        for (TransactionTaiXiuDetail tran : this.potTai.contributors) {
            tienDuocTinh = tran.betValue;
            if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
                tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
            }
            tongTienTaiDaTinh += tienDuocTinh;
            if (this.isBot(tran.username)) {
                totalPrizeBotTai += tienDuocTinh;
                continue;
            }
            totalPrizeUserTai += tienDuocTinh;
        }
        Debug.trace((Object[])new Object[]{"Bot tai: " + totalPrizeBotTai + ", bot xiu: " + totalPrizeBotXiu});
        Debug.trace((Object[])new Object[]{"User tai: " + totalPrizeUserTai + ", user xiu: " + totalPrizeUserXiu});
        short result = -1;
        switch (type) {
            case 1: {
                result = this.tinhCuaThang(totalPrizeBotTai, totalPrizeBotXiu);
                Debug.trace((Object[])new Object[]{"He thong am, force= " + result});
                break;
            }
            case -2: {
                result = this.tinhCuaThang(-this.blackListBetTai, -this.blackListBetXiu);
                Debug.trace((Object[])new Object[]{"Black list: " + result});
                break;
            }
            case -3: {
                result = this.tinhCuaThang(this.whiteListBetTai, this.whiteListBetXiu);
                Debug.trace((Object[])new Object[]{"White list: " + result});
                break;
            }
            case -1: {
                long totalUserWinSystem = Math.abs(totalPrizeUserTai - totalPrizeUserXiu);
                if (totalUserWinSystem <= this.balance.getMaxWinUser()) {
                    result = this.tinhCuaThang(totalPrizeUserTai, totalPrizeUserXiu);
                    Debug.trace((Object[])new Object[]{"Nguoi choi am, force = " + result});
                    break;
                }
                Debug.trace((Object[])new Object[]{"Nguoi choi am nhung so tien an qua lo'n= " + totalUserWinSystem});
                result = this.checkHeThongAm(totalPrizeUserTai, totalPrizeUserXiu, totalPrizeBotTai, totalPrizeBotXiu);
                break;
            }
            default: {
                result = this.checkHeThongAm(totalPrizeUserTai, totalPrizeUserXiu, totalPrizeBotTai, totalPrizeBotXiu);
            }
        }
        return result;
    }

    private short tinhCuaThang(long tai, long xiu) {
        if (tai > xiu) {
            return 1;
        }
        if (tai < xiu) {
            return 0;
        }
        return -1;
    }

    private short checkHeThongAm(long totalPrizeUserTai, long totalPrizeUserXiu, long totalPrizeBotTai, long totalPrizeBotXiu) {
        long maxUserWin;
        short result = -1;
        long fee = this.balance.getFee();
        long revenueUser = this.balance.getRevenueUser();
        if ((float)(revenueUser + (long)((float)(maxUserWin = Math.abs(totalPrizeUserTai - totalPrizeUserXiu)) * (100.0f - this.tax) / 100.0f)) >= (float)(-fee) * ConfigGame.getFloatValue("tx_min_fee", 1.0f)) {
            result = this.tinhCuaThang(totalPrizeBotTai, totalPrizeBotXiu);
            Debug.trace((Object[])new Object[]{"Chong he thong am, force= " + result + ", max user win= " + maxUserWin});
        }
        return result;
    }

    public int calculateForceBalance() {
        boolean hasBlackList = this.blackListBetTai + this.blackListBetXiu > 0L;
        boolean hasWhiteList = this.whiteListBetTai + this.whiteListBetXiu > 0L;
        return this.balance.isForceBalance(hasBlackList, hasWhiteList);
    }

    public void updateTaiXiuInfo(User user) {
        SicboInfoMsg msg = new SicboInfoMsg();
        msg.gameId = (short)2;
        msg.moneyType = this.moneyType;
        msg.referenceId = this.referenceId;
        msg.remainTime = this.getRemainTime();
        msg.bettingState = this.bettingRound;
        msg.potTai = this.getPotTai();
        msg.potXiu = this.getPotXiu();
        msg.myBetTai = this.getTotalBettingTaiByUsername(user.getName());
        msg.myBetXiu = this.getTotalBettingXiuByUsername(user.getName());
        msg.jpTai = fundJpFakeTai + fundJpFakeXiu;
        msg.jpXiu = fundJpFakeXiu + fundJpFakeTai;
        if (this.resultTX != null) {
            msg.dice1 = (short)this.resultTX.dice1;
            msg.dice2 = (short)this.resultTX.dice2;
            msg.dice3 = (short)this.resultTX.dice3;
        }
        msg.betInfo = "";
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    public boolean isBetting() {
        return this.bettingRound;
    }

    public long getPotTai() {
        return this.potTai.getTotalValue();
    }

    public long getBotBetTai() {
        return this.potTai.getTotalBotBet();
    }

    public long getUserBetTai() {
        return this.potTai.getTotalValue() - this.potTai.getTotalBotBet();
    }

    public long getPotXiu() {
        return this.potXiu.getTotalValue();
    }

    public long getBotBetXiu() {
        return this.potXiu.getTotalBotBet();
    }

    public long getUserBetXiu() {
        return this.potXiu.getTotalValue() - this.potXiu.getTotalBotBet();
    }

    public long getTotalBettingTaiByUsername(String usernname) {
        return this.potTai.getTotalBetByUsername(usernname);
    }

    public long getTotalBettingXiuByUsername(String username) {
        return this.potXiu.getTotalBetByUsername(username);
    }

    public BalanceMoneyTX getBalanceTX() {
        return this.balance;
    }

    public boolean isBot(String username) {
        // SUN-1xxx (2026-05-11): null-safe — see MGRoomTaiXiu.isBot for rationale.
        try {
            UserCacheModel model = this.userService.getUser(username);
            if (model == null) return false;
            return model.isBot();
        } catch (Throwable t) {
            return false;
        }
    }

    public static short getMoneyType(int roomId) {
        return roomId == 0 ? (short)0 : 1;
    }

    public static String getKeyRoom(short moneyType) {
        return "" + moneyType + "_" + 2;
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        if (result) {
            user.setProperty("MGROOM_TAI_XIU_SICBO_INFO", this);
        }
        return result;
    }

    @Override
    public boolean quitRoom(User user) {
        boolean result = super.quitRoom(user);
        if (result) {
            user.removeProperty("MGROOM_TAI_XIU_SICBO_INFO");
        }
        return result;
    }

    private static List<String> getWinningStatuses(int[] diceValues) {
        int i;
        int[] diceCounts = new int[7];
        int[] nArray = diceValues;
        int n = nArray.length;
        for (int j = 0; j < n; ++j) {
            int value;
            int n2 = value = nArray[j];
            diceCounts[n2] = diceCounts[n2] + 1;
        }
        ArrayList<String> winningStatuses = new ArrayList<String>();
        int totalValue = Arrays.stream(diceValues).sum();
        boolean storm = false;
        for (i = 1; i <= 6; ++i) {
            if (diceCounts[i] < 3) continue;
            winningStatuses.add("TRIPLE_DICES_" + i);
            storm = true;
            winningStatuses.add("ANY_TRIPLE_DICES");
        }
        if (!storm) {
            if (totalValue >= 11 && totalValue <= 17) {
                winningStatuses.add("TAI");
            }
            if (totalValue >= 4 && totalValue <= 10) {
                winningStatuses.add("XIU");
            }
            if (totalValue % 2 == 0) {
                winningStatuses.add("CHAN");
            } else {
                winningStatuses.add("LE");
            }
            for (i = 1; i <= 6; ++i) {
                for (int j = i + 1; j <= 6; ++j) {
                    if (diceCounts[i] < 1 || diceCounts[j] < 1) continue;
                    int smaller = Math.min(i, j);
                    int bigger = Math.max(i, j);
                    winningStatuses.add("DOUBLE_DICES_" + smaller + "_" + bigger);
                }
            }
        }
        if (totalValue == 3) {
            winningStatuses.add("TRIPLE_DICES_1");
            return winningStatuses;
        }
        if (totalValue == 18) {
            winningStatuses.add("TRIPLE_DICES_6");
            return winningStatuses;
        }
        winningStatuses.add("POINT_" + totalValue);
        for (i = 1; i <= 6; ++i) {
            if (diceCounts[i] < 1) continue;
            winningStatuses.add("ONE_DICE_" + i);
        }
        return winningStatuses;
    }

    public long sotienphaitra(int dice1, int dice2, int dice3) {
        int[] diceValues = new int[]{dice1, dice2, dice3};
        List<String> listResultTemp = MGRoomSicbo.getWinningStatuses(diceValues);
        long sotien = 0L;
        for (TransactionTaiXiuDetail tx : this.listUserBet) {
            PotSicbo betTx;
            if (tx.userId <= 0 || !listResultTemp.contains((betTx = PotSicbo.getEnumById(tx.betSide)).getName())) continue;
            if (tx.betSide >= 15 && tx.betSide <= 20) {
                if (MGRoomSicbo.countOccurrences(diceValues, tx.betSide - 14) == 2) {
                    sotien += tx.betValue * 3L;
                    continue;
                }
                if (MGRoomSicbo.countOccurrences(diceValues, tx.betSide - 14) == 3) {
                    sotien += tx.betValue * 4L;
                    continue;
                }
                sotien += tx.betValue * 2L;
                continue;
            }
            sotien += tx.betValue * (long)betTx.getRotation();
        }
        return sotien;
    }

    public void reward() throws Exception {
        // SUN-EXPLOIT-GUARD (2026-05-03): if this round did not actually
        // generate dice (getResult threw, or the result-saving DB path
        // failed silently leaving listResult/diceRs from a previous round
        // in memory), refunding all bets is safer than running the prize
        // payout against stale state. The historical bug paid out players
        // hundreds of times for a single bet because listUserBet kept
        // accumulating across stale-state rounds. Quochuy98 incident
        // 2026-05-02: 24M VIN paid for a 100k bet (234x) via this path.
        if (this.diceRs == null || this.listResult == null || this.listResult.isEmpty()) {
            Debug.trace((Object[])new Object[]{"[SICBO-EXPLOIT-GUARD] reward() aborted: diceRs="
                    + (this.diceRs == null ? "null" : "ok")
                    + " listResult.size=" + (this.listResult == null ? "null" : this.listResult.size())
                    + " refunding " + this.listUserBet.size() + " bets, ref=" + this.referenceId});
            for (TransactionTaiXiuDetail tx : this.listUserBet) {
                if (tx == null || tx.userId <= 0 || tx.betValue <= 0L) continue;
                try {
                    this.userService.updateMoney(tx.username, tx.betValue, this.moneyTypeStr,
                            "SicBo", Games.TAI_XIU_SICBO.getId() + "",
                            "SICBO refund (no dice rolled) ref=" + this.referenceId,
                            0L, Long.valueOf(this.referenceId), TransType.END_TRANS);
                } catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"[SICBO-EXPLOIT-GUARD] refund failed for "
                            + tx.username + ": " + e.getMessage()});
                }
            }
            // Drain listUserBet so the next round's reward() doesn't re-process
            // these refunded bets if startNewGame is delayed.
            this.listUserBet = new ArrayList<TransactionTaiXiuDetail>();
            return;
        }
        HashMap<String, Long> totalMoneyMap = new HashMap<String, Long>();
        HashMap<String, TransactionTaiXiu> sumTXTMap = new HashMap<String, TransactionTaiXiu>();
        ResultTaiXiu rs = new ResultTaiXiu();
        for (TransactionTaiXiuDetail tx2 : this.listUserBet) {
            PotSicbo betTx = PotSicbo.getEnumById(tx2.betSide);
            if (!this.listResult.contains(betTx.getName())) continue;
            tx2.prize = tx2.betSide >= 15 && tx2.betSide <= 20 ? (MGRoomSicbo.countOccurrences(this.diceRs, tx2.betSide - 14) == 2 ? tx2.betValue * 3L : (MGRoomSicbo.countOccurrences(this.diceRs, tx2.betSide - 14) == 3 ? tx2.betValue * 4L : tx2.betValue * 2L)) : tx2.betValue * (long)betTx.getRotation();
            long fee = (long)(this.tax * (float)tx2.prize / (200.0f - this.tax));
            MoneyResponse res2 = new MoneyResponse(false, "1001");
            res2 = this.userService.updateMoney(tx2.username, tx2.prize, this.moneyTypeStr, "SicBo", Games.TAI_XIU_SICBO.getId() + "", TaiXiuDescriptionUtils.getTaiXiuWinDescription(Games.TAI_XIU_SICBO.getId() + "", this.referenceId, (byte)1), fee, Long.valueOf(this.referenceId), TransType.END_TRANS);
            try {
                this.api.updateTransactionTaiXiuDetail(tx2);
            }
            catch (Exception e) {
                Debug.trace((Object[])new Object[]{"updateTransactionTaiXiuDetail -> ", e});
            }
            this.updateSumTran(sumTXTMap, tx2);
            if (!res2.isSuccess()) continue;
            totalMoneyMap.merge(tx2.username, tx2.prize, Long::sum);
            UpdatePrizeSicboMsg msg = new UpdatePrizeSicboMsg();
            msg.Error = (byte)this.result;
            msg.moneyType = 1;
            msg.totalMoney = tx2.prize;
            msg.currentMoney = res2.getCurrentMoney();
            msg.betSide = PotSicbo.getEnumById(tx2.betSide).getName();
            this.sendMessageToUser((BaseMsg)msg, tx2.username);
        }
        rs.dice1 = (short)this.diceRs[0];
        rs.dice2 = (short)this.diceRs[1];
        rs.dice3 = (short)this.diceRs[2];
        rs.result = 1;
        rs.referenceId = this.referenceId;
        rs.moneyType = 1;
        rs.totalPrize = sumTXTMap.values().stream().mapToLong(tx -> tx.totalPrize).sum();
        try {
            this.api.saveResultTaiXiu(rs);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"saveResultTaiXiu -> ", e});
        }
        ArrayList trans = new ArrayList(sumTXTMap.values());
        try {
            this.api.saveTransactionTaiXiu(trans);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"saveTransactionTaiXiu -> ", e});
        }
        List<UserTotalMoney> userTotalMoneyList = totalMoneyMap.entrySet().stream().map(entry -> new UserTotalMoney((String)entry.getKey(), (Long)entry.getValue())).collect(Collectors.toList());
        for (UserTotalMoney userTotalMoney : userTotalMoneyList) {
            UpdateFinalSicboMsg msg = new UpdateFinalSicboMsg();
            msg.Error = (byte)this.result;
            msg.totalMoney = userTotalMoney.totalMoney;
            this.sendMessageToUser((BaseMsg)msg, userTotalMoney.userName);
        }
    }

    private static int countOccurrences(int[] array, int targetValue) {
        int count = 0;
        for (int value : array) {
            if (value != targetValue) continue;
            ++count;
        }
        return count;
    }

    private void updateGameInfoToCache() {
        try {
            short timeEnd = this.getRemainTime();
            this.cacheService.setObject("sicbo_countTime", timeEnd);
            this.cacheService.setObject("sicbo_isBetting", this.bettingRound);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"Sicbo exception: " + e.getMessage() + ", function: updateGameInfoToCache() " + this.referenceId});
        }
    }

    class UserTotalMoney {
        public String userName;
        public long totalMoney;

        public UserTotalMoney(String userName, long totalMoney) {
            this.userName = userName;
            this.totalMoney = totalMoney;
        }
    }

    private final class UpdateMoneyTXTask
    extends Thread {
        private Map<String, TransactionTaiXiu> trans = new HashMap<String, TransactionTaiXiu>();

        private UpdateMoneyTXTask(Map<String, TransactionTaiXiu> trans) {
            this.trans = trans;
        }

        @Override
        public void run() {
            boolean isJp = false;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            String timeJackpot = simpleDateFormat.format(System.currentTimeMillis());
            ArrayList<JackpotDetail> jackpotDetails = new ArrayList<JackpotDetail>();
            for (Map.Entry<String, TransactionTaiXiu> entry : this.trans.entrySet()) {
                try {
                    String username = entry.getKey();
                    TransactionTaiXiu txt = entry.getValue();
                    // SUN-748 regression fix: use vin balance, not vin_total (cumulative
                    // P&L) — see MGRoomTaiXiu.java:1175 for full rationale.
                    long currentMoney = MGRoomSicbo.this.userService.getMoneyUserCache(username, MGRoomSicbo.this.moneyTypeStr);
                    if (!MGRoomSicbo.this.isBot(username)) {
                        Debug.trace((Object[])new Object[]{"TEST HU username: " + username + " totalPrize: " + txt.totalPrize + "  totalRefund: " + txt.totalRefund + " txt.totalJp: " + txt.totalJp});
                    }
                    if (txt.totalPrize == 0L && txt.totalRefund == 0L) {
                        MGRoomSicbo.this.userService.updateMoney(username, 0L, MGRoomSicbo.this.moneyTypeStr, "TaiXiu", "", "", 0L, Long.valueOf(MGRoomSicbo.this.referenceId), TransType.END_TRANS);
                    } else {
                        MoneyResponse res;
                        if (txt.totalPrize > 0L) {
                            if (txt.totalJp > 0L) {
                                isJp = true;
                                if (!MGRoomSicbo.this.isBot(username) && (res = MGRoomSicbo.this.userService.updateMoney(username, txt.totalJp, MGRoomSicbo.this.moneyTypeStr, "TaiXiu", Games.TAI_XIU_SICBO.getId() + "", TaiXiuDescriptionUtils.getTaiXiuWinDescription(Games.TAI_XIU_SICBO.getId() + "", MGRoomSicbo.this.referenceId, (byte)3), 0L, Long.valueOf(MGRoomSicbo.this.referenceId), TransType.IN_TRANS)).isSuccess()) {
                                    if (MGRoomSicbo.this.moneyType == 1 && !MGRoomSicbo.this.isBot(username)) {
                                        MGRoomSicbo.this.balance.addWin(txt.totalJp);
                                    }
                                    currentMoney = res.getCurrentMoney();
                                    if (MGRoomSicbo.this.moneyType == 1) {
                                        MGRoomSicbo.this.broadcastMsgService.putMessage(Games.TAI_XIU_SICBO.getId(), username, txt.totalJp);
                                    }
                                }
                                jackpotDetails.add(new JackpotDetail(username, txt.totalJp));
                                MGRoomSicbo.this.insertUserJackpotDetailToDb(timeJackpot, String.valueOf(this.trans.size()), String.valueOf(fundJpAll), username, txt.totalJp);
                            }
                            TransType transType = TransType.END_TRANS;
                            if (txt.totalRefund > 0L) {
                                transType = TransType.IN_TRANS;
                            }
                            long fee = (long)(MGRoomSicbo.this.tax * (float)txt.totalPrize / (200.0f - MGRoomSicbo.this.tax));
                            MoneyResponse res2 = new MoneyResponse(false, "1001");
                            if (!MGRoomSicbo.this.isBot(username)) {
                                res2 = MGRoomSicbo.this.userService.updateMoney(username, txt.totalPrize, MGRoomSicbo.this.moneyTypeStr, "TaiXiu", Games.TAI_XIU_SICBO.getId() + "", TaiXiuDescriptionUtils.getTaiXiuWinDescription(Games.TAI_XIU_SICBO.getId() + "", MGRoomSicbo.this.referenceId, (byte)1), fee, Long.valueOf(MGRoomSicbo.this.referenceId), transType);
                            } else {
                                res2.setSuccess(true);
                            }
                            if (res2.isSuccess()) {
                                if (MGRoomSicbo.this.moneyType == 1 && !MGRoomSicbo.this.isBot(username)) {
                                    MGRoomSicbo.this.balance.addWin(txt.totalPrize);
                                    MGRoomSicbo.this.balance.addFee(fee);
                                }
                                currentMoney = res2.getCurrentMoney();
                                long totalExchange = (long)((float)txt.totalPrize * (100.0f - MGRoomSicbo.this.tax) / (200.0f - MGRoomSicbo.this.tax));
                                if (MGRoomSicbo.this.moneyType == 1 && totalExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                    MGRoomSicbo.this.broadcastMsgService.putMessage(Games.TAI_XIU_SICBO.getId(), username, totalExchange);
                                }
                            }
                        }
                        if (txt.totalRefund > 0L && !MGRoomSicbo.this.isBot(username) && (res = MGRoomSicbo.this.userService.updateMoney(username, txt.totalRefund, MGRoomSicbo.this.moneyTypeStr, "TaiXiu", Games.TAI_XIU_SICBO.getId() + "", TaiXiuDescriptionUtils.getTaiXiuRefundDescription(Games.TAI_XIU_SICBO.getId() + "", MGRoomSicbo.this.referenceId), 0L, Long.valueOf(MGRoomSicbo.this.referenceId), TransType.END_TRANS)).isSuccess()) {
                            if (MGRoomSicbo.this.moneyType == 1 && !MGRoomSicbo.this.isBot(username)) {
                                MGRoomSicbo.this.balance.addWin(txt.totalRefund);
                            }
                            currentMoney = res.getCurrentMoney();
                        }
                    }
                    UpdatePrizeTaiXiuMsg msg = new UpdatePrizeTaiXiuMsg();
                    msg.moneyType = MGRoomSicbo.this.moneyType;
                    msg.totalMoney = txt.totalPrize + txt.totalRefund + txt.totalJp;
                    msg.currentMoney = currentMoney;
                    MGRoomSicbo.this.sendMessageToUser((BaseMsg)msg, username);
                }
                catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"Update tai xiu money phien " + MGRoomSicbo.this.referenceId + " error: " + e.getMessage()});
                }
            }
            if (jackpotDetails.size() > 0) {
                Collections.sort(jackpotDetails, (o1, o2) -> {
                    if (o1.getMoney() == o2.getMoney()) {
                        return 0;
                    }
                    if (o1.getMoney() < o2.getMoney()) {
                        return 1;
                    }
                    return -1;
                });
                String data = "";
                int max = Math.min(jackpotDetails.size(), 20);
                for (int i = 0; i < max; ++i) {
                    data = i == 2 ? data + ((JackpotDetail)jackpotDetails.get(i)).getUserName() + "|" + ((JackpotDetail)jackpotDetails.get(i)).getMoney() : data + ((JackpotDetail)jackpotDetails.get(i)).getUserName() + "|" + ((JackpotDetail)jackpotDetails.get(i)).getMoney() + ",";
                }
                MGRoomSicbo.this.insertUserJackpotToDb(timeJackpot, String.valueOf(this.trans.size()), String.valueOf(fundJpAll), data);
            }
            if (isJp) {
                MGRoomSicbo.this.initJackpot();
            }
        }
    }

    private class JackpotDetail {
        private String userName;
        private long money;

        public JackpotDetail(String userName, long money) {
            this.userName = userName;
            this.money = money;
        }

        public String getUserName() {
            return this.userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public long getMoney() {
            return this.money;
        }

        public void setMoney(long money) {
            this.money = money;
        }
    }

    /**
     * SUN-1373 — returns {@code true} when Sicbo is administratively disabled.
     * Reads {@code SICBO_GAME_INACTIVE=1} from the environment.
     * Follows the same env-var kill-switch convention as other game-control flags.
     */
    private static boolean isSicboDisabled() {
        String v = System.getenv("SICBO_GAME_INACTIVE");
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }
}
