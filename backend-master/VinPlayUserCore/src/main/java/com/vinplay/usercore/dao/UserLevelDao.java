/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.UserLevel;
import java.sql.SQLException;
import java.util.Map;

public interface UserLevelDao {
    public String insert(UserLevel var1) throws SQLException;

    public String Update(UserLevel var1) throws SQLException;

    public UserLevel getByNickName(String var1, String var2) throws SQLException;

    public UserLevel getByNickName(String var1) throws SQLException;

    public UserLevel getById(long var1) throws SQLException;

    public Map<String, Object> findChilds(String var1, String var2, String var3, int var4, int var5) throws SQLException;
}

