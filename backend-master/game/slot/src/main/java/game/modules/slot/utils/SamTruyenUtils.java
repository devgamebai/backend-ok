/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.utils;

import game.modules.slot.entities.slot.Cell;
import game.modules.slot.entities.slot.Line;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.samtruyen.*;

import java.util.List;
import java.util.Random;

public class SamTruyenUtils {
    public static SamTruyenItem[][] generateMatrix() {
        SamTruyenItems items = new SamTruyenItems();
        SamTruyenItem[][] matrix = new SamTruyenItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(3);
        for (int i = 0; i < 3; ++i) {
            int r = n + i;
            if (r > 2) {
                r -= 3;
            }
            for (int j = 0; j < 5; ++j) {
                boolean gen = true;
                SamTruyenItem item = null;
                while (gen) {
                    item = items.random(j);
                    gen = false;
                    if (!SamTruyenUtils.specialItem(item)) continue;
                    if (item == SamTruyenItem.WILD) {
                        if (!SamTruyenUtils.specialItem(matrix[0][j]) && !SamTruyenUtils.specialItem(matrix[1][j]) && !SamTruyenUtils.specialItem(matrix[2][j])) continue;
                        gen = true;
                        items.refundItem(item, j);
                        continue;
                    }
                    if (matrix[0][j] != item && matrix[1][j] != item && matrix[2][j] != item && matrix[0][j] != SamTruyenItem.WILD && matrix[1][j] != SamTruyenItem.WILD && matrix[2][j] != SamTruyenItem.WILD) continue;
                    gen = true;
                    items.refundItem(item, j);
                }
                matrix[r][j] = item;
            }
        }
        return matrix;
    }

    public static boolean specialItem(SamTruyenItem item) {
        return item == SamTruyenItem.BONUS || item == SamTruyenItem.SCATTER || item == SamTruyenItem.JACK_POT || item == SamTruyenItem.WILD;
    }

