/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.GetUserResponse
 *  com.vinplay.vbee.common.response.ResultGetUserResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.GetUserResponse;
import com.vinplay.vbee.common.response.ResultGetUserResponse;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

public class GetTotalVinByUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultGetUserResponse response = new ResultGetUserResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        if (nickName != null && !nickName.equals("")) {
            UserServiceImpl service = new UserServiceImpl();
            try {
                UserModel userModel = service.getUserByNickName(nickName);
                GetUserResponse trans = new GetUserResponse();
                trans.totalVin = userModel.getVinTotal();
                trans.safe = userModel.getSafe();
                trans.vippoint = userModel.getVippoint();
                trans.vippointsave = userModel.getVippointSave();
                response.setTransactions(trans);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

