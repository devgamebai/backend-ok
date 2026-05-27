package com.vinplay.api.backend.processors.mission;

import com.gamebase.entities.Mission;
import com.gamebase.service.MissionService;
import com.gamebase.service.impl.MissionServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

public class GetMissionProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<Mission>  baseResponse = new BaseResponse<Mission>();

		MissionService missionService = new MissionServiceImpl();

		// Get the parameters from the request

		try {
			String id = request.getParameter("id");

		    // Call the getCountMission method from the missionService
			Mission mission = missionService.getMission(id);

			// Set result to BaseResponse
		    baseResponse.setData(mission);
		    baseResponse.setSuccess(true);
		} catch (SQLException e) {
		    logger.error("Error while get missions", e);
		    baseResponse.setSuccess(false);
		    baseResponse.setMessage("Error while get missions: " + e.getMessage());
		}

        return baseResponse.toJson();
	}
}
