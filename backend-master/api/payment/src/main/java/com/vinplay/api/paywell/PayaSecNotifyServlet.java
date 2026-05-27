/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.service.impl.RechargePayaSecServiceImpl
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.paywell;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.service.impl.RechargePayaSecServiceImpl;
import com.vinplay.utils.RequestUtil;
import com.vinplay.vbee.common.response.BaseResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PayaSecNotifyServlet
extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PayaSecNotifyServlet.class);
    private static final long serialVersionUID = 1L;
    private static final List<String> IP_PAYASEC = Arrays.asList("144.202.102.152");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String remoteAddr = RequestUtil.getIpAddress(request);
            if (!IP_PAYASEC.contains(remoteAddr)) {
                logger.error((Object)("Remote IP Address IP_PAYASEC_NOTALLOW = " + remoteAddr));
                response.setStatus(403);
                response.getWriter().println(BaseResponse.error((String)"18", (String)"Can not allow accept this api"));
                return;
            }
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Map result = (Map)new Gson().fromJson(body, new TypeToken<HashMap<String, Object>>(){}.getType());
            String created = result.get("created") == null ? "" : result.get("created").toString();
            String updated = result.get("updated") == null ? "" : result.get("updated").toString();
            String refId = result.get("refId") == null ? "" : result.get("refId").toString();
            String refIdPartner = result.get("refIdPartner") == null ? "" : result.get("refIdPartner").toString();
            String gateway = result.get("gateway") == null ? "" : result.get("gateway").toString();
            String gatewayDetail = result.get("gatewayDetail") == null ? "" : result.get("gatewayDetail").toString();
            Double amount = Double.parseDouble(result.get("amount") == null ? "0" : result.get("amount").toString());
            Double fee = Double.parseDouble(result.get("fee") == null ? "0" : result.get("fee").toString());
            Double netAmount = Double.parseDouble(result.get("netAmount") == null ? "0" : result.get("netAmount").toString());
            Double status = Double.parseDouble(result.get("status") == null ? "0" : result.get("status").toString());
            String token = result.get("token") == null ? "" : result.get("token").toString();
            logger.info((Object)("Notify payasec" + body));
            if (StringUtils.isBlank((CharSequence)created)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"created can not empty"));
            }
            if (StringUtils.isBlank((CharSequence)updated)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"updated can not empty"));
            }
            if (StringUtils.isBlank((CharSequence)refId)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"refId can not empty"));
            }
            if (StringUtils.isBlank((CharSequence)refIdPartner)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"refIdPartner can not empty"));
            }
            if (StringUtils.isBlank((CharSequence)gateway)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"gateway can not empty"));
            }
            if (StringUtils.isBlank((CharSequence)gatewayDetail)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"gatewayDetail can not empty"));
            }
            if (amount == 0.0) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"amount is invalid"));
            }
            if (netAmount == 0.0) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"netAmount is invalid"));
            }
            if (StringUtils.isBlank((CharSequence)token)) {
                response.setStatus(400);
                response.getWriter().println(BaseResponse.error((String)"5", (String)"token can not empty"));
            }
            RechargePayaSecServiceImpl service = new RechargePayaSecServiceImpl();
            RechargePaywellResponse responseData = service.notification(created, updated, refId, refIdPartner, gateway, gatewayDetail, amount.longValue(), fee.longValue(), netAmount.longValue(), status.intValue(), token);
            logger.info((Object)("Notify payasec , response = " + responseData.toJson()));
            if (responseData.getCode() == 0) {
                response.setStatus(200);
                response.getWriter().println(responseData.toJson());
                return;
            }
            response.setStatus(400);
            response.getWriter().println(BaseResponse.error((String)"5", (String)responseData.getData()));
        }
        catch (Exception e) {
            logger.error((Object)e);
            response.setStatus(404);
            response.getWriter().println(BaseResponse.error((String)"99", (String)e.getMessage()));
        }
    }
}

