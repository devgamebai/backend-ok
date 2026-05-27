/*
 * Decompiled with CFR 0.152.
 */
package game.GameConfig.ConfigGame.BotJackpotConfig;

import game.utils.GameUtil;

public class CaoThapBotConfig {
    public int[][] jackPotPer5s = new int[][]{{70, 140}, {250, 400}, {600, 1100}};
    public int[][] timeBotEat = new int[][]{{3000, 4200}, {7200, 14400}, {36000, 48000}};

    public int randomJackPotPer5s(int betLevel) {
        return GameUtil.randomBetween(this.jackPotPer5s[betLevel][0], this.jackPotPer5s[betLevel][1]);
    }

    public int randomTimeBotEat(int betLevel) {
        return GameUtil.randomBetween(this.timeBotEat[betLevel][0], this.timeBotEat[betLevel][1]);
    }
}

