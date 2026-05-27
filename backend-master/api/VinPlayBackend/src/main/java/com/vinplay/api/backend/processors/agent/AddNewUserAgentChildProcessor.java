package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.apache.log4j.Logger;

public class AddNewUserAgentChildProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        String pidStr = request.getParameter("pid");
        String username = request.getParameter("un");
        String nickname = request.getParameter("nn");
        String nameagent = request.getParameter("na");
        String address = request.getParameter("adr");
        String phone = request.getParameter("ph");
        String email = request.getParameter("em");
        String code = request.getParameter("ac");
        String password = request.getParameter("ps");
        String commissionRateStr = request.getParameter("cr");
        String depositRateStr = request.getParameter("dr"); // % hoa hồng tiền nạp

        if (pidStr == null || username == null || username.isEmpty() || pidStr.isEmpty()) {
            result.addProperty("errorCode", "1002");
            return result.toString();
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int pid = Integer.parseInt(pidStr);
            double commissionRate = commissionRateStr != null && !commissionRateStr.isEmpty() ? Double.parseDouble(commissionRateStr) : 0;
            double depositRate = depositRateStr != null && !depositRateStr.isEmpty() ? Double.parseDouble(depositRateStr) : 0;

            // Validate format
            if (commissionRateStr != null && !commissionRateStr.trim().isEmpty() && !commissionRateStr.trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                result.addProperty("errorCode", "1002");
                result.addProperty("message", "Invalid commission_rate format");
                return result.toString();
            }
            if (depositRateStr != null && !depositRateStr.trim().isEmpty() && !depositRateStr.trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                result.addProperty("errorCode", "1002");
                result.addProperty("message", "Invalid deposit_rate format (max 2 decimal places)");
                return result.toString();
            }

            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("AddNewUserAgentChildProcessor: cannot get DB connection");
                return result.toString();
            }

            // check parent
            int parentLevel = 1;
            double parentCommissionRate = 0;
            double parentDepositRate = 0;
            try (PreparedStatement psParent = conn.prepareStatement(
                    "SELECT level, commission_rate, IFNULL(deposit_rate, 0) as deposit_rate FROM vinplay_admin.useragent WHERE id = ?")) {
                psParent.setInt(1, pid);
                try (ResultSet rsParent = psParent.executeQuery()) {
                    if (rsParent.next()) {
                        parentLevel = rsParent.getInt("level");
                        parentCommissionRate = rsParent.getDouble("commission_rate");
                        parentDepositRate = rsParent.getDouble("deposit_rate");
                    } else {
                        result.addProperty("errorCode", "1006"); // parent not found
                        return result.toString();
                    }
                }
            }

            if (parentLevel >= 3) {
                result.addProperty("errorCode", "1007"); // ĐL2 cannot create more child
                return result.toString();
            }

            if (commissionRate > parentCommissionRate) {
                result.addProperty("errorCode", "1008");
                result.addProperty("message", "commission_rate cannot exceed parent's rate (" + parentCommissionRate + ")");
                return result.toString();
            }
            if (depositRate > parentDepositRate) {
                result.addProperty("errorCode", "1009");
                result.addProperty("message", "deposit_rate cannot exceed parent's rate (" + parentDepositRate + ")");
                return result.toString();
            }

            // check dupe username
            try (PreparedStatement psDupe = conn.prepareStatement("SELECT id FROM vinplay_admin.useragent WHERE username = ?")) {
                psDupe.setString(1, username);
                try (ResultSet rsDupe = psDupe.executeQuery()) {
                    if (rsDupe.next()) {
                        result.addProperty("errorCode", "1004");
                        return result.toString();
                    }
                }
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());

            // the level and ancestors are automatically calculated by trigger tg_before_useragent_insert
            String insertSql = "INSERT INTO vinplay_admin.useragent (username, nickname, password, nameagent, address, " +
                    "phone, email, code, status, active, `show`, createtime, updatetime, " +
                    "commission_rate, deposit_rate, `order`, login_times, parentid) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 1, ?, ?, ?, ?, 0, 0, ?)";

            ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, nickname != null ? nickname : username);
            ps.setString(3, password != null ? password : "");
            ps.setString(4, nameagent != null ? nameagent : "");
            ps.setString(5, address != null ? address : "");
            ps.setString(6, phone != null ? phone : "");
            ps.setString(7, email != null ? email : "");
            ps.setString(8, code != null ? code : "");
            ps.setTimestamp(9, now);
            ps.setTimestamp(10, now);
            ps.setDouble(11, commissionRate);
            ps.setDouble(12, depositRate);  // deposit_rate
            ps.setInt(13, pid);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                rs = ps.getGeneratedKeys();
                int newId = 0;
                if (rs.next()) {
                    newId = rs.getInt(1);
                }
                
                // Add rebate_config
                try (PreparedStatement rebateCfg = conn.prepareStatement("INSERT INTO rebate_config (agent_user_id, agent_nickname, agent_level, rebate_percentage) VALUES (?, ?, ?, ?)")) {
                	rebateCfg.setInt(1, newId);
                	rebateCfg.setString(2, nickname != null ? nickname : username);
                	rebateCfg.setInt(3, parentLevel + 1);
                	rebateCfg.setDouble(4, commissionRate);
                	rebateCfg.executeUpdate();
                } catch(Exception e) {
                	logger.error("Error inserting rebate config", e);
                }

                result.addProperty("success", true);
                result.addProperty("errorCode", "0");
                JsonObject data = new JsonObject();
                data.addProperty("id", newId);
                result.add("data", data);
            }

        } catch (NumberFormatException e) {
            result.addProperty("errorCode", "1002");
        } catch (Exception e) {
            logger.error("AddNewUserAgentChildProcessor error", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
        }

        return result.toString();
    }
}
