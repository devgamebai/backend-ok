/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultUserInfoModel
 *  com.vinplay.vbee.common.response.UserInfoModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultUserInfoModel;
import com.vinplay.vbee.common.response.UserInfoModel;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetUserInfoByPhoneProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        ResultUserInfoModel response = new ResultUserInfoModel(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String phone = request.getParameter("m");
        String lstExt = "";
        String lstNExt = "";
        UserServiceImpl service = new UserServiceImpl();
        try {
            String[] arrPhone;
            List<UserInfoModel> trans = service.checkPhoneByUser(phone);
            response.setTransactions(trans);
            for (UserInfoModel user : trans) {
                lstExt = String.valueOf(lstExt) + user.mobile + ",";
            }
            for (String i : arrPhone = phone.split(",")) {
                if (lstExt.contains(i)) continue;
                lstNExt = String.valueOf(lstNExt) + i + ",";
            }
            response.setLstPhone(lstNExt);
            response.setErrorCode("0");
            response.setSuccess(true);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}

