/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.SlotMachineServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.models.slot.NoHuModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.slot;

import com.vinplay.api.processors.slot.response.LSNoHuResponse;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.slot.NoHuModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class LSNoHuProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        LSNoHuResponse response = new LSNoHuResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String gameName = request.getParameter("gn");
        int page = Integer.parseInt(request.getParameter("p"));
        SlotMachineServiceImpl service = new SlotMachineServiceImpl();
        List results = service.getLogNoHu(gameName, page);
        response.setTotalPages(10);
        response.setResults(results);
        response.setSuccess(true);
        response.setErrorCode("0");
        return response.toJson();
    }
}

