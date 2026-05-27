/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.common.business.Debug
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.Updates
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package game.modules.minigame.room;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.common.business.Debug;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuServiceImpl;
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
import game.modules.minigame.cmd.rev.BetTaiXiuCmd;
import game.modules.minigame.cmd.send.BetTaiXiuMsg;
import game.modules.minigame.cmd.send.TaiXiuInfoMsg;
import game.modules.minigame.cmd.send.TaiXiuJackpotMsg;
import game.modules.minigame.cmd.send.TaiXiuRefundMsg;
import game.modules.minigame.cmd.send.UpdatePrizeTaiXiuMsg;
import game.modules.minigame.cmd.send.UpdateResultDicesMsg;
import game.modules.minigame.cmd.send.UpdateTaiXiuPerSecondMsg;
import game.modules.minigame.entities.BalanceMoneyTX;
import game.modules.minigame.entities.MinigameConstant;
import game.modules.minigame.entities.PotTaiXiu;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.utils.GenerationTaiXiu;
import game.modules.minigame.utils.TaiXiuUtils;
import game.utils.ConfigGame;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MGRoomTaiXiu
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
    private TaiXiuServiceImpl api = new TaiXiuServiceImpl();
    private UserService userService = new UserServiceImpl();
    private CacheService cacheService = new CacheServiceImpl();
    private BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    private float tax = MinigameConstant.MINIGAME_TAX_TX;
    private float taxJp = MinigameConstant.MINIGAME_TAX_TX_JACKPOT;
    private BalanceMoneyTX balance = new BalanceMoneyTX();
    private long blackListBetTai = 0L;
    private long blackListBetXiu = 0L;
    private long whiteListBetTai = 0L;
    private long whiteListBetXiu = 0L;
    private boolean flagJpTai = false;
    private boolean flagJpXiu = false;
    private boolean flagJp = false;
    private static boolean isJpTai = false;
    private static boolean isJpXiu = false;
    private long fundTaiXiu = 0L;
    private long jackpotAccumulate = 0L;
    private static long fundJpAll = 0L;
    private long minMoneyJp = 10000000L;
    private static AtomicInteger winCount = new AtomicInteger(0);
    private static AtomicInteger countJpTai = new AtomicInteger(0);
    private static AtomicInteger countJpXiu = new AtomicInteger(0);
    private MiniGameService mgService = new MiniGameServiceImpl();
    public long realPotTai = 0L;
    public long realPotXiu = 0L;
    public short realNumBetTai = 0;
    public short realNumBetXiu = 0;
    private boolean balanceGate = true;
    private boolean resetJp = false;

    public MGRoomTaiXiu(String name, long referenceId, short moneyType, long fundTaiXiu, long fundJpTais, long fundJpXius, long jpFkTais, long jpFkXius) {
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
        this.balanceGate = ConfigGame.getIntValue("balance_gate", 0) == 1;
        this.fundTaiXiu = fundTaiXiu;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            db.createCollection("jackpot_tx");
        }
        catch (Exception e) {
            System.out.println("Collection is already created");
        }
        this.jackpotAccumulate = Long.parseLong(this.getJpValue().trim());
        if (this.jackpotAccumulate < 50000000L) {
            this.jackpotAccumulate = 50000000L;
        }
    }

    public void startNewGame(long newReferenceId) {
        // SUN-1246: clear the previous round's resultTX immediately on round
        // start. updateTaiXiuInfo() copies dice values from resultTX into the
        // outbound TaiXiuInfoMsg; without this reset, a player who reconnects
        // or re-fetches game info during the betting phase of round N+1 sees
        // the dice values from round N (sum can land on 18 if the previous
        // round was a [6,6,6] jackpot, which is the case the QC video
        // captured). finish() clears resultTX too, but that's at the very end
        // of the round — the gap between startNewGame and finish() is the
        // entire betting phase, so a join during that window leaks stale
        // data. Reset at the earliest possible moment.
        this.resultTX = null;
        if (this.resetJp) {
            this.resetJp = false;
            this.jackpotAccumulate = 50000000L;
        }
        this.referenceId = newReferenceId;
        this.bettingRound = true;
        this.enableBetting = true;
        this.blackListBetTai = 0L;
        this.blackListBetXiu = 0L;
        this.whiteListBetTai = 0L;
        this.whiteListBetXiu = 0L;
        this.realPotTai = 0L;
        this.realPotXiu = 0L;
        this.realNumBetTai = 0;
        this.realNumBetXiu = 0;
        this.potTai.renew();
        this.potXiu.renew();
        this.startTime = System.currentTimeMillis();
        Debug.trace((Object[])new Object[]{"START NEW ROUND " + this.referenceId});
        this.clearUserBetToDb();
        if (this.moneyType == 1) {
            this.updateJpValue(String.valueOf(this.jackpotAccumulate));
        }
    }

    public void finish() {
        this.resultTX = null;
        // 2026-05-08: do NOT reset startTime here. The SUN-1245 time-based
        // getRemainTime() relies on startTime being the round-start instant
        // so that revealStart = startTime + bettingTime*1000L points at the
        // real finish moment. Resetting startTime made the bet-phase branch
        // re-trigger after finish, returning 50→32 with bettingState=false.
        // The client's `remainTime > 15 ? remainTime - 15 : remainTime`
        // rule then displayed 35→17 ("countdown starts from 34" symptom).
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

    public void disableBetting() {
        this.enableBetting = false;
        this.cacheService.setValue("allow_betting_" + this.referenceId, 0);
    }

    public void updateResultDices(short[] dices, short result) {
        this.result = result;
        UpdateResultDicesMsg msg = new UpdateResultDicesMsg();
        msg.result = result;
        msg.dice1 = dices[0];
        msg.dice2 = dices[1];
        msg.dice3 = dices[2];
        msg.jackpot = this.resetJp;
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
        // SUN-769 (re-applied 2026-04-29) fixed the reveal-gap stuck-at-0
        // by introducing the `revealStart`-based formula below. But it
        // branched on `enableBetting`, which flips at count=45 — five
        // seconds *before* the reveal phase actually starts at count=50
        // (per TaiXiuModule.gameLoop: count=45 → disableBetting,
        // count=50 → finish, count=51 → generateTaiXiuDices).
        //
        // SUN-1245: during those 5 seconds, getRemainTime() entered the
        // reveal-phase branch but `revealStart` was still in the future,
        // so `currentTime - revealStart` was negative → clamped to 0 →
        // formula returned `resultTime - 0 = 33`. The countdown froze on
        // 33 for the last 5s of the betting phase. Players reported
        // "luôn hiển thị số 33" (always shows 33).
        //
        // Fix: branch on the actual reveal-start instant, not on the
        // betting-state flag. Pure time math — no flag races.
        // 2026-05-08: resultTime trimmed 33 → 18 to match the actual reveal
        // window (gameLoop: count=50 finish → count=68 startNewRound = 18s).
        // Old value caused the displayed countdown to snap from 16 → 50 mid-
        // round (the "2 countdowns" symptom). With 18s, the result phase
        // monotonically reaches 0 just as the new round starts.
        final int bettingTime = 50;
        final int resultTime = 18;
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

    public void betTaiXiu(User user, BetTaiXiuCmd cmd) {
        boolean isUBot = this.isUserBot(user.getName());
        if (isUBot) {
            BetTaiXiuMsg msg = this.betTaiXiu(user.getName(), 0, cmd.betValue, cmd.inputTime, cmd.moneyType, cmd.betSide, true);
            this.sendMessageToUser((BaseMsg)msg, user);
        } else {
            BetTaiXiuMsg msg = this.betTaiXiu(user.getName(), cmd.userId, cmd.betValue, cmd.inputTime, cmd.moneyType, cmd.betSide, false);
            this.sendMessageToUser((BaseMsg)msg, user);
        }
        long fee = (long)((double)cmd.betValue * 0.01);
    }

    public void insertUserBetToDb(String nickname, long betValue, int inputTime, int betSide, long balance) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_bet_tai_xiu");
        Document doc = new Document();
        doc.append("referentId", this.referenceId);
        doc.append("nick_name", nickname);
        doc.append("inputTime", inputTime);
        doc.append("betSide", betSide);
        doc.append("betValue", betValue);
        doc.append("balance", balance);
        doc.append("money_type", this.moneyType == 1 ? 1 : 2);
        col.insertOne(doc);
    }

    public void insertUserJackpotDetailToDb(String time, String countBet, String moneyJackpotAll, String nickName, long money) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("user_jackpot_tai_xiu_details");
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
        MongoCollection col = db.getCollection("user_jackpot_tai_xiu");
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
        // GitLab infra issue #1: same as MGRoomSicbo — wrap in retry and
        // swallow on exhaustion so a stale Mongo pool does not freeze the
        // Tài Xỉu round loop.
        try {
            com.vinplay.vbee.common.mongodb.MongoRetryHelper.run(() -> {
                MongoDatabase db = MongoDBConnectionFactory.getDB();
                MongoCollection col = db.getCollection("user_bet_tai_xiu");
                col.drop();
            }, "taixiu.clearUserBetToDb");
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger("api")
                    .warn("MGRoomTaiXiu.clearUserBetToDb: swallowed after retries, will retry next round — "
                            + e.getMessage());
        }
    }

    private boolean isUserBot(String nickName) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMap = client.getMap("usersSetWin");
        return userMap.containsKey(nickName) && (Boolean)userMap.get(nickName) != false;
    }

    public BetTaiXiuMsg betTaiXiu(String nickname, int userId, long betValue, short inputTime, short moneyType, short betSide, boolean isBot) {
        // SUN-1373 — reject bet when TaiXiu is administratively disabled.
        // Result code 11 = GAME_INACTIVE (distinct from all existing codes:
        // 0=success, 1=betting-window-closed, 2=not-in-room, 3=insufficient,
        // 4=bet-too-small, 5=duplicate-side). Bots are not blocked — only
        // real players trigger this gate.
        if (!isBot && isTaiXiuDisabled()) {
            BetTaiXiuMsg inactiveMsg = new BetTaiXiuMsg();
            inactiveMsg.Error = (byte)11; // GAME_INACTIVE
            inactiveMsg.currentMoney = this.userService.getMoneyUserCache(nickname, this.moneyTypeStr);
            return inactiveMsg;
        }
        boolean isLivestream = this.isUserBot(nickname);
        if (!isBot) {
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
        if (this.enableBetting) {
            if (betValue >= 100L) {
                if (betValue > currentMoney) {
                    result = 3;
                } else {
                    TransactionTaiXiuDetail transTX = new TransactionTaiXiuDetail(this.referenceId, userId, nickname, betValue, (int)betSide, (int)inputTime, (int)moneyType);
                    transTX.currentMoney = currentMoney; // capture user balance at bet time
                    if (betSide == 1 && this.potXiu.getTotalBetByUsername(nickname) > 0L || betSide == 0 && this.potTai.getTotalBetByUsername(nickname) > 0L) {
                        result = 5;
                    } else {
                        MoneyResponse res = new MoneyResponse(false, "1001");
                        // SUN-1290: per-bet unique transId so multiple same-side
                        // bets in one round don't collide on the
                        // (tx_id, source, user_id) dedup key in
                        // money_gateway_log. Before this, the second bet by the
                        // same user in the same round returned 1031 "Duplicate
                        // transaction" because every bet shared
                        // transId=referenceId. The complementary HOANTIEN refund
                        // below reuses this same per-bet id so the rollback is
                        // correlated to its bet, not to the round as a whole.
                        // Encoding: roundId * 1e6 + (nanoTime mod 1e6) keeps the
                        // round visible in audit while guaranteeing uniqueness
                        // within a JVM (nanoTime monotonic per process).
                        long perBetTxId = this.referenceId * 1000000L
                                + (System.nanoTime() & 0xFFFFFL);
                        if (!isBot || isLivestream) {
                            res = this.userService.updateMoney(nickname, -betValue, this.moneyTypeStr, "TaiXiu", Games.TAI_XIU.getId() + "", TaiXiuDescriptionUtils.getTaiXiuBetDescription(Games.TAI_XIU.getId() + "", this.referenceId, inputTime + "", betSide), 0L, Long.valueOf(perBetTxId), TransType.START_TRANS);
                        } else {
                            res.setSuccess(true);
                        }
                        if (res.isSuccess()) {
                            if (!this.enableBetting) {
                                result = 1;
                                if (!isBot || isLivestream) {
                                    this.userService.updateMoney(nickname, betValue, this.moneyTypeStr, "TaiXiuHoanTien", Games.TAI_XIU.getId() + "", TaiXiuDescriptionUtils.getTaiXiuTraCuocDescription(Games.TAI_XIU.getId() + "", this.referenceId), 0L, Long.valueOf(perBetTxId), TransType.END_TRANS);
                                }
                            } else {
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
                            }
                            if (!isBot) {
                                try {
                                    this.insertUserBetToDb(nickname, betValue, inputTime, betSide, currentMoney);
                                }
                                catch (Exception exception) {
                                    // SUN-1xxx (2026-05-11): no more silent swallow.
                                    // We lost ~9h of bet history to this empty catch.
                                    Debug.trace((Object[])new Object[]{
                                        "MGRoomTaiXiu.insertUserBetToDb FAILED — bet history NOT logged for user="
                                        + nickname + " ref=" + this.referenceId + " betValue=" + betValue
                                        + " betSide=" + betSide + " err=" + exception.getMessage()});
                                }
                            }
                        } else {
                            result = 1;
                        }
                    }
                }
            } else {
                result = 4;
            }
        }
        BetTaiXiuMsg msg = new BetTaiXiuMsg();
        msg.Error = (byte)result;
        msg.currentMoney = currentMoney;
        return msg;
    }

    // SUN-807: per-round stable virtual-player pad. See MGRoomSicbo for
    // the full rationale. Each side draws independently in [min, max] once
    // per round (keyed on referenceId); collisions get a ±1 jitter so
    // counts on the two sides are never equal.
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

    public void updateTaiXiuPerSecond() {
        UpdateTaiXiuPerSecondMsg msg = new UpdateTaiXiuPerSecondMsg();
        msg.remainTime = this.getRemainTime();
        msg.bettingState = this.bettingRound;
        msg.potTai = this.getPotTai();
        msg.potXiu = this.getPotXiu();
        refreshPadIfNeeded();
        int padTai = this.bettingRound ? this.cachedPadTai : 0;
        int padXiu = this.bettingRound ? this.cachedPadXiu : 0;
        msg.numBetTai = (short)(this.potTai.getNumBet() + padTai);
        msg.numBetXiu = (short)(this.potXiu.getNumBet() + padXiu);
        msg.fundJpTai = this.jackpotAccumulate;
        msg.fundJpXiu = this.jackpotAccumulate;
        msg.referenceId = this.referenceId;
        msg.realNumBetTai = this.realNumBetTai;
        msg.realNumBetXiu = this.realNumBetXiu;
        msg.realPotXiu = this.jackpotAccumulate;
        msg.realPotTai = this.jackpotAccumulate;
        this.sendMessageToRoom(msg);
    }

    public short[] getResult(long id) {
        PotTaiXiu potX = this.potXiu;
        PotTaiXiu potT = this.potTai;
        long tongTienHopLe = potT == null || potX == null ? 0L : Math.min(potT.getTotalValue(), potX.getTotalValue());
        long sumMoneyCurrent = 0L;
        long moneyUserBet = 0L;
        PotTaiXiu pot = potT.getTotalValue() < potX.getTotalValue() ? potT : potX;
        long userOppositeBet = potT.getTotalValue() > potX.getTotalValue() ? this.getUserBetXiu() : this.getUserBetTai();
        this.jackpotAccumulate = (long)((double)this.jackpotAccumulate + (double)pot.getTotalValue() * 0.006);
        for (TransactionTaiXiuDetail tran : pot.contributors) {
            long tienDuocTinh = tran.betValue;
            if (sumMoneyCurrent + tran.betValue > tongTienHopLe) {
                tienDuocTinh = tongTienHopLe - sumMoneyCurrent;
            }
            sumMoneyCurrent += tienDuocTinh;
            if (tran.userId <= 0) continue;
            moneyUserBet += tienDuocTinh;
        }
        long moneyBetXiu = 0L;
        long moneyBetTai = 0L;
        if (potT.getTotalValue() > potX.getTotalValue()) {
            moneyBetXiu = userOppositeBet;
            moneyBetTai = moneyUserBet;
        } else {
            moneyBetXiu = moneyUserBet;
            moneyBetTai = userOppositeBet;
        }
        short[] result = new short[]{};
        int Xtimes = 3;
        short[] dataCache = this.api.suaKetQuaTaiXiu();
        if (dataCache != null) {
            // Admin force result (SetForceSlotResult) — luôn ưu tiên tuyệt đối
            result = dataCache;
        } else {
            // Dynamic house edge: tính forceBetSide tối ưu dựa trên target RTP từ CMS
            // realPotTai/realPotXiu chỉ tính user thật (không tính bot)
            GenerationTaiXiu gen = new GenerationTaiXiu();
            result = gen.generateResultWithHouseEdge(
                    GenerationTaiXiu.GAME_ID_TAIXIU,
                    this.realPotTai,
                    this.realPotXiu,
                    this.tax
            );
        }
        short checkJackpot = this.api.checkJackpotTaiXiu();
        if (checkJackpot == 6) {
            if (potT.getNumBet() % 5 != 0) {
                checkJackpot = 0;
            }
        } else if (checkJackpot == 1 && potX.getNumBet() % 5 != 0) {
            checkJackpot = 0;
        }
        if (checkJackpot != 0) {
            this.resetJp = true;
            result = new short[]{checkJackpot, checkJackpot, checkJackpot};
            if (checkJackpot == 6) {
                this.flagJp = true;
                isJpXiu = false;
                isJpTai = true;
            }
            if (checkJackpot == 1) {
                this.flagJp = true;
                isJpTai = false;
                isJpXiu = true;
            }
        }
        int totalDice = result[0] + result[1] + result[2];
        this.saveFund(totalDice);
        Debug.trace((Object[])new Object[]{"TAIXIUDEBUG Result End:" + result[0] + " " + result[1] + " " + result[2]});
        this.updateResultDices(result, (short)(!TaiXiuUtil.isXiu(result) ? 1 : 0));
        return result;
    }

    private void sendNotifyJp(long id, long amount, short typeJp, String username) {
        TaiXiuJackpotMsg msg = new TaiXiuJackpotMsg();
        msg.amount = amount;
        msg.id = id;
        msg.typeJP = typeJp;
        this.sendMessageToUser((BaseMsg)msg, username);
    }

    public void saveFund(int result) {
        String keyBot = this.moneyType == 1 ? "TaiXiu_Fund_vin" : "TaiXiu_Fund_xu";
        try {
            this.mgService.saveFund(keyBot, this.fundTaiXiu);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            if (isJpXiu || isJpTai) {
                fundJpAll = this.jackpotAccumulate;
            }
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
        if (this.balanceGate) {
            return;
        }
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
                    // portion from taixiu column → rolling (cược hợp lệ)
                    // no longer counts cân-cửa bets.
                    game.modules.minigame.utils.TaiXiuUtils.publishLogReportCuaRefund(
                            this.referenceId, tran.userId, tran.username, refund,
                            this.moneyTypeStr, Games.TAI_XIU.getId(),
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
                            this.moneyTypeStr, Games.TAI_XIU.getId(),
                            this.isBot(tran.username));
                }
                catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"Error calculate prize user2 " + tran.username + " error: " + e.getMessage()});
                }
            }
        }
    }

    public void calculateJackpot() {
        Debug.trace((Object[])new Object[]{"calculateJackpot  "});
        PotTaiXiu potX = this.potXiu;
        PotTaiXiu potT = this.potTai;
        long tongTienHopLe = potT == null || potX == null ? 0L : (potT.getTotalValue() > potX.getTotalValue() ? potX.getTotalValue() : potT.getTotalValue());
        long tongTienTaiDaTinh = 0L;
        long tongTienXiuDaTinh = 0L;
        HashMap<String, TransactionTaiXiu> sumRs = new HashMap<String, TransactionTaiXiu>();
        if (potT != null && potT.contributors != null) {
            for (TransactionTaiXiuDetail tran : potT.contributors) {
                try {
                    long tienDuocTinh = tran.betValue;
                    if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
                        tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
                    }
                    tongTienTaiDaTinh += tienDuocTinh;
                    tran.jpAmount = 0L;
                    this.updateSumTran(sumRs, tran);
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
                    tran.jpAmount = 0L;
                    this.updateSumTran(sumRs, tran);
                }
                catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"Error calculate prize user2 " + tran.username + " error: " + e.getMessage()});
                }
            }
        }
        if (this.resetJp) {
            if (isJpTai && tongTienTaiDaTinh > 0L && potT != null && potT.contributors != null) {
                long denominator = tongTienTaiDaTinh;
                long tongTienTaiDaTinhLap = 0L;
                for (TransactionTaiXiuDetail tran : potT.contributors) {
                    try {
                        long tienDuocTinh = tran.betValue;
                        if (tongTienTaiDaTinhLap + tran.betValue > tongTienHopLe) {
                            tienDuocTinh = tongTienHopLe - tongTienTaiDaTinhLap;
                        }
                        tongTienTaiDaTinhLap += tienDuocTinh;
                        tran.jpAmount = tienDuocTinh * this.jackpotAccumulate / denominator;
                        if (!this.isBot(tran.username)) {
                            Debug.trace((Object[])new Object[]{"TEST HU username: " + tran.username + " tienDuocTinh: " + tienDuocTinh + "  totalJackPot: " + this.jackpotAccumulate + " TongTien b\u00ean T\u00e0i: " + denominator});
                        }
                        this.updateSumTran(sumRs, tran);
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{"Error calculate prize user1 " + tran.username + " error: " + e.getMessage()});
                    }
                }
            } else if (isJpXiu && tongTienXiuDaTinh > 0L && potX != null && potX.contributors != null) {
                long denominator = tongTienXiuDaTinh;
                long tongTienXiuDaTinhLap = 0L;
                for (TransactionTaiXiuDetail tran : potX.contributors) {
                    try {
                        long tienDuocTinh = tran.betValue;
                        if (tongTienXiuDaTinhLap + tran.betValue > tongTienHopLe) {
                            tienDuocTinh = tongTienHopLe - tongTienXiuDaTinhLap;
                        }
                        tongTienXiuDaTinhLap += tienDuocTinh;
                        tran.jpAmount = tienDuocTinh * this.jackpotAccumulate / denominator;
                        if (!this.isBot(tran.username)) {
                            Debug.trace((Object[])new Object[]{"TEST HU username: " + tran.username + " tienDuocTinh: " + tienDuocTinh + "  totalJackPot: " + this.jackpotAccumulate + " TongTien b\u00ean X\u1ec9u: " + denominator});
                        }
                        this.updateSumTran(sumRs, tran);
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{"Error calculate prize user2 " + tran.username + " error: " + e.getMessage()});
                    }
                }
            }
        }
        if (this.resetJp) {
            for (TransactionTaiXiu tran : sumRs.values()) {
                if (tran.totalJp > 0L) {
                    if (this.isBot(tran.username)) continue;
                    Debug.trace((Object[])new Object[]{"TEST HU username: " + tran.username + " jpAmount: " + tran.totalJp});
                    this.sendNotifyJp(this.referenceId, tran.totalJp, (short)1, tran.username);
                    continue;
                }
                if (this.isBot(tran.username)) continue;
                Debug.trace((Object[])new Object[]{"TEST HU username: " + tran.username + " jpAmount: " + tran.totalJp});
                this.sendNotifyJp(this.referenceId, 0L, (short)0, tran.username);
            }
        }
    }

    public void calculatePrize(long referenceId) {
        PotTaiXiu potX = this.potXiu;
        PotTaiXiu potT = this.potTai;
        HashMap<String, TransactionTaiXiu> sumTXTMap = new HashMap<String, TransactionTaiXiu>();
        HashMap<String, TransactionTaiXiu> sumTai = new HashMap<String, TransactionTaiXiu>();
        HashMap<String, TransactionTaiXiu> sumXiu = new HashMap<String, TransactionTaiXiu>();
        long tongTienHopLe = potT == null || potX == null ? 0L : (potT.getTotalValue() > potX.getTotalValue() ? potX.getTotalValue() : potT.getTotalValue());
        long tongTienTaiDaTinh = 0L;
        long tongTienXiuDaTinh = 0L;
        long totalForCalculateJp = 0L;
        if (isJpTai || isJpXiu) {
            totalForCalculateJp = this.result == 0 ? potX.getTotalValue() - potX.getTotalBotBet() : potT.getTotalValue() - potT.getTotalBotBet();
        }
        long totalCashIn = 0L;
        long totalCashOut = 0L;
        ResultTaiXiu rs = new ResultTaiXiu();
        try {
            if (this.resultTX != null) {
                rs = this.resultTX;
                Debug.trace((Object[])new Object[]{"resultTX " + this.resultTX});
            } else {
                Debug.trace((Object[])new Object[]{" error: " + this.resultTX});
            }
        }
        catch (Exception ex) {
            Debug.trace((Object[])new Object[]{" error resultTX: " + ex.getMessage()});
        }
        switch (this.result) {
            case 0: {
                if (potX != null && potX.contributors != null) {
                    for (TransactionTaiXiuDetail tran : potX.contributors) {
                        try {
                            long tienDuocTinh = tran.betValue;
                            if (tongTienXiuDaTinh + tran.betValue > tongTienHopLe) {
                                tienDuocTinh = tongTienHopLe - tongTienXiuDaTinh;
                            }
                            if (this.balanceGate) {
                                tienDuocTinh = tran.betValue;
                            }
                            tongTienXiuDaTinh += tienDuocTinh;
                            tran.prize = (long)((float)tienDuocTinh * (100.0f - this.tax) / 100.0f) + tienDuocTinh;
                            rs.totalPrize += tran.prize;
                            tran.refund = tran.betValue - tienDuocTinh;
                            rs.totalRefundXiu += tran.refund;
                            this.updateSumTran(sumTXTMap, tran);
                            this.updateSumTran(sumXiu, tran);
                            this.saveTransactionDetailTX(tran);
                        }
                        catch (Exception e) {
                            Debug.trace((Object[])new Object[]{"Error calculate prize user9 " + tran.username + " error: " + e.getMessage()});
                        }
                    }
                }
                if (potT == null || potT.contributors == null) break;
                for (TransactionTaiXiuDetail tran : potT.contributors) {
                    try {
                        long tienDuocTinh = tran.betValue;
                        if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
                            tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
                        }
                        if (this.balanceGate) {
                            tienDuocTinh = tran.betValue;
                        }
                        tongTienTaiDaTinh += tienDuocTinh;
                        tran.refund = tran.betValue - tienDuocTinh;
                        rs.totalRefundTai += tran.refund;
                        this.updateSumTran(sumTXTMap, tran);
                        this.updateSumTran(sumTai, tran);
                        this.saveTransactionDetailTX(tran);
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{"Error calculate prize user8 " + tran.username + " error: " + e.getMessage()});
                    }
                }
                break;
            }
            case 1: {
                if (potT != null && potT.contributors != null) {
                    for (TransactionTaiXiuDetail tran : potT.contributors) {
                        try {
                            long tienDuocTinh = tran.betValue;
                            if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
                                tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
                            }
                            if (this.balanceGate) {
                                tienDuocTinh = tran.betValue;
                            }
                            tongTienTaiDaTinh += tienDuocTinh;
                            tran.prize = (long)((float)tienDuocTinh * (100.0f - this.tax) / 100.0f) + tienDuocTinh;
                            rs.totalPrize += tran.prize;
                            tran.refund = tran.betValue - tienDuocTinh;
                            rs.totalRefundTai += tran.refund;
                            this.updateSumTran(sumTXTMap, tran);
                            this.updateSumTran(sumTai, tran);
                            this.saveTransactionDetailTX(tran);
                        }
                        catch (Exception e) {
                            Debug.trace((Object[])new Object[]{"Error calculate prize user7 " + tran.username + " error: " + e.getMessage()});
                        }
                    }
                }
                if (potX == null || potX.contributors == null) break;
                for (TransactionTaiXiuDetail tran : potX.contributors) {
                    try {
                        long tienDuocTinh = tran.betValue;
                        if (tongTienXiuDaTinh + tran.betValue > tongTienHopLe) {
                            tienDuocTinh = tongTienHopLe - tongTienXiuDaTinh;
                        }
                        if (this.balanceGate) {
                            tienDuocTinh = tran.betValue;
                        }
                        tongTienXiuDaTinh += tienDuocTinh;
                        tran.refund = tran.betValue - tienDuocTinh;
                        rs.totalRefundXiu += tran.refund;
                        this.updateSumTran(sumTXTMap, tran);
                        this.updateSumTran(sumXiu, tran);
                        this.saveTransactionDetailTX(tran);
                    }
                    catch (Exception e) {
                        Debug.trace((Object[])new Object[]{"Error calculate prize user6 " + tran.username + " error: " + e.getMessage()});
                    }
                }
                break;
            }
            default: {
                Debug.trace((Object[])new Object[]{"error TX, room=" + this.moneyTypeStr + ", reference= " + referenceId + ", result= " + this.result});
            }
        }
        if (this.moneyType == 1) {
            Debug.trace((Object[])new Object[]{"TX phien= " + referenceId + ", tinh toan xong ket qua"});
        }
        rs.totalTai = potT.getTotalValue() - potT.getTotalBotBet();
        rs.numBetTai = potT.getNumBet() - potT.getNumBotBet();
        rs.totalXiu = potX.getTotalValue() - potX.getTotalBotBet();
        rs.numBetXiu = potX.getNumBet() - potX.getNumBotBet();
        rs.totalJp = isJpTai ? this.jackpotAccumulate : (isJpXiu ? this.jackpotAccumulate : 0L);
        UpdateMoneyTXTask taskTai = new UpdateMoneyTXTask(sumTai);
        taskTai.start();
        if (this.moneyType == 1) {
            Debug.trace((Object[])new Object[]{"TX phien= " + referenceId + ", cap nhat xong ben tai"});
        }
        UpdateMoneyTXTask taskXiu = new UpdateMoneyTXTask(sumXiu);
        taskXiu.start();
        if (this.moneyType == 1) {
            Debug.trace((Object[])new Object[]{"TX phien= " + referenceId + ", cap nhat xong ben xiu"});
        }
        ArrayList trans = new ArrayList(sumTXTMap.values());
        try {
            this.api.saveResultTaiXiu(rs);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.api.saveTransactionTaiXiu(trans);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSumTran(Map<String, TransactionTaiXiu> map, TransactionTaiXiuDetail tranDetail) {
        if (map.containsKey(tranDetail.username)) {
            TransactionTaiXiu txt = map.get(tranDetail.username);
            if (txt.betSide == tranDetail.betSide) {
                txt.betValue += tranDetail.betValue;
                txt.totalPrize += tranDetail.prize;
                txt.totalRefund += tranDetail.refund;
                txt.totalJp += tranDetail.jpAmount;
                map.put(tranDetail.username, txt);
            }
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

    private void saveTransactionDetailTX(TransactionTaiXiuDetail tran) {
        try {
            this.api.saveTransactionTaiXiuDetail(tran);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{"Update transaction detail tai xiu error: " + e.getMessage()});
        }
    }

    public void updateTaiXiuInfo(User user) {
        TaiXiuInfoMsg msg = new TaiXiuInfoMsg();
        msg.gameId = (short)2;
        msg.moneyType = this.moneyType;
        msg.referenceId = this.referenceId;
        msg.remainTime = this.getRemainTime();
        msg.bettingState = this.bettingRound;
        msg.potTai = this.getPotTai();
        msg.potXiu = this.getPotXiu();
        msg.myBetTai = this.getTotalBettingTaiByUsername(user.getName());
        msg.myBetXiu = this.getTotalBettingXiuByUsername(user.getName());
        msg.jpTai = this.jackpotAccumulate;
        msg.jpXiu = this.jackpotAccumulate;
        if (this.resultTX != null) {
            msg.dice1 = (short)this.resultTX.dice1;
            msg.dice2 = (short)this.resultTX.dice2;
            msg.dice3 = (short)this.resultTX.dice3;
        }
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    public boolean isBetting() {
        return this.bettingRound;
    }

    public short getNumBetTai() {
        return this.potTai.getNumBet();
    }

    public short getNumBetXiu() {
        return this.potXiu.getNumBet();
    }

    public short getJackPotApi() {
        return this.api.checkJackpotTaiXiuNotRemove();
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
        // SUN-1xxx (2026-05-11): userService.getUser returns null on Hazelcast
        // cache miss. The prior code NPE'd here, which escaped MGRoomTaiXiu.betTaiXiu
        // *after* money was already debited, and silently skipped the mongo
        // log_taixiu write + insertUserBetToDb. Result: real-player bets vanished
        // from bet history and never got prize-settled — money taken, no payout,
        // no log. Default to false (real player) on cache miss / any error so the
        // bet path completes correctly.
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
            user.setProperty("MGROOM_TAI_XIU_INFO", this);
        }
        return result;
    }

    @Override
    public boolean quitRoom(User user) {
        boolean result = super.quitRoom(user);
        if (result) {
            user.removeProperty("MGROOM_TAI_XIU_INFO");
        }
        return result;
    }

    public void updateJpValue(String newValue) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("jackpot_tx");
        Document doc = (Document)col.find().first();
        if (doc == null) {
            col.insertOne(new Document("jackpotTX", newValue));
        } else {
            Bson update = Updates.set((String)"jackpotTX", newValue);
            UpdateResult result = col.updateOne((Bson)new Document(), update);
            if (result.getModifiedCount() > 0L) {
                System.out.println("Update successful.");
            } else {
                System.out.println("Update failed. No record found.");
            }
        }
    }

    public String getJpValue() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("jackpot_tx");
        Document doc = (Document)col.find().first();
        if (doc == null) {
            return "0";
        }
        return doc.getString("jackpotTX");
    }

    public long getJackpotAccumulate() {
        return this.jackpotAccumulate;
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
                    // SUN-748 regression: getCurrentMoneyUserCache returns vin_total
                    // (cumulative P&L, negative for losing players). Client uses this
                    // as displayed balance and shows a negative wallet the moment a
                    // round ends for a losing player. Use actual vin balance instead;
                    // the winning paths below reassign from updateMoney() response
                    // anyway, so this only matters for the totalPrize==0&&totalRefund==0
                    // branch (the all-lost case) at line 1172.
                    long currentMoney = MGRoomTaiXiu.this.userService.getMoneyUserCache(username, MGRoomTaiXiu.this.moneyTypeStr);
                    if (!MGRoomTaiXiu.this.isBot(username)) {
                        Debug.trace((Object[])new Object[]{"TEST HU username: " + username + " totalPrize: " + txt.totalPrize + "  totalRefund: " + txt.totalRefund + " txt.totalJp: " + txt.totalJp});
                    }
                    if (txt.totalPrize == 0L && txt.totalRefund == 0L) {
                        MGRoomTaiXiu.this.userService.updateMoney(username, 0L, MGRoomTaiXiu.this.moneyTypeStr, "TaiXiu", "", "", 0L, Long.valueOf(MGRoomTaiXiu.this.referenceId), TransType.END_TRANS);
                    } else {
                        MoneyResponse res;
                        if (txt.totalPrize > 0L) {
                            if (txt.totalJp > 0L) {
                                isJp = true;
                                if (!MGRoomTaiXiu.this.isBot(username) && (res = MGRoomTaiXiu.this.userService.updateMoney(username, txt.totalJp, MGRoomTaiXiu.this.moneyTypeStr, "TaiXiu", Games.TAI_XIU.getId() + "", TaiXiuDescriptionUtils.getTaiXiuWinDescription(Games.TAI_XIU.getId() + "", MGRoomTaiXiu.this.referenceId, (byte)3), 0L, Long.valueOf(MGRoomTaiXiu.this.referenceId), TransType.IN_TRANS)).isSuccess()) {
                                    if (MGRoomTaiXiu.this.moneyType == 1 && !MGRoomTaiXiu.this.isBot(username)) {
                                        MGRoomTaiXiu.this.balance.addWin(txt.totalJp);
                                    }
                                    currentMoney = res.getCurrentMoney();
                                    if (MGRoomTaiXiu.this.moneyType == 1) {
                                        MGRoomTaiXiu.this.broadcastMsgService.putMessage(Games.TAI_XIU.getId(), username, txt.totalJp);
                                    }
                                }
                                jackpotDetails.add(new JackpotDetail(username, txt.totalJp));
                                int remainder = this.trans.size() % 5;
                                MGRoomTaiXiu.this.insertUserJackpotDetailToDb(timeJackpot, String.valueOf(this.trans.size() + 5 - remainder), String.valueOf(MGRoomTaiXiu.this.jackpotAccumulate), username, txt.totalJp);
                            }
                            TransType transType = TransType.END_TRANS;
                            if (txt.totalRefund > 0L) {
                                transType = TransType.IN_TRANS;
                            }
                            long fee = (long)(MGRoomTaiXiu.this.tax * (float)txt.totalPrize / (200.0f - MGRoomTaiXiu.this.tax));
                            MoneyResponse res2 = new MoneyResponse(false, "1001");
                            if (!MGRoomTaiXiu.this.isBot(username)) {
                                res2 = MGRoomTaiXiu.this.userService.updateMoney(username, txt.totalPrize, MGRoomTaiXiu.this.moneyTypeStr, "TaiXiu", Games.TAI_XIU.getId() + "", TaiXiuDescriptionUtils.getTaiXiuWinDescription(Games.TAI_XIU.getId() + "", MGRoomTaiXiu.this.referenceId, (byte)1), fee, Long.valueOf(MGRoomTaiXiu.this.referenceId), transType);
                            } else {
                                res2.setSuccess(true);
                            }
                            if (res2.isSuccess()) {
                                if (MGRoomTaiXiu.this.moneyType == 1 && !MGRoomTaiXiu.this.isBot(username)) {
                                    MGRoomTaiXiu.this.balance.addWin(txt.totalPrize);
                                    MGRoomTaiXiu.this.balance.addFee(fee);
                                }
                                currentMoney = res2.getCurrentMoney();
                                long totalExchange = (long)((float)txt.totalPrize * (100.0f - MGRoomTaiXiu.this.tax) / (200.0f - MGRoomTaiXiu.this.tax));
                                if (MGRoomTaiXiu.this.moneyType == 1 && totalExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                    MGRoomTaiXiu.this.broadcastMsgService.putMessage(Games.TAI_XIU.getId(), username, totalExchange);
                                }
                            }
                        }
                        if (txt.totalRefund > 0L && !MGRoomTaiXiu.this.isBot(username) && (res = MGRoomTaiXiu.this.userService.updateMoney(username, txt.totalRefund, MGRoomTaiXiu.this.moneyTypeStr, "TaiXiu", Games.TAI_XIU.getId() + "", TaiXiuDescriptionUtils.getTaiXiuRefundDescription(Games.TAI_XIU.getId() + "", MGRoomTaiXiu.this.referenceId), 0L, Long.valueOf(MGRoomTaiXiu.this.referenceId), TransType.END_TRANS)).isSuccess()) {
                            if (MGRoomTaiXiu.this.moneyType == 1 && !MGRoomTaiXiu.this.isBot(username)) {
                                MGRoomTaiXiu.this.balance.addWin(txt.totalRefund);
                            }
                            currentMoney = res.getCurrentMoney();
                        }
                    }
                    UpdatePrizeTaiXiuMsg msg = new UpdatePrizeTaiXiuMsg();
                    msg.moneyType = MGRoomTaiXiu.this.moneyType;
                    msg.totalMoney = txt.totalPrize + txt.totalRefund + txt.totalJp;
                    msg.currentMoney = currentMoney;
                    MGRoomTaiXiu.this.sendMessageToUser((BaseMsg)msg, username);
                }
                catch (Exception e) {
                    Debug.trace((Object[])new Object[]{"Update tai xiu money phien " + MGRoomTaiXiu.this.referenceId + " error: " + e.getMessage()});
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
                    data = i == max - 1 ? data + ((JackpotDetail)jackpotDetails.get(i)).getUserName() + "|" + ((JackpotDetail)jackpotDetails.get(i)).getMoney() : data + ((JackpotDetail)jackpotDetails.get(i)).getUserName() + "|" + ((JackpotDetail)jackpotDetails.get(i)).getMoney() + ",";
                }
                int remainder = this.trans.size() % 5;
                MGRoomTaiXiu.this.insertUserJackpotToDb(timeJackpot, String.valueOf(this.trans.size() + 5 - remainder), String.valueOf(MGRoomTaiXiu.this.jackpotAccumulate), data);
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
     * SUN-1373 — returns {@code true} when TaiXiu is administratively
     * disabled. Reads {@code TAIXIU_GAME_INACTIVE=1} from the environment
     * (set by admin via c=9982 or .env). Fail-closed to match the env-var
     * kill-switch convention: missing or any non-"1"/"true" value = active.
     *
     * <p>Uses the same env-var pattern as other game-control flags
     * (e.g. {@code XOCDIA_FORCE_ENABLED}, {@code BOT_FUND_MANIPULATION_ENABLED}).
     */
    private static boolean isTaiXiuDisabled() {
        String v = System.getenv("TAIXIU_GAME_INACTIVE");
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }
}
