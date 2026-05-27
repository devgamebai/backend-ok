package com.vinplay.api.backend.processors.mission;

import com.gamebase.entities.Mission;
import com.gamebase.entities.MissionName;
import com.gamebase.entities.MissionRule;
import com.gamebase.entities.TypeRule;
import com.gamebase.service.MissionService;
import com.gamebase.service.impl.MissionServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.sql.SQLException;
import java.util.Date;

public class UpdateMissionProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        BaseResponse<Object> baseResponse = new BaseResponse<Object>();

        MissionService missionService = new MissionServiceImpl();

        // Get the parameters from the request
        String name;
        String description;
        String rules;
        long point;
        long reward;
        int status;
        String id;
        String type;

        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonBody = new JSONObject(sb.toString());

            id = jsonBody.getString("id");
            name = jsonBody.getString("name");
            type = jsonBody.getString("type");
            description = jsonBody.getString("description");
            rules = jsonBody.getString("rules");
            point = jsonBody.getLong("point");
            reward = jsonBody.getLong("reward");
            status = jsonBody.getInt("status");

            switch (type) {
                case MissionName.MissionFirstDepositComplex:
                case MissionName.MissionComplex:
                    rules = jsonBody.getString("rules");
                    if (StringUtils.isBlank(rules)) {
                        baseResponse.setMessage("Missing required fields rules");
                        return baseResponse.toJson();
                    }
                    break;
                default:
                    point = jsonBody.getLong("point");

                    if (point <= 0) {
                        baseResponse.setMessage("Missing required fields or invalid numeric values");
                        return baseResponse.toJson();
                    }
            }


            // Validate required parameters
            if (StringUtils.isBlank(name) ||
                    reward <= 0 ||
                    status <= 0
            ) {
                baseResponse.setMessage("Missing required fields or invalid numeric values");
                return baseResponse.toJson();
            }

        } catch (Exception e) {
            logger.error("Error reading request body", e);
            baseResponse.setErrorCode("1");
            baseResponse.setMessage("Error reading request body");
            return baseResponse.toJson();
        }

        // Create a mission object
        Mission mission = new Mission();
        mission.setId(id);
        mission.setName(name);
        mission.setType(type);
        mission.setDescription(description);
        mission.setPoint(point);
        mission.setReward(reward);
        mission.setStatus(status);
        mission.setUpdated_at(new Date());
        mission.setRuleString(rules);

        if (type.equals(MissionName.MissionComplex)) {
            if (mission.getRules().isEmpty()) {
                baseResponse.setMessage("Missing required fields rules");
                return baseResponse.toJson();
            }

            int countSumDeposit = 0;
            int countSumBet = 0;
            int countSumWin = 0;
            int countSumBetMultiWithDeposit = 0;
            for (MissionRule missionRule : mission.getRules().values()) {
                if (missionRule.getType() == TypeRule.SumDeposit) {
                    countSumDeposit++;
                }
                if (missionRule.getType() == TypeRule.SumBet) {
                    countSumBet++;
                }
                if (missionRule.getType() == TypeRule.SumWin) {
                    countSumWin++;
                }
                if (missionRule.getType() == TypeRule.SumBetMultiWithDeposit) {
                    countSumBetMultiWithDeposit++;
                }
            }
            if (countSumDeposit > 1 || countSumBet > 1 || countSumWin > 1 || countSumBetMultiWithDeposit > 1) {
                baseResponse.setMessage("Duplicate rules");
                return baseResponse.toJson();
            }
        }

        try {
            missionService.updateMission(mission);
            baseResponse.setErrorCode("0");
            baseResponse.setMessage("Mission updated successfully");
            baseResponse.setSuccess(true);
        } catch (SQLException e) {
            logger.error("Error updating mission", e);
            baseResponse.setMessage("System error");
        }

        return baseResponse.toJson();

    }
}
