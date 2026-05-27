/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BauCuaReponseDetail
 *  com.vinplay.vbee.common.response.BauCuaResponse
 *  com.vinplay.vbee.common.response.BauCuaResultResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.BauCuaReponseDetail;
import com.vinplay.vbee.common.response.BauCuaResponse;
import com.vinplay.vbee.common.response.BauCuaResultResponse;
import java.util.List;

public interface LogBauCuaDAO {
    public List<BauCuaResponse> listLogBauCua(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public int countLogBauCua(String var1, String var2, String var3, String var4, String var5, String var6);

    public List<BauCuaReponseDetail> getLogBauCuaDetail(String var1, int var2);

    public int countLogBauCuaDetail(String var1);

    public List<BauCuaResultResponse> listLogBauCauResult(String var1, String var2, String var3, String var4, int var5);

    public long countLogBauCauResult(String var1, String var2, String var3, String var4);
}

