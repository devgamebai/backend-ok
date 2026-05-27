/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.entities.PaymentConfig
 *  com.vinplay.payment.service.impl.PaymentConfigServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.vinplay.api.processors.dto.ConfigUIDto;
import com.vinplay.api.processors.dto.PaymentConfigUiDto;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class GetPaymentConfigProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        block6: {
            HttpServletRequest request = (HttpServletRequest)param.get();
            String nickName = request.getParameter("nn");
            String accessToken = request.getParameter("at");
            String paymentType = request.getParameter("pt");
            logger.info(("Request GetPaymentConfig: nickName: " + nickName + ", accessToken: " + accessToken + ", paymentType: " + paymentType));
            UserServiceImpl userService = new UserServiceImpl();
            if (StringUtils.isBlank((CharSequence)nickName) || StringUtils.isBlank((CharSequence)accessToken)) {
                return BaseResponse.error((String)"10", (String)"input parameter is null or empty");
            }
            try {
                boolean isToken = userService.isActiveToken(nickName, accessToken);
                if (isToken) {
                    PaymentConfigServiceImpl payConfig = new PaymentConfigServiceImpl();
                    if (paymentType.equals("1")) {
                        ArrayList<PaymentConfigUiDto> lstConfig = new ArrayList<PaymentConfigUiDto>();
                        for (PaymentConfig paymentConfig : payConfig.getConfig()) {
                            ConfigUIDto dto = new ConfigUIDto(paymentConfig.getConfig());
                            PaymentConfigUiDto pInfo = new PaymentConfigUiDto(paymentConfig.getName(), dto);
                            lstConfig.add(pInfo);
                        }
                        return new BaseResponse().success(lstConfig);
                    }
                    break block6;
                }
                return BaseResponse.error((String)"11", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
            }
            catch (Exception e) {
                logger.error(e);
                return BaseResponse.error((String)"99", (String)e.getMessage());
            }
        }
        return BaseResponse.error((String)"", (String)"");
    }
}

