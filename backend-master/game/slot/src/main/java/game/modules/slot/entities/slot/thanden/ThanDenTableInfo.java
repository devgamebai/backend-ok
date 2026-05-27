package game.modules.slot.entities.slot.thanden;

import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.TableInfo;
import game.modules.slot.utils.ThanDenUtils;

import java.util.ArrayList;
import java.util.List;

public class ThanDenTableInfo extends TableInfo {
    public ThanDenItem[][] matrix;
    public byte[] tableCalculate;

    // Constructor cho ván quay thường dĩ nhiên
    public ThanDenTableInfo(long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.matrix = ThanDenUtils.generateMatrix();
        this.table = ThanDenUtils.matrixToByteArray(this.matrix);
        this.tableCalculate = this.table.clone();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    // Constructor Force (VD Lên Hũ, FreeSpin)
    public ThanDenTableInfo(short giftType, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;

        if (giftType == GiftType.JACKPOT) {
            this.matrix = ThanDenUtils.generateMatrixNoHu(new String[]{"1", "2", "3"});
        } else if (giftType == GiftType.FREE_SPIN) {
            // FREE SPIN: dùng bộ cuộn riêng theo Table 15 spec
            this.matrix = ThanDenUtils.generateFreeSpinMatrix();
        } else {
            this.matrix = ThanDenUtils.generateMatrix();
        }
        
        this.table = ThanDenUtils.matrixToByteArray(this.matrix);
        this.tableCalculate = this.table.clone();
    }

    public int freeSpinRatio = 1;

    public void calculate(int[] rowsIndex, boolean isFreeSpin, int freeSpinRatio) {
        this.freeSpinRatio = freeSpinRatio;
        this.money = 0;
        this.jackpot = false;
        this.freeSpin = 0;
        this.miniGame = 0;

        List<ThanDenAward> awardList = new ArrayList<>();

        // 1. Đếm Scatter và Bonus từ matrix GỐC (TRƯỚC Wild Expansion)
        //    Wild Expansion sẽ đè Scatter/Bonus nếu cùng cột → phải đếm trước
        int countScatter = 0;
        int countBonus = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 5; c++) {
                if (this.matrix[r][c] == ThanDenItem.SCATTER) countScatter++;
                if (this.matrix[r][c] == ThanDenItem.BONUS) countBonus++;
            }
        }

        // 2. Apply Wild Column Expansion (DOCX spec)
        ThanDenUtils.applyWildExpansion(this.matrix);

        // 3. Tính toán Line (Left-To-Right contiguous)
        //    Wild KHÔNG thay thế A (Jackpot) — xử lý trong ThanDenUtils.calculateLine()
        for (int i = 0; i < rowsIndex.length; i++) {
            awardList.clear();
            ThanDenUtils.calculateLine(this.matrix, rowsIndex[i], awardList);

            long lineMoney = 0;
            boolean isLineHit = false;

            for (ThanDenAward award : awardList) {
                if (award == ThanDenAward.PENTA_A || award == ThanDenAward.PENTA_WILD) {
                    this.jackpot = true;
                    isLineHit = true;
                } else {
                    float ratio = isFreeSpin ? award.getRatioFreeSpin() : award.getRatio();
                    if (ratio > 0) {
                        long prizeMoney = (long) (ratio * this.betLevel * this.freeSpinRatio);
                        lineMoney += prizeMoney;
                        this.money += prizeMoney;
                        isLineHit = true;
                    }
                }
            }

            if (isLineHit) {
                this.lineWin.add(rowsIndex[i]);
                if (this.jackpot) {
                    this.moneyWin.add(this.moneyEatPot);
                } else {
                    this.moneyWin.add(lineMoney);
                }
            }
        }

        // 4. Xử lý Scatter → Free Spin
        if (countScatter == 4) {
            this.freeSpin = 8;
            this.freeSpinRatio = 1;
        } else if (countScatter == 5) {
            this.freeSpin = 8;
            this.freeSpinRatio = 2;
        }

        // 5. Xử lý Bonus → Minigame
        if (countBonus >= 3) {
            this.miniGame = countBonus;
        }

        if (this.miniGame > 0) {
            this.miniGameSlotResponse = this.generatePickStars(this.miniGame);
            this.moneyWin.add(this.miniGameSlotResponse.getTotalPrize());
        }
    }

    @Override
    public String matrixToString() {
        return ThanDenUtils.matrixToString(this.matrix);
    }
}
