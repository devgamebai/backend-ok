/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.payment.entities.AdminBank;
import java.sql.SQLException;
import java.util.List;

public interface AdminBankDao {
    public List<AdminBank> search(String var1, String var2, String var3, String var4, int var5, int var6) throws SQLException;

    public int count(String var1, String var2, String var3, String var4) throws SQLException;

    public boolean insert(AdminBank var1) throws SQLException;

    public boolean update(AdminBank var1) throws SQLException;

    public boolean delete(long var1) throws SQLException;
}

