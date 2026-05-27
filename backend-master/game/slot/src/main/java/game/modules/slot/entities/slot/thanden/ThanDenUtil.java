package game.modules.slot.entities.slot.thanden;

import java.util.List;
import game.modules.slot.utils.ThanDenUtils;

public class ThanDenUtil {

    public static ThanDenTableInfo getThanDenTableInfo(int[] rowsIndex, int betValue, long fund,
                                                       long fundJackPot, long fundMinigame, boolean isMultiJackpot, 
                                                       long pot, long initPotValue, List<Integer> boxValues, 
                                                       boolean isUserBigWin, long maxiumWin, boolean isFreeSpin, int freeSpinRatio) {
        
        long totalBetValue = rowsIndex.length * betValue;
        int maxLoop = 1000;
        int loop = 0;

        while (true) {
            loop++;
            ThanDenTableInfo tableInfo = new ThanDenTableInfo(betValue, pot, boxValues);
            tableInfo.calculate(rowsIndex, isFreeSpin, freeSpinRatio);

            long totalWin = tableInfo.money * betValue;
            boolean valid = true;

            // Kiểm tra Hũ
            if (tableInfo.jackpot) {
                totalWin += pot;
                if (isMultiJackpot) {
                    totalWin += pot * 2; // Ví dụ, config x2
                }
                
                if (totalWin > fundJackPot) {
                    valid = false;
                }
            }

            // Kiểm tra X2 hoặc thắng lớn (Phải trừ vào quỹ)
            if (totalWin > 0 && !tableInfo.jackpot) {
                if (totalWin > fund) {
                    valid = false;
                }
                
                if (totalWin > maxiumWin && maxiumWin > 0) {
                    valid = false;
                }
            }
            
            // Ép userBigWin nếu được cấu hình
            if (isUserBigWin && loop < 100 && totalWin < (totalBetValue * 5)) {
                valid = false; // Bắt buộc cố tìm mâm lớn hơn
            }

            if (valid) {
                return tableInfo;
            }
            if (loop >= maxLoop) {
                ThanDenTableInfo loseTable = new ThanDenTableInfo(betValue, pot, boxValues);
                loseTable.matrix = ThanDenUtils.generateLoseMatrix();
                loseTable.table = ThanDenUtils.matrixToByteArray(loseTable.matrix);
                loseTable.tableCalculate = loseTable.table.clone();
                loseTable.calculate(rowsIndex, isFreeSpin, freeSpinRatio);
                return loseTable;
            }
        }
    }
    
    public static ThanDenTableInfo getThanDenBigWinTableInfo(int[] rowsIndex, int betValue, long fund,
                                                       long fundJackPot, long fundMinigame, boolean isMultiJackpot, 
                                                       long pot, long initPotValue, List<Integer> boxValues, 
                                                       boolean isUserBigWin, long maxiumWin, boolean isFreeSpin, int freeSpinRatio) {
         return getThanDenTableInfo(rowsIndex, betValue, fund, fundJackPot, fundMinigame, isMultiJackpot, pot, initPotValue, boxValues, true, maxiumWin, isFreeSpin, freeSpinRatio);
    }
    
    public static long getMoneyJackPot(long pot, boolean isMultiJackpot, long initPotValue) {
         return isMultiJackpot ? pot * 2 : pot; // config tuỳ chọn
    }
}
