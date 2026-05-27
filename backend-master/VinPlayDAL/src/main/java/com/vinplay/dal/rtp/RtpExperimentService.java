package com.vinplay.dal.rtp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.rtp.RtpExperiment;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RtpExperimentService {
    private static final Logger logger = Logger.getLogger("api");
    private static final String MAP_EXPERIMENTS = "cacheRtpExperiments";
    private static volatile IMap<String, String> cachedExperimentMap = null;
    // Performance: reuse ObjectMapper (thread-safe)
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static IMap<String, String> getExperimentMap() {
        if (cachedExperimentMap == null) {
            synchronized (RtpExperimentService.class) {
                if (cachedExperimentMap == null)
                    cachedExperimentMap = HazelcastClientFactory.getInstance().getMap(MAP_EXPERIMENTS);
            }
        }
        return cachedExperimentMap;
    }

    public boolean createExperiment(RtpExperiment exp) {
        String sql = "INSERT INTO rtp_experiment (name, game_code, bucket_json, status, created_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql)) {
            sttm.setString(1, exp.getName());
            sttm.setString(2, exp.getGameCode());
            sttm.setString(3, exp.getBucketJson());
            sttm.setString(4, exp.getStatus());
            sttm.setString(5, exp.getCreatedBy());
            boolean ok = sttm.executeUpdate() > 0;
            if (ok && "RUNNING".equals(exp.getStatus())) {
                getExperimentMap().put(exp.getGameCode(), exp.getBucketJson());
            }
            return ok;
        } catch (SQLException e) {
            logger.error("Error createExperiment", e);
            return false;
        }
    }

    public List<RtpExperiment> listExperiments() {
        List<RtpExperiment> list = new ArrayList<>();
        String sql = "SELECT * FROM rtp_experiment";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement sttm = conn.prepareStatement(sql);
             ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                RtpExperiment e = new RtpExperiment();
                e.setId(rs.getLong("id"));
                e.setName(rs.getString("name"));
                e.setGameCode(rs.getString("game_code"));
                e.setBucketJson(rs.getString("bucket_json"));
                e.setStatus(rs.getString("status"));
                e.setStartedAt(rs.getString("started_at"));
                e.setEndedAt(rs.getString("ended_at"));
                e.setWinnerBucket(rs.getString("winner_bucket"));
                e.setCreatedBy(rs.getString("created_by"));
                e.setCreatedAt(rs.getString("created_at"));
                list.add(e);
            }
        } catch (SQLException e) {
            logger.error("Error listExperiments", e);
        }
        return list;
    }

    public static Double getExperimentRtpForUser(long userId, String gameCode) {
        String bucketJsonStr = getExperimentMap().get(gameCode);
        if (bucketJsonStr == null) return null;

        try {
            List<Map<String, Object>> buckets = MAPPER.readValue(bucketJsonStr, new TypeReference<List<Map<String, Object>>>(){});
            if (buckets == null || buckets.isEmpty()) return null;

            // Bug fix: userId % 100 can be negative for large IDs — use Math.abs
            int hash = (int) (Math.abs(userId) % 100);
            double cumulativeShare = 0;

            for (Map<String, Object> bucket : buckets) {
                double share = Double.parseDouble(bucket.get("share").toString()) * 100;
                cumulativeShare += share;
                if (hash < cumulativeShare) {
                    return Double.parseDouble(bucket.get("pct").toString());
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing bucket_json", e);
        }
        return null;
    }
}
