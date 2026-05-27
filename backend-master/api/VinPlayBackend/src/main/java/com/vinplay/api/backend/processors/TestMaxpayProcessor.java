/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  com.vinplay.vtc.VTCRechargeClient
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vtc.VTCRechargeClient;
import javax.servlet.http.HttpServletRequest;

public class TestMaxpayProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        try {
            VTCRechargeClient client = new VTCRechargeClient();
            String transId = String.valueOf(VinPlayUtils.generateTransId());
            String seri = "ID0352180817";
            String pin = "859790410017";
            String nickName = "taikhoan3";
            VTCRechargeClient.rechargeVcoinCard((String)transId, (String)"ID0352180817", (String)"859790410017", (String)"taikhoan3");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}

