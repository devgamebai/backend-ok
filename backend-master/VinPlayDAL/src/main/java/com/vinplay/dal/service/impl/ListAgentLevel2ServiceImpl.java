/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.ListAgentLevel2DAOImpl;
import com.vinplay.dal.service.ListAgentLevel2Service;
import java.sql.SQLException;
import java.util.List;

public class ListAgentLevel2ServiceImpl
implements ListAgentLevel2Service {
    @Override
    public List<String> listAgentLevel2(String nickName) throws SQLException {
        ListAgentLevel2DAOImpl dao = new ListAgentLevel2DAOImpl();
        return dao.listAgentLevel2(nickName);
    }

    @Override
    public String getAgentLevel1ByNickName(String nickName) throws SQLException {
        ListAgentLevel2DAOImpl dao = new ListAgentLevel2DAOImpl();
        return dao.getAgentLevel1ByNickName(nickName);
    }
}

