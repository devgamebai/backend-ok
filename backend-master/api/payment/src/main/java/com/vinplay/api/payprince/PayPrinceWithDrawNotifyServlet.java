/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.service.impl.WithDrawPrincePayServiceImpl
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.payprince;

import com.vinplay.payment.service.impl.WithDrawPrincePayServiceImpl;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PayPrinceWithDrawNotifyServlet
extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PayPrinceWithDrawNotifyServlet.class);
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
        String status = request.getParameter("status");
        String result = request.getParameter("result");
        String sign = request.getParameter("sign");
        logger.info((Object)("Notify princepay , status = " + status + ",result=" + result + ",sign=" + sign));
        WithDrawPrincePayServiceImpl service = new WithDrawPrincePayServiceImpl();
        int statusInt = 0;
        try {
            statusInt = Integer.parseInt(status);
        }
        catch (NumberFormatException e) {
            logger.error((Object)e);
            return;
        }
        if (StringUtils.isBlank((CharSequence)result) || StringUtils.isBlank((CharSequence)sign)) {
            return;
        }
        boolean responseData = service.notify(statusInt, result, sign);
        if (responseData) {
            response.getWriter().println("{\"get\":\"success\"}");
        } else {
            response.getWriter().println("{\"get\":\"false\"}");
        }
    }
}

