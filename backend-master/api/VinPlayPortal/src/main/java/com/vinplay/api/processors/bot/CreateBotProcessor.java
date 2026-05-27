/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.bot;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class CreateBotProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String res = "1";
        try {
            String un = request.getParameter("un");
            String nn = request.getParameter("nn");
            String pw = VinPlayUtils.getMD5Hash((String)request.getParameter("pw"));
            long vin = Long.parseLong(request.getParameter("vin"));
            long xu = Long.parseLong(request.getParameter("xu"));
            int status = Integer.parseInt(request.getParameter("st"));
            UserServiceImpl service = new UserServiceImpl();
            if (service.insertBot(un, nn, pw, vin, xu, status)) {
                res = "0";
            }
        }
        catch (Exception e) {
            logger.debug(e);
        }
        return res;
    }
}

