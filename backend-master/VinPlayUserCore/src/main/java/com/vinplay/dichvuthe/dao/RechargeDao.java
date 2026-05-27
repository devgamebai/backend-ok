/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.response.LogRechargeBankNLResponse
 *  com.vinplay.usercore.response.LogRechargeBankNapasResponse
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 *  org.bson.Document
 */
package com.vinplay.dichvuthe.dao;

import com.vinplay.dichvuthe.entities.DepositBankModel;
import com.vinplay.dichvuthe.entities.DepositMomoModel;
import com.vinplay.iap.lib.Purchase;
import com.vinplay.usercore.response.LogRechargeBankNLResponse;
import com.vinplay.usercore.response.LogRechargeBankNapasResponse;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import org.bson.Document;

public interface RechargeDao {
    public List<RechargeByCardMessage> getListCardPending(String var1, String var2) throws NumberFormatException, KeyNotFoundException;

    public List<RechargeByCardMessage> getListCardPending() throws NumberFormatException, KeyNotFoundException;

    public boolean updateCard(String var1, int var2, int var3, String var4, int var5);

    public RechargeByBankMessage getRechargeByBank(String var1);

    public boolean logRechargeByBank(RechargeByBankMessage var1) throws Exception;

    public boolean updateRechargeByBank(String var1, String var2, String var3, String var4, String var5, String var6) throws Exception;

    public boolean insertLogRechargeByBankError(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10, String var11, String var12, String var13, String var14) throws Exception;

    public boolean logRechargeByNL(String var1, String var2, String var3, String var4, String var5, int var6, int var7, int var8, int var9, String var10, String var11, String var12, String var13, String var14, String var15, String var16, String var17);

    public boolean updateRechargeByNL(String var1, String var2, String var3);

    public boolean logRechargeByNLError(String var1, String var2, String var3);

    public LogRechargeBankNapasResponse getLogNapas(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, int var9);

    public LogRechargeBankNLResponse getLogNL(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, int var9);

    public boolean saveLogIAP(Purchase var1, String var2, int var3, int var4, String var5);

    public boolean checkOrderId(String var1);

    public long getTotalRechargeIapInday(String var1, Calendar var2) throws ParseException;

    public boolean checkRequestIdSMS(String var1);

    public boolean checkRequestIdSMSPlus(String var1);

    public boolean saveLogRechargeBySMS(String var1, String var2, String var3, int var4, String var5, String var6, String var7, int var8, String var9, int var10);

    public boolean saveLogRechargeBySMSPlus(String var1, String var2, String var3, int var4, String var5, String var6, String var7, String var8, String var9, int var10, String var11, int var12);

    public boolean saveLogRechargeBySMSPlusCheckMO(String var1, String var2, int var3, String var4, int var5, String var6);

    public boolean saveLogRequestApiOTP(String var1, String var2, int var3, String var4, String var5, String var6, String var7, int var8, String var9);

    public boolean saveLogConfirmApiOTP(String var1, String var2, int var3, String var4, String var5, String var6, String var7, String var8, int var9, String var10, int var11);

    public RechargeByCardMessage getPendingCardByReferenceId(String var1);

    public boolean insertLogUpdateCardPending(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10, String var11, String var12) throws Exception;

    public boolean isAgent(String var1) throws SQLException;

    public List<String> getListSmsIdNearly();

    public List<String> getListSmsPlusIdNearly();

    public boolean updateSMS(String var1, int var2, String var3, int var4);

    public Document getRechargeByGachthe(String var1);

    public Document getRechargeByGachthe(String var1, String var2);

    public Document getRechargeByGachthe(String var1, String var2, String var3);

    public List<Document> getRechargeByGachtheRecently();

    public boolean saveLogRechargeByGachThe(String var1, String var2, String var3, long var4, String var6, String var7, int var8, String var9, long var10, String var12, String var13, long var14, long var16, int var18, String var19, String var20, String var21);

    public boolean UpdateGachtheTransctions(String var1, int var2, String var3, long var4);

    public boolean UpdateGachtheTransctionsSent(String var1);

    public Document getRechargeByNapTienGa(String var1);

    public Document getRechargeByNapTienGa(String var1, String var2, String var3);

    public List<Document> getRechargeByNapTienGaRecently();

    public boolean saveLogRechargeByNapTienGa(String var1, String var2, String var3, long var4, String var6, String var7, int var8, String var9, long var10, String var12, String var13, long var14, long var16, int var18, String var19, int var20);

    public boolean UpdateNapTienGaTransctions(String var1, int var2, String var3);

    public boolean UpdateNapTienGaTransctionsSent(String var1);

    public boolean UpdateDepositBankManualStatus(String var1, int var2, String var3, String var4, long var5);

    public boolean isPendingTransDepositBank(String var1);

    public boolean UpdateDepositMomoManualStatus(String var1, int var2, String var3, String var4, long var5);

    public boolean isPendingTransDepositMomo(String var1);

    public String InsertDepositMomoManual(DepositMomoModel var1);

    public String InsertDepositMomoManualId(DepositMomoModel var1);

    public String InsertDepositBankManualId(DepositBankModel var1);

    public DepositMomoModel FindDepositMomoById(String var1);

    public DepositBankModel FindDepositBankById(String var1);
}

