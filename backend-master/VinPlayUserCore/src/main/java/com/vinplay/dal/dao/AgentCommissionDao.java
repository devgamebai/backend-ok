/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.agent.AgentCommissionDaily;
import java.util.List;

public interface AgentCommissionDao {
    public void upsert(AgentCommissionDaily var1);

    public void upsertBatch(List<AgentCommissionDaily> var1);

    public List<AgentCommissionDaily> searchByAgent(String var1, String var2, String var3, int var4, int var5);

    public long countByAgent(String var1, String var2, String var3);

    public List<AgentCommissionDaily> searchByUser(String var1, String var2, String var3, String var4, int var5, int var6);

    public long countByUser(String var1, String var2, String var3, String var4);
}

