/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors;

import com.google.gson.Gson;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ReceiveBonusProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");
    private static Gson gson = new Gson();

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static void main(String[] args) {
        String[] ad;
        String ip = "14.243.87.92, 172.68.226.139";
        for (String string : ad = ip.split(",")) {
            System.out.println(string.trim());
        }
    }

    public String execute(Param<HttpServletRequest> param) {
        return BaseResponse.error((String)"99", (String)"Qu\u00fd kh\u00e1ch vui l\u00f2ng nh\u1eadn KM \u0111\u1ee3t 2 !");
    }
}

