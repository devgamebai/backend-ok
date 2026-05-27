/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.utils;

import game.modules.slot.entities.slot.Cell;
import game.modules.slot.entities.slot.Line;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.avengers.AvengersAward;
import game.modules.slot.entities.slot.avengers.AvengersAwards;
import game.modules.slot.entities.slot.avengers.AvengersFreeSpinAward;
import game.modules.slot.entities.slot.avengers.AvengersFreeSpinAwards;
import game.modules.slot.entities.slot.avengers.AvengersFreeSpinItems;
import game.modules.slot.entities.slot.avengers.AvengersItem;
import game.modules.slot.entities.slot.avengers.AvengersItems;
import game.modules.slot.entities.slot.avengers.AvengersLines;
import game.modules.slot.utils.Constant;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

public class AvengersUtils {
    public static AvengersItem[][] generateMatrix() {
        AvengersItems items = new AvengersItems();
        AvengersItem[][] matrix = new AvengersItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(3);
        for (int i = 0; i < 3; ++i) {
            int r = n + i;
            if (r > 2) {
                r -= 3;
            }
            for (int j = 0; j < 5; ++j) {
                boolean gen = true;
                AvengersItem item = null;
                while (gen) {
                    item = items.random(j);
                    gen = false;
                    if (!AvengersUtils.specialItem(item)) continue;
                    if (item == AvengersItem.WILD) {
                        if (!AvengersUtils.specialItem(matrix[0][j]) && !AvengersUtils.specialItem(matrix[1][j]) && !AvengersUtils.specialItem(matrix[2][j])) continue;
                        gen = true;
                        items.refundItem(item, j);
                        continue;
                    }
                    if (matrix[0][j] != item && matrix[1][j] != item && matrix[2][j] != item && matrix[0][j] != AvengersItem.WILD && matrix[1][j] != AvengersItem.WILD && matrix[2][j] != AvengersItem.WILD) continue;
                    gen = true;
                    items.refundItem(item, j);
                }
                matrix[r][j] = item;
            }
        }
        return matrix;
    }

    public static boolean specialItem(AvengersItem item) {
        return item == AvengersItem.BONUS || item == AvengersItem.SCATTER || item == AvengersItem.JACK_POT || item == AvengersItem.WILD;
    }

    public static AvengersItem[][] generateMatrixFreeSpin(String itemsWild) {
        int i;
        String[] arr = itemsWild.split(",");
        AvengersFreeSpinItems items = new AvengersFreeSpinItems();
        AvengersItem[][] matrix = new AvengersItem[3][5];
        if (arr.length > 0) {
            for (i = 0; i < arr.length - 1; i += 2) {
                int r = Integer.parseInt(arr[i]);
                int c = Integer.parseInt(arr[i + 1]);
                matrix[r][c] = AvengersItem.WILD;
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

    public static AvengersItem[][] generateMatrixNoHu(String[] lineArr) {
        AvengersItem[][] matrix = new AvengersItem[3][5];
        Random rd = new Random();
        int n = rd.nextInt(lineArr.length);
        int indexLineNoHu = Integer.parseInt(lineArr[n]) - 1;
        AvengersLines lines = new AvengersLines();
        AvengersItems items = new AvengersItems();
        Line lineNoHu = lines.get(indexLineNoHu);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                boolean genRandom = true;
                for (int k = 0; k < lineNoHu.getCells().size(); ++k) {
                    if (i != lineNoHu.getCell(k).getRow() || j != lineNoHu.getCell(k).getCol()) continue;
                    genRandom = false;
                    matrix[i][j] = AvengersItem.JACK_POT;
                }
                if (!genRandom) continue;
                AvengersItem item = AvengersItem.JACK_POT;
                while (item == AvengersItem.JACK_POT || item == AvengersItem.WILD) {
                    item = items.random(j);
                }
                matrix[i][j] = item;
            }
        }
        return matrix;
    }

    public static String matrixToString(AvengersItem[][] matrix) {
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

    public static Line getLine(AvengersLines lines, AvengersItem[][] matrix, int lineIndex) {
        Line line = lines.get(lineIndex - 1);
        for (Object obj : line.getCells()) {
            Cell cell = (Cell) obj;
            AvengersItem itemInMatrix = matrix[cell.getRow()][cell.getCol()];
            cell.setItem(itemInMatrix);
        }
        return line;
    }

    public static MiniGameSlotResponse addMiniGameSlot(int baseBetting, int countBonus) {
        Random rd = new Random();
        int indexRatioCol = rd.nextInt(3);
        int indexRatioRow = countBonus - 3;
        int ratio = Constant.AVENGERS_BONUS_RATIO[indexRatioRow][indexRatioCol];
        MiniGameSlotResponse res = AvengersUtils.generateMiniGameSlot(baseBetting);
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
            if ((numChonTiep = rd.nextInt(100) + 1) > Constant.AVENGERS_TANK_TI_LE_TRUOT[step]) {
                int indexCol = rd.nextInt(15);
                int prize = Constant.AVENGERS_TANK_PRIZES[step][indexCol] * baseBetting;
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
            MiniGameSlotResponse res = AvengersUtils.generateMiniGameSlot(100);
            total += res.getTotalPrize();
            System.out.println(res.getTotalPrize());
        }
        System.out.println("Trung binh: " + total / 1000L);
    }

    public static void calculateLine(Line line, List<AvengersAward> awardList) {
        int countNumItems = 0;
        AvengersItem itemSample = (AvengersItem)((Object)line.getCell(0).getItem());
        if (itemSample != AvengersItem.BONUS && itemSample != AvengersItem.SCATTER) {
            AvengersAward award;
            byte id;
            for (int j = 0; j < line.getCells().size() && ((id = ((AvengersItem)((Object)line.getCell(j).getItem())).getId()) == itemSample.getId() || itemSample.getId() != AvengersItem.JACK_POT.getId() && id == AvengersItem.WILD.getId()); ++j) {
                ++countNumItems;
            }
            if (countNumItems >= 3 && (award = AvengersAwards.getAward(itemSample, countNumItems)) != null) {
                awardList.add(award);
            }
        }
    }

    public static void calculateFreeSpinLine(Line line, List<AvengersFreeSpinAward> awardList) {
        int countNumItems = 0;
        AvengersItem itemSample = (AvengersItem)((Object)line.getCell(0).getItem());
        if (itemSample != AvengersItem.BONUS && itemSample != AvengersItem.SCATTER && itemSample != AvengersItem.JACK_POT) {
            AvengersFreeSpinAward award;
            for (int j = 0; j < line.getCells().size() && (line.getCell(j).getItem() == itemSample || line.getCell(j).getItem() == AvengersItem.WILD); ++j) {
                ++countNumItems;
            }
            if (countNumItems >= 3 && (award = AvengersFreeSpinAwards.getAward(itemSample, countNumItems)) != null) {
                awardList.add(award);
            }
        }
    }

    public static AvengersItem[][] revertMatrix(AvengersItem[][] m) {
        AvengersItem[][] matrix = new AvengersItem[3][5];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                if (matrix[i][j] != null) continue;
                matrix[i][j] = m[i][j];
                if (matrix[i][j] != AvengersItem.WILD) continue;
                matrix[0][j] = AvengersItem.WILD;
                matrix[1][j] = AvengersItem.WILD;
                matrix[2][j] = AvengersItem.WILD;
            }
        }
        return matrix;
    }
}

