/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.modules.minigame.utils;

import bitzero.server.util.TaskScheduler;
import com.vinplay.vbee.common.rtp.RtpResolver;
import com.vinplay.vbee.common.config.VBeePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Sinh kết quả xúc xắc cho Tài Xỉu và Bầu Cua.
 *
 * [ĐÃ NÂNG CẤP] Tích hợp Target House Edge từ Admin CMS:
 *
 * Admin set win_rate = 20 (cho game_id "taixiu" trong Hazelcast "cacheGameWinRate")
 * → nghĩa là: nhà cái mục tiêu giữ lại 20% trên tổng chênh lệch cược.
 *
 * Cơ chế:
 *   1. Tính tổng cược Tài (betTai) và tổng cược Xỉu (betXiu) trong phòng.
 *   2. Tính lợi nhuận nếu rớt Tài:  profit_tai = betXiu - betTai * (1 - tax)
 *      Tính lợi nhuận nếu rớt Xỉu:  profit_xiu = betTai - betXiu * (1 - tax)
 *   3. So sánh xem cạnh nào cho profit gần targetHouseEdge% nhất → ép forceBetSide.
 *   4. Nếu không có lệch cược đủ lớn (balanced) → không ép, random tự do.
 *
 * Nếu CMS chưa cấu hình → fallback về logic cũ (random tự do nếu forceBetSide=-1).
 */
public class GenerationTaiXiu {
    private List<String> listCau = new ArrayList<String>();
    private CauTaiXiu cauTX = null;
    private String basePath = VBeePath.basePath;
    public final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    // Game IDs cho Hazelcast lookup
    public static final String GAME_ID_TAIXIU = "taixiu";
    public static final String GAME_ID_SICBO  = "sicbo";

    // DEFAULT khi chưa cài: 0% = không ép house edge, chỉ dùng fund protection tự nhiên
    private static final double DEFAULT_HOUSE_EDGE = 0.0;

    // Ngưỡng chênh lệch tối thiểu để kích hoạt house edge logic (tránh ép khi bet gần như balanced)
    private static final double MIN_IMBALANCE_RATIO = 0.05; // 5% chênh lệch

