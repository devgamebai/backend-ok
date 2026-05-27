/*
 * Decompiled with CFR 0.152.
 */
package game.GameConfig.ConfigGame.BotJackpotConfig;

import game.utils.GameUtil;

public class Slot3x3BotConfig {
    public int[][] jackPotPer5s;
    public int[][] timeBotEat;

    public int randomJackPotPer5s(int betLevel) {
        return GameUtil.randomBetween(this.jackPotPer5s[betLevel][0], this.jackPotPer5s[betLevel][1]);
    }

    public int randomTimeBotEat(int betLevel) {
        return GameUtil.randomBetween(this.timeBotEat[betLevel][0], this.timeBotEat[betLevel][1]);
    }
}

