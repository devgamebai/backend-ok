package com.vinplay.api.backend.processors.commission;

import com.vinplay.dal.dao.impl.GameCommissionRateDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class DeleteGameCommissionRateProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String idStr = request.getParameter("id");

        if (idStr == null || idStr.trim().isEmpty()) {
            return BaseResponse.error("-1", "id is required");
        }

        try {
            long id = Long.parseLong(idStr);
            GameCommissionRateDaoImpl dao = new GameCommissionRateDaoImpl();
            boolean ok = dao.delete(id);

            if (ok) {
                return BaseResponse.success("OK", 1);
            } else {
                return BaseResponse.error("-1", "Record not found or delete failed");
            }
        } catch (NumberFormatException e) {
            return BaseResponse.error("-1", "Invalid id format");
        } catch (Exception e) {
            logger.error("DeleteGameCommissionRate error", e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
