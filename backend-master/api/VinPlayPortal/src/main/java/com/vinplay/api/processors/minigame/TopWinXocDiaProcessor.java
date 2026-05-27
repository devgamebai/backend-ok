/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.XocDiaServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.minigame;

import com.vinplay.api.processors.minigame.response.TopWinXocDiaResponse;
import com.vinplay.usercore.service.impl.XocDiaServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class TopWinXocDiaProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public String execute(Param<HttpServletRequest> param) {
        TopWinXocDiaResponse response = new TopWinXocDiaResponse(false, "1001");
        XocDiaServiceImpl service = new XocDiaServiceImpl();
        try {
            List result = service.getTopAward();
            response.setTopXocDia(result);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return response.toJson();
    }
}

