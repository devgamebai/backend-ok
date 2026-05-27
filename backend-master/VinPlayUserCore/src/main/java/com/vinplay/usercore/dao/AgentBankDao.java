/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.AgentBank;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface AgentBankDao {
    public List<AgentBank> findAll();

    public AgentBank getById(long var1);

    public AgentBank getByBankNumber(String var1);

    public AgentBank getByBankCode(String var1, String var2);

    public boolean create(AgentBank var1);

    public boolean update(AgentBank var1);

    public boolean delete(long var1);

    public Map<String, Object> search(String var1, String var2, int var3, int var4) throws SQLException;
}

