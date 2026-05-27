/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 */
package com.vinplay.api.processors.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.payment.service.impl.RechargeOneClickPayServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public class GetBanksOneClickPayProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> var1) {
        HttpServletRequest request = (HttpServletRequest)var1.get();
        String providerName = request.getParameter("pn");
        if (StringUtils.isBlank((CharSequence)providerName)) {
            return BaseResponse.error((String)"5", (String)"Nh\u00e0 cung c\u1ea5p kh\u00f4ng \u0111\u00fang");
        }
        switch (providerName) {
            case "paywell": {
                break;
            }
            case "princepay": {
                break;
            }
            case "clickpay": {
                RechargeOneClickPayServiceImpl oneClick = new RechargeOneClickPayServiceImpl();
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                try {
                    return ow.writeValueAsString(oneClick.getLstOneClickBank());
                }
                catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}

