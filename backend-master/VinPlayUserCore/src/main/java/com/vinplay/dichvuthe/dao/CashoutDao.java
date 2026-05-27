/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage
 */
package com.vinplay.dichvuthe.dao;

import com.vinplay.dichvuthe.entities.BankAccountInfo;
import com.vinplay.dichvuthe.response.CashoutTransResponse;
import com.vinplay.dichvuthe.response.CashoutUserDailyResponse;
import com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage;
import java.sql.SQLException;
import java.util.List;

public interface CashoutDao {
    public long getSystemCashout() throws SQLException;

    public boolean updateSystemCashout(long var1) throws SQLException;

    public void logCashoutByBank(CashoutByBankMessage var1) throws Exception;

    public BankAccountInfo getBankAccountInfo(String var1) throws SQLException;

    public CashoutUserDailyResponse getCashoutUserToday(String var1);

    public List<CashoutTransResponse> getListCashoutByCardPending() throws Exception;

    public void updateCashOutByCard(String var1, String var2, int var3, String var4, int var5, int var6) throws Exception;

    public void insertCardIntoDB(String var1, int var2, String var3, String var4, String var5) throws Exception;

    public List<CashoutTransResponse> getListCashoutByCardPending(String var1, String var2, String var3) throws Exception;
}

