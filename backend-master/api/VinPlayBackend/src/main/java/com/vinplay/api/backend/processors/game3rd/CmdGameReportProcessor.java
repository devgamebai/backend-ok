package com.vinplay.api.backend.processors.game3rd;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * c=9304 -- Log WM/CMD 3rd party games (MongoDB).
 * Params: nn (nickname), ts, te, p
 */
public class CmdGameReportProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;
    private static final String COLLECTION_NAME = "log_game_cmd";

    public String execute(Param<HttpServletRequest> param) {
        return Game3rdLogHelper.executeMongoQuery(param, COLLECTION_NAME, PAGE_SIZE, logger, "CmdGameReportProcessor");
    }
}
