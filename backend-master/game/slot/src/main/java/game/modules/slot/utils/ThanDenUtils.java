package game.modules.slot.utils;

import game.modules.slot.entities.slot.thanden.*;
import java.util.List;
import java.util.Random;

public class ThanDenUtils {
    // 5 mảng Cuộn (Reels) thiết kế cứng theo chuẩn
    public static final ThanDenItem[] M1 = {
        ThanDenItem.B, ThanDenItem.C, ThanDenItem.D, ThanDenItem.E, ThanDenItem.B, 
        ThanDenItem.F, ThanDenItem.A, ThanDenItem.E, ThanDenItem.H, ThanDenItem.G, 
        ThanDenItem.C, ThanDenItem.H, ThanDenItem.G, ThanDenItem.SCATTER, ThanDenItem.H, 
        ThanDenItem.G, ThanDenItem.C, ThanDenItem.F, ThanDenItem.H, ThanDenItem.E, 
        ThanDenItem.G, ThanDenItem.H, ThanDenItem.D, ThanDenItem.G, ThanDenItem.F, 
        ThanDenItem.BONUS, ThanDenItem.E, ThanDenItem.H, ThanDenItem.D, ThanDenItem.A, 
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.C, ThanDenItem.B, ThanDenItem.A, 
        ThanDenItem.H, ThanDenItem.E, ThanDenItem.F, ThanDenItem.D,
        ThanDenItem.C, ThanDenItem.G
    };

    public static final ThanDenItem[] M2 = {
        ThanDenItem.E, ThanDenItem.D, ThanDenItem.A, ThanDenItem.B, ThanDenItem.F, 
        ThanDenItem.D, ThanDenItem.E, ThanDenItem.F, ThanDenItem.A, ThanDenItem.G, 
        ThanDenItem.H, ThanDenItem.WILD, ThanDenItem.G, ThanDenItem.H, ThanDenItem.SCATTER, 
        ThanDenItem.F, ThanDenItem.H, ThanDenItem.C, ThanDenItem.F, ThanDenItem.E, 
        ThanDenItem.F, ThanDenItem.C, ThanDenItem.G, ThanDenItem.A, ThanDenItem.D, 
        ThanDenItem.E, ThanDenItem.BONUS, ThanDenItem.B, ThanDenItem.D, ThanDenItem.C, 
        ThanDenItem.G, ThanDenItem.H, ThanDenItem.G, ThanDenItem.WILD,
        ThanDenItem.H, ThanDenItem.G, ThanDenItem.H
    };

    public static final ThanDenItem[] M3 = {
        ThanDenItem.C, ThanDenItem.H, ThanDenItem.A, ThanDenItem.H, ThanDenItem.G, 
        ThanDenItem.F, ThanDenItem.C, ThanDenItem.D, ThanDenItem.E, ThanDenItem.A, 
        ThanDenItem.E, ThanDenItem.F, ThanDenItem.SCATTER, ThanDenItem.D, ThanDenItem.H, 
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.WILD, ThanDenItem.F, ThanDenItem.G, 
        ThanDenItem.B, ThanDenItem.H, ThanDenItem.BONUS
    };

    public static final ThanDenItem[] M4 = {
        ThanDenItem.G, ThanDenItem.B, ThanDenItem.G, ThanDenItem.H, ThanDenItem.WILD, 
        ThanDenItem.G, ThanDenItem.H, ThanDenItem.H, ThanDenItem.D, ThanDenItem.SCATTER, 
        ThanDenItem.E, ThanDenItem.H, ThanDenItem.A, ThanDenItem.C, ThanDenItem.F, 
        ThanDenItem.A, ThanDenItem.E, ThanDenItem.F, ThanDenItem.BONUS, ThanDenItem.D
    };

    public static final ThanDenItem[] M5 = {
        ThanDenItem.G, ThanDenItem.H, ThanDenItem.H, ThanDenItem.G, ThanDenItem.F, 
        ThanDenItem.H, ThanDenItem.A, ThanDenItem.G, ThanDenItem.H, ThanDenItem.SCATTER, 
        ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.B, ThanDenItem.A, 
        ThanDenItem.F, ThanDenItem.C, ThanDenItem.E, ThanDenItem.BONUS
    };

