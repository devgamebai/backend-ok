package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9901 — Token heartbeat / session verify.
 * If the request reaches here, the aat middleware already validated the token.
 * Returns success:true = token is valid, session is active.
 * If token is invalid/expired, the middleware returns 9001 before this runs.
 */
public class AdminLoginVerifyProcessor implements BaseProcessor<HttpServletRequest, String> {

    public String execute(Param<HttpServletRequest> param) {
        return "{\"success\":true,\"errorCode\":\"0\",\"message\":\"Token valid\"}";
    }
}
