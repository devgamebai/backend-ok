package com.vinplay.api.backend.processors.eventMission;

import com.gamebase.dao.EventMissionDao;
import com.gamebase.dao.MissionDao;
import com.gamebase.dao.impl.EventMissionDaoImpl;
import com.gamebase.dao.impl.MissionDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

public class DeleteEventMissionProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<Object> baseResponse = new BaseResponse<Object>();

		EventMissionDao eventMissionDao = new EventMissionDaoImpl();

		// parse the id from the request
		// call the deleteEvent method from the eventMissionDao
		// try catch SQLException if it has error return message "System error" and code 1

		// parse the id from the request
		String idStr = request.getParameter("id");
		if (StringUtils.isEmpty(idStr)) {
			baseResponse.setErrorCode("1");
			baseResponse.setMessage("ID is required");
			return baseResponse.toJson();
		}

		int id;
		try {
			id = Integer.parseInt(idStr);
		} catch (NumberFormatException e) {
			logger.error("Invalid ID format", e);
			baseResponse.setErrorCode("1");
			baseResponse.setMessage("Invalid ID format");
			return baseResponse.toJson();
		}

		// call the deleteEvent method from the eventMissionDao
		try {
			eventMissionDao.deleteEvent(id);
			baseResponse.setErrorCode("0");
			baseResponse.setMessage("Event mission deleted successfully");
			baseResponse.setSuccess(true);
		} catch (SQLException e) {
			logger.error("Error deleting event mission", e);
			baseResponse.setErrorCode("1");
			baseResponse.setMessage("System error");
		}

		return baseResponse.toJson();

	}
}