    public void readConfig() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(basePath.concat("config/taixiu.dat")));
        String str = null;
        while ((str = reader.readLine()) != null) {
            if (str.isEmpty()) continue;
            this.listCau.add(str);
        }
    }

    /**
     * [ORIGINAL] Sinh kết quả dựa trên forceBetSide đã được tính sẵn bên ngoài.
     * forceBetSide = -1 → random tự do.
     * forceBetSide = 0  → ép kết quả ra Xỉu (0=Xỉu, 1=Tài theo convention).
     * forceBetSide = 1  → ép kết quả ra Tài.
     */
    public short[] generateResult(short forceBetSide) {
        short[] dices = this.generateDices(); // init ở đây để đảm bảo không bao giờ uninitialized
        if (forceBetSide != -1) {
            // Ép kết quả: quay xúc xắc đến khi đúng cửa cần (0=Xỉu, 1=Tài)
            int genResult;
            int totalDices;
            while ((genResult = (totalDices = (dices = this.generateDices())[0] + dices[1] + dices[2]) > 10 ? 1 : 0) != forceBetSide) {
            }
        }
        return dices;
    }

    /**
     * [MAIN] Sinh kết quả xúc xắc với Target House Edge từ Admin CMS, có hỗ trợ per-user override.
     *
     * @param userId       ID người chơi (0 = không có user, chỉ dùng game default)
     * @param gameId       "taixiu" hoặc "sicbo"
     * @param totalBetTai  Tổng cược vào cửa Tài
     * @param totalBetXiu  Tổng cược vào cửa Xỉu
     * @param taxRate      Tỉ lệ phí (e.g. 0.05 = 5%)
     * @return short[] dices - 3 xúc xắc kết quả
     */
    public short[] generateResultWithHouseEdge(long userId, String gameId, long totalBetTai, long totalBetXiu, float taxRate) {
        // Lấy target house edge qua RtpResolver (user override → game default → hardcode)
        // Note: win_rate_pct trong RtpResolver = % payback cho user
        // house edge target = 100 - win_rate_pct
        // VD: win_rate=80 → house edge target = 20%
        double winRatePct = RtpResolver.effectivePct(userId, gameId);
        // Nếu win_rate = DEFAULT (92% hoặc 100%) và game chưa cài riêng → fallback DEFAULT_HOUSE_EDGE
        // Distinguish: nếu game config tồn tại (< 99%), dùng house edge = 100 - winRate
        // Nếu fallback về DEFAULT (92%) → dùng DEFAULT_HOUSE_EDGE = 0 (không ép)
        double targetEdgePercent;
        if (winRatePct >= 92.0 && RtpResolver.effectivePct(gameId) >= 92.0) {
            // Chưa cài hướng riêng cho game này → không ép
            targetEdgePercent = DEFAULT_HOUSE_EDGE;
        } else {
            targetEdgePercent = 100.0 - winRatePct;
        }

        // Nếu admin chưa cấu hình hoặc set về 0 → không ép, random hoàn toàn
        if (targetEdgePercent <= 0.0) {
            logger.debug("GenerationTaiXiu: no house edge config for {}, random result", gameId);
            return generateResult((short) -1);
        }

        long totalBet = totalBetTai + totalBetXiu;
        if (totalBet <= 0) {
            // Không có cược → random tự do
            return generateResult((short) -1);
        }

        // Kiểm tra chênh lệch tối thiểu
        double imbalanceRatio = Math.abs(totalBetTai - totalBetXiu) / (double) totalBet;
        if (imbalanceRatio < MIN_IMBALANCE_RATIO) {
            // Cược gần balanced → house edge tự nhiên từ phí, không cần ép
            logger.debug("GenerationTaiXiu: balanced bets (imbalance={}%), random result", imbalanceRatio * 100);
            return generateResult((short) -1);
        }

        // ── Tính lợi nhuận của từng kịch bản ──────────────────────────────────
        // Kịch bản rớt Tài (forceBetSide=1):
        //   Xỉu mất hết → nhà thu betXiu
        //   Tài thắng   → nhà trả betTai * (1 - taxRate/100.0) (taxRate là % phí)
        //   profit_tai  = betXiu - betTai * (1.0 - taxRate / 100.0)
        double profitIfTai = totalBetXiu - totalBetTai * (1.0 - taxRate / 100.0);

        // Kịch bản rớt Xỉu (forceBetSide=0):
        //   profit_xiu  = betTai - betXiu * (1.0 - taxRate / 100.0)
        double profitIfXiu = totalBetTai - totalBetXiu * (1.0 - taxRate / 100.0);

        // Target profit tương ứng với house edge %
        double targetProfit = totalBet * targetEdgePercent / 100.0;

        logger.debug("GenerationTaiXiu: gameId={} betTai={} betXiu={} targetEdge={}% " +
                "profitTai={} profitXiu={} targetProfit={}",
                gameId, totalBetTai, totalBetXiu, targetEdgePercent,
                profitIfTai, profitIfXiu, targetProfit);

        // Nếu cả hai kịch bản đều cho profit âm (nhà thua cả 2 cửa) → random tự do
        if (profitIfTai < 0 && profitIfXiu < 0) {
            logger.debug("GenerationTaiXiu: both scenarios negative profit, random result");
            return generateResult((short) -1);
        }

        // Chọn kịch bản nào gần target profit nhất
        double diffTai = Math.abs(profitIfTai - targetProfit);
        double diffXiu = Math.abs(profitIfXiu - targetProfit);

        short forceSide = (diffTai <= diffXiu) ? (short) 1 : (short) 0;

        logger.info("GenerationTaiXiu: forcing side {} (profit={}) for game {}",
                forceSide == 1 ? "Tài" : "Xỉu",
                forceSide == 1 ? profitIfTai : profitIfXiu, gameId);

        return generateResult(forceSide);
    }

    /**
     * [BACKWARD COMPAT] Overload không có userId — dùng game default + global overrides.
     */
    public short[] generateResultWithHouseEdge(String gameId, long totalBetTai, long totalBetXiu, float taxRate) {
        return generateResultWithHouseEdge(0L, gameId, totalBetTai, totalBetXiu, taxRate);
    }

    private short[] generateDices() {
        // ThreadLocalRandom: không tạo object mới, không cần AtomicLong sync → ~5x nhanh hơn new Random()
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        short[] dices = new short[3];
        dices[0] = (short)(rd.nextInt(6) + 1);
        dices[1] = (short)(rd.nextInt(6) + 1);
        dices[2] = (short)(rd.nextInt(6) + 1);
        logger.debug("generateDices: " + dices[0] + "," + dices[1] + "," + dices[2]);
        return dices;
    }

    public static void main(String[] agrs) throws IOException {
    }

    private class CauTaiXiu {
        public int index = 0;
        private String data;

        private CauTaiXiu() {
        }

        public void setData(String data) {
            this.data = data;
            this.index = data.length() - 1;
        }

        public int getResultTX() {
            if (this.index < 0 || this.index >= this.data.length()) {
                return -1;
            }
            int result = Integer.parseInt("" + this.data.charAt(this.index));
            --this.index;
            return result;
        }
    }

}
