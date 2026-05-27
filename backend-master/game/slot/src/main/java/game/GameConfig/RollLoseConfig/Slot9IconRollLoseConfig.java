package game.GameConfig.RollLoseConfig;

import game.modules.GameUtil;

public class Slot9IconRollLoseConfig {
    public byte[][] rollLose0;

    public byte[] getTableRollLose() {
        return this.rollLose0[GameUtil.randomMax(this.rollLose0.length)];
    }
}
