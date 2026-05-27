/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.UserInfoServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ResultUserInfoResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.UserInfoServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ResultUserInfoResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ListUserInfoProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ResultUserInfoResponse response = new ResultUserInfoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String ip = request.getParameter("ip");
        String startDate = request.getParameter("ts");
        String endDate = request.getParameter("te");
        String type = request.getParameter("type");
        int page = 1;
        try {
            String pStr = request.getParameter("p");
            if (pStr == null || pStr.isEmpty()) pStr = request.getParameter("page");
            if (pStr == null || pStr.isEmpty()) pStr = request.getParameter("pn");
            if (pStr != null && !pStr.isEmpty()) {
                page = Integer.parseInt(pStr);
            }
        } catch (NumberFormatException ignored) {}

        if (page < 1) {
            page = 1;
        }

        try {
            if (type != null && !type.isEmpty()) {
                // validate type is integer, otherwise empty it out
                Integer.parseInt(type);
            }
        } catch (NumberFormatException e) {
            type = "";
        }

        UserInfoServiceImpl service = new UserInfoServiceImpl();
        try {
            List trans = service.searchUserInfo(nickName, ip, startDate, endDate, type, page);
            long totalRecord = service.countsearchUserInfo(nickName, ip, startDate, endDate, type);
            long totalPages = totalRecord / 50 + (totalRecord % 50 == 0 ? 0 : 1);
            if (totalPages == 0) totalPages = 1;
            
            response.setTotal(totalPages);
            response.setTotalRecord(totalRecord);
            response.setTransactions(trans);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)("ListUserInfoProcessor ERROR: " + e.getMessage()), e);
        }
        return response.toJson();
    }
}

