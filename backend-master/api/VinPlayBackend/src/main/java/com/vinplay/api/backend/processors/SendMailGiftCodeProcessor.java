/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.SendMailResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.SendMailResponse;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

public class SendMailGiftCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        SendMailResponse response = new SendMailResponse(true, "0", "");
        String nickName = request.getParameter("nn");
        String giftcode = request.getParameter("gc");
        String type = request.getParameter("type");
        String price = request.getParameter("gp");
        if (!nickName.isEmpty() && !giftcode.isEmpty()) {
            MailBoxServiceImpl service = new MailBoxServiceImpl();
            try {
                response = service.sendmailGiftCode(nickName, giftcode, type, price);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            if (response.isSuccess()) {
                response.setErrorCode("0");
                response.setSuccess(true);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

