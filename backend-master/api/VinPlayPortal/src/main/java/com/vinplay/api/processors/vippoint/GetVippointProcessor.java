/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.VPResponse
 *  com.vinplay.usercore.service.impl.VippointServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Vippoint
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.vippoint;

import com.vinplay.usercore.entities.VPResponse;
import com.vinplay.usercore.service.impl.VippointServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Vippoint;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetVippointProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        VPResponse res = new VPResponse(false, "1001");
        String nickname = request.getParameter("nn");
        if (nickname != null) {
            try {
                VippointServiceImpl ser = new VippointServiceImpl();
                res = ser.getVippoint(nickname);
                ArrayList<Integer> ratioList = new ArrayList<Integer>();
                ratioList.add(Vippoint.DA.getRatio());
                ratioList.add(Vippoint.DONG.getRatio());
                ratioList.add(Vippoint.BAC.getRatio());
                ratioList.add(Vippoint.VANG.getRatio());
                ratioList.add(Vippoint.BACH_KIM_1.getRatio());
                ratioList.add(Vippoint.BACH_KIM_2.getRatio());
                ratioList.add(Vippoint.KIM_CUONG_1.getRatio());
                ratioList.add(Vippoint.KIM_CUONG_2.getRatio());
                ratioList.add(Vippoint.KIM_CUONG_3.getRatio());
                res.setRatioList(ratioList);
            }
            catch (Exception e) {
                logger.debug(e);
            }
        }
        return res.toJson();
    }
}

