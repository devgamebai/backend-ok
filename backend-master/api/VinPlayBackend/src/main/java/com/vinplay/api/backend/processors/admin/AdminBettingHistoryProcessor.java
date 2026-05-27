package com.vinplay.api.backend.processors.admin;

import com.hazelcast.core.IMap;
import com.vinplay.api.backend.processors.user.AdminUserSupport;
import com.vinplay.dal.service.GameHistoryService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class AdminBettingHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");
    private static final int MAX_SCOPE_PLAYERS = 500;
    private static final int MAX_PER_SOURCE_FETCH = 500;
    private static final int BOT_NICKNAME_WARN_CAP = 50_000;
    private static final long BOT_NICKNAME_CACHE_TTL_MS = 5 * 60_000L;
    private static volatile Set<String> cachedBotNicknames = java.util.Collections.emptySet();
    private static volatile long cachedBotNicknamesAt = 0L;

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // 1. Admin Authentication
            if (AdminUserSupport.requireAdmin(request, response) == null) {
                return response.toString();
            }

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
            try { if (request.getParameter("p") != null) page = Integer.parseInt(request.getParameter("p")); } catch (Exception ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (Exception ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 50) limit = 20;

            String sort = request.getParameter("sort");
            String dir = request.getParameter("dir");
            final boolean isAsc = "asc".equalsIgnoreCase(dir);

            // Same response cache as c=9541 / c=9843. Admin views are
            // read-mostly with the same shape repeating; TTL 30 s for
            // current period absorbs reload churn.
            // hide_bot is included in the cache key so bot/no-bot views
            // never share a cached response.
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "9930", playerFilter, gameFilter, fromTime, toTime, page, limit, sort, dir, hideBot);
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(toTime);

            // 2. Resolve player nicknames
            // When hideBot=true: getPlayerNicknames() already excludes is_bot=1 users.
            List<String> playerNicks = null;
            if (playerFilter != null && !playerFilter.trim().isEmpty()) {
                playerNicks = getPlayerNicknames(playerFilter, hideBot);
                if (playerNicks.isEmpty()) {
                    response.put("success", true); response.put("errorCode", "0");
                    response.put("data", new JSONArray()); response.put("total", 0);
                    response.put("page", page); response.put("totalPages", 0);
                    response.put("summary", new JSONObject()
                            .put("total_count", 0)
                            .put("sum_bet", 0)
                            .put("sum_prize", 0)
                            .put("sum_net", 0));
                    return response.toString();
                }
            }

            // 3. Query game logs
            // Large scope if we fetched the MAX fallback or wildcard.
            // Either way fetchLimit must be > 0 — fetchAll treats 0 as
            // "no limit" and would scan whole collections for narrow
            // scopes too. Bound at MAX_PER_SOURCE_FETCH.
            boolean isApproximate = (playerNicks == null) || (playerNicks.size() > 50);
            int pageWindow = Math.max(page, 1) * limit * 3;
            int fetchLimit = Math.min(Math.max(pageWindow, 120), MAX_PER_SOURCE_FETCH);

            Set<String> excludedBotNicks = (hideBot && playerNicks == null)
                    ? loadBotNicknames()
                    : java.util.Collections.emptySet();
            List<JSONObject> allBets = GameHistoryService.fetchAll(
                    playerNicks, gameFilter, fromTime, toTime, fetchLimit, true,
                    20_000L, excludedBotNicks);

            // 4. Overlay balances backwards per player
            overlayMoneyBeforeAfter(allBets, playerNicks);

            // 5. Sorting
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
                // Default order is time_ms desc
                java.util.Collections.reverse(allBets);
            }

            // 6. Summary & Pagination
            JSONObject summary = GameHistoryService.summarize(allBets);
            if (isApproximate) {
                summary.put("approximate", true);
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

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("totalRecords", total);
            response.put("data", data);
            response.put("summary", summary);
            response.put("scope_players", playerNicks == null ? "ALL" : playerNicks.size());
            response.put("excluded_bot_players", excludedBotNicks.size());
            response.put("window_limited", isApproximate);
            if (isApproximate) {
                response.put("note", "Large query: data/summary reflect a window of the most recent "
                        + fetchLimit + " records per source. Filter by nn/game/ft/et for exact figures.");
            }
            String json = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, histOnly);
            return json;
        } catch (Exception e) {
            logger.error("AdminBettingHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Resolve danh sách nick theo filter.
     * @param hideBot nếu true, chỉ lấy user có is_bot = 0 (bỏ bot).
     */
    private List<String> getPlayerNicknames(String playerFilter, boolean hideBot) {
        List<String> nicknames = new ArrayList<>();
        if (playerFilter == null || playerFilter.trim().isEmpty()) return nicknames;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sql = hideBot
                    ? "SELECT nick_name FROM users WHERE nick_name LIKE ? AND is_bot = 0 LIMIT ?"
                    : "SELECT nick_name FROM users WHERE nick_name LIKE ? LIMIT ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "%" + playerFilter.trim() + "%");
                ps.setInt(2, MAX_SCOPE_PLAYERS);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String nn = rs.getString("nick_name");
                        if (nn != null && !nn.trim().isEmpty()) {
                            nicknames.add(nn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Subtree fetch error: " + e.getMessage());
        }
        return nicknames;
    }

    /**
     * Load tập hợp nick_name của tất cả bot (is_bot = 1).
     * Dùng để đẩy điều kiện exclude xuống từng source query khi truy vấn
     * toàn sàn (không có nn filter), trước khi source áp LIMIT.
     * Cache 5 phút để tránh SELECT danh sách bot trên mỗi lần reload admin.
     */
    private Set<String> loadBotNicknames() {
        long now = System.currentTimeMillis();
        Set<String> cached = cachedBotNicknames;
        if (cachedBotNicknamesAt > 0L && now - cachedBotNicknamesAt < BOT_NICKNAME_CACHE_TTL_MS) {
            return cached;
        }

        synchronized (AdminBettingHistoryProcessor.class) {
            now = System.currentTimeMillis();
            cached = cachedBotNicknames;
            if (cachedBotNicknamesAt > 0L && now - cachedBotNicknamesAt < BOT_NICKNAME_CACHE_TTL_MS) {
                return cached;
            }

            Set<String> bots = new java.util.LinkedHashSet<>();
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
                    logger.warn("loadBotNicknames: bot nickname list reached cap "
                            + BOT_NICKNAME_WARN_CAP + "; remaining bot accounts are not included in source-level exclusion");
                }
                logger.info("loadBotNicknames: loaded " + bots.size() + " bot nick_names for source-level exclusion");
                Set<String> immutable = java.util.Collections.unmodifiableSet(bots);
                cachedBotNicknames = immutable;
                cachedBotNicknamesAt = System.currentTimeMillis();
                return immutable;
            } catch (Exception e) {
                logger.warn("loadBotNicknames error: " + e.getMessage());
                return java.util.Collections.emptySet();
            }
        }
    }

    private void overlayMoneyBeforeAfter(List<JSONObject> allBets, List<String> playerNicks) {
        if (allBets.isEmpty()) return;

        Map<String, List<JSONObject>> betsByPlayer = new LinkedHashMap<>();
        for (JSONObject bet : allBets) {
            String name = bet.optString("player", "");
            if (name.isEmpty()) continue;
            betsByPlayer.computeIfAbsent(name, k -> new ArrayList<>()).add(bet);
        }

        IMap<String, UserCacheModel> userMap = null;
        try {
            userMap = HazelcastClientFactory.getInstance().getMap("users");
        } catch (Exception ignored) {}

        Set<String> missingPlayers = new LinkedHashSet<>();
        Map<String, Long> balances = new HashMap<>();
        for (Map.Entry<String, List<JSONObject>> entry : betsByPlayer.entrySet()) {
            String player = entry.getKey();
            if (userMap != null) {
                UserCacheModel uc = userMap.get(player);
                if (uc != null) {
                    balances.put(player, uc.getVin());
                    continue;
                }
            }
            missingPlayers.add(player);
        }

        loadBalancesFromMysql(missingPlayers, balances);

        for (Map.Entry<String, List<JSONObject>> entry : betsByPlayer.entrySet()) {
            String player = entry.getKey();
            List<JSONObject> playerBets = entry.getValue();
            playerBets.sort((a, b) -> Long.compare(b.optLong("time_ms", 0L), a.optLong("time_ms", 0L)));

            // Fallback anchor: current balance, used only for records missing stored balance
            long runningBalance = balances.getOrDefault(player, 0L);
            if (runningBalance < 0) runningBalance = 0;

            for (JSONObject play : playerBets) {
                long betAmount = play.optLong("bet", 0L);
                long prizeAmount = play.optLong("prize", 0L);
                long storedMoneyBefore = play.optLong("money_before", 0L);

                if (storedMoneyBefore > 0) {
                    // Record already has valid historical balance from GameHistoryService (current_money).
                    // Trust it and re-anchor the backward walk so adjacent missing records stay consistent.
                    long moneyAfter = storedMoneyBefore - betAmount + prizeAmount;
                    play.put("money_after", Math.max(0L, moneyAfter));
                    runningBalance = storedMoneyBefore;
                } else {
                    // No stored balance (e.g. MiniPoker logs) — apply backward walk as fallback.
                    play.put("money_after", runningBalance);
                    long moneyBefore = runningBalance + betAmount - prizeAmount;
                    if (moneyBefore < 0) moneyBefore = 0;
                    play.put("money_before", moneyBefore);
                    runningBalance = moneyBefore;
                }
            }
        }
    }

    private void loadBalancesFromMysql(Set<String> players, Map<String, Long> balances) {
        if (players == null || players.isEmpty()) return;

        List<String> batch = new ArrayList<>(500);
        for (String player : players) {
            if (player == null || player.isEmpty()) continue;
            batch.add(player);
            if (batch.size() >= 500) {
                loadBalanceBatch(batch, balances);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            loadBalanceBatch(batch, balances);
        }
    }

    private void loadBalanceBatch(List<String> players, Map<String, Long> balances) {
        if (players == null || players.isEmpty()) return;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nick_name, vin FROM users WHERE nick_name IN (" + placeholders + ")")) {
            int idx = 1;
            for (String player : players) {
                ps.setString(idx++, player);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    balances.put(rs.getString("nick_name"), rs.getLong("vin"));
                }
            }
        } catch (Exception e) {
            logger.warn("loadBalanceBatch error: " + e.getMessage());
        }
    }
}
