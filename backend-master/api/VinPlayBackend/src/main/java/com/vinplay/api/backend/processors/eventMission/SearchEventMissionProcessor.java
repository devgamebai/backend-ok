package com.vinplay.api.backend.processors.eventMission;

import com.gamebase.dao.EventMissionDao;
import com.gamebase.dao.impl.EventMissionDaoImpl;
import com.gamebase.entities.EventMission;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

public class SearchEventMissionProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<List<EventMission>>  baseResponse = new BaseResponse<List<EventMission>>();

		EventMissionDao eventMissionDao = new EventMissionDaoImpl();

		// get the parameters from the request
		// parameters: search, page, limit, status, start, end
		// field: limit is null, set default value is 10
		// field: page is null, set default value is 1
		// call method getPartitionEvent to get list
		// call method getCountEvent to total
		// try catch SQLException if have error return message "System error" and code 1

		try {
			// get the parameters from the request
			String search = request.getParameter("search");
			int limit = request.getParameter("limit") != null ? Integer.parseInt(request.getParameter("limit")) : 10;
			int page = request.getParameter("page") != null ? Integer.parseInt(request.getParameter("page")) : 1;
			int status = (request.getParameter("status") != null && !request.getParameter("status").isEmpty()) ? Integer.parseInt(request.getParameter("status")) : 0;
			String start = request.getParameter("start");
			String end = request.getParameter("end");

			// calculate offset

			int offset = 0;
			if (page > 1)  {
				offset = (page - 1) * limit;
			}

			// call method getPartitionEvent to get list
			List<EventMission> eventMissions = eventMissionDao.getPartitionEvent(search, limit, offset, status, start, end);

			// call method getCountEvent to get total
			int total = eventMissionDao.getCountEvent(search, status, start, end);

			// set response data
			baseResponse.setData(eventMissions);
			baseResponse.setTotalRecords(total);
			baseResponse.setErrorCode("0");
			baseResponse.setMessage("Success");
			baseResponse.setSuccess(true);
		} catch (SQLException e) {
			logger.error("Error while searching event missions", e);
			baseResponse.setErrorCode("1");
			baseResponse.setMessage("System error");
		}

        return baseResponse.toJson();
	}
}
