/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.payment.entities.AgentTransaction;
import java.util.Map;

public interface AgentTransactionsService {
    public String create(AgentTransaction var1);

    public AgentTransaction getById(String var1);

    public String updateStatus(String var1, int var2, long var3, String var5, String var6);

    public String updateStatus(String var1, int var2, String var3, String var4);

    public String delete(String var1, String var2, String var3);

    public Map<String, Object> search(String var1, int var2, String var3, String var4, int var5);

    public Map<String, Object> searchWithAgentCode(String var1, int var2, String var3, String var4, int var5);
}

