/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.GameConfig;

import bitzero.util.common.business.Debug;
import game.utils.GameUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class BotBetConfig {
    public static int[] xocDiaNumberBot = new int[]{135, 90, 45, 45, 45, 45, 45, 90, 135, 135, 180, 180, 180, 180, 135, 135, 135, 135, 135, 180, 180, 180, 180, 135};
    public static int[] xocDiaFundBet = new int[]{4000000, 2500000, 2500000, 2500000, 2500000, 2500000, 2500000, 2500000, 4000000, 4000000, 5000000, 5000000, 5000000, 5000000, 4000000, 4000000, 4000000, 4000000, 4000000, 5000000, 5000000, 5000000, 5000000, 4000000};
    public static int[] xocDiaFundBetDelta = new int[]{4000000, 2000000, 1000000, 1000000, 1000000, 1000000, 1000000, 2000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000, 4000000};

    public static byte getHourOfDay() {
        return (byte)Calendar.getInstance().get(11);
    }

    public static synchronized ArrayList<ArrayList<Long>> getListBetBotXocDia() {
        byte hourOfDay = BotBetConfig.getHourOfDay();
        int[] number = new int[6];
        for (int i = 0; i < number.length; ++i) {
            number[i] = xocDiaNumberBot[hourOfDay] / 10;
        }
        int currentNumber = xocDiaNumberBot[hourOfDay] - number[0] * number.length;
        for (int i = 0; i < currentNumber; ++i) {
            int value;
            int n = value = GameUtil.randomMax(number.length);
            number[n] = number[n] + 1;
        }
        int[] goldBet = new int[number.length];
        for (int i = 0; i < goldBet.length; ++i) {
            goldBet[i] = xocDiaFundBet[hourOfDay] + GameUtil.randomMax(xocDiaFundBetDelta[hourOfDay] / 10000) * 10000;
        }
        ArrayList<ArrayList<Long>> listReturn = new ArrayList<ArrayList<Long>>();
        for (int i = 0; i < number.length; ++i) {
            ArrayList<Long> listBet = BotBetConfig.getListBet(goldBet[i], number[i]);
            Collections.shuffle(listBet);
            listReturn.add(listBet);
        }
        return listReturn;
    }

    public static ArrayList<Long> getListBet(long money, int number) {
        ArrayList<Long> listBet = new ArrayList<Long>();
        long goldTB = (money /= 10L) / (long)number;
        if (goldTB > 100000L) {
            long[] goldForBetLevel = new long[4];
            goldForBetLevel[3] = (long)((double)money * 37.5 / 100.0);
            goldForBetLevel[2] = (long)((double)money * 37.5 / 100.0);
            goldForBetLevel[1] = (long)((double)money * 24.0 / 100.0);
            goldForBetLevel[0] = money - goldForBetLevel[3] - goldForBetLevel[2] - goldForBetLevel[1];
            long currentGoldBetLevel3 = goldForBetLevel[3] / 1000000L * 1000000L;
            goldForBetLevel[2] = goldForBetLevel[2] + (goldForBetLevel[3] - currentGoldBetLevel3);
            goldForBetLevel[3] = currentGoldBetLevel3 / 1000000L;
            long currentGoldBetLevel2 = goldForBetLevel[2] / 100000L * 100000L;
            goldForBetLevel[1] = goldForBetLevel[1] + (goldForBetLevel[2] - currentGoldBetLevel2);
            goldForBetLevel[2] = currentGoldBetLevel2 / 100000L;
            long currentGoldBetLevel1 = goldForBetLevel[1] / 10000L * 10000L;
            goldForBetLevel[0] = goldForBetLevel[0] + (goldForBetLevel[1] - currentGoldBetLevel1);
            goldForBetLevel[1] = currentGoldBetLevel1 / 10000L;
            goldForBetLevel[0] = goldForBetLevel[0] / 1000L;
            int[] botForBetLevel = new int[4];
            botForBetLevel[3] = number * 5 / 100;
            if (botForBetLevel[3] == 0) {
                botForBetLevel[3] = 1;
            }
            botForBetLevel[2] = number * 25 / 100;
            botForBetLevel[1] = number * 40 / 100;
            botForBetLevel[0] = number - botForBetLevel[3] - botForBetLevel[2] - botForBetLevel[1];
            int[] listBet1K = BotBetConfig.getList1KByValue(botForBetLevel[0], (int)goldForBetLevel[0]);
            for (int i = 0; i < listBet1K.length; ++i) {
                listBet.add((long)listBet1K[i] * 1000L * 10L);
            }
            int[] listBet10K = BotBetConfig.getList10KByValue(botForBetLevel[1], (int)goldForBetLevel[1]);
            for (int i = 0; i < listBet10K.length; ++i) {
                listBet.add((long)listBet10K[i] * 10000L * 10L);
            }
            int[] listBet100K = BotBetConfig.getList100KByValue(botForBetLevel[2], (int)goldForBetLevel[2]);
            for (int i = 0; i < listBet100K.length; ++i) {
                listBet.add((long)listBet100K[i] * 100000L * 10L);
            }
            int[] listBet1000K = BotBetConfig.getList1000KByValue(botForBetLevel[3], (int)goldForBetLevel[3]);
            for (int i = 0; i < listBet1000K.length; ++i) {
                listBet.add((long)listBet1000K[i] * 1000000L * 10L);
            }
        } else {
            long[] goldForBetLevel = new long[3];
            goldForBetLevel[2] = money * 60L / 100L;
            goldForBetLevel[1] = (long)((double)money * 38.5 / 100.0);
            goldForBetLevel[0] = money - goldForBetLevel[2] - goldForBetLevel[1];
            long currentGoldBetLevel2 = goldForBetLevel[2] / 100000L * 100000L;
            goldForBetLevel[1] = goldForBetLevel[1] + (goldForBetLevel[2] - currentGoldBetLevel2);
            goldForBetLevel[2] = currentGoldBetLevel2 / 100000L;
            long currentGoldBetLevel1 = goldForBetLevel[1] / 10000L * 10000L;
            goldForBetLevel[0] = goldForBetLevel[0] + (goldForBetLevel[1] - currentGoldBetLevel1);
            goldForBetLevel[1] = currentGoldBetLevel1 / 10000L;
            goldForBetLevel[0] = goldForBetLevel[0] / 1000L;
            int[] botForBetLevel = new int[3];
            botForBetLevel[2] = number * 30 / 100 + 1;
            botForBetLevel[1] = number * 40 / 100;
            botForBetLevel[0] = number - botForBetLevel[2] - botForBetLevel[1];
            int[] listBet1K = BotBetConfig.getList1KByValue(botForBetLevel[0], (int)goldForBetLevel[0]);
            for (int i = 0; i < listBet1K.length; ++i) {
                listBet.add((long)listBet1K[i] * 1000L * 10L);
            }
            int[] listBet10K = BotBetConfig.getList10KByValue(botForBetLevel[1], (int)goldForBetLevel[1]);
            for (int i = 0; i < listBet10K.length; ++i) {
                listBet.add((long)listBet10K[i] * 10000L * 10L);
            }
            int[] listBet100K = BotBetConfig.getList100KByValue(botForBetLevel[2], (int)goldForBetLevel[2]);
            for (int i = 0; i < listBet100K.length; ++i) {
                listBet.add((long)listBet100K[i] * 100000L * 10L);
            }
        }
        return listBet;
    }

    public static int[] getList1KByValue(int numberList, int value) {
        int i;
        int[] goldForPlayer = new int[numberList];
        for (int i2 = 0; i2 < goldForPlayer.length; ++i2) {
            goldForPlayer[i2] = 1;
            if (--value > 0) continue;
            return goldForPlayer;
        }
        int value10 = value / 18;
        value -= value10 * 9;
        for (int i3 = 0; i3 < value10; ++i3) {
            int n = GameUtil.randomMax(goldForPlayer.length);
            goldForPlayer[n] = goldForPlayer[n] + 9;
        }
        int value5 = value / 4;
        int valueLe = value % 4;
        for (i = 0; i < value5; ++i) {
            int n = GameUtil.randomMax(goldForPlayer.length);
            goldForPlayer[n] = goldForPlayer[n] + 4;
        }
        for (i = 0; i < valueLe; ++i) {
            int n = GameUtil.randomMax(goldForPlayer.length);
            goldForPlayer[n] = goldForPlayer[n] + 1;
        }
        int count = 0;
        for (int i4 = 0; i4 < goldForPlayer.length; ++i4) {
            count += goldForPlayer[i4];
        }
        Debug.trace((Object[])new Object[]{count});
        return goldForPlayer;
    }

    public static int[] getList10KByValue(int numberList, int value) {
        int i;
        int[] goldForPlayer = new int[numberList];
        for (i = 0; i < goldForPlayer.length; ++i) {
            goldForPlayer[i] = 1;
            if (--value > 0) continue;
            return goldForPlayer;
        }
        for (i = 0; i < value; ++i) {
            int n = GameUtil.randomMax(goldForPlayer.length);
            goldForPlayer[n] = goldForPlayer[n] + 1;
        }
        return goldForPlayer;
    }

    public static int[] getList100KByValue(int numberList, int value) {
        int i;
        int[] goldForPlayer = new int[numberList];
        for (i = 0; i < goldForPlayer.length; ++i) {
            goldForPlayer[i] = 1;
            if (--value > 0) continue;
            return goldForPlayer;
        }
        for (i = 0; i < value; ++i) {
            int n = GameUtil.randomMax(goldForPlayer.length);
            goldForPlayer[n] = goldForPlayer[n] + 1;
        }
        return goldForPlayer;
    }

    public static int[] getList1000KByValue(int numberList, int value) {
        int i;
        int[] goldForPlayer = new int[numberList];
        for (i = 0; i < goldForPlayer.length; ++i) {
            goldForPlayer[i] = 1;
            if (--value > 0) continue;
            return goldForPlayer;
        }
        for (i = 0; i < value; ++i) {
            int n = GameUtil.randomMax(goldForPlayer.length);
            goldForPlayer[n] = goldForPlayer[n] + 1;
        }
        return goldForPlayer;
    }
}

