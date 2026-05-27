/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.ServerInfoServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.CCUResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors;

import com.vinplay.dal.service.impl.ServerInfoServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.CCUResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetCCUProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        CCUResponse response = new CCUResponse(false, "1001");
        String startDate = request.getParameter("ts");
        String endDate = request.getParameter("te");
        // Default to today if dates not provided or empty
        // MongoDB time_log format: "yyyy-MM-dd HH:mm:ss"
        if (startDate == null || startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String today = sdf.format(new java.util.Date());
            if (startDate == null || startDate.isEmpty()) startDate = today;
            if (endDate == null || endDate.isEmpty()) endDate = today + " 23:59:59";
        }
        ServerInfoServiceImpl service = new ServerInfoServiceImpl();
        List trans = service.getLogCCU(startDate, endDate);
        // Precompiled DAO only sets ccu field; normalize to total/phone/desktop/other
        if (trans != null) {
            for (Object obj : trans) {
                com.vinplay.vbee.common.models.LogCCUModel m = (com.vinplay.vbee.common.models.LogCCUModel) obj;
                if (m.total == 0 && m.ccu > 0) m.total = m.ccu;
                if (m.phone == 0 && (m.ad + m.ios) > 0) m.phone = m.ad + m.ios;
                if (m.desktop == 0 && m.dt > 0) m.desktop = m.dt;
                if (m.other == 0 && (m.wp + m.fb + m.ot) > 0) m.other = m.wp + m.fb + m.ot;
            }
        }
        // FE expects at least one record; return zeros if no data
        if (trans == null || trans.isEmpty()) {
            trans = new java.util.ArrayList();
            com.vinplay.vbee.common.models.LogCCUModel zero = new com.vinplay.vbee.common.models.LogCCUModel();
            zero.ts = startDate;
            zero.time_log = startDate;
            trans.add(zero);
        }
        response.setTransactions(trans);
        response.setErrorCode("0");
        response.setSuccess(true);
        return response.toJson();
    }
}

