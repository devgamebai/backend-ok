/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.liveUser.dao;

import com.vinplay.liveUser.entities.LiveUserDepositEntity;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public interface LiveUserDepositDAO {
    public boolean create(LiveUserDepositEntity var1) throws SQLException;

    public List<LiveUserDepositEntity> runNow(Date var1) throws SQLException;

    public boolean setRan(int var1) throws SQLException;

    public int count(String var1, String var2, String var3, String var4) throws SQLException;

    public Long sum(String var1, String var2, String var3, String var4) throws SQLException;

    public List<LiveUserDepositEntity> search(String var1, String var2, String var3, String var4, int var5, int var6) throws SQLException;
}

