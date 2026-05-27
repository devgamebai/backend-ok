package com.vinplay.dal.rtp;

import com.vinplay.vbee.common.models.rtp.GameRtpSchedule;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RtpScheduleService {
    private static final Logger logger = Logger.getLogger("api");

    public boolean createSchedule(GameRtpSchedule schedule) {
        String sql = "INSERT INTO game_rtp_schedule (game_code, cron_expr, win_rate_pct, duration_min, active, created_by, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setString(1, schedule.getGameCode());
            sttm.setString(2, schedule.getCronExpr());
            sttm.setDouble(3, schedule.getWinRatePct());
            sttm.setInt(4, schedule.getDurationMin());
            sttm.setInt(5, schedule.getActive());
            sttm.setString(6, schedule.getCreatedBy());
            sttm.setString(7, schedule.getDescription());
            sttm.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error createSchedule", e);
            return false;
        }
    }

    public List<GameRtpSchedule> listActiveSchedules() {
        List<GameRtpSchedule> list = new ArrayList<>();
        String sql = "SELECT * FROM game_rtp_schedule WHERE active = 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql);
             ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                GameRtpSchedule s = new GameRtpSchedule();
                s.setId(rs.getLong("id"));
                s.setGameCode(rs.getString("game_code"));
                s.setCronExpr(rs.getString("cron_expr"));
                s.setWinRatePct(rs.getDouble("win_rate_pct"));
                s.setDurationMin(rs.getInt("duration_min"));
                s.setActive(rs.getInt("active"));
                s.setCreatedBy(rs.getString("created_by"));
                s.setCreatedAt(rs.getString("created_at"));
                s.setLastFiredAt(rs.getString("last_fired_at"));
                s.setDescription(rs.getString("description"));
                list.add(s);
            }
        } catch (SQLException e) {
            logger.error("Error listActiveSchedules", e);
        }
        return list;
    }

    public boolean deleteSchedule(long id) {
        String sql = "DELETE FROM game_rtp_schedule WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setLong(1, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleteSchedule", e);
            return false;
        }
    }

    public void updateLastFiredAt(long id) {
        String sql = "UPDATE game_rtp_schedule SET last_fired_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setLong(1, id);
            sttm.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updateLastFiredAt", e);
        }
    }
}
