package com.vinplay.dal.service.seamless.awc;

import org.bson.Document;

/**
 * Per-platform strategy for AWC providers (SEXYBCRT, JILI, CQ9, FACHAI,
 * PGSOFT, …) — sister of the GSC {@code ProviderAdapter} pattern.
 *
 * <p>Captures behaviour that differs between AWC platforms so the
 * read-path code in {@link com.vinplay.dal.service.GameHistoryService}
 * (and any future renderer) can dispatch via
 * {@link AwcPlatformRegistry#forPlatform(String)} instead of inline
 * {@code equalsIgnoreCase} branches that drift apart over time.
 *
 * <h2>Current scope</h2>
 * Only the agency LS Cược / Rolling grouping varies today:
 *
 * <ul>
 *   <li>Live-casino platforms (SEXYBCRT) ship one {@code platform_tx_id}
 *       per <em>leg</em> within a SUBMIT. The vendor's transaction
 *       report shows one row per SUBMIT (sum of that submit's legs).
 *       Grouping by raw {@code platform_tx_id} splits one submit into
 *       multiple agency rows — see Cyan QC 2026-05-13 on SUN-1250.</li>
 *   <li>Slot-style platforms (JILI / CQ9 / FACHAI / PGSOFT) emit one
 *       {@code platform_tx_id} per <em>spin</em>; each spin is its own
 *       independent round and the per-ptxn grain matches the player's
 *       in-game transaction history. Keep ptxn grouping.</li>
 * </ul>
 *
 * <h2>Extending</h2>
 * Add a new platform by:
 * <ol>
 *   <li>Subclass {@link AwcPlatformAdapter} with the platform's quirks
 *       and a {@code public static final INSTANCE} singleton.</li>
 *   <li>Register it in {@link AwcPlatformRegistry} via
 *       {@code register("PLATFORM_CODE", FooAdapter.INSTANCE)}.</li>
 * </ol>
 * Implementations MUST be stateless (singleton-safe). Default methods
 * mirror the historical "ptxn|" behaviour so an un-overridden adapter
 * stays backward-compatible with the pre-strategy code path.
 */
public interface AwcPlatformAdapter {

    /**
     * AWC platform code as it appears on the seamless wallet wire
     * (e.g. {@code "SEXYBCRT"}, {@code "JILI"}). Used as the registry key.
     */
    String platformCode();

    /**
     * Group key for collapsing multi-leg AWC rows in the agency
     * LS Cược / Rolling renderer. The renderer keys its
     * {@link java.util.LinkedHashMap} by this string and accumulates
     * bet/win/turnover amounts across legs sharing the same key.
     *
     * <p>Default: key by {@code platform_tx_id} (the SUN-1250 follow-up
     * behaviour from commit {@code 62944975}). Each leg gets its own
     * row — correct for slot games and Baccarat hedge pairs that
     * already collapse via a separate {@code vendor_game_id} branch
     * upstream.
     *
     * <p>Override (e.g. {@link SexyBcrtAdapter}) to group by a coarser
     * key when the vendor's submit boundary differs from the leg
     * boundary.
     */
    default String historyGroupKey(Document doc) {
        String platformTxId = doc.getString("platform_tx_id");
        if (platformTxId != null && !platformTxId.trim().isEmpty()) {
            return "ptxn|" + platformTxId.trim();
        }
        return null;
    }

    /**
     * Parse a vendor-side table identifier out of the AWC {@code roundId}
     * string. Used by {@code AwcGameNameResolver} to look up the
     * per-table {@code vinplay.games} row (game_code + table_tag) so the
     * agency UI can show "Sexy Baccarat C07" instead of the generic
     * platform stub.
     *
     * <p>Default: return {@code null}. Slot-style providers (JILI, CQ9,
     * FACHAI, PG) emit one round_id per spin with no embedded table
     * identifier — they don't have multiple tables sharing a single
     * game_code, so returning null (and falling back to per-game lookup)
     * is correct.
     *
     * <p>Override (e.g. {@link SexyBcrtAdapter}) when the platform packs
     * a table identifier into the round_id. Pre-2026-05-18 the parsing
     * logic lived as a static regex on
     * {@code AwcGameNameResolver.parseTableSuffix}, which applied
     * Sexy-shaped patterns to <em>every</em> platform's round_id —
     * risking spurious matches on JILI/CQ9 ids that happen to look
     * similar. Centralising in the per-platform adapter eliminates that
     * cross-talk and makes new platforms an additive change.
     */
    default String parseTableSuffix(String roundId) {
        return null;
    }

    /**
     * No-op default used when an unknown platform code arrives. Mirrors
     * the legacy inline branch — falls through to the upstream
     * {@code wager_code} / {@code event_key} / {@code _id} chain when
     * {@link #historyGroupKey(Document)} returns null.
     */
    AwcPlatformAdapter DEFAULT = new AwcPlatformAdapter() {
        @Override public String platformCode() { return ""; }
    };
}
