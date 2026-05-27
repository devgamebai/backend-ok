package com.vinplay.api.backend.processors.agent;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public class GetRebateLogs4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest) param.get();
        JSONObject response = new JSONObject();

        try {
            String agentCode = request.getParameter("rc");
            String nickName = request.getParameter("nn");

            // Player username/nickname filter (partial match on player_nickname)
            String playerFilter = request.getParameter("un");
            if (playerFilter != null) playerFilter = playerFilter.trim();
            if (playerFilter != null && playerFilter.isEmpty()) playerFilter = null;

            // Filters
            String dateFrom = request.getParameter("ft");
            String dateTo = request.getParameter("et");
            int page = 1;
            int limit = 20;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (Exception e) {}
            try { if (request.getParameter("mi") != null) limit = Integer.parseInt(request.getParameter("mi")); } catch (Exception e) {}

            String sort = request.getParameter("sort");
            String dir = request.getParameter("dir");

            if ((agentCode == null || agentCode.isEmpty()) && (nickName == null || nickName.isEmpty())) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing agent code or nickname");
                return response.toString();
            }

            int agentId = -1;
            String resolvedCode = null;
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Preserve old lookup priority: rc > nn
                String lookupVal = (agentCode != null && !agentCode.trim().isEmpty())
                        ? agentCode.trim()
                        : (nickName != null ? nickName.trim() : "");

                try (PreparedStatement stm = conn.prepareStatement(
                    "SELECT id, code, username FROM vinplay_admin.useragent WHERE code = ? OR nickname = ? OR username = ? LIMIT 1")) {
                    stm.setString(1, lookupVal);
                    stm.setString(2, lookupVal);
                    stm.setString(3, lookupVal);
                    try (ResultSet rs = stm.executeQuery()) {
                        if (rs.next()) {
                            agentId = rs.getInt("id");
                            resolvedCode = rs.getString("code");
                        }
                    }
                }
            }
            // SpecialAccount-class observer: resolved useragent.code='0' is a
            // read-only supervisor (RoleGuard.isSpecialAccount, payout-skipped).
            // Their own rebate_logs rows always carry cascade-allocated 0
            // because every downline tier in their tree consumes the rate via
            // differential cascade. To make the dashboard meaningful for them,
            // emit each row's full configured rate × volume as commission_rate
            // / commission_earned (i.e. the platform's per-bet liability),
            // and recompute the summary's sum_commission_earned the same way.
            // DB / actual payouts are unchanged — this is a viewer-context
            // display override only.
            final boolean supervisorView = "0".equals(resolvedCode);

            if (agentId == -1) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agent not found");
                return response.toString();
            }

            // SUN-1108 Tier 4: response cache. Same agent + same query shape
            // (agent_id, date range, paging, sort) returns cached JSON within
            // TTL. 30 s for queries that include today, 300 s for purely
            // historical ranges.
            // Suffix bump invalidates pre-deploy cached responses that
            // rendered AWC rows as "Sexy Baccarat" (no platform). Without
            // it, /api/rolling keeps returning stale labels for
            // ResponseCacheHelper.TTL after every label/format change.
            // SUN-1226: append "bots_excl" so any PROD cache entries generated before
            // the bot-filter fix (which did not properly exclude NULL player_nickname rows)
            // are automatically invalidated on deploy. This endpoint always uses
            // excludeBots=true, so the suffix is a permanent part of the key.
            // Supervisor-view rows have different commission_rate /
            // commission_earned values (configured rate × volume) than the
            // standard cascade view, so suffix the cache key to keep the
            // two response shapes from bleeding into each other.
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "9541-awcplat", agentId, dateFrom, dateTo, page, limit, sort, dir,
                    supervisorView ? "bots_excl_supv" : "bots_excl",
                    playerFilter != null ? "pf_" + playerFilter : "pf_none");
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(dateTo);

            Integer targetAgentId = agentId;

            // SUN-1248 (Mr.DEAL multi-bet baccarat fix): switch from queryLogs
            // (raw 1-row-per-click) to queryLogsAggregated which now GROUPs BY
            // wager_code. For single-bet rounds the aggregation is a no-op
            // (1 row per click == 1 row per wager). For multi-bet rounds
            // (Evolution Baccarat 1/4/5/6/Cxx where the player adds to bet
            // during the round), all sub-bet sub-rows collapse to ONE row
            // showing the cumulative round volume (260 = 220 + 40), which
            // is what operators expect to see in Rolling.
            //
            // SUN-1233: previously chose queryLogs to match LS Cược's per-event
            // granularity. With wager_code in GROUP BY, queryLogsAggregated
            // gives the same per-event granularity for single-bet rounds AND
            // per-round granularity for multi-bet rounds — strictly better.
            //
            // SUN-1226: excludeBots filtering still applied via the WHERE
            // NOT EXISTS clause inside queryLogsAggregated.
            // SUN-1240: REVERSED/REJECTED filtering — handled by the row-level
            // status filter below before the SQL GROUP BY happens.
            List<Map<String, Object>> logs = RebateService.queryLogsAggregated(targetAgentId, null, dateFrom, dateTo, sort, dir, page, limit, true, playerFilter);
            long total = RebateService.countLogs(targetAgentId, null, dateFrom, dateTo, true, true, playerFilter);
            Map<String, Object> summary = RebateService.summarizeLogs(targetAgentId, null, dateFrom, dateTo, true, true, playerFilter);
            long totalPages = Math.max(1L, (total + limit - 1) / limit);

            // Supervisor view: replace the cascade-allocated commission_rate /
            // commission_earned with full configured rate × volume so the
            // observer sees per-bet platform liability instead of their own
            // always-zero slice. Summary's sum_commission_earned is recomputed
            // at the same liability level via supervisorSumCommission.
            if (supervisorView) {
                java.math.BigDecimal supSum = RebateService.supervisorSumCommission(
                        targetAgentId, dateFrom, dateTo, true, true);
                summary.put("sum_commission_earned",
                        com.vinplay.dal.utils.PctFormatter.format(supSum));
                for (Map<String, Object> row : logs) {
                    Object ownObj = row.get("own_rate_pct");
                    Object betObj = row.get("bet_amount");
                    if (ownObj == null || betObj == null) continue;
                    try {
                        java.math.BigDecimal ownRate = new java.math.BigDecimal(ownObj.toString());
                        long bet = ((Number) betObj).longValue();
                        java.math.BigDecimal earned = java.math.BigDecimal.valueOf(bet)
                                .multiply(ownRate)
                                .divide(java.math.BigDecimal.valueOf(100), 2,
                                        java.math.RoundingMode.HALF_UP);
                        row.put("commission_rate", ownObj);
                        row.put("commission_earned",
                                com.vinplay.dal.utils.PctFormatter.format(earned));
                    } catch (Exception ignore) { /* keep cascade values on parse error */ }
                }
            }

            JSONArray arr = new JSONArray();
            for (Map<String, Object> row : logs) {
                // SUN-943: resolve gsc_<pc>_<gc> action in note to human-readable game name
                Object noteObj = row.get("note");
                if (noteObj != null) {
                    String note = noteObj.toString();
                    int actionIdx = note.indexOf("action=");
                    if (actionIdx >= 0) {
                        String actionPart = note.substring(actionIdx + 7);
                        int spaceIdx = actionPart.indexOf(' ');
                        String actionKey = spaceIdx > 0 ? actionPart.substring(0, spaceIdx) : actionPart;
                        if (actionKey.startsWith("gsc_")) {
                            try {
                                String[] parts = actionKey.split("_", 3);
                                if (parts.length == 3) {
                                    int pc = Integer.parseInt(parts[1]);
                                    String gc = parts[2];
                                    String gameName = com.vinplay.dal.service.GscGameNameResolver.displayName(pc, gc);
                                    row.put("game_name", gameName);
                                    row.put("game_key", actionKey);
                                }
                            } catch (Exception ignored) {}
                        } else if (actionKey.startsWith("awc_")) {
                            try {
                                String[] parts = actionKey.split("_", 3);
                                if (parts.length == 3) {
                                    String platform = parts[1];
                                    String gc = parts[2];
                                    // SUN-1252 follow-up 2026-05-18 — pass the
                                    // round_id (= wager_code for AWC; vendor
                                    // uses one field for both) so the resolver
                                    // hits the per-table catalog row and
                                    // returns "Sexy Baccarat C07" instead of
                                    // the generic "Sexy Baccarat" stub. Per-
                                    // table data has been backfilled into
                                    // rebate_logs.round_id, but the existing
                                    // SELECT already surfaces wager_code (added
                                    // SUN-1248) which carries the same value
                                    // for AWC rows. Use it directly to avoid
                                    // a second column round-trip.
                                    Object wcObj = row.get("wager_code");
                                    String roundId = wcObj == null ? null : wcObj.toString();
                                    String gameName = com.vinplay.dal.service.AwcGameNameResolver
                                            .displayName(platform, gc, roundId);
                                    // Match GameHistoryService log_awc_bets renderer:
                                    // "<curatedName> (<PLATFORM>)" — keeps the bet
                                    // history view (mongo log_awc_bets) and rolling
                                    // view (rebate_logs) using identical labels.
                                    String platformUpper = platform == null ? "" : platform.toUpperCase();
                                    if (gameName != null && !gameName.isEmpty() && !platformUpper.isEmpty()) {
                                        row.put("game_name", gameName + " (" + platformUpper + ")");
                                    } else if (gameName != null && !gameName.isEmpty()) {
                                        row.put("game_name", gameName);
                                    } else if (!platformUpper.isEmpty()) {
                                        row.put("game_name", platformUpper);
                                    }
                                    row.put("game_key", actionKey);
                                }
                            } catch (Exception ignored) {}
                        } else {
                            row.put("game_name", actionKey);
                            row.put("game_key", actionKey);
                        }
                    }
                }
                arr.put(new JSONObject(row));
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", total);
            response.put("totalPages", totalPages);
            response.put("summary", new JSONObject(summary));

            // SUN-1108 Tier 4: cache the rendered response for the configured TTL.
            // SUN-1155: also tag with agentId so c=3083 claim can invalidate
            // every cached response for this agent in one call.
            String json = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.putWithAgent(cacheKey, json, histOnly, agentId);
            return json;

        } catch (Exception e) {
            logger.error("GetRebateLogs4AgencyProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
