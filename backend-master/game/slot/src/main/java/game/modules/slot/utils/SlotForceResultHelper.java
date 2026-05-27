package game.modules.slot.utils;

import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import org.apache.log4j.Logger;

/**
 * Checks Hazelcast for admin-set forced slot results.
 * Used by all slot Room classes before matrix generation.
 *
 * Hazelcast map: "cacheForceResult"
 * Key format: "{GAME_NAME}:{username}"
 * Value: JSON string {"game":"KHOBAU","username":"player1","matrix":[0,1,2,...],"createdBy":"admin","createdAt":"..."}
 *
 * The force result is consumed (removed) after use — one-shot only.
 */
public class SlotForceResultHelper {

    private static final Logger logger = Logger.getLogger("api");
    private static final String MAP_NAME = "cacheForceResult";

    /**
     * Check if there's a forced result for this user+game.
     * If found, parse the matrix, remove the entry, and return the matrix.
     * Returns null if no force result is pending.
     *
     * @param gameName e.g. "KHOBAU", "NUDIEPVIEN", "VQV", "AVENGER"
     * @param username player nickname
     * @param rows grid rows (typically 3)
     * @param cols grid columns (typically 5)
     * @return int[][] matrix [rows][cols] or null
     */
    public static int[][] checkAndConsume(String gameName, String username, int rows, int cols) {
        if (username == null || gameName == null) return null;

        try {
            DistCache<String, String> forceMap = CacheFactory.get(MAP_NAME, String.class);
            String key = gameName + ":" + username;

            String json = forceMap.remove(key); // atomic get-and-remove
            if (json == null) return null;

            logger.info("SlotForceResult: CONSUMED force result for " + key);

            // Parse matrix from JSON — extract "matrix" array
            // Simple JSON parsing without external lib (avoid dependency issues)
            int startIdx = json.indexOf("\"matrix\":[");
            if (startIdx < 0) {
                logger.warn("SlotForceResult: invalid JSON (no matrix field) for " + key);
                return null;
            }
            startIdx = json.indexOf("[", startIdx);
            int endIdx = json.indexOf("]", startIdx);
            if (startIdx < 0 || endIdx < 0) {
                logger.warn("SlotForceResult: invalid JSON (bad matrix array) for " + key);
                return null;
            }

            String matrixStr = json.substring(startIdx + 1, endIdx);
            String[] parts = matrixStr.split(",");
            if (parts.length != rows * cols) {
                logger.warn("SlotForceResult: matrix size mismatch for " + key
                        + " expected=" + (rows * cols) + " got=" + parts.length);
                return null;
            }

            int[][] matrix = new int[rows][cols];
            for (int i = 0; i < parts.length; i++) {
                matrix[i / cols][i % cols] = Integer.parseInt(parts[i].trim());
            }

            logger.info("SlotForceResult: applied forced matrix for " + key
                    + " matrix=" + matrixStr);
            return matrix;

        } catch (Exception e) {
            logger.error("SlotForceResult: error checking force result for "
                    + gameName + ":" + username, e);
            return null;
        }
    }
}
