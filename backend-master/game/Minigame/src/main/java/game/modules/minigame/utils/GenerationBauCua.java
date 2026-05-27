package game.modules.minigame.utils;

import com.vinplay.vbee.common.rtp.RtpResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GenerationBauCua {
    private static final Logger logger = LoggerFactory.getLogger("backend");
    public static final String GAME_ID_BAUCUA = "baucua";
    private static final double MIN_IMBALANCE_RATIO = 0.05;
    private static final double DEFAULT_HOUSE_EDGE = 0.0;

    public byte[] generateResultWithHouseEdge(String gameId, long[] totalBets, byte xPot, byte xValue) {
        return generateResultWithHouseEdge(0L, gameId, totalBets, xPot, xValue);
    }

    /**
     * Sinh kết quả Bầu Cua với Target House Edge
     * Bầu Cua có 6 cửa, đúc 3 xúc xắc (6^3 = 216 trường hợp)
     */
    public byte[] generateResultWithHouseEdge(long userId, String gameId, long[] totalBets, byte xPot, byte xValue) {
        if (!isDynamicRtpEnabled()) {
            return generateDices();
        }

        double winRatePct = RtpResolver.effectivePct(userId, gameId);
        
        double targetEdgePercent;
        if (winRatePct >= 92.0 && RtpResolver.effectivePct(gameId) >= 92.0) {
            targetEdgePercent = DEFAULT_HOUSE_EDGE;
        } else {
            targetEdgePercent = 100.0 - winRatePct;
        }

        if (targetEdgePercent <= 0.0) {
            return generateDices();
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
            return generateDices();
        }

        double imbalanceRatio = (double) (maxBet - minBet) / totalBet;
        if (imbalanceRatio < MIN_IMBALANCE_RATIO) {
            return generateDices();
        }

        double targetProfit = totalBet * (targetEdgePercent / 100.0);
        
        double minDiff = Double.MAX_VALUE;
        List<byte[]> bestCombinations = new ArrayList<>();

        for (byte d1 = 0; d1 < 6; d1++) {
            for (byte d2 = 0; d2 < 6; d2++) {
                for (byte d3 = 0; d3 < 6; d3++) {
                    int[] faceCounts = new int[6];
                    faceCounts[d1]++;
                    faceCounts[d2]++;
                    faceCounts[d3]++;

                    long totalPayout = 0;
                    for (int i = 0; i < 6; i++) {
                        if (faceCounts[i] > 0) {
                            int multiplier = faceCounts[i];
                            if (i == xPot) {
                                multiplier *= xValue;
                            }
                            totalPayout += totalBets[i] * multiplier + totalBets[i];
                        }
                    }

                    double profit = totalBet - totalPayout;
                    double diff = Math.abs(profit - targetProfit);

                    if (diff < minDiff) {
                        minDiff = diff;
                        bestCombinations.clear();
                        bestCombinations.add(new byte[]{d1, d2, d3});
                    } else if (diff == minDiff) {
                        bestCombinations.add(new byte[]{d1, d2, d3});
                    }
                }
            }
        }

        if (bestCombinations.isEmpty()) {
            return generateDices();
        }

        int selectedIdx = ThreadLocalRandom.current().nextInt(bestCombinations.size());
        byte[] result = bestCombinations.get(selectedIdx);
        
        logger.debug("GenerationBauCua: game={} edge={}% targetProfit={} minDiff={} bestCombinationsSize={}", 
                     gameId, targetEdgePercent, targetProfit, minDiff, bestCombinations.size());

        return result;
    }

    private byte[] generateDices() {
        byte[] dices = new byte[3];
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        dices[0] = (byte) rd.nextInt(6);
        dices[1] = (byte) rd.nextInt(6);
        dices[2] = (byte) rd.nextInt(6);
        return dices;
    }

    private boolean isDynamicRtpEnabled() {
        String env = System.getenv("CANCUA_USE_DYNAMIC_RTP");
        return "1".equals(env) || "true".equalsIgnoreCase(env);
    }
}
