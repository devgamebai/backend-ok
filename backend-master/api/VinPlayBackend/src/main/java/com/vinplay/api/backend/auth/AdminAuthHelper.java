package com.vinplay.api.backend.auth;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Admin token management using Hazelcast cacheToken map.
 * Uses the SAME map as player tokens so processors that already check
 * cacheToken internally (c=9610-9615, c=104, c=105) will accept admin tokens too.
 *
 * Token format in cacheToken: token → userName (same as player tokens: token → nickname)
 */
public class AdminAuthHelper {

    private static final Logger logger = Logger.getLogger("backend");
    private static final String TOKEN_MAP = "cacheToken";
    private static final String ADMIN_SESSION_MAP = "adminSessions"; // userName → current token
    private static final long TOKEN_TTL_HOURS = 8;

    // ─── Auth bypass config ───────────────────────────────────────────────────
    //
    // Explicit list of command IDs that do NOT require an admin token.
    // These are either public system endpoints or agency-dashboard commands
    // that route through /api_backend (FE workaround — see SUN-767).
    //
    // RULE: add a command here ONLY if it is genuinely agent/agency-facing.
    //       Admin-panel commands (RBAC enforced via SUN-907) must NOT appear here.
    //       When in doubt, leave it out — agents can always call /api_agent instead.
    //
    private static final Set<Integer> NO_AUTH_COMMANDS = new HashSet<>(Arrays.asList(

        // ── Service-to-service bridges (own auth — GitLab #31) ────────────────
        // Each processor MUST validate its own auth (hash / X-Service-Token).
        // Adding here only bypasses the global admin-token middleware.
        8797,   // UpdateMoneyUserBanCaProcessor   — hash h=md5(nn+mn+BANCA_XXENG_SECRET)
        9854,   // LogBetCommissionProcessor       — X-Service-Token header (BANCA_SERVICE_TOKEN)
        9998,   // BanCaSettleProcessor            — X-Service-Token header (BANCA_SERVICE_TOKEN) Phase 5b
        9997,   // BanCaBalanceQueryProcessor      — X-Service-Token header (BANCA_SERVICE_TOKEN) Phase 5c follow-up

        // ── System / login (always public, no auth by design) ────────────────
        108,    // GetCCUProcessor              — get CCU
        1080,   // GetCCUFilteredProcessor      — get CCU filtered
        124,    // CaptchaProcessor             — get Captcha (public for login)
        701,    // LoginAdminProcessor          — login admin (legacy c=701)
        9901,   // LoginAdminProcessor          — login admin (new c=9901)
        9904,   // LogAdminActionProcessor      — log admin action (fire-and-forget)
        9428,   // LoginAgentProcessor          — agent login

        // ── Legacy agent commands (pre-94xx era) ─────────────────────────────
        // Written before the agency auth system. Verified: no admin token check.
        31,     // InsertAgencyProcessor              — check mã đại lý
        75,     // ReportAgencyProfitProcessor        — report lợi nhuận agency
        103,    // UpdateStatusDailybyNickNameProcessor — update trạng thái đại lý
        106,    // SearchAgentTranferMoneyProcessor    — lịch sử chuyển khoản agent
        107,    // SearchAgentTranferProcessor         — tổng chuyển khoản agent
        118,    // SearchAgentTranferMoneyStatusProcessor — lịch sử mua bán vin đại lý
        127,    // UpdateTopDsFromAgentProcessor       — update top doanh số từ agent
        139,    // SeachAgentTranferMoneyByNickNameProcessor — tìm chuyển khoản agent theo nick
        143,    // FreezeMoneyTranferAgentProcessor    — phong toả tiền chuyển khoản agent
        144,    // GetListFreezeMoneyTranferAgentProcessor — ds phong toả tiền agent
        145,    // RestoreFreezeMoneyTranferAgentProcessor — khôi phục phong toả agent
        155,    // SearchAgentTongTranferMoneyProcessor — lịch sử chuyển khoản đại lý tổng
        156,    // ShowPercentBonusVincardProcessor    — tỉ lệ thưởng vincard của đại lý
        311,    // ExportGiftCodeAgentProcessor        — xuất giftcode đại lý
        712,    // LogRefundFeeAgentProcessor          — log hoàn phí agent

        // ── Legacy 88xx agent commands ────────────────────────────────────────
        8826,   // ListAgentProcessor               — danh sách agent
        8827,   // ListUserOfAgentProcessor         — danh sách user của agent
        8828,   // DetailUserOfAgentProcessor       — chi tiết user của agent
        8829,   // ListChildAgentProcessor          — danh sách agent con
        8830,   // ListMemberOfAgentProcessor       — danh sách member của agent
        8840,   // CountUserInAgentProcessor        — đếm user trong agent
        8850,   // SearchLogUserDetailProccessor    — chi tiết log user của agent

        // ── Agency core: deposit / withdraw / wallet / reports ───────────────
        9400,   // SearchDepositAgentProcessor          — search deposit cho agent
        9401,   // InsertDepositAgentProcessor          — tạo lệnh deposit
        9402,   // UpdateStatusDepositAgentProcessor    — cập nhật trạng thái deposit
        9403,   // SearchWithdrawAgentProcessor         — search withdraw của agent
        9404,   // InsertWithdrawAgentProcessor         — tạo lệnh withdraw
        9405,   // UpdateStatusWithdrawAgentProcessor   — cập nhật trạng thái withdraw
        9406,   // GetDetailDepositAgentProcessor       — chi tiết deposit
        9407,   // GetDetailWithdrawAgentProcessor      — chi tiết withdraw
        9408,   // GetAgentInfoByCodeProcessor          — lấy info agent theo code
        9409,   // GetListAgentWithdrawProcessor        — danh sách withdraw của agent
        9410,   // InsertAgentBankUserProcessor         — thêm ngân hàng agent
        9411,   // GetAgentBankUserProcessor            — lấy ngân hàng agent
        9412,   // DeleteAgentBankUserProcessor         — xoá ngân hàng agent
        9413,   // UpdateAgentBankUserProcessor         — cập nhật ngân hàng agent
        9414,   // GetAgentInfoProcessor                — lấy info agent
        9415,   // GetListAgentProcessor                — danh sách agent
        9416,   // GetListAgentByLevelProcessor         — danh sách agent theo cấp
        9420,   // SearchDepositAgentAdminProccessor    — search deposit (admin agent)
        9421,   // InsertDepositAgentAdminProccessor    — tạo deposit (admin agent)
        9422,   // UpdateStatusDepositAgentAdminProccessor — update status deposit (admin agent)
        9423,   // SearchWithdrawAgentAdminProccessor   — search withdraw (admin agent)
        9424,   // InsertWithdrawAgentAdminProccessor   — tạo withdraw (admin agent)
        9425,   // UpdateStatusWithdrawAgentAdminProccessor — update status withdraw (admin agent)
        9426,   // GetDetailDepositAgentAdminProccessor — chi tiết deposit (admin agent)
        9427,   // GetDetailWithdrawAgentAdminProccessor — chi tiết withdraw (admin agent)
        9430,   // GetInfoUserAgentProccessor           — lấy info user của agent
        9431,   // GetListUserAgentProccessor           — danh sách user của agent
        9432,   // GetListUserDepositAgentProccessor    — danh sách user nạp của agent
        9433,   // GetListUserWithdrawAgentProccessor   — danh sách user rút của agent
        9434,   // GetListUserGameAgentProccessor       — danh sách user chơi game
        9440,   // GetAgentConfigBankProcessor          — config ngân hàng agent
        9441,   // InsertAgentConfigBankProcessor       — thêm config ngân hàng
        9442,   // UpdateAgentConfigBankProcessor       — cập nhật config ngân hàng
        9443,   // DeleteAgentConfigBankProcessor       — xoá config ngân hàng
        9444,   // SearchListAgentConfigBankProcessor   — tìm kiếm config ngân hàng
        9450,   // SearchLogMoneyAgentProcessor         — log biến động ví agent
        9451,   // GetDetailLogMoneyAgentProcessor      — chi tiết log biến động ví
        9452,   // GetTotalLogMoneyAgentProcessor       — tổng log biến động ví
        9455,   // SearchDepositUserToAgentAdminProccessor — search deposit user cho agent (admin)
        9456,   // GetDetailDepositUserToAgentAdminProccessor — chi tiết deposit user cho agent
        9457,   // SearchWithdrawUserToAgentAdminProccessor — search withdraw user cho agent
        9458,   // SearchWithdraw4AgentAdminProccessor  — search withdraw cho admin agent
        9459,   // SearchWithdraw4BOProccessor          — search withdraw cho BO
        9460,   // GetAgencyWalletProcessor             — lấy số dư ví agency
        9461,   // UpdateInfoAgentProcessor             — cập nhật info agent
        9462,   // TopupUser4AgentAdminProccessor       — nạp tiền cho user (cash)
        9464,   // SendOtp4AgentProcessor               — gửi OTP cho agent topup
        9465,   // SearchDepositUserToAgentProccessor   — search deposit user tới agent
        9466,   // GetTotalTransactionAgentProccessor   — tổng giao dịch của agent
        9467,   // UpdateDepositUserToAgentProccessor   — cập nhật comment withdraw
        9468,   // GetTransactionAgentProccessor        — tất cả giao dịch của agent
        9469,   // GetAgentBalanceProccessor            — số dư agent
        9470,   // ReferentCodeProcessor                — referent code
        9471,   // ListNewUserOfUserAgentProcessor      — danh sách user mới của agent
        9480,   // SearchSendMailProcessor              — search send mail content
        9481,   // CreateSendMailProcessor              — tạo send mail content
        9482,   // UpdateSendMailProcessor              — cập nhật send mail content
        9483,   // DeleteSendMailProcessor              — xoá send mail content
        9484,   // GetSendMailProcessor                 — lấy send mail content
        9490,   // SearchLiveUserGameProcessor          — search live user game
        9491,   // CreateLiveUserGameProcessor          — tạo live user game
        9492,   // UpdateLiveUserGameProcessor          — cập nhật live user game
        9493,   // DeleteLiveUserGameProcessor          — xoá live user game
        9494,   // HistoryLiveUserDepositProcessor      — history live user deposit
        9495,   // CreateLiveUserDepositProcessor       — tạo live user deposit
        9496,   // GetLiveUserGameProcessor             — lấy live user game
        9500,   // SearchEventMissionProcessor          — search event mission
        9501,   // AddEventMissionProcessor             — thêm event mission
        9502,   // UpdateEventMissionProcessor          — cập nhật event mission
        9503,   // DeleteEventMissionProcessor          — xoá event mission
        9504,   // GetEventMissionProcessor             — lấy event mission
        9505,   // GetEventMissionAvailableProcessor    — lấy event mission available
        9510,   // SearchMissionProcessor               — search mission
        9511,   // AddMissionProcessor                  — thêm mission
        9512,   // UpdateMissionProcessor               — cập nhật mission
        9513,   // DeleteMissionProcessor               — xoá mission
        9514,   // GetMissionProcessor                  — lấy mission
        9515,   // GetGameProcessor                     — lấy game
        9516,   // ListLiveUserProcessor                — danh sách live users
        9517,   // UpdateIsLiveProcessor                — update is_live cho user
        9537,   // ReportGeneral4AgencyProcessor        — báo cáo tổng quan agency
        9538,   // ChartDepositWithdraw4AgencyProcessor — biểu đồ nạp rút agency
        9539,   // TodayReport4AgencyProcessor          — báo cáo hôm nay
        9540,   // GetRebateConfig4AgencyProcessor      — config hoa hồng agency
        9541,   // GetRebateLogs4AgencyProcessor        — lịch sử hoa hồng agency
        9542,   // SearchLogMoney4AgencyProcessor       — log biến động ví downline
        9543,   // DetailMemberOfAgencyProcessor        — chi tiết thành viên downline
        9544,   // PartnerSettlement4AgencyProcessor    — quyết toán partner theo ngày
        9545,   // SearchGame3rdBettingHistory4AgencyProcessor — lịch sử cược 3rd-party agency
        9546,   // TopPartners4AgencyProcessor          — top partners của agency

        // ── Rebate / Rolling commission module ────────────────────────────────
        9740,   // SearchUsersProcessor             — tìm kiếm users
        9741,   // UserGameListProcessor            — danh sách game của user
        9742,   // UserAggregateProcessor           — tổng hợp user
        9750,   // ListRebateDashboardProcessor     — dashboard rebate
        9751,   // GetRebateDetailProcessor         — chi tiết rebate
        9752,   // GetRebateLogsProcessor           — lịch sử rebate
        9753,   // TriggerRebatePayoutProcessor     — trigger payout rebate
        9754,   // BatchRebatePayoutProcessor       — batch payout rebate
        9755,   // UpdateRebateConfigProcessor      — cập nhật config rebate
        9756,   // GetRebateConfigProcessor         — lấy config rebate
        9757,   // ListRebateConfigProcessor        — danh sách config rebate
        9758,   // CalcDifferentialCommissionProcessor — tính hoa hồng vi sai
        9759,   // RevertRebateLogProcessor         — hoàn lại rebate log
        9760,   // TrackDeviceInstallProcessor      — track device install
        9761,   // GetSigningBonusConfigProcessor   — config signing bonus
        9762,   // UpdateSigningBonusConfigProcessor — cập nhật signing bonus
        9763,   // ListSigningBonusLogProcessor     — lịch sử signing bonus
        9764,   // ManualPayoutSigningBonusProcessor — payout thủ công signing bonus
        9765,   // ToggleUserSigningBonusProcessor  — bật/tắt signing bonus user
        9766,   // CheckAbusePatternsProcessor      — kiểm tra abuse patterns
        9767,   // TriggerCommissionCronProcessor   — trigger commission cron
        9770,   // ListGameRtpConfigProcessor       — danh sách game rtp config

        // ── Agency extended: code requests & agent management ─────────────────
        9830,   // RequestAgentCodeProcessor            — yêu cầu agent code
        9831,   // ListMyCodeRequestsProcessor          — danh sách yêu cầu của tôi
        9832,   // ListPendingCodeRequestsProcessor     — danh sách yêu cầu pending
        9833,   // ApproveCodeRequestProcessor          — duyệt yêu cầu
        9834,   // RejectCodeRequestProcessor           — từ chối yêu cầu
        9835,   // ListReservedCodesProcessor           — danh sách code đã đặt
        9836,   // AddReservedCodeProcessor             — thêm code đặt trước
        9837,   // DeleteReservedCodeProcessor          — xoá code đặt trước
        9838,   // ListAllPlayersUnderAgentProcessor    — danh sách player dưới agent
        9839,   // ListAllAgentsUnderAgentProcessor     — danh sách agent dưới agent
        9840,   // PromotePlayerToAgentProcessor        — nâng player lên agent

        // ── Agency extended: betting, settlement, commission ──────────────────
        9843,   // GetBettingHistoryByAgentProcessor        — lịch sử cược theo agent
        9844,   // GetAgentSettlementProcessor              — quyết toán agent
        9845,   // ListOnlineOfflineMembersProcessor        — ds thành viên online/offline
        9846,   // GetDepositCommissionLogs4AgencyProcessor — log hoa hồng nạp tiền
        9848,   // ListGameCommissionConfigProcessor        — config hoa hồng game
        9849,   // SetGameCommissionRateProcessor           — thiết lập tỉ lệ hoa hồng

        // ── Agency extended: wallet, hierarchy, GSC, AWC ─────────────────────
        9890,   // WithdrawAgencyWalletProcessor    — chuyển ví agency sang ví game (1:1)
        9891,   // GetAgencyWalletHistoryProcessor  — lịch sử giao dịch ví agency
        9892,   // GetAgentHierarchyTreeProcessor   — cây phân cấp agent
        9893,   // ListGSCProvidersProcessor        — danh sách GSC providers
        9894,   // ListGSCGamesProcessor            — danh sách game GSC theo provider
        9895,   // GetAwcBetHistoryProcessor        — lịch sử cược AWC

        // ── CreditWallet agent-side ───────────────────────────────────────────
        // NOTE: 9921 (AdminTopup) và 9924 (AdminRevoke) là admin-only → không có ở đây
        9920,   // GetCreditWalletProcessor         — xem số dư credit wallet
        9922,   // AgentTransferCreditProcessor     — chuyển credit wallet agent → agent
        9923,   // AgentCreditDepositProcessor      — nạp credit wallet vào ví game
        9925    // GetCreditWalletHistoryProcessor  — lịch sử giao dịch credit wallet

    ));

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this command requires a valid admin token (+ RBAC).
     * Returns {@code false} for public system endpoints and all agency-dashboard commands.
     *
     * Only called for requests arriving at /api_backend (see VinPlayBackendMain).
     * Requests to /api_agent skip this check entirely.
     */
    public static boolean requiresAuth(int commandId) {
        return !NO_AUTH_COMMANDS.contains(commandId);
    }

