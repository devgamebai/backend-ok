/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.UserMissionServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.userMission.LogReceivedRewardResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.userMission;

import com.vinplay.dal.service.impl.UserMissionServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.userMission.LogReceivedRewardResponse;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetLogReceivedRewardProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LogReceivedRewardResponse response = new LogReceivedRewardResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String gameName = request.getParameter("gn");
        String moneyType = request.getParameter("mt");
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String page = request.getParameter("p");
        if (page == null || page.isEmpty()) {
            page = "1";
        }
        try {
            UserMissionServiceImpl service = new UserMissionServiceImpl();
            List trans = service.getLogReceivedReward(nickName, gameName, moneyType, timeStart, timeEnd, Integer.parseInt(page));
            response.setSuccess(true);
            response.setErrorCode("0");
            response.setTransactions(trans);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (SQLException e2) {
            e2.printStackTrace();
        }
        return response.toJson();
    }
}

