/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 *  org.json.JSONObject
 */
package com.vinplay.api.processors;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class GetConfigVinPlusProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        String platform = request.getParameter("pf");
        String IOS = "ios";
        String ANDROID = "ad";
        String WINDOW_PHONE = "wp";
        try {
            if (platform != null && (platform.equals("ios") || platform.equals("ad") || platform.equals("wp"))) {
                JSONObject plusObj = new JSONObject(GameCommon.getValueStr((String)"VIN_PLUS"));
                return plusObj.getString(platform);
            }
            return "";
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}

