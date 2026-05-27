package com.vinplay.api.backend.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Validates commission_rate constraints for agency hierarchy.
 *
 * Rules (Spec §2.3, §8.1):
 *   1. Rate must be 0–100%
 *   2. Rate must not exceed parent's rate (child ≤ parent)
 *   3. Rate must not be lower than any direct child's rate (parent ≥ children)
 *
 * Returns null if valid, error message string if invalid.
 */
public class CommissionRateValidator {

    /**
     * Validate a commission rate change for an agent.
     *
     * @param adminConn  Connection to vinplay_admin database
     * @param agentId    The agent being updated (0 for new agent)
     * @param newRate    The proposed new commission_rate (percentage, e.g. 1.5 = 1.5%)
     * @param parentId   The agent's parent ID (-1 for top-level TĐL)
     * @return null if valid, error message if invalid
     */
    public static String validate(Connection adminConn, int agentId, double newRate, int parentId) {
        // Rule 1: Range check
        if (newRate < 0) {
            return "Commission rate cannot be negative: " + newRate + "%";
        }
        if (newRate > 100) {
            return "Commission rate cannot exceed 100%: " + newRate + "%";
        }

        try {
            // Rule 2: Cannot exceed parent's rate
            if (parentId > 0) {
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT commission_rate FROM useragent WHERE id = ?")) {
                    ps.setInt(1, parentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            double parentRate = rs.getDouble("commission_rate");
                            if (newRate > parentRate) {
                                return "Rate " + newRate + "% exceeds parent agent rate " + parentRate + "%";
                            }
                        }
                    }
                }
            }

            // Rule 3: Cannot be lower than any direct child's rate
            if (agentId > 0) {
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "SELECT id, nickname, commission_rate FROM useragent WHERE parentid = ? AND commission_rate > ?")) {
                    ps.setInt(1, agentId);
                    ps.setDouble(2, newRate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String childNick = rs.getString("nickname");
                            double childRate = rs.getDouble("commission_rate");
                            return "Rate " + newRate + "% is lower than child agent '" + childNick + "' rate " + childRate + "%";
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "Validation error: " + e.getMessage();
        }

        return null; // Valid
    }
}
