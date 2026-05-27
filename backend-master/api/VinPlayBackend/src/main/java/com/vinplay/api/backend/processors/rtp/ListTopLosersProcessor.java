package com.vinplay.api.backend.processors.rtp;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import javax.servlet.http.HttpServletRequest;

/** c=9782 — Top net-negative players (retention / VIP candidates). Same params as c=9781. */
public class ListTopLosersProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        return ListTopWinnersProcessor.listByNet(param, false);
    }
}