    // ==========================================================
    // FREE SPIN REEL STRIPS (Table 15 — GAME_THANDEN.md.docx)
    // ==========================================================
    public static final ThanDenItem[] M1_FREE = {
        ThanDenItem.E, ThanDenItem.C, ThanDenItem.H, ThanDenItem.D, ThanDenItem.G,
        ThanDenItem.E, ThanDenItem.H, ThanDenItem.B, ThanDenItem.G, ThanDenItem.F,
        ThanDenItem.C, ThanDenItem.F, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F,
        ThanDenItem.A, ThanDenItem.G, ThanDenItem.H, ThanDenItem.E, ThanDenItem.G,
        ThanDenItem.D, ThanDenItem.H, ThanDenItem.F, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.B, ThanDenItem.H, ThanDenItem.E, ThanDenItem.G, ThanDenItem.C,
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H
    };

    public static final ThanDenItem[] M2_FREE = {
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.E,
        ThanDenItem.F, ThanDenItem.C, ThanDenItem.G, ThanDenItem.F, ThanDenItem.H,
        ThanDenItem.D, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H, ThanDenItem.WILD,
        ThanDenItem.H, ThanDenItem.D, ThanDenItem.G, ThanDenItem.F, ThanDenItem.C,
        ThanDenItem.H, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H, ThanDenItem.B,
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F,
        ThanDenItem.C, ThanDenItem.G, ThanDenItem.F, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.A, ThanDenItem.F, ThanDenItem.E, ThanDenItem.G, ThanDenItem.D,
        ThanDenItem.E, ThanDenItem.H, ThanDenItem.C, ThanDenItem.F, ThanDenItem.E,
        ThanDenItem.H, ThanDenItem.D, ThanDenItem.G, ThanDenItem.H, ThanDenItem.B,
        ThanDenItem.H, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F,
        ThanDenItem.C, ThanDenItem.H, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.G, ThanDenItem.E, ThanDenItem.B
    };

    public static final ThanDenItem[] M3_FREE = {
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.E,
        ThanDenItem.F, ThanDenItem.C, ThanDenItem.H, ThanDenItem.D, ThanDenItem.G,
        ThanDenItem.E, ThanDenItem.H, ThanDenItem.WILD, ThanDenItem.G, ThanDenItem.F,
        ThanDenItem.H, ThanDenItem.D, ThanDenItem.G, ThanDenItem.F, ThanDenItem.C,
        ThanDenItem.H, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H, ThanDenItem.B,
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F,
        ThanDenItem.C, ThanDenItem.G, ThanDenItem.F, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.A, ThanDenItem.F, ThanDenItem.E, ThanDenItem.G, ThanDenItem.D,
        ThanDenItem.E, ThanDenItem.H, ThanDenItem.C, ThanDenItem.F, ThanDenItem.E,
        ThanDenItem.H, ThanDenItem.D, ThanDenItem.G, ThanDenItem.H, ThanDenItem.B,
        ThanDenItem.H, ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F,
        ThanDenItem.C, ThanDenItem.H, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.E, ThanDenItem.B, ThanDenItem.C, ThanDenItem.D, ThanDenItem.WILD,
        ThanDenItem.F, ThanDenItem.D, ThanDenItem.C, ThanDenItem.G
    };

    public static final ThanDenItem[] M4_FREE = {
        ThanDenItem.E, ThanDenItem.G, ThanDenItem.C, ThanDenItem.F, ThanDenItem.H,
        ThanDenItem.D, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H, ThanDenItem.B,
        ThanDenItem.G, ThanDenItem.WILD, ThanDenItem.F, ThanDenItem.C, ThanDenItem.H,
        ThanDenItem.F, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F, ThanDenItem.A,
        ThanDenItem.G, ThanDenItem.H, ThanDenItem.E, ThanDenItem.G, ThanDenItem.D,
        ThanDenItem.H, ThanDenItem.WILD, ThanDenItem.F, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.B, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H, ThanDenItem.C,
        ThanDenItem.F, ThanDenItem.G, ThanDenItem.D
    };

