/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.TaiXiuLiveDaoImpl;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class TaiXiuLiveServiceImpl {
    private Logger logger = Logger.getLogger((String)"rmq");
    private TaiXiuLiveDaoImpl dao = new TaiXiuLiveDaoImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();

    // P1: Body intentionally retained (with no publish call) so F1–F5
    // producer migration is a 1-line mechanical swap (publish → MessageBus.publish).
    // See docs/RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md.
    public boolean saveResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType, long totalJp) throws Exception {
        ResultTaiXiuMessage msg = new ResultTaiXiuMessage();
        msg.referenceId = referenceId;
        msg.result = result;
        msg.dice1 = dice1;
        msg.dice2 = dice2;
        msg.dice3 = dice3;
        msg.totalTai = totalTai;
        msg.totalXiu = totalXiu;
        msg.numBetTai = numBetTai;
        msg.numBetXiu = numBetXiu;
        msg.totalPrize = totalPrize;
        msg.totalRefundTai = totalRefundTai;
        msg.totalRefundXiu = totalRefundXiu;
        msg.totalRevenue = totalRevenue;
        msg.moneyType = moneyType;
        msg.totalJackpot = totalJp;
        msg.isMD5 = true;
        // P1 cleanup: queue_taixiu_live retired (no consumer); SaveResultTaiXiuLiveProcessor deleted.
        this.logger.debug(("saveResultTaiXiu Test2: " + referenceId + " md5:" + msg.isMD5));
        return true;
    }

    // P1: Body intentionally retained (with no publish call) so F1–F5
    // producer migration is a 1-line mechanical swap (publish → MessageBus.publish).
    // See docs/RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md.
    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetail tran) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuDetailMessage msg = new TransactionTaiXiuDetailMessage();
        msg.referenceId = tran.referenceId;
        msg.transactionCode = tran.transactionCode;
        msg.userId = tran.userId;
        msg.username = tran.username;
        msg.betValue = tran.betValue;
        msg.betSide = tran.betSide;
        msg.prize = tran.prize;
        msg.refund = tran.refund;
        msg.inputTime = tran.inputTime;
        msg.moneyType = tran.moneyType;
        msg.isMD5 = true;
        // P1 cleanup: queue_taixiu_live retired (no consumer); SaveTransactionDetailTaiXiuLiveProcessor deleted.
        return true;
    }

    // P1: Body intentionally retained (with no publish call) so F1–F5
    // producer migration is a 1-line mechanical swap (publish → MessageBus.publish).
    // See docs/RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md.
    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetail tran) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuDetailMessage msg = new TransactionTaiXiuDetailMessage();
        msg.referenceId = tran.referenceId;
        msg.transactionCode = tran.transactionCode;
        msg.userId = tran.userId;
        msg.username = tran.username;
        msg.betValue = tran.betValue;
        msg.betSide = tran.betSide;
        msg.prize = tran.prize;
        msg.refund = tran.refund;
        msg.inputTime = tran.inputTime;
        msg.moneyType = tran.moneyType;
        msg.isMD5 = true;
        // P1 cleanup: queue_taixiu_live retired (no consumer); UpdateTransactionDetailTaiXiuLiveProcessor deleted.
        return true;
    }

    public List<TransactionTaiXiu> getLichSuGiaoDich(String username, int page, int moneyType) throws SQLException {
        return this.dao.getLichSuGiaoDich(username, page, moneyType);
    }
}

