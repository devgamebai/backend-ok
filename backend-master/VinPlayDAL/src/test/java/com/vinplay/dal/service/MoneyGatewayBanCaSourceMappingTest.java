package com.vinplay.dal.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * SUN-1054 / Wallet Phase 5b — pure unit coverage for the three new BanCa
 * MoneyGateway source mappings introduced for the unified-wallet bridge
 * (c=9998 BanCaSettleProcessor + C# {@code MoneyGatewayClient.SettleAsync}):
 *
 * <ul>
 *   <li>{@code WAGER_DEBIT_BANCA}  -> debit  WAGER_DEBIT  / HOUSE_GAME_POT</li>
 *   <li>{@code WAGER_CREDIT_BANCA} -> credit WAGER_CREDIT / HOUSE_GAME_POT</li>
 *   <li>{@code EMERGENCY_BANCA}    -> debit  WAGER_DEBIT  / HOUSE_GAME_POT</li>
 * </ul>
 *
 * <p>No DB access — the test only exercises the static map methods so it
 * runs as part of the default {@code :VinPlayDAL:test} target without any
 * MySQL prerequisites (unlike {@link MoneyGatewayDualWriteTest}).
 *
 * <pre>
 *   ./gradlew :VinPlayDAL:test --tests MoneyGatewayBanCaSourceMappingTest
 * </pre>
 */
public class MoneyGatewayBanCaSourceMappingTest {

    private static final String[] DEBIT_SOURCES = {
            "WAGER_DEBIT_BANCA",
            "EMERGENCY_BANCA"
    };
    private static final String[] CREDIT_SOURCES = {
            "WAGER_CREDIT_BANCA",
            // Phase 5c (SUN-1054): EMERGENCY_BANCA is direction-agnostic — it
            // is reused for daily bonus credits, IAP top-ups, cancel-cashout
            // refunds, admin-credits AND Revive crash-recovery debits. Both
            // map directions are valid.
            "EMERGENCY_BANCA"
    };
    // Direction-agnostic sources that legitimately appear on BOTH the credit
    // and debit maps. They are excluded from the cross-domain isolation
    // assertions below.
    private static final java.util.Set<String> DUAL_DIRECTION_SOURCES =
            new java.util.HashSet<>(java.util.Arrays.asList("EMERGENCY_BANCA"));

    @Test
    public void debitSources_mapToWagerDebit() {
        for (String s : DEBIT_SOURCES) {
            assertEquals("debit source " + s + " must map to WAGER_DEBIT",
                    "WAGER_DEBIT", MoneyGateway.mapDebitSourceToLedgerType(s));
        }
    }

    @Test
    public void debitSources_mapToHouseGamePot() {
        for (String s : DEBIT_SOURCES) {
            assertEquals("debit source " + s + " must map to HOUSE_GAME_POT",
                    "HOUSE_GAME_POT", MoneyGateway.mapDebitSourceToSystemAccount(s));
        }
    }

    @Test
    public void creditSource_mapsToWagerCredit() {
        for (String s : CREDIT_SOURCES) {
            assertEquals("credit source " + s + " must map to WAGER_CREDIT",
                    "WAGER_CREDIT", MoneyGateway.mapSourceToLedgerType(s));
        }
    }

    @Test
    public void creditSource_mapsToHouseGamePot() {
        for (String s : CREDIT_SOURCES) {
            assertEquals("credit source " + s + " must map to HOUSE_GAME_POT",
                    "HOUSE_GAME_POT", MoneyGateway.mapSourceToSystemAccount(s));
        }
    }

    /**
     * Symmetric invariant — every new source must resolve to BOTH a ledger
     * type AND a system account. A half-mapped source would make the
     * dual-write helper log a WARN and silently skip ledger writes, leading
     * to drift between money_gateway_log and money_ledger.
     */
    @Test
    public void allBanCaSources_areSymmetric() {
        for (String s : DEBIT_SOURCES) {
            assertNotNull("debit source " + s + " missing ledger type",
                    MoneyGateway.mapDebitSourceToLedgerType(s));
            assertNotNull("debit source " + s + " missing system account",
                    MoneyGateway.mapDebitSourceToSystemAccount(s));
        }
        for (String s : CREDIT_SOURCES) {
            assertNotNull("credit source " + s + " missing ledger type",
                    MoneyGateway.mapSourceToLedgerType(s));
            assertNotNull("credit source " + s + " missing system account",
                    MoneyGateway.mapSourceToSystemAccount(s));
        }
    }

    /**
     * Confirm the new sources are exposed as SOURCE_* constants — callers
     * (BanCaSettleProcessor) reference these by name, so renaming the
     * literal here without bumping the constant must fail the build.
     */
    @Test
    public void sourceConstants_matchLiteralValues() {
        assertEquals("WAGER_DEBIT_BANCA",  MoneyGateway.SOURCE_WAGER_DEBIT_BANCA);
        assertEquals("WAGER_CREDIT_BANCA", MoneyGateway.SOURCE_WAGER_CREDIT_BANCA);
        assertEquals("EMERGENCY_BANCA",    MoneyGateway.SOURCE_EMERGENCY_BANCA);
    }

    /**
     * Cross-domain isolation — a debit-only source must not appear on the
     * credit path (and vice versa). Catches a foot-gun where someone
     * accidentally adds {@code WAGER_DEBIT_BANCA} to mapSourceToLedgerType
     * and double-routes a credit through both directions.
     */
    @Test
    public void debitSourcesAreNotCreditMapped() {
        for (String s : DEBIT_SOURCES) {
            if (DUAL_DIRECTION_SOURCES.contains(s)) continue;
            assertNull("debit source " + s + " must NOT be on the credit map",
                    MoneyGateway.mapSourceToLedgerType(s));
            assertNull("debit source " + s + " must NOT have a credit system account",
                    MoneyGateway.mapSourceToSystemAccount(s));
        }
    }

    @Test
    public void creditSourcesAreNotDebitMapped() {
        for (String s : CREDIT_SOURCES) {
            if (DUAL_DIRECTION_SOURCES.contains(s)) continue;
            assertNull("credit source " + s + " must NOT be on the debit map",
                    MoneyGateway.mapDebitSourceToLedgerType(s));
            assertNull("credit source " + s + " must NOT have a debit system account",
                    MoneyGateway.mapDebitSourceToSystemAccount(s));
        }
    }

    /**
     * Phase 5c (SUN-1054) — EMERGENCY_BANCA is direction-agnostic. The
     * BanCaSettleProcessor derives credit vs. debit from the sign of
     * amount_milli, so the mapping tables must resolve in BOTH directions.
     */
    @Test
    public void emergencyBanca_isBidirectional() {
        assertEquals("WAGER_DEBIT",
                MoneyGateway.mapDebitSourceToLedgerType("EMERGENCY_BANCA"));
        assertEquals("HOUSE_GAME_POT",
                MoneyGateway.mapDebitSourceToSystemAccount("EMERGENCY_BANCA"));
        assertEquals("WAGER_CREDIT",
                MoneyGateway.mapSourceToLedgerType("EMERGENCY_BANCA"));
        assertEquals("HOUSE_GAME_POT",
                MoneyGateway.mapSourceToSystemAccount("EMERGENCY_BANCA"));
    }
}
