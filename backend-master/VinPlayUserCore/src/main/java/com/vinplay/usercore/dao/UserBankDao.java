/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.payment.entities.UserBank;
import java.sql.SQLException;
import java.util.List;

public interface UserBankDao {
    public boolean isExistBankNumber(String var1) throws SQLException;

    public boolean isAddedBank(String var1);

    public long add(UserBank var1) throws SQLException;

    public boolean update(UserBank var1) throws SQLException;

    public boolean delete(long var1) throws SQLException;

    public UserBank getById(Long var1) throws SQLException;

    public UserBank getByBankNumber(String var1, String var2) throws SQLException;

    public int getCountByNickName(String var1) throws SQLException;

    public List<UserBank> getByCustomerName(String var1, String var2) throws SQLException;

    public List<UserBank> search(String var1, String var2, String var3, int var4, int var5, int var6) throws SQLException;

    public int search_count(String var1, String var2, String var3, int var4) throws SQLException;

    public List<UserBank> search(String var1, String var2, String var3, String var4, int var5, int var6, int var7) throws SQLException;

    public int search_count(String var1, String var2, String var3, String var4, int var5) throws SQLException;
}

