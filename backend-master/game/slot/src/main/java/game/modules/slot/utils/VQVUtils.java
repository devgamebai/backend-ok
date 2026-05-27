/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.utils;

import game.modules.slot.entities.slot.vqv.Awards;
import game.modules.slot.entities.slot.vqv.CellVQV;
import game.modules.slot.entities.slot.vqv.Line;
import game.modules.slot.entities.slot.vqv.VQVAward;
import game.modules.slot.entities.slot.vqv.VQVItem;
import game.modules.slot.entities.slot.vqv.VQVItems;
import game.modules.slot.entities.slot.vqv.VQVLines;
import java.util.List;
import java.util.Random;

public class VQVUtils {
    public static VQVItem[][] generateMatrix() {
        VQVItems items = new VQVItems();
        VQVItem[][] matrix = new VQVItem[3][5];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                matrix[i][j] = items.random();
            }
        }
        return matrix;
    }

    public static VQVItem[][] generateMatrixNoHu(String[] lineArr) {
        VQVItem[][] matrix = new VQVItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(lineArr.length);
        int indexLineNoHu = Integer.parseInt(lineArr[n]) - 1;
        VQVLines lines = new VQVLines();
        VQVItems items = new VQVItems();
        Line lineNoHu = lines.get(indexLineNoHu);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                boolean genRandom = true;
                for (int k = 0; k < lineNoHu.getCells().size(); ++k) {
                    if (i != lineNoHu.getCell(k).getRow() || j != lineNoHu.getCell(k).getCol()) continue;
                    genRandom = false;
                    matrix[i][j] = VQVItem.JACKPOT;
                }
                if (!genRandom) continue;
                VQVItem item = VQVItem.JACKPOT;
                while (item == VQVItem.JACKPOT) {
                    item = items.random();
                }
                matrix[i][j] = item;
            }
        }
        return matrix;
    }

    public static String matrixToString(VQVItem[][] matrix) {
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

    public static Line getLine(VQVLines lines, VQVItem[][] matrix, int lineIndex) {
        Line line = lines.get(lineIndex - 1);
        for (Object obj : line.getCells()) {
            CellVQV cell = (CellVQV) obj;
            VQVItem itemInMatrix = matrix[cell.getRow()][cell.getCol()];
            cell.setItem(itemInMatrix);
        }
        return line;
    }

    public static void calculateLine(Line line, List<VQVAward> awardList) {
        for (int i = 0; i < line.getCells().size(); ++i) {
            VQVAward award;
            int countNumItems = 0;
            VQVItem itemSample = line.getItem(i);
            for (int j = 0; j < line.getCells().size(); ++j) {
                if (line.getItem(j) != itemSample) continue;
                ++countNumItems;
            }
            if (countNumItems < 3 || (award = Awards.getAward(itemSample, countNumItems)) == null || VQVUtils.checkAwardExist(awardList, award)) continue;
            awardList.add(award);
        }
    }

    private static boolean checkAwardExist(List<VQVAward> awardList, VQVAward awardLine) {
        for (VQVAward award : awardList) {
            if (award != awardLine) continue;
            return true;
        }
        return false;
    }
}

