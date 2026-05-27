package com.vinplay.vbee.common.ledger;

import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.ledger.MoneyLedger.LedgerResult;
import com.vinplay.vbee.common.ledger.MoneyLedger.Status;
import com.vinplay.vbee.common.ledger.MoneyLedger.Direction;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Integration tests for MoneyLedger — exercises the live staging MySQL.
 *
 * <p><b>Prerequisite:</b> sunwinkr-mysql Docker container must be running and
 * reachable on 127.0.0.1:3306. Run the Phase 0 migrations first (tasks #174–#176).
 *
 * <p><b>Password injection:</b> {@code db_pool.properties} contains the placeholder
 * {@code __INJECT_AT_TEST_TIME__} instead of a real password. Supply the password via
 * {@code -Dtest.mysql.password=<pw>} or the {@code MYSQL_PASSWORD} environment variable.
 * When neither is set tests are <em>skipped</em> (not failed) via {@link Assume}.
 *
 * <pre>
 * To run:
 *   ./gradlew :VbeeCommon:test --tests MoneyLedgerTest \
 *      -Dtest.mysql.password=$(grep MYSQL_PASSWORD /root/sunwinkr/sunwinkr-backend/.env | cut -d= -f2)
 * </pre>
 *
 * <p><b>Test isolation strategy:</b>
 * <ul>
 *   <li>Each test uses a unique externalRef prefixed with {@code "test_ml_"}.
 *   <li>{@link #setUpTestAccounts()} deletes any leftover {@code money_entry},
 *       {@code money_transaction}, and ephemeral account rows from a previous run
 *       <em>before</em> creating fresh accounts — ensuring deterministic state.
 *   <li>{@link #cleanUp()} performs the same full cleanup after the suite.
 * </ul>
 *
 * <p><b>Note on ConnectionPool bootstrap:</b> The pool reads from
 * {@code VBeePath.basePath + "config/db_pool.properties"}. The @BeforeClass sets
 * basePath to the test-resources directory so HikariCP connects to localhost:3306
 * (the Docker-mapped port) rather than the container-internal {@code mysql:3306}.
 */
public class MoneyLedgerTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** User 5107 exists in staging and has all 4 player account types seeded. */
    private static final long TEST_USER_ID = 5107L;

    /** Starting balance for the "from" / player account in each test. */
    private static final long INITIAL_BALANCE_A = 1000L;

    /** Starting balance for the "to" / secondary account in each test. */
    private static final long INITIAL_BALANCE_B = 0L;

    // -------------------------------------------------------------------------
    // Suite-level state
    // -------------------------------------------------------------------------

    /** Set to true if password is missing and bootstrap was skipped.
     * When true, each @Before will issue Assume.assumeFalse to cleanly skip the test. */
    private static volatile boolean bootstrapSkipped = false;

    // -------------------------------------------------------------------------
    // Instance fields — one fresh set per test instance (parallel-safe)
    // -------------------------------------------------------------------------

    /** account_id of the BANK_INBOX system account (seeded in #175). */
    private long BANK_INBOX_ID = -1L;

    /** account_id of the ephemeral TEST_PLAYER_A account created per-test. */
    private long TEST_ACCOUNT_A = -1L;

    /** account_id of the ephemeral TEST_PLAYER_B account created per-test. */
    private long TEST_ACCOUNT_B = -1L;

    // -------------------------------------------------------------------------
    // Suite-level setup / teardown
    // -------------------------------------------------------------------------

    @BeforeClass
    public static void bootstrapPool() throws Exception {
        // Resolve the password: system property first, then env var, then skip.
        String pw = System.getProperty("test.mysql.password");
        if (pw == null || pw.isEmpty()) {
            pw = System.getenv("MYSQL_PASSWORD");
        }

        // If password is missing, set a flag and return silently.
        // Each @Before will then skip the test cleanly via Assume.
        if (pw == null || pw.isEmpty()) {
            bootstrapSkipped = true;
            System.out.println("MoneyLedgerTest: password not supplied; tests will be skipped");
            return;
        }

        // Point VBeePath.basePath to the test-resources directory so the pool
        // reads test/resources/config/db_pool.properties (localhost:3306).
        URL propsUrl = MoneyLedgerTest.class
                .getClassLoader()
                .getResource("config/db_pool.properties");
        String resourcesDir = propsUrl.getPath()
                .replace("config/db_pool.properties", "");
        VBeePath.basePath = resourcesDir;

        // Load the properties file and substitute the password placeholder BEFORE
        // ConnectionPool reads it.  This must happen before getInstance() is called.
        Properties props = new Properties();
        try (InputStream is = propsUrl.openStream()) {
            props.load(is);
        }
        final String PLACEHOLDER = "__INJECT_AT_TEST_TIME__";
        String currentPw = props.getProperty("mysqlpoolname.password", "");
        if (PLACEHOLDER.equals(currentPw)) {
            props.setProperty("mysqlpoolname.password", pw);
        }

        // Persist the substituted properties back to the file so ConnectionPool
        // (which re-reads the file via VBeePath) picks up the real password.
        java.io.File propsFile = new java.io.File(propsUrl.toURI());
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propsFile)) {
            props.store(fos, "Auto-generated by MoneyLedgerTest — password injected at test time");
        }

        // Force pool initialization (singleton; safe to call multiple times).
        ConnectionPool.getInstance();
    }

    @AfterClass
    public static void cleanUp() {
        // If bootstrap was skipped (no password), skip cleanup as well.
        if (bootstrapSkipped) {
            return;
        }

        // Restore placeholder in the properties file so the real password is not
        // left committed on disk after the test run.
        try {
            URL propsUrl = MoneyLedgerTest.class
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
            System.err.println("MoneyLedgerTest: could not restore placeholder (non-fatal): "
                    + e.getMessage());
        }

        // Delete all test entries / transactions / accounts.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // 1. entries first (referencing transactions)
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE e FROM vinplay.money_entry e"
                    + " JOIN vinplay.money_transaction t ON t.transaction_id = e.transaction_id"
                    + " WHERE t.external_ref LIKE 'test_ml_%'")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyLedgerTest cleanup: deleted " + rows + " money_entry rows");
            }
            // 2. transactions
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_transaction WHERE external_ref LIKE 'test_ml_%'")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyLedgerTest cleanup: deleted " + rows + " money_transaction rows");
            }
            // 3. ephemeral test accounts
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_account"
                    + " WHERE account_type IN ('TEST_LEDGER_A', 'TEST_LEDGER_B')")) {
                int rows = ps.executeUpdate();
                System.out.println("MoneyLedgerTest cleanup: deleted " + rows + " test account rows");
            }
        } catch (Exception e) {
            System.err.println("MoneyLedgerTest cleanup error (non-fatal): " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Per-test setup: clean prior test data, then create fresh ephemeral accounts
    // -------------------------------------------------------------------------

    @Before
    public void setUpTestAccounts() throws Exception {
        // Skip if bootstrap failed due to missing password. Each test will be skipped cleanly.
        Assume.assumeFalse(
                "test.mysql.password system property or MYSQL_PASSWORD env var must be set."
                + " Supply via: ./gradlew :VbeeCommon:test --tests MoneyLedgerTest"
                + " -Dtest.mysql.password=$(grep MYSQL_PASSWORD /path/to/.env | cut -d= -f2)",
                bootstrapSkipped);

        // Resolve BANK_INBOX for this test instance.
        Long bankInbox = MoneyLedger.findSystemAccount("BANK_INBOX");
        assertNotNull("BANK_INBOX system account must exist (run task #175 migration)", bankInbox);
        BANK_INBOX_ID = bankInbox;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // 1. Delete leftover money_entry rows from previous tests (entries first).
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE e FROM vinplay.money_entry e"
                    + " JOIN vinplay.money_transaction t ON t.transaction_id = e.transaction_id"
                    + " WHERE t.external_ref LIKE 'test_ml_%'")) {
                ps.executeUpdate();
            }
            // 2. Delete leftover money_transaction rows from previous tests.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_transaction WHERE external_ref LIKE 'test_ml_%'")) {
                ps.executeUpdate();
            }
            // 3. Remove any leftover ephemeral accounts from a previous interrupted run.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM vinplay.money_account"
                    + " WHERE account_type IN ('TEST_LEDGER_A', 'TEST_LEDGER_B')")) {
                ps.executeUpdate();
            }
        }

        // Insert fresh accounts with deterministic balances.
        // is_system=0 so DEBIT is floor-checked; owner_user_id set to null to avoid
        // the uk_owner_type unique constraint (we just want ephemeral test rows).
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO vinplay.money_account"
                    + " (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)"
                    + " VALUES (?, NULL, ?, 'VND', ?, 0, 0)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {

                // Account A
                ps.setString(1, "TEST_LEDGER_A");
                ps.setString(2, "test_player_a");
                ps.setLong(3, INITIAL_BALANCE_A);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    assertTrue("TEST_LEDGER_A insert must succeed", keys.next());
                    TEST_ACCOUNT_A = keys.getLong(1);
                }

                // Account B
                ps.setString(1, "TEST_LEDGER_B");
                ps.setString(2, "test_player_b");
                ps.setLong(3, INITIAL_BALANCE_B);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    assertTrue("TEST_LEDGER_B insert must succeed", keys.next());
                    TEST_ACCOUNT_B = keys.getLong(1);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper: read current balance from money_account
    // -------------------------------------------------------------------------

    private long readBalance(long accountId) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT balance FROM vinplay.money_account WHERE account_id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Account " + accountId + " must exist", rs.next());
                return rs.getLong("balance");
            }
        }
    }

    /** Count money_entry rows for a given transaction_id. */
    private int countEntries(long transactionId) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_entry WHERE transaction_id = ?")) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Sum of signed entry amounts (CREDIT positive, DEBIT negative) for a given tx. */
    private long sumSignedEntries(long transactionId) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(IF(direction='CREDIT', amount, -amount)), 0)"
                     + " FROM vinplay.money_entry WHERE transaction_id = ?")) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Test 1: credit happy path
    // -------------------------------------------------------------------------

    /**
     * Pre-condition: TEST_ACCOUNT_A balance = 1000.
     * Action: credit 500 (DEBIT BANK_INBOX, CREDIT TEST_ACCOUNT_A).
     * Assert: status=POSTED, txId>0, balance=1500, 2 entry rows summing to 0.
     */
    @Test
    public void testCredit_HappyPath() throws Exception {
        // Start: TEST_ACCOUNT_A = INITIAL_BALANCE_A (1000)
        LedgerResult result = MoneyLedger.credit(
                TEST_ACCOUNT_A, BANK_INBOX_ID, 500L,
                "DEPOSIT_BANK", "test_ml_credit_happy_001",
                "Test credit happy path",
                Collections.<String, Object>singletonMap("test", "testCredit_HappyPath"));

        assertEquals("Status must be POSTED", Status.POSTED, result.status);
        assertTrue("transactionId must be > 0", result.transactionId > 0);

        long balance = readBalance(TEST_ACCOUNT_A);
        assertEquals("Balance must be 1500 after credit of 500", 1500L, balance);

        int entries = countEntries(result.transactionId);
        assertEquals("Exactly 2 entry rows expected", 2, entries);

        long net = sumSignedEntries(result.transactionId);
        assertEquals("Entries must sum to 0 (balanced)", 0L, net);
    }

    // -------------------------------------------------------------------------
    // Test 2: debit — insufficient balance
    // -------------------------------------------------------------------------

    /**
     * Pre-condition: TEST_ACCOUNT_A balance = 1000.
     * Action: debit 5000 (more than balance).
     * Assert: status=INSUFFICIENT_BALANCE, balance unchanged, no entries.
     */
    @Test
    public void testDebit_Insufficient() throws Exception {
        LedgerResult result = MoneyLedger.debit(
                TEST_ACCOUNT_A, BANK_INBOX_ID, 5000L,
                "WITHDRAW_BANK", "test_ml_debit_insufficient_001",
                "Test debit insufficient",
                null);

        assertEquals("Status must be INSUFFICIENT_BALANCE",
                Status.INSUFFICIENT_BALANCE, result.status);

        long balance = readBalance(TEST_ACCOUNT_A);
        assertEquals("Balance must be unchanged at 1000", INITIAL_BALANCE_A, balance);

        // Verify no transaction was committed
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_transaction"
                     + " WHERE external_ref = 'test_ml_debit_insufficient_001'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("No transaction row should exist", 0, rs.getInt(1));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Test 3: debit — happy path
    // -------------------------------------------------------------------------

    /**
     * Pre-condition: TEST_ACCOUNT_A balance = 1000.
     * Action: debit 300.
     * Assert: status=POSTED, balance=700, entries inserted.
     */
    @Test
    public void testDebit_HappyPath() throws Exception {
        LedgerResult result = MoneyLedger.debit(
                TEST_ACCOUNT_A, BANK_INBOX_ID, 300L,
                "WITHDRAW_BANK", "test_ml_debit_happy_001",
                "Test debit happy path",
                null);

        assertEquals("Status must be POSTED", Status.POSTED, result.status);
        assertTrue("transactionId must be > 0", result.transactionId > 0);

        long balance = readBalance(TEST_ACCOUNT_A);
        assertEquals("Balance must be 700 after debit of 300", 700L, balance);

        int entries = countEntries(result.transactionId);
        assertEquals("Exactly 2 entry rows expected", 2, entries);

        long net = sumSignedEntries(result.transactionId);
        assertEquals("Entries must sum to 0", 0L, net);
    }

    // -------------------------------------------------------------------------
    // Test 4: idempotency — duplicate
    // -------------------------------------------------------------------------

    /**
     * Post a credit. Then post the same credit again with the same (type, externalRef).
     * Assert: first call=POSTED, second call=DUPLICATE, balance unchanged after second call.
     */
    @Test
    public void testIdempotency_Duplicate() throws Exception {
        String ref = "test_ml_idem_001";
        String type = "DEPOSIT_BANK";

        // First post
        LedgerResult first = MoneyLedger.credit(
                TEST_ACCOUNT_A, BANK_INBOX_ID, 500L,
                type, ref, "Idempotency test — first post", null);
        assertEquals("First post must be POSTED", Status.POSTED, first.status);

        long balanceAfterFirst = readBalance(TEST_ACCOUNT_A);
        assertEquals("Balance after first post must be 1500", 1500L, balanceAfterFirst);

        // Second post — same type + ref
        LedgerResult second = MoneyLedger.credit(
                TEST_ACCOUNT_A, BANK_INBOX_ID, 500L,
                type, ref, "Idempotency test — second post", null);
        assertEquals("Second post must be DUPLICATE", Status.DUPLICATE, second.status);
        assertEquals("DUPLICATE must return the original transactionId",
                first.transactionId, second.transactionId);

        long balanceAfterSecond = readBalance(TEST_ACCOUNT_A);
        assertEquals("Balance must not change on DUPLICATE — no double-credit",
                balanceAfterFirst, balanceAfterSecond);
    }

    // -------------------------------------------------------------------------
    // Test 5: transfer — happy path
    // -------------------------------------------------------------------------

    /**
     * Pre-condition: TEST_ACCOUNT_A=1000, TEST_ACCOUNT_B=0.
     * Action: transfer 400 from A to B.
     * Assert: A=600, B=400.
     */
    @Test
    public void testTransfer_HappyPath() throws Exception {
        LedgerResult result = MoneyLedger.transfer(
                TEST_ACCOUNT_A, TEST_ACCOUNT_B, 400L,
                "PLAYER_TRANSFER", "test_ml_xfer_happy_001",
                "Test transfer happy path",
                null);

        assertEquals("Status must be POSTED", Status.POSTED, result.status);

        long balanceA = readBalance(TEST_ACCOUNT_A);
        long balanceB = readBalance(TEST_ACCOUNT_B);
        assertEquals("Account A must be 600 after transferring 400", 600L, balanceA);
        assertEquals("Account B must be 400 after receiving 400", 400L, balanceB);
    }

    // -------------------------------------------------------------------------
    // Test 6: transfer — insufficient balance
    // -------------------------------------------------------------------------

    /**
     * Pre-condition: TEST_ACCOUNT_A=1000.
     * Action: transfer 5000 from A to B.
     * Assert: INSUFFICIENT_BALANCE; both balances unchanged.
     */
    @Test
    public void testTransfer_Insufficient() throws Exception {
        LedgerResult result = MoneyLedger.transfer(
                TEST_ACCOUNT_A, TEST_ACCOUNT_B, 5000L,
                "PLAYER_TRANSFER", "test_ml_xfer_insuf_001",
                "Test transfer insufficient",
                null);

        assertEquals("Status must be INSUFFICIENT_BALANCE",
                Status.INSUFFICIENT_BALANCE, result.status);

        long balanceA = readBalance(TEST_ACCOUNT_A);
        long balanceB = readBalance(TEST_ACCOUNT_B);
        assertEquals("Account A must be unchanged at 1000", INITIAL_BALANCE_A, balanceA);
        assertEquals("Account B must be unchanged at 0", INITIAL_BALANCE_B, balanceB);
    }

    // -------------------------------------------------------------------------
    // Test 7: findPlayerAccount — caching
    // -------------------------------------------------------------------------

    /**
     * Find user 5107's PLAYER_VIN account. Must return non-null (seeded in #176).
     * Call again — must return same value (hits cache).
     * Verify the value is deterministic across two calls.
     */
    @Test
    public void testFindPlayerAccount_Cached() {
        Long accountId1 = MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        assertNotNull("PLAYER_VIN account for user 5107 must exist", accountId1);
        assertTrue("account_id must be positive", accountId1 > 0);

        // Second call — should be served from cache without DB hit
        Long accountId2 = MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_VIN");
        assertNotNull("Second call must also return non-null", accountId2);
        assertEquals("Both calls must return the same account_id (cache hit)", accountId1, accountId2);

        // Also verify PLAYER_XU is a different account
        Long xuId = MoneyLedger.findPlayerAccount(TEST_USER_ID, "PLAYER_XU");
        assertNotNull("PLAYER_XU account must exist", xuId);
        assertNotEquals("PLAYER_VIN and PLAYER_XU must be different accounts", accountId1, xuId);
    }

    // -------------------------------------------------------------------------
    // Test 8: unbalanced entries rejected
    // -------------------------------------------------------------------------

    /**
     * Post a transaction where entries don't sum to zero.
     * Assert: status=UNBALANCED_ENTRIES; no transaction inserted.
     */
    @Test
    public void testPost_UnbalancedEntries() throws Exception {
        java.util.List<MoneyLedger.Entry> entries = new java.util.ArrayList<MoneyLedger.Entry>();
        // DEBIT 500, CREDIT 300 — net = -200 (unbalanced)
        entries.add(new MoneyLedger.Entry(TEST_ACCOUNT_A, Direction.DEBIT, 500L, null));
        entries.add(new MoneyLedger.Entry(TEST_ACCOUNT_B, Direction.CREDIT, 300L, null));

        LedgerResult result = MoneyLedger.post(
                "TEST_UNBALANCED", "test_ml_unbalanced_001",
                null, null, "Unbalanced entries test", null, null,
                entries);

        assertEquals("Status must be UNBALANCED_ENTRIES",
                Status.UNBALANCED_ENTRIES, result.status);

        // Verify no transaction was committed and balances are untouched
        long balanceA = readBalance(TEST_ACCOUNT_A);
        assertEquals("Account A balance must be unchanged", INITIAL_BALANCE_A, balanceA);

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM vinplay.money_transaction"
                     + " WHERE external_ref = 'test_ml_unbalanced_001'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("No transaction row should exist for unbalanced post", 0, rs.getInt(1));
            }
        }
    }
}
