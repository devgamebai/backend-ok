package com.vinplay.api.backend.processors.mission;

import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.MissionService;
import com.gamebase.service.impl.EventMissionServiceImpl;
import com.gamebase.service.impl.MissionServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchMissionProcessor implements BaseProcessor<HttpServletRequest, String>{
	private static final Logger logger =  Logger.getLogger("backend");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = param.get();
		BaseResponse<List<MissionExtra>>  baseResponse = new BaseResponse<List<MissionExtra>>();

		MissionService missionService = new MissionServiceImpl();

		// Get the parameters from the request
		String search = request.getParameter("search");
		int limit = parseInteger(request.getParameter("limit"), 10);
		int page = parseInteger(request.getParameter("page"), 1);
		int eventId = parseInteger(request.getParameter("event_id"), -1);
		int gameId = parseInteger(request.getParameter("game_id"), -1);
		int status = parseInteger(request.getParameter("status"), -1);
		String type = request.getParameter("type");
		String start = request.getParameter("start");
		String end = request.getParameter("end");

		int offset = 0;
		if (page > 1)  {
			offset = (page - 1) * limit;
		};

		try {
		    // Call the getPartitionMission method from the missionService
		    List<Mission> missions = missionService.getPartitionMission(search, limit, offset, eventId, gameId, status, type, start, end);

		    // Call the getCountMission method from the missionService
		    int count = missionService.getCountMission(search, eventId, gameId, status, type, start, end);

			EventMissionService eventMissionService = new EventMissionServiceImpl();
			List<MissionExtra> missionResult = new ArrayList<>();

			for (Mission mission : missions) {
				MissionExtra missionExtra = new MissionExtra(mission);
				EventMission eventMission = eventMissionService.getEventMissionById(mission.getEvent_id());
				if(eventMission != null) {
					missionExtra.setExpired_at(eventMission.getExpiredAt());
					missionExtra.setEvent_name(eventMission.getName());
				}
				missionResult.add(missionExtra);
			}

		    // Set result to BaseResponse
		    baseResponse.setData(missionResult);
		    baseResponse.setTotalRecords(count);
		    baseResponse.setSuccess(true);
		} catch (SQLException e) {
			e.printStackTrace();
		    logger.error("Error while searching missions", e);
		    baseResponse.setSuccess(false);
		    baseResponse.setMessage("Error while searching missions: " + e.getMessage());
		}

        return baseResponse.toJson();
	}

	private int parseInteger(String value, int defaultValue) {
		try {
			return value != null ? Integer.parseInt(value) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}


	public static class MissionExtra extends Mission {

		private Date expired_at;

		public MissionExtra(Mission mission) {
			this.setId(mission.getId());
			this.setName(mission.getName());
			this.setDescription(mission.getDescription());
			this.setType(mission.getType());
			this.setPoint(mission.getPoint());
			this.setEvent_id(mission.getEvent_id());
			this.setGame_id(mission.getGame_id());
			this.setReward(mission.getReward());
			this.setStatus(mission.getStatus());
			this.setCreated_at(mission.getCreated_at());
			this.setUpdated_at(mission.getUpdated_at());
			this.setRules(mission.getRules());
		}

		private String event_name;

		public String getEvent_name() {
			return event_name;
		}

		public void setEvent_name(String event_name) {
			this.event_name = event_name;
		}

		public Date getExpired_at() {
			return expired_at;
		}

		public void setExpired_at(Date expired_at) {
			this.expired_at = expired_at;
		}
	}
}
