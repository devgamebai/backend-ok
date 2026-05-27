/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.bonusAgent.RegisPercentBonusVincardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.bonusAgent;

import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.bonusAgent.RegisPercentBonusVincardResponse;
import javax.servlet.http.HttpServletRequest;

public class RegisPercentBonusVincardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        String[] percentSub;
        RegisPercentBonusVincardResponse response = new RegisPercentBonusVincardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String percent = request.getParameter("per");
        if (nickName == null || nickName.isEmpty() || percent == null || percent.isEmpty()) {
            return "MISSING PARAMETTER";
        }
        String regex = "\\d+";
        if (!nickName.contains(",") || !percent.contains(",")) {
            if (percent.matches("\\d+") && Integer.parseInt(percent) >= 10 && Integer.parseInt(percent) <= 100) {
                AgentServiceImpl service = new AgentServiceImpl();
                try {
                    int recordNumber = service.registerPercentBonusVincard(nickName, Integer.parseInt(percent));
                    if (recordNumber > 0) {
                        response.setSuccess(true);
                        response.setErrorCode("0");
                        return response.toJson();
                    }
                    response.setErrorCode("1045");
                    return response.toJson();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return response.toJson();
                }
            }
            response.setErrorCode("1044");
            return response.toJson();
        }
        String[] nickNameSub = nickName.split(",");
        if (nickNameSub.length != (percentSub = percent.split(",")).length) {
            response.setErrorCode("1046");
            return response.toJson();
        }
        for (int i = 0; i < nickNameSub.length; ++i) {
            if (!percentSub[i].matches("\\d+") || Integer.parseInt(percentSub[i]) < 0 || Integer.parseInt(percentSub[i]) > 100) {
                response.setErrorCode("1044");
                break;
            }
            AgentServiceImpl service2 = new AgentServiceImpl();
            try {
                int recordNumber2 = service2.registerPercentBonusVincard(nickNameSub[i], Integer.parseInt(percentSub[i]));
                if (recordNumber2 > 0) {
                    response.setSuccess(true);
                    response.setErrorCode("0");
                    continue;
                }
                response.setErrorCode("1045");
            }
            catch (Exception e2) {
                e2.printStackTrace();
                response.setSuccess(false);
                response.setErrorCode("1001");
            }
            break;
        }
        return response.toJson();
    }
}

