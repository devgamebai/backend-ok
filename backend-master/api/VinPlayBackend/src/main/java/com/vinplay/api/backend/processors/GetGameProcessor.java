package com.vinplay.api.backend.processors;

import com.vinplay.usercore.service.impl.GameConfigServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.response.GameConfigResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetGameProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        BaseResponse<List<Map<String, String>>> res = new BaseResponse<>();
        try {
            List<Map<String, String>> result = new ArrayList<>();
            for (Games game : Games.values()) {
                Map<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(game.getId()));
                map.put("name", game.getName());
                result.add(map);
            }
            res.setData(result);
        }
        catch (Exception e) {
            logger.debug(e);
        }
        return res.toJson();
    }
}

