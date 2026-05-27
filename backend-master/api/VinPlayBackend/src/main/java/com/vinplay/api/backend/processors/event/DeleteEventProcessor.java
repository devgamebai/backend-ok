package com.vinplay.api.backend.processors.event;

import com.google.gson.Gson;
import com.vinplay.dal.dao.EventDAO;
import com.vinplay.dal.dao.impl.EventDAOImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;

public class DeleteEventProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String name = request.getParameter("n");

        Integer id = null;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e){

        }

        try {
            EventDAO dao = new EventDAOImpl();
            Boolean check = false;
            if(id == null){
                check = dao.deleteEvent(name);
            }else{
                check = dao.deleteEvent(id);
            }

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