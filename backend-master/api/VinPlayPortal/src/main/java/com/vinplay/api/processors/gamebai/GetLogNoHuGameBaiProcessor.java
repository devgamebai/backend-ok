/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogMoneyUserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.gamebai;

import com.vinplay.api.processors.gamebai.response.LogNoHuGameBaiResponse;
import com.vinplay.dal.service.impl.LogMoneyUserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetLogNoHuGameBaiProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        LogNoHuGameBaiResponse response = new LogNoHuGameBaiResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        int page = Integer.parseInt(request.getParameter("p"));
        String gameName = request.getParameter("gn");
        LogMoneyUserServiceImpl service = new LogMoneyUserServiceImpl();
        try {
            int totalRows = 99;
            List results = service.getNoHuGameBaiHistory(page, gameName);
            response.setTotalPages(10);
            response.setNoHu(results);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            logger.debug(e);
            return e.getMessage();
        }
        return response.toJson();
    }
}

