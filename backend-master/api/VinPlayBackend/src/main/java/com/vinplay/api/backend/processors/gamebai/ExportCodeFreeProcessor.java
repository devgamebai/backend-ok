/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.gamebai;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;

public class ExportCodeFreeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel res = new BaseResponseModel(false, "-1");
        try {
            String gamename = request.getParameter("gn");
            int quantity = Integer.parseInt(request.getParameter("qty"));
            int amount = Integer.parseInt(request.getParameter("am"));
            int codeType = Integer.parseInt(request.getParameter("ct"));
            String expire = request.getParameter("ep");
            String actor = request.getParameter("act");
            String otp = request.getParameter("otp");
            String otpType = request.getParameter("ot");
            if (gamename != null && !gamename.isEmpty() && quantity > 0 && amount > 0 && actor != null && !actor.isEmpty()) {
                String[] arr = GameCommon.getValueStr((String)"SUPER_ADMIN").split(",");
                res.setErrorCode("-2");
                for (String admin : arr) {
                    if (!admin.equals(actor)) continue;
                    OtpServiceImpl otpService = new OtpServiceImpl();
                    int code = otpService.checkOtp(otp, actor, otpType, (String)null);
                    if (code == 0) {
                        GameTourServiceImpl ser = new GameTourServiceImpl();
                        int packageId = ser.exportFreeCode(gamename, quantity, amount, codeType, VinPlayUtils.getCalendar((String)expire), actor);
                        res.setErrorCode(String.valueOf(packageId));
                        continue;
                    }
                    if (code == 3 || code != 4) continue;
                    res.setErrorCode("-3");
                }
            }
            return res.toJson();
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }
}

