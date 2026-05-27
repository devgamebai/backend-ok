/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

import game.modules.minigame.entities.pokego.Item;

public class Cell {
    private int row;
    private int col;
    private Item item = Item.NONE;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return this.row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return this.col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public Item getItem() {
        return this.item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}

