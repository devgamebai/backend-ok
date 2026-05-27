package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=157 - Agent Ratio: update agent commission ratio.
 * Params: nn (nick_name), per (ratio percentage)
 * The 'dai_ly' field indicates agent status, 'ti_gia' in log_tranfer_agent stores ratio.
 */
public class AgentRatioProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickName = request.getParameter("nn");
            String ratioStr = request.getParameter("per");

            if (nickName == null || nickName.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "nn is required");
                return response.toString();
            }

            if (ratioStr == null || ratioStr.isEmpty()) {
                // GET mode: return current agent info
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    String sql = "SELECT id, nick_name, dai_ly FROM users WHERE nick_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nickName);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                JSONObject data = new JSONObject();
                                data.put("id", rs.getLong("id"));
                                data.put("nick_name", rs.getString("nick_name"));
                                data.put("dai_ly", rs.getInt("dai_ly"));

                                // Get latest ti_gia from log_tranfer_agent
                                String sqlRatio = "SELECT ti_gia FROM log_tranfer_agent WHERE nick_name_send = ? " +
                                        "ORDER BY id DESC LIMIT 1";
                                try (PreparedStatement ps2 = conn.prepareStatement(sqlRatio)) {
                                    ps2.setString(1, nickName);
                                    try (ResultSet rs2 = ps2.executeQuery()) {
                                        if (rs2.next()) {
                                            data.put("ratio", rs2.getInt("ti_gia"));
                                        } else {
                                            data.put("ratio", 0);
                                        }
                                    }
                                }

                                response.put("success", true);
                                response.put("errorCode", "0");
                                response.put("data", data);
                            } else {
                                response.put("success", false);
                                response.put("errorCode", "1002");
                                response.put("message", "User not found");
                            }
                        }
                    }
                }
            } else {
                // UPDATE mode: set agent ratio
                int ratio;
                try {
                    ratio = Integer.parseInt(ratioStr);
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    response.put("message", "Invalid ratio value");
                    return response.toString();
                }

                if (ratio < 0 || ratio > 100) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    response.put("message", "Ratio must be between 0 and 100");
                    return response.toString();
                }

                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    // Verify user exists and is an agent
                    String checkSql = "SELECT id, dai_ly FROM users WHERE nick_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, nickName);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                response.put("success", false);
                                response.put("errorCode", "1002");
                                response.put("message", "User not found");
                                return response.toString();
                            }
                        }
                    }

                    // Update the latest log_tranfer_agent record's ti_gia for this agent
                    String updateSql = "UPDATE log_tranfer_agent SET ti_gia = ? WHERE nick_name_send = ? " +
                            "ORDER BY id DESC LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setInt(1, ratio);
                        ps.setString(2, nickName);
                        int rows = ps.executeUpdate();

                        if (rows > 0) {
                            response.put("success", true);
                            response.put("errorCode", "0");
                            logger.info("Updated agent ratio for " + nickName + " to " + ratio + "%");
                        } else {
                            // No existing record, just confirm success (agent has no transfers yet)
                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("message", "No transfer records found for agent, ratio not applied");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("AgentRatioProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
