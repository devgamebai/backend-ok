/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dichvuthe.response.RechargePaywellResponse
 *  com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl
 *  com.vinplay.payment.service.impl.RechargePayWellServiceImpl
 *  com.vinplay.payment.service.impl.RechargePrincePayServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.payment;

import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl;
import com.vinplay.payment.service.impl.RechargePayWellServiceImpl;
import com.vinplay.payment.service.impl.RechargePrincePayServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PaymentStatusProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(PaymentStatusProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String cartId = request.getParameter("ci");
        String providerName = request.getParameter("pn");
        if (StringUtils.isBlank((CharSequence)providerName)) {
            return BaseResponse.error((String)"5", (String)"Thi\u1ebfu t\u00ean c\u1ed5ng thanh to\u00e1n");
        }
        if (StringUtils.isBlank((CharSequence)cartId)) {
            return BaseResponse.error((String)"5", (String)"cartId is null or empty");
        }
        RechargePaywellResponse response = null;
        try {
            switch (providerName) {
                case "paywell": {
                    RechargePayWellServiceImpl pwellService = new RechargePayWellServiceImpl();
                    response = pwellService.checkStatusTrans(cartId);
                    break;
                }
                case "princepay": {
                    RechargePrincePayServiceImpl prinService = new RechargePrincePayServiceImpl();
                    response = prinService.checkStatusTrans(cartId);
                    break;
                }
                case "clickpay": {
                    RechargeOneClickPayServiceImpl clickService = new RechargeOneClickPayServiceImpl();
                    response = clickService.getDataTrans(cartId);
                }
            }
            return new BaseResponse(true, response.getCode() + "", response.getData(), response.getData()).toJson();
        }
        catch (Exception e) {
            logger.error(e);
            return BaseResponse.error((String)"99", (String)e.getMessage());
        }
    }
}

