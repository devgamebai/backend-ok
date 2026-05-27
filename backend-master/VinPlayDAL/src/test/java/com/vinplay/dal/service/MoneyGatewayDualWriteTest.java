package com.vinplay.dal.service;

import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Integration tests for MoneyGateway Phase 1 dual-write.
 *
 * <p>Verifies that with {@code MONEY_LEDGER_DUAL_WRITE=true}, calling
 * {@code MoneyGateway.creditUser} writes <em>both</em>:
 * <ol>
 *   <li>A row in {@code money_gateway_log} (legacy audit)
 *   <li>A row in {@code money_transaction} (new ledger)
 * </ol>
 *
 * <p>With the flag absent/false the dual-write block is skipped entirely, so
 * only the legacy row is written.
 *
 * <p><b>Prerequisite:</b> sunwinkr-mysql Docker container running on 127.0.0.1:3306
 * with the Phase 0 ledger migration applied (tasks #174–#176). User 5107 must have
 * a {@code PLAYER_VIN} account and the {@code BANK_INBOX} system account must exist.
 *
 * <p><b>Password injection:</b> supply via {@code -Dtest.mysql.password=<pw>} or the
 * {@code MYSQL_PASSWORD} environment variable. When neither is set, tests are
 * <em>skipped</em> (not failed) via {@link Assume}.
 *
 * <pre>
 * To run:
 *   ./gradlew :VinPlayDAL:test --tests MoneyGatewayDualWriteTest \
 *      -Dtest.mysql.password=$(grep MYSQL_PASSWORD /root/sunwinkr/sunwinkr-backend/.env | cut -d= -f2)
 * </pre>
 *
 * <p><b>Note on feature flag:</b> {@code MoneyGateway.DUAL_WRITE_ENABLED} is a
 * {@code static volatile} field (not final) initialized at class-load time from
 * the env var {@code MONEY_LEDGER_DUAL_WRITE}. The gate logic in {@code creditUser}
 * re-reads the field on every call, so {@link #testFlagOff_OnlyLegacyRowWritten}
 * can flip the field via reflection and have the change take effect immediately.
 *
 * <p>The dual-write specific tests ({@link #testDualWrite_LegacyAndLedgerRowBothWritten}
 * and {@link #testDualWrite_Idempotent}) exercise the helper method
 * {@code dualWriteToLedger} directly (via the mapping helpers + {@code MoneyLedger.credit})
 * to ensure both rows are written, independent of the env var.
 * The {@link #testFlagOff_OnlyLegacyRowWritten} test toggles the flag OFF via
 * reflection and exercises {@code creditUser} end-to-end, asserting that the
 * legacy row is written and no ledger row appears.
 */
public class MoneyGatewayDualWriteTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** User that has a PLAYER_VIN money_account (seeded by task #176). */
    private static final long TEST_USER_ID = 5107L;
    private static final String TEST_NICKNAME = "testUser5107";

    /**
     * Second user used by the transfer tests as the destination side.
     * Like {@link #TEST_USER_ID} this id+nickname must exist in {@code users} and
     * have a {@code PLAYER_VIN} money_account; tests {@link Assume#assumeNotNull
     * skip} otherwise.
     */
    private static final long TEST_DEST_USER_ID = 5108L;
    private static final String TEST_DEST_NICKNAME = "testUser5108";

    /** Prefix for all external_refs created by this test suite. */
    private static final String REF_PREFIX = "test_mgw_dw_";

    // -------------------------------------------------------------------------
    // Suite-level state
    // -------------------------------------------------------------------------

    /** True when the DB password is missing and bootstrap was skipped. */
    private static volatile boolean bootstrapSkipped = false;

    // -------------------------------------------------------------------------
    // Suite-level setup / teardown
    // -------------------------------------------------------------------------

    @BeforeClass
    public static void bootstrapPool() throws Exception {
        String pw = System.getProperty("test.mysql.password");
        if (pw == null || pw.isEmpty()) {
            pw = System.getenv("MYSQL_PASSWORD");
        }
        if (pw == null || pw.isEmpty()) {
            bootstrapSkipped = true;
            System.out.println("MoneyGatewayDualWriteTest: password not supplied; tests will be skipped");
            return;
        }

        URL propsUrl = MoneyGatewayDualWriteTest.class
                .getClassLoader()
                .getResource("config/db_pool.properties");
        String resourcesDir = propsUrl.getPath().replace("config/db_pool.properties", "");
        VBeePath.basePath = resourcesDir;

        Properties props = new Properties();
        try (InputStream is = propsUrl.openStream()) {
            props.load(is);
        }
        final String PLACEHOLDER = "__INJECT_AT_TEST_TIME__";
        if (PLACEHOLDER.equals(props.getProperty("mysqlpoolname.password", ""))) {
            props.setProperty("mysqlpoolname.password", pw);
        }
        java.io.File propsFile = new java.io.File(propsUrl.toURI());
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propsFile)) {
            props.store(fos, "Auto-generated by MoneyGatewayDualWriteTest — password injected at test time");
        }

        ConnectionPool.getInstance();
    }

    @AfterClass
    public static void cleanUp() {
        if (bootstrapSkipped) return;

        // Restore the placeholder in the properties file
        try {
            URL propsUrl = MoneyGatewayDualWriteTest.class
                    .getClassLoader()
                    .getResource("config/db_pool.properties");
            if (propsUrl != null) {
                Properties props = new Properties();
                try (InputStream is = propsUrl.openStream()) {
                    props.load(is);
                }
                props.setProperty("mysqlpoolname.password", "__INJECT_AT_TEST_TIME__");
                java.io.File propsFile = new java.io.File(propsUrl.toURI());
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propsFile)) {
                    props.store(fos,
                            "Integration-test DB pool — password placeholder restored after run");
                }
            }
        } catch (Exception e) {
            System.err.println("MoneyGatewayDualWriteTest: could not restore placeholder (non-fatal): "
                    + e.getMessage());
        }

        // Delete all test rows generated by this suite
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // Ledger entries
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE e FROM vinplay.money_entry e"
                    + " JOIN vinplay.money_transaction t ON t.transaction_id = e.transaction_id"
                    + " WHERE t.external_ref LIKE '" + REF_PREFIX + "%'")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyGatewayDualWriteTest cleanup: deleted " + rows + " money_entry rows");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_transaction WHERE external_ref LIKE '" + REF_PREFIX + "%'")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyGatewayDualWriteTest cleanup: deleted " + rows + " money_transaction rows");
            }
            // Idempotency keys — must be cleared too, otherwise a rerun of the
            // suite hits "DUPLICATE" on every external_ref reused from the
            // previous run, which silently fails the dual-write assertions.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_idempotency WHERE external_ref LIKE '" + REF_PREFIX + "%'")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyGatewayDualWriteTest cleanup: deleted " + rows + " money_idempotency rows");
            }
            // Legacy audit rows
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_gateway_log WHERE tx_id LIKE '" + REF_PREFIX + "%'")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyGatewayDualWriteTest cleanup: deleted " + rows + " money_gateway_log rows");
            }
        } catch (Exception e) {
            System.err.println("MoneyGatewayDualWriteTest cleanup error (non-fatal): " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Per-test setup
    // -------------------------------------------------------------------------

    @Before
    public void skipIfNoPassword() {
        Assume.assumeFalse(
                "test.mysql.password or MYSQL_PASSWORD must be set. "
                + "Run: ./gradlew :VinPlayDAL:test --tests MoneyGatewayDualWriteTest "
                + "-Dtest.mysql.password=$(grep MYSQL_PASSWORD /path/to/.env | cut -d= -f2)",
                bootstrapSkipped);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Count rows in money_gateway_log with the given tx_id.
     */
    private int countLegacyRows(String txId) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_gateway_log WHERE tx_id = ?")) {
            ps.setString(1, txId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Count rows in money_transaction with the given external_ref.
     */
    private int countLedgerRows(String externalRef) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_transaction WHERE external_ref = ?")) {
            ps.setString(1, externalRef);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Clean up any leftover rows for a given txId before a test so the test is
     * deterministic even if a previous run left stale data.
     */
    private void cleanTxId(String txId) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // Match both the bare txId and the per-currency suffixed forms
            // (txId:vin, txId:xu, etc.) used by the multi-currency dual-write.
            String likePattern = txId + "%";
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE e FROM vinplay.money_entry e"
                    + " JOIN vinplay.money_transaction t ON t.transaction_id = e.transaction_id"
                    + " WHERE t.external_ref LIKE ?")) {
                ps.setString(1, likePattern);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_transaction WHERE external_ref LIKE ?")) {
                ps.setString(1, likePattern);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_idempotency WHERE external_ref LIKE ?")) {
                ps.setString(1, likePattern);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_gateway_log WHERE tx_id LIKE ?")) {
                ps.setString(1, likePattern);
                ps.executeUpdate();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Test 1: mapping helpers — unit-level, no DB needed
    // -------------------------------------------------------------------------

    /**
     * Verify mapSourceToLedgerType returns expected values for all known sources,
     * and null for unknown sources.
     */
    @Test
    public void testMapSourceToLedgerType_coversAllKnownSources() {
        assertEquals("DEPOSIT_BANK",    MoneyGateway.mapSourceToLedgerType("DEPOSIT_BANK"));
        assertEquals("DEPOSIT_BANK",    MoneyGateway.mapSourceToLedgerType("DEPOSIT_TELEGRAM"));
        assertEquals("DEPOSIT_CRYPTO",  MoneyGateway.mapSourceToLedgerType("DEPOSIT_CRYPTO"));
        assertEquals("DEPOSIT_BANK",    MoneyGateway.mapSourceToLedgerType("CARD_RECHARGE"));
        assertEquals("DEPOSIT_BANK",    MoneyGateway.mapSourceToLedgerType("CREDIT_WALLET_DEPOSIT"));
        assertEquals("ADMIN_TOPUP",     MoneyGateway.mapSourceToLedgerType("ADMIN_TOPUP"));
        assertEquals("AGENT_TOPUP",     MoneyGateway.mapSourceToLedgerType("AGENT_TOPUP"));
        assertEquals("DEPOSIT_PROMO",   MoneyGateway.mapSourceToLedgerType("PROMO_BONUS"));
        assertEquals("SIGNUP_BONUS",    MoneyGateway.mapSourceToLedgerType("SIGNUP_BONUS"));
        assertEquals("CASHBACK",        MoneyGateway.mapSourceToLedgerType("CASHBACK"));
        assertEquals("JACKPOT_WIN",     MoneyGateway.mapSourceToLedgerType("JACKPOT_WIN"));
        assertEquals("DEPOSIT_BANK",    MoneyGateway.mapSourceToLedgerType("GSC_RECONCILE"));
        assertEquals("DEPOSIT_BANK",    MoneyGateway.mapSourceToLedgerType("AWC_CREDIT"));
        assertEquals("REFUND_WITHDRAW", MoneyGateway.mapSourceToLedgerType("REFUND_WITHDRAW"));
        assertNull("null source → null", MoneyGateway.mapSourceToLedgerType(null));
        assertNull("unknown source → null", MoneyGateway.mapSourceToLedgerType("UNKNOWN_SOURCE_XYZ"));
    }

    /**
     * Verify mapSourceToSystemAccount is consistent with mapSourceToLedgerType:
     * every source that has a ledger type also has a system account, and vice versa.
     */
    @Test
    public void testMapSourceToSystemAccount_consistentWithLedgerType() {
        String[] sources = {
            "DEPOSIT_BANK", "DEPOSIT_TELEGRAM", "DEPOSIT_CRYPTO",
            "CARD_RECHARGE", "CREDIT_WALLET_DEPOSIT",
            "ADMIN_TOPUP", "AGENT_TOPUP", "PROMO_BONUS",
            "SIGNUP_BONUS", "CASHBACK", "JACKPOT_WIN",
            "GSC_RECONCILE", "AWC_CREDIT", "REFUND_WITHDRAW"
        };
        for (String source : sources) {
            String ledgerType = MoneyGateway.mapSourceToLedgerType(source);
            String systemAcct = MoneyGateway.mapSourceToSystemAccount(source);
            assertNotNull("source=" + source + " must have a ledger type", ledgerType);
            assertNotNull("source=" + source + " must have a system account", systemAcct);
        }

        // Unknown and null should return null in both
        assertNull(MoneyGateway.mapSourceToSystemAccount(null));
        assertNull(MoneyGateway.mapSourceToSystemAccount("TOTALLY_UNKNOWN"));
    }

    // -------------------------------------------------------------------------
    // Test 2: dual-write ON — both legacy and ledger rows written
    // -------------------------------------------------------------------------

    /**
     * With the dual-write flag enabled (exercised directly via helper methods),
     * calling creditUser with DEPOSIT_BANK should produce:
     * - 1 row in money_gateway_log (legacy)
     * - 1 row in money_transaction (ledger)
     *
     * This test calls the mapping + MoneyLedger helpers directly to be independent
     * of the MONEY_LEDGER_DUAL_WRITE env var at JVM startup time.
     */
    @Test
    public void testDualWrite_LegacyAndLedgerRowBothWritten() throws Exception {
        // Verify the ledger accounts needed for this test exist
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull(
                "PLAYER_VIN account for userId=" + TEST_USER_ID + " must exist (run task #176 migration)",
                playerAccId);
        Long bankInboxId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_INBOX");
        Assume.assumeNotNull("BANK_INBOX system account must exist (run task #175 migration)", bankInboxId);

        String txId = REF_PREFIX + "both_001";
        cleanTxId(txId);

        // 1. Write the legacy row directly (simulating the legacy credit path)
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO vinplay.money_gateway_log"
                     + " (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at)"
                     + " VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
            ps.setLong(1, TEST_USER_ID);
            ps.setString(2, TEST_NICKNAME);
            ps.setLong(3, 10000L);
            ps.setString(4, "DEPOSIT_BANK");
            ps.setString(5, txId);
            ps.setString(6, "Test dual-write both rows");
            ps.setLong(7, 999999L);
            ps.executeUpdate();
        }

        // 2. Write the ledger row (simulating the dual-write path)
        String txType = MoneyGateway.mapSourceToLedgerType("DEPOSIT_BANK");
        assertNotNull("DEPOSIT_BANK must map to a ledger type", txType);

        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult ledgerResult =
                com.vinplay.vbee.common.ledger.MoneyLedger.credit(
                        playerAccId, bankInboxId, 10000L,
                        txType, txId,
                        "Test dual-write both rows",
                        null);

        // 3. Assert both rows exist
        int legacyRows = countLegacyRows(txId);
        int ledgerRows = countLedgerRows(txId);

        assertEquals("Legacy money_gateway_log must have exactly 1 row for txId=" + txId, 1, legacyRows);

        assertTrue("Ledger money_transaction must be POSTED or DUPLICATE (already in ledger) for ref=" + txId,
                ledgerResult.status == com.vinplay.vbee.common.ledger.MoneyLedger.Status.POSTED
                || ledgerResult.status == com.vinplay.vbee.common.ledger.MoneyLedger.Status.DUPLICATE);
        assertEquals("money_transaction must have exactly 1 row for external_ref=" + txId, 1, ledgerRows);
    }

    // -------------------------------------------------------------------------
    // Test 3: idempotency — second dual-write with same txId is a no-op
    // -------------------------------------------------------------------------

    /**
     * Call the ledger credit twice with the same external_ref.
     * The second call must return DUPLICATE; only 1 money_transaction row must exist.
     */
    @Test
    public void testDualWrite_Idempotent() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);
        Long bankInboxId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_INBOX");
        Assume.assumeNotNull("BANK_INBOX must exist", bankInboxId);

        String txId = REF_PREFIX + "idem_001";
        cleanTxId(txId);

        String txType = MoneyGateway.mapSourceToLedgerType("DEPOSIT_BANK");

        // First call
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult first =
                com.vinplay.vbee.common.ledger.MoneyLedger.credit(
                        playerAccId, bankInboxId, 5000L, txType, txId,
                        "Idempotency test first call", null);
        assertEquals("First call must be POSTED",
                com.vinplay.vbee.common.ledger.MoneyLedger.Status.POSTED, first.status);
        assertTrue("First call transactionId must be > 0", first.transactionId > 0);

        // Second call — same txId
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult second =
                com.vinplay.vbee.common.ledger.MoneyLedger.credit(
                        playerAccId, bankInboxId, 5000L, txType, txId,
                        "Idempotency test second call", null);
        assertEquals("Second call must be DUPLICATE",
                com.vinplay.vbee.common.ledger.MoneyLedger.Status.DUPLICATE, second.status);
        assertEquals("DUPLICATE must return the same transactionId as the first post",
                first.transactionId, second.transactionId);

        // Only one ledger row
        int ledgerRows = countLedgerRows(txId);
        assertEquals("Only 1 money_transaction row for the given external_ref", 1, ledgerRows);
    }

    // -------------------------------------------------------------------------
    // Test 4: flag off — verify only legacy row written (no ledger row leaks in)
    // -------------------------------------------------------------------------

    /**
     * When DUAL_WRITE_ENABLED is false, the dual-write block in creditUser must
     * never be reached and no ledger row must ever appear — regardless of how
     * mappable the source is.
     *
     * The flag is now {@code static volatile} (not final) and the gate logic in
     * {@code creditUser} reads the field on every call, so we can flip it via
     * reflection inside the test and the change takes effect immediately. We
     * exercise the real {@code creditUser} code path (not a mocked stand-in) so
     * the OFF path is genuinely tested end-to-end:
     *
     * <ol>
     *   <li>Force the flag OFF via reflection.
     *   <li>Call {@code creditUser} with a known-mappable source (DEPOSIT_BANK).
     *       If the gate were broken (read env, not field), this would write a
     *       ledger row and the assertion would catch it.
     *   <li>Assert: 1 legacy row, 0 ledger rows.
     *   <li>Restore the flag and reverse the credit so the test is side-effect
     *       neutral on users.vin.
     * </ol>
     */
    @Test
    public void testFlagOff_OnlyLegacyRowWritten() throws Exception {
        // Sanity: the test user must exist in the ledger so any leak would actually
        // be writeable (otherwise an ACCOUNT_NOT_FOUND would falsely make the
        // assertion pass even with a broken gate).
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);
        Long bankInboxId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_INBOX");
        Assume.assumeNotNull("BANK_INBOX must exist", bankInboxId);

        String txId = REF_PREFIX + "flagoff_001";
        cleanTxId(txId);

        // Snapshot the flag so we can restore it. Reflection works because the
        // field is `static volatile` (no `final`), per the Issue-4 restructure.
        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        long amount = 1L; // smallest possible to minimise the integration footprint
        try {
            flagField.setBoolean(null, false);
            assertFalse("Flag must read false after reflection write",
                    flagField.getBoolean(null));

            // Real call: this exercises the gate inside creditUser.
            // With the flag OFF, no money_transaction row may appear.
            MoneyGateway.CreditResult result = MoneyGateway.creditUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_DEPOSIT_BANK, txId,
                    "Flag-off test");
            Assume.assumeTrue("creditUser must succeed for the assertion to be meaningful: "
                    + (result != null ? result.error : "null result"),
                    result != null && result.success);

            int legacyRows = countLegacyRows(txId);
            int ledgerRows = countLedgerRows(txId);
            assertEquals("Legacy row must exist (legacy path always runs)", 1, legacyRows);
            assertEquals("No ledger row must exist when DUAL_WRITE_ENABLED is false", 0, ledgerRows);
        } finally {
            // Restore flag first — even if the assertion fired, the rest of the suite
            // (and any other tests sharing this JVM) must see the original state.
            flagField.setBoolean(null, original);

            // Reverse the credit so users.vin is unchanged. Use a different txId
            // to avoid the dedup short-circuit on (txId, source).
            try {
                MoneyGateway.debitUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_DEPOSIT_BANK, txId + "_undo",
                        "Flag-off test undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    // -------------------------------------------------------------------------
    // Test 5: debit dual-write — flag ON, ledger debit row written
    // -------------------------------------------------------------------------

    /**
     * With DUAL_WRITE_ENABLED true, calling debitUser with WITHDRAW_BANK must
     * produce both:
     * - a row in money_gateway_log (legacy, with NEGATIVE amount)
     * - a row in money_transaction (ledger debit: DEBIT player → CREDIT BANK_OUTBOX)
     *
     * Toggles the flag via reflection to be independent of the JVM-launch env var.
     */
    @Test
    public void testFlagOn_DebitLedgerRowWritten() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);
        Long bankOutboxId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_OUTBOX");
        Assume.assumeNotNull("BANK_OUTBOX system account must exist (run task #175 migration)", bankOutboxId);

        String txId = REF_PREFIX + "debit_on_001";
        cleanTxId(txId);

        // Pre-credit the user so the debit floor-check passes — but do NOT use the
        // same source/txId as the debit, otherwise dedup will short-circuit.
        long amount = 100L;
        MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                TEST_USER_ID, TEST_NICKNAME, amount,
                MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_setup",
                "Debit test pre-credit");
        Assume.assumeTrue("setup credit must succeed", cr != null && cr.success);

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, true);

            MoneyGateway.CreditResult result = MoneyGateway.debitUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_WITHDRAW_BANK, txId,
                    "Debit dual-write flag-on test");
            assertTrue("debitUser must succeed: " + (result != null ? result.error : "null"),
                    result != null && result.success);

            int legacyRows = countLegacyRows(txId);
            int ledgerRows = countLedgerRows(txId);
            assertEquals("Legacy money_gateway_log must have exactly 1 row", 1, legacyRows);
            assertEquals("Ledger money_transaction must have exactly 1 row when flag is on", 1, ledgerRows);
        } finally {
            flagField.setBoolean(null, original);
            // Reverse the debit so users.vin is unchanged.
            try {
                MoneyGateway.creditUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_undo",
                        "Debit dual-write flag-on undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    // -------------------------------------------------------------------------
    // Test 6: debit dual-write — flag OFF, only legacy row written
    // -------------------------------------------------------------------------

    /**
     * When DUAL_WRITE_ENABLED is false the debit dual-write block must never
     * be reached and no ledger row may appear — even with a known-mappable
     * debit source like WITHDRAW_BANK.
     */
    @Test
    public void testFlagOff_DebitOnlyLegacyRowWritten() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);
        Long bankOutboxId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_OUTBOX");
        Assume.assumeNotNull("BANK_OUTBOX must exist", bankOutboxId);

        String txId = REF_PREFIX + "debit_off_001";
        cleanTxId(txId);

        long amount = 1L;
        MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                TEST_USER_ID, TEST_NICKNAME, amount,
                MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_setup",
                "Debit flag-off test pre-credit");
        Assume.assumeTrue("setup credit must succeed", cr != null && cr.success);

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, false);
            assertFalse("Flag must read false", flagField.getBoolean(null));

            MoneyGateway.CreditResult result = MoneyGateway.debitUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_WITHDRAW_BANK, txId,
                    "Debit flag-off test");
            Assume.assumeTrue("debitUser must succeed for the assertion to be meaningful: "
                    + (result != null ? result.error : "null result"),
                    result != null && result.success);

            int legacyRows = countLegacyRows(txId);
            int ledgerRows = countLedgerRows(txId);
            assertEquals("Legacy row must exist (legacy debit always runs)", 1, legacyRows);
            assertEquals("No ledger row must exist when DUAL_WRITE_ENABLED is false", 0, ledgerRows);
        } finally {
            flagField.setBoolean(null, original);
            try {
                MoneyGateway.creditUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_undo",
                        "Debit flag-off undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    // -------------------------------------------------------------------------
    // Test 7: ledger-level debit idempotency (direct helper call)
    // -------------------------------------------------------------------------

    /**
     * Call MoneyLedger.debit twice with the same external_ref. The second call
     * must return DUPLICATE and only one money_transaction row may exist.
     *
     * Like {@link #testDualWrite_Idempotent}, this exercises the helper directly
     * to isolate the idempotency contract from the legacy-side dedup
     * (which would short-circuit before the ledger write was even attempted).
     *
     * Renamed from testFlagOn_DebitDedup so the name no longer implies it
     * exercises MoneyGateway.debitUser — it does not.
     */
    @Test
    public void testLedgerDebitIdempotent_directHelper() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);
        Long bankOutboxId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_OUTBOX");
        Assume.assumeNotNull("BANK_OUTBOX must exist", bankOutboxId);

        String txId = REF_PREFIX + "debit_dedup_001";
        cleanTxId(txId);

        // Ensure the ledger PLAYER_VIN balance can cover the debit. We pre-fund
        // via the helper credit() (separate ref) so the test does not depend on
        // any legacy-side state for the dedup assertion.
        long amount = 10L;
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult fund =
                com.vinplay.vbee.common.ledger.MoneyLedger.credit(
                        playerAccId,
                        com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_INBOX"),
                        amount, "DEPOSIT_BANK", txId + "_fund",
                        "Debit dedup pre-fund", null);
        Assume.assumeTrue("ledger pre-fund must POST: " + fund.status,
                fund.status == com.vinplay.vbee.common.ledger.MoneyLedger.Status.POSTED);

        String txType = MoneyGateway.mapDebitSourceToLedgerType("WITHDRAW_BANK");
        assertNotNull("WITHDRAW_BANK must map to a debit ledger type", txType);

        // First debit — must POST
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult first =
                com.vinplay.vbee.common.ledger.MoneyLedger.debit(
                        playerAccId, bankOutboxId, amount, txType, txId,
                        "Debit dedup first call", null);
        assertEquals("First debit must be POSTED",
                com.vinplay.vbee.common.ledger.MoneyLedger.Status.POSTED, first.status);
        assertTrue("First debit transactionId must be > 0", first.transactionId > 0);

        // Second debit — same external_ref must DUPLICATE, no double-debit
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult second =
                com.vinplay.vbee.common.ledger.MoneyLedger.debit(
                        playerAccId, bankOutboxId, amount, txType, txId,
                        "Debit dedup second call", null);
        assertEquals("Second debit must be DUPLICATE",
                com.vinplay.vbee.common.ledger.MoneyLedger.Status.DUPLICATE, second.status);
        assertEquals("DUPLICATE returns the same transactionId as the original",
                first.transactionId, second.transactionId);

        int ledgerRows = countLedgerRows(txId);
        assertEquals("Only 1 money_transaction row for the given external_ref", 1, ledgerRows);
    }

    // -------------------------------------------------------------------------
    // Test 8: debit mapping helpers — unit-level
    // -------------------------------------------------------------------------

    /**
     * Verify mapDebitSourceToLedgerType + mapDebitSourceToSystemAccount cover all
     * debit sources that {@code debitUser} is currently called with, and return
     * null for unknown / null inputs.
     */
    @Test
    public void testMapDebitSource_coversAllKnownSources() {
        // All currently-routed debit sources
        assertEquals("WITHDRAW_BANK",   MoneyGateway.mapDebitSourceToLedgerType("WITHDRAW_BANK"));
        assertEquals("WITHDRAW_CRYPTO", MoneyGateway.mapDebitSourceToLedgerType("WITHDRAW_CRYPTO"));
        assertEquals("ADMIN_DEDUCT",    MoneyGateway.mapDebitSourceToLedgerType("ADMIN_DEDUCT"));
        assertEquals("WAGER_DEBIT",     MoneyGateway.mapDebitSourceToLedgerType("AWC_DEBIT"));
        assertEquals("WAGER_DEBIT",     MoneyGateway.mapDebitSourceToLedgerType("USERSERVICE_GAME"));
        assertNull(MoneyGateway.mapDebitSourceToLedgerType(null));
        assertNull(MoneyGateway.mapDebitSourceToLedgerType("UNKNOWN_DEBIT_SRC"));

        assertEquals("BANK_OUTBOX",     MoneyGateway.mapDebitSourceToSystemAccount("WITHDRAW_BANK"));
        assertEquals("CRYPTO_OUTBOX",   MoneyGateway.mapDebitSourceToSystemAccount("WITHDRAW_CRYPTO"));
        assertEquals("PROMO_POOL",      MoneyGateway.mapDebitSourceToSystemAccount("ADMIN_DEDUCT"));
        assertEquals("HOUSE_GAME_POT",  MoneyGateway.mapDebitSourceToSystemAccount("AWC_DEBIT"));
        assertEquals("HOUSE_GAME_POT",  MoneyGateway.mapDebitSourceToSystemAccount("USERSERVICE_GAME"));
        assertNull(MoneyGateway.mapDebitSourceToSystemAccount(null));
        assertNull(MoneyGateway.mapDebitSourceToSystemAccount("UNKNOWN_DEBIT_SRC"));

        // Credit path: USERSERVICE_GAME + AWC_CREDIT must hit WAGER_CREDIT / HOUSE_GAME_POT (SUN-13xx)
        assertEquals("WAGER_CREDIT",    MoneyGateway.mapSourceToLedgerType("USERSERVICE_GAME"));
        assertEquals("WAGER_CREDIT",    MoneyGateway.mapSourceToLedgerType("AWC_CREDIT"));
        assertEquals("HOUSE_GAME_POT",  MoneyGateway.mapSourceToSystemAccount("USERSERVICE_GAME"));
        assertEquals("HOUSE_GAME_POT",  MoneyGateway.mapSourceToSystemAccount("AWC_CREDIT"));

        // Symmetry: every mapped ledger type must have a corresponding system account.
        String[] debitSources = { "WITHDRAW_BANK", "WITHDRAW_CRYPTO", "ADMIN_DEDUCT", "AWC_DEBIT", "USERSERVICE_GAME" };
        for (String s : debitSources) {
            assertNotNull("debit source " + s + " must map to a ledger type",
                    MoneyGateway.mapDebitSourceToLedgerType(s));
            assertNotNull("debit source " + s + " must map to a system account",
                    MoneyGateway.mapDebitSourceToSystemAccount(s));
        }
    }

    /**
     * SUN-1308 — admin VIN deduct must behave like a wallet debit, not like
     * game P&L. It may decrease users.vin, but must never mutate vin_total.
     */
    @Test
    public void testAdminDeduct_debitsVinWithoutTouchingVinTotal() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);
        Long promoPoolId = com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("PROMO_POOL");
        Assume.assumeNotNull("PROMO_POOL system account must exist", promoPoolId);

        String txId = REF_PREFIX + "admin_deduct_001";
        cleanTxId(txId);

        long originalVin = readVin(TEST_USER_ID);
        long originalVinTotal = readVinTotal(TEST_USER_ID);
        long startingVin = 1000L;
        long startingVinTotal = 777L;
        long amount = 100L;

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean originalFlag = flagField.getBoolean(null);

        try {
            setVinState(TEST_USER_ID, startingVin, startingVinTotal);
            flagField.setBoolean(null, true);

            MoneyGateway.CreditResult result = MoneyGateway.debitUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_ADMIN_DEDUCT, txId,
                    "SUN-1308 admin deduct contract test");
            assertTrue("admin deduct must succeed: " + (result != null ? result.error : "null"),
                    result != null && result.success);

            assertEquals("users.vin must decrease by the deduct amount",
                    startingVin - amount, readVin(TEST_USER_ID));
            assertEquals("users.vin_total must stay unchanged",
                    startingVinTotal, readVinTotal(TEST_USER_ID));
            assertEquals("ADMIN_DEDUCT audit row must be written", 1, countLegacyRows(txId));
            assertEquals("ADMIN_DEDUCT ledger row must be written when dual-write is on",
                    1, countLedgerRows(txId));
        } finally {
            flagField.setBoolean(null, originalFlag);
            setVinState(TEST_USER_ID, originalVin, originalVinTotal);
            cleanTxId(txId);
        }
    }

    // -------------------------------------------------------------------------
    // Test 9: transfer dual-write — flag ON, ledger transfer transaction written
    // -------------------------------------------------------------------------

    /**
     * Count entries in money_entry for the given external_ref.
     * Used by transfer tests to assert "1 transaction with 2 entries" instead of
     * the credit/debit pattern of "1 transaction".
     */
    private int countLedgerEntries(String externalRef) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_entry e"
                     + " JOIN vinplay.money_transaction t ON t.transaction_id = e.transaction_id"
                     + " WHERE t.external_ref = ?")) {
            ps.setString(1, externalRef);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * With DUAL_WRITE_ENABLED true, calling transferBetweenUsers must produce:
     * - 2 rows in money_gateway_log (legacy: -amount on src, +amount on dest, same tx_id)
     * - 1 row in money_transaction with 2 entries (DEBIT src PLAYER_VIN, CREDIT dest PLAYER_VIN)
     */
    @Test
    public void testFlagOn_TransferLedgerRowsWritten() throws Exception {
        Long srcAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for src must exist", srcAccId);
        Long destAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_DEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for dest must exist (need a second test user)", destAccId);

        String txId = REF_PREFIX + "transfer_on_001";
        cleanTxId(txId);

        // Pre-fund src so the transfer's floor-check passes — use a different txId
        // so the inter-user-transfer dedup (tx_id, source) doesn't short-circuit.
        long amount = 100L;
        MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                TEST_USER_ID, TEST_NICKNAME, amount,
                MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_setup",
                "Transfer test pre-credit");
        Assume.assumeTrue("setup credit must succeed", cr != null && cr.success);

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, true);

            MoneyGateway.TransferResult result = MoneyGateway.transferBetweenUsers(
                    TEST_USER_ID, TEST_NICKNAME, TEST_DEST_USER_ID, TEST_DEST_NICKNAME,
                    amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId,
                    "Transfer dual-write flag-on test");
            assertTrue("transferBetweenUsers must succeed: " + (result != null ? result.error : "null"),
                    result != null && result.success);

            int legacyRows = countLegacyRows(txId);
            int ledgerTxs = countLedgerRows(txId);
            int ledgerEntries = countLedgerEntries(txId);
            assertEquals("Legacy money_gateway_log must have 2 rows (src DEBIT, dest CREDIT)", 2, legacyRows);
            assertEquals("Ledger money_transaction must be exactly 1 row", 1, ledgerTxs);
            assertEquals("Ledger money_entry must be exactly 2 rows (src DEBIT + dest CREDIT)", 2, ledgerEntries);
        } finally {
            flagField.setBoolean(null, original);
            // Reverse: pull the amount back to src and burn off the setup credit so
            // users.vin is unchanged on both ends.
            try {
                MoneyGateway.transferBetweenUsers(
                        TEST_DEST_USER_ID, TEST_DEST_NICKNAME, TEST_USER_ID, TEST_NICKNAME,
                        amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId + "_undo",
                        "Transfer dual-write flag-on undo");
                MoneyGateway.debitUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_WITHDRAW_BANK, txId + "_undo_setup",
                        "Transfer dual-write flag-on undo setup");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    // -------------------------------------------------------------------------
    // Test 10: transfer dual-write — flag OFF, only legacy rows written
    // -------------------------------------------------------------------------

    /**
     * When DUAL_WRITE_ENABLED is false the transfer dual-write block must never
     * be reached and no ledger row may appear — even with a known-mappable
     * source like INTER_USER_TRANSFER.
     */
    @Test
    public void testFlagOff_TransferOnlyLegacyRowsWritten() throws Exception {
        Long srcAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for src must exist", srcAccId);
        Long destAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_DEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for dest must exist", destAccId);

        String txId = REF_PREFIX + "transfer_off_001";
        cleanTxId(txId);

        long amount = 1L;
        MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                TEST_USER_ID, TEST_NICKNAME, amount,
                MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_setup",
                "Transfer flag-off test pre-credit");
        Assume.assumeTrue("setup credit must succeed", cr != null && cr.success);

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, false);
            assertFalse("Flag must read false", flagField.getBoolean(null));

            MoneyGateway.TransferResult result = MoneyGateway.transferBetweenUsers(
                    TEST_USER_ID, TEST_NICKNAME, TEST_DEST_USER_ID, TEST_DEST_NICKNAME,
                    amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId,
                    "Transfer flag-off test");
            Assume.assumeTrue("transferBetweenUsers must succeed for the assertion to be meaningful: "
                    + (result != null ? result.error : "null result"),
                    result != null && result.success);

            int legacyRows = countLegacyRows(txId);
            int ledgerTxs = countLedgerRows(txId);
            assertEquals("Legacy money_gateway_log must have 2 rows (legacy transfer always runs)",
                    2, legacyRows);
            assertEquals("No ledger row must exist when DUAL_WRITE_ENABLED is false", 0, ledgerTxs);
        } finally {
            flagField.setBoolean(null, original);
            try {
                MoneyGateway.transferBetweenUsers(
                        TEST_DEST_USER_ID, TEST_DEST_NICKNAME, TEST_USER_ID, TEST_NICKNAME,
                        amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId + "_undo",
                        "Transfer flag-off undo");
                MoneyGateway.debitUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_WITHDRAW_BANK, txId + "_undo_setup",
                        "Transfer flag-off undo setup");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    // -------------------------------------------------------------------------
    // Test 11: transfer ledger-level idempotency (direct helper call)
    // -------------------------------------------------------------------------

    /**
     * Call {@code MoneyLedger.transfer} twice with the same {@code (transaction_type,
     * external_ref)}. The second call must return DUPLICATE and only one
     * money_transaction row may exist.
     *
     * <p>Like the credit/debit idempotency tests, this exercises the helper directly
     * to isolate the ledger contract from the legacy-side dedup
     * (which would short-circuit before the ledger write was attempted).
     */
    @Test
    public void testFlagOn_TransferDedup() throws Exception {
        Long srcAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for src must exist", srcAccId);
        Long destAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_DEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for dest must exist", destAccId);

        String txId = REF_PREFIX + "transfer_dedup_001";
        cleanTxId(txId);

        // Pre-fund src so the first transfer balance-check passes. Pre-fund directly
        // via the ledger helper (separate ref) so the test is independent of legacy state.
        long amount = 10L;
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult fund =
                com.vinplay.vbee.common.ledger.MoneyLedger.credit(
                        srcAccId,
                        com.vinplay.vbee.common.ledger.MoneyLedger.findSystemAccount("BANK_INBOX"),
                        amount, "DEPOSIT_BANK", txId + "_fund",
                        "Transfer dedup pre-fund", null);
        Assume.assumeTrue("ledger pre-fund must POST: " + fund.status,
                fund.status == com.vinplay.vbee.common.ledger.MoneyLedger.Status.POSTED);

        String txType = MoneyGateway.mapTransferSourceToLedgerType("INTER_USER_TRANSFER");
        assertNotNull("INTER_USER_TRANSFER must map to a transfer ledger type", txType);

        // First transfer — must POST
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult first =
                com.vinplay.vbee.common.ledger.MoneyLedger.transfer(
                        srcAccId, destAccId, amount, txType, txId,
                        "Transfer dedup first call", null);
        assertEquals("First transfer must be POSTED",
                com.vinplay.vbee.common.ledger.MoneyLedger.Status.POSTED, first.status);
        assertTrue("First transfer transactionId must be > 0", first.transactionId > 0);

        // Second transfer — same external_ref must DUPLICATE, no double-post
        com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult second =
                com.vinplay.vbee.common.ledger.MoneyLedger.transfer(
                        srcAccId, destAccId, amount, txType, txId,
                        "Transfer dedup second call", null);
        assertEquals("Second transfer must be DUPLICATE",
                com.vinplay.vbee.common.ledger.MoneyLedger.Status.DUPLICATE, second.status);
        assertEquals("DUPLICATE returns the same transactionId as the original",
                first.transactionId, second.transactionId);

        int ledgerRows = countLedgerRows(txId);
        assertEquals("Only 1 money_transaction row for the given external_ref", 1, ledgerRows);
    }

    // -------------------------------------------------------------------------
    // Audit-#17 race-safe dedup tests — verify the (tx_id, source, user_id)
    // UNIQUE key + transactional INSERT IGNORE prevents double-credit on
    // retried provider webhooks.  These exercise the SECOND identical call,
    // which used to leak past the SELECT-based dedup and double-credit.
    // -------------------------------------------------------------------------

    /**
     * Read users.vin for the given userId.  Used to assert the second of two
     * identical creditUser/debitUser calls did NOT produce a wallet movement.
     */
    private long readVin(long userId) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT vin FROM vinplay.users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long readVinTotal(long userId) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT vin_total FROM vinplay.users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void setVinState(long userId, long vin, long vinTotal) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE vinplay.users SET vin = ?, vin_total = ? WHERE id = ?")) {
            ps.setLong(1, vin);
            ps.setLong(2, vinTotal);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Audit #17 — creditUser called twice with the same (tx_id, source) must:
     *  1) succeed once and return Duplicate the second time;
     *  2) credit users.vin by exactly amount, not 2*amount;
     *  3) leave exactly one row in money_gateway_log for that (tx_id, source).
     */
    @Test
    public void testCreditUser_doubleCallSameTxIdReturnsDuplicate() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);

        String txId = REF_PREFIX + "credit_dup_001";
        cleanTxId(txId);
        long amount = 1234L;

        long balBefore = readVin(TEST_USER_ID);

        // Use ADMIN_TOPUP rather than DEPOSIT_BANK so the deposit-promotion path
        // isn't triggered — a first-deposit / daily-deposit bonus could fire
        // alongside the credit and skew users.vin by an unpredictable bonus
        // amount.  ADMIN_TOPUP is in DEPOSIT_SOURCES (so recharge_money is still
        // tracked) but NOT in PROMO_SOURCES, exercising the same code path under
        // test without the noise.
        try {
            MoneyGateway.CreditResult first = MoneyGateway.creditUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_ADMIN_TOPUP, txId,
                    "Audit-17 dedup test first");
            Assume.assumeTrue("first creditUser must succeed for the assertion to be meaningful: "
                    + (first != null ? first.error : "null"),
                    first != null && first.success);

            MoneyGateway.CreditResult second = MoneyGateway.creditUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_ADMIN_TOPUP, txId,
                    "Audit-17 dedup test second");
            assertNotNull(second);
            assertFalse("Second creditUser must NOT succeed", second.success);
            assertEquals("Duplicate transaction", second.error);

            long balAfter = readVin(TEST_USER_ID);
            assertEquals("users.vin must be credited by exactly 1x amount, not 2x",
                    balBefore + amount, balAfter);

            int rows = countLegacyRows(txId);
            assertEquals("money_gateway_log must have exactly 1 row for the txId", 1, rows);
        } finally {
            // Reverse the credit so users.vin is unchanged for downstream tests.
            try {
                MoneyGateway.debitUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_WITHDRAW_BANK, txId + "_undo",
                        "Audit-17 credit dedup undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    /**
     * Audit #17 — debitUser called twice with the same (tx_id, source) must:
     *  1) succeed once, return Duplicate the second time;
     *  2) debit users.vin by exactly amount, not 2*amount;
     *  3) leave exactly one row in money_gateway_log for that (tx_id, source).
     */
    @Test
    public void testDebitUser_doubleCallSameTxIdReturnsDuplicate() throws Exception {
        Long playerAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerAccId);

        String txId = REF_PREFIX + "debit_dup_001";
        cleanTxId(txId);
        long amount = 1234L;

        // Pre-fund the user so the floor-check passes — different txId so dedup
        // doesn't short-circuit the debit.
        MoneyGateway.CreditResult prefund = MoneyGateway.creditUser(
                TEST_USER_ID, TEST_NICKNAME, amount,
                MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_setup",
                "Audit-17 debit dedup pre-fund");
        Assume.assumeTrue("pre-fund must succeed", prefund != null && prefund.success);

        long balBefore = readVin(TEST_USER_ID);

        try {
            MoneyGateway.CreditResult first = MoneyGateway.debitUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_WITHDRAW_BANK, txId,
                    "Audit-17 debit dedup first");
            Assume.assumeTrue("first debitUser must succeed: "
                    + (first != null ? first.error : "null"),
                    first != null && first.success);

            MoneyGateway.CreditResult second = MoneyGateway.debitUser(
                    TEST_USER_ID, TEST_NICKNAME, amount,
                    MoneyGateway.SOURCE_WITHDRAW_BANK, txId,
                    "Audit-17 debit dedup second");
            assertNotNull(second);
            assertFalse("Second debitUser must NOT succeed", second.success);
            assertEquals("Duplicate transaction", second.error);

            long balAfter = readVin(TEST_USER_ID);
            assertEquals("users.vin must be debited by exactly 1x amount, not 2x",
                    balBefore - amount, balAfter);

            int rows = countLegacyRows(txId);
            assertEquals("money_gateway_log must have exactly 1 row for the txId", 1, rows);
        } finally {
            // No cleanup needed: we burned the prefund credit with the debit, so
            // users.vin is back where it started.
        }
    }

    /**
     * Audit #17 — transferBetweenUsers called twice with the same (tx_id, source)
     * must:
     *  1) succeed once, return Duplicate the second time;
     *  2) move users.vin between src and dest by exactly amount, not 2*amount;
     *  3) leave exactly TWO rows in money_gateway_log for that (tx_id, source) —
     *     one for src (negative amount) and one for dest (positive amount).
     */
    @Test
    public void testTransferBetweenUsers_doubleCallSameTxIdReturnsDuplicate() throws Exception {
        Long srcAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for src must exist", srcAccId);
        Long destAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_DEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account for dest must exist", destAccId);

        String txId = REF_PREFIX + "transfer_dup_001";
        cleanTxId(txId);
        long amount = 1234L;

        // Pre-fund src so the transfer's floor-check passes.
        MoneyGateway.CreditResult prefund = MoneyGateway.creditUser(
                TEST_USER_ID, TEST_NICKNAME, amount,
                MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_setup",
                "Audit-17 transfer dedup pre-fund");
        Assume.assumeTrue("pre-fund must succeed", prefund != null && prefund.success);

        long srcBefore = readVin(TEST_USER_ID);
        long destBefore = readVin(TEST_DEST_USER_ID);

        try {
            MoneyGateway.TransferResult first = MoneyGateway.transferBetweenUsers(
                    TEST_USER_ID, TEST_NICKNAME, TEST_DEST_USER_ID, TEST_DEST_NICKNAME,
                    amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId,
                    "Audit-17 transfer dedup first");
            Assume.assumeTrue("first transfer must succeed: "
                    + (first != null ? first.error : "null"),
                    first != null && first.success);

            MoneyGateway.TransferResult second = MoneyGateway.transferBetweenUsers(
                    TEST_USER_ID, TEST_NICKNAME, TEST_DEST_USER_ID, TEST_DEST_NICKNAME,
                    amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId,
                    "Audit-17 transfer dedup second");
            assertNotNull(second);
            assertFalse("Second transfer must NOT succeed", second.success);
            assertEquals("Duplicate transaction", second.error);

            long srcAfter = readVin(TEST_USER_ID);
            long destAfter = readVin(TEST_DEST_USER_ID);
            assertEquals("src.vin must be debited by exactly 1x amount, not 2x",
                    srcBefore - amount, srcAfter);
            assertEquals("dest.vin must be credited by exactly 1x amount, not 2x",
                    destBefore + amount, destAfter);

            int rows = countLegacyRows(txId);
            assertEquals("money_gateway_log must have exactly 2 rows for the txId (src + dest)",
                    2, rows);
        } finally {
            // Reverse: dest -> src for amount, then burn off the prefund credit on src.
            try {
                MoneyGateway.transferBetweenUsers(
                        TEST_DEST_USER_ID, TEST_DEST_NICKNAME, TEST_USER_ID, TEST_NICKNAME,
                        amount, MoneyGateway.SOURCE_INTER_USER_TRANSFER, txId + "_undo",
                        "Audit-17 transfer dedup undo");
                MoneyGateway.debitUser(
                        TEST_USER_ID, TEST_NICKNAME, amount,
                        MoneyGateway.SOURCE_WITHDRAW_BANK, txId + "_undo_setup",
                        "Audit-17 transfer dedup undo setup");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    // -------------------------------------------------------------------------
    // Phase 2 — multi-currency credit/debit/setAbsolute tests
    //
    // These exercise the new MoneyGateway.{credit,debit}Currency +
    // setCurrencyAbsolute methods, asserting that:
    //   1. The legacy users.<col> column is updated correctly.
    //   2. money_gateway_log gets a row with the right currency value.
    //   3. The ledger gets a corresponding money_transaction (when the
    //      flag is on).
    //   4. Unknown currencies are rejected before any DB write.
    //   5. Per-currency dedup works: same (tx_id, source, user_id) for
    //      vin AND xu both succeed (different currency rows).
    // -------------------------------------------------------------------------

    /** Read users.<column> for a given userId. */
    private long readColumn(long userId, String column) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT " + column + " FROM vinplay.users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Count rows in money_gateway_log with the given tx_id AND currency.
     */
    private int countLegacyRowsForCurrency(String txId, String currency) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_gateway_log WHERE tx_id = ? AND currency = ?")) {
            ps.setString(1, txId);
            ps.setString(2, currency);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Phase 2 — credit xu through MoneyGateway.creditCurrency.  Assert legacy
     * users.xu UPDATE applied, money_gateway_log row written with currency='xu',
     * and (with the flag on) the ledger PLAYER_XU account gets a credit entry.
     */
    @Test
    public void testCreditCurrency_xu() throws Exception {
        Long playerXuAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_XU");
        Assume.assumeNotNull("PLAYER_XU account must exist (run backfill migration)", playerXuAccId);

        String txId = REF_PREFIX + "xu_credit_001";
        cleanTxId(txId);
        long amount = 555L;
        long balBefore = readColumn(TEST_USER_ID, "xu");

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, true);

            MoneyGateway.CreditResult r = MoneyGateway.creditCurrency(
                    TEST_USER_ID, TEST_NICKNAME, "xu", amount,
                    MoneyGateway.SOURCE_ADMIN_TOPUP, txId,
                    "xu credit test");
            assertNotNull(r);
            assertTrue("creditCurrency xu must succeed: " + (r != null ? r.error : ""), r.success);

            long balAfter = readColumn(TEST_USER_ID, "xu");
            assertEquals("users.xu must be credited by exactly amount", balBefore + amount, balAfter);
            assertEquals("money_gateway_log row for xu must exist",
                    1, countLegacyRowsForCurrency(txId, "xu"));

            // Per-currency external_ref synthesised by dual-write helper is
            // "<txId>:xu" — count by that suffix.
            int ledgerRows = countLedgerRows(txId + ":xu");
            assertEquals("Ledger money_transaction for xu must have exactly 1 row when flag on",
                    1, ledgerRows);
        } finally {
            flagField.setBoolean(null, original);
            // Reverse the credit so users.xu is unchanged.
            try {
                MoneyGateway.debitCurrency(
                        TEST_USER_ID, TEST_NICKNAME, "xu", amount,
                        MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_undo",
                        "xu credit undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    /**
     * Phase 2 — set safe to an absolute value via setCurrencyAbsolute.  This is
     * the path used by the migrated MoneyInGameDaoImpl.updateSafeMoney.  Assert
     * users.safe set, audit row delta = newValue - oldValue, ledger entry
     * present (sign matches delta).
     */
    @Test
    public void testCreditCurrency_safe() throws Exception {
        Long playerSafeAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_SAFE");
        Assume.assumeNotNull("PLAYER_SAFE account must exist", playerSafeAccId);

        String txId = REF_PREFIX + "safe_set_001";
        cleanTxId(txId);
        long oldSafe = readColumn(TEST_USER_ID, "safe");
        long newSafe = oldSafe + 777L;

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, true);

            MoneyGateway.CreditResult r = MoneyGateway.setCurrencyAbsolute(
                    TEST_USER_ID, TEST_NICKNAME, "safe", newSafe,
                    MoneyGateway.SOURCE_SAFE_FREEZE_DRAIN, txId,
                    "safe set test");
            assertNotNull(r);
            assertTrue("setCurrencyAbsolute safe must succeed: " + (r != null ? r.error : ""), r.success);

            assertEquals("users.safe must equal new absolute value",
                    newSafe, readColumn(TEST_USER_ID, "safe"));
            assertEquals("money_gateway_log row for safe must exist",
                    1, countLegacyRowsForCurrency(txId, "safe"));

            int ledgerRows = countLedgerRows(txId + ":safe");
            assertEquals("Ledger money_transaction for safe must have exactly 1 row",
                    1, ledgerRows);
        } finally {
            flagField.setBoolean(null, original);
            // Restore old safe value via setCurrencyAbsolute.
            try {
                MoneyGateway.setCurrencyAbsolute(
                        TEST_USER_ID, TEST_NICKNAME, "safe", oldSafe,
                        MoneyGateway.SOURCE_SAFE_FREEZE_DRAIN, txId + "_undo",
                        "safe set undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    /**
     * Phase 2 — credit money_vp through creditCurrency. Assert users.money_vp
     * UPDATE applied, audit row written with currency='money_vp', ledger
     * entry on PLAYER_VP account.
     */
    @Test
    public void testCreditCurrency_money_vp() throws Exception {
        Long playerVpAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VP");
        Assume.assumeNotNull("PLAYER_VP account must exist", playerVpAccId);

        String txId = REF_PREFIX + "money_vp_credit_001";
        cleanTxId(txId);
        long amount = 99L;
        long balBefore = readColumn(TEST_USER_ID, "money_vp");

        java.lang.reflect.Field flagField = MoneyGateway.class.getDeclaredField("DUAL_WRITE_ENABLED");
        flagField.setAccessible(true);
        boolean original = flagField.getBoolean(null);

        try {
            flagField.setBoolean(null, true);

            MoneyGateway.CreditResult r = MoneyGateway.creditCurrency(
                    TEST_USER_ID, TEST_NICKNAME, "money_vp", amount,
                    MoneyGateway.SOURCE_VIPPOINT_UPDATE, txId,
                    "money_vp credit test");
            assertNotNull(r);
            assertTrue("creditCurrency money_vp must succeed: " + (r != null ? r.error : ""), r.success);

            assertEquals("users.money_vp must be credited", balBefore + amount,
                    readColumn(TEST_USER_ID, "money_vp"));
            assertEquals("money_gateway_log row for money_vp must exist",
                    1, countLegacyRowsForCurrency(txId, "money_vp"));

            int ledgerRows = countLedgerRows(txId + ":money_vp");
            assertEquals("Ledger money_transaction for money_vp must have exactly 1 row",
                    1, ledgerRows);
        } finally {
            flagField.setBoolean(null, original);
            try {
                MoneyGateway.debitCurrency(
                        TEST_USER_ID, TEST_NICKNAME, "money_vp", amount,
                        MoneyGateway.SOURCE_VIPPOINT_UPDATE, txId + "_undo",
                        "money_vp credit undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }

    /**
     * Phase 2 — passing an unknown currency to creditCurrency must return
     * fail("Unknown currency: ...") WITHOUT writing any row to either
     * users, money_gateway_log, or money_transaction.  This is the SQL-injection
     * guard.
     */
    @Test
    public void testCreditCurrency_unknownCurrency_returnsError() throws Exception {
        // No DB password needed for this assertion — it short-circuits on
        // validation — but we still need the suite-wide skip-if-no-pw to fire
        // so the @Before skipIfNoPassword Assume doesn't reject this test
        // alongside the rest. Let it run normally; the validation gate is
        // evaluated before any pool access.
        MoneyGateway.CreditResult r = MoneyGateway.creditCurrency(
                TEST_USER_ID, TEST_NICKNAME, "invalid",
                100L, MoneyGateway.SOURCE_ADMIN_TOPUP, "irrelevant_tx", "boom");
        assertNotNull(r);
        assertFalse("Unknown currency must NOT succeed", r.success);
        assertNotNull("error must be set", r.error);
        assertTrue("error must mention 'Unknown currency': " + r.error,
                r.error.startsWith("Unknown currency"));

        // Same for debitCurrency.
        MoneyGateway.CreditResult r2 = MoneyGateway.debitCurrency(
                TEST_USER_ID, TEST_NICKNAME, "DROP",
                100L, MoneyGateway.SOURCE_WITHDRAW_BANK, "irrelevant_tx", "boom");
        assertNotNull(r2);
        assertFalse("Unknown debit currency must NOT succeed", r2.success);
        assertTrue("error must mention 'Unknown currency': " + r2.error,
                r2.error.startsWith("Unknown currency"));

        // Also reject null currency.
        MoneyGateway.CreditResult r3 = MoneyGateway.creditCurrency(
                TEST_USER_ID, TEST_NICKNAME, null,
                100L, MoneyGateway.SOURCE_ADMIN_TOPUP, "irrelevant_tx", "boom");
        assertFalse(r3.success);
        assertTrue(r3.error.startsWith("Unknown currency"));
    }

    /**
     * Phase 2 — same {@code (tx_id, source, user_id)} can credit BOTH vin AND
     * xu independently because the UNIQUE key is now 4-column
     * {@code (tx_id, source, user_id, currency)}.  This verifies the
     * legitimate "one deposit credits multiple currencies" case.
     */
    @Test
    public void testCreditCurrency_dedup_perCurrency() throws Exception {
        Long playerVinAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        Assume.assumeNotNull("PLAYER_VIN account must exist", playerVinAccId);
        Long playerXuAccId = com.vinplay.vbee.common.ledger.MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_XU");
        Assume.assumeNotNull("PLAYER_XU account must exist", playerXuAccId);

        String txId = REF_PREFIX + "per_ccy_dedup_001";
        cleanTxId(txId);
        long amount = 33L;

        long vinBefore = readColumn(TEST_USER_ID, "vin");
        long xuBefore = readColumn(TEST_USER_ID, "xu");

        try {
            // First call: vin
            MoneyGateway.CreditResult rVin = MoneyGateway.creditCurrency(
                    TEST_USER_ID, TEST_NICKNAME, "vin", amount,
                    MoneyGateway.SOURCE_ADMIN_TOPUP, txId,
                    "Per-currency dedup vin");
            assertTrue("vin credit must succeed: " + (rVin != null ? rVin.error : "null"),
                    rVin != null && rVin.success);

            // Second call SAME txId/source/user but different currency: xu
            MoneyGateway.CreditResult rXu = MoneyGateway.creditCurrency(
                    TEST_USER_ID, TEST_NICKNAME, "xu", amount,
                    MoneyGateway.SOURCE_ADMIN_TOPUP, txId,
                    "Per-currency dedup xu");
            assertTrue("xu credit must succeed (different currency, dedup is per-currency): "
                    + (rXu != null ? rXu.error : "null"),
                    rXu != null && rXu.success);

            // Both balances credited.
            assertEquals("users.vin must be credited by amount",
                    vinBefore + amount, readColumn(TEST_USER_ID, "vin"));
            assertEquals("users.xu must be credited by amount",
                    xuBefore + amount, readColumn(TEST_USER_ID, "xu"));

            // Audit table has 1 row per currency.
            assertEquals("vin audit row must exist", 1, countLegacyRowsForCurrency(txId, "vin"));
            assertEquals("xu audit row must exist", 1, countLegacyRowsForCurrency(txId, "xu"));

            // Third call: same as vin call → must DUPLICATE.
            MoneyGateway.CreditResult rVin2 = MoneyGateway.creditCurrency(
                    TEST_USER_ID, TEST_NICKNAME, "vin", amount,
                    MoneyGateway.SOURCE_ADMIN_TOPUP, txId,
                    "Per-currency dedup vin retry");
            assertNotNull(rVin2);
            assertFalse("Second vin credit must NOT succeed", rVin2.success);
            assertEquals("Duplicate transaction", rVin2.error);
        } finally {
            // Reverse both credits so users.vin and users.xu are unchanged.
            try {
                MoneyGateway.debitCurrency(
                        TEST_USER_ID, TEST_NICKNAME, "vin", amount,
                        MoneyGateway.SOURCE_WITHDRAW_BANK, txId + "_undo_vin",
                        "Per-currency dedup vin undo");
                MoneyGateway.debitCurrency(
                        TEST_USER_ID, TEST_NICKNAME, "xu", amount,
                        MoneyGateway.SOURCE_ADMIN_TOPUP, txId + "_undo_xu",
                        "Per-currency dedup xu undo");
            } catch (Exception ignore) { /* best effort */ }
        }
    }
}
