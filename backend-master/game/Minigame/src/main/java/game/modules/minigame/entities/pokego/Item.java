/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

public enum Item {
    NONE("none", (byte)-1),
    POKER_BALL("poker_ball", (byte)0),
    PIKACHU("pikachu", (byte)1),
    BULBASAUR("bulbasaur", (byte)2),
    CLEFABLE("clefable", (byte)3),
    MOUSE("mouse", (byte)4),
    TOGEPI("togepi", (byte)5);
    
    private String name;
    private byte id;

    private Item(String name, byte id) {
        this.name = name;
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public static Item findItem(byte id) {
        for (Item entry : Item.values()) {
            if (entry.getId() != id) continue;
            return entry;
        }
        return null;
    }
}

