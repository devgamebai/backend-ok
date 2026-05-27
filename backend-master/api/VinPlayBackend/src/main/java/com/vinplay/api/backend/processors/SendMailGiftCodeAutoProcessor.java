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

public class SendMailGiftCodeAutoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        SendMailResponse response = new SendMailResponse(true, "1", "");
        String title = request.getParameter("tm");
        String content = request.getParameter("cm");
        String giftcode = request.getParameter("gc");
        String nickName = request.getParameter("nn");
        MailBoxServiceImpl service = new MailBoxServiceImpl();
        if (!(title == null || title.isEmpty() || content == null || content.isEmpty() || giftcode == null || giftcode.isEmpty() || nickName == null || nickName.isEmpty())) {
            if (content.contains("%s")) {
                String contentGiftCode = "";
                if (nickName.contains(",") && giftcode.contains(",")) {
                    String[] arrGC;
                    String[] arrNN = nickName.split(",");
                    if (arrNN.length == (arrGC = giftcode.split(",")).length) {
                        for (int i = 0; i < arrNN.length; ++i) {
                            String nn = arrNN[i].trim();
                            String gc = arrGC[i].trim();
                            contentGiftCode = String.format(content, nn, gc);
                            try {
                                response = service.sendMailGiftCode(nn, gc, title, contentGiftCode);
                                continue;
                            }
                            catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (arrNN.length > arrGC.length) {
                        response.setErrorCode("10002");
                        response.setSuccess(false);
                    }
                    if (arrGC.length > arrNN.length) {
                        response.setErrorCode("10003");
                        response.setSuccess(false);
                    }
                    if (response.isSuccess()) {
                        response.setErrorCode("0");
                        response.setSuccess(true);
                    }
                } else {
                    contentGiftCode = String.format(content, nickName, giftcode);
                    try {
                        response = service.sendMailGiftCode(nickName, giftcode, title, contentGiftCode);
                    }
                    catch (SQLException e2) {
                        e2.printStackTrace();
                    }
                }
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

