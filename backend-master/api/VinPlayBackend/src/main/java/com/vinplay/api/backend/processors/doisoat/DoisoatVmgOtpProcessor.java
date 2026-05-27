/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.doisoat.entities.DoisoatVmg
 *  com.vinplay.doisoat.entities.DoisoatVmgByProvider
 *  com.vinplay.usercore.dao.impl.OtpDaoImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.doisoat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.doisoat.entities.DoisoatVmg;
import com.vinplay.doisoat.entities.DoisoatVmgByProvider;
import com.vinplay.usercore.dao.impl.OtpDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class DoisoatVmgOtpProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        String res = "";
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
                DoisoatVmg model = new DoisoatVmg();
                OtpDaoImpl dao = new OtpDaoImpl();
                model = dao.doisoatVMG(model, startTime, endTime);
                Map<Integer, DoisoatVmgByProvider> providers = model.getProviders();
                for (Map.Entry entry : providers.entrySet()) {
                    DoisoatVmgByProvider m = (DoisoatVmgByProvider)entry.getValue();
                    m.calculate();
                    providers.put((Integer) entry.getKey(), m);
                }
                model.setProviders(providers);
                model.calculate();
                ObjectMapper mapper = new ObjectMapper();
                res = mapper.writeValueAsString((Object)model);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}

