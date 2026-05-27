/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogRefundFeeAgentProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        String res = "";
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String month = request.getParameter("month");
        if (nickname != null && month != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List list = new ArrayList();
                AgentDAOImpl dao = new AgentDAOImpl();
                if (!month.isEmpty() && month.startsWith("0")) {
                    month = month.substring(1);
                }
                list = dao.getLogRefundFeeAgent(nickname, month);
                res = mapper.writeValueAsString(list);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return res;
    }
}

