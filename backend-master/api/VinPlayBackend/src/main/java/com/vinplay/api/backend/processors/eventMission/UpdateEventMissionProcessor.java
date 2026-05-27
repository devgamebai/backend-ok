package com.vinplay.api.backend.processors.eventMission;

import com.gamebase.dao.EventMissionDao;
import com.gamebase.dao.MissionDao;
import com.gamebase.dao.impl.EventMissionDaoImpl;
import com.gamebase.dao.impl.MissionDaoImpl;
import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class UpdateEventMissionProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");// Logger.getLogger(BankSearchProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        BaseResponse<Object> baseResponse = new BaseResponse<Object>();

        EventMissionDao eventMissionDao = new EventMissionDaoImpl();

        // Get the parameters from the request
        // parameters: id, name, content, show, status, expired_at
        // required: name, content, id, status,
        // call method updateEvent
        // try catch SQLException if it has error return message "System error" and code 1

        String name;
        String content;
        boolean show;
        int status;
        String expiredAtStr;
        int id;

        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonBody = new JSONObject(sb.toString());

            id = jsonBody.getInt("id");
            name = jsonBody.getString("name");
            content = jsonBody.getString("content");
            show = jsonBody.getBoolean("show");
            status = jsonBody.getInt("status");
            expiredAtStr = jsonBody.getString("expired_at");

            // Validate required parameters
            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(content) || StringUtils.isEmpty(expiredAtStr)) {
                baseResponse.setErrorCode("1");
                baseResponse.setMessage("All fields are required");
                return baseResponse.toJson();
            }
        } catch (Exception e) {
            logger.error("Error reading request body", e);
            baseResponse.setErrorCode("1");
            baseResponse.setMessage("Error reading request body");
            return baseResponse.toJson();
        }

        // Parse the id and expired_at parameters
        if (id == 0) {
            baseResponse.setErrorCode("1");
            baseResponse.setMessage("Error reading request body");
            return baseResponse.toJson();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date expiredAt;
        try {
            expiredAt = dateFormat.parse(expiredAtStr);
        } catch (ParseException e) {
            logger.error("Error parsing expired_at date", e);
            baseResponse.setErrorCode("1");
            baseResponse.setMessage("Invalid date format");
            return baseResponse.toJson();
        }

        // Update the event mission
        try {
            EventMission eventMission = new EventMission();
            eventMission.setId(id);
            eventMission.setName(name);
            eventMission.setContent(content);
            eventMission.setShow(show);
            eventMission.setStatus(status);
            eventMission.setExpiredAt(new Timestamp(expiredAt.getTime()));
            eventMissionDao.updateEvent(eventMission);

            baseResponse.setSuccess(true);
            baseResponse.setErrorCode("0");
            baseResponse.setMessage("Event mission updated successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Error updating event mission", e);
            baseResponse.setErrorCode("1");
            baseResponse.setMessage("System error");
        }

        return baseResponse.toJson();

    }
}
