package com.vinplay.dal.service.seamless;

/**
 * Single source of truth for the {@code provider} field stamped on every
 * seamless-wallet bet log row (Mongo {@code log_awc_bets} / {@code log_gsc_bets}
 * and the MySQL {@code awc_transactions} ledger).
 *
 * <p>Both AWC and GSC write into separate collections but share the same
 * downstream readers (admin/agency LS Cược, agent commission rebate
 * pipeline). Stamping a uniform {@code provider} string lets consumers
 * filter or group on a single field instead of inferring source from the
 * collection name — important once a third aggregator (e.g. EVO direct,
 * BBIN) lands on the same reader path.</p>
 *
 * <p>Constants only — never instantiate.</p>
 */
public final class SeamlessProvider {
    public static final String AWC = "AWC";
    public static final String GSC = "GSC";

    public static final String FIELD = "provider";

    private SeamlessProvider() {}
}
