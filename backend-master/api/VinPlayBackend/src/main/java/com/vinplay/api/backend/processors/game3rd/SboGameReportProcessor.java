package com.vinplay.api.backend.processors.game3rd;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9435 -- Log SBO 3rd party games (MongoDB).
 * Params: nn, ts, te, p
 */
public class SboGameReportProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;
    private static final String COLLECTION_NAME = "log_game_sbo";

    public String execute(Param<HttpServletRequest> param) {
        return Game3rdLogHelper.executeMongoQuery(param, COLLECTION_NAME, PAGE_SIZE, logger, "SboGameReportProcessor");
    }
}
