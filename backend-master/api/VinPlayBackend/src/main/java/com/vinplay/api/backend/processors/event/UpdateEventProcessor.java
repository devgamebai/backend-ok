package com.vinplay.api.backend.processors.event;

import com.google.gson.Gson;
import com.vinplay.dal.dao.EventDAO;
import com.vinplay.dal.dao.impl.EventDAOImpl;
import com.vinplay.dal.entities.event.EventModel;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;

public class UpdateEventProcessor implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        Gson gson = new Gson();
        HttpServletRequest request = param.get();
        String name = request.getParameter("n");
        String created_date = request.getParameter("cd");
        String expired_date = request.getParameter("ed");

        Integer id = null;
        Long amount = null;
        try {
            amount = Long.parseLong(request.getParameter("am"));
        } catch (NumberFormatException e){
        }
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e){
        }

        try {
            EventDAO dao = new EventDAOImpl();
            Boolean check = false;
            if(id == null){
                check = dao.updateEventByName(name, created_date, amount, expired_date);
            }else{
                check = dao.updateEventById(id, name, created_date, amount, expired_date);
            }

            if(check){
                return BaseResponse.success("", "Update thành công", null);
            } else{
                return BaseResponse.error("-1", "Update không thành công !");
            }
        }
        catch (Exception e) {
            return BaseResponse.error("-1", e.getMessage());
        }
    }
}