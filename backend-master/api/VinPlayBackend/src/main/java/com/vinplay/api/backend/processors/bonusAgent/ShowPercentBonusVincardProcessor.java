/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.AgentServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.bonusAgent.ShowPercentBonusVincardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.bonusAgent;

import com.vinplay.dal.service.impl.AgentServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.bonusAgent.ShowPercentBonusVincardResponse;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ShowPercentBonusVincardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ShowPercentBonusVincardResponse response = new ShowPercentBonusVincardResponse(false, "1001");
        AgentServiceImpl service = new AgentServiceImpl();
        try {
            List listPercentBonus = service.getListPercentBonusVincard("all");
            response.setSuccess(true);
            response.setErrorCode("0");
            response.setListPercentBonus(listPercentBonus);
            return response.toJson();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return response.toJson();
        }
    }
}

