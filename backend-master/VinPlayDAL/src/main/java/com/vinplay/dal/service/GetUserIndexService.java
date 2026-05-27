/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.service;

import java.sql.SQLException;

public interface GetUserIndexService {
    public int getRegister(String var1, String var2, String var3) throws SQLException;

    public int getRecharge(String var1, String var2, String var3) throws SQLException;

    public int getSecMobile(String var1, String var2, String var3) throws SQLException;

    public int getBoth(String var1, String var2, String var3) throws SQLException;
}

