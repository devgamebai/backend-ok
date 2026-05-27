package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * c=9892 — Get Agent Hierarchy Tree
 *
 * Returns a nested JSON structure (tree) of all downline agents for a given agent.
 *
 * Params: rc (agent nickname or code)
 */
public class GetAgentHierarchyTreeProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String rc = request.getParameter("rc");

            if (rc == null || rc.trim().isEmpty()) {
                return err(response, "1001", "rc (agent code or nickname) required");
            }

            // 1. Resolve root agent
            AgentInfo rootAgent = AgentHierarchyHelper.resolveAgent(rc);
            if (rootAgent == null) {
                return err(response, "1002", "Agent not found");
            }

            // 2. Get all agent IDs in subtree (including root)
            List<Integer> subtreeIds = AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(rootAgent.id);
            if (subtreeIds.isEmpty()) {
                // Fallback safe return
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", new JSONObject());
                return response.toString();
            }

            String inIds = AgentHierarchyHelper.inPlaceholders(subtreeIds.size());

            // 3. Query details of these agents from useragent table
            // We use useragent table because we only want agents (not players)
            Map<Integer, JSONObject> nodeMap = new HashMap<>();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Admin-only master agents (TĐL tier) exist in vinplay_admin.useragent
                // without a paired vinplay.users row. The previous `u.id IS NOT NULL AND`
                // gate turned LEFT JOIN into an implicit INNER JOIN and silently dropped
                // those agents — plus every descendant whose parent got dropped, because
                // nodeMap.get(parentId) returns null in the tree-builder below.
                // Source of truth for the tree is `useragent` (it owns `ancestors` /
                // `parentid`); the LEFT JOIN only enriches with users.parent_agent_id
                // as a drift-safety signal, so each OR branch must stand on its own.
                String sql = "SELECT ua.id AS id, ua.nickname, ua.code, ua.level, " +
                             "IF(u.parent_agent_id IN (" + inIds + "), u.parent_agent_id, ua.parentid) AS actual_parentid, " +
                             "ua.commission_rate, ua.percent_bonus_vincard, ua.status " +
                             "FROM useragent ua " +
                             "LEFT JOIN vinplay.users u ON u.nick_name COLLATE utf8mb4_general_ci = ua.nickname " +
                             "WHERE ua.active = 1 AND (ua.id = ? OR ua.id IN (" + inIds + ") OR u.parent_agent_id IN (" + inIds + ")) " +
                             "ORDER BY ua.level ASC";
                
                try (PreparedStatement stm = conn.prepareStatement(sql)) {
                    int idx = 1;
                    idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds); // Placeholder for IF
                    stm.setInt(idx++, rootAgent.id);                               // Placeholder for ua.id = ?
                    idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds); // Placeholder for ua.id IN
                    idx = AgentHierarchyHelper.setIntParams(stm, idx, subtreeIds); // Placeholder for u.parent_agent_id IN
                    
                    try (ResultSet rs = stm.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            JSONObject node = new JSONObject();
                            node.put("id", id);
                            String nickname = rs.getString("nickname");
                            node.put("nickname", nickname);
                            node.put("mebId", nickname); // Alias for FE mockup
                            node.put("name", nickname);  // Alias for FE mockup
                            String code = rs.getString("code");
                            node.put("code", code != null ? code : "");
                            int level = rs.getInt("level");
                            node.put("level", level);
                            // mebRank: FE tree uses descending rank (higher = more senior).
                            // Formula: 11 - level → TĐL(1)=10, ĐL1(2)=9, ĐL2(3)=8.
                            // If a 4th level is ever added, update this mapping accordingly.
                            node.put("mebRank", 11 - level);
                            node.put("level_name", getLevelName(level));
                            node.put("parentid", rs.getInt("actual_parentid"));
                            // SUN-1098: emit as 2-decimal string so JSONObject doesn't trim trailing zeros.
                            node.put("commission_rate", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "commission_rate"));
                            node.put("status", rs.getInt("status"));
                            node.put("settleType", 1); // TODO: replace with real DB column when available
                            node.put("memberType", 2); // TODO: replace with real DB column when available
                            node.put("childList", new JSONArray());

                            nodeMap.put(id, node);
                        }
                    }
                }
            }

            // 4. Build Tree
            // Loop through all nodes, attach them to their parent's children array
            // Only nodes that match rootAgent.id will be the top-level returned items
            JSONObject rootObj = new JSONObject();

            for (Map.Entry<Integer, JSONObject> entry : nodeMap.entrySet()) {
                JSONObject node = entry.getValue();
                int id = node.getInt("id");
                int parentId = node.getInt("parentid");

                if (id == rootAgent.id) {
                    // This is the root node requested
                    rootObj = node;
                } else {
                    // This is a child node, attach it to parent
                    JSONObject parentNode = nodeMap.get(parentId);
                    if (parentNode != null) {
                        parentNode.getJSONArray("childList").put(node);
                    }
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", rootObj); // Returning 1 object instead of array

        } catch (Exception e) {
            logger.error("GetAgentHierarchyTreeProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String getLevelName(int level) {
        if (level == 1) return "TĐL";
        if (level == 2) return "ĐL1";
        if (level == 3) return "ĐL2";
        return "Level " + level;
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
