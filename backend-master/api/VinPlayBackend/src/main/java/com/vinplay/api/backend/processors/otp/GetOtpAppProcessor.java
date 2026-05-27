/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.otp;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

public class GetOtpAppProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        StringBuilder res = new StringBuilder("");
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String nickname = request.getParameter("nn");
            String mobile = request.getParameter("m");
            Calendar now = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            String minute = sdf.format(now.getTime());
            String second = now.get(13) < 30 ? "0" : "1";
            String otpTime = String.valueOf(minute) + second;
            String bd = nickname + mobile + "@VinPlay#6102$817" + otpTime;
            res.append(bd).append(" | ");
            String md5 = VinPlayUtils.getMD5Hash((String)bd);
            res.append(md5).append(" | ");
            md5 = md5.substring(0, 5);
            res.append(md5);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res.toString();
    }
}

