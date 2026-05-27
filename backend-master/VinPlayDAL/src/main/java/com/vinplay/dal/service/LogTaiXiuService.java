/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.TaiXiuDetailReponse
 *  com.vinplay.vbee.common.response.TaiXiuResponse
 *  com.vinplay.vbee.common.response.TaiXiuResultResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.TaiXiuDetailReponse;
import com.vinplay.vbee.common.response.TaiXiuResponse;
import com.vinplay.vbee.common.response.TaiXiuResultResponse;
import java.sql.SQLException;
import java.util.List;

public interface LogTaiXiuService {
    public List<TaiXiuResponse> listLogTaiXiu(String var1, String var2, String var3, String var4, String var5, String var6, int var7) throws SQLException;

    public int countLogTaiXiu(String var1, String var2, String var3, String var4, String var5, String var6) throws SQLException;

    public List<TaiXiuDetailReponse> getLogTaiXiuDetail(String var1, String var2, String var3, String var4, int var5) throws SQLException;

    public int countLogTaiXiuDetail(String var1, String var2, String var3, String var4) throws SQLException;

    public List<TaiXiuResultResponse> listLogTaiXiuResult(String var1, String var2, String var3, String var4, int var5) throws SQLException;

    public int countLogTaiXiuResult(String var1, String var2, String var3, String var4) throws SQLException;
}

