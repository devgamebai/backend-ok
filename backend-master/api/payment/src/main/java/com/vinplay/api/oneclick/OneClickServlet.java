/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.oneclick;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl;
import com.vinplay.vbee.common.response.BaseResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class OneClickServlet
extends HttpServlet {
    private static final Logger logger = Logger.getLogger(OneClickServlet.class);
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        String userId = request.getParameter("userId");
        String username = request.getParameter("username");
        String nickname = request.getParameter("nickname");
        String customerName = request.getParameter("customerName");
        String bankCode = request.getParameter("bankCode");
        String channel = request.getParameter("channel");
        String ip = request.getParameter("ip");
        String res = "";
        long amount = 0L;
        try {
            amount = Long.parseLong(request.getParameter("amount"));
        }
        catch (Exception e) {
            res = "{\"code\":99,\"message\":\"" + e.getMessage() + "\"}";
        }
        try {
            RechargeOneClickPayServiceImpl service = new RechargeOneClickPayServiceImpl();
            RechargePaywellResponse rechargeResponse = service.createTransaction(userId, username, nickname, amount, channel, customerName, bankCode, ip);
            res = rechargeResponse.getCode() == 0 ? "{\"code\":1,\"message\":\"success\"}" : "{\"code\":0,\"message\":\"" + rechargeResponse.getData() + "\"}";
        }
        catch (Exception e) {
            logger.error((Object)e);
            res = BaseResponse.error((String)"99", (String)e.getMessage());
        }
        response.getWriter().println(res);
    }
}

