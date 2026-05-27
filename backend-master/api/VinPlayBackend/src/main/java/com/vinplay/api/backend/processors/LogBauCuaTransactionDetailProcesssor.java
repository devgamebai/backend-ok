/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.LogBauCuaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.LogBauCuaDetailResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.LogBauCuaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.LogBauCuaDetailResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LogBauCuaTransactionDetailProcesssor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        LogBauCuaDetailResponse response = new LogBauCuaDetailResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String referent_id = request.getParameter("rid");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        if (!referent_id.isEmpty()) {
            try {
                LogBauCuaServiceImpl service = new LogBauCuaServiceImpl();
                List trans = service.getLogBauCuaDetail(referent_id, page);
                int totalRecord = service.countLogBauCuaDetail(referent_id);
                int totalPages = 0;
                totalPages = totalRecord % 50 == 0 ? totalRecord / 50 : totalRecord / 50 + 1;
                response.setTotal((long)totalPages);
                response.setTotalRecord((long)totalRecord);
                response.setTransactions(trans);
                response.setSuccess(true);
                response.setErrorCode("0");
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.debug((Object)e);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

