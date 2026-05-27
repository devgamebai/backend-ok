package com.vinplay.dal.service.seamless.awc;

import org.bson.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy adapter for AWC platform {@code SEXYBCRT} (Sexy Baccarat /
 * SicBo live casino).
 *
 * <p><b>SUN-1250 reopen (Cyan QC 2026-05-13).</b> Sexy ships one
 * {@code platform_tx_id} per <em>leg</em> within a SUBMIT (Rồng / Tie /
 * Bonus / …), and the player can issue multiple SUBMITs against the same
 * round_id. The vendor's transaction report shows ONE row per submit
 * (sum of the submit's legs); operators audit against that. The plain
 * {@code platform_tx_id} grain (the SUN-1250 follow-up default) split a
 * 2-submit / 5-leg round into 5 agency rows instead of 2.
 *
 * <p><b>Group key:</b> {@code sexy|wager_code|bet_time[:19]}. Legs of one
 * submit cluster within ~10 ms (verified on staging round
 * {@code Mexico-C05-GA217960048}: 3 legs inside 11 ms, then 2 legs
 * 5 seconds later); truncating {@code bet_time} to seconds collapses each
 * submit cluster to one display row. Inter-submit gap is typically
 * seconds, so the 1-second bucket is unambiguous in practice.
 */
public final class SexyBcrtAdapter implements AwcPlatformAdapter {

    public static final SexyBcrtAdapter INSTANCE = new SexyBcrtAdapter();

    private SexyBcrtAdapter() {}

    @Override public String platformCode() { return "SEXYBCRT"; }

    @Override
    public String historyGroupKey(Document doc) {
        String wagerCode = doc.getString("wager_code");
        String betTime = doc.getString("bet_time");
        String submitBucket = (betTime != null && betTime.length() >= 19)
                ? betTime.substring(0, 19)     // yyyy-MM-ddTHH:mm:ss
                : (betTime != null ? betTime : "");
        return "sexy|" + (wagerCode != null ? wagerCode : "") + "|" + submitBucket;
    }

    /**
     * SUN-1252 / SUN-1258 / SUN-1259 — Sexy Live tables share a single
     * AWC {@code game_code} per game type (e.g. all baccarat tables =
     * {@code MX-LIVE-001}). The actual table identity lives in the round
     * id prefix, so we parse it out here and feed it into the per-table
     * {@code vinplay.games} lookup ({@code game_code + table_tag}).
     *
     * <p>Patterns observed in production (May 2026):
     * <ul>
     *   <li>{@code Mexico-CNN-GA<digits>}  → {@code "CNN"} (Macau casino hall, e.g. C07)</li>
     *   <li>{@code Mexico-NN-GA<digits>}   → {@code "NN"}  (Mexico hall, plain 2-3 digit table)</li>
     *   <li>{@code RNN-<unix-ms>}          → {@code "C<NN>"} (legacy standalone private hall — pre-Mexico namespace)</li>
     *   <li>{@code R-<unix-ms>}            → {@code null} (lobby auto-rotation, no table info)</li>
     * </ul>
     *
     * <p>Pre-2026-05-18: this logic lived as a static method on
     * {@code AwcGameNameResolver}, applied to every AWC platform's
     * round_id. JILI / CQ9 / FACHAI / PG round_ids are completely
     * different shapes (e.g. JILI uses {@code <gameId>-<sessionTs>}),
     * so the Sexy regex either matched nothing (harmless) or — worst
     * case — accidentally matched a CQ9 string starting with R{digits}.
     * Centralising in this adapter eliminates cross-talk; non-Sexy
     * platforms get the {@code null}-returning default and skip the
     * per-table catalog hop entirely.
     */
    @Override
    public String parseTableSuffix(String roundId) {
        if (roundId == null || roundId.isEmpty()) return null;
        Matcher mex = MEXICO_PATTERN.matcher(roundId);
        if (mex.find()) return mex.group(1);
        Matcher cas = CASINO_PATTERN.matcher(roundId);
        if (cas.find()) return "C" + cas.group(1);
        return null;
    }

    private static final Pattern MEXICO_PATTERN =
            Pattern.compile("^Mexico-([A-Z]?\\d{1,3})-GA\\d+$");
    private static final Pattern CASINO_PATTERN =
            Pattern.compile("^R(\\d{1,3})-\\d+$");
}
