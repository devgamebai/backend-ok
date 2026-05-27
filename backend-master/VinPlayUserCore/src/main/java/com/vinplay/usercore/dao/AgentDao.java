/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.dao;

import java.sql.SQLException;

public interface AgentDao {
    public boolean checkSMSAgent(String var1, long var2) throws SQLException;

    public String getNicknameAgent1(String var1) throws SQLException;

    public String getAgentLevel1ByNickName(String var1) throws SQLException;
}

