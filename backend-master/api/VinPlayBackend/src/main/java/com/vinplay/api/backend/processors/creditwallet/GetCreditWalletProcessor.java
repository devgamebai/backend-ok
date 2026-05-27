package com.vinplay.api.backend.processors.creditwallet;

import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
import com.vinplay.dal.service.CreditWalletService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9920 — Lấy balance Credit Wallet của agent.
 * Path: /api_agent hoặc /api_backend
 * Params: rc (agent code) HOẶC nn (agent nickname)
 *
 * Response:
 *   { success:true, errorCode:"0", agent_id:int, nick_name:str, credit_wallet:long }
 */
public class GetCreditWalletProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentCode = request.getParameter("rc");
            String agentNick = request.getParameter("nn");

            if (isEmpty(agentCode) && isEmpty(agentNick)) {
                return fail(response, "4001", "rc (agent code) or nn (agent nickname) required");
            }

            // Resolve agent
            CreditWalletService.AgentInfo agent = !isEmpty(agentCode)
                    ? CreditWalletService.getAgentInfoByCode(agentCode)
                    : CreditWalletService.getAgentInfoByNick(agentNick);

            if (agent == null) {
                return fail(response, "1002", "Agent not found");
            }

            CreditWalletDao dao = new CreditWalletDaoImpl();
            long balance = dao.getBalance(agent.id);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("agent_id", agent.id);
            response.put("nick_name", agent.nickname);
            response.put("credit_wallet", balance);

        } catch (Exception e) {
            logger.error("GetCreditWalletProcessor error", e);
            return fail(response, "1001", "Internal error");
        }
        return response.toString();
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private String fail(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
