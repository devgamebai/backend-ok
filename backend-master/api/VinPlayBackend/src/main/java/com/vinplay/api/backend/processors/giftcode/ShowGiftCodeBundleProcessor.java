package com.vinplay.api.backend.processors.giftcode;

import com.vinplay.dal.dao.GiftCodeBundleDAO;
import com.vinplay.dal.dao.impl.GiftCodeBundleDAOImpl;
import com.vinplay.dal.entities.giftcode.GiftCodeBundleModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

public class ShowGiftCodeBundleProcessor implements BaseProcessor<HttpServletRequest, String> {
//    private static final Logger logger = Logger.getLogger("backend");
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
//        String log =  String.format(("%s - %s - %s"), request.getRemoteAddr(), this.getClass().getName(), request.getQueryString());
//        logger.info(log);

        String created_by = request.getParameter("cb");

        int page, maxItem;

        try {
            page = Integer.parseInt(request.getParameter("pg"));
        } catch (NumberFormatException e) {
            page =1;
        }
        try {
            maxItem = Integer.parseInt(request.getParameter("mi"));
        } catch (NumberFormatException e) {
            maxItem = 10;
        }

        GiftCodeBundleDAO dao = new GiftCodeBundleDAOImpl();
        try {
            long total = dao.countGiftCodeBundle(created_by);
            List<GiftCodeBundleModel> list = dao.showListGiftCodeBundle(created_by, page, maxItem);
            return BaseResponse.success(list, total);
        } catch (SQLException e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}
