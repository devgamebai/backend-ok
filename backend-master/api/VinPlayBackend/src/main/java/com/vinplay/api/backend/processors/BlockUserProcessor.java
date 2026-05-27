/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.BlockUserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultBlockUserResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.BlockUserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultBlockUserResponse;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

public class BlockUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultBlockUserResponse response = new ResultBlockUserResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String action = request.getParameter("ac");
        String type = request.getParameter("type");
        BlockUserServiceImpl service = new BlockUserServiceImpl();
        String lst = null;
        try {
            lst = service.listBlockUser(nickName, action, type);
            if (lst != null && !lst.equals("")) {
                response.setNickName(lst);
                response.setSuccess(false);
                response.setErrorCode("10001");
            } else {
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}

