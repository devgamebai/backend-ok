package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * c=9843 — Lịch sử cược by agent subtree.
 *
 * Agent sees ALL bet records of ALL players in their subtree.
 * "từ đó nhìn vào thì sẽ biết nhận được bao nhiêu hoa hồng, từ thằng users nào"
 *
 * Flow:
 *   1. Resolve agent → subtree agent IDs (FIND_IN_SET)
 *   2. Collect all referral codes (current + historical)
 *   3. Find all player nicknames linked to those codes
 *   4. Query per-game log collections for those players
 *   5. Return unified, time-sorted, paginated bet records
 *
 * Params: rc (agent nickname), p (page), l (limit), game (optional filter),
 *         ft/et (date range), nn (player nickname filter).
 */
public class GetBettingHistoryByAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private static final int MAX_SCOPE_PLAYERS = 5000;
    private static final int EXACT_SCOPE_PLAYER_THRESHOLD = 300;
    private static final int MAX_PER_SOURCE_FETCH = 500;
    private static final int BOT_NICKNAME_WARN_CAP = 50_000;
    private static final long BOT_NICKNAME_CACHE_TTL_MS = 5 * 60_000L;
    private static volatile Boolean agentCodeHistoryTableExists = null;
    private static final Object agentCodeHistoryTableLock = new Object();
    private static volatile Set<String> cachedBotNicknames = java.util.Collections.emptySet();
    private static volatile long cachedBotNicknamesAt = 0L;

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            String playerFilter = request.getParameter("nn");
            String gameFilter = request.getParameter("game");
            String fromTime = request.getParameter("ft");
            String toTime = request.getParameter("et");

            // hide_bot: ẩn lịch sử cược của bot. Mặc định true (ẩn bot).
            // Truyền hide_bot=0 hoặc hide_bot=false để xem cả bot.
            String hideBotParam = request.getParameter("hide_bot");
            final boolean hideBot = hideBotParam == null
                    || !("0".equals(hideBotParam) || "false".equalsIgnoreCase(hideBotParam));

            int page = 1, limit = 20;
            try { if (request.getParameter("p") != null) page = Integer.parseInt(request.getParameter("p")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 50) limit = 20;

            String sort = request.getParameter("sort");
            String dir = request.getParameter("dir");
            final boolean isAsc = "asc".equalsIgnoreCase(dir);

            if (agentNick == null || agentNick.isEmpty()) {
                return err(response, "1001", "rc required");
            }

            AgentInfo agent = AgentHierarchyHelper.resolveAgent(agentNick);
            if (agent == null) {
                return err(response, "1002", "agent not found");
            }
            final boolean isSiteMaster = AgentHierarchyHelper.isSiteMaster(agent);
            final boolean hasPlayerFilter = playerFilter != null && !playerFilter.trim().isEmpty();
            // SUN-1297 guard removed 2026-05-13: agency UI calls this with an
            // agent session cookie, not an admin token, so the previous
            // requirement that site-master callers carry `aat` broke every
            // master-account betting-history query through the agency portal.
            // Whole-site fan-out is still bounded by MAX_SCOPE_PLAYERS and
            // the master-summary fast path skips the nickname expansion.

            // SUN-1108 Tier 4: response cache lookup. Same agent + same query
            // shape returns the cached JSON within TTL. Cache miss falls
            // through to the full computation below; the result is cached
            // before return.
            // hide_bot bao gồm trong cache key để tránh trả nhầm cached response.
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "9843:v3:exact-master-summary", agentNick, playerFilter, gameFilter, fromTime, toTime,
                    page, limit, sort, dir, hideBot, isSiteMaster);
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(toTime);

            // 1. Resolve agent subtree → player nicknames
            // Khi hideBot=true, computeSubtreePlayerNicknames loại is_bot=1 từ SQL.
            // SpecialAccount/root Master has whole-site visibility. Without a player
            // filter, avoid expanding the entire site into a 5k nickname list: the
            // detail query and exact summary can push down an all-site scope directly.
            PlayerScope scope = (isSiteMaster && !hasPlayerFilter)
                    ? new PlayerScope(null, false)
                    : getSubtreePlayerNicknames(agentNick, playerFilter, MAX_SCOPE_PLAYERS, hideBot);
            List<String> playerNicks = scope.nicknames;
            if (playerNicks != null && playerNicks.isEmpty()) {
                response.put("success", true); response.put("errorCode", "0");
                response.put("data", new JSONArray()); response.put("total", 0);
                response.put("page", page); response.put("totalPages", 1);
                response.put("summary", new JSONObject()
                        .put("total_count", 0)
                        .put("sum_bet", 0)
                        .put("sum_prize", 0)
                        .put("sum_net", 0));
                String emptyJson = response.toString();
                com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, emptyJson, histOnly);
                return emptyJson;
            }

            // 2. Query game logs via shared GameHistoryService (same source as c=303).
            // For large subtrees, avoid full scans (can hit reverse-proxy timeout).
            Set<String> excludedBotNicks = (hideBot && isSiteMaster && playerNicks == null)
                    ? loadBotNicknames()
                    : java.util.Collections.emptySet();
            boolean largeScope = playerNicks == null || playerNicks.size() > EXACT_SCOPE_PLAYER_THRESHOLD;
            int fetchLimit = 0;
            if (largeScope) {
                int pageWindow = Math.max(page, 1) * limit * 3;
                fetchLimit = Math.max(pageWindow, 120);
                fetchLimit = Math.min(fetchLimit, MAX_PER_SOURCE_FETCH);
            }

            List<JSONObject> allBets = com.vinplay.dal.service.GameHistoryService.fetchAll(
                    playerNicks, gameFilter, fromTime, toTime, fetchLimit, true,
                    20_000L, excludedBotNicks);

            // SUN-859: some game logs (e.g. log_mini_poker) have no current_money,
            // and the supplementMissingBalances fallback via log_money_user_vin can
            // surface upstream bad data (MiniPoker logs have negative current_money)
            // → money_before renders as a negative number in the agency UI.
            // Override with a per-player backward walk from the real wallet
            // (Hazelcast "users" map, falling back to MySQL users.vin). Same pattern
            // as c=303 uses for the player's own history. Walk runs newest→oldest
            // per player and clamps money_before at 0 so the agency column can
            // never display a negative balance even when the upstream log is bad.
            overlayMoneyBeforeAfter(allBets, playerNicks);

            if (sort != null && !sort.isEmpty() && !"time".equals(sort) && !"time_ms".equals(sort)) {
                allBets.sort((a, b) -> {
                    int result = 0;
                    if ("bet".equals(sort) || "prize".equals(sort) || "net".equals(sort) || "money_before".equals(sort) || "money_after".equals(sort)) {
                        result = Long.compare(a.optLong(sort, 0L), b.optLong(sort, 0L));
                    } else {
                        result = a.optString(sort, "").compareTo(b.optString(sort, ""));
                    }
                    return isAsc ? result : -result;
                });
            } else if (isAsc) {
                // Default order is time_ms desc, so if asc, reverse it
                java.util.Collections.reverse(allBets);
            }

            // When window-limited, summary/total are derived from only `fetchLimit` records
            // (e.g. 500 out of potentially 50,000). Expose this clearly:
            //   - data_approximate=true  → FE should render "~" prefix on all aggregate figures
            //   - summary is still emitted but marked with an "approximate" field so FE can decide
            //     to show a warning banner instead of authoritative numbers
            boolean isApproximate = fetchLimit > 0;
            JSONObject summary = scope.truncated
                    ? new JSONObject().put("exact", false).put("reason", "scope_limited")
                    : com.vinplay.dal.service.GameHistoryService.summarizeExact(
                            playerNicks, gameFilter, fromTime, toTime, excludedBotNicks);
            boolean summaryExact = summary.optBoolean("exact", false);
            if (!summaryExact) {
                summary = com.vinplay.dal.service.GameHistoryService.summarize(allBets);
                if (isApproximate) {
                    summary.put("approximate", true);
                }
                summary.put("exact", false);
            }

            int total = allBets.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) total / limit));
            int from = (page - 1) * limit, to = Math.min(from + limit, total);

            JSONArray data = new JSONArray();
            for (int i = from; i < to; i++) {
                JSONObject b = allBets.get(i);
                b.remove("time_ms");
                data.put(b);
            }

            response.put("success", true); response.put("errorCode", "0");
            response.put("data", data);
            // When data_approximate=true, "total" reflects only the windowed fetch (not the
            // true record count). FE MUST check data_approximate before displaying "total".
            response.put("total", total);
            response.put("data_approximate", isApproximate);
            response.put("page", page); response.put("totalPages", totalPages);
            response.put("summary", summary);
            response.put("scope_players", playerNicks == null ? "ALL" : playerNicks.size());
            response.put("scope_limited", scope.truncated);
            response.put("window_limited", isApproximate);
            response.put("excluded_bot_players", excludedBotNicks.size());
            if (isApproximate) {
                response.put("note", "Large subtree: data/summary/total reflect a window of the most recent "
                        + fetchLimit + " records per source. Summary is exact when summary.exact=true.");
            }
            // SUN-1108 Tier 4: cache the rendered response for the configured TTL.
            // Approximate / window-limited responses still cache — the FE handles the
            // approximation flag, and caching saves the larger fetch on next request.
            String json = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, histOnly);
            return json;
        } catch (Exception e) {
            logger.error("GetBettingHistoryByAgentProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Load all bot nicknames for all-site Master queries so source-level
     * aggregate and detail windows can exclude bots before any per-source LIMIT.
     */
    private static Set<String> loadBotNicknames() {
        long now = System.currentTimeMillis();
        Set<String> cached = cachedBotNicknames;
        if (cachedBotNicknamesAt > 0L && now - cachedBotNicknamesAt < BOT_NICKNAME_CACHE_TTL_MS) {
            return cached;
        }
        synchronized (GetBettingHistoryByAgentProcessor.class) {
            now = System.currentTimeMillis();
            cached = cachedBotNicknames;
            if (cachedBotNicknamesAt > 0L && now - cachedBotNicknamesAt < BOT_NICKNAME_CACHE_TTL_MS) {
                return cached;
            }

            Set<String> bots = new LinkedHashSet<>();
            boolean capped = false;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT nick_name FROM users WHERE is_bot = 1 ORDER BY nick_name LIMIT ?")) {
                ps.setInt(1, BOT_NICKNAME_WARN_CAP + 1);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String nn = rs.getString("nick_name");
                        if (nn == null || nn.trim().isEmpty()) continue;
                        if (bots.size() >= BOT_NICKNAME_WARN_CAP) {
                            capped = true;
                            break;
                        }
                        bots.add(nn.trim());
                    }
                }
                if (capped) {
                    logger.warn("GetBettingHistoryByAgentProcessor.loadBotNicknames reached cap "
                            + BOT_NICKNAME_WARN_CAP);
                }
                Set<String> immutable = java.util.Collections.unmodifiableSet(bots);
                cachedBotNicknames = immutable;
                cachedBotNicknamesAt = System.currentTimeMillis();
                return immutable;
            } catch (Exception e) {
                logger.warn("GetBettingHistoryByAgentProcessor.loadBotNicknames error: " + e.getMessage());
                return java.util.Collections.emptySet();
            }
        }
    }

    /**
     * Get all player nicknames in an agent's subtree.
     *
     * <p>SUN-1108 Tier 2: wrapped by a Hazelcast cache (10-minute TTL by
     * default). The underlying SQL resolution is unchanged — the cache
     * is purely additive and disables instantly via env var
     * {@code SUBTREE_CACHE_ENABLED=false} on any production issue.
     */
    private PlayerScope getSubtreePlayerNicknames(String agentNick, String playerFilter,
                                                   int maxPlayers, boolean hideBot) {
        // Tier 2 cache lookup — bao gồm hideBot trong key để tránh trả nhầm scope bot/no-bot.
        String cacheKey = com.vinplay.api.backend.perf.SubtreeCacheHelper.key(
                agentNick, playerFilter, maxPlayers, hideBot);
        com.vinplay.api.backend.perf.SubtreeCacheHelper.CachedScope cached =
                com.vinplay.api.backend.perf.SubtreeCacheHelper.get(cacheKey);
        if (cached != null) {
            return new PlayerScope(cached.getNicknames(), cached.isTruncated());
        }
        PlayerScope fresh = computeSubtreePlayerNicknames(agentNick, playerFilter, maxPlayers, hideBot);
        com.vinplay.api.backend.perf.SubtreeCacheHelper.put(cacheKey,
                new com.vinplay.api.backend.perf.SubtreeCacheHelper.CachedScope(fresh.nicknames, fresh.truncated));
        return fresh;
    }

    /**
     * Original SQL-backed resolution; renamed to make space for the cache wrapper above.
     * @param hideBot nếu true, thêm AND is_bot = 0 vào query users để loại bot.
     */
    private PlayerScope computeSubtreePlayerNicknames(String agentNick, String playerFilter,
                                                       int maxPlayers, boolean hideBot) {
        Set<String> nicks = new LinkedHashSet<>();
        boolean truncated = false;
        try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            int agentId = -1;
            try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE nickname=?")) {
                ps.setString(1, agentNick);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) agentId = rs.getInt(1); }
            }
            if (agentId <= 0) return new PlayerScope(new ArrayList<>(), false);

            List<Integer> agentIds = new ArrayList<>();
            List<String> referralCodes = new ArrayList<>();
            // SUN-799: agency=player — agents participate in their own rebate pool
            // and bet from the same account. Their own bets live under nick_name =
            // useragent.nickname, but users.dai_ly=1 and users.parent_agent_id points
            // UP (to their parent), so the subtree UNION below misses them. Collect
            // agent nicknames here and add them directly to the scope after the player
            // query runs. Matches the rolling/commission side which already includes
            // the agent's own self-rebate entry.
            List<String> agentNicknames = new ArrayList<>();

            // 1. Fetch subtree agent IDs and current codes
            try (PreparedStatement ps = adminConn.prepareStatement("SELECT id, code, nickname FROM useragent WHERE id=? OR FIND_IN_SET(?, IFNULL(ancestors,'')) > 0")) {
                ps.setInt(1, agentId);
                ps.setInt(2, agentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        agentIds.add(rs.getInt("id"));
                        String code = rs.getString("code");
                        if (code != null && !code.trim().isEmpty()) {
                            referralCodes.add(code.trim());
                        }
                        String aNick = rs.getString("nickname");
                        if (aNick != null && !aNick.trim().isEmpty()) {
                            agentNicknames.add(aNick.trim());
                        }
                    }
                }
            }

            // 2. Fetch history codes if table exists
            boolean hasAgentCodeHistory = hasAgentCodeHistoryTable(adminConn);
            if (hasAgentCodeHistory) {
                String histSql = "SELECT h.old_code FROM agent_code_history h " +
                                 "JOIN useragent ua ON ua.id = h.agent_id " +
                                 "WHERE (ua.id = ? OR FIND_IN_SET(?, IFNULL(ua.ancestors,'')) > 0) " +
                                 "AND h.old_code IS NOT NULL AND h.old_code <> ''";
                try (PreparedStatement ps = adminConn.prepareStatement(histSql)) {
                    ps.setInt(1, agentId);
                    ps.setInt(2, agentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String code = rs.getString("old_code");
                            if (code != null && !code.trim().isEmpty()) {
                                referralCodes.add(code.trim());
                            }
                        }
                    }
                }
            }

            if (agentIds.isEmpty() && referralCodes.isEmpty()) {
                return new PlayerScope(new ArrayList<>(), false);
            }

            // 3. Construct heavily optimized user query using fetched IDs
            // Use UNION ALL to avoid `OR` full scans across different indexes
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT nick_name, MAX(id) as uid FROM (");

            // Điều kiện is_bot tái sử dụng theo hideBot flag:
            final String botCond = hideBot ? " AND is_bot = 0" : "";

            boolean hasIds = !agentIds.isEmpty();
            if (hasIds) {
                sql.append("SELECT nick_name, id FROM users WHERE dai_ly = 0 AND nick_name IS NOT NULL AND nick_name <> '' AND parent_agent_id IN (");
                for (int i = 0; i < agentIds.size(); i++) sql.append(i > 0 ? ",?" : "?");
                sql.append(")").append(botCond);
                if (playerFilter != null && !playerFilter.isEmpty()) sql.append(" AND nick_name LIKE ?");
            }

            boolean hasCodes = !referralCodes.isEmpty();
            if (hasCodes) {
                if (hasIds) sql.append(" UNION ALL ");
                sql.append("SELECT nick_name, id FROM users WHERE dai_ly = 0 AND nick_name IS NOT NULL AND nick_name <> '' AND referral_code IN (");
                for (int i = 0; i < referralCodes.size(); i++) sql.append(i > 0 ? ",?" : "?");
                sql.append(")").append(botCond);
                if (playerFilter != null && !playerFilter.isEmpty()) sql.append(" AND nick_name LIKE ?");
            }

            sql.append(") u GROUP BY nick_name ORDER BY uid DESC LIMIT ?");

            try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = userConn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (hasIds) {
                    for (Integer id : agentIds) { ps.setInt(idx++, id); }
                    if (playerFilter != null && !playerFilter.isEmpty()) { ps.setString(idx++, "%" + playerFilter + "%"); }
                }
                if (hasCodes) {
                    for (String code : referralCodes) { ps.setString(idx++, code); }
                    if (playerFilter != null && !playerFilter.isEmpty()) { ps.setString(idx++, "%" + playerFilter + "%"); }
                }
                ps.setInt(idx, maxPlayers + 1);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String nick = rs.getString(1);
                        if (nick != null && !nick.isEmpty()) {
                            nicks.add(nick);
                            if (nicks.size() > maxPlayers) {
                                truncated = true;
                                break;
                            }
                        }
                    }
                }
            }
            // SUN-799: fold in agent nicknames so the agent's own bets (and
            // sub-agents' bets) show up. Apply playerFilter (LIKE semantics).
            // SUN-BOT-HIDE: khi hideBot=true, loại agent nicknames có is_bot=1
            // (trường hợp bot account được đăng ký là agent).
            String filterLc = (playerFilter != null && !playerFilter.isEmpty())
                    ? playerFilter.toLowerCase() : null;
            java.util.Set<String> botAgentNicks = hideBot
                    ? loadBotNicknameSet(agentNicknames)
                    : java.util.Collections.emptySet();
            for (String aNick : agentNicknames) {
                if (hideBot && botAgentNicks.contains(aNick)) continue;  // skip bot agents
                if (filterLc != null && !aNick.toLowerCase().contains(filterLc)) continue;
                if (nicks.size() >= maxPlayers) { truncated = true; break; }
                nicks.add(aNick);
            }
        } catch (Exception e) {
            logger.warn("getSubtreePlayerNicknames error", e);
        }
        List<String> list = new ArrayList<>(nicks);
        if (list.size() > maxPlayers) {
            list = list.subList(0, maxPlayers);
        }
        return new PlayerScope(list, truncated);
    }

    private static boolean hasAgentCodeHistoryTable(Connection adminConn) {
        Boolean cached = agentCodeHistoryTableExists;
        if (cached != null) return cached.booleanValue();

        synchronized (agentCodeHistoryTableLock) {
            if (agentCodeHistoryTableExists != null) {
                return agentCodeHistoryTableExists.booleanValue();
            }
            boolean exists = false;
            try (PreparedStatement ps = adminConn.prepareStatement("SHOW TABLES LIKE 'agent_code_history'");
                 ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            } catch (Exception e) {
                logger.warn("detect agent_code_history table failed", e);
            }
            agentCodeHistoryTableExists = exists;
            return exists;
        }
    }

    private static final class PlayerScope {
        final List<String> nicknames;
        final boolean truncated;

        PlayerScope(List<String> nicknames, boolean truncated) {
            this.nicknames = nicknames;
            this.truncated = truncated;
        }
    }

    /**
     * Overlay money_before/money_after per player.
     * For records that already carry a valid stored balance (current_money > 0 from MongoDB),
     * those values are trusted as-is and used to re-anchor the running total.
     * For records without stored balance (e.g. MiniPoker logs, SUN-859), a backward walk
     * from the real wallet is used as a fallback. Must run before any re-sorting — expects
     * time-desc order (which is what GameHistoryService.fetchAll produces).
     */
    private static void overlayMoneyBeforeAfter(List<JSONObject> bets, List<String> playerNicks) {
        if (bets == null || bets.isEmpty()) return;
        java.util.Set<String> playersWithBets = new java.util.LinkedHashSet<>();
        for (JSONObject b : bets) {
            String nick = b.optString("player", "");
            if (nick != null && !nick.isEmpty()) {
                playersWithBets.add(nick);
            }
        }
        if (playersWithBets.isEmpty()) return;

        java.util.Map<String, Long> bal = new java.util.HashMap<>();
        try {
            com.hazelcast.core.IMap<String, com.vinplay.vbee.common.models.cache.UserCacheModel> userMap =
                    com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("users");
            java.util.List<String> missing = new java.util.ArrayList<>();
            java.util.Map<String, com.vinplay.vbee.common.models.cache.UserCacheModel> cachedUsers =
                    userMap.getAll(playersWithBets);
            for (String nick : playersWithBets) {
                com.vinplay.vbee.common.models.cache.UserCacheModel uc = cachedUsers.get(nick);
                if (uc != null) {
                    bal.put(nick, uc.getVin());
                } else {
                    missing.add(nick);
                }
            }
            if (!missing.isEmpty()) {
                loadBalancesFromMysql(missing, bal);
            }
        } catch (Exception e) {
            logger.warn("overlayMoneyBeforeAfter: balance lookup failed - " + e.getMessage());
            return;
        }
        for (JSONObject b : bets) {
            String nick = b.optString("player", "");
            Long cur = bal.get(nick);
            if (cur == null) continue;
            long bet = b.optLong("bet", 0L);
            long prize = b.optLong("prize", 0L);
            long storedMoneyBefore = b.optLong("money_before", 0L);

            if (storedMoneyBefore > 0) {
                // Record already has valid historical balance from GameHistoryService (current_money).
                // Trust it and re-anchor the backward walk so adjacent missing records stay consistent.
                long moneyAfter = storedMoneyBefore - bet + prize;
                b.put("money_after", Math.max(0L, moneyAfter));
                bal.put(nick, storedMoneyBefore);
            } else {
                // No stored balance (e.g. MiniPoker logs) — apply backward walk as fallback.
                b.put("money_after", Math.max(0L, cur));
                long before = cur - (prize - bet);
                b.put("money_before", Math.max(0L, before));
                bal.put(nick, before);
            }
        }
    }

    private static void loadBalancesFromMysql(java.util.List<String> missing, java.util.Map<String, Long> bal) {
        if (missing == null || missing.isEmpty()) return;

        java.util.List<String> batch = new java.util.ArrayList<>(500);
        for (String nick : missing) {
            if (nick == null || nick.isEmpty()) continue;
            batch.add(nick);
            if (batch.size() >= 500) {
                loadBalanceBatch(batch, bal);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            loadBalanceBatch(batch, bal);
        }
    }

    private static void loadBalanceBatch(java.util.List<String> batch, java.util.Map<String, Long> bal) {
        if (batch == null || batch.isEmpty()) return;

        StringBuilder ph = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) ph.append(",");
            ph.append("?");
        }

        try (java.sql.Connection c = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT nick_name, vin FROM users WHERE nick_name IN (" + ph + ")")) {
            int idx = 1;
            for (String nick : batch) ps.setString(idx++, nick);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) bal.put(rs.getString("nick_name"), rs.getLong("vin"));
            }
        } catch (Exception e) {
            logger.warn("overlayMoneyBeforeAfter: balance batch lookup failed - " + e.getMessage());
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }

    /**
     * SUN-BOT-HIDE: Kiểm tra danh sách nicknames nào có is_bot=1 trong bảng users.
     * Dùng để filter agentNicknames (SUN-799) khi hideBot=true.
     * Trả về Set các nick_name là bot (is_bot=1).
     */
    private static java.util.Set<String> loadBotNicknameSet(java.util.List<String> candidates) {
        java.util.Set<String> bots = new java.util.HashSet<>();
        if (candidates == null || candidates.isEmpty()) return bots;
        StringBuilder ph = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) ph.append(",");
            ph.append("?");
        }
        try (java.sql.Connection c = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT nick_name FROM users WHERE nick_name IN (" + ph + ") AND is_bot = 1")) {
            for (int i = 0; i < candidates.size(); i++) ps.setString(i + 1, candidates.get(i));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nn = rs.getString("nick_name");
                    if (nn != null && !nn.trim().isEmpty()) bots.add(nn);
                }
            }
        } catch (Exception e) {
            logger.warn("loadBotNicknameSet error: " + e.getMessage());
        }
        return bots;
    }
}
