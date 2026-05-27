package com.vinplay.api.backend.processors.user;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.UserDao;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.UserForAdminService;
import com.vinplay.usercore.service.impl.UserForAdminServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

//
public class CountListUserRechargeInDay implements BaseProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
//        Code logic cũ
//        String date_param = request.getParameter("date");
//        Date date;
//        if (date_param==null || date_param.isEmpty()){
//            date = new Date();
//        } else {
//            try {
//                date = new SimpleDateFormat("yyyy-MM-dd").parse(date_param);
//            } catch (ParseException e) {
//                return BaseResponse.error("-1", "Nhập sai định dạng ngày yyyy-MM-dd");
//            }
//        }
//
//        UserDao dao = new UserDaoImpl();
//        try {
//            Long count = dao.countListUserRechargeInDay(new java.sql.Date(date.getTime()));
//            return BaseResponse.success("00", "Thành công", count);
//        }
//        catch (Exception e) {
//            return BaseResponse.error("-1", e.getMessage());
//        }
        
        String timeStart = request.getParameter("ts");
        String timeEnd = request.getParameter("te");
        String refferal_code = request.getParameter("cd");
        int recharge = 0;
        AgentDAO dao = new AgentDAOImpl();
        List<Map<String, Object>> data = new ArrayList<>();
        try {
			data = dao.getUsersDepositFirstInDay(timeStart, timeEnd, "", refferal_code, 1, 1);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
        
        if (data == null || data.size() == 0)
        	recharge = 0;
        else {
        	try{
        		recharge = Integer.parseInt(data.get(data.size() - 1).get("total").toString());
        	}catch (Exception e) { }
        }
        
        return BaseResponse.success("00", "Thành công", recharge);
    }
}