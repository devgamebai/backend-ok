/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.ListAgentLevel2ServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.AgentLevel2Response
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.ListAgentLevel2ServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.AgentLevel2Response;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ListAgentLevel2Processor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        AgentLevel2Response response = new AgentLevel2Response(false, "10001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        if (nickName == null) {
            return "MISSING INPUT PARAMETER";
        }
        try {
            ListAgentLevel2ServiceImpl service = new ListAgentLevel2ServiceImpl();
            List listAgent = service.listAgentLevel2(nickName);
            response.setListAgent(listAgent);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}

