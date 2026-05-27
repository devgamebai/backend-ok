/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogMoneyUserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateProcessLogChuyenTienDailyProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickNameSend = request.getParameter("nns");
        String nickNameReceive = request.getParameter("nnr");
        String timeLog = request.getParameter("t");
        String status = request.getParameter("st");
        boolean result = false;
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        if (!(nickNameSend == null || nickNameSend.equals("") || nickNameReceive == null || nickNameReceive.equals("") || timeLog == null || timeLog.equals(""))) {
            try {
                LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
                result = service.UpdateProcessLogChuyenTienDaiLy(nickNameSend, nickNameReceive, timeLog, status);
                if (result) {
                    response.setSuccess(true);
                    response.setErrorCode("0");
                } else {
                    response.setSuccess(false);
                    response.setErrorCode("1001");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

