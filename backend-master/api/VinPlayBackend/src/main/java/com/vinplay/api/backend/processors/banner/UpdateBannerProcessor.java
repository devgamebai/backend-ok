package com.vinplay.api.backend.processors.banner;

import com.gamebase.dao.BannerDAO;
import com.gamebase.dao.impl.BannerDAOImpl;
import com.gamebase.entities.BannerModel;
import com.gamebase.service.BannerService;
import com.gamebase.service.impl.BannerServiceImpl;
import com.google.gson.Gson;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;

public class UpdateBannerProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        Gson gson = new Gson();
        HttpServletRequest request = param.get();
        String title = request.getParameter("ti");
        String image_path = request.getParameter("ip");
        String url = request.getParameter("url");
        String actionType = request.getParameter("at");
        String statusStr = request.getParameter("status");

        int status = 0;
        try {
            status = Integer.parseInt(statusStr);
        } catch (NumberFormatException e){
            status = 1;
        }

        Integer index = 0;
        try {
            index = Integer.parseInt(request.getParameter("index"));
        } catch (NumberFormatException e){
            index = 0;
        }

        int eventID = 0;
        try {
            eventID = Integer.parseInt(request.getParameter("ev"));
        } catch (NumberFormatException e){

        }

        int id = 0;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e){

        }

        BannerModel bannerModel = new BannerModel();
        bannerModel.setId(id);
        bannerModel.setTitle(title);
        bannerModel.setStatus(status);
        bannerModel.setImage_path(image_path);
        bannerModel.setUrl(url);
        bannerModel.setIndex(index);
        bannerModel.setEventId(eventID);
        bannerModel.setActionType(actionType);

        try {
            BannerService service = new BannerServiceImpl();
            Boolean check = false;
            check = service.updateBannerById(bannerModel);

            if(check){
                return BaseResponse.success("", "Update thành công", bannerModel);
            } else{
                return BaseResponse.error("-1", "Update không thành công !");
            }
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}