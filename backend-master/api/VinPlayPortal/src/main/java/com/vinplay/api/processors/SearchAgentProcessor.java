/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.dal.entities.agent.UserAgentModel
 *  com.vinplay.dal.utils.AgentUtils
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors;

import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserAgentModel;
import com.vinplay.dal.utils.AgentUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

public class SearchAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        int maxItem;
        int page;
        HttpServletRequest request = (HttpServletRequest)param.get();
        String keyword = request.getParameter("key");
        String code = request.getParameter("code");
        AgentDAOImpl dao = new AgentDAOImpl();
        UserAgentModel currentAgent = new UserAgentModel();
        try {
            currentAgent = dao.DetailUserAgentByCode(code);
        }
        catch (SQLException e) {
            currentAgent = null;
        }
        try {
            page = Integer.parseInt(request.getParameter("pg"));
        }
        catch (NumberFormatException e) {
            page = 1;
        }
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        }
        catch (NumberFormatException e) {
            maxItem = 10;
        }
        String levelStr = request.getParameter("lv");
        int level = currentAgent == null ? -1 : currentAgent.getLevel() + 1;
        try {
            level = Integer.parseInt(levelStr);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            return AgentUtils.searchChilds((int)(currentAgent == null ? -1 : currentAgent.getId()), (String)keyword, (int)level, (int)page, (int)maxItem).toJson();
        }
        catch (Exception e) {
            return BaseResponse.success(new ArrayList(), (long)0L);
        }
    }
}

