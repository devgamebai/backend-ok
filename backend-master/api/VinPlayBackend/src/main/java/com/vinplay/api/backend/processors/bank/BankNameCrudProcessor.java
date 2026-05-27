package com.vinplay.api.backend.processors.bank;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * c=8819 — Create or update bank name.
 * Params: id (0 or empty = create, >0 = update), bn (bank name), bc (bank code), lg (logo URL), nn (admin name)
 */
public class BankNameCrudProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String idStr = request.getParameter("id");
            String bankName = request.getParameter("bn");
            String bankCode = request.getParameter("bc");
            String logo = request.getParameter("lg");
            String adminName = request.getParameter("nn");

            if (bankName == null || bankName.isEmpty() || bankCode == null || bankCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Bank name and code are required");
                return response.toString();
            }

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            int id = 0;
            if (idStr != null && !idStr.isEmpty()) {
                try { id = Integer.parseInt(idStr); } catch (NumberFormatException ignored) {}
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                if (id > 0) {
                    // Update
                    String sql = "UPDATE banks SET bank_name=?, code=?, logo=?, update_date=?, add_by=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, bankName);
                        ps.setString(2, bankCode);
                        ps.setString(3, logo != null ? logo : "");
                        ps.setString(4, now);
                        ps.setString(5, adminName != null ? adminName : "");
                        ps.setInt(6, id);
                        ps.executeUpdate();
                    }
                    // Cascade the rename to denormalized users_bank.bank_name
                    // snapshots so legacy readers that don't JOIN (e.g.
                    // WithdrawBankProcessor snapshotting into bank_withdrawals)
                    // also see the new name. The c=3002 read path already
                    // returns the live banks.bank_name via the bank_id JOIN,
                    // so this is belt-and-braces. Scoped by bank_id FK
                    // (2026-05-12 migration) so legacy rows with bank_id=NULL
                    // are untouched.
                    String cascade = "UPDATE users_bank SET bank_name=? WHERE bank_id=?";
                    try (PreparedStatement ps = conn.prepareStatement(cascade)) {
                        ps.setString(1, bankName);
                        ps.setInt(2, id);
                        int touched = ps.executeUpdate();
                        if (touched > 0) {
                            logger.info("BankNameCrud: renamed banks.id=" + id + " to '"
                                    + bankName + "' — cascaded to " + touched + " users_bank row(s)");
                        }
                    }
                    // Same cascade for admin_banks (company receiving banks).
                    // 2026-05-12 admin_banks_fk_bank_id migration.
                    String cascadeAdmin = "UPDATE admin_banks SET bank_name=? WHERE bank_id=?";
                    try (PreparedStatement ps = conn.prepareStatement(cascadeAdmin)) {
                        ps.setString(1, bankName);
                        ps.setInt(2, id);
                        int touched = ps.executeUpdate();
                        if (touched > 0) {
                            logger.info("BankNameCrud: cascaded rename to "
                                    + touched + " admin_banks row(s)");
                        }
                    }
                } else {
                    // Create
                    String sql = "INSERT INTO banks (bank_name, code, logo, status, create_date, add_by) VALUES (?, ?, ?, 1, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, bankName);
                        ps.setString(2, bankCode);
                        ps.setString(3, logo != null ? logo : "");
                        ps.setString(4, now);
                        ps.setString(5, adminName != null ? adminName : "");
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) id = rs.getInt(1);
                        }
                    }
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("id", id);

        } catch (Exception e) {
            logger.error("BankNameCrudProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
