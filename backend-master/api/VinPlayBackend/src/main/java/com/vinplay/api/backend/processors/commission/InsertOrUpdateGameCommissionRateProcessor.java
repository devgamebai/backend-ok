package com.vinplay.api.backend.processors.commission;

import com.vinplay.dal.dao.impl.GameCommissionRateDaoImpl;
import com.vinplay.dal.entities.agent.GameCommissionRate;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class InsertOrUpdateGameCommissionRateProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String nickName = request.getParameter("nn");
        String gameIdStr = request.getParameter("gid");
        String rateStr = request.getParameter("rate");

        if (nickName == null || nickName.trim().isEmpty()) {
            return BaseResponse.error("-1", "nick_name is required");
        }
        if (gameIdStr == null || rateStr == null) {
            return BaseResponse.error("-1", "game_id and rate are required");
        }

        try {
            int gameId = Integer.parseInt(gameIdStr);
            double rate = Double.parseDouble(rateStr);

            if (rate < 0 || rate > 100) {
                return BaseResponse.error("-1", "rate must be between 0 and 100");
            }

            Games game = Games.findGameById(gameId);
            String gameName = game != null ? game.getDescription() : "Unknown(" + gameId + ")";

            GameCommissionRate gcr = new GameCommissionRate();
            gcr.setNickName(nickName.trim());
            gcr.setGameId(gameId);
            gcr.setGameName(gameName);
            gcr.setCommissionRate(rate);
            gcr.setStatus(1);

            GameCommissionRateDaoImpl dao = new GameCommissionRateDaoImpl();
            boolean ok = dao.insertOrUpdate(gcr);

            if (ok) {
                return BaseResponse.success("OK", 1);
            } else {
                return BaseResponse.error("-1", "Insert/Update failed");
            }
        } catch (NumberFormatException e) {
            return BaseResponse.error("-1", "Invalid game_id or rate format");
        } catch (Exception e) {
            logger.error("InsertOrUpdateGameCommissionRate error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
