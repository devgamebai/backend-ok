/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.AgentBank;
import java.util.Map;

public interface AgentBankService {
    public String create(AgentBank var1);

    public AgentBank getById(long var1);

    public AgentBank getByBankNumber(String var1);

    public AgentBank getByBankCode(String var1, String var2);

    public String update(AgentBank var1);

    public String Delete(long var1);

    public Map<String, Object> search(String var1, String var2, int var3, int var4);
}

