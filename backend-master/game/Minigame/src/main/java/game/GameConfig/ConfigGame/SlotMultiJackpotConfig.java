/*
 * Decompiled with CFR 0.152.
 */
package game.GameConfig.ConfigGame;

import game.utils.GameUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotMultiJackpotConfig {
    public Map<String, List<Integer>> mapMultiJackpot;
    public Map<String, Boolean> mapIsMultiJackot = new HashMap<String, Boolean>();

    public boolean isMultiJackpot(String gameName) {
        if (this.mapIsMultiJackot.containsKey(gameName)) {
            return this.mapIsMultiJackot.get(gameName);
        }
        return false;
    }

    public void checkIsJackPotAllGame() {
        this.mapIsMultiJackot.clear();
        int currentDay = GameUtil.getDayNumber();
        for (Map.Entry<String, List<Integer>> entry : this.mapMultiJackpot.entrySet()) {
            String key = entry.getKey();
            List<Integer> value = entry.getValue();
            boolean isJackpot = value.contains(currentDay);
            this.mapIsMultiJackot.put(key, isJackpot);
        }
    }
}

