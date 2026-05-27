package game.modules.slot.entities.slot.thanden;

public class CellThanDen {
    private int row;
    private int col;
    private ThanDenItem item;

    public CellThanDen(int row, int col) {
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

    public ThanDenItem getItem() {
        return this.item;
    }

    public void setItem(ThanDenItem item) {
        this.item = item;
    }
}
