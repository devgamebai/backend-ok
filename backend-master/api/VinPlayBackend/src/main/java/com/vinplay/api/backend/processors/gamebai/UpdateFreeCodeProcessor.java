/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.gamebai;

import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class UpdateFreeCodeProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String res = "1";
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String code = request.getParameter("co");
            int packageId = Integer.parseInt(request.getParameter("pkid"));
            String gamename = request.getParameter("gn");
            int amount = Integer.parseInt(request.getParameter("am"));
            int codeType = Integer.parseInt(request.getParameter("ct"));
            int status = Integer.parseInt(request.getParameter("st"));
            String nickname = request.getParameter("nn");
            String otp = request.getParameter("otp");
            String otpType = request.getParameter("ot");
            if (!(status != 1 && status != 2 || packageId <= 0 || nickname == null || nickname.isEmpty())) {
                String[] arr;
                res = "2";
                for (String admin : arr = GameCommon.getValueStr((String)"SUPER_ADMIN").split(",")) {
                    if (!admin.equals(nickname)) continue;
                    OtpServiceImpl otpService = new OtpServiceImpl();
                    int err = otpService.checkOtp(otp, nickname, otpType, (String)null);
                    if (err == 0) {
                        GameTourServiceImpl ser = new GameTourServiceImpl();
                        ArrayList<Integer> statusW = new ArrayList<Integer>();
                        switch (status) {
                            case 1: {
                                statusW.add(0);
                                statusW.add(2);
                                break;
                            }
                            case 2: {
                                statusW.add(1);
                                statusW.add(0);
                            }
                        }
                        if (!ser.updateFreeCodeDetail(status, gamename, id, code, packageId, codeType, amount, statusW)) continue;
                        res = "0";
                        continue;
                    }
                    if (err == 3 || err != 4) continue;
                    res = "3";
                }
            }
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return res;
    }
}

