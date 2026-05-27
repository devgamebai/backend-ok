/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 *  org.json.JSONException
 */
package com.vinplay.api.processors;

import com.vinplay.api.utils.PortalUtils;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.sql.SQLException;
import java.text.ParseException;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.json.JSONException;

public class UpdateAppConfigProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        BaseResponseModel res = new BaseResponseModel(false, "1001");
        try {
            PortalUtils.loadGameConfig();
            res.setErrorCode("0");
            res.setSuccess(true);
        }
        catch (SQLException | ParseException | JSONException ex2) {
            Throwable e = ex2;
            logger.debug(e);
        }
        return res.toJson();
    }
}

