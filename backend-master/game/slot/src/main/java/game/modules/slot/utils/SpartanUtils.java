/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.utils;

import game.modules.slot.entities.slot.Cell;
import game.modules.slot.entities.slot.Line;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.spartan.*;

import java.util.List;
import java.util.Random;

public class SpartanUtils {
    public static SpartanItem[][] generateMatrix() {
        SpartanItems items = new SpartanItems();
        SpartanItem[][] matrix = new SpartanItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(3);
        for (int i = 0; i < 3; ++i) {
            int r = n + i;
            if (r > 2) {
                r -= 3;
            }
            for (int j = 0; j < 5; ++j) {
                boolean gen = true;
                SpartanItem item = null;
                while (gen) {
                    item = items.random(j);
                    gen = false;
                    if (!SpartanUtils.specialItem(item)) continue;
                    if (item == SpartanItem.WILD) {
                        if (!SpartanUtils.specialItem(matrix[0][j]) && !SpartanUtils.specialItem(matrix[1][j]) && !SpartanUtils.specialItem(matrix[2][j])) continue;
                        gen = true;
                        items.refundItem(item, j);
                        continue;
                    }
                    if (matrix[0][j] != item && matrix[1][j] != item && matrix[2][j] != item && matrix[0][j] != SpartanItem.WILD && matrix[1][j] != SpartanItem.WILD && matrix[2][j] != SpartanItem.WILD) continue;
                    gen = true;
                    items.refundItem(item, j);
                }
                matrix[r][j] = item;
            }
        }
        return matrix;
    }

    public static boolean specialItem(SpartanItem item) {
        return item == SpartanItem.BONUS || item == SpartanItem.SCATTER || item == SpartanItem.JACK_POT || item == SpartanItem.WILD;
    }

    public static SpartanItem[][] generateMatrixFreeSpin(String itemsWild) {
        int i;
        String[] arr = itemsWild.split(",");
        SpartanFreeSpinItems items = new SpartanFreeSpinItems();
        SpartanItem[][] matrix = new SpartanItem[3][5];
        if (arr.length > 0) {
            for (i = 0; i < arr.length - 1; i += 2) {
                int r = Integer.parseInt(arr[i]);
                int c = Integer.parseInt(arr[i + 1]);
                matrix[r][c] = SpartanItem.WILD;
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

    public static SpartanItem[][] generateMatrixNoHu(String[] lineArr) {
        SpartanItem[][] matrix = new SpartanItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(lineArr.length);
        int indexLineNoHu = Integer.parseInt(lineArr[n]) - 1;
        SpartanLines lines = new SpartanLines();
        SpartanItems items = new SpartanItems();
        Line lineNoHu = lines.get(indexLineNoHu);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                boolean genRandom = true;
                for (int k = 0; k < lineNoHu.getCells().size(); ++k) {
                    if (i != lineNoHu.getCell(k).getRow() || j != lineNoHu.getCell(k).getCol()) continue;
                    genRandom = false;
                    matrix[i][j] = SpartanItem.JACK_POT;
                }
                if (!genRandom) continue;
                SpartanItem item = SpartanItem.JACK_POT;
                while (item == SpartanItem.JACK_POT || item == SpartanItem.WILD) {
                    item = items.random(j);
                }
                matrix[i][j] = item;
            }
        }
        return matrix;
    }

    public static String matrixToString(SpartanItem[][] matrix) {
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

    public static Line getLine(SpartanLines lines, SpartanItem[][] matrix, int lineIndex) {
        Line line = lines.get(lineIndex - 1);
        for (Object obj : line.getCells()) {
            Cell cell = (Cell) obj;
            SpartanItem itemInMatrix = matrix[cell.getRow()][cell.getCol()];
            cell.setItem(itemInMatrix);
        }
        return line;
    }

    public static MiniGameSlotResponse addMiniGameSlot(int baseBetting, int countBonus) {
        Random rd = new Random();
        int indexRatioCol = rd.nextInt(3);
        int indexRatioRow = countBonus - 3;
        int ratio = Constant.SPARTAN_BONUS_RATIO[indexRatioRow][indexRatioCol];
        MiniGameSlotResponse res = SpartanUtils.generateMiniGameSlot(baseBetting);
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
            MiniGameSlotResponse res = SpartanUtils.generateMiniGameSlot(100);
            total += res.getTotalPrize();
            System.out.println(res.getTotalPrize());
        }
        System.out.println("Trung binh: " + total / 1000L);
    }

    public static void calculateLine(Line line, List<SpartanAward> awardList) {
        int countNumItems = 0;
        SpartanItem itemSample = (SpartanItem)((Object)line.getCell(0).getItem());
        if (itemSample != SpartanItem.BONUS && itemSample != SpartanItem.SCATTER) {
            SpartanAward award;
            byte id;
            for (int j = 0; j < line.getCells().size() && ((id = ((SpartanItem)((Object)line.getCell(j).getItem())).getId()) == itemSample.getId() || itemSample.getId() != SpartanItem.JACK_POT.getId() && id == SpartanItem.WILD.getId()); ++j) {
                ++countNumItems;
            }
            if (countNumItems >= 3 && (award = SpartanAwards.getAward(itemSample, countNumItems)) != null) {
                awardList.add(award);
            }
        }
    }

    public static void calculateFreeSpinLine(Line line, List<SpartanFreeSpinAward> awardList) {
        int countNumItems = 0;
        SpartanItem itemSample = (SpartanItem)((Object)line.getCell(0).getItem());
        if (itemSample != SpartanItem.BONUS && itemSample != SpartanItem.SCATTER && itemSample != SpartanItem.JACK_POT) {
            SpartanFreeSpinAward award;
            for (int j = 0; j < line.getCells().size() && (line.getCell(j).getItem() == itemSample || line.getCell(j).getItem() == SpartanItem.WILD); ++j) {
                ++countNumItems;
            }
            if (countNumItems >= 3 && (award = SpartanFreeSpinAwards.getAward(itemSample, countNumItems)) != null) {
                awardList.add(award);
            }
        }
    }

    public static SpartanItem[][] revertMatrix(SpartanItem[][] m) {
        SpartanItem[][] matrix = new SpartanItem[3][5];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                if (matrix[i][j] != null) continue;
                matrix[i][j] = m[i][j];
                if (matrix[i][j] != SpartanItem.WILD) continue;
                matrix[0][j] = SpartanItem.WILD;
                matrix[1][j] = SpartanItem.WILD;
                matrix[2][j] = SpartanItem.WILD;
            }
        }
        return matrix;
    }
}

