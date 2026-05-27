/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.core.common.StringUtil
 *  com.payment.model.Code
 *  com.payment.model.Result
 *  com.payment.service.impl.ProviderServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 */
package com.vinplay.api;

import com.payment.core.common.StringUtil;
import com.payment.model.Code;
import com.payment.model.Result;
import com.payment.service.impl.ProviderServiceImpl;
import com.vinplay.response.BasePortalResponse;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ListBankServlet
extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String nickName = request.getParameter("nick_name");
        String token = request.getParameter("token");
        boolean errorResult = StringUtil.handleBlankParams((String[])new String[]{nickName, token});
        if (errorResult) {
            response.setStatus(400);
            response.getWriter().println(BasePortalResponse.New(1, "Kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng c\u00e1c tr\u01b0\u1eddng!").toJson());
            return;
        }
        try {
            UserServiceImpl userService = new UserServiceImpl();
            boolean isToken = userService.isActiveToken(nickName, token);
            if (isToken) {
                Result result = ProviderServiceImpl.getInstance().bankList("default");
                if (result.getCode() == Code.SUCCESS) {
                    response.setStatus(200);
                    response.getWriter().println(BasePortalResponse.Success(0, result.getData()).toJson());
                    return;
                }
                response.setStatus(400);
                response.getWriter().println(BasePortalResponse.New(1, result.getDataRaw()).toJson());
                return;
            }
            response.setStatus(400);
            response.getWriter().println(BasePortalResponse.New(1, "user not login").toJson());
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().println(BasePortalResponse.New(1, "Server Error").toJson());
        }
    }
}

