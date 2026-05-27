package com.vinplay.vbee.common.ledger;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MoneyLedger — the ONLY path for application code to mutate ledger balances.
 *
 * <p>Wraps the {@code vinplay.post_money_transaction} stored procedure with a
 * clean, type-safe Java API. Every overload ultimately calls {@link #post},
 * which validates inputs, serializes entries to JSON, and delegates atomically
 * to the SP. The SP owns all balance arithmetic, idempotency, and invariant
 * enforcement — this class is deliberately thin.
 *
 * <p>Phase 0: the ledger runs in parallel with the legacy {@code users.vin}
 * column — no dual-write yet. Phase 1 will enable dual-write from this class.
 *
 * <p>Design patterns: Static Facade, Thread-Safe Cache (ConcurrentHashMap),
 * Fail-Fast validation.
 *
 * @see <a href="../../../../../../../../docs/architecture/ledger-system-tech-stack-plan.pdf">Financial Ledger System — Tech Stack &amp; Implementation Plan</a>
 * @see <a href="../../../../../../../../docs/architecture/LEDGER_HARDENING_ROADMAP.md">Ledger Hardening Roadmap</a>
 */
public class MoneyLedger {

    private static final Logger logger = Logger.getLogger("backend");

    // -------------------------------------------------------------------------
    // Public enums
    // -------------------------------------------------------------------------

    /**
     * Every status string the SP can return, plus EXCEPTION for Java-layer errors.
     * Mirrors the SP's OUT parameter exactly — any new SP status must be added here.
     */
    public enum Status {
        POSTED,
        DUPLICATE,
        INSUFFICIENT_BALANCE,
        ACCOUNT_NOT_FOUND,
        ACCOUNT_FROZEN,
        UNBALANCED_ENTRIES,
        NEED_AT_LEAST_TWO_ENTRIES,
        AMOUNT_MUST_BE_POSITIVE,
        INVALID_DIRECTION,
        /** Java-layer exception; SP was not reached or did not commit. */
        EXCEPTION
    }

    /** Direction of an individual ledger entry. */
    public enum Direction {
        DEBIT, CREDIT
    }

    // -------------------------------------------------------------------------
    // Nested classes
    // -------------------------------------------------------------------------

    /**
     * Result returned by every MoneyLedger call.
     */
    public static class LedgerResult {
        /** The outcome of the call. Never null. */
        public final Status status;
        /** The new transaction_id if POSTED; 0 if DUPLICATE (id of original tx); 0 if error. */
        public final long transactionId;
        /** Human-readable error detail. Null when status is POSTED. */
        public final String errorMessage;

        private LedgerResult(Status status, long transactionId, String errorMessage) {
            this.status = status;
            this.transactionId = transactionId;
            this.errorMessage = errorMessage;
        }

        /** True iff the transaction was newly posted (not a duplicate). */
        public boolean isPosted() {
            return status == Status.POSTED;
        }

        /** True iff this is either a fresh post or an idempotent duplicate. */
        public boolean isSuccessOrDuplicate() {
            return status == Status.POSTED || status == Status.DUPLICATE;
        }

        @Override
        public String toString() {
            return "LedgerResult{status=" + status
                    + ", transactionId=" + transactionId
                    + (errorMessage != null ? ", error=" + errorMessage : "")
                    + "}";
        }

        // Package-private factory used by post()
        static LedgerResult of(Status status, long transactionId, String errorMessage) {
            return new LedgerResult(status, transactionId, errorMessage);
        }
    }

    /**
     * A single debit or credit line in a ledger transaction.
     */
    public static class Entry {
        public final long accountId;
        public final Direction direction;
        public final long amount;
        /** Optional memo line; may be null. */
        public final String note;

        public Entry(long accountId, Direction direction, long amount, String note) {
            this.accountId = accountId;
            this.direction = direction;
            this.amount = amount;
            this.note = note;
        }

        JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("account_id", accountId);
            obj.put("direction", direction.name());
            obj.put("amount", amount);
            if (note != null) {
                obj.put("note", note);
            } else {
                obj.put("note", JSONObject.NULL);
            }
            return obj;
        }

        @Override
        public String toString() {
            return "Entry{accountId=" + accountId + ", direction=" + direction
                    + ", amount=" + amount + ", note=" + note + "}";
        }
    }

    // -------------------------------------------------------------------------
    // Account-lookup cache
    // Key format: userId + ":" + accountType  (e.g. "5107:PLAYER_VIN")
    // System accounts keyed by accountType only (e.g. "BANK_INBOX")
    // These mappings are stable for the process lifetime — no TTL needed.
    // -------------------------------------------------------------------------

    private static final ConcurrentHashMap<String, Long> PLAYER_ACCOUNT_CACHE
            = new ConcurrentHashMap<String, Long>();
    private static final ConcurrentHashMap<String, Long> SYSTEM_ACCOUNT_CACHE
            = new ConcurrentHashMap<String, Long>();

    // -------------------------------------------------------------------------
    // Core method
    // -------------------------------------------------------------------------

    /**
     * Post a double-entry money transaction.
     *
     * <p>Entries MUST balance: sum(CREDIT amounts) == sum(DEBIT amounts).
     * The SP enforces this atomically and returns UNBALANCED_ENTRIES if violated.
     *
     * @param transactionType       business type label (e.g. "DEPOSIT_BANK") — must not be null/empty
     * @param externalRef           idempotency key — same (type, ref) will never double-post
     * @param initiatorUserId       user who triggered this action; null for system-initiated
     * @param initiatorLabel        human label for the initiator; null OK
     * @param description           free-text memo; null OK
     * @param correlatedTransactionId link to a related transaction_id; null OK
     * @param metadata              arbitrary JSON payload; null OK
     * @param entries               two or more Entry objects that balance
     * @return LedgerResult with status, transactionId, and any error message
     */
    public static LedgerResult post(
            String transactionType,
            String externalRef,
            Long initiatorUserId,
            String initiatorLabel,
            String description,
            Long correlatedTransactionId,
            Map<String, Object> metadata,
            List<Entry> entries) {

        // --- Java-layer validation (fast path before hitting the DB) ---
        if (transactionType == null || transactionType.isEmpty()) {
            return LedgerResult.of(Status.EXCEPTION, 0L, "transactionType must not be null or empty");
        }
        if (externalRef == null || externalRef.isEmpty()) {
            return LedgerResult.of(Status.EXCEPTION, 0L, "externalRef must not be null or empty");
        }
        if (entries == null || entries.size() < 2) {
            return LedgerResult.of(Status.NEED_AT_LEAST_TWO_ENTRIES, 0L,
                    "At least 2 entries are required");
        }

        logger.info("MoneyLedger.post: type=" + transactionType
                + " ref=" + externalRef
                + " initiator=" + initiatorUserId
                + " entries=" + entries.size());

        // Build entries JSON array
        JSONArray entriesJson = new JSONArray();
        for (Entry e : entries) {
            entriesJson.put(e.toJson());
        }

        // Build optional metadata JSON string (null → null to SP)
        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            metadataJson = new JSONObject(metadata).toString();
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // {CALL vinplay.post_money_transaction(p_type, p_ref, p_init_uid,
            //   p_init_label, p_description, p_correlated_tx_id,
            //   p_metadata, p_entries_json, OUT p_transaction_id, OUT p_status)}
            CallableStatement cs = conn.prepareCall(
                    "{CALL vinplay.post_money_transaction(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            cs.setString(1, transactionType);
            cs.setString(2, externalRef);

            if (initiatorUserId != null) {
                cs.setInt(3, initiatorUserId.intValue());
            } else {
                cs.setNull(3, Types.INTEGER);
            }

            if (initiatorLabel != null) {
                cs.setString(4, initiatorLabel);
            } else {
                cs.setNull(4, Types.VARCHAR);
            }

            if (description != null) {
                cs.setString(5, description);
            } else {
                cs.setNull(5, Types.VARCHAR);
            }

            if (correlatedTransactionId != null) {
                cs.setLong(6, correlatedTransactionId);
            } else {
                cs.setNull(6, Types.BIGINT);
            }

            if (metadataJson != null) {
                cs.setString(7, metadataJson);
            } else {
                cs.setNull(7, Types.VARCHAR);
            }

            cs.setString(8, entriesJson.toString());

            // OUT parameters
            cs.registerOutParameter(9, Types.BIGINT);   // p_transaction_id
            cs.registerOutParameter(10, Types.VARCHAR); // p_status

            cs.execute();

            long txId = cs.getLong(9);
            String statusStr = cs.getString(10);

            Status status = parseStatus(statusStr);

            if (status == Status.POSTED) {
                logger.info("MoneyLedger.post OK: type=" + transactionType
                        + " ref=" + externalRef + " txId=" + txId);
            } else if (status == Status.DUPLICATE) {
                logger.info("MoneyLedger.post DUPLICATE: type=" + transactionType
                        + " ref=" + externalRef + " originalTxId=" + txId);
            } else {
                logger.warn("MoneyLedger.post " + status + ": type=" + transactionType
                        + " ref=" + externalRef + " spStatus=" + statusStr);
            }

            String errMsg = (status == Status.POSTED || status == Status.DUPLICATE)
                    ? null : statusStr;
            return LedgerResult.of(status, txId, errMsg);

        } catch (Exception e) {
            logger.error("MoneyLedger.post EXCEPTION: type=" + transactionType
                    + " ref=" + externalRef, e);
            return LedgerResult.of(Status.EXCEPTION, 0L, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Convenience methods
    // -------------------------------------------------------------------------

    /**
     * Credit a player account from a system pool.
     *
     * <p>Creates a balanced two-entry transaction:
     * <ul>
     *   <li>DEBIT  systemAccountId by {@code amount}
     *   <li>CREDIT playerAccountId by {@code amount}
     * </ul>
     *
     * @param playerAccountId account_id of the player's wallet (from money_account)
     * @param systemAccountId account_id of the source pool (e.g. BANK_INBOX)
     * @param amount          positive amount in base currency units
     * @param type            transaction type label (e.g. "DEPOSIT_BANK")
     * @param externalRef     idempotency key
     * @param description     optional memo; null OK
     * @param metadata        optional metadata; null OK
     * @return LedgerResult
     */
    public static LedgerResult credit(
            long playerAccountId,
            long systemAccountId,
            long amount,
            String type,
            String externalRef,
            String description,
            Map<String, Object> metadata) {

        List<Entry> entries = new java.util.ArrayList<Entry>();
        entries.add(new Entry(systemAccountId, Direction.DEBIT, amount, "system source"));
        entries.add(new Entry(playerAccountId, Direction.CREDIT, amount, "player credit"));

        return post(type, externalRef, null, null, description, null, metadata, entries);
    }

    /**
     * Debit a player account to a system pool (atomic; returns INSUFFICIENT_BALANCE
     * if the player's balance would go negative).
     *
     * <p>Creates a balanced two-entry transaction:
     * <ul>
     *   <li>DEBIT  playerAccountId by {@code amount}
     *   <li>CREDIT systemAccountId by {@code amount}
     * </ul>
     *
     * @param playerAccountId account_id of the player's wallet
     * @param systemAccountId account_id of the destination pool (e.g. BANK_OUTBOX)
     * @param amount          positive amount in base currency units
     * @param type            transaction type label (e.g. "WITHDRAW_BANK")
     * @param externalRef     idempotency key
     * @param description     optional memo; null OK
     * @param metadata        optional metadata; null OK
     * @return LedgerResult; status will be INSUFFICIENT_BALANCE if player cannot cover {@code amount}
     */
    public static LedgerResult debit(
            long playerAccountId,
            long systemAccountId,
            long amount,
            String type,
            String externalRef,
            String description,
            Map<String, Object> metadata) {

        List<Entry> entries = new java.util.ArrayList<Entry>();
        entries.add(new Entry(playerAccountId, Direction.DEBIT, amount, "player debit"));
        entries.add(new Entry(systemAccountId, Direction.CREDIT, amount, "system destination"));

        return post(type, externalRef, null, null, description, null, metadata, entries);
    }

    /**
     * Transfer between two non-system accounts.
     *
     * <p>Creates a balanced two-entry transaction:
     * <ul>
     *   <li>DEBIT  fromAccountId by {@code amount}
     *   <li>CREDIT toAccountId   by {@code amount}
     * </ul>
     *
     * @param fromAccountId source account_id (balance checked — returns INSUFFICIENT_BALANCE if short)
     * @param toAccountId   destination account_id
     * @param amount        positive amount
     * @param type          transaction type label
     * @param externalRef   idempotency key
     * @param description   optional memo; null OK
     * @param metadata      optional metadata; null OK
     * @return LedgerResult
     */
    public static LedgerResult transfer(
            long fromAccountId,
            long toAccountId,
            long amount,
            String type,
            String externalRef,
            String description,
            Map<String, Object> metadata) {

        List<Entry> entries = new java.util.ArrayList<Entry>();
        entries.add(new Entry(fromAccountId, Direction.DEBIT, amount, "transfer out"));
        entries.add(new Entry(toAccountId, Direction.CREDIT, amount, "transfer in"));

        return post(type, externalRef, null, null, description, null, metadata, entries);
    }

    // -------------------------------------------------------------------------
    // Account lookup helpers
    // -------------------------------------------------------------------------

    /**
     * Find the money_account.account_id for a given player + account type.
     *
     * <p>Results are cached permanently in-process (mappings are stable).
     * Logs WARN if not found, returns null.
     *
     * @param userId      users.id
     * @param accountType one of PLAYER_VIN | PLAYER_XU | PLAYER_SAFE | PLAYER_VP
     * @return account_id, or null if not found
     */
    public static Long findPlayerAccount(long userId, String accountType) {
        String cacheKey = userId + ":" + accountType;
        Long cached = PLAYER_ACCOUNT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT account_id FROM vinplay.money_account"
                     + " WHERE owner_user_id = ? AND account_type = ? LIMIT 1")) {
            ps.setLong(1, userId);
            ps.setString(2, accountType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    PLAYER_ACCOUNT_CACHE.putIfAbsent(cacheKey, accountId);
                    return accountId;
                }
            }
        } catch (Exception e) {
            logger.error("MoneyLedger.findPlayerAccount error: userId=" + userId
                    + " type=" + accountType, e);
            return null;
        }

        logger.warn("MoneyLedger.findPlayerAccount: no account found for userId="
                + userId + " type=" + accountType);
        return null;
    }

    /**
     * Find the money_account.account_id for a system account by type.
     *
     * <p>Results are cached permanently in-process.
     *
     * @param accountType e.g. BANK_INBOX, HOUSE_REVENUE, SUSPENSE
     * @return account_id, or null if not found
     */
    public static Long findSystemAccount(String accountType) {
        Long cached = SYSTEM_ACCOUNT_CACHE.get(accountType);
        if (cached != null) {
            return cached;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT account_id FROM vinplay.money_account"
                     + " WHERE account_type = ? AND is_system = 1 AND owner_user_id IS NULL LIMIT 1")) {
            ps.setString(1, accountType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    SYSTEM_ACCOUNT_CACHE.putIfAbsent(accountType, accountId);
                    return accountId;
                }
            }
        } catch (Exception e) {
            logger.error("MoneyLedger.findSystemAccount error: type=" + accountType, e);
            return null;
        }

        logger.warn("MoneyLedger.findSystemAccount: no system account found for type=" + accountType);
        return null;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Convert the SP's OUT status string to the Java enum.
     * Unknown values map to EXCEPTION so callers always get a typed result.
     */
    private static Status parseStatus(String spStatus) {
        if (spStatus == null) return Status.EXCEPTION;
        try {
            return Status.valueOf(spStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("MoneyLedger: unknown SP status string '" + spStatus
                    + "' — mapping to EXCEPTION");
            return Status.EXCEPTION;
        }
    }
}
