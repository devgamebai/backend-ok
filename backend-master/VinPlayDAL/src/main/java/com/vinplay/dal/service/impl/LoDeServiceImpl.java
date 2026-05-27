/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LotteryMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.LoDeDao;
import com.vinplay.dal.dao.impl.LoDeDaoImpl;
import com.vinplay.dal.service.LoDeService;
import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class LoDeServiceImpl
implements LoDeService {
    private Logger logger = Logger.getLogger((String)"lode");
    private LoDeDao loDeDao = new LoDeDaoImpl();

    @Override
    public void saveTransactionLode(long userId, String nickName, long betValue, long mode, String ticket, long prize) {
        try {
            this.loDeDao.saveTransactionLode(new LotteryMessage(userId, nickName, betValue, mode, ticket, Long.valueOf(prize)));
        }
        catch (Exception e) {
            this.logger.error(("saveTransactionLode error: " + e.getMessage()));
        }
    }

    /** SUN-1295 — saves with the per-bet rate/prize snapshot stamped on the row. */
    @Override
    public void saveTransactionLode(long userId, String nickName, long betValue, long mode, String ticket, long prize,
                                    long betUnit, int rateAtPurchase, int prizeMultiplier) {
        try {
            LotteryMessage msg = new LotteryMessage(userId, nickName, betValue, mode, ticket, Long.valueOf(prize));
            msg.setBetUnit(Long.valueOf(betUnit));
            msg.setRateAtPurchase(Integer.valueOf(rateAtPurchase));
            msg.setPrizeMultiplier(Integer.valueOf(prizeMultiplier));
            this.loDeDao.saveTransactionLode(msg);
        }
        catch (Exception e) {
            this.logger.error(("saveTransactionLode (with snapshot) error: " + e.getMessage()));
        }
    }

    @Override
    public void updatePrize(long id, long prize) {
        this.loDeDao.updatePrize(id, prize);
    }

    @Override
    public List<LotteryMessage> getLotteryTicket(String time) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Date date = format.parse(time);
        return this.loDeDao.getRecordsWithNullPrizeBefore1830Today(date);
    }

    @Override
    public List<LotteryMessage> getLotteryTicketByUserName(String nickName) {
        return this.loDeDao.getRowsByNickname(nickName);
    }

    @Override
    public List<String> getLotteryResultByDate() {
        return this.loDeDao.getListOfResultsByDateRange();
    }

    @Override
    public void saveLotteryResult(String result, Date time) {
        this.loDeDao.saveToDatabase(result, time);
    }

    @Override
    public String getLatestResult(String time) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Date date = format.parse(time);
        return this.loDeDao.getLatestResult(date);
    }

    @Override
    public List<LotteryMessage> search(String nickName, String ticket, String model, String timeStart, String timeEnd, int page, int limit) throws SQLException {
        return this.loDeDao.search(nickName, ticket, model, timeStart, timeEnd, page, limit);
    }

    @Override
    public long count(String nickName, String ticket, String model, String timeStart, String timeEnd) throws SQLException {
        return this.loDeDao.count(nickName, ticket, model, timeStart, timeEnd);
    }
}

