/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.dao;

import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.entities.WithDrawPaygateReponse;
import com.vinplay.payment.model.WithDrawHistory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface WithDrawPaygateDao {
    public long countNumberWithdrawSuccessInDay(String var1);

    public boolean CheckPending(String var1, String var2);

    public WithDrawPaygateModel GetById(String var1);

    public long Add(WithDrawPaygateModel var1);

    public Boolean UpdateInfo(String var1, String var2, String var3, String var4, String var5, String var6);

    public Boolean UpdateInfo(String var1, String var2, String var3, String var4, String var5);

    public Boolean UpdateStatus(String var1, String var2, int var3, String var4, String var5);

    public Boolean UpdateStatus(String var1, String var2, int var3, String var4);

    public Boolean UpdateStatus(String var1, int var2, String var3);

    public Boolean UpdateRequestTime(String var1, String var2, String var3);

    public Boolean UpdateAmount(String var1, long var2, long var4, String var6);

    public WithDrawPaygateModel GetByReferenceId(String var1);

    public WithDrawPaygateModel GetByOrderId(String var1);

    public WithDrawPaygateReponse Find(WithDrawPaygateModel var1, int var2, int var3, String var4, String var5);

    public WithDrawPaygateReponse Find(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public WithDrawPaygateReponse Find(String var1, Integer var2, int var3, int var4, String var5, String var6, String var7, String var8, String var9, String var10, String var11, String var12, Double var13, Double var14);

    public ArrayList<Object> find(HashMap<String, Object> var1, int var2, int var3) throws Exception;

    public long count(HashMap<String, Object> var1);

    public Long[] statistic(HashMap<String, Object> var1);

    public ArrayList<WithDrawPaygateModel> GetRecevied(Integer var1);

    public Boolean UpdateStatus(String var1, int var2, String var3, String var4);

    public Map<String, Object> FindTransaction(String var1, int var2, int var3, int var4, String var5, String var6, String var7);

    public Map<String, Object> FindTransaction(String var1, String var2, int var3, int var4, int var5, String var6, String var7, String var8);

    public Boolean delete(String var1);

    public Boolean Update(WithDrawPaygateModel var1);

    public WithDrawHistory WithDrawHistoryWithNickName(String var1, int var2, int var3, int var4, String var5, String var6, String var7);
}

