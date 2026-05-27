package com.vinplay.api.backend.processors.rtp;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9779 — Manual trigger for the RTP aggregator.
 * Admin-only. Fires the incremental aggregation pipeline across all sources,
 * rebuilds summaries and heatmap cache. Returns rounds ingested.
 */
public class RunRtpAggregatorProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            long start = System.currentTimeMillis();
            long ingested = RtpAggregator.runIncremental();
            long elapsed = System.currentTimeMillis() - start;
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("roundsIngested", ingested);
            response.put("elapsedMs", elapsed);
        } catch (Exception e) {
            logger.error("RunRtpAggregatorProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
