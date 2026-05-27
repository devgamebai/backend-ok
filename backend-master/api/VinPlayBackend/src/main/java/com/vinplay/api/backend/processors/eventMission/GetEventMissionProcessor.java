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

public class GetEventMissionProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<EventMission>  baseResponse = new BaseResponse<EventMission>();

		EventMissionDao eventMissionDao = new EventMissionDaoImpl();

		// get the parameters from the request
		// parameters: id

		try {
			// get the parameters from the request
			String id = request.getParameter("id");
			EventMission event = eventMissionDao.getEvent(Integer.parseInt(id));

			// set response data
			baseResponse.setData(event);
			baseResponse.setErrorCode("0");
			baseResponse.setMessage("Success");
			baseResponse.setSuccess(true);
		} catch (SQLException e) {
			logger.error("Error while get event missions", e);
			baseResponse.setErrorCode("1");
			baseResponse.setMessage("System error");
		}

        return baseResponse.toJson();
	}
}
