package com.vinplay.dal.rtp;

import com.vinplay.vbee.common.models.rtp.RtpAutoPolicy;
import com.vinplay.vbee.common.models.rtp.RtpAutoHistory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RtpAutoTargeterService {
    private static final Logger logger = Logger.getLogger("api");

    public boolean createPolicy(RtpAutoPolicy policy) {
        String sql = "INSERT INTO rtp_auto_policy (policy_name, max_win_amount, time_window_min, action_rtp_pct, action_duration, is_active, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setString(1, policy.getPolicyName());
            sttm.setLong(2, policy.getMaxWinAmount());
            sttm.setInt(3, policy.getTimeWindowMin());
            sttm.setDouble(4, policy.getActionRtpPct());
            sttm.setInt(5, policy.getActionDuration());
            sttm.setInt(6, policy.getIsActive());
            sttm.setString(7, policy.getDescription());
            sttm.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error createPolicy", e);
            return false;
        }
    }

    public List<RtpAutoPolicy> listActivePolicies() {
        List<RtpAutoPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM rtp_auto_policy WHERE is_active = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql);
             ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                RtpAutoPolicy p = new RtpAutoPolicy();
                p.setId(rs.getInt("id"));
                p.setPolicyName(rs.getString("policy_name"));
                p.setMaxWinAmount(rs.getLong("max_win_amount"));
                p.setTimeWindowMin(rs.getInt("time_window_min"));
                p.setActionRtpPct(rs.getDouble("action_rtp_pct"));
                p.setActionDuration(rs.getInt("action_duration"));
                p.setIsActive(rs.getInt("is_active"));
                p.setCreatedAt(rs.getString("created_at"));
                p.setDescription(rs.getString("description"));
                list.add(p);
            }
        } catch (SQLException e) {
            logger.error("Error listActivePolicies", e);
        }
        return list;
    }

    public boolean updatePolicyStatus(int id, int isActive) {
        String sql = "UPDATE rtp_auto_policy SET is_active = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setInt(1, isActive);
            sttm.setInt(2, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updatePolicyStatus", e);
            return false;
        }
    }

    public void logHistory(RtpAutoHistory history) {
        String sql = "INSERT INTO rtp_auto_history (user_id, nick_name, policy_id, trigger_win, applied_rtp, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setInt(1, history.getUserId());
            sttm.setString(2, history.getNickName());
            sttm.setInt(3, history.getPolicyId());
            sttm.setLong(4, history.getTriggerWin());
            sttm.setDouble(5, history.getAppliedRtp());
            sttm.setString(6, history.getExpiresAt());
            sttm.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logHistory", e);
        }
    }
}
