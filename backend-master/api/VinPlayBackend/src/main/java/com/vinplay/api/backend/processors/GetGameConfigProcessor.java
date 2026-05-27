/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.GameConfigServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.GameConfigResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GameConfigServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.GameConfigResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class GetGameConfigProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");
    GameConfigResponse response = new GameConfigResponse(false, "1001");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String platform = request.getParameter("pf");
        String name = request.getParameter("nm");
        try {
            GameConfigServiceImpl service = new GameConfigServiceImpl();
            List config = service.getGameConfigAdmin(name, platform);
            if (config != null) {
                this.response.setErrorCode("0");
                this.response.setSuccess(true);
                this.response.setGameconfig(config);
            } else {
                this.response.setErrorCode("10001");
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return this.response.toJson();
    }
}

