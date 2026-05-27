/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors;

import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

public class CountMailStatusProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        String nn = request.getParameter("nn");
        if (nn != null && !nn.isEmpty()) {
            MailBoxServiceImpl service = new MailBoxServiceImpl();
            try {
                int mailnotread = service.countMailBoxInActive(nn);
                response.setErrorCode("0");
                response.setSuccess(true);
                response.setData(mailnotread);
                return response.toJson();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return "MISSING PARAMETTER";
    }
}

