package com.vinplay.api.backend.processors.log;

import com.vinplay.api.backend.processors.user.AdminUserSupport;
import com.vinplay.dal.dao.LogMoneyUserDao;
import com.vinplay.dal.dao.impl.LogMoneyUserDaoImpl;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryCategory;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryCategoryMapper;
import com.vinplay.vbee.common.balancehistory.BalanceHistoryRow;
import com.vinplay.vbee.common.balancehistory.BalanceHistorySummary;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * c=9972 — User Balance History.
 * Returns paginated VIN balance feed for one user, categorized, plus full-range summary.
 * Spec: docs/superpowers/specs/2026-05-14-user-balance-history-api-design.md
 *
 * Error codes (per project convention):
 *   1001 unauthorized          1002 user not found
 *   4001 missing required      4002 invalid format       4003 invalid range
 *   4004 range exceeded        4005 invalid category     4006 identifier mismatch
 *   9999 internal
 */
public class UserBalanceHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_LOOKBACK_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 365;
    // Hard cap on page index: prevents Mongo skip from scanning a huge prefix
    // of the index on a pathological page=10_000_000 request.
    private static final int MAX_PAGE = 10_000;
    private static final TimeZone TZ_SEOUL = TimeZone.getTimeZone("Asia/Seoul");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        long startNs = System.nanoTime();
        JSONObject response = new JSONObject();
        try {
            // ── Admin auth — must come first; reading another user's wallet
            //    history is PII + financial data.
            String actor = AdminUserSupport.requireAdmin(request, response);
            if (actor == null) {
                return response.toString();
            }

            String nn = trimToNull(request.getParameter("nn"));
            String uidParam = trimToNull(request.getParameter("uid"));
            if (nn == null && uidParam == null) {
                return errorResponse(response, "4001", "Thiếu nickname hoặc user_id");
            }

            // ── Date range — validate BEFORE user lookup so invalid params
            //    return early regardless of whether the user exists ─────────
            String from = trimToNull(request.getParameter("from"));
            String to   = trimToNull(request.getParameter("to"));
            SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
            dayFmt.setTimeZone(TZ_SEOUL);
            Calendar cal = Calendar.getInstance(TZ_SEOUL);
            Date today = cal.getTime();
            if (to == null) to = dayFmt.format(today);
            if (from == null) {
                cal.add(Calendar.DAY_OF_MONTH, -DEFAULT_LOOKBACK_DAYS);
                from = dayFmt.format(cal.getTime());
            }
            Date fromDate, toDate;
            try {
                fromDate = dayFmt.parse(from);
                toDate   = dayFmt.parse(to);
            } catch (Exception e) {
                return errorResponse(response, "4002", "Định dạng ngày không hợp lệ (yyyy-MM-dd)");
            }
            if (fromDate.after(toDate)) {
                return errorResponse(response, "4003", "Khoảng thời gian không hợp lệ");
            }
            long rangeDays = (toDate.getTime() - fromDate.getTime()) / (1000L * 60 * 60 * 24);
            if (rangeDays > MAX_RANGE_DAYS) {
                return errorResponse(response, "4004", "Tối đa " + MAX_RANGE_DAYS + " ngày 1 lần truy vấn");
            }
            // Expand to full-day boundaries
            String fromTs = from + " 00:00:00";
            String toTs   = to   + " 23:59:59";

            // ── Category filter — also validated before user lookup ─────
            String catCsv = trimToNull(request.getParameter("category"));
            Set<BalanceHistoryCategory> categories = BalanceHistoryCategoryMapper.parseCsv(catCsv);
            if (categories == null) {
                return errorResponse(response, "4005", "Category không hợp lệ: " + catCsv);
            }

            LogMoneyUserDao dao = new LogMoneyUserDaoImpl();

            // ── Resolve target user ─────────────────────────────────────
            UserModel target = null;
            try {
                if (nn != null) {
                    target = dao.getUserByNickName(nn);
                }
                if (target == null && uidParam != null) {
                    // uid is the numeric users.id column; reject non-numeric early.
                    long uidLong;
                    try {
                        uidLong = Long.parseLong(uidParam);
                    } catch (NumberFormatException nfe) {
                        return errorResponse(response, "4002", "uid không hợp lệ (phải là số)");
                    }
                    target = dao.getUserById(uidLong);
                }
            } catch (Exception lookupEx) {
                logger.warn("balance-history: user lookup failed nn=" + nn + " uid=" + uidParam, lookupEx);
            }
            if (target == null) {
                return errorResponse(response, "1002", "Không tìm thấy user");
            }
            String resolvedNick = target.getNickname();
            if (nn != null && uidParam != null && !nn.equals(resolvedNick)) {
                return errorResponse(response, "4006", "nickname và user_id không khớp");
            }

            // ── Pagination ─────────────────────────────────────────────
            int page  = parseIntOr(request.getParameter("page"),  1);
            int limit = parseIntOr(request.getParameter("limit"), DEFAULT_LIMIT);
            if (page  < 1) page  = 1;
            if (page  > MAX_PAGE) page  = MAX_PAGE;
            if (limit < 1) limit = DEFAULT_LIMIT;
            if (limit > MAX_LIMIT) limit = MAX_LIMIT;

            // ── Query ──────────────────────────────────────────────────
            List<BalanceHistoryRow> rows = dao.queryBalanceHistory(
                    resolvedNick, fromTs, toTs, categories, page, limit);
            long total = dao.countBalanceHistory(
                    resolvedNick, fromTs, toTs, categories);
            // Per spec line 100: summary aggregates across the full date range,
            // ignoring the category filter (so the card always shows the full story).
            BalanceHistorySummary summary = dao.summarizeBalanceHistory(
                    resolvedNick, fromTs, toTs);

            // ── Build response payload ─────────────────────────────────
            JSONArray list = new JSONArray();
            for (BalanceHistoryRow r : rows) {
                r.nickname = resolvedNick;
                list.put(r.toJson());
            }

            JSONObject pagination = new JSONObject();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", total);
            pagination.put("total_pages", limit == 0 ? 0 : (long) Math.ceil(total / (double) limit));

            // Build the top-level response directly (same pattern as LogMoneyUserVinProcessor)
            // so that org.json's JSONObject.toString() is responsible for serialization end-to-end,
            // avoiding a BaseResponse Jackson round-trip that misrepresents nested JSONObjects.
            // Keep list/pagination/summary nested under "data" for API contract compliance.
            JSONObject data = new JSONObject();
            data.put("list", list);
            data.put("pagination", pagination);
            data.put("summary", summary.toJson());

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", "");
            response.put("data", data);

            long durMs = (System.nanoTime() - startNs) / 1_000_000L;
            logger.info("balance-history actor=" + actor + " user=" + resolvedNick
                    + " from=" + from + " to=" + to
                    + " cat=" + catCsv + " rows=" + rows.size()
                    + " total=" + total + " dur_ms=" + durMs);

            return response.toString();
        } catch (Exception e) {
            logger.error("balance-history failed", e);
            return errorResponse(response, "9999", "Lỗi truy vấn dữ liệu");
        }
    }

    private static String errorResponse(JSONObject response, String code, String message) {
        // Reset any partial state from a prior put() before we return an error.
        response.put("success", false);
        response.put("errorCode", code);
        response.put("message", message);
        if (response.has("data")) {
            response.remove("data");
        }
        return response.toString();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int parseIntOr(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
