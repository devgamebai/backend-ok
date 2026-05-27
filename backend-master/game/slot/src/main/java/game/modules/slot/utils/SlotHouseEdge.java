package game.modules.slot.utils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.rtp.RtpResolver;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import game.GameConfig.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Dynamic house edge controller for 3x3 slot games.
 *
 * Hai lớp kiểm soát, hoạt động độc lập:
 *
 * Lớp 1 – RTP từ Admin CMS (Hazelcast "cacheGameWinRate"):
 *   Admin set win_rate = 85 → máy slot trả lại 85% cho user theo thời gian.
 *   Cụ thể: force lose probability = (100 - RTP%) / 100.
 *   Ví dụ: RTP=85 → force lose 15% số vòng quay.
 *
 * Lớp 2 – Bảo vệ Quỹ (fund safety guard):
 *   Dù admin set RTP bao cao, nếu quỹ đang cạn kiệt thì engine
 *   vẫn ép thua mạnh hơn để bảo vệ khả năng tồn tại của nhà cái.
 *   Hai lớp kết hợp bằng OR: nếu một trong hai muốn force lose → force lose.
 *
 * Lớp 3 – capPrize() (không thể override):
 *   Chốt cuối cùng: dù engine quyết định thắng, nếu giải thưởng > 30% quỹ → huỷ thưởng.
 *
 * Thứ tự gọi đúng trong game core:
 *   1. boolean lose = shouldForceLose(gameId, fund, fundJackpot, totalBet, betValue)
 *   2. Nếu (!lose) → tính prize → prize = capPrize(prize, fund)
 *
 * Also adjusts winRate multipliers at startup to target ~92% theoretical RTP.
 *
 * Phase 3 addition: {@link #shouldForceLoseWithPct} layers runtime RTP config
 * (RtpResolver) on top of the fund guard. When SLOT_USE_DYNAMIC_RTP=1, users
 * with a configured pct below the 92% baseline get proportional extra
 * force-lose probability on every spin. Kill switch leaves original behaviour.
 */
public class SlotHouseEdge {

    // Không dùng shared static Random nữa — ThreadLocalRandom.current() nhanh hơn ~5x
    // và không có contention giữa nhiều thread slot

    // Game ID dùng để đọc từ Hazelcast cache
    public static final String GAME_ID_SLOT3X3   = "slot3x3";
    public static final String GAME_ID_MINIPOKER  = "minipoker";

    // Default RTP nếu chưa cấu hình trên CMS (~92% built-in)
    private static final double DEFAULT_RTP = 92.0;

    /** Baseline RTP that BALANCED_WIN_RATE targets — used as reference for delta calc. */
    public static final double BASELINE_PCT = 92.0d;

    /** Max extra force-lose % applied — sanity cap to avoid brutal experience. */
    private static final int MAX_EXTRA_FORCE_LOSE_PCT = 60;

    // Balanced winRate: targets ~92% RTP before fund checks
    // Index: 0=jackpot(-1), 1=sym1_3match, 2=sym2_3match, 3=sym3_3match,
    //        4=sym4_3match, 5=sym4_2match, 6=sym5_3match, 7=sym5_2match
    public static final int[] BALANCED_WIN_RATE = {-1, 150, 80, 40, 15, 2, 8, 1};

    /**
     * Initialize the global slot3x3GameConfig with balanced winRate.
     * Call once during module init.
     */
    public static void initBalancedConfig() {
        try {
            Object config = GameConfig.getInstance().slot3x3GameConfig;
            if (config != null) {
                java.lang.reflect.Field f = config.getClass().getField("winRate");
                f.set(config, BALANCED_WIN_RATE.clone());
            }
        } catch (Exception e) {
            // Fallback: config not available, use engine defaults
        }
    }

    /**
     * [MAIN] Force-lose check — truyền userId để áp dụng per-user override.
     *
     * @param userId      ID người chơi (0 = không có user context, chỉ dùng game default)
     * @param gameId      mã game: "galaxy", "dragonball", "slot3x3", "minipoker"
     * @param fund        số tiền quỹ hiện tại
     * @param fundJackpot quỹ jackpot
     * @param totalBet    tổng cược của vòng quay này
     * @param betValue    giá trị cược cơ bản
     * @return true nếu vòng quay này bị ép thua
     */
    public static boolean shouldForceLose(long userId, String gameId, long fund, long fundJackpot, long totalBet, int betValue) {
        if (totalBet <= 0) return false;

        // ── Lớp 1: RTP từ RtpResolver (user override → game default → hardcode) ──────────
        double rtpPercent;
        if (System.getProperty("MOCK_RTP_PCT") != null) {
            rtpPercent = Double.parseDouble(System.getProperty("MOCK_RTP_PCT"));
        } else {
            rtpPercent = RtpResolver.effectivePct(userId, gameId);
        }
        // Clamp [10, 99] để tránh admin set 0 hoặc 100 gây edge case
        rtpPercent = Math.max(10.0, Math.min(99.0, rtpPercent));
        double forceLoseRate = (100.0 - rtpPercent) / 100.0;
        int roll1 = ThreadLocalRandom.current().nextInt(1000); // precision 0.1%
        boolean rtpForceLose = roll1 < (int)(forceLoseRate * 1000);

        // ── Lớp 2: Fund protection ────────────────────────────────────────────
        boolean fundForceLose = shouldForceLoseByFundOnly(fund, totalBet);

        // ── Kết hợp: OR (một trong hai muốn ép → ép) ─────────────────────────
        return rtpForceLose || fundForceLose;
    }

    /**
     * [BACKWARD COMPAT] Overload không có userId — dùng game default.
     */
    public static boolean shouldForceLose(String gameId, long fund, long fundJackpot, long totalBet, int betValue) {
        return shouldForceLose(0L, gameId, fund, fundJackpot, totalBet, betValue);
    }

    /**
     * [BACKWARD COMPAT] Overload cũ - gameId mặc định "slot3x3".
     */
    public static boolean shouldForceLose(long fund, long fundJackpot, long totalBet, int betValue) {
        return shouldForceLose(0L, GAME_ID_SLOT3X3, fund, fundJackpot, totalBet, betValue);
    }

    /**
     * [PHASE 3c] Pure RTP-only force-lose — không có Fund Guard layer.
     * Dùng cho các room Slot7IconWild (Audition, Avenger, Benley, Bikini, Candy, ChiemTinh,
     * MayBach, RangeRover, RollRoy, Spartan, TamHung, ThanBai, ThanDen, ThanTai)
     * vì các room này dùng chung quỹ hoặc không track fund riêng biệt.
     *
     * Jackpot (result==3 hoặc ==4) PHẢI được exempt trước khi gọi hàm này.
     *
     * @param username người chơi (để resolve per-user override từ cache)
     * @param gameCode mã game: "slot_audition", "slot_avenger", ... (phải khớp game_rtp_config)
     * @return true = ép thua vòng quay này (re-roll matrix)
     */
    public static boolean shouldForceLoseByPctOnly(String username, String gameCode) {
        double rtpPercent;
        if (System.getProperty("MOCK_RTP_PCT") != null) {
            rtpPercent = Double.parseDouble(System.getProperty("MOCK_RTP_PCT"));
        } else {
            long userId = 0L;
            try {
                userId = lookupUserId(username);
            } catch (Exception ignored) {}
            rtpPercent = RtpResolver.effectivePct(userId, gameCode);
        }
        // RtpResolver trả -1.0 khi game bị disable → block tất cả spins
        if (rtpPercent < 0) return true;
        rtpPercent = Math.max(10.0, Math.min(99.0, rtpPercent));
        double forceLoseRate = (100.0 - rtpPercent) / 100.0;
        int roll = ThreadLocalRandom.current().nextInt(1000);
        return roll < (int)(forceLoseRate * 1000);
    }

    /**
     * Cap the prize based on fund health. Lớp 3 - KHÔNG THỂ override bởi RTP config.
     * Prevents wins larger than 30% of the fund in a single spin.
     *
     * @param prize calculated prize amount
     * @param fund  current fund
     * @return capped prize (0 if prize exceeds safe threshold)
     */
    public static long capPrize(long prize, long fund) {
        if (prize <= 0) return prize;
        long maxPayout = fund * 30 / 100;
        if (maxPayout < 0) maxPayout = 0;
        if (prize > maxPayout) {
            return 0; // Force lose — prize too large for current fund
        }
        return prize;
    }

    /**
     * Phase 3 — fund guard PLUS runtime RTP config.
     *
     * First applies the fund-ratio force-lose check. If that fires, returns true
     * (fund guard is always in force). Otherwise, if dynamic RTP is enabled and
     * the user's effective pct is below the 92% baseline, applies proportional
     * extra force-lose probability:
     *   extraPct = min(60, 92 - effectivePct)
     *
     * Example: configured pct=80 → 12% extra force-lose per spin on top of fund guard.
     *
     * Safe fallbacks everywhere — any lookup failure reverts to legacy behaviour.
     */
    public static boolean shouldForceLoseWithPct(long fund, long fundJackpot, long totalBet, int betValue,
                                                  String userName, String gameCode) {
        // Fund guard always runs first
        if (shouldForceLoseByFundOnly(fund, totalBet)) return true;

        // Feature flag: keep legacy behaviour unless dynamic RTP is on
        if (!isDynamicRtpEnabled()) return false;

        if (userName == null || userName.isEmpty() || gameCode == null || gameCode.isEmpty()) return false;

        double pct;
        try {
            long userId = lookupUserId(userName);
            pct = RtpResolver.effectivePct(userId, gameCode);
        } catch (Throwable t) {
            return false; // any lookup failure → legacy behaviour
        }

        double delta = BASELINE_PCT - pct;
        if (delta <= 0) return false; // pct >= baseline → no extra force-lose

        int extraPct = (int) Math.min(MAX_EXTRA_FORCE_LOSE_PCT, Math.round(delta));
        if (extraPct <= 0) return false;

        int roll = ThreadLocalRandom.current().nextInt(100);
        return roll < extraPct;
    }

    /**
     * Universal one-liner for slot rooms with different control flow.
     * Returns 0 (force-lose) or the original prize. Protects jackpot (result==3)
     * and mega-jackpot (result==4) wins — never deny those to avoid frustration.
     *
     * Typical use in the hot path:
     *   totalPrizes = SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_xxx");
     *
     * Only fires when SLOT_USE_DYNAMIC_RTP=1 AND user's configured pct &lt; baseline.
     */
    public static long maybeForceLose(long prize, int result, String userName, String gameCode) {
        if (prize <= 0L || result == 3 || result == 4) return prize;
        return shouldForceLoseByPctOnly(userName, gameCode) ? 0L : prize;
    }

    /**
     * Pct-only force-lose — for slot rooms that have their own fund-guard loop
     * (e.g. KhoBau, VQV, SAH, NDV). Does NOT call shouldForceLose, so fund logic
     * stays untouched. Returns true when the user's configured pct is below baseline
     * and the random roll fires.
     *
     * Typical use: after computing totalPrize in the room loop, if this returns true,
     * zero the prize + reset result to lose.
     */
    // Duplicate shouldForceLoseByPctOnly removed — already defined at line 140

    /** Resolve a username to its numeric user_id via the shared Hazelcast "users" cache. */
    private static long lookupUserId(String userName) {
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            if (hz == null) return -1L;
            IMap<String, UserCacheModel> userMap = hz.getMap("users");
            UserCacheModel u = userMap.get(userName);
            return u != null ? u.getId() : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * Fund-only guard shared by legacy and pct-aware flows.
     * This intentionally does not read any RTP config.
     */
    private static boolean shouldForceLoseByFundOnly(long fund, long totalBet) {
        if (fund <= 0 || totalBet <= 0) return false;

        long fundRatio = fund / totalBet;
        int roll = ThreadLocalRandom.current().nextInt(100); // 0-99

        if (fundRatio < 5) {
            return roll < 90; // CRITICAL: 90% force lose
        } else if (fundRatio < 20) {
            return roll < 70; // LOW: 70%
        } else if (fundRatio < 50) {
            return roll < 40; // MODERATE: 40%
        } else if (fundRatio < 100) {
            return roll < 20; // NORMAL: 20% slight edge
        }

        // fundRatio >= 100 → quỹ rất healthy → lớp này không ép
        return false;
    }

    /** Feature flag — env SLOT_USE_DYNAMIC_RTP=1 enables pct-aware force-lose. */
    private static boolean isDynamicRtpEnabled() {
        String v = System.getenv("SLOT_USE_DYNAMIC_RTP");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}
