/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.TaiXiuService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.TaiXiuServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 */
package game.modules.minigame.room;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.TaiXiuMD5ServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import game.modules.minigame.cmd.rev.BetTaiXiuCmd;
import game.modules.minigame.cmd.send.txmini_md5.*;
import game.modules.minigame.entities.BalanceMoneyTX;
import game.modules.minigame.entities.MinigameConstant;
import game.modules.minigame.entities.PotTaiXiu;
import game.modules.minigame.utils.TaiXiuUtils;
import game.utils.ConfigGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MGRoomTaiXiuMD5
        extends MGRoom {
    public long referenceId;
    private short moneyType;
    private String moneyTypeStr;
    private PotTaiXiu potTai;
    private PotTaiXiu potXiu;
    private long startTime = 0L;
    private short result = (short) -1;
    public boolean bettingRound = false;
    public boolean enableBetting = false;
    private ResultTaiXiu resultTX;
    public TaiXiuMD5ServiceImpl api = new TaiXiuMD5ServiceImpl();
    private UserService userService = new UserServiceImpl();
    private BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    private float tax = MinigameConstant.MINIGAME_TAX_VIN_MD5;
    private BalanceMoneyTX balance = new BalanceMoneyTX();
    private long blackListBetTai = 0L;
    private long blackListBetXiu = 0L;
    private long whiteListBetTai = 0L;
    private long whiteListBetXiu = 0L;
    public long nguoichoidatTai = 0L;
    public long nguoichoidatXiu = 0L;

    public final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    public MGRoomTaiXiuMD5(String name, long referenceId, short moneyType) {
        super(name);
        this.moneyType = moneyType;
        this.moneyTypeStr = "xu";
        if (moneyType == 1) {
            this.moneyTypeStr = "vin";
            this.tax = MinigameConstant.MINIGAME_TAX_VIN_MD5;
            ReportMoneySystemModel model = this.api.getReportTX(ConfigGame.getIntValue("interval_reset_balance", 10));
            if (model != null) {
                this.balance = new BalanceMoneyTX(model.moneyWin, model.moneyLost, model.fee, model.dateReset);
                Debug.trace( ("TAI XIU MD5 VIN, win=" + model.moneyWin + ", loss=" + model.moneyLost + ", fee= " + model.fee + ", date reset= " + model.dateReset));
            }
        } else {
            this.tax = MinigameConstant.MINIGAME_TAX_VIN_MD5;
        }
        this.potTai = new PotTaiXiu();
        this.potXiu = new PotTaiXiu();
        this.referenceId = referenceId;
    }

    public void startNewGame(long newReferenceId) {
        // SUN-1246: see MGRoomTaiXiu.startNewGame for the full rationale.
        // Clear stale resultTX so updateTaiXiuInfo() during the new round's
        // betting phase doesn't leak the previous round's dice values to
        // reconnecting / mid-round-joining clients.
        this.resultTX = null;
        this.referenceId = newReferenceId;
        this.bettingRound = true;
        this.enableBetting = true;
        this.blackListBetTai = 0L;
        this.blackListBetXiu = 0L;
        this.whiteListBetTai = 0L;
        this.whiteListBetXiu = 0L;
        this.nguoichoidatTai = 0L;
        this.nguoichoidatXiu = 0L;
        this.potTai.renew();
        this.potXiu.renew();
        this.startTime = System.currentTimeMillis();
        Debug.trace( ("START NEW ROUND " + this.referenceId));
//        logger.debug("START NEW ROUND MD5 + this.referenceId");
    }

    public void finish() {
        this.resultTX = null;
        // 2026-05-08: do NOT reset startTime here. SUN-1245 time-based
        // getRemainTime() relies on startTime being the round-start instant
        // so revealStart = startTime + bettingTime*1000L. Resetting on
        // finish made the bet-phase branch re-trigger and FE displayed a
        // bogus countdown during the reveal window.
        this.bettingRound = false;
    }

    public void disableBetting() {
        this.enableBetting = false;
    }

    public void updateResultDices(short[] dices, short result, String before_md5, String md5) {
        this.result = result;
        UpdateResultDicesMsg msg = new UpdateResultDicesMsg();
        msg.result = result;
        msg.dice1 = dices[0];
        msg.dice2 = dices[1];
        msg.dice3 = dices[2];
        msg.before_md5 = before_md5;
        this.resultTX = new ResultTaiXiu();
        this.resultTX.referenceId = this.referenceId;
        this.resultTX.dice1 = msg.dice1;
        this.resultTX.dice2 = msg.dice2;
        this.resultTX.dice3 = msg.dice3;
        this.resultTX.result = msg.result;
        this.resultTX.moneyType = this.moneyType;
        this.resultTX.before_md5 = before_md5;
        this.resultTX.md5 = md5;
        this.sendMessageToRoom(msg);
    }

    public short getRemainTime() {
        // SUN-769 (re-applied 2026-04-29): see MGRoomTaiXiu.getRemainTime
        // for full rationale. Same regression reintroduced by a60816b0 on
        // 2026-04-12, same fix applied here.
        //
        // SUN-1245: TaiXiuMD5Module.gameLoop has the same count=45 →
        // disableBetting / count=50 → finish pattern as TaiXiuModule, so
        // the 5-second flag/formula skew applies here too — countdown
        // would freeze on `resultTime` (15) during that window. Branch
        // on the time-based reveal-start instead of `enableBetting`.
        final int bettingTime = 50;
        final int resultTime = 15;
        long currentTime = System.currentTimeMillis();
        long revealStart = this.startTime + (long) bettingTime * 1000L;
        if (currentTime < revealStart) {
            int elapsed = (int) ((currentTime - this.startTime) / 1000L);
            if (elapsed < 0) elapsed = 0;
            if (elapsed > bettingTime) elapsed = bettingTime;
            return (short) (bettingTime - elapsed);
        }
        int elapsedSinceReveal = (int) ((currentTime - revealStart) / 1000L);
        if (elapsedSinceReveal < 0) elapsedSinceReveal = 0;
        if (elapsedSinceReveal > resultTime) elapsedSinceReveal = resultTime;
        return (short) (resultTime - elapsedSinceReveal);
    }

    public void betTaiXiu(User user, BetTaiXiuCmd cmd) {
        BetTaiXiuMsg msg = this.betTaiXiu(user.getName(), cmd.userId, cmd.betValue, cmd.inputTime, cmd.moneyType, cmd.betSide, false);
        this.sendMessageToUser((BaseMsg) msg, user);
    }

    public BetTaiXiuMsg betTaiXiu(String nickname, int userId, long betValue, short inputTime, short moneyType, short betSide, boolean isBot) {
        inputTime = this.getRemainTime();
        long currentMoney = this.userService.getMoneyUserCache(nickname, this.moneyTypeStr);
        int result = 2;
        if (this.enableBetting) {
            if (betValue >= 100L) {
                if (betValue > currentMoney) {
                    result = 3;
                } else {
                    result = 2;
                    TransactionTaiXiuDetail transTX = new TransactionTaiXiuDetail(this.referenceId, userId, nickname, betValue, (int) betSide, (int) inputTime, (int) moneyType);
                    if (betSide == 1 && this.potXiu.getTotalBetByUsername(nickname) > 0L || betSide == 0 && this.potTai.getTotalBetByUsername(nickname) > 0L) {
                        result = 5;
                    } else {
                        String betSideStr = betSide == 0 ? "X\u1ec9u" : "T\u00e0i";
                        // SUN-1290: per-bet unique transId \u2014 see MGRoomTaiXiu
                        // for the full rationale. Without this, the second
                        // same-side bet by the same user in one round
                        // collides on money_gateway_log.uk_tx_source and
                        // returns 1031 "Duplicate transaction".
                        long perBetTxId = this.referenceId * 1000000L
                                + (System.nanoTime() & 0xFFFFFL);
                        MoneyResponse res = this.userService.updateMoney(nickname, -betValue, this.moneyTypeStr, "TaiXiuMD5", "T\u00e0i x\u1ec9u MD5: \u0110\u1eb7t c\u01b0\u1ee3c", "Phii\u00ean " + this.referenceId + ": \u0111\u1eb7t " + betSideStr + " (" + inputTime + ")", 0L, Long.valueOf(perBetTxId), TransType.START_TRANS);
                        if (res.isSuccess()) {
                            if (!this.enableBetting) {
                                result = 1;
                                this.userService.updateMoney(nickname, betValue, this.moneyTypeStr, "TaiXiuMD5", "T\u00e0i x\u1ec9u: Tr\u1ea3 c\u01b0\u1ee3c", "Ho\u00e0n tr\u1ea3 \u0111\u1eb7t c\u01b0\u1ee3c phi\u00ean " + this.referenceId, 0L, Long.valueOf(perBetTxId), TransType.END_TRANS);
                            } else {
                                //Alex
                                if (!isBot && moneyType == 1) {
                                    if (betSide == 1) {
                                        this.nguoichoidatTai += betValue;
                                    } else {
                                        this.nguoichoidatXiu += betValue;
                                    }
                                }
                                //EndAlex
                                isBot = this.isBot(nickname);
                                if (moneyType == 1 && !isBot) {
                                    Random rd;
                                    int n;
                                    this.balance.addBet(betValue);
                                    if (betValue >= (long) ConfigGame.getIntValue("tx_min_money_black_list", 2000000) && ConfigGame.inBlackList(nickname) && (n = (rd = new Random()).nextInt(100)) <= ConfigGame.getIntValue("tx_black_list_percent", 50)) {
                                        Debug.trace( ("Black list " + nickname + " money= " + betValue + ", bet side= " + betSide));
                                        if (betSide == 1) {
                                            this.blackListBetTai += betValue;
                                        } else {
                                            this.blackListBetXiu += betValue;
                                        }
                                    }
                                    if (betValue >= (long) ConfigGame.getIntValue("tx_min_money_white_list", 2000000) && ConfigGame.inWhiteList(nickname) && (n = (rd = new Random()).nextInt(100)) <= ConfigGame.getIntValue("tx_white_list_percent", 50)) {
                                        Debug.trace( ("White list " + nickname + " money= " + betValue + ", bet side= " + betSide));
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
        msg.Error = (byte) result;
        msg.currentMoney = currentMoney;
        return msg;
    }

    public void updateTaiXiuPerSecond() {
        UpdateTaiXiuPerSecondMsg msg = new UpdateTaiXiuPerSecondMsg();
        msg.remainTime = this.getRemainTime();
        msg.bettingState = this.bettingRound;
        msg.potTai = this.getPotTai();
        msg.potXiu = this.getPotXiu();
        msg.numBetTai = this.potTai.getNumBet();
        msg.numBetXiu = this.potXiu.getNumBet();
        this.sendMessageToRoom(msg);
    }

    private void calculateNhaCai(int result) {
        long delta = this.nguoichoidatTai - this.nguoichoidatXiu;
        long total = 0L;
        int PHE_NHA_CAI = (int) MinigameConstant.MINIGAME_TAX_VIN_PHE_NHA_CAI;

        if (result == 1) {//Tai
            total = (nguoichoidatTai) * PHE_NHA_CAI / 100 + ((nguoichoidatTai - nguoichoidatXiu)>0?-1:1) * delta;
        } else {
            total = (nguoichoidatXiu) * PHE_NHA_CAI / 100 + ((nguoichoidatTai - nguoichoidatXiu)>0?1:-1) * delta;
        }

        //update
        MoneyResponse res2 = MGRoomTaiXiuMD5.this.userService.updateMoney(MinigameConstant.TAI_XIU_MD5_NHA_CAI_USERNAME
                , total, MGRoomTaiXiuMD5.this.moneyTypeStr, "TaiXiuMD5", "Cap nhat nha cai", "Phien " + MGRoomTaiXiuMD5.this.referenceId, 0, Long.valueOf(MGRoomTaiXiuMD5.this.referenceId), TransType.END_TRANS);
        if (res2.isSuccess()) {
            try {
                api.updateDealerProfit(referenceId, result, this.nguoichoidatTai, this.nguoichoidatXiu, total, res2.getCurrentMoney());
            }catch(Exception ex){
                Debug.trace( (" error resultTX: " + ex.getMessage()));
                logger.error("error:", ex);
            }
            logger.debug("WARNING: update money for boss_md5 fail!!!!!!!!! - " + res2.getErrorCode());
        }
    }

    public void calculatePrize(long referenceId) {
        PotTaiXiu potX = this.potXiu;
        PotTaiXiu potT = this.potTai;
        HashMap<String, TransactionTaiXiu> sumTXTMap = new HashMap<String, TransactionTaiXiu>();
        HashMap<String, TransactionTaiXiu> sumTai = new HashMap<String, TransactionTaiXiu>();
        HashMap<String, TransactionTaiXiu> sumXiu = new HashMap<String, TransactionTaiXiu>();
        long tongTienHopLe = potT.getTotalValue() > potX.getTotalValue() ? potX.getTotalValue() : potT.getTotalValue();
//        long tongTienTaiDaTinh = 0L;
//        long tongTienXiuDaTinh = 0L;
        logger.debug("calculatePrize MD5");
        ResultTaiXiu rs = new ResultTaiXiu();
        try {
            if (this.resultTX != null) {
                rs = this.resultTX;
                Debug.trace( ("resultTX " +  this.resultTX));
            } else {
                Debug.trace( (" error: " +  this.resultTX));
            }
        } catch (Exception ex) {
            Debug.trace( (" error resultTX: " + ex.getMessage()));
        }
        //Alex
        calculateNhaCai(this.result);
        //
        switch (this.result) {
            case 0: {//XIU
                if (potX != null && potX.contributors != null) {
                    for (TransactionTaiXiuDetail tran : potX.contributors) {
                        try {
                            long tienDuocTinh = tran.betValue;
//                            if (tongTienXiuDaTinh + tran.betValue > tongTienHopLe) {
//                                tienDuocTinh = tongTienHopLe - tongTienXiuDaTinh;
//                            }
//                            tongTienXiuDaTinh += tienDuocTinh;
                            tran.prize = (long) ((float) tienDuocTinh * (100.0f - this.tax) / 100.0f) + tienDuocTinh;
                            rs.totalPrize += tran.prize;
                            tran.refund = tran.betValue - tienDuocTinh;
                            rs.totalRefundXiu += tran.refund;
                            this.updateSumTran(sumTXTMap, tran);
                            this.updateSumTran(sumXiu, tran);
                            this.saveTransactionDetailTX(tran);
                        } catch (Exception e) {
                            Debug.trace( ("Error calculate prize user " + tran.username + " error: " + e.getMessage()));
                        }
                    }
                }
                if (potT == null || potT.contributors == null) break;
                for (TransactionTaiXiuDetail tran : potT.contributors) {
                    try {
                        long tienDuocTinh = tran.betValue;
//                        if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
//                            tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
//                        }
//                        tongTienTaiDaTinh += tienDuocTinh;
                        tran.refund = tran.betValue - tienDuocTinh;
                        rs.totalRefundTai += tran.refund;
                        this.updateSumTran(sumTXTMap, tran);
                        this.updateSumTran(sumTai, tran);
                        this.saveTransactionDetailTX(tran);
                    } catch (Exception e) {
                        Debug.trace( ("Error calculate prize user " + tran.username + " error: " + e.getMessage()));
                    }
                }
                break;
            }
            case 1: {
                if (potT != null && potT.contributors != null) {
                    for (TransactionTaiXiuDetail tran : potT.contributors) {
                        try {
                            long tienDuocTinh = tran.betValue;
//                            if (tongTienTaiDaTinh + tran.betValue > tongTienHopLe) {
//                                tienDuocTinh = tongTienHopLe - tongTienTaiDaTinh;
//                            }
//                            tongTienTaiDaTinh += tienDuocTinh;
                            tran.prize = (long) ((float) tienDuocTinh * (100.0f - this.tax) / 100.0f) + tienDuocTinh;
                            rs.totalPrize += tran.prize;
                            tran.refund = tran.betValue - tienDuocTinh;
                            rs.totalRefundTai += tran.refund;
                            this.updateSumTran(sumTXTMap, tran);
                            this.updateSumTran(sumTai, tran);
                            this.saveTransactionDetailTX(tran);
                        } catch (Exception e) {
                            Debug.trace( ("Error calculate prize user " + tran.username + " error: " + e.getMessage()));
                        }
                    }
                }
                if (potX == null || potX.contributors == null) break;
                for (TransactionTaiXiuDetail tran : potX.contributors) {
                    try {
                        long tienDuocTinh = tran.betValue;
//                        if (tongTienXiuDaTinh + tran.betValue > tongTienHopLe) {
//                            tienDuocTinh = tongTienHopLe - tongTienXiuDaTinh;
//                        }
//                        tongTienXiuDaTinh += tienDuocTinh;
                        tran.refund = tran.betValue - tienDuocTinh;
                        rs.totalRefundXiu += tran.refund;
                        this.updateSumTran(sumTXTMap, tran);
                        this.updateSumTran(sumXiu, tran);
                        this.saveTransactionDetailTX(tran);
                    } catch (Exception e) {
                        Debug.trace( ("Error calculate prize user " + tran.username + " error: " + e.getMessage()));
                    }
                }
                break;
            }
            default: {
                Debug.trace( ("Fuck error TX, room=" + this.moneyTypeStr + ", reference= " + referenceId + ", result= " + this.result));
            }
        }
        if (this.moneyType == 1) {
            Debug.trace( ("TX phien= " + referenceId + ", tinh toan xong ket qua"));
        }
        rs.totalTai = potT.getTotalValue();
        rs.numBetTai = potT.getNumBet();
        rs.totalXiu = potX.getTotalValue();
        rs.numBetXiu = potX.getNumBet();
        UpdateMoneyTXTask taskTai = new UpdateMoneyTXTask(sumTai);
        taskTai.start();
        if (this.moneyType == 1) {
            Debug.trace( ("TX phien= " + referenceId + ", cap nhat xong ben tai"));
        }
        UpdateMoneyTXTask taskXiu = new UpdateMoneyTXTask(sumXiu);
        taskXiu.start();
        if (this.moneyType == 1) {
            Debug.trace( ("TX phien= " + referenceId + ", cap nhat xong ben xiu"));
        }
        ArrayList<TransactionTaiXiu> trans = new ArrayList<TransactionTaiXiu>(sumTXTMap.values());
        try {
           // this.api.saveResultTaiXiu(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
          //  this.api.saveTransactionTaiXiu(trans);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (this.moneyType == 1) {
//            CalculateEndTXTask task = new CalculateEndTXTask(trans);
//            task.start();
//        }
    }

    private void updateSumTran(Map<String, TransactionTaiXiu> map, TransactionTaiXiuDetail tranDetail) {
        if (map.containsKey(tranDetail.username)) {
            TransactionTaiXiu txt = map.get(tranDetail.username);
            if (txt.betSide == tranDetail.betSide) {
                txt.betValue += tranDetail.betValue;
                txt.totalPrize += tranDetail.prize;
                txt.totalRefund += tranDetail.refund;
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
        Debug.trace( ("Bot tai: " + totalPrizeBotTai + ", bot xiu: " + totalPrizeBotXiu));
        Debug.trace( ("User tai: " + totalPrizeUserTai + ", user xiu: " + totalPrizeUserXiu));
        short result = -1;
        switch (type) {
            case 1: {
                result = this.tinhCuaThang(totalPrizeBotTai, totalPrizeBotXiu);
                Debug.trace( ("He thong am, force= " + result));
                break;
            }
            case -2: {
                result = this.tinhCuaThang(-this.blackListBetTai, -this.blackListBetXiu);
                Debug.trace( ("Black list: " + result));
                break;
            }
            case -3: {
                result = this.tinhCuaThang(this.whiteListBetTai, this.whiteListBetXiu);
                Debug.trace( ("White list: " + result));
                break;
            }
            case -1: {
                long totalUserWinSystem = Math.abs(totalPrizeUserTai - totalPrizeUserXiu);
                if (totalUserWinSystem <= this.balance.getMaxWinUser()) {
                    result = this.tinhCuaThang(totalPrizeUserTai, totalPrizeUserXiu);
                    Debug.trace( ("Nguoi choi am, force = " + result));
                    break;
                }
                Debug.trace( ("Nguoi choi am nhung so tien an qua lo'n= " + totalUserWinSystem));
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
        short result = -1;
        long fee = balance.getFee();
        long revenueUser = balance.getRevenueUser();
        long maxUserWin = Math.abs(totalPrizeUserTai - totalPrizeUserXiu);
        if ((float) (revenueUser + (long) ((float) maxUserWin * (100.0F - tax) / 100.0F)) >= (float) -fee * ConfigGame.getFloatValue("tx_min_fee", 1.0F)) {
            result = tinhCuaThang(totalPrizeBotTai, totalPrizeBotXiu);
            Debug.trace("Chong he thong am, force= " + result + ", max user win= " + maxUserWin);
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
      //      this.api.saveTransactionTaiXiuDetail(tran);
        } catch (Exception e) {
            Debug.trace( ("Update transaction detail tai xiu error: " + e.getMessage()));
        }
    }

    public void updateTaiXiuInfo(User user, short remainTimeRutLoc, String md5, String before_md5) {
        TaiXiuInfoMsg msg = new TaiXiuInfoMsg();
        msg.gameId = (short) 2;
        msg.moneyType = this.moneyType;
        msg.referenceId = this.referenceId;
        msg.remainTime = this.getRemainTime();
        msg.bettingState = this.bettingRound;
        msg.potTai = this.getPotTai();
        msg.potXiu = this.getPotXiu();
        msg.myBetTai = this.getTotalBettingTaiByUsername(user.getName());
        msg.myBetXiu = this.getTotalBettingXiuByUsername(user.getName());
        if (this.resultTX != null) {
            msg.dice1 = (short) this.resultTX.dice1;
            msg.dice2 = (short) this.resultTX.dice2;
            msg.dice3 = (short) this.resultTX.dice3;
        }
        msg.remainTimeRutLoc = remainTimeRutLoc;
        msg.md5 = md5;
        if (!msg.bettingState) {
            //ket thuc
            msg.before_md5 = before_md5;
        }
        logger.debug("updateTaiXiuInfo md5 with referenceId=" + referenceId);
        this.sendMessageToUser((BaseMsg) msg, user);
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
        return roomId == 0 ? (short) 0 : 1;
    }

    public static String getKeyRoom(short moneyType) {
        return "" + moneyType + "_" + 2;
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        if (result) {
            user.setProperty( "MGROOM_TAI_XIU_INFO",  this);
        }
        return result;
    }

    @Override
    public boolean quitRoom(User user) {
        boolean result = super.quitRoom(user);
        if (result) {
            user.removeProperty( "MGROOM_TAI_XIU_INFO");
        }
        return result;
    }

    private final class UpdateMoneyTXTask
            extends Thread {
        private Map<String, TransactionTaiXiu> trans = new HashMap<String, TransactionTaiXiu>();

        private UpdateMoneyTXTask(Map<String, TransactionTaiXiu> trans) {
            this.trans = trans;
        }

        @Override
        public void run() {
            for (Map.Entry<String, TransactionTaiXiu> entry : this.trans.entrySet()) {
                try {
                    String username = entry.getKey();
                    TransactionTaiXiu txt = entry.getValue();
                    // SUN-748 regression fix: use vin balance, not vin_total (cumulative
                    // P&L) — see MGRoomTaiXiu.java:1175 for full rationale.
                    long currentMoney = MGRoomTaiXiuMD5.this.userService.getMoneyUserCache(username, MGRoomTaiXiuMD5.this.moneyTypeStr);
                    if (txt.totalPrize == 0L && txt.totalRefund == 0L) {
                        MGRoomTaiXiuMD5.this.userService.updateMoney(username, 0L, MGRoomTaiXiuMD5.this.moneyTypeStr, "TaiXiuMD5", "", "", 0L, Long.valueOf(MGRoomTaiXiuMD5.this.referenceId), TransType.END_TRANS);
                    } else {
                        MoneyResponse res;
                        if (txt.totalPrize > 0L) {
                            TransType transType = TransType.END_TRANS;
                            if (txt.totalRefund > 0L) {
                                transType = TransType.IN_TRANS;
                            }
                            long fee = (long) (MGRoomTaiXiuMD5.this.tax * (float) txt.totalPrize / (200.0f - MGRoomTaiXiuMD5.this.tax));
                            MoneyResponse res2 = MGRoomTaiXiuMD5.this.userService.updateMoney(username, txt.totalPrize, MGRoomTaiXiuMD5.this.moneyTypeStr, "TaiXiuMD5", "Th\u1eafng t\u00e0i x\u1ec9u", "Phi\u00ean " + MGRoomTaiXiuMD5.this.referenceId, fee, Long.valueOf(MGRoomTaiXiuMD5.this.referenceId), transType);
                            if (res2.isSuccess()) {
                                if (MGRoomTaiXiuMD5.this.moneyType == 1 && !MGRoomTaiXiuMD5.this.isBot(username)) {
                                    MGRoomTaiXiuMD5.this.balance.addWin(txt.totalPrize);
                                    MGRoomTaiXiuMD5.this.balance.addFee(fee);
                                }
                                currentMoney = res2.getCurrentMoney();
                                long totalExchange = (long) ((float) txt.totalPrize * (100.0f - MGRoomTaiXiuMD5.this.tax) / (200.0f - MGRoomTaiXiuMD5.this.tax));
                                if (MGRoomTaiXiuMD5.this.moneyType == 1 && totalExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                                    MGRoomTaiXiuMD5.this.broadcastMsgService.putMessage(Games.TAI_XIU.getId(), username, totalExchange);
                                }
                            }
                        }
                        if (txt.totalRefund > 0L && (res = MGRoomTaiXiuMD5.this.userService.updateMoney(username, txt.totalRefund, MGRoomTaiXiuMD5.this.moneyTypeStr, "TaiXiuMD5", "Ho\u00e0n tr\u1ea3 t\u00e0i x\u1ec9u", "Phi\u00ean " + MGRoomTaiXiuMD5.this.referenceId, 0L, Long.valueOf(MGRoomTaiXiuMD5.this.referenceId), TransType.END_TRANS)).isSuccess()) {
                            if (MGRoomTaiXiuMD5.this.moneyType == 1 && !MGRoomTaiXiuMD5.this.isBot(username)) {
                                MGRoomTaiXiuMD5.this.balance.addWin(txt.totalRefund);
                            }
                            currentMoney = res.getCurrentMoney();
                        }
                    }
                    UpdatePrizeTaiXiuMsg msg = new UpdatePrizeTaiXiuMsg();
                    msg.moneyType = MGRoomTaiXiuMD5.this.moneyType;
                    msg.totalMoney = txt.totalPrize + txt.totalRefund;
                    msg.currentMoney = currentMoney;
                    MGRoomTaiXiuMD5.this.sendMessageToUser((BaseMsg) msg, username);
                } catch (Exception e) {
                    Debug.trace( ("Update tai xiu money phien " + MGRoomTaiXiuMD5.this.referenceId + " error: " + e.getMessage()));
                }
            }
        }
    }

    private final class CalculateEndTXTask
            extends Thread {
        private List<TransactionTaiXiu> trans;

        private CalculateEndTXTask(List<TransactionTaiXiu> trans) {
            this.trans = trans;
        }

        @Override
        public void run() {

        }
    }

}

