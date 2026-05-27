/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.ListMoneyTranferServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.MoneyTranferResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.ListMoneyTranferServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.MoneyTranferResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ListMoneyTranferProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        MoneyTranferResponse response = new MoneyTranferResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        String pageStr = request.getParameter("p");
        String isFreezeMoneyStr = request.getParameter("ifm");
        if (nickName == null || pageStr == null || isFreezeMoneyStr == null || nickName.isEmpty() || pageStr.isEmpty()) {
            return "MISSING INPUT PARAMETER";
        }
        int isFreezeMoneyInt = isFreezeMoneyStr.isEmpty() ? -1 : Integer.parseInt(isFreezeMoneyStr);
        try {
            ListMoneyTranferServiceImpl service = new ListMoneyTranferServiceImpl();
            List listTranfer = service.listMoneyTranfer(nickName, isFreezeMoneyInt, Integer.parseInt(pageStr), GameCommon.getValueInt((String)"TIME_SEARCH"));
            int totalRecord = service.countTotalRecord(nickName, isFreezeMoneyInt, Integer.parseInt(pageStr), GameCommon.getValueInt((String)"TIME_SEARCH"));
            response.setErrorCode("0");
            response.setSuccess(true);
            response.setListTranfer(listTranfer);
            response.setTotalRecord(totalRecord);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}

