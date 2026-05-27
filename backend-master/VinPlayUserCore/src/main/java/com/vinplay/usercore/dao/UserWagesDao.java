/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.UserWages;
import java.sql.SQLException;
import java.util.Map;

public interface UserWagesDao {
    public String insert(UserWages var1) throws SQLException;

    public boolean insertByJob(String var1) throws SQLException;

    public String update(UserWages var1) throws SQLException;

    public String updateStatus(long var1, int var3) throws SQLException;

    public String updateAllStatusToReceivedBonus(String var1) throws SQLException;

    public long getSumBonusByStatus(String var1, int var2) throws SQLException;

    public UserWages getById(long var1) throws SQLException;

    public UserWages getByDate(String var1) throws SQLException;

    public Map<String, Object> history(String var1, String var2, String var3, int var4, int var5, int var6) throws SQLException;
}

