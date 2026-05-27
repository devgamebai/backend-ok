package com.vinplay.api.backend.processors.giftcode;

import com.vinplay.dal.dao.GiftCodeBundleDAO;
import com.vinplay.dal.dao.GiftCodeDAO;
import com.vinplay.dal.dao.impl.GiftCodeBundleDAOImpl;
import com.vinplay.dal.dao.impl.GiftCodeDAOImpl;
import com.vinplay.dal.entities.giftcode.GiftCodeBundleModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ShowGiftCodeBundleDetailProcessor implements BaseProcessor<HttpServletRequest, String> {
//    private static final Logger logger = Logger.getLogger("backend");
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        String created_by = request.getParameter("cb");
        String bundle_id = request.getParameter("bi");

        GiftCodeBundleDAO dao = new GiftCodeBundleDAOImpl();
        GiftCodeDAO daoGiftcode = new GiftCodeDAOImpl();
        try {
            long total = daoGiftcode.countUsedGiftCode(bundle_id);
            Map<String, Object> map = new HashMap<>();
            map.put("totalUsed", total);

            GiftCodeBundleModel bundle = dao.showGiftCodeBundle(created_by, bundle_id);
            return BaseResponse.success(bundle, 1, map);
        } catch (SQLException e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
