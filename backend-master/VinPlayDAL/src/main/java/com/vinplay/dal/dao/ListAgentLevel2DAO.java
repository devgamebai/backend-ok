/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.dao;

import java.sql.SQLException;
import java.util.List;

public interface ListAgentLevel2DAO {
    public List<String> listAgentLevel2(String var1) throws SQLException;

    public String getAgentLevel1ByNickName(String var1) throws SQLException;
}

