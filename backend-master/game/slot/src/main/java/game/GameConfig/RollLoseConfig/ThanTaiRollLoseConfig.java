package game.GameConfig.RollLoseConfig;

import game.GameConfig.SlotConfig.ThanTaiConfig;
import game.modules.GameUtil;
import game.modules.ThanTai.ThanTaiUtil;

public class ThanTaiRollLoseConfig {
    public byte[][] rollLose0;

    public byte[] getTableRollLose() {
        if (rollLose0 != null && rollLose0.length > 0) {
            int index = GameUtil.randomMax(rollLose0.length);
            return rollLose0[index].clone();
        }
        return generateRandomTable();
    }

    private byte[] generateRandomTable() {
        ThanTaiConfig config = new ThanTaiConfig();
        byte[] table = config.generateRandomTable();
        return ThanTaiUtil.validateTable(table);
    }
}
