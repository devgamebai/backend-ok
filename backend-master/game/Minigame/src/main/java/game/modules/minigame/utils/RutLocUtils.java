/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public class RutLocUtils {
    private static final int[] prizes = new int[]{500000, 200000, 100000, 50000, 20000, 10000, 5000};

    public static short[] tiLeGiaiThuong(long fund) {
        if (100000L <= fund && fund < 2000000L) {
            return new short[]{0, 0, 0, 0, 0, 0, 100};
        }
        if (2000000L <= fund && fund < 5000000L) {
            return new short[]{0, 0, 0, 0, 0, 10, 100};
        }
        if (5000000L <= fund && fund < 20000000L) {
            return new short[]{0, 0, 0, 0, 5, 20, 100};
        }
        if (20000000L <= fund && fund < 50000000L) {
            return new short[]{0, 0, 1, 2, 10, 35, 100};
        }
        if (50000000L <= fund) {
            return new short[]{1, 2, 4, 6, 15, 40, 100};
        }
        return new short[]{0, 0, 0, 0, 0, 0, 0};
    }

    public static int getPrize(short[] tiLe) {
        if (tiLe.length != prizes.length) {
            return 0;
        }
        Random rd = new Random();
        int n = rd.nextInt(100);
        for (int i = 0; i < tiLe.length; ++i) {
            if (n >= tiLe[i]) continue;
            return prizes[i];
        }
        return 0;
    }

    public static int[] getPrizes(long fund, int maxPrizes) {
        short[] tiLe = RutLocUtils.tiLeGiaiThuong(fund);
        int[] outPrizes = new int[maxPrizes];
        for (int i = 0; i < maxPrizes; ++i) {
            outPrizes[i] = RutLocUtils.getPrize(tiLe);
        }
        return outPrizes;
    }

    public static int[] phanBoGiaiThuong(int tongSoNguoiRutLocLanTruoc, int maxPrizes) {
        int[] phanBo = new int[maxPrizes];
        if (tongSoNguoiRutLocLanTruoc <= maxPrizes) {
            int i = 0;
            while (i < maxPrizes) {
                phanBo[i] = i++;
            }
        } else {
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (int i = 0; i < tongSoNguoiRutLocLanTruoc; ++i) {
                list.add(i);
            }
            Random rd = new Random();
            for (int i = 0; i < maxPrizes; ++i) {
                int n = rd.nextInt(list.size());
                phanBo[i] = (Integer)list.remove(n);
            }
        }
        return phanBo;
    }

    public static int getLuotRutLoc(long moneyExchange) {
        if (20000L <= moneyExchange && moneyExchange < 100000L) {
            return 1;
        }
        if (100000L <= moneyExchange && moneyExchange < 500000L) {
            return 5;
        }
        if (500000L <= moneyExchange && moneyExchange < 10000000L) {
            return 10;
        }
        if (10000000L <= moneyExchange && moneyExchange < 50000000L) {
            return 15;
        }
        if (50000000L <= moneyExchange && moneyExchange < 200000000L) {
            return 25;
        }
        if (200000000L <= moneyExchange) {
            return 40;
        }
        return 0;
    }

    private static void testTiLe() {
        int i;
        long fund = 50000000L;
        int maxPrizes = 100000;
        int[] outPrizes = RutLocUtils.getPrizes(fund, maxPrizes);
        int[] thongKe = new int[]{0, 0, 0, 0, 0, 0, 0};
        for (i = 0; i < maxPrizes; ++i) {
            for (int j = 0; j < prizes.length; ++j) {
                if (outPrizes[i] != prizes[j]) continue;
                int[] arrn = thongKe;
                int n = j;
                arrn[n] = arrn[n] + 1;
            }
        }
        for (i = 0; i < thongKe.length; ++i) {
            System.out.println("" + prizes[i] + " \t " + thongKe[i] + " \t " + (float)thongKe[i] / (float)maxPrizes * 100.0f);
        }
    }

    private static void testPhanBoGiaiThuong() {
        int soNguoi = 20;
        int maxPrizes = 10;
        int[] result = RutLocUtils.phanBoGiaiThuong(soNguoi, maxPrizes);
        System.out.println("PHAN BO GIAI THUONG, so nguoi = " + soNguoi + ", tong giai = " + maxPrizes);
        for (int i = 0; i < result.length; ++i) {
            System.out.println(result[i]);
        }
    }

    public static void main(String[] args) {
        RutLocUtils.testTiLe();
        RutLocUtils.testPhanBoGiaiThuong();
    }
}

