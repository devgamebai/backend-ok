/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.dao;

import java.sql.SQLException;

public interface MoneySystemDao {
    public boolean updateMoneySystem(String var1, long var2) throws SQLException;

    public long getMoneySystem(String var1) throws SQLException;
}

