package com.vinplay.api.backend.processors.giftcode;

import com.vinplay.dal.dao.GiftCodeBundleDAO;
import com.vinplay.dal.dao.impl.GiftCodeBundleDAOImpl;
import com.vinplay.dal.entities.giftcode.BundleUsedGiftCodeModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowUsedGiftCodeBundleProcessor implements BaseProcessor<HttpServletRequest, String> {
//    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        Integer bundleId = Integer.parseInt(request.getParameter("bi"));

        GiftCodeBundleDAO dao = new GiftCodeBundleDAOImpl();
        try {
            List<BundleUsedGiftCodeModel> list = dao.showUsedGiftCodeInBundle(bundleId);
            Map<String, Object> map = new HashMap<>();
            map.put("totalValue", 0);
            return BaseResponse.success(list, list.size(), map);
        } catch (SQLException e) {
            return BaseResponse.error("-1", e.toString());
        }
    }
}