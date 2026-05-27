/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.usercore.service.impl.MoneyInGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultBankSmsResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class GetListSmsBankProcessor
        implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultBankSmsResponse response = new ResultBankSmsResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String startTime = request.getParameter("ts");
        String endTime = request.getParameter("te");
        String content = request.getParameter("content");
        String sms = request.getParameter("sms");
        String id = request.getParameter("id");
        String pages = request.getParameter("p");
        String pageSizes = request.getParameter("pageSize");
        String statusStr = request.getParameter("status");
        String from = request.getParameter("from");
        String to = request.getParameter("to");
        if (startTime != null && endTime != null && pages != null && pageSizes != null) {
            try {
                int page = Integer.parseInt(pages);
                int pageSize = Integer.parseInt(pageSizes);
                int status = statusStr == null || statusStr.isEmpty() ? -1 : Integer.parseInt(statusStr);
                startTime += " 00:00:00";
                endTime += " 23:59:59";
                UserService service = new UserServiceImpl();
                List res = service.getBankSmsLst(id, content, sms, startTime, endTime, status, page, pageSize, from, to);
                int count = service.countBankSmsLst(id, content, sms, startTime, endTime, status, from, to);
                response.setTotal(count % pageSize == 0 ? count / pageSize : count / pageSize + 1);
                response.setTotalRecord(count);
                response.setTransactions(res);
                response.setErrorCode("0");
                response.setSuccess(true);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(response);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
        }
        return "";
    }
}

