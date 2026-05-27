/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.dao.impl.AgentDAOImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.api.entities.AgentTransferMoneyResponse;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class CheckTransactionProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String tid = request.getParameter("tid");
        AgentDAOImpl dao = new AgentDAOImpl();
        AgentTransferMoneyResponse resp = new AgentTransferMoneyResponse(404, "not found");
        LogAgentTranferMoneyResponse response = dao.searchAgentTranferMoney(tid);
        if (response != null && response.nick_name_send != null && response.nick_name_send != "") {
            resp.code = 0;
            resp.des_receive = response.des_receive;
            resp.des_send = response.des_send;
            resp.fee = response.fee;
            resp.message = "success";
            resp.money_receive = response.money_receive;
            resp.money_send = response.money_send;
            resp.nick_name_receive = response.nick_name_receive;
            resp.nick_name_send = response.nick_name_send;
            resp.process = response.process;
            resp.status = response.status;
            resp.top_ds = response.top_ds;
            resp.trans_id = tid;
            resp.trans_time = response.trans_time;
        }
        return resp.toJson();
    }
}

