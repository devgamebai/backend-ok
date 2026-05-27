/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.service.impl.RechargePayWellServiceImpl
 *  com.vinplay.payment.service.impl.WithDrawOneClickPayServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.payment.service.impl.RechargePayWellServiceImpl;
import com.vinplay.payment.service.impl.WithDrawOneClickPayServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class DepositHistory4UserplayProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(DepositHistory4UserplayProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        if (StringUtils.isBlank((CharSequence)nickname)) {
            return BaseResponse.error((String)"5", (String)"Nickname not empty");
        }
        String transactionType = request.getParameter("tt");
        if (transactionType == null || transactionType.trim().isEmpty()) {
            transactionType = "1";
        }
        if (!transactionType.equals("0") && !transactionType.equals("1")) {
            return BaseResponse.error((String)"5", (String)"Value of transaction type is invalid");
        }
        int status = -1;
        try {
            status = Integer.parseInt(request.getParameter("st"));
        }
        catch (Exception exception) {
            // empty catch block
        }
        int page = 0;
        try {
            page = Integer.parseInt(request.getParameter("p"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"5", (String)"Page index not empty");
        }
        int maxItem = 0;
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        }
        catch (Exception e) {
            return BaseResponse.error((String)"5", (String)"Limit item per page not empty");
        }
        if (page < 0) {
            return BaseResponse.error((String)"5", (String)"page <0");
        }
        if (maxItem < 0) {
            return BaseResponse.error((String)"5", (String)"maxItem <0");
        }
        String fromTime = request.getParameter("ft");
        String endTime = request.getParameter("et");
        String accessToken = request.getParameter("at");
        if (StringUtils.isBlank((CharSequence)accessToken)) {
            return BaseResponse.error((String)"5", (String)"AccessToken not empty");
        }
        logger.info(("Request payment history nickname= " + nickname + ", status: " + status + ", page: " + page + ", maxItem: " + maxItem + ", fromTime: " + fromTime + ", endTime: " + endTime + ", accessToken: " + accessToken + ", transactionType: " + transactionType));
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (isToken) {
            Map data = new HashMap();
            if (transactionType.equals("0")) {
                RechargePayWellServiceImpl rechargeService = new RechargePayWellServiceImpl();
                data = rechargeService.FindTransaction(nickname, status, page, maxItem, fromTime, endTime, "");
            }
            if (transactionType.equals("1")) {
                WithDrawOneClickPayServiceImpl withdrawService = new WithDrawOneClickPayServiceImpl();
                data = withdrawService.FindTransaction(nickname, status, page, maxItem, fromTime, endTime, "");
            }
            try {
                if (data.isEmpty()) {
                    return new BaseResponse(true, "0", null, null, 0L).toJson();
                }
                int totalRecord = Integer.parseInt(data.get("totalRecord").toString());
                data.remove("totalRecord");
                BaseResponse res = new BaseResponse(true, "0", null, data.get("data"), (long)totalRecord);
                return res.toJson();
            }
            catch (Exception e) {
                logger.error(e);
                return new BaseResponse(true, "1001", null, null, 0L).toJson();
            }
        }
        return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
    }
}

