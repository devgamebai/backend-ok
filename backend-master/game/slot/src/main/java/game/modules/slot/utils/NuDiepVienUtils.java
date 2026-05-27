/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.utils;

import game.modules.slot.entities.slot.Cell;
import game.modules.slot.entities.slot.Line;
import game.modules.slot.entities.slot.ndv.NDVAward;
import game.modules.slot.entities.slot.ndv.NDVAwards;
import game.modules.slot.entities.slot.ndv.NDVItem;
import game.modules.slot.entities.slot.ndv.NDVItems;
import game.modules.slot.entities.slot.ndv.NDVLines;
import java.util.List;
import java.util.Random;

public class NuDiepVienUtils {
    public static NDVItem[][] generateMatrix() {
        NDVItems items = new NDVItems();
        NDVItem[][] matrix = new NDVItem[3][5];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                matrix[i][j] = items.random();
            }
        }
        return matrix;
    }
    
//    public static NDVItem[][] generateMatrixNew(int room) {
//        NDVItems items = new NDVItems(room);
//        NDVItem[][] matrix = new NDVItem[3][5];
//        Random rd = new Random();
//        int n = rd.nextInt(3);
//        for (int i = 0; i < 3; ++i) {
//            int r = n + i;
//            if (r > 2) {
//                r -= 3;
//            }
//            for (int j = 0; j < 5; ++j) {
//                boolean gen = true;
//                NDVItem item = null;
//                while (gen) {
//                    item = items.random(j);
//                    gen = false;
////                    if (!NuDiepVienUtils.specialItem(item)) continue;
////                    if (item == NDVItem.THAY_THE) {
////                        if (!NuDiepVienUtils.specialItem(matrix[0][j]) && !NuDiepVienUtils.specialItem(matrix[1][j]) && !NuDiepVienUtils.specialItem(matrix[2][j])) continue;
////                        gen = true;
////                        items.refundItem(item, j);
////                        continue;
////                    }
////                    if (matrix[0][j] != item && matrix[1][j] != item && matrix[2][j] != item && matrix[0][j] != NDVItem.THAY_THE && matrix[1][j] != NDVItem.THAY_THE && matrix[2][j] != NDVItem.THAY_THE) continue;
////                    gen = true;
////                    items.refundItem(item, j);
//                }
//                matrix[r][j] = item;
//            }
//        }
//        return matrix;
//    }
    
    public static boolean specialItem(NDVItem item) {
        return item == NDVItem.NU_DIEP_VIEN || item == NDVItem.KIEM_NHAT || item == NDVItem.THAY_THE;
    }

    public static NDVItem[][] generateMatrixNoHu(String[] lineArr) {
        NDVItem[][] matrix = new NDVItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(lineArr.length);
        int indexLineNoHu = Integer.parseInt(lineArr[n]) - 1;
        NDVLines lines = new NDVLines();
        NDVItems items = new NDVItems();
        Line lineNoHu = lines.get(indexLineNoHu);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                boolean genRandom = true;
                for (int k = 0; k < lineNoHu.getCells().size(); ++k) {
                    if (i != lineNoHu.getCell(k).getRow() || j != lineNoHu.getCell(k).getCol()) continue;
                    genRandom = false;
                    matrix[i][j] = NDVItem.NU_DIEP_VIEN;
                }
                if (!genRandom) continue;
                matrix[i][j] = items.random();
            }
        }
        return matrix;
    }

    public static String matrixToString(NDVItem[][] matrix) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                builder.append(",");
                builder.append(matrix[i][j].getId());
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    public static Line getLine(NDVLines lines, NDVItem[][] matrix, int lineIndex) {
        Line line = lines.get(lineIndex - 1);
        for (Object obj : line.getCells()) {
            Cell cell = (Cell) obj;
            NDVItem itemInMatrix = matrix[cell.getRow()][cell.getCol()];
            cell.setItem(itemInMatrix);
        }
        return line;
    }

    public static void calculateLine(Line line, List<NDVAward> awardList) {
        for (int i = 0; i < line.getCells().size(); ++i) {
            NDVAward award;
            int countNumItems = 0;
            NDVItem itemSample = (NDVItem)((Object)line.getCell(i).getItem());
            for (int j = 0; j < line.getCells().size(); ++j) {
                if (line.getCell(j).getItem() != itemSample && line.getCell(j).getItem() != NDVItem.THAY_THE) continue;
                ++countNumItems;
            }
            if (countNumItems < 3 || (award = NDVAwards.getAward(itemSample, countNumItems)) == null || NuDiepVienUtils.checkAwardExist(awardList, award)) continue;
            awardList.add(award);
        }
    }

    private static boolean checkAwardExist(List<NDVAward> awardList, NDVAward awardLine) {
        for (NDVAward award : awardList) {
            if (award != awardLine) continue;
            return true;
        }
        return false;
    }
}

