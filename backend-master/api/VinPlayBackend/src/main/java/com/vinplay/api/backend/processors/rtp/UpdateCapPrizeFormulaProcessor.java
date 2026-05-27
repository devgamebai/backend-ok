package com.vinplay.api.backend.processors.rtp;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;

public class UpdateCapPrizeFormulaProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        return "{\"success\":true,\"errorCode\":0,\"message\":\"Mock endpoint generated for Appendix A & B\"}";
    }
}
