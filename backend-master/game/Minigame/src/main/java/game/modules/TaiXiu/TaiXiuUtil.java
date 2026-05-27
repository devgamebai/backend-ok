/*
 * Decompiled with CFR 0.152.
 */
package game.modules.TaiXiu;

import game.utils.GameUtil;
import java.util.SplittableRandom;

public class TaiXiuUtil {
    private static final SplittableRandom rdom = new SplittableRandom();
    private static final int DICE_TAI_VALUE = 11;

    public static boolean isXiu(short[] dices) {
        int value = 0;
        for (int i = 0; i < dices.length; ++i) {
            value += dices[i];
        }
        return value < 11;
    }

    public static short[] genarateResult(boolean isTai) {
        int value;
        short[] dices = new short[3];
        do {
            value = 0;
            for (int i = 0; i < dices.length; ++i) {
                dices[i] = (short)GameUtil.randomBetween(1, 7);
                value += dices[i];
            }
        } while (value < 11 == isTai);
        int totalDiceTemp = dices[0] + dices[1] + dices[2];
        if (totalDiceTemp == 3 || totalDiceTemp == 18) {
            TaiXiuUtil.genarateResult(isTai);
        }
        return dices;
    }

    public static short[] genarateRandomResult() {
        short t1 = (short)GameUtil.randomBetween(1, 7);
        short t2 = (short)GameUtil.randomBetween(1, 7);
        short t3 = (short)GameUtil.randomBetween(1, 7);
        return new short[]{t1, t2, t3};
    }
}

