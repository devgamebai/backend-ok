package com.vinplay.api.backend.processors.commission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vinplay.dal.dao.impl.GameCommissionRateDaoImpl;
import com.vinplay.dal.entities.agent.GameCommissionRate;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class SearchGameCommissionRateProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String nickName = request.getParameter("nn");

        if (nickName == null || nickName.trim().isEmpty()) {
            return BaseResponse.error("-1", "nick_name is required");
        }

        try {
            GameCommissionRateDaoImpl dao = new GameCommissionRateDaoImpl();
            List<GameCommissionRate> list = dao.searchByNickName(nickName.trim());

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String dataJson = ow.writeValueAsString(list);
            String result = "{\"total\":" + list.size() + ",\"data\":" + dataJson + "}";

            return new BaseResponse<String>().success(result);
        } catch (Exception e) {
            logger.error("SearchGameCommissionRate error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
