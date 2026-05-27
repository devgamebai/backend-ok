/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.vinplay.brandname.dao.impl.BrandnameDaoImpl
 *  com.vinplay.doisoat.entities.DoisoatBrandname
 *  com.vinplay.doisoat.entities.DoisoatBrandnameProvider
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.backend.processors.doisoat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.brandname.dao.impl.BrandnameDaoImpl;
import com.vinplay.doisoat.entities.DoisoatBrandname;
import com.vinplay.doisoat.entities.DoisoatBrandnameProvider;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class DoisoatBrandnameProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        String res = "";
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
                DoisoatBrandname model = new DoisoatBrandname();
                BrandnameDaoImpl dao = new BrandnameDaoImpl();
                model = dao.doisoatBrandname(model, startTime, endTime);
                Map<Integer, DoisoatBrandnameProvider> providers = model.getProviders();
                for (Map.Entry entry : providers.entrySet()) {
                    DoisoatBrandnameProvider m = (DoisoatBrandnameProvider)entry.getValue();
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

