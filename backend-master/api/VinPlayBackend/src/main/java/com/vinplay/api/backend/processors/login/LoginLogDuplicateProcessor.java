package com.vinplay.api.backend.processors.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.usercore.dao.UserInfoDao;
import com.vinplay.usercore.dao.impl.UserInfoDaoImpl;
import com.vinplay.usercore.model.DuplicateIpGroupRow;
import com.vinplay.usercore.model.DuplicateIpUserRow;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * c=110: List duplicate-IP login groups.
 *
 * <p>Groups user_login_info records by the first client IP (real IP, ignoring
 * CDN edge hops) and returns IPs shared by >= mu distinct users. Adaptive
 * filtering: nn/ip/type apply pre-group. Default 7-day window, 90-day cap,
 * page 50 IPs.
 */
public class LoginLogDuplicateProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger(LoginLogDuplicateProcessor.class);

    private static final TimeZone VN_TZ = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private static final String DATE_FMT = "yyyy-MM-dd";
    private static final String DATETIME_FMT = "yyyy-MM-dd HH:mm:ss";
    private static final int MAX_RANGE_DAYS = 90;
    private static final int MIN_MU = 2;
    private static final int MAX_MU = 50;

    private final UserInfoDao dao = new UserInfoDaoImpl();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        // ── 1. Read raw params ────────────────────────────────────────────────
        String tsParam = request.getParameter("ts");
        String teParam = request.getParameter("te");
        String typeParam = request.getParameter("type");
        String nnParam = request.getParameter("nn");
        String ipParam = request.getParameter("ip");
        String muParam = request.getParameter("mu");
        String pParam = request.getParameter("p");

        try {
            // ── 2. Date defaults and validation ───────────────────────────────
            SimpleDateFormat dateFmt = new SimpleDateFormat(DATE_FMT);
            dateFmt.setTimeZone(VN_TZ);

            Calendar cal = Calendar.getInstance(VN_TZ);
            String today = dateFmt.format(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, -7);
            String sevenDaysAgo = dateFmt.format(cal.getTime());

            String ts = (tsParam == null || tsParam.trim().isEmpty()) ? sevenDaysAgo : tsParam.trim();
            String te = (teParam == null || teParam.trim().isEmpty()) ? today : teParam.trim();

            // Swap silently if ts > te
            if (ts.compareTo(te) > 0) {
                String tmp = ts;
                ts = te;
                te = tmp;
            }

            // 90-day cap check
            Calendar calTs = Calendar.getInstance(VN_TZ);
            Calendar calTe = Calendar.getInstance(VN_TZ);
            try {
                calTs.setTime(dateFmt.parse(ts));
                calTe.setTime(dateFmt.parse(te));
            } catch (java.text.ParseException e) {
                return buildErrorResponse("ERR_INVALID_DATE", "Định dạng ngày không hợp lệ (yyyy-MM-dd)");
            }
            long diffMs = calTe.getTimeInMillis() - calTs.getTimeInMillis();
            long diffDays = diffMs / (1000L * 60 * 60 * 24);

            if (diffDays > MAX_RANGE_DAYS) {
                return buildErrorResponse("ERR_DATE_RANGE_TOO_LARGE",
                        "Khoảng thời gian không được quá 90 ngày");
            }

            // ── 3. Build tsFrom / tsTo as datetime strings ────────────────────
            String tsFrom = ts + " 00:00:00";
            String tsTo = te + " 23:59:59";

            // ── 4. Parse type (optional int) ──────────────────────────────────
            Integer type = null;
            if (typeParam != null && !typeParam.trim().isEmpty()) {
                try {
                    type = Integer.parseInt(typeParam.trim());
                } catch (NumberFormatException e) {
                    // ignore invalid type — treat as no filter
                }
            }

            // ── 5. Parse nn (optional substring, pass raw to DAO) ─────────────
            String nn = (nnParam == null || nnParam.trim().isEmpty()) ? null : nnParam.trim();

            // ── 6. Parse ip (optional exact match) ───────────────────────────
            String ip = (ipParam == null || ipParam.trim().isEmpty()) ? null : ipParam.trim();

            // ── 7. Parse mu (default 2, clamp [2, 50]) ───────────────────────
            int mu = MIN_MU;
            if (muParam != null && !muParam.trim().isEmpty()) {
                try {
                    mu = Integer.parseInt(muParam.trim());
                } catch (NumberFormatException e) {
                    mu = MIN_MU;
                }
            }
            if (mu < MIN_MU) mu = MIN_MU;
            if (mu > MAX_MU) mu = MAX_MU;

            // ── 8. Parse page (default 1, min 1) ─────────────────────────────
            int page = 1;
            if (pParam != null && !pParam.trim().isEmpty()) {
                try {
                    page = Integer.parseInt(pParam.trim());
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }
            if (page < 1) page = 1;

            // ── 8b. Audit who is pulling this PII (admin-only endpoint).
            // The framework auth filter has already verified `aat`; resolve
            // the admin nickname for forensic logging. Read-only query, no
            // _audit table row — INFO log is sufficient.
            String aat = request.getParameter("aat");
            String adminNick = "unknown";
            try {
                com.hazelcast.core.HazelcastInstance hz =
                        com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
                if (hz != null && aat != null) {
                    Object cached = hz.getMap("cacheToken").get(aat);
                    if (cached != null) adminNick = String.valueOf(cached);
                }
            } catch (Throwable ignored) { /* logging only */ }
            logger.info("c=110 USER_LOGIN_LOG_DUPLICATE actor=" + adminNick
                    + " ts=" + tsFrom + " te=" + tsTo
                    + " type=" + type + " nn=" + nn + " ip=" + ip
                    + " mu=" + mu + " p=" + page);

            // ── 9. Query duplicate IP groups ──────────────────────────────────
            List<DuplicateIpGroupRow> groups = dao.findDuplicateIpGroups(
                    tsFrom, tsTo, type, nn, ip, mu, page);

            long totalRecord = groups.isEmpty() ? 0L : groups.get(0).totalRecord;

            // ── 10. Collect IP list for user detail query ─────────────────────
            List<String> ipList = new ArrayList<>();
            for (DuplicateIpGroupRow g : groups) {
                ipList.add(g.firstIp);
            }

            // ── 11. Query per-IP user rows. Pass `type` and `nn` through so
            // the user-detail list matches the userCount reported by the
            // group query (review feedback: filter parity).
            List<DuplicateIpUserRow> users = dao.findUsersForFirstIps(tsFrom, tsTo, ipList, type, nn);

            // ── 12. Group users by firstIp ────────────────────────────────────
            Map<String, List<DuplicateIpUserRow>> usersByIp = new LinkedHashMap<>();
            for (DuplicateIpUserRow u : users) {
                usersByIp.computeIfAbsent(u.firstIp, k -> new ArrayList<>()).add(u);
            }

            // ── 13. Build response JSON ───────────────────────────────────────
            List<Map<String, Object>> groupList = new ArrayList<>();
            for (DuplicateIpGroupRow g : groups) {
                List<Map<String, Object>> userList = new ArrayList<>();
                List<DuplicateIpUserRow> groupUsers = usersByIp.get(g.firstIp);
                if (groupUsers != null) {
                    for (DuplicateIpUserRow u : groupUsers) {
                        Map<String, Object> uMap = new LinkedHashMap<>();
                        uMap.put("user_id", u.userId);
                        uMap.put("user_name", u.userName);
                        uMap.put("nick_name", u.nickName);
                        uMap.put("login_count", u.loginCount);
                        uMap.put("first_login", u.firstLogin);
                        uMap.put("last_login", u.lastLogin);
                        userList.add(uMap);
                    }
                }

                Map<String, Object> gMap = new LinkedHashMap<>();
                gMap.put("ip", g.firstIp);
                gMap.put("user_count", g.userCount);
                gMap.put("first_seen", g.firstSeen);
                gMap.put("last_seen", g.lastSeen);
                gMap.put("users", userList);
                groupList.add(gMap);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", null);
            response.put("data", null);
            response.put("total", groups.size());
            response.put("totalRecord", totalRecord);
            response.put("groups", groupList);
            return OBJECT_MAPPER.writeValueAsString(response);

        } catch (Exception e) {
            // Review feedback: 1001 is reserved for "unauthorized". Internal
            // errors must use 9999 so the admin CMS reports the right banner
            // and the FE can distinguish auth bounces from server faults.
            logger.error("LoginLogDuplicateProcessor error", e);
            try {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("errorCode", "9999");
                err.put("message", "Internal: " + e.getMessage());
                err.put("data", null);
                err.put("total", 0);
                err.put("totalRecord", 0);
                err.put("groups", new ArrayList<>());
                return OBJECT_MAPPER.writeValueAsString(err);
            } catch (Exception jsonEx) {
                return "{\"success\":false,\"errorCode\":\"9999\",\"message\":null,\"data\":null,\"total\":0,\"totalRecord\":0,\"groups\":[]}";
            }
        }
    }

    private String buildErrorResponse(String errorCode, String message) {
        try {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("errorCode", errorCode);
            err.put("message", message);
            err.put("data", null);
            err.put("total", 0);
            err.put("totalRecord", 0);
            err.put("groups", new ArrayList<>());
            return OBJECT_MAPPER.writeValueAsString(err);
        } catch (Exception e) {
            return "{\"success\":false,\"errorCode\":\"" + errorCode + "\",\"message\":null,\"data\":null,\"total\":0,\"totalRecord\":0,\"groups\":[]}";
        }
    }
}
