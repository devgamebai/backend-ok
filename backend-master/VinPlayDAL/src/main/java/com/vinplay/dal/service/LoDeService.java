/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LotteryMessage
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface LoDeService {
    public void saveTransactionLode(long var1, String var3, long var4, long var6, String var8, long var9);

    /**
     * SUN-1295: extended overload that stamps the per-bet rate/prize snapshot
     * at purchase time. Settle-side ({@code LotteryModule.getPrize}) reads
     * these values from the row instead of the live {@code LotteryMode} enum,
     * so a future rate change cannot retroactively alter pending bets.
     *
     * @param betUnit         user's per-number stake (before the rate multiplier)
     * @param rateAtPurchase  {@code LotteryMode.rate} at purchase
     * @param prizeMultiplier {@code LotteryMode.prizeMultiplier} at purchase
     */
    public void saveTransactionLode(long userId, String nickName, long betValue, long mode, String ticket, long prize,
                                    long betUnit, int rateAtPurchase, int prizeMultiplier);

    public void updatePrize(long var1, long var3);

    public List<LotteryMessage> getLotteryTicket(String var1) throws ParseException;

    public List<LotteryMessage> getLotteryTicketByUserName(String var1);

    public List<String> getLotteryResultByDate();

    public void saveLotteryResult(String var1, Date var2);

    public String getLatestResult(String var1) throws ParseException;

    public List<LotteryMessage> search(String var1, String var2, String var3, String var4, String var5, int var6, int var7) throws SQLException;

    public long count(String var1, String var2, String var3, String var4, String var5) throws SQLException;
}

