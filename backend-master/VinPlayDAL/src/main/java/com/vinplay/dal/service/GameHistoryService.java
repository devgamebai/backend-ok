package com.vinplay.dal.service;

import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.service.seamless.awc.AwcPlatformAdapter;
import com.vinplay.dal.service.seamless.awc.AwcPlatformRegistry;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.bson.Document;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared game history service — single source of truth for both:
 *   c=303  (player's own game history)
 *   c=9843 (agency view of player betting history)
 *
 * All game data queries are defined ONCE here. Both endpoints inherit
 * from this service to ensure data always matches.
 */
public class GameHistoryService {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("backend");

    public static final java.util.concurrent.ExecutorService FETCH_POOL =
        java.util.concurrent.Executors.newFixedThreadPool(12,
            r -> { Thread t = new Thread(r, "game-history-fetch"); t.setDaemon(true); return t; });

    private static volatile java.util.Set<String> existingColsCache = null;
    private static volatile long existingColsCachedAt = 0;
    private static final long EXISTING_COLS_TTL_MS = 5 * 60_000L;

    private static final String[] AMOUNT_KEYS = {
        "bet", "prize", "net", "fee",
        "money_before", "money_after",
        "current_money", "current_money_after",
        "win", "lose", "turnover", "amount",
        "total", "total_bet", "total_win", "total_loss"
    };

    private static String vnFormat(Object value) {
        if (value == null) return "0";
        try {
            double d;
            if (value instanceof Number) {
                d = ((Number) value).doubleValue();
            } else {
                d = Double.parseDouble(value.toString());
            }
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return nf.format((long) d);
            }
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(d);
        } catch (Throwable t) {
            return String.valueOf(value);
        }
    }

    private static void addDisplaySiblings(org.json.JSONObject row) {
        if (row == null) return;
        for (String key : AMOUNT_KEYS) {
            if (row.has(key) && !row.has(key + "_display")) {
                row.put(key + "_display", vnFormat(row.get(key)));
            }
        }
    }

    private static java.util.Set<String> getExistingCols(MongoDatabase db) {
        long now = System.currentTimeMillis();
        if (existingColsCache != null && (now - existingColsCachedAt) < EXISTING_COLS_TTL_MS) {
            return existingColsCache;
        }
        java.util.Set<String> cols = new java.util.HashSet<>(db.listCollectionNames().into(new ArrayList<>()));
        existingColsCache = cols;
        existingColsCachedAt = now;
        return cols;
    }

    private static List<String> normalizeExcludedNicknames(java.util.Collection<String> excludedNicknames) {
        List<String> out = new ArrayList<>();
        if (excludedNicknames == null || excludedNicknames.isEmpty()) return out;
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String nick : excludedNicknames) {
            if (nick == null) continue;
            String n = nick.trim();
            if (!n.isEmpty() && seen.add(n)) out.add(n);
        }
        return out;
    }

    private static void applyUserFilter(Document query, String field, List<String> nicknames, List<String> excludedNicknames) {
        boolean hasIncludes = nicknames != null && !nicknames.isEmpty();
        boolean hasExcludes = excludedNicknames != null && !excludedNicknames.isEmpty();
        if (!hasIncludes && !hasExcludes) return;

        Document filter = new Document();
        if (hasIncludes) {
            filter.put("$in", nicknames);
        }
        if (hasExcludes) {
            filter.put("$nin", excludedNicknames);
        }
        query.put(field, filter);
    }

    private static String placeholders(int size) {
        StringBuilder ph = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) ph.append(",");
            ph.append("?");
        }
        return ph.toString();
    }

    @SuppressWarnings("unchecked")
    private static void addAndCondition(Document query, Document condition) {
        if (condition == null || condition.isEmpty()) return;
        if (query.isEmpty()) {
            query.putAll(condition);
            return;
        }
        if (query.containsKey("$and")) {
            ((List<Document>) query.get("$and")).add(condition);
            return;
        }
        Document existing = new Document(query);
        query.clear();
        List<Document> andConditions = new ArrayList<>();
        andConditions.add(existing);
        andConditions.add(condition);
        query.put("$and", andConditions);
    }

    // Mongo sources: {collection, gameLabel, userField, betField, prizeField, gameId}
    //
    // SUN-777 — The module class names here are legacy placeholders that no longer
    // match the real slot product the user sees on the lobby icon. For now only
    // ChiemTinhModule has been remapped: it is the engine for "Pirate King" now.
    // Full mapping (other slots + proper fix plan): docs/SUN-777_slot-game-name-mapping.md
    //
    // Note: log_ChiemTinh may also receive bets from ThanTaiRoom via the legacy
    // logChiemtinh() method — those cross-routed bets will also display as
    // "Pirate King" until the write path in ThanTaiRoom is fixed separately.
    public static final String[][] MONGO_SOURCES = {
        {"log_KhoBau",       "Kho Báu",        "user_name", "bet_value", "prize", "20"},
        {"log_VuongQuocVin", "Vương Quốc Vin",  "user_name", "bet_value", "prize", "22"},
        {"log_SieuAnhHung",  "Siêu Anh Hùng",   "user_name", "bet_value", "prize", "18"},
        {"log_NuDiepVien",   "Nữ Điệp Viên",    "user_name", "bet_value", "prize", "21"},
        {"log_ChiemTinh",    "Pirate King",      "user_name", "bet_value", "prize", "55"}, // SUN-777: was "Chiêm Tinh"
        {"log_mini_poker",   "MiniPoker",        "user_name", "bet_value", "prize", "1"},
        {"log_cao_thap",     "Cao Thấp",         "nick_name", "bet_value", "prize", "4"},
        // SUN-LIVE-HIST: GSC third-party LIVE games (Evolution, SA, Dream, PG Soft,
        // JILI, JDB, Pragmatic, Saba, SBO, …). Written by thirdParty WithdrawProcess
        // on bet, prize field back-filled by DepositProcess on settlement.
        {"log_gsc_bets",     "Live Casino (GSC)","user_name", "bet_value", "prize", "90"},
        // SUN-AWC-LSC-VIS: AWC seamless wallet bets (SEXYBCRT live casino,
        // JILI slot, etc.). Written by AwcCallbackProcessor.writeMongoLog
        // on every bet/betNSettle/settle. Bet field is `bet_amount`, prize
        // is `win_amount`. user_name + nick_name + create_time fields all
        // match the MONGO_SOURCES contract so admin LS Cược bet-history +
        // agency view both render AWC rows alongside the existing live/slot
        // collections without per-source code change.
        {"log_awc_bets",     "Live Casino (AWC)","user_name", "bet_amount", "win_amount", "80"},
    };

    public static final String[][] THIRD_PARTY_MONGO_SOURCES = {
        {"log_game_ebet", "EBET Casino", "bet_amount", "payout"},
        {"log_game_wm", "WM Casino", "bet", "win_amount"},
        {"log_game_ag", "AG Casino", "validbet", "payout"},
        {"log_game_ibc", "IBC Sports", "validbet", "win_amount"},
        {"log_game_sbo", "SBO Sports", "validbet", "win_amount"},
        {"log_game_cmd", "CMD Sports", "bet_amount", "win_amount"},
        {"log_game_fish", "Bắn Cá", "bet_amount", "win_amount"},
        {"ebetgamerecord", "EBET Casino", "bet_amount", "payout"},
        {"aggamerecord", "AG Casino", "validbet", "payout"},
        {"log_evolution", "Evolution Casino", "bet_amount", "win_amount"},
        {"log_game_evolution", "Evolution Casino", "bet_amount", "win_amount"},
        {"log_live_casino", "Live Casino", "bet_amount", "win_amount"}
    };

    /**
     * Fetch slot/minigame history from Mongo sources — all collections run in parallel.
     * @param deadlineMs absolute epoch-ms deadline; 0 or Long.MAX_VALUE = no deadline (15s cap)
     */
    public static List<JSONObject> fetchMongoGames(List<String> nicknames, String gameFilter,
                                                     Document dateFilter, int fetchLimit) {
        return fetchMongoGames(nicknames, gameFilter, false, dateFilter, fetchLimit, Long.MAX_VALUE,
                java.util.Collections.emptyList());
    }

    static List<JSONObject> fetchMongoGames(List<String> nicknames, String gameFilter, boolean isFreeTextFilter,
                                                     Document dateFilter, int fetchLimit, long deadlineMs) {
        return fetchMongoGames(nicknames, gameFilter, isFreeTextFilter, dateFilter, fetchLimit, deadlineMs,
                java.util.Collections.emptyList());
    }

    static List<JSONObject> fetchMongoGames(List<String> nicknames, String gameFilter, boolean isFreeTextFilter,
                                                     Document dateFilter, int fetchLimit, long deadlineMs,
                                                     java.util.Collection<String> excludedNicknames) {
        MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
        List<java.util.concurrent.Future<List<JSONObject>>> futures = new ArrayList<>();
        List<String> excluded = normalizeExcludedNicknames(excludedNicknames);

        for (String[] src : MONGO_SOURCES) {
            String col = src[0];
            if (gameFilter != null && !gameFilter.isEmpty() && !"all".equals(gameFilter)) {
                if ("slot".equals(gameFilter) && (col.contains("mini_poker") || col.contains("cao_thap"))) continue;
                if ("minipoker".equals(gameFilter) && !col.contains("mini_poker")) continue;
                if ("caothap".equals(gameFilter) && !col.contains("cao_thap")) continue;
                if ("taixiu".equals(gameFilter) || "baucua".equals(gameFilter) || "sicbo".equals(gameFilter)) continue;
                // SUN-LIVE-HIST-FIX2: for live/casino keywords, only log_gsc_bets
                // and log_awc_bets are relevant in MONGO_SOURCES. Slot/minigame
                // collections (log_KhoBau, log_mini_poker, etc.) must be skipped
                // so their records do not contaminate live-only result sets.
                // SUN-AWC-LSC-VIS: include log_awc_bets so AWC live casino bets
                // surface under the live filter.
                boolean isLiveFilter = "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                        || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                        || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter)
                        || "awc".equals(gameFilter) || "sexybcrt".equals(gameFilter) || "jili".equals(gameFilter);
                if (isLiveFilter && !"log_gsc_bets".equals(col) && !"log_awc_bets".equals(col)) continue;
            }
            final String[] s = src;
            futures.add(FETCH_POOL.submit(() -> fetchOneMongoSource(db, s, nicknames, dateFilter,
                    fetchLimit, gameFilter, isFreeTextFilter, excluded)));
        }

        List<JSONObject> results = new ArrayList<>();
        // Use caller's deadline; fall back to 15s cap if no deadline was given
        long effectiveDeadline = (deadlineMs == Long.MAX_VALUE)
                ? System.currentTimeMillis() + 15_000L
                : deadlineMs;
        for (java.util.concurrent.Future<List<JSONObject>> f : futures) {
            try {
                long remaining = effectiveDeadline - System.currentTimeMillis();
                if (remaining <= 0) { logger.warn("GameHistoryService: fetchMongoGames deadline exceeded"); break; }
                results.addAll(f.get(remaining, java.util.concurrent.TimeUnit.MILLISECONDS));
            }
            catch (Exception e) { logger.warn("GameHistoryService: mongo source failed: " + e.getMessage()); }
        }
        return results;
    }

    private static List<JSONObject> fetchOneMongoSource(MongoDatabase db, String[] src,
                                                         List<String> nicknames, Document dateFilter, int fetchLimit,
                                                         String gameFilter, boolean isFreeTextFilter,
                                                         List<String> excludedNicknames) {
        List<JSONObject> results = new ArrayList<>();
        String col = src[0], gameLabel = src[1], userField = src[2], betField = src[3], prizeField = src[4], gameId = src[5];
        try {
            Document query = new Document();
            applyUserFilter(query, userField, nicknames, excludedNicknames);
            if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
            // SUN-1204: for GSC live-casino bets only, hide rounds whose settle
            // event hasn't arrived from Dream/GSC yet. Without this, an
            // in-flight win renders as a fake -bet loss in c=303 / c=9843
            // (QC: bet=25,481 prize=0 settled=false → "lose -25,481" while
            // Dream's iframe shows the player won +14,549). Filter targets
            // log_gsc_bets only — other collections don't have the `settled`
            // field and would be silently empty if we applied it broadly.
            // The reconciler scheduler (Step 3) will eventually pull the
            // missing settle from GSC's wager-history API; until then the
            // round is just hidden, not deleted.
            if ("log_awc_bets".equals(col)) {
                // SUN-1248 / Phuong: AwcCallbackProcessor writes ONE doc per
                // callback — that means a normal SEXYBCRT round produces TWO
                // docs per platformTxId (action="bet" + action="settle"),
                // and instant rounds produce one (action="betNSettle").
                // Both bet & settle docs carry bet_amount, so summing the
                // collection without filtering doubles every round's volume
                // (260 per round → 520 in agency LS Cược).
                //
                // Restrict the read to terminal-action docs (settle /
                // betNSettle) — those carry the final bet_amount AND
                // win_amount, exactly what the agency view needs. The
                // intermediate "bet" doc is audit-only.
                java.util.List<String> terminalActions = new java.util.ArrayList<>();
                terminalActions.add("settle");
                terminalActions.add("betNSettle");
                query.put("action", new Document("$in", terminalActions));
            }
            if ("log_gsc_bets".equals(col)) {
                query.put("settled", true);
                if (isFreeTextFilter && gameFilter != null && !gameFilter.trim().isEmpty()) {
                    query.put("game_name", new Document("$regex", gameFilter.trim()).append("$options", "i"));
                }
                // SUN-1205/1206/1208 — hide full-hedge wagers (valid_bet=0)
                // from LSC. The bet was effectively a no-op (refund) on
                // Dream's side, so showing a row in agency history with
                // bet=X prize=X (refund) misleads agents into expecting
                // commission. The deferred-rebate pipeline already
                // skips creating rebate rows (LSR clean), this hides
                // the LSC counterpart so both views agree.
                java.util.Set<String> skipWagers = loadSkipZeroBetWagers();
                if (!skipWagers.isEmpty()) {
                    query.put("wager_code", new Document("$nin", new ArrayList<>(skipWagers)));
                }
            }
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", query));
            pipeline.add(new Document("$sort", new Document("create_time", -1)));
            if (fetchLimit > 0) {
                int effectiveLimit = "log_gsc_bets".equals(col)
                        ? Math.min(fetchLimit * 5, 5000)
                        : fetchLimit;
                pipeline.add(new Document("$limit", effectiveLimit));
            }
            com.mongodb.client.AggregateIterable<Document> docs = db.getCollection(col)
                    .aggregate(pipeline).allowDiskUse(true);
            // SUN-1250: collapse multi-bet sub-bets for BOTH GSC and AWC.
            // GSC stores bet_value/prize; AWC stores bet_amount/win_amount.
            // Same wager_code grouping rule applies to both — Sexy Live
            // Baccarat 5/6 (AWC SEXYBCRT) emits one BET callback per
            // sub-bet just like Evolution (GSC), each with a distinct
            // event_key but a shared wager_code per round.
            Iterable<Document> outputDocs = docs;
            if ("log_gsc_bets".equals(col)) {
                outputDocs = coalesceMongoBetDocs(docs, fetchLimit, "bet_value", "prize", "fee");
            } else if ("log_awc_bets".equals(col)) {
                outputDocs = coalesceMongoBetDocs(docs, fetchLimit, "bet_amount", "win_amount", null);
            }
            for (Document doc : outputDocs) {
                JSONObject bet = new JSONObject();
                bet.put("player", doc.getString(userField));
                // SUN-LIVE-HIST: for log_gsc_bets rows, prefer the per-bet
                // game_name stamped by thirdParty WithdrawProcess (e.g.
                // "Baccarat D07", "Mega Ball") instead of the generic
                // "Live Casino (GSC)" label. Fallback to on-read resolver
                // for legacy rows that predate the writer change.
                String resolvedLabel = gameLabel;
                if ("log_gsc_bets".equals(col)) {
                    Object pcObj = doc.get("product_code");
                    String gc = doc.getString("game_code");
                    int pc = 0;
                    if (pcObj != null) {
                        try { pc = ((Number) pcObj).intValue(); }
                        catch (Throwable ignored) { /* keep pc=0 */ }
                    }
                    // SUN-1207/1210: Dream Gaming (1052) bets carry stale
                    // table attribution (vendor sends empty game_code; we
                    // fill from the player's last-launch session, which
                    // drifts when Dream auto-switches tables or the
                    // player navigates inside Dream's UI). Always route
                    // Dream rows through the resolver's category-level
                    // display path — never trust the stored game_name
                    // even for historical rows.
                    if (pc == 1052) {
                        try { resolvedLabel = GscGameNameResolver.displayName(pc, gc); }
                        catch (Throwable ignored) { /* keep generic label */ }
                    } else {
                        // Other providers stamp the specific game_code
                        // in their seamless push, so the stored label is
                        // trustworthy. Prefer it; fall back to the
                        // resolver only for legacy or "gsc_*" placeholder
                        // rows that predate the writer change.
                        String stored = doc.getString("game_name");
                        if (stored != null && !stored.isEmpty() && !stored.startsWith("gsc_")) {
                            resolvedLabel = stored;
                        } else if (pc > 0) {
                            try { resolvedLabel = GscGameNameResolver.displayName(pc, gc); }
                            catch (Throwable ignored) { /* keep generic label */ }
                        }
                    }
                } else if ("log_awc_bets".equals(col)) {
                    // SUN-AWC-LSC-VIS: render per-bet label as
                    // "<curatedGameName> (<platform>)" — match what
                    // /api/rolling shows. Rolling reads rebate_logs which
                    // resolves the canonical game name via awc_game_catalog
                    // (e.g. "Sexy Baccarat"); AWC's callback ships a vendor-
                    // internal name (e.g. "BaccaratClassic") that drifts
                    // from ops' catalog. Resolve the curated name here so
                    // both views agree. Fall back to the stored game_name
                    // if the catalog row is missing, then to platform alone.
                    String gameCode = doc.getString("game_code");
                    String plat     = doc.getString("platform");
                    String stored   = doc.getString("game_name");
                    String roundId  = doc.getString("round_id");
                    String curated  = null;
                    if (plat != null && !plat.isEmpty()) {
                        // SUN-1252/1258/1259: pass roundId so the resolver can
                        // append the parsed table tag (e.g. "Sexy Baccarat M31"
                        // for round Mexico-31-GA…). Sexy Live tables share one
                        // game_code per game type, so the table id lives in
                        // the round prefix.
                        try { curated = AwcGameNameResolver.displayName(plat, gameCode, roundId); }
                        catch (Throwable ignored) { /* keep curated=null */ }
                    }
                    String gName = (curated != null && !curated.isEmpty()) ? curated : stored;
                    boolean hasG = gName != null && !gName.isEmpty();
                    boolean hasP = plat  != null && !plat.isEmpty();
                    // QC drop "(SEXYBCRT)" suffix — catalog name already
                    // carries the table tag, so the platform stamp was
                    // redundant. LS Rolling renderer shipped the same
                    // change; both views stay in sync.
                    if (hasG)              resolvedLabel = gName;
                    else if (hasP)         resolvedLabel = plat;
                }
                bet.put("game", resolvedLabel);
                bet.put("game_id", gameId);
                long b = toLong(doc.get(betField)), p = toLong(doc.get(prizeField));
                // SUN-1275: AWC stores VND × 1000 in *_milli sister fields so
                // the agency Win/Lose column can render fractional VND
                // (e.g. 314.45) the same way the player-facing transaction
                // report does. floorDiv'd integer bet_amount/win_amount lose
                // the .45. Reader prefers the milli value when present and
                // emits a Number with 2 decimals; legacy rows without milli
                // fall through to the integer VND (no decimal change).
                if ("log_awc_bets".equals(col)) {
                    long bMilli = toLong(doc.get("bet_amount_milli"));
                    long pMilli = toLong(doc.get("win_amount_milli"));
                    long bForCalc = bMilli > 0L ? bMilli : b * 1000L;
                    long pForCalc = pMilli > 0L ? pMilli : p * 1000L;
                    bet.put("bet",   exactVnd(bForCalc));
                    bet.put("prize", exactVnd(pForCalc));
                    bet.put("net",   exactVnd(pForCalc - bForCalc));
                } else if ("log_gsc_bets".equals(col)) {
                    // SUN-1367: Dream Gaming (product 1052) ships decimal
                    // amounts (e.g. prize "63096.15") on the seamless
                    // SETTLED push. The legacy writer truncates to int via
                    // NumberLong, losing the .15. Backfill stamps
                    // bet_value_milli / prize_milli (×1000) sister fields
                    // from gsc_event_log raw payload; writer going forward
                    // does the same. When milli is present, render at
                    // 2-decimal precision matching the vendor iframe.
                    long bMilli = toLong(doc.get("bet_value_milli"));
                    long pMilli = toLong(doc.get("prize_milli"));
                    long bForCalc = bMilli > 0L ? bMilli : b * 1000L;
                    long pForCalc = pMilli > 0L ? pMilli : p * 1000L;
                    bet.put("bet",   exactVnd(bForCalc));
                    bet.put("prize", exactVnd(pForCalc));
                    bet.put("net",   exactVnd(pForCalc - bForCalc));
                } else {
                    bet.put("bet", b);
                    bet.put("prize", p);
                    bet.put("net", p - b);
                }
                bet.put("result", p > b ? "win" : (p < b ? "lose" : "draw"));
                bet.put("time", normalizeTime(doc.get("create_time")));
                bet.put("time_ms", extractTimeMs(doc.get("create_time")));
                long currentMoney = toLong(doc.get("current_money"));
                bet.put("money_before", currentMoney);
                // SUN-1248: ALSO emit `current_money` so c=303's GetGamePlayHistory
                // round-aggregation reader (which checks this field name) trusts
                // the writer-stamped value and skips its own walk-back. Without
                // this, c=303 saw current_money=0 → fell back to backward-walking
                // from users.vin (current) which drifted by 25M+ for active GSC
                // players. Same rule for both: writer-stamped value wins; the
                // walk-back only fires for legacy pre-stamping rows.
                bet.put("current_money", currentMoney);
                // SUN-1248 / Phase 2: prefer the writer-stamped
                // current_money_after when present (settle wrote it
                // exactly); fall back to derived for legacy rows that
                // predate the stamping.
                long currentMoneyAfter = toLong(doc.get("current_money_after"));
                if (currentMoneyAfter > 0L) {
                    bet.put("money_after", currentMoneyAfter);
                } else {
                    bet.put("money_after", currentMoney > 0 ? currentMoney - b + p : 0);
                }

                // Detail
                StringBuilder detail = new StringBuilder();
                if (col.equals("log_mini_poker") && doc.get("cards") != null)
                    detail.append("cards: ").append(doc.get("cards"));
                else if (col.equals("log_cao_thap")) {
                    if (doc.get("cards") != null) detail.append("card: ").append(doc.get("cards"));
                    if (doc.get("step") != null) detail.append(" | step: ").append(toLong(doc.get("step")));
                }
                if (detail.length() > 0) bet.put("detail", detail.toString());
                addDisplaySiblings(bet);
                results.add(bet);
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: skip " + col + " — " + e.getMessage());
        }
        return results;
    }

    private static List<Document> coalesceGscHistoryDocs(Iterable<Document> docs, int fetchLimit) {
        return coalesceMongoBetDocs(docs, fetchLimit, "bet_value", "prize", "fee");
    }

    /**
     * Collapse multi-bet sub-bets that share a round identity
     * ({@link #gscHistoryGroupKey}) into a single row with cumulative
     * bet/win/fee. {@code feeField} may be null when the source
     * collection does not carry a fee column (AWC's log_awc_bets).
     *
     * <p>Field-name parameters are required because GSC and AWC
     * collections store bet/win under different keys
     * ({@code bet_value}/{@code prize} vs {@code bet_amount}/{@code win_amount}).
     */
    private static List<Document> coalesceMongoBetDocs(Iterable<Document> docs, int fetchLimit,
                                                        String betField, String winField, String feeField) {
        Map<String, Document> grouped = new LinkedHashMap<>();
        for (Document doc : docs) {
            String key = gscHistoryGroupKey(doc);
            Document merged = grouped.get(key);
            if (merged == null) {
                grouped.put(key, new Document(doc));
                continue;
            }

            merged.put(betField, toLong(merged.get(betField)) + toLong(doc.get(betField)));
            merged.put(winField, toLong(merged.get(winField)) + toLong(doc.get(winField)));
            if (feeField != null) {
                merged.put(feeField, toLong(merged.get(feeField)) + toLong(doc.get(feeField)));
            }
            // SUN-1250 follow-up: AWC stores VND×1000 in sister *_milli fields
            // for fractional precision. Renderer prefers the milli value over
            // the integer field when present (see fetchOneMongoSource:377+).
            // Without summing the milli sister, the merged row keeps the
            // FIRST sub-bet's milli value and the user sees only that
            // sub-bet's amount (e.g. 50 instead of 250 for 200+50). Sum any
            // sister `<field>_milli` keys when both rows carry them.
            String betMilliKey = betField + "_milli";
            if (merged.get(betMilliKey) != null || doc.get(betMilliKey) != null) {
                merged.put(betMilliKey, toLong(merged.get(betMilliKey)) + toLong(doc.get(betMilliKey)));
            }
            String winMilliKey = winField + "_milli";
            if (merged.get(winMilliKey) != null || doc.get(winMilliKey) != null) {
                merged.put(winMilliKey, toLong(merged.get(winMilliKey)) + toLong(doc.get(winMilliKey)));
            }
            // log_awc_bets writer also stamps `turnover` / `turnover_milli`
            // (= sum of sub-bet bet_amounts at settle time). Keep them in
            // sync so any rolling/rebate join downstream sees the merged
            // total, not the first sub-bet's slice.
            if (merged.get("turnover") != null || doc.get("turnover") != null) {
                merged.put("turnover", toLong(merged.get("turnover")) + toLong(doc.get("turnover")));
            }
            if (merged.get("turnover_milli") != null || doc.get("turnover_milli") != null) {
                merged.put("turnover_milli", toLong(merged.get("turnover_milli")) + toLong(doc.get("turnover_milli")));
            }

            long mergedTime = extractTimeMs(merged.get("create_time"));
            long docTime = extractTimeMs(doc.get("create_time"));
            if (docTime > 0 && (mergedTime == 0 || docTime < mergedTime)) {
                merged.put("create_time", doc.get("create_time"));
                merged.put("time_log", doc.get("time_log"));
                // SUN-1248 / Phuong (LS Cược feedback): carry the
                // EARLIEST sub-bet's current_money (pre-debit balance) so
                // money_before reflects the round's true starting balance,
                // not the post-sub-bet-1 balance that sub-bet 2 stamped.
                if (doc.get("current_money") != null) {
                    merged.put("current_money", doc.get("current_money"));
                }
            }
            // current_money_after: keep the LATEST sub-bet's value (post-
            // settle balance). The settle write stamped it on whichever
            // sub-bet ended last; we want that one.
            long mergedAfter = toLong(merged.get("current_money_after"));
            long docAfter    = toLong(doc.get("current_money_after"));
            if (docAfter > mergedAfter) {
                merged.put("current_money_after", docAfter);
            }
            Boolean settled = merged.getBoolean("settled");
            Boolean docSettled = doc.getBoolean("settled");
            merged.put("settled", Boolean.TRUE.equals(settled) || Boolean.TRUE.equals(docSettled));
        }

        List<Document> out = new ArrayList<>(grouped.values());

        // SUN-1184: drop ALL free-spin rows (bet_value == 0), not just
        // the no-op losses SUN-1168 caught. The free-spin trigger is
        // PG Soft / Pragmatic calling Withdraw with amount=0 — the prize,
        // if any, is credited via DepositProcess.userMoneyService.reward,
        // independent of this collection. LSC-ingame and LS Rolling
        // already exclude these phantom rows, so the agent "Lịch sử cược"
        // count was the only one inflated. Paid bets (bet>0) and their
        // prizes are unaffected.
        // (WithdrawProcess.java now skips the insert at write time too;
        //  this filter remains as a safety net for in-flight rows.)
        final String betFieldRef = betField;
        out.removeIf(d -> toLong(d.get(betFieldRef)) == 0L);

        out.sort((a, b) -> Long.compare(extractTimeMs(b.get("create_time")), extractTimeMs(a.get("create_time"))));
        if (fetchLimit > 0 && out.size() > fetchLimit) {
            return new ArrayList<>(out.subList(0, fetchLimit));
        }
        return out;
    }

    private static String gscHistoryGroupKey(Document doc) {
        // SUN-1188: vendor's hand-level game_id wins. Live-casino webhooks
        // (Blackjack, Casino Hold'em, etc.) carry it in payload.<round_key>.game_id;
        // the WithdrawProcess writer extracts and stamps it. Two transactions
        // with the same vendor game_id are sub-bets of the same hand
        // (e.g. main bet + insurance) — collapse into one row to match
        // the player-facing LSC-ingame display. Slot games don't carry
        // this field so they fall through to the existing keys unchanged.
        String vendorGameId = doc.getString("vendor_game_id");
        if (vendorGameId != null && !vendorGameId.trim().isEmpty()) {
            return "vgame|" + String.valueOf(doc.get("user_name")) + "|" + vendorGameId.trim();
        }
        // AWC platform grouping — dispatched via strategy registry
        // ({@link AwcPlatformRegistry}) so per-platform divergences live
        // in dedicated adapter classes instead of inline branches.
        //
        // Default behaviour (slot-style platforms — JILI / CQ9 / FACHAI
        // / PGSOFT): one platform_tx_id per spin → key by ptxn (the
        // SUN-1250 follow-up grain from commit 62944975, correct for
        // independent spin / hedge-pair rows that already collapse via
        // the upstream vendor_game_id branch).
        //
        // SEXYBCRT (Cyan QC 2026-05-13 re-open): the platform ships one
        // ptxn per LEG within a submit and the player can issue several
        // submits per round. {@link SexyBcrtAdapter} groups legs by
        // {@code (wager_code, bet_time truncated to seconds)} so the
        // agency LS Cược row count matches the vendor's transaction
        // report. Bet-time within one submit clusters within ~10 ms;
        // inter-submit gap is seconds.
        String platformTxId = doc.getString("platform_tx_id");
        if (platformTxId != null && !platformTxId.trim().isEmpty()) {
            String platform = doc.getString("platform");
            AwcPlatformAdapter adapter = AwcPlatformRegistry.forPlatform(platform);
            String adapterKey = adapter.historyGroupKey(doc);
            if (adapterKey != null) return adapterKey;
            return "ptxn|" + platformTxId.trim();
        }
        // GSC (Evolution) keeps the wager_code merge for legacy reasons —
        // its main-bet/insurance-side pair was the original SUN-1188 ask.
        String wagerCode = doc.getString("wager_code");
        if (wagerCode != null && !wagerCode.trim().isEmpty()) {
            return "wager|" + String.valueOf(doc.get("user_name")) + "|"
                    + String.valueOf(doc.get("product_code")) + "|"
                    + String.valueOf(doc.get("game_code")) + "|"
                    + wagerCode.trim();
        }
        String eventKey = doc.getString("event_key");
        if (eventKey != null && !eventKey.trim().isEmpty()) {
            return "event|" + eventKey.trim();
        }
        return "doc|" + String.valueOf(doc.get("_id"));
    }

    /**
     * Fetch BauCua history from Mongo.
     */
    public static List<JSONObject> fetchBauCua(List<String> nicknames, Document dateFilter, int fetchLimit) {
        return fetchBauCua(nicknames, dateFilter, fetchLimit, java.util.Collections.emptyList());
    }

    public static List<JSONObject> fetchBauCua(List<String> nicknames, Document dateFilter, int fetchLimit,
                                                java.util.Collection<String> excludedNicknames) {
        List<JSONObject> results = new ArrayList<>();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            Document query = new Document();
            applyUserFilter(query, "user_name", nicknames, normalizeExcludedNicknames(excludedNicknames));
            if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", query));
            pipeline.add(new Document("$sort", new Document("create_time", -1)));
            if (fetchLimit > 0) {
                pipeline.add(new Document("$limit", fetchLimit));
            }
            com.mongodb.client.AggregateIterable<Document> docs = db.getCollection("bau_cua_transaction")
                    .aggregate(pipeline).allowDiskUse(true);
            for (Document doc : docs) {
                long bet = toLong(doc.get("bet_bau")) + toLong(doc.get("bet_cua")) + toLong(doc.get("bet_tom"))
                        + toLong(doc.get("bet_ca")) + toLong(doc.get("bet_ga")) + toLong(doc.get("bet_huou"));
                long prize = toLong(doc.get("prize_bau")) + toLong(doc.get("prize_cua")) + toLong(doc.get("prize_tom"))
                        + toLong(doc.get("prize_ca")) + toLong(doc.get("prize_ga")) + toLong(doc.get("prize_huou"));
                JSONObject b = new JSONObject();
                b.put("player", doc.getString("user_name"));
                b.put("game", "Bầu Cua");
                b.put("game_id", "3");
                b.put("bet", bet); b.put("prize", prize); b.put("net", prize - bet);
                b.put("result", prize > bet ? "win" : (prize < bet ? "lose" : "draw"));
                b.put("time", normalizeTime(doc.get("create_time")));
                b.put("time_ms", extractTimeMs(doc.get("create_time")));
                long currentMoney = toLong(doc.get("current_money"));
                b.put("money_before", currentMoney);
                b.put("money_after", currentMoney > 0 ? currentMoney - bet + prize : 0);
                addDisplaySiblings(b);
                results.add(b);
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: skip baucua — " + e.getMessage());
        }
        return results;
    }

    /**
     * Fetch TaiXiu history from Mongo (primary) + MySQL fallback.
     */
    public static List<JSONObject> fetchTaiXiu(List<String> nicknames, Document dateFilter,
                                                String fromTime, String toTime, int fetchLimit) {
        return fetchTaiXiu(nicknames, dateFilter, fromTime, toTime, fetchLimit,
                java.util.Collections.emptyList());
    }

    public static List<JSONObject> fetchTaiXiu(List<String> nicknames, Document dateFilter,
                                                String fromTime, String toTime, int fetchLimit,
                                                java.util.Collection<String> excludedNicknames) {
        List<JSONObject> mongoResults = new ArrayList<>();
        List<JSONObject> mysqlResults;
        List<String> excluded = normalizeExcludedNicknames(excludedNicknames);
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            Document query = new Document("money_type", 1);
            applyUserFilter(query, "user_name", nicknames, excluded);
            if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", query));
            pipeline.add(new Document("$sort", new Document("create_time", -1)));
            if (fetchLimit > 0) {
                pipeline.add(new Document("$limit", fetchLimit));
            }
            com.mongodb.client.AggregateIterable<Document> docs = db.getCollection("log_taixiu")
                    .aggregate(pipeline).allowDiskUse(true);
            for (Document doc : docs) {
                JSONObject bet = new JSONObject();
                bet.put("player", doc.getString("user_name"));
                bet.put("game", "Tài Xỉu");
                bet.put("game_id", "33");
                bet.put("bet", toLong(doc.get("bet_value")));
                long prize = toLong(doc.get("prize"));
                bet.put("prize", prize);
                bet.put("net", prize - toLong(doc.get("bet_value")));
                bet.put("result", prize > toLong(doc.get("bet_value")) ? "win" : (prize < toLong(doc.get("bet_value")) ? "lose" : "draw"));
                bet.put("detail", "side=" + (toLong(doc.get("bet_side")) == 0 ? "Xỉu" : "Tài"));
                bet.put("time", normalizeTime(doc.get("create_time")));
                bet.put("time_ms", extractTimeMs(doc.get("create_time")));
                bet.put("money_before", 0L);
                bet.put("money_after", 0L);
                if (doc.containsKey("current_money")) {
                    long currentMoney = toLong(doc.get("current_money"));
                    bet.put("money_before", currentMoney);
                    bet.put("money_after", currentMoney - toLong(doc.get("bet_value")) + prize);
                }
                mongoResults.add(bet);
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: skip log_taixiu mongo — " + e.getMessage());
        }
        // Mongo is the current write path; MySQL (transaction_tai_xiu_md5) holds
        // pre-migration rows only. Querying both and merging produced duplicates
        // because the dedup signature embeds time_ms, and Mongo has sub-second
        // precision while MySQL is second-level — the same bet got two
        // signatures and both slipped through. Fall back to MySQL only when
        // Mongo truly has nothing for this player (legacy-data compat).
        if (!mongoResults.isEmpty()) {
            return mongoResults;
        }
        mysqlResults = fetchMysqlTx(nicknames, "transaction_tai_xiu_md5", "Tài Xỉu", "33", fromTime, toTime, fetchLimit, excluded);
        return mergeUnique(mongoResults, mysqlResults);
    }

    /**
     * Fetch Sicbo history from Mongo (primary) + MySQL fallback.
     */
    public static List<JSONObject> fetchSicbo(List<String> nicknames, Document dateFilter,
                                               String fromTime, String toTime, int fetchLimit) {
        return fetchSicbo(nicknames, dateFilter, fromTime, toTime, fetchLimit,
                java.util.Collections.emptyList());
    }

    public static List<JSONObject> fetchSicbo(List<String> nicknames, Document dateFilter,
                                               String fromTime, String toTime, int fetchLimit,
                                               java.util.Collection<String> excludedNicknames) {
        List<JSONObject> mongoResults = new ArrayList<>();
        List<JSONObject> mysqlResults;
        List<String> excluded = normalizeExcludedNicknames(excludedNicknames);
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            Document query = new Document("money_type", 1);
            applyUserFilter(query, "user_name", nicknames, excluded);
            if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", query));
            pipeline.add(new Document("$sort", new Document("create_time", -1)));
            if (fetchLimit > 0) {
                pipeline.add(new Document("$limit", fetchLimit));
            }
            // SUN-1201: log_sicbo holds ONE doc per (user, reference_id, bet_side)
            // because SaveTransactionDetailTaiXiuSicboProcessor's upsert filter
            // includes bet_side. Group by (user, reference_id) on read so a
            // multi-side round renders as ONE row in the player's history.
            com.mongodb.client.AggregateIterable<Document> docs = db.getCollection("log_sicbo")
                    .aggregate(pipeline).allowDiskUse(true);
            java.util.Map<String, JSONObject> byRound = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.List<Long>> sidesByRound = new java.util.LinkedHashMap<>();
            for (Document doc : docs) {
                String user = doc.getString("user_name");
                long refId = toLong(doc.get("reference_id"));
                String key = user + "#" + refId;
                long docBet = toLong(doc.get("bet_value"));
                long docPrize = toLong(doc.get("prize"));
                long sideId = toLong(doc.get("bet_side"));

                JSONObject bet = byRound.get(key);
                if (bet == null) {
                    bet = new JSONObject();
                    bet.put("player", user);
                    bet.put("game", "Sicbo");
                    bet.put("game_id", "30");
                    bet.put("bet", docBet);
                    bet.put("prize", docPrize);
                    bet.put("time", normalizeTime(doc.get("create_time")));
                    bet.put("time_ms", extractTimeMs(doc.get("create_time")));
                    if (doc.containsKey("current_money")) {
                        long currentMoney = toLong(doc.get("current_money"));
                        bet.put("money_before", currentMoney);
                        bet.put("money_after", currentMoney - docBet + docPrize);
                    } else {
                        bet.put("money_before", 0L);
                        bet.put("money_after", 0L);
                    }
                    byRound.put(key, bet);
                    java.util.List<Long> sides = new java.util.ArrayList<>();
                    sides.add(sideId);
                    sidesByRound.put(key, sides);
                } else {
                    bet.put("bet", bet.optLong("bet") + docBet);
                    bet.put("prize", bet.optLong("prize") + docPrize);
                    long earlier = bet.optLong("time_ms");
                    long ts = extractTimeMs(doc.get("create_time"));
                    if (ts > 0 && (earlier == 0 || ts < earlier)) {
                        bet.put("time", normalizeTime(doc.get("create_time")));
                        bet.put("time_ms", ts);
                    }
                    sidesByRound.get(key).add(sideId);
                }
            }
            // Finalize: derived columns (net/result/detail/money_after) computed
            // once on the aggregated row so they reflect the full round, not
            // the first sub-bet. SUN-1296: previously money_after was set in
            // the per-doc branch above using only the first sub-bet's amounts,
            // so a multi-side losing round under-reported the wallet debit
            // (e.g. bet 1M on side A + 2M on side B → money_after only
            // subtracted 1M). Player saw "balance unchanged" on losing rows
            // because the displayed delta didn't match the actual debit.
            // Recomputing here from the aggregated totals fixes the display
            // without touching the writer (per-doc current_money is correct).
            for (java.util.Map.Entry<String, JSONObject> e : byRound.entrySet()) {
                JSONObject bet = e.getValue();
                long totalBet = bet.optLong("bet");
                long totalPrize = bet.optLong("prize");
                bet.put("net", totalPrize - totalBet);
                bet.put("result", totalPrize > totalBet ? "win" : (totalPrize < totalBet ? "lose" : "draw"));
                // SUN-1296: derive money_after from the round-level totals.
                // money_before stays at the FIRST sub-bet's pre-debit balance
                // (correct — that's the round's true starting balance for this
                // user). Clamp to 0 so a malformed row never displays negative
                // (mirrors BalanceGuard.clamp).
                long mb = bet.optLong("money_before");
                if (mb > 0L) {
                    bet.put("money_after", Math.max(0L, mb - totalBet + totalPrize));
                }
                StringBuilder sb = new StringBuilder();
                for (Long sid : sidesByRound.get(e.getKey())) {
                    String name = sicboSideName(sid.intValue());
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name != null ? name : ("ô " + sid));
                }
                bet.put("detail", sb.toString());
                mongoResults.add(bet);
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: skip log_sicbo mongo — " + e.getMessage());
        }
        // Same Mongo+MySQL dedup mismatch as TaiXiu above — fall back to MySQL
        // only when Mongo has no rows for this player.
        //
        // SUN-1201: SicBo-specific aggregator. SicBo writes one row per (user,
        // reference_id, bet_side) into transaction_tai_xiu_sicbo (a "5 nút" +
        // "Tài" round produces 2 rows). Grouping on the JDBC side gives the
        // player ONE row per round with the total bet/prize and a detail
        // string listing each side. fetchMysqlTx is kept for TaiXiu where the
        // 1-row-per-round invariant already holds.
        if (!mongoResults.isEmpty()) {
            return mongoResults;
        }
        mysqlResults = fetchMysqlSicboGrouped(nicknames, fromTime, toTime, fetchLimit, excluded);
        return mergeUnique(mongoResults, mysqlResults);
    }

    /**
     * SUN-1201: MySQL fallback for SicBo grouped by reference_id so a
     * multi-side round renders as ONE row in c=303 / LSC-ingame.
     *
     * <p>SicBo bet_side values are {@code PotSicbo} ids (1–52, e.g. 48=TAI,
     * 2=POINT_5). The legacy {@link #fetchMysqlTx} hard-codes
     * "side=Tài/Xỉu" using bet_side==0 — fine for Tài Xỉu (sides 0/1) but
     * always reports "Tài" for SicBo (sides ≥ 1). This method emits a
     * GROUP_CONCAT of the readable side names instead.
     */
    private static List<JSONObject> fetchMysqlSicboGrouped(List<String> nicknames,
                                                            String from, String to, int limit) {
        return fetchMysqlSicboGrouped(nicknames, from, to, limit, java.util.Collections.emptyList());
    }

    private static List<JSONObject> fetchMysqlSicboGrouped(List<String> nicknames,
                                                            String from, String to, int limit,
                                                            java.util.Collection<String> excludedNicknames) {
        List<JSONObject> results = new ArrayList<>();
        if (nicknames == null || nicknames.isEmpty()) return results;
        List<String> excluded = normalizeExcludedNicknames(excludedNicknames);

        StringBuilder dateCond = new StringBuilder();
        List<String> dateParams = new ArrayList<>();
        if (from != null && !from.isEmpty()) { dateCond.append(" AND timestamp >= ?"); dateParams.add(from + " 00:00:00"); }
        if (to != null && !to.isEmpty()) { dateCond.append(" AND timestamp <= ?"); dateParams.add(to + " 23:59:59"); }
        String excludeCond = excluded.isEmpty() ? "" : " AND user_name NOT IN (" + placeholders(excluded.size()) + ")";

        String sql =
            "SELECT user_name, reference_id, " +
            "       SUM(bet_value) AS total_bet, " +
            "       SUM(total_prize) AS total_prize, " +
            "       MAX(timestamp) AS last_ts, " +
            "       GROUP_CONCAT(bet_side ORDER BY bet_side) AS sides " +
            "FROM vinplay_minigame.transaction_tai_xiu_sicbo " +
            "WHERE user_name IN (" + placeholders(nicknames.size()) + ") AND money_type=1" + excludeCond + dateCond +
            " GROUP BY user_name, reference_id " +
            " ORDER BY last_ts DESC" + (limit > 0 ? " LIMIT ?" : "");

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String n : nicknames) ps.setString(idx++, n);
            for (String n : excluded) ps.setString(idx++, n);
            for (String d : dateParams) ps.setString(idx++, d);
            if (limit > 0) ps.setInt(idx, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject b = new JSONObject();
                    b.put("player", rs.getString("user_name"));
                    b.put("game", "Sicbo");
                    b.put("game_id", "30");
                    long bet = rs.getLong("total_bet");
                    long prize = rs.getLong("total_prize");
                    b.put("bet", bet);
                    b.put("prize", prize);
                    b.put("net", prize - bet);
                    b.put("result", prize > bet ? "win" : (prize < bet ? "lose" : "draw"));
                    b.put("detail", formatSicboSides(rs.getString("sides")));
                    String ts = rs.getString("last_ts");
                    b.put("time", ts);
                    b.put("time_ms", rs.getTimestamp("last_ts") != null ? rs.getTimestamp("last_ts").getTime() : 0L);
                    b.put("money_before", 0L);
                    b.put("money_after", 0L);
                    addDisplaySiblings(b);
                    results.add(b);
                }
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: mysqlSicboGrouped error: " + e.getMessage());
        }
        return results;
    }

    /** Render a CSV of PotSicbo bet_side ids as a comma-joined name list.
     *  Falls back to the raw csv if the enum lookup fails (defensive against
     *  data that pre-dates a given enum). */
    private static String formatSicboSides(String csv) {
        if (csv == null || csv.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String part : csv.split(",")) {
            try {
                int id = Integer.parseInt(part.trim());
                String name = sicboSideName(id);
                if (out.length() > 0) out.append(", ");
                out.append(name != null ? name : part.trim());
            } catch (NumberFormatException nfe) {
                if (out.length() > 0) out.append(", ");
                out.append(part.trim());
            }
        }
        return out.toString();
    }

    /** Minimal local mapping — the game-server PotSicbo enum lives in the
     *  Minigame module which the DAL doesn't depend on. Mirrors PotSicbo. */
    private static String sicboSideName(int id) {
        switch (id) {
            case 1: return "POINT_4";   case 2: return "POINT_5";
            case 3: return "POINT_6";   case 4: return "POINT_7";
            case 5: return "POINT_8";   case 6: return "POINT_9";
            case 7: return "POINT_10";  case 8: return "POINT_11";
            case 9: return "POINT_12";  case 10: return "POINT_13";
            case 11: return "POINT_14"; case 12: return "POINT_15";
            case 13: return "POINT_16"; case 14: return "POINT_17";
            case 15: return "ONE_DICE_1"; case 16: return "ONE_DICE_2";
            case 17: return "ONE_DICE_3"; case 18: return "ONE_DICE_4";
            case 19: return "ONE_DICE_5"; case 20: return "ONE_DICE_6";
            case 48: return "TAI";  case 49: return "XIU";
            case 50: return "CHAN"; case 51: return "LE";
            case 52: return "ANY_TRIPLE_DICES";
            default:
                if (id >= 21 && id <= 41) return "DOUBLE_DICES_" + id;
                if (id >= 42 && id <= 47) return "TRIPLE_DICES_" + (id - 41);
                return null;
        }
    }

    /**
     * MySQL fallback for TaiXiu/Sicbo when Mongo has no data.
     */
    private static List<JSONObject> fetchMysqlTx(List<String> nicknames, String table,
                                                   String gameLabel, String gameId,
                                                   String from, String to, int limit) {
        return fetchMysqlTx(nicknames, table, gameLabel, gameId, from, to, limit,
                java.util.Collections.emptyList());
    }

    private static List<JSONObject> fetchMysqlTx(List<String> nicknames, String table,
                                                   String gameLabel, String gameId,
                                                   String from, String to, int limit,
                                                   java.util.Collection<String> excludedNicknames) {
        List<JSONObject> results = new ArrayList<>();
        if (nicknames == null || nicknames.isEmpty()) return results; // skip legacy MySQL query for wildcard
        List<String> excluded = normalizeExcludedNicknames(excludedNicknames);
        StringBuilder dateCond = new StringBuilder();
        List<String> dateParams = new ArrayList<>();
        if (from != null && !from.isEmpty()) { dateCond.append(" AND timestamp >= ?"); dateParams.add(from + " 00:00:00"); }
        if (to != null && !to.isEmpty()) { dateCond.append(" AND timestamp <= ?"); dateParams.add(to + " 23:59:59"); }
        String excludeCond = excluded.isEmpty() ? "" : " AND user_name NOT IN (" + placeholders(excluded.size()) + ")";

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT user_name, bet_value, total_prize, bet_side, timestamp FROM vinplay_minigame." + table +
                     " WHERE user_name IN (" + placeholders(nicknames.size()) + ") AND money_type=1" + excludeCond + dateCond +
                     " ORDER BY timestamp DESC" + (limit > 0 ? " LIMIT ?" : ""))) {
            int idx = 1;
            for (String n : nicknames) ps.setString(idx++, n);
            for (String n : excluded) ps.setString(idx++, n);
            for (String d : dateParams) ps.setString(idx++, d);
            if (limit > 0) {
                ps.setInt(idx, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject b = new JSONObject();
                    b.put("player", rs.getString("user_name"));
                    b.put("game", gameLabel);
                    b.put("game_id", gameId);
                    b.put("bet", rs.getLong("bet_value"));
                    long prize = rs.getLong("total_prize");
                    b.put("prize", prize);
                    b.put("net", prize - rs.getLong("bet_value"));
                    b.put("result", prize > rs.getLong("bet_value") ? "win" : (prize < rs.getLong("bet_value") ? "lose" : "draw"));
                    b.put("detail", "side=" + (rs.getInt("bet_side") == 0 ? "Xỉu" : "Tài"));
                    b.put("time", rs.getString("timestamp"));
                    b.put("time_ms", rs.getTimestamp("timestamp") != null ? rs.getTimestamp("timestamp").getTime() : 0L);
                    b.put("money_before", 0L);
                    b.put("money_after", 0L);
                    addDisplaySiblings(b);
                    results.add(b);
                }
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: mysqlTx " + table + " error: " + e.getMessage());
        }

        return results;
    }

    /**
     * Fetch ALL game history for a list of nicknames.
     * Both c=303 and c=9843 call this.
     */
    public static List<JSONObject> fetchAll(List<String> nicknames, String gameFilter,
                                             String fromTime, String toTime, int fetchLimit) {
        return fetchAll(nicknames, gameFilter, fromTime, toTime, fetchLimit, false, 20_000L);
    }

    public static List<JSONObject> fetchAll(List<String> nicknames, String gameFilter,
                                             String fromTime, String toTime, int fetchLimit,
                                             boolean skipBalanceSupplement) {
        return fetchAll(nicknames, gameFilter, fromTime, toTime, fetchLimit, skipBalanceSupplement, 20_000L);
    }

    /**
     * Master fetchAll with configurable per-source timeout.
     * @param sourceTimeoutMs global wall-clock deadline in ms (c=303 uses 5 000, admin uses 20 000)
     */
    public static List<JSONObject> fetchAll(List<String> nicknames, String gameFilter,
                                             String fromTime, String toTime, int fetchLimit,
                                             boolean skipBalanceSupplement, long sourceTimeoutMs) {
        return fetchAll(nicknames, gameFilter, fromTime, toTime, fetchLimit, skipBalanceSupplement,
                sourceTimeoutMs, java.util.Collections.emptyList());
    }

    /**
     * Master fetchAll with source-level nickname exclusion. This is used by
     * admin-wide betting history to hide bot users before each source applies
     * LIMIT, so real-player rows are not lost from the fetch window.
     */
    public static List<JSONObject> fetchAll(List<String> nicknames, String gameFilter,
                                             String fromTime, String toTime, int fetchLimit,
                                             boolean skipBalanceSupplement, long sourceTimeoutMs,
                                             java.util.Collection<String> excludedNicknames) {
        Document dateFilter = new Document();
        if (fromTime != null && !fromTime.isEmpty()) {
            try { dateFilter.put("$gte", OUT_FMT.get().parse(fromTime + " 00:00:00")); }
            catch (Exception ignored) {}
        }
        if (toTime != null && !toTime.isEmpty()) {
            try { dateFilter.put("$lte", OUT_FMT.get().parse(toTime + " 23:59:59")); }
            catch (Exception ignored) {}
        }

        final Document df = dateFilter;
        final String ft = fromTime, tt = toTime;
        final int fl = fetchLimit;
        final List<String> excluded = normalizeExcludedNicknames(excludedNicknames);

        // SUN-LIVE-HIST-FIX: detect free-text game name filter (e.g. "Câu Cá Sông Bằng").
        // The category keywords below are all lowercase enum values sent by the frontend
        // dropdown. Any other string is treated as a free-text game name search:
        //   → fetch ALL sources (ignoring category guards),
        //   → then filter the merged result in-memory by game.contains(freeText).
        // This fixes the issue where Live/GSC games (log_gsc_bets via fetchMongoGames
        // and 3rd-party live collections via fetchThirdPartyLiveGames) returned 0 rows
        // when searched by display name because no source was ever submitted.
        final boolean isCategoryKeyword = (gameFilter == null || gameFilter.isEmpty()
                || "all".equals(gameFilter)
                || "slot".equals(gameFilter) || "minipoker".equals(gameFilter) || "caothap".equals(gameFilter)
                || "baucua".equals(gameFilter) || "taixiu".equals(gameFilter) || "sicbo".equals(gameFilter)
                || "banca".equals(gameFilter) || "fish".equals(gameFilter)
                || "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter));
        final boolean isFreeTextFilter = !isCategoryKeyword; // e.g. "câu cá", "baccarat d07"

        // When free-text → query every source; category filtering happens post-fetch.
        // When category keyword → original selective behaviour is preserved.
        // SUN-LIVE-HIST-FIX2: log_gsc_bets lives in MONGO_SOURCES (fetched by fetchMongoGames).
        // When gameFilter is a live/casino keyword, fetchMongoGames MUST run so log_gsc_bets
        // is queried. fetchMongoGames will skip all non-GSC collections for those keywords.
        boolean wantSlot = isFreeTextFilter || gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter)
                || "slot".equals(gameFilter) || "minipoker".equals(gameFilter) || "caothap".equals(gameFilter)
                || "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter);
        boolean wantBauCua = isFreeTextFilter || gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "baucua".equals(gameFilter);
        boolean wantTaiXiu = isFreeTextFilter || gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "taixiu".equals(gameFilter);
        boolean wantSicbo  = isFreeTextFilter || gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "sicbo".equals(gameFilter);
        boolean wantBanCa  = isFreeTextFilter || gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter)
                || "banca".equals(gameFilter) || "fish".equals(gameFilter);
        boolean wantLive   = isFreeTextFilter || gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter)
                || "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter);

        // For fetchMongoGames: pass the gameFilter down so that it can be applied as a regex
        // in collections with very large variety like log_gsc_bets, instead of relying solely
        // on post-fetch in-memory filtering which might miss matching records if they are deep.
        final String mongoGameFilter = gameFilter;

        // Compute absolute deadline BEFORE submitting tasks so lambdas can capture it
        final long deadline = System.currentTimeMillis() + sourceTimeoutMs;

        // Submit all sources to the thread pool concurrently
        List<java.util.concurrent.Future<List<JSONObject>>> futures = new ArrayList<>();
        if (wantSlot)   futures.add(FETCH_POOL.submit(() -> fetchMongoGames(nicknames, mongoGameFilter, isFreeTextFilter, df, fl, deadline, excluded)));
        if (wantBauCua) futures.add(FETCH_POOL.submit(() -> fetchBauCua(nicknames, df, fl, excluded)));
        if (wantTaiXiu) futures.add(FETCH_POOL.submit(() -> fetchTaiXiu(nicknames, df, ft, tt, fl, excluded)));
        if (wantSicbo)  futures.add(FETCH_POOL.submit(() -> fetchSicbo(nicknames, df, ft, tt, fl, excluded)));
        if (wantBanCa)  futures.add(FETCH_POOL.submit(() -> fetchBanCa(nicknames, ft, tt, fl, excluded)));
        if (wantLive)   futures.add(FETCH_POOL.submit(() -> fetchThirdPartyLiveGames(nicknames, isFreeTextFilter ? null : gameFilter, ft, tt, fl, deadline, excluded)));

        List<JSONObject> all = new ArrayList<>();
        for (java.util.concurrent.Future<List<JSONObject>> f : futures) {
            try {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) { logger.warn("fetchAll: global deadline exceeded, dropping remaining sources"); break; }
                all.addAll(f.get(remaining, java.util.concurrent.TimeUnit.MILLISECONDS));
            }
            catch (java.util.concurrent.TimeoutException e) { logger.warn("fetchAll: source timed out (limit=" + sourceTimeoutMs + "ms)"); }
            catch (Exception e) { logger.warn("fetchAll: source failed: " + e.getMessage()); }
        }

        // SUN-LIVE-HIST-FIX: post-fetch in-memory filter for free-text game name search.
        // Applied AFTER merging all sources so it works across log_gsc_bets, 3rd-party
        // live collections, slot/minigame sources — no per-collection logic needed.
        if (isFreeTextFilter) {
            final String lowerFilter = gameFilter.toLowerCase();
            all.removeIf(b -> !b.optString("game", "").toLowerCase().contains(lowerFilter));
            logger.info("fetchAll: free-text filter=\"" + gameFilter + "\" matched=" + all.size() + " records");
        }

        if (!skipBalanceSupplement) {
            supplementMissingBalances(all, nicknames, fromTime, toTime);
        }

        // Sort by time descending
        all.sort((a, b) -> Long.compare(b.optLong("time_ms", 0), a.optLong("time_ms", 0)));

        return all;
    }

    /**
     * BanCa fish shooting — reads from cgame.bc_log (MySQL).
     * Reason=4 = KillFish (reward), Extra has fish name + reward.
     * Maps cgame.users.nickname → vinplay nickname.
     */
    private static List<JSONObject> fetchBanCa(List<String> nicknames, String fromTime, String toTime, int fetchLimit) {
        return fetchBanCa(nicknames, fromTime, toTime, fetchLimit, java.util.Collections.emptyList());
    }

    private static List<JSONObject> fetchBanCa(List<String> nicknames, String fromTime, String toTime, int fetchLimit,
                                                java.util.Collection<String> excludedNicknames) {
        List<JSONObject> results = new ArrayList<>();
        if (nicknames == null || nicknames.isEmpty()) return results;
        List<String> excluded = normalizeExcludedNicknames(excludedNicknames);

        StringBuilder inPlaceholders = new StringBuilder();
        for (int i = 0; i < nicknames.size(); i++) {
            if (i > 0) inPlaceholders.append(",");
            inPlaceholders.append("?");
        }

        // SUN-13xx: switch bet-history grain from per-kill (Reason=4 KillFish)
        // to per-play-episode (Reason=23 SessionEnd) so c=303 / c=9843 render
        // one row per session bracket, matching the money_transaction grain
        // produced by the BanCa idle/leave settle.
        String sql = "SELECT bl.ChangeCash, bl.Cash, bl.Extra, bl.Time, cu.nickname "
                + "FROM cgame.bc_log bl "
                + "JOIN cgame.users cu ON cu.user_id = bl.UserId "
                + "WHERE cu.nickname IN (" + inPlaceholders + ") AND bl.Reason = 23 ";
        if (!excluded.isEmpty()) sql += "AND cu.nickname NOT IN (" + placeholders(excluded.size()) + ") ";
        if (fromTime != null && !fromTime.isEmpty()) sql += "AND bl.Time >= ? ";
        if (toTime != null && !toTime.isEmpty()) sql += "AND bl.Time <= ? ";
        sql += "ORDER BY bl.Time DESC LIMIT ?";

        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String nick : nicknames) ps.setString(idx++, nick);
            for (String nick : excluded) ps.setString(idx++, nick);
            if (fromTime != null && !fromTime.isEmpty()) ps.setString(idx++, fromTime + " 00:00:00");
            if (toTime != null && !toTime.isEmpty()) ps.setString(idx++, toTime + " 23:59:59");
            ps.setInt(idx, fetchLimit > 0 ? fetchLimit : 50);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject bet = new JSONObject();
                    bet.put("player", rs.getString("nickname"));
                    bet.put("game", "Bắn Cá");
                    bet.put("game_id", "banca");
                    // ChangeCash on Reason=23 SessionEnd is the bracket's NET
                    // profit: positive = player ended the episode up, negative
                    // = ended down. Render as bet/prize/net so the agency UI
                    // matches every other game's row shape:
                    //   net > 0 → win  (bet=0, prize=net)
                    //   net < 0 → lose (bet=|net|, prize=0)
                    //   net = 0 → draw (everything zero)
                    long net = rs.getLong("ChangeCash");
                    long betAmount = net < 0 ? -net : 0L;
                    long prize     = net > 0 ?  net : 0L;
                    String result  = net > 0 ? "win" : (net < 0 ? "lose" : "draw");
                    bet.put("bet",   betAmount);
                    bet.put("prize", prize);
                    bet.put("net",   net);
                    bet.put("result", result);
                    long cash = rs.getLong("Cash"); // post-settle balance
                    bet.put("money_after",  cash);
                    bet.put("money_before", Math.max(0L, cash - net));
                    // Preserve the sessionId|closeReason pair for forensic
                    // joining back to the money_transaction row, but expose
                    // it under a distinct key so `result` keeps the same
                    // win/lose/draw vocabulary the FE uses everywhere else.
                    String extra = rs.getString("Extra");
                    if (extra != null && !extra.isEmpty()) {
                        bet.put("session", extra);
                    }
                    java.sql.Timestamp ts = rs.getTimestamp("Time");
                    if (ts != null) {
                        bet.put("time", OUT_FMT.get().format(ts));
                        bet.put("time_ms", ts.getTime());
                    }
                    addDisplaySiblings(bet);
                    results.add(bet);
                }
            }
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger("dal").warn("fetchBanCa error: " + e.getMessage());
        }
        return results;
    }

    private static List<JSONObject> fetchThirdPartyLiveGames(List<String> nicknames, String gameFilter, String fromTime, String toTime, int fetchLimit) {
        return fetchThirdPartyLiveGames(nicknames, gameFilter, fromTime, toTime, fetchLimit, Long.MAX_VALUE);
    }

    private static List<JSONObject> fetchThirdPartyLiveGames(List<String> nicknames, String gameFilter, String fromTime, String toTime, int fetchLimit, long deadlineMs) {
        return fetchThirdPartyLiveGames(nicknames, gameFilter, fromTime, toTime, fetchLimit, deadlineMs,
                java.util.Collections.emptyList());
    }

    private static List<JSONObject> fetchThirdPartyLiveGames(List<String> nicknames, String gameFilter,
                                                              String fromTime, String toTime, int fetchLimit,
                                                              long deadlineMs,
                                                              java.util.Collection<String> excludedNicknames) {
        List<JSONObject> results = new ArrayList<>();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            java.util.Set<String> existingCols = getExistingCols(db);
            List<java.util.concurrent.Future<List<JSONObject>>> futures = new ArrayList<>();
            List<String> excluded = normalizeExcludedNicknames(excludedNicknames);

            for (String[] src : THIRD_PARTY_MONGO_SOURCES) {
                String colName = src[0];
                String providerLabel = src[1];
                String betField = src[2];
                String prizeField = src[3];
                
                if (gameFilter != null && !gameFilter.isEmpty() && !"all".equalsIgnoreCase(gameFilter) && !"live".equalsIgnoreCase(gameFilter) && !"casino".equalsIgnoreCase(gameFilter)) {
                    if (!providerLabel.toLowerCase().contains(gameFilter.toLowerCase())) {
                        continue;
                    }
                }
                
                // Skip fish here to avoid duplication with fetchBanCa
                if (colName.equals("log_game_fish")) {
                    continue;
                }

                if (!existingCols.contains(colName)) continue;
                
                futures.add(FETCH_POOL.submit(() -> {
                    List<JSONObject> sourceResults = new ArrayList<>();
                    try {
                        com.mongodb.client.MongoCollection<Document> col = db.getCollection(colName);
                        
                        // Build filter for multiple possible user fields
                        Document query = new Document();
                        if (nicknames != null && !nicknames.isEmpty()) {
                            Document userFilter = new Document("$in", nicknames);
                            List<Document> orUserConds = new ArrayList<>();
                            orUserConds.add(new Document("username", userFilter));
                            orUserConds.add(new Document("nick_name", userFilter));
                            orUserConds.add(new Document("nickName", userFilter));
                            query.put("$or", orUserConds);
                        }

                        // Build date filter (trying multiple fields with both String and Date types)
                        java.util.Date fromDate = null;
                        java.util.Date toDate = null;
                        if (fromTime != null && !fromTime.isEmpty()) {
                            try { fromDate = OUT_FMT.get().parse(fromTime + " 00:00:00"); } catch (Exception ignored) {}
                        }
                        if (toTime != null && !toTime.isEmpty()) {
                            try { toDate = OUT_FMT.get().parse(toTime + " 23:59:59"); } catch (Exception ignored) {}
                        }
                        
                        Document timeCondStr = new Document();
                        if (fromTime != null && !fromTime.isEmpty()) timeCondStr.put("$gte", fromTime);
                        if (toTime != null && !toTime.isEmpty()) timeCondStr.put("$lte", toTime + " 23:59:59");
                        
                        Document timeCondDate = new Document();
                        if (fromDate != null) timeCondDate.put("$gte", fromDate);
                        if (toDate != null) timeCondDate.put("$lte", toDate);

                        if (!timeCondStr.isEmpty() || !timeCondDate.isEmpty()) {
                            List<Document> orTimeConds = new ArrayList<>();
                            if (!timeCondStr.isEmpty()) {
                                orTimeConds.add(new Document("time_log", timeCondStr));
                                orTimeConds.add(new Document("bettime", timeCondStr));
                                orTimeConds.add(new Document("createtime", timeCondStr));
                            }
                            if (!timeCondDate.isEmpty()) {
                                orTimeConds.add(new Document("create_time", timeCondDate));
                                orTimeConds.add(new Document("createtime", timeCondDate));
                            }
                            
                            if (query.containsKey("$or")) {
                                List<Document> rootAnd = new ArrayList<>();
                                rootAnd.add(new Document("$or", query.get("$or")));
                                rootAnd.add(new Document("$or", orTimeConds));
                                query.remove("$or");
                                query.put("$and", rootAnd);
                            } else {
                                query.put("$or", orTimeConds);
                            }
                        }

                        if (!excluded.isEmpty()) {
                            addAndCondition(query, new Document("username", new Document("$nin", excluded)));
                            addAndCondition(query, new Document("nick_name", new Document("$nin", excluded)));
                            addAndCondition(query, new Document("nickName", new Document("$nin", excluded)));
                        }

                        com.mongodb.client.FindIterable<Document> docs = col.find(query).limit(fetchLimit > 0 ? fetchLimit : 50);
                        for (Document doc : docs) {
                            JSONObject bet = new JSONObject();
                            
                            // User mapping
                            String p = doc.getString("username");
                            if (p == null) p = doc.getString("nick_name");
                            if (p == null) p = doc.getString("nickName");
                            
                            // TransID mapping (avoid ObjectIDs as per Major 2)
                            String transId = "";
                            if (doc.get("trans_id") != null) transId = doc.get("trans_id").toString();
                            else if (doc.get("billno") != null) transId = doc.get("billno").toString();
                            else if (doc.get("bethistoryid") != null) transId = doc.get("bethistoryid").toString();
                            else if (doc.get("ref_no") != null) transId = doc.get("ref_no").toString();

                            // Bet / Prize / Net (using specific mapping as per Major 1)
                            long betValue = toLong(doc.get(betField));
                            long prizeValue = toLong(doc.get(prizeField));

                            // Time mapping
                            String time = doc.getString("time_log");
                            if (time == null) time = doc.getString("createtime");
                            if (time == null) time = doc.getString("bettime");
                            if (time == null && doc.get("create_time") != null) {
                                Object ct = doc.get("create_time");
                                time = (ct instanceof java.util.Date) ? OUT_FMT.get().format((java.util.Date) ct) : ct.toString();
                            }
                            long timeMs = 0L;
                            if (time != null && !time.isEmpty()) {
                                try {
                                    timeMs = OUT_FMT.get().parse(time).getTime();
                                } catch (Exception ignored) {}
                            }

                            bet.put("player", p != null ? p : "Unknown");
                            if (bet.getString("player").equals("Unknown")) continue; // Drop invalid
                            
                            bet.put("game", providerLabel);
                            bet.put("game_id", "live_3rd");
                            bet.put("trans_id", transId);
                            bet.put("bet", betValue);
                            bet.put("prize", prizeValue);
                            bet.put("net", prizeValue - betValue);
                            bet.put("result", prizeValue > betValue ? "win" : (prizeValue < betValue ? "lose" : "draw"));
                            bet.put("time", time != null ? time : "");
                            bet.put("time_ms", timeMs);

                            addDisplaySiblings(bet);
                            sourceResults.add(bet);
                        }
                    } catch (Exception e) {
                        logger.warn("GameHistoryService: skip 3rd party " + colName + " - " + e.getMessage());
                    }
                    return sourceResults;
                }));
            }

            // Use caller's deadline; fall back to 8s cap if no deadline given (preserves original behavior)
            long endTime = (deadlineMs == Long.MAX_VALUE)
                    ? System.currentTimeMillis() + 8_000L
                    : deadlineMs;
            java.util.Set<String> seenTransIds = new java.util.HashSet<>();
            for (java.util.concurrent.Future<List<JSONObject>> f : futures) {
                try {
                    long waitTime = endTime - System.currentTimeMillis();
                    if (waitTime <= 0) break;
                    List<JSONObject> partial = f.get(waitTime, java.util.concurrent.TimeUnit.MILLISECONDS);
                    for (JSONObject bet : partial) {
                        String tid = bet.optString("trans_id", "");
                        if (tid.isEmpty() || seenTransIds.add(tid)) {
                            results.add(bet);
                        }
                    }
                }
                catch (Exception e) { logger.warn("GameHistoryService: 3rd party source failed or timed out: " + e.getMessage()); }
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: fetchThirdPartyLiveGames error: " + e.getMessage());
        }
        return results;
    }

    private static void supplementMissingBalances(List<JSONObject> all, List<String> nicknames, String from, String to) {
        List<JSONObject> missing = new ArrayList<>();
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        for (JSONObject b : all) {
            if (b.optLong("money_before", 0L) == 0L && b.optLong("bet", 0L) > 0L) {
                missing.add(b);
                long t = b.optLong("time_ms", 0L);
                if (t > 0) {
                    if (t < minTime) minTime = t;
                    if (t > maxTime) maxTime = t;
                }
            }
        }
        
        if (missing.isEmpty() || minTime == Long.MAX_VALUE) return;

        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            Document query = new Document();
            if (nicknames != null && !nicknames.isEmpty()) {
                query.put("nick_name", nicknames.size() == 1 ? nicknames.get(0) : new Document("$in", nicknames));
            }
            
            // Limit to games that might have missing balances
            query.put("action_name", new Document("$in", java.util.Arrays.asList(
                "TaiXiu", "TaiXiuMD5", "Sicbo", "BauCua", "KhoBau", "NuDiepVien", "SieuAnhHung", 
                "VuongQuocVin", "ChiemTinh", "ThanDen", "RollRoye", "Bikini", "Benley", 
                "Audition", "TamHung", "Spartan", "RangeRover", "MiniPoker", "CaoThap"
            )));

            // Focus on deduction records
            query.put("money_exchange", new Document("$lt", 0L));

            // Expand the bounds slightly to cover pre-resolving deductions (e.g. TaiXiu 50s betting window)
            String timeStart = OUT_FMT.get().format(new java.util.Date(minTime - 70000L));
            String timeEnd   = OUT_FMT.get().format(new java.util.Date(maxTime + 5000L));
            query.put("trans_time", new Document("$gte", timeStart).append("$lte", timeEnd));

            // Execute ONE fast bounded query without limit or sorting overhead
            com.mongodb.client.FindIterable<Document> docs = db.getCollection("log_money_user_vin")
                    .find(query).sort(new Document("trans_time", 1)).limit(2000); // ASC to get first deduction

            List<Document> deductions = new ArrayList<>();
            for (Document d : docs) {
                deductions.add(d);
            }

            // In-memory matching
            for (JSONObject b : missing) {
                String nick = b.optString("player", "");
                long betVal = b.optLong("bet", 0L);
                long prizeVal = b.optLong("prize", 0L);
                long bTimeMs = b.optLong("time_ms", 0L);
                String game = b.optString("game", "");

                // Map UI game name to Mongo action_name loosely
                String actionName = game;
                if ("Tài Xỉu".equals(game)) actionName = "TaiXiu";
                else if ("Sicbo".equals(game)) actionName = "Sicbo";
                else if ("Bầu Cua".equals(game)) actionName = "BauCua";
                else if ("Pirate King".equals(game) || "Chiêm Tinh".equals(game)) actionName = "ChiemTinh";
                else if ("Kho Báu".equals(game)) actionName = "KhoBau";
                else if ("Vương Quốc Vin".equals(game)) actionName = "VuongQuocVin";
                else if ("Siêu Anh Hùng".equals(game)) actionName = "SieuAnhHung";
                else if ("Nữ Điệp Viên".equals(game)) actionName = "NuDiepVien";

                long aggregatedBetDeduction = 0;
                long firstMoneyBefore = -1;

                for (Document d : deductions) {
                    if (!nick.equals(d.getString("nick_name"))) continue;

                    String aName = d.getString("action_name");
                    if (aName == null || (!aName.contains(actionName) && !("TaiXiu".equals(actionName) && aName.contains("TaiXiu")))) {
                        continue;
                    }

                    long dTime = 0L;
                    try {
                        String t = d.getString("trans_time");
                        if (t != null) dTime = OUT_FMT.get().parse(t).getTime();
                    } catch(Exception ignored) {}

                    // TaiXiu betting can occur up to 60s before round ends. Use 70s threshold.
                    if (dTime >= bTimeMs - 70000L && dTime <= bTimeMs + 5000L) {
                        long mEx = toLong(d.get("money_exchange"));
                        long cMoney = toLong(d.get("current_money"));

                        if (firstMoneyBefore == -1) {
                            firstMoneyBefore = cMoney + Math.abs(mEx);
                        }
                        aggregatedBetDeduction += Math.abs(mEx);
                    }
                }

                if (firstMoneyBefore != -1) {
                    b.put("money_before", firstMoneyBefore);
                    // Prevent crazy values if deduplication fails, by capping max bet matching
                    long appliedBet = Math.max(betVal, aggregatedBetDeduction);
                    b.put("money_after", Math.max(0L, firstMoneyBefore - appliedBet + prizeVal));
                }
            }
        } catch (Exception e) {
            logger.warn("GameHistoryService: failed to supplement balances - " + e.getMessage());
        }
    }

    public static JSONObject summarize(List<JSONObject> bets) {
        JSONObject summary = new JSONObject();
        long sumBet = 0L;
        long sumPrize = 0L;
        long sumNet = 0L;
        for (JSONObject bet : bets) {
            sumBet += bet.optLong("bet", 0L);
            sumPrize += bet.optLong("prize", 0L);
            sumNet += bet.optLong("net", 0L);
        }
        summary.put("total_count", bets.size());
        summary.put("sum_bet", sumBet);
        summary.put("sum_prize", sumPrize);
        summary.put("sum_net", sumNet);
        return summary;
    }

    /**
     * Exact aggregate summary for c=9843/admin betting history.
     *
     * <p>This deliberately does not materialize detail rows and does not apply
     * the per-source window LIMIT used by {@link #fetchAll}. Large Master/root
     * scopes can therefore return authoritative header totals while the table
     * body remains bounded for response-time safety.
     */
    public static JSONObject summarizeExact(List<String> nicknames, String gameFilter,
                                             String fromTime, String toTime,
                                             java.util.Collection<String> excludedNicknames) {
        SummaryTotals totals = new SummaryTotals();
        try {
            Document dateFilter = new Document();
            if (fromTime != null && !fromTime.isEmpty()) {
                try { dateFilter.put("$gte", OUT_FMT.get().parse(fromTime + " 00:00:00")); }
                catch (Exception ignored) {}
            }
            if (toTime != null && !toTime.isEmpty()) {
                try { dateFilter.put("$lte", OUT_FMT.get().parse(toTime + " 23:59:59")); }
                catch (Exception ignored) {}
            }

            final boolean isCategoryKeyword = (gameFilter == null || gameFilter.isEmpty()
                    || "all".equals(gameFilter)
                    || "slot".equals(gameFilter) || "minipoker".equals(gameFilter) || "caothap".equals(gameFilter)
                    || "baucua".equals(gameFilter) || "taixiu".equals(gameFilter) || "sicbo".equals(gameFilter)
                    || "banca".equals(gameFilter) || "fish".equals(gameFilter)
                    || "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                    || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                    || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter)
                    || "awc".equals(gameFilter) || "sexybcrt".equals(gameFilter) || "jili".equals(gameFilter));
            if (!isCategoryKeyword) {
                // Free-text game search is resolved after row rendering because
                // labels can be catalog-derived (GSC/AWC). Falling back avoids an
                // exact-looking but semantically incomplete aggregate.
                return new JSONObject().put("exact", false).put("reason", "free_text_game_filter");
            }

            boolean wantSlot = gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter)
                    || "slot".equals(gameFilter) || "minipoker".equals(gameFilter) || "caothap".equals(gameFilter)
                    || "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                    || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                    || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter)
                    || "awc".equals(gameFilter) || "sexybcrt".equals(gameFilter) || "jili".equals(gameFilter);
            boolean wantBauCua = gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "baucua".equals(gameFilter);
            boolean wantTaiXiu = gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "taixiu".equals(gameFilter);
            boolean wantSicbo  = gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "sicbo".equals(gameFilter);
            boolean wantBanCa  = gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter)
                    || "banca".equals(gameFilter) || "fish".equals(gameFilter);
            boolean wantLive   = gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter)
                    || "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                    || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                    || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter);

            List<String> excluded = normalizeExcludedNicknames(excludedNicknames);
            List<java.util.concurrent.Future<SummaryTotals>> futures = new ArrayList<>();
            if (wantSlot)   futures.add(FETCH_POOL.submit(() -> summarizeMongoGames(nicknames, gameFilter, dateFilter, excluded)));
            if (wantBauCua) futures.add(FETCH_POOL.submit(() -> summarizeBauCua(nicknames, dateFilter, excluded)));
            if (wantTaiXiu) futures.add(FETCH_POOL.submit(() -> summarizeTaiXiu(nicknames, dateFilter, fromTime, toTime, excluded)));
            if (wantSicbo)  futures.add(FETCH_POOL.submit(() -> summarizeSicbo(nicknames, dateFilter, fromTime, toTime, excluded)));
            if (wantBanCa)  futures.add(FETCH_POOL.submit(() -> summarizeBanCa(nicknames, fromTime, toTime, excluded)));
            if (wantLive)   futures.add(FETCH_POOL.submit(() -> summarizeThirdPartyLiveGames(nicknames, gameFilter, fromTime, toTime, excluded)));

            long deadline = System.currentTimeMillis() + 20_000L;
            for (java.util.concurrent.Future<SummaryTotals> f : futures) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    logger.warn("summarizeExact: deadline exceeded");
                    return new JSONObject().put("exact", false).put("reason", "timeout");
                }
                try {
                    totals.add(f.get(remaining, java.util.concurrent.TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    logger.warn("summarizeExact: source failed: " + e.getMessage());
                    return new JSONObject().put("exact", false).put("reason", "source_failed");
                }
            }
            return totals.toJson().put("exact", true).put("approximate", false);
        } catch (Exception e) {
            logger.warn("summarizeExact failed: " + e.getMessage());
            return new JSONObject().put("exact", false).put("reason", "error");
        }
    }

    private static SummaryTotals summarizeMongoGames(List<String> nicknames, String gameFilter,
                                                      Document dateFilter, List<String> excludedNicknames) {
        SummaryTotals totals = new SummaryTotals();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            java.util.Set<String> existingCols = getExistingCols(db);
            for (String[] src : MONGO_SOURCES) {
                String col = src[0], userField = src[2], betField = src[3], prizeField = src[4];
                if (!existingCols.contains(col)) continue;
                if (gameFilter != null && !gameFilter.isEmpty() && !"all".equals(gameFilter)) {
                    if ("slot".equals(gameFilter) && (col.contains("mini_poker") || col.contains("cao_thap"))) continue;
                    if ("minipoker".equals(gameFilter) && !col.contains("mini_poker")) continue;
                    if ("caothap".equals(gameFilter) && !col.contains("cao_thap")) continue;
                    if ("taixiu".equals(gameFilter) || "baucua".equals(gameFilter) || "sicbo".equals(gameFilter)) continue;
                    boolean isLiveFilter = "live".equals(gameFilter) || "ag".equals(gameFilter) || "wm".equals(gameFilter)
                            || "ebet".equals(gameFilter) || "sbo".equals(gameFilter) || "ibc".equals(gameFilter)
                            || "cmd".equals(gameFilter) || "evolution".equals(gameFilter) || "casino".equals(gameFilter)
                            || "awc".equals(gameFilter) || "sexybcrt".equals(gameFilter) || "jili".equals(gameFilter);
                    if (isLiveFilter && !"log_gsc_bets".equals(col) && !"log_awc_bets".equals(col)) continue;
                }

                Document query = new Document();
                applyUserFilter(query, userField, nicknames, excludedNicknames);
                if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
                if ("log_awc_bets".equals(col)) {
                    query.put("action", new Document("$in", java.util.Arrays.asList("settle", "betNSettle")));
                }
                if ("log_gsc_bets".equals(col)) {
                    query.put("settled", true);
                    java.util.Set<String> skipWagers = loadSkipZeroBetWagers();
                    if (!skipWagers.isEmpty()) {
                        query.put("wager_code", new Document("$nin", new ArrayList<>(skipWagers)));
                    }
                    // Match the detail reader's safety net for free-spin rows.
                    query.put(betField, new Document("$ne", 0L));
                }

                List<Document> pipeline = new ArrayList<>();
                pipeline.add(new Document("$match", query));
                pipeline.add(new Document("$group", new Document("_id", null)
                        .append("sum_bet", new Document("$sum", "$" + betField))
                        .append("sum_prize", new Document("$sum", "$" + prizeField))
                        .append("total_count", new Document("$sum", 1))));
                Document d = db.getCollection(col).aggregate(pipeline).allowDiskUse(true).first();
                totals.add(d);
            }
        } catch (Exception e) {
            logger.warn("summarizeMongoGames error: " + e.getMessage());
        }
        return totals;
    }

    private static SummaryTotals summarizeBauCua(List<String> nicknames, Document dateFilter, List<String> excludedNicknames) {
        SummaryTotals totals = new SummaryTotals();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            Document query = new Document();
            applyUserFilter(query, "user_name", nicknames, excludedNicknames);
            if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
            Document betExpr = new Document("$add", java.util.Arrays.asList("$bet_bau", "$bet_cua", "$bet_tom", "$bet_ca", "$bet_ga", "$bet_huou"));
            Document prizeExpr = new Document("$add", java.util.Arrays.asList("$prize_bau", "$prize_cua", "$prize_tom", "$prize_ca", "$prize_ga", "$prize_huou"));
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", query));
            pipeline.add(new Document("$project", new Document("bet", betExpr).append("prize", prizeExpr)));
            pipeline.add(new Document("$group", new Document("_id", null)
                    .append("sum_bet", new Document("$sum", "$bet"))
                    .append("sum_prize", new Document("$sum", "$prize"))
                    .append("total_count", new Document("$sum", 1))));
            totals.add(db.getCollection("bau_cua_transaction").aggregate(pipeline).allowDiskUse(true).first());
        } catch (Exception e) {
            logger.warn("summarizeBauCua error: " + e.getMessage());
        }
        return totals;
    }

    private static SummaryTotals summarizeTaiXiu(List<String> nicknames, Document dateFilter,
                                                  String fromTime, String toTime, List<String> excludedNicknames) {
        SummaryTotals mongo = summarizeSimpleMongo("log_taixiu", "user_name", "bet_value", "prize",
                new Document("money_type", 1), nicknames, dateFilter, excludedNicknames);
        if (mongo.totalCount > 0L) return mongo;
        return summarizeMysqlTx("transaction_tai_xiu_md5", nicknames, fromTime, toTime, excludedNicknames, false);
    }

    private static SummaryTotals summarizeSicbo(List<String> nicknames, Document dateFilter,
                                                 String fromTime, String toTime, List<String> excludedNicknames) {
        SummaryTotals mongo = summarizeSimpleMongo("log_sicbo", "user_name", "bet_value", "prize",
                new Document("money_type", 1), nicknames, dateFilter, excludedNicknames);
        if (mongo.totalCount > 0L) return mongo;
        return summarizeMysqlTx("transaction_tai_xiu_sicbo", nicknames, fromTime, toTime, excludedNicknames, true);
    }

    private static SummaryTotals summarizeSimpleMongo(String collection, String userField, String betField,
                                                       String prizeField, Document baseQuery,
                                                       List<String> nicknames, Document dateFilter,
                                                       List<String> excludedNicknames) {
        SummaryTotals totals = new SummaryTotals();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            Document query = baseQuery == null ? new Document() : new Document(baseQuery);
            applyUserFilter(query, userField, nicknames, excludedNicknames);
            if (!dateFilter.isEmpty()) query.put("create_time", dateFilter);
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", query));
            pipeline.add(new Document("$group", new Document("_id", null)
                    .append("sum_bet", new Document("$sum", "$" + betField))
                    .append("sum_prize", new Document("$sum", "$" + prizeField))
                    .append("total_count", new Document("$sum", 1))));
            totals.add(db.getCollection(collection).aggregate(pipeline).allowDiskUse(true).first());
        } catch (Exception e) {
            logger.warn("summarizeSimpleMongo " + collection + " error: " + e.getMessage());
        }
        return totals;
    }

    private static SummaryTotals summarizeMysqlTx(String table, List<String> nicknames, String from, String to,
                                                   List<String> excludedNicknames, boolean groupedSicbo) {
        SummaryTotals totals = new SummaryTotals();
        if (nicknames != null && nicknames.isEmpty()) return totals;
        boolean hasNickFilter = nicknames != null;
        StringBuilder dateCond = new StringBuilder();
        List<String> dateParams = new ArrayList<>();
        if (from != null && !from.isEmpty()) { dateCond.append(" AND timestamp >= ?"); dateParams.add(from + " 00:00:00"); }
        if (to != null && !to.isEmpty()) { dateCond.append(" AND timestamp <= ?"); dateParams.add(to + " 23:59:59"); }
        String excludeCond = excludedNicknames.isEmpty() ? "" : " AND user_name NOT IN (" + placeholders(excludedNicknames.size()) + ")";
        String userCond = hasNickFilter ? "user_name IN (" + placeholders(nicknames.size()) + ") AND " : "";
        String sql;
        if (groupedSicbo) {
            sql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(total_bet),0) AS sum_bet, COALESCE(SUM(total_prize),0) AS sum_prize FROM (" +
                    "SELECT user_name, reference_id, SUM(bet_value) AS total_bet, SUM(total_prize) AS total_prize " +
                    "FROM vinplay_minigame." + table + " WHERE " + userCond + "money_type=1" +
                    excludeCond + dateCond + " GROUP BY user_name, reference_id) x";
        } else {
            sql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(bet_value),0) AS sum_bet, COALESCE(SUM(total_prize),0) AS sum_prize " +
                    "FROM vinplay_minigame." + table + " WHERE " + userCond + "money_type=1" +
                    excludeCond + dateCond;
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (hasNickFilter) for (String n : nicknames) ps.setString(idx++, n);
            for (String n : excludedNicknames) ps.setString(idx++, n);
            for (String d : dateParams) ps.setString(idx++, d);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totals.add(rs.getLong("sum_bet"), rs.getLong("sum_prize"), rs.getLong("total_count"));
            }
        } catch (Exception e) {
            logger.warn("summarizeMysqlTx " + table + " error: " + e.getMessage());
        }
        return totals;
    }

    private static SummaryTotals summarizeBanCa(List<String> nicknames, String fromTime, String toTime,
                                                 List<String> excludedNicknames) {
        SummaryTotals totals = new SummaryTotals();
        if (nicknames != null && nicknames.isEmpty()) return totals;

        // SUN-13xx: see fetchBanCa — switched grain to per-episode (Reason=23
        // SessionEnd). total_count is now episode count, ChangeCash is net
        // profit per episode (positive = won, negative = lost), so sum_prize
        // becomes net win/loss across all episodes which is what the agency
        // dashboard reports under "Win/Loss".
        String sql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(bl.ChangeCash),0) AS sum_prize "
                + "FROM cgame.bc_log bl JOIN cgame.users cu ON cu.user_id = bl.UserId "
                + "WHERE bl.Reason = 23 ";
        if (nicknames != null) sql += "AND cu.nickname IN (" + placeholders(nicknames.size()) + ") ";
        if (!excludedNicknames.isEmpty()) sql += "AND cu.nickname NOT IN (" + placeholders(excludedNicknames.size()) + ") ";
        if (fromTime != null && !fromTime.isEmpty()) sql += "AND bl.Time >= ? ";
        if (toTime != null && !toTime.isEmpty()) sql += "AND bl.Time <= ? ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (nicknames != null) for (String n : nicknames) ps.setString(idx++, n);
            for (String n : excludedNicknames) ps.setString(idx++, n);
            if (fromTime != null && !fromTime.isEmpty()) ps.setString(idx++, fromTime + " 00:00:00");
            if (toTime != null && !toTime.isEmpty()) ps.setString(idx++, toTime + " 23:59:59");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totals.add(0L, rs.getLong("sum_prize"), rs.getLong("total_count"));
            }
        } catch (Exception e) {
            logger.warn("summarizeBanCa error: " + e.getMessage());
        }
        return totals;
    }

    private static SummaryTotals summarizeThirdPartyLiveGames(List<String> nicknames, String gameFilter,
                                                               String fromTime, String toTime,
                                                               List<String> excludedNicknames) {
        SummaryTotals totals = new SummaryTotals();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            java.util.Set<String> existingCols = getExistingCols(db);
            for (String[] src : THIRD_PARTY_MONGO_SOURCES) {
                String colName = src[0], providerLabel = src[1], betField = src[2], prizeField = src[3];
                if ("log_game_fish".equals(colName) || !existingCols.contains(colName)) continue;
                if (gameFilter != null && !gameFilter.isEmpty() && !"all".equalsIgnoreCase(gameFilter)
                        && !"live".equalsIgnoreCase(gameFilter) && !"casino".equalsIgnoreCase(gameFilter)) {
                    if (!providerLabel.toLowerCase().contains(gameFilter.toLowerCase())) continue;
                }
                Document query = new Document();
                if (nicknames != null && !nicknames.isEmpty()) {
                    Document userFilter = new Document("$in", nicknames);
                    query.put("$or", java.util.Arrays.asList(
                            new Document("username", userFilter),
                            new Document("nick_name", userFilter),
                            new Document("nickName", userFilter)));
                }

                java.util.Date fromDate = null, toDate = null;
                if (fromTime != null && !fromTime.isEmpty()) {
                    try { fromDate = OUT_FMT.get().parse(fromTime + " 00:00:00"); } catch (Exception ignored) {}
                }
                if (toTime != null && !toTime.isEmpty()) {
                    try { toDate = OUT_FMT.get().parse(toTime + " 23:59:59"); } catch (Exception ignored) {}
                }
                Document timeCondStr = new Document();
                if (fromTime != null && !fromTime.isEmpty()) timeCondStr.put("$gte", fromTime);
                if (toTime != null && !toTime.isEmpty()) timeCondStr.put("$lte", toTime + " 23:59:59");
                Document timeCondDate = new Document();
                if (fromDate != null) timeCondDate.put("$gte", fromDate);
                if (toDate != null) timeCondDate.put("$lte", toDate);
                if (!timeCondStr.isEmpty() || !timeCondDate.isEmpty()) {
                    List<Document> orTimeConds = new ArrayList<>();
                    if (!timeCondStr.isEmpty()) {
                        orTimeConds.add(new Document("time_log", timeCondStr));
                        orTimeConds.add(new Document("bettime", timeCondStr));
                        orTimeConds.add(new Document("createtime", timeCondStr));
                    }
                    if (!timeCondDate.isEmpty()) {
                        orTimeConds.add(new Document("create_time", timeCondDate));
                        orTimeConds.add(new Document("createtime", timeCondDate));
                    }
                    addAndCondition(query, new Document("$or", orTimeConds));
                }
                if (!excludedNicknames.isEmpty()) {
                    addAndCondition(query, new Document("username", new Document("$nin", excludedNicknames)));
                    addAndCondition(query, new Document("nick_name", new Document("$nin", excludedNicknames)));
                    addAndCondition(query, new Document("nickName", new Document("$nin", excludedNicknames)));
                }
                List<Document> pipeline = new ArrayList<>();
                pipeline.add(new Document("$match", query));
                pipeline.add(new Document("$group", new Document("_id", null)
                        .append("sum_bet", new Document("$sum", "$" + betField))
                        .append("sum_prize", new Document("$sum", "$" + prizeField))
                        .append("total_count", new Document("$sum", 1))));
                totals.add(db.getCollection(colName).aggregate(pipeline).allowDiskUse(true).first());
            }
        } catch (Exception e) {
            logger.warn("summarizeThirdPartyLiveGames error: " + e.getMessage());
        }
        return totals;
    }

    private static final class SummaryTotals {
        long sumBet;
        long sumPrize;
        long totalCount;

        void add(SummaryTotals other) {
            if (other == null) return;
            this.sumBet += other.sumBet;
            this.sumPrize += other.sumPrize;
            this.totalCount += other.totalCount;
        }

        void add(Document d) {
            if (d == null) return;
            add(toLong(d.get("sum_bet")), toLong(d.get("sum_prize")), toLong(d.get("total_count")));
        }

        void add(long bet, long prize, long count) {
            this.sumBet += bet;
            this.sumPrize += prize;
            this.totalCount += count;
        }

        JSONObject toJson() {
            return new JSONObject()
                    .put("total_count", totalCount)
                    .put("sum_bet", sumBet)
                    .put("sum_prize", sumPrize)
                    .put("sum_net", sumPrize - sumBet);
        }
    }

    private static List<JSONObject> mergeUnique(List<JSONObject> primary, List<JSONObject> secondary) {
        Map<String, JSONObject> deduped = new LinkedHashMap<>();
        for (JSONObject bet : primary) {
            deduped.put(buildBetSignature(bet), bet);
        }
        for (JSONObject bet : secondary) {
            deduped.putIfAbsent(buildBetSignature(bet), bet);
        }
        return new ArrayList<>(deduped.values());
    }

    private static String buildBetSignature(JSONObject bet) {
        return bet.optString("game_id", "") + "|"
                + bet.optString("player", "") + "|"
                + bet.optLong("bet", 0L) + "|"
                + bet.optLong("prize", 0L) + "|"
                + bet.optString("detail", "") + "|"
                + bet.optString("time", "") + "|"
                + bet.optLong("time_ms", 0L);
    }

    // === Utilities ===

    public static long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * SUN-1275: convert a milli-VND long (× 1000 scale) to an exact
     * decimal VND. No rounding — strips trailing zeros so 314450 → 314.45,
     * 100000 → 100, 314456 → 314.456. Negative inputs (e.g. net for a
     * losing bet) preserve sign. Returns BigDecimal so the JSON encoder
     * emits a Number (not a String).
     */
    public static java.math.BigDecimal exactVnd(long milli) {
        java.math.BigDecimal v = new java.math.BigDecimal(milli).movePointLeft(3).stripTrailingZeros();
        // stripTrailingZeros on integer values yields scale=-N (e.g. 100E2);
        // movePointLeft handled the divide already — coerce scale ≥ 0 so the
        // FE renders "100" not "1.0E+2".
        if (v.scale() < 0) v = v.setScale(0);
        return v;
    }

    /**
     * SUN-1205/1206/1208 — load wager codes the deferred-rebate
     * reconciler tagged as full-hedge (valid_bet_amount=0). These
     * wagers are excluded from LSC so agents don't see misleading
     * "bet=X prize=X" rows that paid no commission.
     *
     * <p>Lookback bounded to 60 days to keep the IN-list manageable
     * (full-hedge bets are uncommon — typical day produces single-digit
     * SKIP_ZERO_BET rows). Cache TTL 60s amortizes DB cost across
     * concurrent c=303 / c=9843 callers.
     */
    private static volatile java.util.Set<String> SKIP_ZERO_BET_CACHE = java.util.Collections.emptySet();
    private static volatile long SKIP_ZERO_BET_CACHE_AT = 0L;
    private static final long SKIP_ZERO_BET_CACHE_TTL_MS = 60_000L;

    private static java.util.Set<String> loadSkipZeroBetWagers() {
        long now = System.currentTimeMillis();
        if (now - SKIP_ZERO_BET_CACHE_AT < SKIP_ZERO_BET_CACHE_TTL_MS) {
            return SKIP_ZERO_BET_CACHE;
        }
        java.util.Set<String> set = new java.util.HashSet<>();
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                .getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT wager_code FROM vinplay.gsc_wager_drift_audit "
                             + "WHERE status = 'SKIP_ZERO_BET' "
                             + "  AND checked_at > NOW() - INTERVAL 60 DAY")) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString(1));
            }
        } catch (Exception e) {
            // Audit table may be absent on older deploys; cache empty so
            // we don't hammer the DB. Will retry on next cache miss.
        }
        SKIP_ZERO_BET_CACHE = java.util.Collections.unmodifiableSet(set);
        SKIP_ZERO_BET_CACHE_AT = now;
        return SKIP_ZERO_BET_CACHE;
    }

    private static final ThreadLocal<java.text.SimpleDateFormat> OUT_FMT = ThreadLocal.withInitial(() -> {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fmt.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        return fmt;
    });

    public static String normalizeTime(Object createTime) {
        if (createTime instanceof java.util.Date) {
            return OUT_FMT.get().format((java.util.Date) createTime);
        }
        if (createTime != null) return createTime.toString();
        return "";
    }

    public static long extractTimeMs(Object createTime) {
        if (createTime instanceof java.util.Date) return ((java.util.Date) createTime).getTime();
        return 0L;
    }
}
