package com.vinplay.api.backend.processors.creditwallet;

import com.vinplay.dal.service.CreditWalletService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9921 — Admin bơm tiền vào Credit Wallet của agent.
 * Path: /api_backend (yêu cầu admin access token)
 * Params:
 *   at  = admin access token
 *   nn  = agent nickname
 *   am  = amount (1 to 10,000,000)
 *   nt  = note (optional)
 *
 * Không cần duyệt, không cần OTP — admin action trực tiếp.
 */
public class AdminCreditWalletTopupProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // Validate admin token
            String accessToken = request.getParameter("at");
            if (isEmpty(accessToken)) accessToken = request.getParameter("aat");
            if (isEmpty(accessToken)) return fail(response, "1001", "Admin token required");

            com.hazelcast.core.IMap<String, String> tokenMap =
                    HazelcastClientFactory.getInstance().getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                return fail(response, "1001", "Invalid or expired admin token");
            }
            String adminNick = tokenMap.get(accessToken);

            // Params
            String agentNick = request.getParameter("nn");
            String amountStr = request.getParameter("am");
            String note = request.getParameter("nt");

            if (isEmpty(agentNick)) return fail(response, "4001", "Agent nickname (nn) required");
            if (isEmpty(amountStr)) return fail(response, "4001", "Amount (am) required");

            long amount;
            try { amount = Long.parseLong(amountStr); }
            catch (NumberFormatException e) { return fail(response, "4002", "Invalid amount format"); }

            // Resolve agent
            CreditWalletService.AgentInfo agent = CreditWalletService.getAgentInfoByNick(agentNick);
            if (agent == null) return fail(response, "1002", "Agent not found: " + agentNick);

            // Execute topup
            CreditWalletService.CreditResult result =
                    CreditWalletService.adminTopup(agent.id, agent.nickname, amount, adminNick, note);

            if (!result.success) return fail(response, result.errorCode, result.message);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("amount", amount);
            response.put("agent_nickname", agent.nickname);
            response.put("credit_balance", result.senderBalance);
            response.put("admin", adminNick);

        } catch (Exception e) {
            logger.error("AdminCreditWalletTopupProcessor error", e);
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
