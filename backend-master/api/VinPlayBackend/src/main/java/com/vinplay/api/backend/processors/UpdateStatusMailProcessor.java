/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;

public class UpdateStatusMailProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        String mailId = request.getParameter("mid");
        if (!mailId.isEmpty()) {
            MailBoxServiceImpl service = new MailBoxServiceImpl();
            int result = service.updateStatusMailBox(mailId);
            if (result == 0) {
                response.setErrorCode("0");
                response.setSuccess(true);
            } else {
                response.setErrorCode("10001");
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

