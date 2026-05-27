package game.GameConfig.RollLoseConfig;

import game.GameConfig.GameConfig;
import game.modules.GameUtil;

public class Slot11IconWildLienTucRollLoseConfig {
    public byte[][] rollLose0;

    public byte[] getTableRollLose(){
        if (this.rollLose0 == null || this.rollLose0.length == 0) {
            // Fallback: return random table from config
            return GameConfig.getInstance().slot11IconWildLienTucConfig.generateRandomTable();
        }
        return this.rollLose0[GameUtil.randomMax(this.rollLose0.length)];
    }
}
