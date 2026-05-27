/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

import game.modules.minigame.entities.pokego.Award;
import game.modules.minigame.entities.pokego.Item;
import java.util.ArrayList;
import java.util.List;

public class Awards {
    private static List<Award> awards = new ArrayList<Award>();

    public Awards() {
        for (Award entry : Award.values()) {
            awards.add(entry);
        }
    }

    public static List<Award> list() {
        return awards;
    }

    public static Award getAward(Item item, int numItems) {
        for (Award entry : Award.values()) {
            if (entry.getItem() != item || entry.getDuplicate() != numItems) continue;
            return entry;
        }
        return null;
    }
}

