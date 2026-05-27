/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  org.apache.log4j.Logger
 */
package game.modules.minigame.utils;

import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import game.modules.minigame.entities.pokego.Award;
import game.modules.minigame.entities.pokego.Awards;
import game.modules.minigame.entities.pokego.Cell;
import game.modules.minigame.entities.pokego.Item;
import game.modules.minigame.entities.pokego.Items;
import game.modules.minigame.entities.pokego.Line;
import game.modules.minigame.entities.pokego.Lines;
import game.utils.ConfigGame;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;
import org.apache.log4j.Logger;

public class PokeGoUtils {
    private static Logger logger = Logger.getLogger((String)"csvPokeGo");
    private static String FORMAT_PLAY_POKE_GO = ", %10d, %15s, %8d, %20s, %5d, %5d, %10d, %15s, %20s";

    public static void log(long referenceId, String username, int betValue, String matrix, short result, short moneyType, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        logger.debug(String.format(FORMAT_PLAY_POKE_GO, referenceId, username, betValue, matrixStr, result, moneyType, handleTime, ratioTime, timeLog));
    }

    public static Item[][] generateMatrix() {
        Items items = new Items();
        Item[][] matrix = new Item[3][3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                matrix[i][j] = items.random();
            }
        }
        return matrix;
    }

    public static Item[][] generateMatrixNoHu(String[] lineArr) {
        Item[][] matrix = new Item[3][3];
        Random rd = new Random();
        int n = rd.nextInt(lineArr.length);
        int indexLineNoHu = Integer.parseInt(lineArr[n]) - 1;
        Lines lines = new Lines();
        Items items = new Items();
        Line lineNoHu = lines.get(indexLineNoHu);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                boolean genRandom = true;
                for (int k = 0; k < lineNoHu.getCells().size(); ++k) {
                    if (i != lineNoHu.getCell(k).getRow() || j != lineNoHu.getCell(k).getCol()) continue;
                    genRandom = false;
                    matrix[i][j] = Item.POKER_BALL;
                }
                if (!genRandom) continue;
                matrix[i][j] = items.random();
            }
        }
        return matrix;
    }

    public static String matrixToString(Item[][] matrix) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                builder.append(",");
                builder.append(matrix[i][j].getId());
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    public static Line getLine(Lines lines, Item[][] matrix, int lineIndex) {
        Line line = lines.get(lineIndex - 1);
        for (Cell cell : line.getCells()) {
            Item itemInMatrix = matrix[cell.getRow()][cell.getCol()];
            cell.setItem(itemInMatrix);
        }
        return line;
    }

    public static void calculateLine(Line line, List<Award> awardList) {
        for (int i = 0; i < line.getCells().size(); ++i) {
            Award award;
            int countNumItems = 0;
            Item itemSample = line.getItem(i);
            for (int j = 0; j < line.getCells().size(); ++j) {
                if (line.getItem(j) != itemSample && line.getItem(j) != Item.POKER_BALL) continue;
                ++countNumItems;
            }
            if (countNumItems < 2 || (award = Awards.getAward(itemSample, countNumItems)) == null || PokeGoUtils.checkAwardExist(awardList, award)) continue;
            awardList.add(award);
        }
    }

    private static boolean checkAwardExist(List<Award> awardList, Award awardLine) {
        for (Award award : awardList) {
            if (award != awardLine) continue;
            return true;
        }
        return false;
    }

    public static int[] getX2Days() {
        String x2DaysStr = ConfigGame.getValueString("poke_go_days_x2");
        String[] arr = x2DaysStr.split(",");
        int[] result = new int[arr.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = Integer.parseInt(arr[i]);
        }
        return result;
    }

    public static int getLastDayX2() {
        CacheServiceImpl cache = new CacheServiceImpl();
        try {
            return cache.getValueInt("poke_go_last_day_x2");
        }
        catch (KeyNotFoundException e) {
            int lastDay = ConfigGame.getIntValue("poke_last_day_gio_vang");
            PokeGoUtils.saveLastDayX2(lastDay);
            return lastDay;
        }
    }

    public static void saveLastDayX2(int lastDay) {
        CacheServiceImpl cache = new CacheServiceImpl();
        cache.setValue("poke_go_last_day_x2", lastDay);
    }

    public static void main(String[] args) {
        String lines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
        Item[][] matrix = PokeGoUtils.generateMatrixNoHu(lines.split(","));
        for (int i = 0; i < 3; ++i) {
            StringBuilder b = new StringBuilder();
            for (int j = 0; j < 3; ++j) {
                b.append(" " + matrix[i][j].getId());
            }
            System.out.println(b.toString());
        }
    }
}

