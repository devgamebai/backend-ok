package com.vinplay.api.backend.processors.banner;

import com.gamebase.dao.BannerDAO;
import com.gamebase.dao.impl.BannerDAOImpl;
import com.gamebase.service.BannerService;
import com.gamebase.service.impl.BannerServiceImpl;
import com.google.gson.Gson;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;

public class DeleteBannerProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        Integer id = null;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e){

        }

        try {
            BannerService service = new BannerServiceImpl();
            Boolean check = service.deleteBanner(id);

            if(check){
                return BaseResponse.success("", "Delete thành công", null);
            } else{
                return BaseResponse.error("-1", "Delete không thành công !");
            }
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}