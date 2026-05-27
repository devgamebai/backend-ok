package com.vinplay.api.backend.processors.bank;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class InsertOrUpdateAdminBankProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String idStr = request.getParameter("id");
            String typeStr = request.getParameter("t");
            String customerName = request.getParameter("cn");
            String bankName = request.getParameter("bn");
            String bankNumber = request.getParameter("bnum");
            String branch = request.getParameter("br");
            String statusStr = request.getParameter("st");
            String approvedName = request.getParameter("ann");

            // Validate required params
            if (customerName == null || customerName.trim().isEmpty()
                    || bankName == null || bankName.trim().isEmpty()
                    || bankNumber == null || bankNumber.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "5");
                response.put("message", "Missing required parameters");
                return response.toString();
            }

            customerName = customerName.trim();
            bankName = bankName.trim();
            bankNumber = bankNumber.trim();
            if (branch != null) branch = branch.trim();

            int type = 0;
            if (typeStr != null && !typeStr.isEmpty()) {
                if ("insert".equalsIgnoreCase(typeStr) || "create".equalsIgnoreCase(typeStr)) {
                    type = 0;
                } else if ("update".equalsIgnoreCase(typeStr)) {
                    type = 1;
                } else {
                    try {
                        type = Integer.parseInt(typeStr);
                    } catch (NumberFormatException e) {
                        response.put("success", false);
                        response.put("errorCode", "5");
                        response.put("message", "Invalid type parameter");
                        return response.toString();
                    }
                }
            }

            int status = 1;
            try {
                if (statusStr != null && !statusStr.isEmpty()) {
                    status = Integer.parseInt(statusStr);
                }
            } catch (NumberFormatException ignored) {
            }

            String editor = (approvedName != null && !approvedName.trim().isEmpty()) ? approvedName.trim() : "admin";
            Timestamp now = new Timestamp(System.currentTimeMillis());

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // 2026-05-12: resolve bank_id from the bn param (may be either
                // a code "CITI" or a canonical name "씨티은행"). Used to populate
                // admin_banks.bank_id so canonical names flow through readers
                // via the JOIN.
                int resolvedBankId = -1;
                try (PreparedStatement bs = conn.prepareStatement(
                        "SELECT id FROM banks WHERE code = ? OR bank_name = ? LIMIT 1")) {
                    bs.setString(1, bankName);
                    bs.setString(2, bankName);
                    try (java.sql.ResultSet brs = bs.executeQuery()) {
                        if (brs.next()) resolvedBankId = brs.getInt("id");
                    }
                }

                if (type == 0) {
                    // Insert
                    String sql = "INSERT INTO admin_banks (customer_name, bank_name, bank_id, bank_number, branch, status, last_editor, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, customerName);
                        ps.setString(2, bankName);
                        if (resolvedBankId > 0) ps.setInt(3, resolvedBankId);
                        else ps.setNull(3, java.sql.Types.INTEGER);
                        ps.setString(4, bankNumber);
                        ps.setString(5, branch != null ? branch : "");
                        ps.setInt(6, status);
                        ps.setString(7, editor);
                        ps.setTimestamp(8, now);
                        ps.setTimestamp(9, now);
                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("message", "OK");
                        } else {
                            response.put("success", false);
                            response.put("errorCode", "5");
                            response.put("message", "Insert failed");
                        }
                    }
                } else if (type == 1) {
                    // Update
                    int id;
                    try {
                        id = Integer.parseInt(idStr);
                    } catch (NumberFormatException e) {
                        response.put("success", false);
                        response.put("errorCode", "5");
                        response.put("message", "Invalid id parameter");
                        return response.toString();
                    }

                    String sql = "UPDATE admin_banks SET customer_name=?, bank_name=?, bank_id=?, bank_number=?, branch=?, status=?, last_editor=?, updated_at=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, customerName);
                        ps.setString(2, bankName);
                        if (resolvedBankId > 0) ps.setInt(3, resolvedBankId);
                        else ps.setNull(3, java.sql.Types.INTEGER);
                        ps.setString(4, bankNumber);
                        ps.setString(5, branch != null ? branch : "");
                        ps.setInt(6, status);
                        ps.setString(7, editor);
                        ps.setTimestamp(8, now);
                        ps.setInt(9, id);
                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("message", "OK");
                        } else {
                            response.put("success", false);
                            response.put("errorCode", "5");
                            response.put("message", "No record found with id " + id);
                        }
                    }
                } else {
                    response.put("success", false);
                    response.put("errorCode", "5");
                    response.put("message", "Invalid type: must be 0 (insert) or 1 (update)");
                }
            }
        } catch (Exception e) {
            logger.error("InsertOrUpdateAdminBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
            response.put("message", e.getMessage());
        }
        return response.toString();
    }
}
