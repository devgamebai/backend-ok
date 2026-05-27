/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

import game.modules.minigame.entities.pokego.Cell;
import game.modules.minigame.entities.pokego.Item;
import java.util.ArrayList;
import java.util.List;

public class Line {
    private String name;
    private List<Cell> cells = new ArrayList<Cell>();

    public Line(String name) {
        this.name = name;
    }

    public Line(String name, List<Cell> cells) {
        this.name = name;
        this.cells = cells;
    }

    public Line(String name, int r1, int c1, int r2, int c2, int r3, int c3) {
        this.name = name;
        Cell cell1 = new Cell(r1, c1);
        this.cells.add(cell1);
        Cell cell2 = new Cell(r2, c2);
        this.cells.add(cell2);
        Cell cell3 = new Cell(r3, c3);
        this.cells.add(cell3);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Cell> getCells() {
        return this.cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public Cell getCell(int index) {
        return this.cells.get(index);
    }

    public Item getItem(int index) {
        return this.cells.get(index).getItem();
    }
}

