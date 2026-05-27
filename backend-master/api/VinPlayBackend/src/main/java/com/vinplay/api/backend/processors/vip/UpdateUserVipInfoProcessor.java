/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.SecurityServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.vip;

import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class UpdateUserVipInfoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        int res = 1;
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = request.getParameter("nn");
        String birthday = request.getParameter("bd");
        String gender = request.getParameter("gd");
        String address = request.getParameter("address");
        if (nickname != null && !nickname.isEmpty() && birthday != null && gender != null && address != null) {
            if (!gender.equals("0") && !gender.equals("1")) {
                return String.valueOf(res);
            }
            try {
                String bd = VinPlayUtils.parseDateToString((Date)VinPlayUtils.getDateTimeFromDate((String)birthday));
                SecurityServiceImpl service = new SecurityServiceImpl();
                res = service.updateUserVipInfo(nickname, bd, gender, address);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return String.valueOf(res);
    }
}

