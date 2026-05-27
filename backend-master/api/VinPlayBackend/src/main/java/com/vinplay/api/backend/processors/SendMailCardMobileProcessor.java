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

public class SendMailCardMobileProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        SendMailResponse response = new SendMailResponse(true, "0", "");
        String title = request.getParameter("tm");
        String content = request.getParameter("cm");
        String serial = request.getParameter("sr");
        String pin = request.getParameter("pn");
        String nickName = request.getParameter("nn");
        MailBoxServiceImpl service = new MailBoxServiceImpl();
        if (!(title == null || title.isEmpty() || content == null || content.equals("") || serial == null || serial.equals("") || pin == null || pin.equals("") || nickName == null || nickName.equals(""))) {
            if (content.contains("%s")) {
                String contentCardMobile = "";
                String lsNickName = "";
                if (nickName.contains(",") && serial.contains(",") && pin.contains(",")) {
                    String[] arrNN = nickName.split(",");
                    String[] arrSr = serial.split(",");
                    String[] arrPn = pin.split(",");
                    if (arrNN.length == arrSr.length) {
                        for (int i = 0; i < arrNN.length; ++i) {
                            String nn = arrNN[i].trim();
                            String ser = arrSr[i].trim();
                            String pn = arrPn[i].trim();
                            contentCardMobile = String.format(content, nn, ser, pn);
                            try {
                                response = service.sendMailCardMobile(nn, ser, pn, title, contentCardMobile, lsNickName);
                                lsNickName = String.valueOf(lsNickName) + response.getNickName();
                                continue;
                            }
                            catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (arrNN.length > arrSr.length || arrNN.length > arrPn.length) {
                        response.setErrorCode("7002");
                        response.setSuccess(false);
                    }
                    if (arrSr.length > arrNN.length || arrPn.length > arrNN.length) {
                        response.setErrorCode("7003");
                        response.setSuccess(false);
                    }
                    if (arrSr.length > arrPn.length) {
                        response.setErrorCode("7004");
                        response.setSuccess(false);
                    }
                    if (arrPn.length > arrSr.length) {
                        response.setErrorCode("7005");
                        response.setSuccess(false);
                    }
                    if (response.isSuccess()) {
                        response.setErrorCode("0");
                        response.setSuccess(true);
                    }
                } else {
                    contentCardMobile = String.format(content, nickName, serial, pin);
                    try {
                        response = service.sendMailCardMobile(nickName, serial, pin, title, contentCardMobile, lsNickName);
                    }
                    catch (SQLException e2) {
                        e2.printStackTrace();
                    }
                }
            } else {
                response.setErrorCode("10001");
                response.setSuccess(false);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

