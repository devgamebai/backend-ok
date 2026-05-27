package com.vinplay.dal.rebate;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertTrue;

/**
 * Real DB integration checks for rebate tables.
 *
 * Run only when REBATE_IT_ENABLE=1 is provided.
 */
public class RebateServiceIntegrationDbTest {

    @BeforeClass
    public static void beforeClass() {
        String enabled = System.getenv("REBATE_IT_ENABLE");
        Assume.assumeTrue("Integration test disabled. Set REBATE_IT_ENABLE=1 to run.", "1".equals(enabled));
        try {
            ConnectionPool cp = ConnectionPool.getInstance();
            Assume.assumeTrue("ConnectionPool is not initialized for integration test", cp != null);
            Connection conn = cp.getConnection("mysqlpoolname");
            Assume.assumeTrue("mysqlpoolname is not available for integration test", conn != null);
            conn.close();
        } catch (Throwable t) {
            Assume.assumeNoException("Integration DB is not reachable/configured", t);
        }
    }

    @Test
    public void testTablesExist() throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            assertTrue(tableExists(conn, "rebate_config"));
            assertTrue(tableExists(conn, "rebate_logs"));
            assertTrue(tableExists(conn, "rebate_payout"));
        }
    }

    @Test
    public void testCrudFlowConfigLogPayout() throws Exception {
        int testAgentId = -99001;
        String testNick = "it_rebate_agent";

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // 1) Upsert config
            assertTrue(RebateService.upsertConfig(testAgentId, testNick, 1.25, 0.25, "WEEKLY", 0, true));

            // 2) Create log
            long logId = RebateService.createRebateLog(
                    testAgentId, testNick,
                    "2026-03-16 00:00:00", "2026-03-22 23:59:59", "WEEKLY",
                    1_000_000L, 1.25, 12_500L, 0.25, 2_500L, 10_000L
            );
            assertTrue(logId > 0);

            // 3) Claim -> paid -> create payout
            assertTrue(RebateService.claimForPayout(logId, "it_admin"));
            assertTrue(RebateService.markPaid(logId, "it_admin"));
            long payoutId = RebateService.createPayout(logId, testAgentId, testNick, 10_000L, 0, 0, "it_admin", "it");
            assertTrue(payoutId > 0);

            // 4) Cleanup test data
            deleteByAgentId(conn, "rebate_payout", testAgentId);
            deleteByAgentId(conn, "rebate_logs", testAgentId);
            deleteByAgentId(conn, "rebate_config", testAgentId);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SHOW TABLES LIKE ?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void deleteByAgentId(Connection conn, String table, int agentUserId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE agent_user_id = ?")) {
            ps.setInt(1, agentUserId);
            ps.executeUpdate();
        }
    }
}
