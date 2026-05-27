package game.modules.XocDia.model;

import game.modules.XocDia.XocDiaConstant;

import com.vinplay.vbee.common.rtp.RtpResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dice result generator for Xoc Dia Tu Linh.
 * 4 dice, each is 0 (trang/white) or 1 (den/black).
 * The existing bytecode shows: random < 0.5 => 0, else => 1
 *
 * For Tu Linh mapping:
 * - Each dice value 0-3 represents a beast
 * - But the compiled code uses binary (0/1), so we keep that pattern
 * - listTrang = count of 0s in result
 * - Payout based on listTrang count:
 *   - Even (0,2,4) listTrang: chan wins, multiply by 2 for door 0
 *   - Odd (1,3) listTrang: le wins, multiply by 2 for door 1
 *   - 0 trang (all den): door 5 (BET_0_4) wins, multiply by 16
 *   - 4 trang (all same): door 4 (BET_4_0) wins, multiply by 16
 *   - 1 trang: door 3 (BET_1_3) wins, multiply by 4
 *   - 3 trang: door 2 (BET_3_1) wins, multiply by 4
 */
public class DiceResult {

    public static final String GAME_ID_XOCDIA = "xocdia";
    private static final double MIN_IMBALANCE_RATIO = 0.05;
    private static final double DEFAULT_HOUSE_EDGE = 0.0;

    public DiceResult() {
    }

    public byte[] getResult(long[] listUserBet) {
        return getResultWithHouseEdge(0L, GAME_ID_XOCDIA, listUserBet);
    }

    public byte[] getResultWithHouseEdge(long userId, String gameId, long[] totalBets) {
        double winRatePct = RtpResolver.effectivePct(userId, gameId);
        
        double targetEdgePercent;
        if (winRatePct >= 92.0 && RtpResolver.effectivePct(gameId) >= 92.0) {
            targetEdgePercent = DEFAULT_HOUSE_EDGE;
        } else {
            targetEdgePercent = 100.0 - winRatePct;
        }

        if (targetEdgePercent <= 0.0) {
            return generateRandomDice();
        }

        long totalBet = 0;
        long maxBet = 0;
        long minBet = Long.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            totalBet += totalBets[i];
            if (totalBets[i] > maxBet) maxBet = totalBets[i];
            if (totalBets[i] < minBet) minBet = totalBets[i];
        }

        if (totalBet <= 0) {
            return generateRandomDice();
        }

        // Tỉ lệ chênh lệch: nếu cược quá đều nhau, không cần ép kết quả (cân cửa tự nhiên)
        double imbalanceRatio = (double) (maxBet - minBet) / totalBet;
        if (imbalanceRatio < MIN_IMBALANCE_RATIO) {
            return generateRandomDice();
        }

        double targetProfit = totalBet * (targetEdgePercent / 100.0);
        
        double minDiff = Double.MAX_VALUE;
        List<byte[]> bestCombinations = new ArrayList<>();

        // 16 kịch bản cho 4 đồng xu (0 hoặc 1)
        for (int i = 0; i < 16; i++) {
            byte[] candidate = new byte[4];
            candidate[0] = (byte) ((i >> 3) & 1);
            candidate[1] = (byte) ((i >> 2) & 1);
            candidate[2] = (byte) ((i >> 1) & 1);
            candidate[3] = (byte) (i & 1);

            long totalPayout = getAllMoneyBetWithData(totalBets, candidate);
            double profit = totalBet - totalPayout;
            double diff = Math.abs(profit - targetProfit);

            if (diff < minDiff) {
                minDiff = diff;
                bestCombinations.clear();
                bestCombinations.add(candidate);
            } else if (diff == minDiff) {
                bestCombinations.add(candidate);
            }
        }

        if (bestCombinations.isEmpty()) {
            return generateRandomDice();
        }

        int selectedIdx = ThreadLocalRandom.current().nextInt(bestCombinations.size());
        return bestCombinations.get(selectedIdx);
    }

    private byte[] generateRandomDice() {
        byte[] toReturn = new byte[4];
        for (int i = 0; i < toReturn.length; i++) {
            toReturn[i] = (ThreadLocalRandom.current().nextDouble() < 0.5) ? (byte) 0 : (byte) 1;
        }
        return toReturn;
    }

    public long getAllMoneyBetWithData(long[] listUserBet, byte[] result) {
        byte listTrang = 0;
        for (int i = 0; i < result.length; i++) {
            if (result[i] == 0) {
                listTrang = (byte) (listTrang + 1);
            }
        }
        long moneyWin = 0;
        // Chan (even trang count) or Le (odd trang count)
        if (listTrang % 2 == 0) {
            moneyWin = moneyWin + listUserBet[0] * 2L; // BET_CHAN door payout
        } else {
            moneyWin = moneyWin + listUserBet[1] * 2L; // BET_LE door payout
        }
        // 4-0 (all trang)
        if (listTrang == 0) {
            moneyWin = moneyWin + listUserBet[5] * 16L;
        }
        // 0-4 (all den)
        if (listTrang == 4) {
            moneyWin = moneyWin + listUserBet[4] * 16L;
        }
        // 1 trang = 1-3 door
        if (listTrang == 1) {
            moneyWin = moneyWin + listUserBet[3] * 4L;
        }
        // 3 trang = 3-1 door
        if (listTrang == 3) {
            moneyWin = moneyWin + listUserBet[2] * 4L;
        }
        return moneyWin;
    }
}
