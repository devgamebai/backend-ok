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

public class GetEventMissionAvailableProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<List<EventMission>>  baseResponse = new BaseResponse<List<EventMission>>();

		EventMissionDao eventMissionDao = new EventMissionDaoImpl();

		try {
			List<EventMission> list = eventMissionDao.getListEvent();

			// set response data
			baseResponse.setData(list);
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
