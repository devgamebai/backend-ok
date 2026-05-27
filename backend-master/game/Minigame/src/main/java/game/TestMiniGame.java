/*
 * Decompiled with CFR 0.152.
 */
package game;

import game.modules.TaiXiu.TaiXiuUtil;

public class TestMiniGame {
    public static void main(String[] args) {
        long moneyMinusFund;
        long moneyBetXiu = 1000000L;
        long moneyBetTai = 2000000L;
        long fundTaiXiu = -50000L;
        long tax = 2L;
        short[] result = TaiXiuUtil.genarateRandomResult();
        if (TaiXiuUtil.isXiu(result)) {
            if (moneyBetXiu > moneyBetTai && fundTaiXiu - (moneyMinusFund = moneyBetXiu * (100L - tax) / 100L - moneyBetTai) < 0L) {
                result = TaiXiuUtil.genarateResult(true);
            }
        } else if (moneyBetTai > moneyBetXiu && fundTaiXiu - (moneyMinusFund = moneyBetTai * (100L - tax) / 100L - moneyBetXiu) < 0L) {
            result = TaiXiuUtil.genarateResult(false);
        }
        if (TaiXiuUtil.isXiu(result)) {
            moneyMinusFund = moneyBetXiu * (100L - tax) / 100L - moneyBetTai;
            fundTaiXiu += moneyMinusFund;
        } else {
            moneyMinusFund = moneyBetTai * (100L - tax) / 100L - moneyBetXiu;
            fundTaiXiu += moneyMinusFund;
        }
    }
}

