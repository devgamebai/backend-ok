/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.dao;

import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.DepositPaygateReponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface RechargePaygateDao {
    public boolean isExistDeposit(String var1);

    public boolean CheckPending(String var1, String var2);

    public DepositPaygateModel GetById(String var1);

    public long topupByCash(DepositPaygateModel var1);

    public Boolean delete(String var1);

    public long Add(DepositPaygateModel var1);

    public Boolean Update(DepositPaygateModel var1);

    public Boolean Delete(String var1);

    public boolean updatePendingStatusToFailedAfterMinus(int var1, String var2);

    public Boolean UpdateStatus(String var1, String var2, int var3, String var4);

    public Boolean UpdateStatus(String var1, int var2, String var3);

    public Boolean UpdateStatus(String var1, String var2, int var3, String var4, String var5);

    public Boolean UpdateRequestTime(String var1, String var2, String var3);

    public Boolean UpdateAmount(String var1, long var2, long var4, String var6);

    public DepositPaygateModel GetByReferenceId(String var1);

    public DepositPaygateModel GetByOrderId(String var1);

    public DepositPaygateReponse Find(DepositPaygateModel var1, int var2, int var3, String var4, String var5);

    public DepositPaygateReponse Find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public List<Object> find(HashMap<String, Object> var1, int var2, int var3) throws Exception;

    public long count(HashMap<String, Object> var1);

    public Long[] statistic(HashMap<String, Object> var1);

    public DepositPaygateReponse Find(String var1, int var2, int var3, int var4, String var5, String var6, String var7, String var8, String var9, String var10, Double var11, Double var12);

    public Map<String, Object> FindTransaction(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public Map<String, Object> FindTransaction(String var1, String var2, int var3, int var4, int var5, String var6, String var7, String var8);

    public Map<String, Object> FindTransactionUserToAgent(String var1, String var2, int var3, int var4, int var5, String var6, String var7, String var8);

    public boolean UpdateTransactionDetail(String var1, String var2, String var3, int var4);
}

