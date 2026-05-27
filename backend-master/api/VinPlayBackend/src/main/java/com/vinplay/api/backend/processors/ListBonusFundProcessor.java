/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.ListBonusFundResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.ListBonusFundResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class ListBonusFundProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        ListBonusFundResponse response = new ListBonusFundResponse(false, "10001");
        MiniGameServiceImpl service = new MiniGameServiceImpl();
        try {
            List listRecord = service.getFunds();
            response.setTransactions(listRecord);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object)e);
        }
        return response.toJson();
    }
}