    public static final ThanDenItem[] M5_FREE = {
        ThanDenItem.G, ThanDenItem.H, ThanDenItem.B, ThanDenItem.G, ThanDenItem.F,
        ThanDenItem.C, ThanDenItem.G, ThanDenItem.F, ThanDenItem.D, ThanDenItem.H,
        ThanDenItem.F, ThanDenItem.WILD, ThanDenItem.G, ThanDenItem.H, ThanDenItem.E,
        ThanDenItem.G, ThanDenItem.D, ThanDenItem.H, ThanDenItem.F, ThanDenItem.E,
        ThanDenItem.H, ThanDenItem.WILD, ThanDenItem.G, ThanDenItem.E, ThanDenItem.H,
        ThanDenItem.C, ThanDenItem.F, ThanDenItem.G, ThanDenItem.D
    };

    // 25 lines matrix mapping (0, 1, 2 for rows)
    public static final byte[][] PAYLINES = {
        {1, 1, 1, 1, 1}, // Line 1
        {0, 0, 0, 0, 0}, // Line 2
        {2, 2, 2, 2, 2}, // Line 3
        {1, 1, 0, 1, 1}, // Line 4
        {1, 1, 2, 1, 1}, // Line 5
        {0, 0, 1, 0, 0}, // Line 6
        {2, 2, 1, 2, 2}, // Line 7
        {0, 2, 0, 2, 0}, // Line 8
        {2, 0, 2, 0, 2}, // Line 9
        {1, 0, 2, 0, 1}, // Line 10
        {2, 1, 0, 1, 2}, // Line 11
        {0, 1, 2, 1, 0}, // Line 12
        {1, 2, 1, 0, 1}, // Line 13
        {1, 0, 1, 2, 1}, // Line 14
        {2, 1, 1, 1, 2}, // Line 15
        {0, 1, 1, 1, 0}, // Line 16
        {1, 0, 0, 0, 1}, // Line 17
        {1, 2, 2, 2, 1}, // Line 18
        {2, 2, 1, 0, 0}, // Line 19
        {0, 0, 1, 2, 2}, // Line 20
        {1, 1, 0, 0, 0}, // Line 21
        {1, 1, 2, 2, 2}, // Line 22
        {0, 0, 0, 1, 1}, // Line 23
        {2, 2, 2, 1, 1}, // Line 24
        {0, 1, 2, 2, 2}  // Line 25
    };

    private static final Random rd = new Random();

    public static ThanDenItem[][] generateMatrix() {
        ThanDenItem[][] matrix = new ThanDenItem[3][5];
        
        int r1 = rd.nextInt(M1.length);
        int r2 = rd.nextInt(M2.length);
        int r3 = rd.nextInt(M3.length);
        int r4 = rd.nextInt(M4.length);
        int r5 = rd.nextInt(M5.length);

        for (int row = 0; row < 3; row++) {
            matrix[row][0] = M1[(r1 + row) % M1.length];
            matrix[row][1] = M2[(r2 + row) % M2.length];
            matrix[row][2] = M3[(r3 + row) % M3.length];
            matrix[row][3] = M4[(r4 + row) % M4.length];
            matrix[row][4] = M5[(r5 + row) % M5.length];
        }
        return matrix;
    }

    /**
     * Wild Column Expansion (DOCX spec):
     * "Nếu biểu tượng Wild xuất hiện ở một vị trí bất kỳ,
     *  thì tất cả phần tử của cột đó sẽ được thay thế bằng Wild"
     * Phải gọi TRƯỚC khi tính payline.
     */
    public static void applyWildExpansion(ThanDenItem[][] matrix) {
        for (int col = 0; col < 5; col++) {
            boolean hasWild = false;
            for (int row = 0; row < 3; row++) {
                if (matrix[row][col] == ThanDenItem.WILD) {
                    hasWild = true;
                    break;
                }
            }
            if (hasWild) {
                for (int row = 0; row < 3; row++) {
                    matrix[row][col] = ThanDenItem.WILD;
                }
            }
        }
    }

    public static ThanDenItem[][] generateFreeSpinMatrix() {
        ThanDenItem[][] matrix = new ThanDenItem[3][5];
        int r1 = rd.nextInt(M1_FREE.length);
        int r2 = rd.nextInt(M2_FREE.length);
        int r3 = rd.nextInt(M3_FREE.length);
        int r4 = rd.nextInt(M4_FREE.length);
        int r5 = rd.nextInt(M5_FREE.length);
        for (int row = 0; row < 3; row++) {
            matrix[row][0] = M1_FREE[(r1 + row) % M1_FREE.length];
            matrix[row][1] = M2_FREE[(r2 + row) % M2_FREE.length];
            matrix[row][2] = M3_FREE[(r3 + row) % M3_FREE.length];
            matrix[row][3] = M4_FREE[(r4 + row) % M4_FREE.length];
            matrix[row][4] = M5_FREE[(r5 + row) % M5_FREE.length];
        }
        return matrix;
    }

