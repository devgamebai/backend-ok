/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.taixiu.LogTaiXiuSieuToc;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface LogTaiXiuSieuTocDAO {
    public List<LogTaiXiuSieuToc> search(String var1, String var2, int var3, int var4, int var5) throws SQLException;

    public Map<String, Object> getDetailByLogId(long var1, String var3, String var4, int var5, int var6, int var7, String var8, int var9, int var10) throws SQLException;
}

