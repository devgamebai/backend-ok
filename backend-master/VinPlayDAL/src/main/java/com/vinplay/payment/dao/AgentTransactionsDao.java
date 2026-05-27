/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.AgentResponse
 *  org.bson.Document
 */
package com.vinplay.payment.dao;

import com.vinplay.payment.entities.AgentTransaction;
import com.vinplay.vbee.common.response.AgentResponse;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public interface AgentTransactionsDao {
    public long create(AgentTransaction var1);

    public Boolean updateStatus(String var1, int var2, long var3, String var5, String var6);

    public Boolean updateStatus(String var1, int var2, String var3, String var4);

    public Boolean delete(String var1, String var2, String var3);

    public AgentTransaction getById(String var1);

    public Map<String, Object> search(String var1, int var2, String var3, String var4, int var5);

    public Map<String, Object> searchWithAgentCode(String var1, int var2, String var3, String var4, int var5);

    public List<Document> getTotalTransferOutTransaction(List<AgentResponse> var1, String var2, String var3);

    public List<Document> getTotalTransferInTransaction(List<AgentResponse> var1, String var2, String var3);
}

