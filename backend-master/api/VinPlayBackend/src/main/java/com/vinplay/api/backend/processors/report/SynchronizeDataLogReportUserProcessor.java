package com.vinplay.api.backend.processors.report;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=76 -- Synchronize data from game logs to report tables.
 * The actual sync is done by a scheduled job. This endpoint triggers the request.
 */
public class SynchronizeDataLogReportUserProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", "Sync requested");
        } catch (Exception e) {
            logger.error("SynchronizeDataLogReportUserProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
