/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.config.Config
 *  com.payment.config.PaymentConfigLoad
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 */
package com.vinplay.api;

import com.payment.config.Config;
import com.payment.config.PaymentConfigLoad;
import com.vinplay.vbee.common.response.BaseResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class GetListProviderSupportWithdrawServlet
extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(GetListProviderSupportWithdrawServlet.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List lstPConfigs = PaymentConfigLoad.getPaymentConfig().getProviders();
            ArrayList<Provider> uniProvider = new ArrayList<Provider>();
            uniProvider.add(new Provider("default", "C\u1ed5ng M\u1eb7c \u0111\u1ecbnh", 1));
            int index = 2;
            for (Object _conf : lstPConfigs) {
                Config conf = (Config) _conf;
                if (!conf.getEnable().booleanValue() || !conf.getAvailable().contains("bankOut")) continue;
                uniProvider.add(new Provider(conf.getKey(), conf.getName(), index));
                ++index;
            }
            uniProvider.add(new Provider("manualbank", "Chuy\u1ec3n kho\u1ea3n b\u1eb1ng tay", index));
            uniProvider.sort((o1, o2) -> o1.index - o2.index);
            response.getWriter().print(new BaseResponse().success(uniProvider));
        }
        catch (Exception e) {
            logger.error((Object)e);
            response.getWriter().print(BaseResponse.error((String)"99", (String)e.getMessage()));
        }
    }

    static class Provider {
        public String provider;
        public String name;
        public int index;

        public Provider(String provider, String name, int index) {
            this.provider = provider;
            this.name = name;
            this.index = index;
        }

        public Provider() {
        }
    }
}

