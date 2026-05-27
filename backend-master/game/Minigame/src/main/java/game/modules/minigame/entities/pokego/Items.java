/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

import game.modules.minigame.entities.pokego.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Items {
    private int[] config = new int[]{1, 6, 12, 12, 20, 20};
    public List<Item> items = new ArrayList<Item>();

    public Items() {
        for (int i = 0; i < this.config.length; ++i) {
            for (int j = 0; j < this.config[i]; ++j) {
                this.items.add(Item.findItem((byte)i));
            }
        }
    }

    public int size() {
        return this.items.size();
    }

    public Item random() {
        Random rd = new Random();
        int index = rd.nextInt(this.items.size());
        return this.items.get(index);
    }
}

