/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.liveUser.dao;

import com.vinplay.liveUser.entities.LiveUserGameEntity;
import java.sql.SQLException;
import java.util.List;

public interface LiveUserGameDAO {
    public LiveUserGameEntity getByNickname(String var1) throws SQLException;

    public boolean create(LiveUserGameEntity var1) throws SQLException;

    public boolean update(LiveUserGameEntity var1) throws SQLException;

    public boolean delete(int var1, String var2) throws SQLException;

    public LiveUserGameEntity get(int var1) throws SQLException;

    public int count(String var1, String var2, String var3) throws SQLException;

    public List<LiveUserGameEntity> search(String var1, String var2, String var3, int var4, int var5) throws SQLException;
}

