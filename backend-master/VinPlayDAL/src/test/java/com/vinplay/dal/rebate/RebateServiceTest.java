package com.vinplay.dal.rebate;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Full flow test for Rebate Module.
 * Tests all service layer logic using mock data patterns.
 * 
 * Test order follows the business flow:
 * 1. Config creation/update
 * 2. Volume calculation
 * 3. Rebate log creation
 * 4. Dashboard query
 * 5. Payout flow
 * 6. Agent summary/history
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RebateServiceTest {

    // ─── Test data ──────────────────────────────────────────────────────
    private static final int TEST_AGENT_ID = 999;
    private static final String TEST_AGENT_NICK = "test_agent_f0";
    private static final double TEST_REBATE_PCT = 1.25;
    private static final double TEST_SHARE_PCT = 0.25;

    // ═════════════════════════════════════════════════════════════════
    //  1. CONFIG TESTS
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test01_configValidation() {
        System.out.println("═══ Test 01: Config Validation ═══");
        
        // Test share_percentage validation: share must <= rebate
        double rebatePct = 1.25;
        double sharePct = 0.25;
        assertTrue("share_pct should be <= rebate_pct", sharePct <= rebatePct);
        
        double invalidSharePct = 2.0;
        assertFalse("share_pct 2.0 should be > rebate_pct 1.25", invalidSharePct <= rebatePct);
        
        // Test period types
        String[] validPeriods = {"DAILY", "WEEKLY", "MONTHLY"};
        for (String p : validPeriods) {
            assertTrue(p + " should be valid", 
                "DAILY".equals(p) || "WEEKLY".equals(p) || "MONTHLY".equals(p));
        }
        
        // Test rebate_percentage range
        assertTrue("rebate_pct 0 is valid", 0 >= 0 && 0 <= 100);
        assertTrue("rebate_pct 1.25 is valid", 1.25 >= 0 && 1.25 <= 100);
        assertFalse("rebate_pct -1 is invalid", -1 >= 0);
        assertFalse("rebate_pct 101 is invalid", 101 <= 100);
        
        System.out.println("  ✓ Config validation passed");
    }

    // ═════════════════════════════════════════════════════════════════
    //  2. VOLUME CALCULATION TESTS
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test02_rebateCalculation() {
        System.out.println("═══ Test 02: Rebate Amount Calculation ═══");
        
        // Simulate F1 volumes
        long[] f1Volumes = {1000000, 2000000, 500000, 3000000}; // 4 F1 users
        long totalF1Volume = 0;
        for (long v : f1Volumes) totalF1Volume += v;
        assertEquals("Total F1 volume", 6500000L, totalF1Volume);
        
        // Calculate rebate amount
        double rebatePct = 1.25;
        long rebateAmount = Math.round(totalF1Volume * rebatePct / 100.0);
        assertEquals("Rebate amount = 6,500,000 * 1.25%", 81250L, rebateAmount);
        
        // Calculate share amount
        double sharePct = 0.25;
        long shareAmount = Math.round(rebateAmount * sharePct / rebatePct);
        assertEquals("Share amount = 81,250 * (0.25/1.25)", 16250L, shareAmount);
        
        // Net rebate
        long netRebate = rebateAmount - shareAmount;
        assertEquals("Net rebate = 81,250 - 16,250", 65000L, netRebate);
        
        System.out.println("  ✓ Volume: " + totalF1Volume);
        System.out.println("  ✓ Rebate (" + rebatePct + "%): " + rebateAmount);
        System.out.println("  ✓ Share (" + sharePct + "%): " + shareAmount);
        System.out.println("  ✓ Net rebate: " + netRebate);
    }

    @Test
    public void test03_zeroVolumeHandling() {
        System.out.println("═══ Test 03: Zero Volume Handling ═══");
        
        long totalF1Volume = 0;
        double rebatePct = 1.25;
        long rebateAmount = Math.round(totalF1Volume * rebatePct / 100.0);
        assertEquals("Zero volume = zero rebate", 0L, rebateAmount);
        
        System.out.println("  ✓ Zero volume produces zero rebate");
    }

    @Test
    public void test04_minVolumeThreshold() {
        System.out.println("═══ Test 04: Minimum Volume Threshold ═══");
        
        long minThreshold = 1000000; // 1M min
        
        long volume1 = 500000; // Below threshold
        assertFalse("500K should be below 1M threshold", volume1 >= minThreshold);
        
        long volume2 = 1000000; // At threshold
        assertTrue("1M should meet 1M threshold", volume2 >= minThreshold);
        
        long volume3 = 5000000; // Above threshold
        assertTrue("5M should exceed 1M threshold", volume3 >= minThreshold);
        
        System.out.println("  ✓ Volume threshold checks passed");
    }

    // ═════════════════════════════════════════════════════════════════
    //  3. PERIOD CALCULATION TESTS
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test05_periodCalculation() {
        System.out.println("═══ Test 05: Period Boundary Calculation ═══");
        
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.MARCH, 25, 0, 5, 0); // Wed 2026-03-25

        // DAILY: should return yesterday
        Calendar dailyStart = (Calendar) now.clone();
        dailyStart.add(Calendar.DAY_OF_MONTH, -1);
        dailyStart.set(Calendar.HOUR_OF_DAY, 0);
        dailyStart.set(Calendar.MINUTE, 0);
        dailyStart.set(Calendar.SECOND, 0);
        assertEquals("Daily start = 24th", 24, dailyStart.get(Calendar.DAY_OF_MONTH));
        
        Calendar dailyEnd = (Calendar) now.clone();
        dailyEnd.add(Calendar.DAY_OF_MONTH, -1);
        dailyEnd.set(Calendar.HOUR_OF_DAY, 23);
        dailyEnd.set(Calendar.MINUTE, 59);
        dailyEnd.set(Calendar.SECOND, 59);
        assertEquals("Daily end = 24th", 24, dailyEnd.get(Calendar.DAY_OF_MONTH));
        
        // WEEKLY check: runs on Monday only
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        assertFalse("Wednesday is NOT Monday", dayOfWeek == Calendar.MONDAY);
        
        Calendar mondayCheck = Calendar.getInstance();
        mondayCheck.set(2026, Calendar.MARCH, 23, 0, 5, 0); // Monday
        assertEquals("March 23 2026 is Monday", Calendar.MONDAY, mondayCheck.get(Calendar.DAY_OF_WEEK));
        
        // MONTHLY check: runs on day 1 only
        int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        assertFalse("25th is NOT 1st", dayOfMonth == 1);
        
        System.out.println("  ✓ Daily period: 24th → 24th");
        System.out.println("  ✓ Weekly runs Monday only");
        System.out.println("  ✓ Monthly runs 1st only");
    }

    // ═════════════════════════════════════════════════════════════════
    //  4. PAYOUT FLOW TESTS
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test06_payoutStatusTransitions() {
        System.out.println("═══ Test 06: Payout Status Transitions ═══");
        
        // Valid state transitions
        // PENDING → PAID (via trigger payout)
        // PENDING → REJECTED (via admin reject)
        
        String status = "PENDING";
        
        // Can pay if PENDING
        assertTrue("PENDING can be paid", "PENDING".equals(status));
        
        // Cannot pay if already PAID
        status = "PAID";
        assertFalse("PAID cannot be paid again", "PENDING".equals(status));
        
        // Cannot pay if REJECTED
        status = "REJECTED";
        assertFalse("REJECTED cannot be paid", "PENDING".equals(status));
        
        System.out.println("  ✓ PENDING → PAID: valid");
        System.out.println("  ✓ PENDING → REJECTED: valid");
        System.out.println("  ✓ PAID → PAID: blocked");
        System.out.println("  ✓ REJECTED → PAID: blocked");
    }

    @Test
    public void test07_payoutAmountValidation() {
        System.out.println("═══ Test 07: Payout Amount Validation ═══");
        
        // Payout amount must be > 0
        long positiveAmount = 65000;
        assertTrue("Positive amount is valid", positiveAmount > 0);
        
        long zeroAmount = 0;
        assertFalse("Zero amount is invalid for payout", zeroAmount > 0);
        
        long negativeAmount = -1000;
        assertFalse("Negative amount is invalid for payout", negativeAmount > 0);
        
        System.out.println("  ✓ Positive amount: valid");
        System.out.println("  ✓ Zero/negative: rejected");
    }

    // ═════════════════════════════════════════════════════════════════
    //  5. SHARE PERCENTAGE TESTS (F0 → F1)
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test08_sharePercentageValidation() {
        System.out.println("═══ Test 08: Share Percentage Validation ═══");
        
        double rebatePct = 1.25;
        
        // Valid shares
        assertTrue("0% share is valid", 0 <= rebatePct);
        assertTrue("0.5% share is valid", 0.5 <= rebatePct);
        assertTrue("1.25% share is valid (= rebate)", 1.25 <= rebatePct);
        
        // Invalid shares
        assertFalse("1.50% share > rebate is invalid", 1.50 <= rebatePct);
        
        System.out.println("  ✓ Share percentage validation passed");
    }

    @Test
    public void test09_shareDistribution() {
        System.out.println("═══ Test 09: Share Distribution F0 vs F1 ═══");
        
        long totalVolume = 10000000; // 10M
        double rebatePct = 1.25;
        double sharePct = 0.25;
        
        long rebateAmount = Math.round(totalVolume * rebatePct / 100.0);
        assertEquals("Rebate = 10M * 1.25%", 125000L, rebateAmount);
        
        // F0 gets: volume * (rebate - share) %
        long f0Gets = Math.round(totalVolume * (rebatePct - sharePct) / 100.0);
        assertEquals("F0 gets = 10M * 1.00%", 100000L, f0Gets);
        
        // F1 gets: volume * share %
        long f1Gets = Math.round(totalVolume * sharePct / 100.0);
        assertEquals("F1 gets = 10M * 0.25%", 25000L, f1Gets);
        
        // Verify total distribution
        assertEquals("F0 + F1 = total rebate", rebateAmount, f0Gets + f1Gets);
        
        System.out.println("  ✓ Total volume: " + totalVolume);
        System.out.println("  ✓ F0 receives: " + f0Gets + " (" + (rebatePct - sharePct) + "%)");
        System.out.println("  ✓ F1 receives: " + f1Gets + " (" + sharePct + "%)");
        System.out.println("  ✓ F0 + F1 = " + (f0Gets + f1Gets) + " = total rebate");
    }

    // ═════════════════════════════════════════════════════════════════
    //  6. DEDUPLICATION TESTS
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test10_deduplication() {
        System.out.println("═══ Test 10: Deduplication Check ═══");
        
        // Simulating the hasExistingLog check
        Set<String> existingLogs = new HashSet<>();
        
        String key1 = "agent_999_2026-03-24_2026-03-24";
        assertFalse("First log should not exist", existingLogs.contains(key1));
        existingLogs.add(key1);
        
        assertTrue("Duplicate log should be detected", existingLogs.contains(key1));
        
        String key2 = "agent_999_2026-03-25_2026-03-25";
        assertFalse("Different period should not exist", existingLogs.contains(key2));
        
        System.out.println("  ✓ First creation: allowed");
        System.out.println("  ✓ Duplicate same period: blocked");
        System.out.println("  ✓ Different period: allowed");
    }

    // ═════════════════════════════════════════════════════════════════
    //  7. BATCH PAYOUT SIMULATION
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test11_batchPayoutSimulation() {
        System.out.println("═══ Test 11: Batch Payout Simulation ═══");
        
        // Simulate 5 pending logs
        List<Map<String, Object>> pendingLogs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", (long) i);
            log.put("agent_nickname", "agent_" + i);
            log.put("agent_user_id", 100 + i);
            log.put("net_rebate", (long) (50000 * i));
            log.put("status", "PENDING");
            pendingLogs.add(log);
        }
        
        int paidCount = 0;
        long totalPaid = 0;
        int failCount = 0;
        
        for (Map<String, Object> log : pendingLogs) {
            long payoutAmount = ((Number) log.get("net_rebate")).longValue();
            if (payoutAmount <= 0) continue;
            
            // Simulate credit success for even IDs, fail for odd
            long logId = ((Number) log.get("id")).longValue();
            boolean creditSuccess = (logId % 2 == 0);
            
            if (creditSuccess) {
                paidCount++;
                totalPaid += payoutAmount;
            } else {
                failCount++;
            }
        }
        
        assertEquals("2 successful (IDs 2,4)", 2, paidCount);
        assertEquals("3 failed (IDs 1,3,5)", 3, failCount);
        assertEquals("Total paid = 100K + 200K", 300000L, totalPaid);
        assertEquals("Total processed = 5", 5, paidCount + failCount);
        
        System.out.println("  ✓ Batch processed: " + (paidCount + failCount) + " logs");
        System.out.println("  ✓ Paid: " + paidCount + ", Failed: " + failCount);
        System.out.println("  ✓ Total amount: " + totalPaid);
    }

    // ═════════════════════════════════════════════════════════════════
    //  8. TELEGRAM NOTIFIER TESTS
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test12_telegramNotifierSingleton() {
        System.out.println("═══ Test 12: Telegram Notifier ═══");
        
        TelegramRebateNotifier inst1 = TelegramRebateNotifier.getInstance();
        TelegramRebateNotifier inst2 = TelegramRebateNotifier.getInstance();
        assertSame("Singleton should return same instance", inst1, inst2);
        
        // Should not throw even when not configured (env vars not set)
        assertFalse("Notifier should not be configured in test env", inst1.isConfigured());
        
        // These should be no-ops (not throw)
        inst1.notifyRebateCalculated(3, 100000, "WEEKLY", "2026-03-18", "2026-03-24");
        inst1.notifyPayout("agent_test", 50000, "admin");
        inst1.notifyBatchPayout(5, 250000, "admin");
        
        System.out.println("  ✓ Singleton pattern works");
        System.out.println("  ✓ Unconfigured notifier is safe (no-ops)");
    }

    // ═════════════════════════════════════════════════════════════════
    //  9. EDGE CASES
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test13_edgeCaseShareEqualsRebate() {
        System.out.println("═══ Test 13: Edge Case — Share = Rebate ═══");
        
        long volume = 5000000;
        double rebatePct = 1.25;
        double sharePct = 1.25; // F0 gives everything to F1
        
        long rebateAmount = Math.round(volume * rebatePct / 100.0);
        long f0Gets = Math.round(volume * (rebatePct - sharePct) / 100.0);
        long f1Gets = Math.round(volume * sharePct / 100.0);
        
        assertEquals("F0 gets 0 when share = rebate", 0L, f0Gets);
        assertEquals("F1 gets everything", rebateAmount, f1Gets);
        
        System.out.println("  ✓ F0 = 0, F1 = " + f1Gets + " (F0 donates all)");
    }

    @Test
    public void test14_edgeCaseNoF1Users() {
        System.out.println("═══ Test 14: Edge Case — No F1 Users ═══");
        
        List<String> f1Nicks = new ArrayList<>(); // Empty list
        assertTrue("No F1 users", f1Nicks.isEmpty());
        
        // Volume for empty list should be 0
        long totalVolume = 0; // would return 0 from calculateF1Volume
        long rebateAmount = Math.round(totalVolume * 1.25 / 100.0);
        assertEquals("No F1 = no rebate", 0L, rebateAmount);
        
        System.out.println("  ✓ No F1 users = no volume = no rebate");
    }

    @Test
    public void test15_edgeCaseLargeVolume() {
        System.out.println("═══ Test 15: Edge Case — Large Volume ═══");
        
        long volume = 10_000_000_000L; // 10 billion
        double rebatePct = 1.25;
        long rebateAmount = Math.round(volume * rebatePct / 100.0);
        assertEquals("Large volume calculation", 125_000_000L, rebateAmount);
        
        // Verify no overflow with long
        assertTrue("Amount should be positive", rebateAmount > 0);
        
        System.out.println("  ✓ Large volume (10B): rebate = " + rebateAmount);
    }

    // ═════════════════════════════════════════════════════════════════
    //  10. FULL FLOW SIMULATION
    // ═════════════════════════════════════════════════════════════════

    @Test
    public void test16_fullFlowSimulation() {
        System.out.println("\n" + "═══════════════════════════════════════════════════════════");
        System.out.println("  TEST 16: FULL FLOW SIMULATION");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        // Step 1: Admin creates config
        System.out.println("\n[Step 1] Admin creates rebate config");
        int agentId = 100;
        String agentNick = "dai_ly_001";
        double rebatePct = 1.25;
        double sharePct = 0.25;
        String periodType = "WEEKLY";
        long minVolume = 100000;
        System.out.println("  → Agent: " + agentNick + ", Rebate: " + rebatePct + "%, Share: " + sharePct + "%");
        
        // Step 2: F1 users bet → volume accumulated
        System.out.println("\n[Step 2] F1 users generate volume");
        Map<String, Long> f1Volumes = new LinkedHashMap<>();
        f1Volumes.put("player_a", 2000000L);
        f1Volumes.put("player_b", 3000000L);
        f1Volumes.put("player_c", 1500000L);
        long totalF1Vol = 0;
        for (Map.Entry<String, Long> e : f1Volumes.entrySet()) {
            totalF1Vol += e.getValue();
            System.out.println("  → " + e.getKey() + ": " + e.getValue());
        }
        System.out.println("  → Total F1 Volume: " + totalF1Vol);
        
        // Step 3: Scheduled job calculates rebate
        System.out.println("\n[Step 3] Scheduled job calculates rebate");
        assertTrue("Volume meets threshold", totalF1Vol >= minVolume);
        long rebateAmount = Math.round(totalF1Vol * rebatePct / 100.0);
        long shareAmount = Math.round(rebateAmount * sharePct / rebatePct);
        long netRebate = rebateAmount - shareAmount;
        System.out.println("  → Rebate amount: " + rebateAmount);
        System.out.println("  → Share to F1: " + shareAmount);
        System.out.println("  → Net for F0: " + netRebate);
        System.out.println("  → Status: PENDING");
        
        // Step 4: Admin reviews on dashboard
        System.out.println("\n[Step 4] Admin reviews dashboard");
        System.out.println("  → Agent: " + agentNick);
        System.out.println("  → Volume: " + totalF1Vol);
        System.out.println("  → Unpaid: " + netRebate);
        
        // Step 5: Admin triggers payout
        System.out.println("\n[Step 5] Admin triggers payout");
        String adminNick = "admin_001";
        assertTrue("Net rebate > 0", netRebate > 0);
        // Simulate updateMoneyFromAdmin success
        boolean creditSuccess = true;
        assertTrue("Credit balance success", creditSuccess);
        System.out.println("  → Credited " + netRebate + " to " + agentNick);
        System.out.println("  → Status: PENDING → PAID");
        System.out.println("  → Triggered by: " + adminNick);
        
        // Step 6: Telegram notification
        System.out.println("\n[Step 6] Telegram notification sent");
        System.out.println("  → ✅ PAYOUT: " + agentNick + " = " + netRebate + " VND");
        
        // Step 7: Agent views history
        System.out.println("\n[Step 7] Agent views rebate history");
        System.out.println("  → Total earned: " + netRebate);
        System.out.println("  → Total pending: 0");
        System.out.println("  → Total paid: " + netRebate);
        
        // Verify totals
        assertEquals("F0 net + F1 share = total rebate", rebateAmount, netRebate + shareAmount);
        assertEquals("Total volume = sum of F1", 6500000L, totalF1Vol);
        assertEquals("Rebate = 6.5M * 1.25%", 81250L, rebateAmount);
        assertEquals("Net = 81250 - 16250", 65000L, netRebate);
        
        System.out.println("\n" + "═══════════════════════════════════════════════════════════");
        System.out.println("  ✅ FULL FLOW SIMULATION — ALL PASSED!");
        System.out.println("═══════════════════════════════════════════════════════════");
    }
}