    public static ThanDenItem[][] generateMatrixNoHu(String[] lineArr) {
        ThanDenItem[][] matrix = generateMatrix(); // Lấy ma trận background trước
        int n = rd.nextInt(lineArr.length);
        int lineIndex = Integer.parseInt(lineArr[n]);
        if (lineIndex < 1 || lineIndex > 25) return matrix;

        byte[] linePoints = PAYLINES[lineIndex - 1];
        
        // Ép toàn bộ 5 ô vào hệ thống A để tạo điều kiện nổ Hũ
        for (int col = 0; col < 5; ++col) {
             matrix[linePoints[col]][col] = ThanDenItem.A;
        }

        return matrix;
    }

    public static String matrixToString(ThanDenItem[][] matrix) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                builder.append(",").append(matrix[i][j].getId());
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    public static byte[] matrixToByteArray(ThanDenItem[][] matrix) {
        byte[] arr = new byte[15];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                arr[i * 5 + j] = matrix[i][j].getId();
            }
        }
        return arr;
    }

    public static ThanDenItem[][] generateLoseMatrix() {
        ThanDenItem[][] matrix = new ThanDenItem[3][5];
        ThanDenItem[] r1 = {ThanDenItem.A, ThanDenItem.B, ThanDenItem.C, ThanDenItem.D, ThanDenItem.E};
        ThanDenItem[] r2 = {ThanDenItem.F, ThanDenItem.G, ThanDenItem.H, ThanDenItem.A, ThanDenItem.B};
        ThanDenItem[] r3 = {ThanDenItem.C, ThanDenItem.D, ThanDenItem.E, ThanDenItem.F, ThanDenItem.G};

        for (int i=0; i<5; i++) {
           matrix[0][i] = r1[i];
           matrix[1][i] = r2[i];
           matrix[2][i] = r3[i];
        }
        return matrix;
    }

    public static void calculateLine(ThanDenItem[][] matrix, int lineIndex, List<ThanDenAward> awardList) {
        if (lineIndex < 1 || lineIndex > 25) return;
        byte[] linePoints = PAYLINES[lineIndex - 1];
        
        ThanDenItem firstItem = matrix[linePoints[0]][0];
        if (firstItem == ThanDenItem.SCATTER || firstItem == ThanDenItem.BONUS) return; // Bonus & Scatter calculated separately
        
        // Optimize Performance: Traverse left-to-right immediately
        int count = 1;
        ThanDenItem matchedItem = firstItem;

        // Nếu bắt đầu là WILD, tìm symbol tiếp theo để gộp
        // RULE: Wild KHÔNG thay thế A (Jackpot) — DOCX spec
        if (matchedItem == ThanDenItem.WILD) {
            for (int col = 1; col < 5; col++) {
                ThanDenItem nextItem = matrix[linePoints[col]][col];
                // Bỏ qua WILD, SCATTER, BONUS, và A (Jackpot) khi tìm anchor
                if (nextItem != ThanDenItem.WILD && nextItem != ThanDenItem.SCATTER 
                    && nextItem != ThanDenItem.BONUS && nextItem != ThanDenItem.A) {
                    matchedItem = nextItem;
                    break;
                }
            }
        }

        for (int col = 1; col < 5; col++) {
            ThanDenItem currentItem = matrix[linePoints[col]][col];
            // Wild thay thế tất cả TRỪ A (Jackpot)
            if (currentItem == matchedItem || (currentItem == ThanDenItem.WILD && matchedItem != ThanDenItem.A)) {
                count++;
            } else {
                break; // Cắt đứt chuỗi Left-To-Right
            }
        }

        if (count >= 2) {
            ThanDenAward award = getAward(matchedItem, count);
            if (award != null) {
                awardList.add(award);
            }
        }
    }

    private static ThanDenAward getAward(ThanDenItem item, int count) {
        for (ThanDenAward award : ThanDenAward.values()) {
            if (award.getItem() == item && award.getDuplicate() == count) {
                return award;
            }
        }
        return null;
    }
}
