/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.AgentModel
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse
 *  com.vinplay.vbee.common.response.TranferAgentResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.models.AgentModel;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse;
import com.vinplay.vbee.common.response.TranferAgentResponse;
import java.sql.SQLException;
import java.util.List;

public interface AgentService {
    public List<AgentResponse> listAgent() throws SQLException;

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6, int var7, String var8);


    public List<AgentResponse> listUserAgent(String var1) throws SQLException;

    public List<TranferAgentResponse> searchAgentTranfer(String var1, String var2, String var3, String var4, double var5, double var7, double var9, long var11) throws SQLException;

    public long countsearchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6);

    public long countsearchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6, String var7);

    public long totalMoneyVinReceiveFromAgent(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinSendFromAgent(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinFeeFromAgent(String var1, String var2, String var3, String var4, String var5, String var6);

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoneyVinSale(String var1, String var2, String var3, String var4, int var5, int var6);

    public long countSearchAgentTranferMoneyVinSale(String var1, String var2, String var3, String var4);

    public long totalMoneyVinReceiveFromAgentByStatus(String var1, String var2, String var3, String var4);

    public long totalMoneyVinSendFromAgentByStatus(String var1, String var2, String var3, String var4);

    public boolean updateTopDsFromAgent(String var1, String var2, String var3, String var4);

    public List<TranferAgentResponse> searchAgentTranferAdmin(String var1, String var2, String var3, String var4, double var5, double var7, double var9, long var11) throws SQLException;

    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5, int var6);

    public long countsearchAgentTranferMoney(String var1, String var2, String var3, String var4, String var5);

    public long totalMoneyVinReceiveFromAgent(String var1, String var2, String var3, String var4, String var5);

    public long totalMoneyVinSendFromAgent(String var1, String var2, String var3, String var4, String var5);

    public long totalMoneyVinFeeFromAgent(String var1, String var2, String var3, String var4, String var5);

    public List<AgentResponse> listUserAgentActive(String var1) throws SQLException;

    public List<AgentResponse> listUserAgentLevel1Active() throws SQLException;

    public List<LogAgentTranferMoneyResponse> searchAgentTongTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6, int var7);

    public long countsearchAgentTongTranferMoney(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinReceiveFromAgentTong(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinSendFromAgentTong(String var1, String var2, String var3, String var4, String var5, String var6);

    public long totalMoneyVinFeeFromAgentTong(String var1, String var2, String var3, String var4, String var5, String var6);

    public List<AgentModel> getListPercentBonusVincard(String var1) throws SQLException;

    public int registerPercentBonusVincard(String var1, int var2) throws SQLException;
}

