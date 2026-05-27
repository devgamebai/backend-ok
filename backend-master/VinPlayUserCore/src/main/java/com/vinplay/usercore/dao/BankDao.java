/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.payment.entities.Bank;
import java.sql.SQLException;
import java.util.List;

public interface BankDao {
    public boolean addBank(Bank var1);

    public boolean editBank(Bank var1);

    public boolean deleteBank(long var1);

    public Bank get(long var1);

    public Bank getByBankCode(String var1);

    public List<Bank> search(String var1, String var2, int var3, int var4) throws SQLException;

    public int count(String var1, String var2) throws SQLException;

    public List<Bank> findAll();
}

