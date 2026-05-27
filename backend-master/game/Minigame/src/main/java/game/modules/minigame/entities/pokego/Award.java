/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

import game.modules.minigame.entities.pokego.Item;

public enum Award {
    TRIPLE_POKER_BALL((byte)1, Item.POKER_BALL, (byte)3, -2.0f),
    TRIPLE_PIKACHU((byte)2, Item.PIKACHU, (byte)3, 85.0f),
    TRIPLE_BULBASAUR((byte)3, Item.BULBASAUR, (byte)3, 40.0f),
    TRIPLE_CLEFABLE((byte)4, Item.CLEFABLE, (byte)3, 20.0f),
    TRIPLE_MOUSE((byte)5, Item.MOUSE, (byte)3, 8.0f),
    DOUBLE_MOUSE((byte)6, Item.MOUSE, (byte)2, 0.8f),
    TRIPLE_TOGEPI((byte)7, Item.TOGEPI, (byte)3, 3.0f),
    DOUBLE_TOGEPI((byte)8, Item.TOGEPI, (byte)2, 0.4f);
    
    private byte id;
    private Item item;
    private byte duplicate;
    private float ratio;

    private Award(byte id, Item item, byte duplicate, float ratio) {
        this.id = id;
        this.item = item;
        this.duplicate = duplicate;
        this.ratio = ratio;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return this.item;
    }

    public void setDuplicate(byte duplicate) {
        this.duplicate = duplicate;
    }

    public byte getDuplicate() {
        return this.duplicate;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    public float getRatio() {
        return this.ratio;
    }
}

