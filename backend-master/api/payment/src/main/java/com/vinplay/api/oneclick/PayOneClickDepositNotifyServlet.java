/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.oneclick;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl;
import com.vinplay.utils.RequestUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class PayOneClickDepositNotifyServlet
extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PayOneClickDepositNotifyServlet.class);
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
        String remoteAddr = RequestUtil.getIpAddress(request);
        if (!"52.77.84.74".equalsIgnoreCase(remoteAddr)) {
            logger.error((Object)("Remote IP Address PayOneClickDepositNotifyServlet = " + remoteAddr));
            return;
        }
        String amount = request.getParameter("amount");
        String net_amount = request.getParameter("net_amount");
        String transactionId = request.getParameter("tnx");
        String orderId = request.getParameter("merchant_txn");
        String sign = request.getParameter("sign");
        logger.info((Object)("Notify oneclick , amount = " + amount + ",net_amount=" + net_amount + ",transactionId=" + transactionId + ",orderId=" + orderId + ",sign=" + sign));
        RechargeOneClickPayServiceImpl service = new RechargeOneClickPayServiceImpl();
        RechargePaywellResponse responseData = service.notify(amount, net_amount, transactionId, orderId, sign);
        if (responseData.getCode() == 0) {
            response.getWriter().println("VERIFIED");
        } else {
            response.getWriter().println("FAILED");
        }
    }
}