    public static SamTruyenItem[][] generateMatrixFreeSpin(String itemsWild) {
        int i;
        String[] arr = itemsWild.split(",");
        SamTruyenFreeSpinItems items = new SamTruyenFreeSpinItems();
        SamTruyenItem[][] matrix = new SamTruyenItem[3][5];
        if (arr.length > 0) {
            for (i = 0; i < arr.length - 1; i += 2) {
                int r = Integer.parseInt(arr[i]);
                int c = Integer.parseInt(arr[i + 1]);
                matrix[r][c] = SamTruyenItem.WILD;
            }
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                if (matrix[i][j] != null) continue;
                matrix[i][j] = items.random(j);
            }
        }
        return matrix;
    }

    public static SamTruyenItem[][] generateMatrixNoHu(String[] lineArr) {
        SamTruyenItem[][] matrix = new SamTruyenItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(lineArr.length);
        int indexLineNoHu = Integer.parseInt(lineArr[n]) - 1;
        SamTruyenLines lines = new SamTruyenLines();
        SamTruyenItems items = new SamTruyenItems();
        Line lineNoHu = lines.get(indexLineNoHu);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                boolean genRandom = true;
                for (int k = 0; k < lineNoHu.getCells().size(); ++k) {
                    if (i != lineNoHu.getCell(k).getRow() || j != lineNoHu.getCell(k).getCol()) continue;
                    genRandom = false;
                    matrix[i][j] = SamTruyenItem.JACK_POT;
                }
                if (!genRandom) continue;
                SamTruyenItem item = SamTruyenItem.JACK_POT;
                while (item == SamTruyenItem.JACK_POT || item == SamTruyenItem.WILD) {
                    item = items.random(j);
                }
                matrix[i][j] = item;
            }
        }
        return matrix;
    }

    public static String matrixToString(SamTruyenItem[][] matrix) {
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

    public static Line getLine(SamTruyenLines lines, SamTruyenItem[][] matrix, int lineIndex) {
        Line line = lines.get(lineIndex - 1);
        for (Object obj : line.getCells()) {
            Cell cell = (Cell) obj;
            SamTruyenItem itemInMatrix = matrix[cell.getRow()][cell.getCol()];
            cell.setItem(itemInMatrix);
        }
        return line;
    }

    public static MiniGameSlotResponse addMiniGameSlot(int baseBetting, int countBonus) {
        Random rd = new Random();
        int indexRatioCol = rd.nextInt(3);
        int indexRatioRow = countBonus - 3;
        int ratio = Constant.SPARTAN_BONUS_RATIO[indexRatioRow][indexRatioCol];
        MiniGameSlotResponse res = SamTruyenUtils.generateMiniGameSlot(baseBetting);
        res.setTotalPrize(res.getTotalPrize() * (long)ratio);
        res.setPrizes(String.valueOf(res.getPrizes()) + "," + ratio + "," + countBonus);
        return res;
    }

    private static MiniGameSlotResponse generateMiniGameSlot(int baseBetting) {
        MiniGameSlotResponse response = new MiniGameSlotResponse();
        int step = 0;
        long tongGiai = 0L;
        boolean chonTiep = false;
        Random rd = new Random();
        StringBuilder sb = new StringBuilder();
        do {
            int numChonTiep;
            if ((numChonTiep = rd.nextInt(100) + 1) > Constant.SPARTAN_TANK_TI_LE_TRUOT[step]) {
                int indexCol = rd.nextInt(15);
                int prize = Constant.SPARTAN_TANK_PRIZES[step][indexCol] * baseBetting;
                sb.append(prize);
                sb.append(",");
                tongGiai += (long)prize;
                chonTiep = true;
            } else {
                chonTiep = false;
            }
            ++step;
        } while (chonTiep);
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        response.setPrizes(sb.toString());
        response.setTotalPrize(tongGiai);
        return response;
    }

    public static void main(String[] args) {
        int n = 1000;
        long total = 0L;
        for (int i = 0; i < 1000; ++i) {
            MiniGameSlotResponse res = SamTruyenUtils.generateMiniGameSlot(100);
            total += res.getTotalPrize();
            System.out.println(res.getTotalPrize());
        }
        System.out.println("Trung binh: " + total / 1000L);
    }

    public static void calculateLine(Line line, List<SamTruyenAward> awardList) {
        int countNumItems = 0;
        SamTruyenItem itemSample = (SamTruyenItem)((Object)line.getCell(0).getItem());
        if (itemSample != SamTruyenItem.BONUS && itemSample != SamTruyenItem.SCATTER) {
            SamTruyenAward award;
            byte id;
            for (int j = 0; j < line.getCells().size() && ((id = ((SamTruyenItem)((Object)line.getCell(j).getItem())).getId()) == itemSample.getId() || itemSample.getId() != SamTruyenItem.JACK_POT.getId() && id == SamTruyenItem.WILD.getId()); ++j) {
                ++countNumItems;
            }
            if (countNumItems >= 3 && (award = SamTruyenAwards.getAward(itemSample, countNumItems)) != null) {
                awardList.add(award);
            }
        }
    }

    public static void calculateFreeSpinLine(Line line, List<SamTruyenFreeSpinAward> awardList) {
        int countNumItems = 0;
        SamTruyenItem itemSample = (SamTruyenItem)((Object)line.getCell(0).getItem());
        if (itemSample != SamTruyenItem.BONUS && itemSample != SamTruyenItem.SCATTER && itemSample != SamTruyenItem.JACK_POT) {
            SamTruyenFreeSpinAward award;
            for (int j = 0; j < line.getCells().size() && (line.getCell(j).getItem() == itemSample || line.getCell(j).getItem() == SamTruyenItem.WILD); ++j) {
                ++countNumItems;
            }
            if (countNumItems >= 3 && (award = SamTruyenFreeSpinAwards.getAward(itemSample, countNumItems)) != null) {
                awardList.add(award);
            }
        }
    }

    public static SamTruyenItem[][] revertMatrix(SamTruyenItem[][] m) {
        SamTruyenItem[][] matrix = new SamTruyenItem[3][5];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                if (matrix[i][j] != null) continue;
                matrix[i][j] = m[i][j];
                if (matrix[i][j] != SamTruyenItem.WILD) continue;
                matrix[0][j] = SamTruyenItem.WILD;
                matrix[1][j] = SamTruyenItem.WILD;
                matrix[2][j] = SamTruyenItem.WILD;
            }
        }
        return matrix;
    }
}

