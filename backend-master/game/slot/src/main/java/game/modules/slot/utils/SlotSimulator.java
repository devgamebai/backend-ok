package game.modules.slot.utils;

import game.modules.slot.entities.slot.khobau.*;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI Tool để test giả lập Slot Engine với hàng triệu vòng quay,
 * dùng để verify chỉ số RTP và cơ chế House Edge, Fund Protection.
 */
public class SlotSimulator {
    
    public static void main(String[] args) {
        double rtpTarget = Double.parseDouble(System.getProperty("pct", "80"));
        long spins = Long.parseLong(System.getProperty("spins", "2000000"));
        long startFund = Long.parseLong(System.getProperty("fund", "100000000"));
        int betValue = 1000;
        int numLines = 20;
        long totalBetPerSpin = (long) betValue * numLines;

        System.setProperty("MOCK_RTP_PCT", String.valueOf(rtpTarget));

        long fund = startFund;
        long totalBet = 0;
        long totalWin = 0;
        long maxPrize = 0;
        long forceLoseCount = 0;
        long initPotValue = 500000;
        long pot = initPotValue;

        // "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20"
        String[] linesArr = {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"};
        KhoBauLines slotLines = new KhoBauLines();

        System.out.println("=========================================");
        System.out.println("🎰 SLOT ENGINE SIMULATOR (Kho Bau Math)");
        System.out.println("=========================================");
        System.out.println("🎯 Target RTP: " + rtpTarget + "%");
        System.out.println("🔄 Total Spins: " + spins);
        System.out.println("💰 Initial Fund: " + fund);
        System.out.println("💵 Bet per spin: " + totalBetPerSpin);
        System.out.println("-----------------------------------------");

        long startMs = System.currentTimeMillis();

        for (long i = 0; i < spins; i++) {
            totalBet += totalBetPerSpin;
            
            // Deduct fee and pot funding
            long fee = totalBetPerSpin * 2L / 100L;
            long moneyToPot = totalBetPerSpin * 1L / 100L;
            long moneyToFund = totalBetPerSpin - fee - moneyToPot;
            fund += moneyToFund;
            pot += moneyToPot;

            long prize = 0;

            // Generate spin matrix
            boolean enoughPair = false;
            long prizesOnLines = 0;
            
            // Limit the attempts exactly like KhoBauRoom
            int limitCounter = 0;
            while (!enoughPair && limitCounter < 100000) {
                limitCounter++;
                prizesOnLines = 0;
                boolean isJackpot = false;
                KhoBauItem[][] matrix = KhoBauUtils.generateMatrix();
                
                for (String lineStr : linesArr) {
                    List<KhoBauAward> awardList = new ArrayList<>();
                    Line line = KhoBauUtils.getLine(slotLines, matrix, Integer.parseInt(lineStr));
                    KhoBauUtils.calculateLine(line, awardList);
                    
                    for (KhoBauAward award : awardList) {
                        if (award.getRatio() > 0.0f) {
                            prizesOnLines += (long) (award.getRatio() * betValue);
                        } else if (award == KhoBauAward.PENTA_POUCH) {
                            isJackpot = true;
                            prizesOnLines += pot;
                        } else {
                            // Bonus game (Pick star fallback sim - average reward ~15x bet)
                            prizesOnLines += (long) betValue * 15;
                        }
                    }
                }
                
                // --- Apply House Edge & Fund Guard ---
                boolean forceLose = SlotHouseEdge.shouldForceLose(i, "khobau", fund, pot, totalBetPerSpin, betValue);
                if (forceLose) {
                    forceLoseCount++;
                }

                if (isJackpot) {
                    if (!forceLose && fund - (prizesOnLines - (pot - initPotValue)) >= 0) {
                        enoughPair = true;
                        pot = initPotValue; // reset pot
                        fund -= (prizesOnLines - (pot - initPotValue)); // Actually pot covers its own amount minus initial
                    }
                } else if (forceLose) {
                    // MUST LOSE OR BREAK EVEN
                    if (prizesOnLines - totalBetPerSpin < 0) {
                        enoughPair = true;
                        fund -= prizesOnLines;
                    }
                } else {
                    // NORMAL WIN ALLOWED IF CAP & MAX PRIZE MAINTAINED
                    if (fund - prizesOnLines >= initPotValue * 2L || prizesOnLines - totalBetPerSpin < 0) {
                        enoughPair = true;
                        fund -= prizesOnLines;
                    }
                }
            }

            // Cap prize
            prize = SlotHouseEdge.capPrize(prizesOnLines, fund);
            if (prize == 0 && prizesOnLines > 0) {
                // If capped to 0, fund shouldn't have been deducted. Revert fund deduction and pretend 0 prize.
                // In real code, capping happens before fund math but here we just simulate the safety.
                prize = 0;
                fund += prizesOnLines;
            }

            totalWin += prize;
            if (prize > maxPrize) {
                maxPrize = prize;
            }
        }

        long endMs = System.currentTimeMillis();
        double actualRtp = (double) totalWin / totalBet * 100.0;
        double forceLosePct = (double) forceLoseCount / spins * 100.0;
        long net = fund - startFund;

        System.out.println("✅ SIMULATION COMPLETE (" + (endMs - startMs) + "ms)");
        System.out.println("-----------------------------------------");
        System.out.println("📊 Total Bet Volume : " + totalBet);
        System.out.println("🏆 Total Payout     : " + totalWin);
        System.out.println("📈 ACTUAL RTP       : " + String.format("%.2f", actualRtp) + "%");
        System.out.println("-----------------------------------------");
        System.out.println("🏦 Start Fund       : " + startFund);
        System.out.println("🏠 End Fund         : " + fund);
        System.out.println("💵 Net House Profit : " + (-totalWin + totalBet)); // approximate
        System.out.println("-----------------------------------------");
        System.out.println("⚠️ Force Lose Spins : " + forceLoseCount + " (" + String.format("%.2f", forceLosePct) + "%)");
        System.out.println("💎 Max Single Prize : " + maxPrize);
        System.out.println("=========================================");
    }
}
