/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeMachineServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.GiftCodeMachineMessage
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeMachineServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeMachineMessage;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeMachineProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String gia = request.getParameter("gp");
        String soluong = request.getParameter("gq");
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        if (gia != null && soluong != null) {
            try {
                GiftCodeMachineServiceImpl service = new GiftCodeMachineServiceImpl();
                for (int i = 0; i < Integer.parseInt(soluong); ++i) {
                    String giftCode = VinPlayUtils.genGiftCode((int)5);
                    GiftCodeMachineMessage msg = new GiftCodeMachineMessage(giftCode.toUpperCase(), gia, Integer.parseInt(soluong), "", 0);
                    service.exportGiftCodeMachine(msg);
                    response.setErrorCode("0");
                    response.setSuccess(true);
                }
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