    /**
     * Generate a new admin token and store in Hazelcast cacheToken map.
     * Stored as token → userName (same format as player tokens → nickname).
     */
    public static String generateToken(int adminId, String userName) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap(TOKEN_MAP);
            IMap<String, String> sessionMap = hz.getMap(ADMIN_SESSION_MAP);

            // Single-session: invalidate old token for this user
            String oldToken = sessionMap.get(userName);
            if (oldToken != null) {
                tokenMap.remove(oldToken);
                logger.info("Admin session kicked for " + userName + " (old token removed)");
            }

            // SUN-816: sliding idle TTL — active admin sessions don't silently evict.
            // ttl = absolute cap (8h), maxIdle = 3h (reset on every tokenMap.get).
            tokenMap.put(token, userName,
                    TOKEN_TTL_HOURS, TimeUnit.HOURS,
                    180L, TimeUnit.MINUTES);
            sessionMap.put(userName, token, TOKEN_TTL_HOURS, TimeUnit.HOURS);
            logger.info("Admin token created for " + userName + " (adminId=" + adminId + ")");
        } catch (Exception e) {
            logger.error("Failed to store admin token in Hazelcast", e);
        }
        return token;
    }

    /**
     * Verify an admin token exists in cacheToken AND its owning admin row is
     * still {@code Status='A'} in MySQL.
     *
     * <p>SUN-1244: re-checking the DB on every verify closes the loophole
     * where an admin disabled mid-session would still hold a valid 8-hour
     * Hazelcast token. The cacheToken IMap has no MapStore — we cannot rely
     * on it auto-flushing — so the next-call DB check is the fastest
     * guaranteed path to invalidate active sessions for accounts that just
     * got moved to {@code Status='D'}. Single short SELECT, indexed lookup
     * by primary-key-equivalent UNIQUE(UserName) — adds ~1ms to admin
     * requests, acceptable for the security gain.
     *
     * <p>Falls open on DB failure (logs + returns true) only if the cache
     * has the token. Belt-and-suspenders: the cache check still has to
     * pass first, so a complete DB outage doesn't grant impostors access —
     * it just lets already-cached real sessions keep working through the
     * outage.
     */
    public static boolean verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap(TOKEN_MAP);
            if (!tokenMap.containsKey(token)) {
                return false;
            }
            String userName = tokenMap.get(token);
            if (userName == null || userName.isEmpty()) {
                return false;
            }
            return isAdminActive(userName);
        } catch (Exception e) {
            logger.error("Failed to verify admin token", e);
            return false;
        }
    }

    /**
     * SUN-1244 helper. Returns true iff the admin row for {@code userName}
     * exists and has {@code Status='A'}. Cached failures (DB hiccup) fall
     * open to "active" because the caller has already passed the cacheToken
     * presence check above — only real impostors with no token can sneak in.
     */
    private static boolean isAdminActive(String userName) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT Status FROM user WHERE UserName = ?")) {
            ps.setString(1, userName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    logger.warn("verifyToken: admin row missing for " + userName);
                    return false;
                }
                String status = rs.getString("Status");
                if (!"A".equalsIgnoreCase(status)) {
                    logger.warn("verifyToken: token rejected — " + userName + " status=" + status);
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("verifyToken: DB check failed (falling open) for " + userName, e);
            return true;
        }
    }

    /**
     * Resolve admin username from token.
     * Returns null when token is missing/invalid.
     */
    public static String getAdminUsernameByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap(TOKEN_MAP);
            return tokenMap.get(token);
        } catch (Exception e) {
            logger.error("Failed to resolve admin username from token", e);
            return null;
        }
    }

    /**
     * Get token expiry time in epoch milliseconds.
     */
    public static long getTokenExpiry() {
        return System.currentTimeMillis() + TOKEN_TTL_HOURS * 3600 * 1000;
    }

    /**
     * Build error response JSON for auth failures.
     */
    public static String errorResponse(String errorCode, String message) {
        return "{\"success\":false,\"errorCode\":\"" + errorCode + "\",\"message\":\"" + message + "\"}";
    }
}
