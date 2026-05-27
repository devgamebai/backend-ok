/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GiftCodeServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GiftCodeSearchResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GiftCodeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GiftCodeSearchResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GiftCodeByNickNameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickName = request.getParameter("nn");
        GiftCodeSearchResponse response = new GiftCodeSearchResponse(false, "1001");
        int page = Integer.parseInt(request.getParameter("p"));
        if (page < 0) {
            return response.toJson();
        }
        if (!nickName.equals(null) && !nickName.equals("")) {
            try {
                GiftCodeServiceImpl service = new GiftCodeServiceImpl();
                List trans = service.searchAllGiftCodeByNickName(nickName, page);
                long totalPages = 0L;
                long totalRecord = service.countAllGiftCodeByNickName(nickName);
                totalPages = totalRecord % 50L == 0L ? totalRecord / 50L : totalRecord / 50L + 1L;
                response.setTotal(totalPages);
                response.setTotalRecord(totalRecord);
                response.setTransactions(trans);
                response.setErrorCode("0");
                response.setSuccess(true);
            }
            catch (Exception e) {
                logger.debug((Object)e);
            }
            return response.toJson();
        }
        return "MISSING PARAMETTER";
    }
}

