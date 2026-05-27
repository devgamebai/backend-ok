/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ListMailBoxResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ListMailBoxResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ListMailBoxProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        ListMailBoxResponse response = new ListMailBoxResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        int page = Integer.parseInt(request.getParameter("p"));
        int total = 0;
        int totalPages = 0;
        if (page < 0) {
            return response.toJson();
        }
        MailBoxServiceImpl service = new MailBoxServiceImpl();
        try {
            List trans = service.listMailBox(nickName, page);
            if (trans.size() > 0) {
                total = service.countMailBox(nickName);
                totalPages = total % 5 == 0 ? total / 5 : total / 5 + 1;
                response.setTotalPages((long)totalPages);
                response.setTransactions(trans);
                response.setSuccess(true);
                response.setErrorCode("0");
            } else {
                response.setErrorCode("10001");
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}

