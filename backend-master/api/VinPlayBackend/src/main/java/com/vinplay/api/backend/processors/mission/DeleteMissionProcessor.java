package com.vinplay.api.backend.processors.mission;

import com.gamebase.service.MissionService;
import com.gamebase.service.impl.MissionServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

public class DeleteMissionProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<Object> baseResponse = new BaseResponse<Object>();

		MissionService missionService = new MissionServiceImpl();

		// Get the parameters from the request
		String id = request.getParameter("id");

		// Validate required fields
		if (StringUtils.isBlank(id)) {
			baseResponse.setMessage("Missing required fields");
			return baseResponse.toJson();
		}

		try {
			missionService.deleteMission(id);
			baseResponse.setErrorCode("0");
			baseResponse.setMessage("Mission deleted successfully");
			baseResponse.setSuccess(true);
		} catch (SQLException e) {
			logger.error("Error deleting mission", e);
			baseResponse.setMessage("System error");
		}

		return baseResponse.toJson();

	}
}
