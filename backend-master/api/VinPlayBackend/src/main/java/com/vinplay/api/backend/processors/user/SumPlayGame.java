package com.vinplay.api.backend.processors.user;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.entities.agent.UserOfAgentModel;
import com.vinplay.payment.utils.Constant;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SumPlayGame implements BaseProcessor<HttpServletRequest, String> {
	@Override
	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		String nick_name = request.getParameter("nn");
		String fromTime = request.getParameter("ft");
		String endTime = request.getParameter("et");
		int page, maxItem;
		try {
			page = Integer.parseInt(request.getParameter("pg"));
		} catch (NumberFormatException e) {
			page = 1;
		}

		try {
			maxItem = Integer.parseInt(request.getParameter("mi"));
		} catch (NumberFormatException e) {
			maxItem = 10;
		}

		AgentDAO dao = new AgentDAOImpl();
		try {
			Map<String, Object> data = dao.getSumPlayInGame(nick_name, fromTime, endTime, page, maxItem);
			return BaseResponse.success(data.get("list"), Long.parseLong(data.get("totalRecords").toString()));
		} catch (Exception e) {
			return BaseResponse.error("-1", e.getMessage());
		}
	}
}