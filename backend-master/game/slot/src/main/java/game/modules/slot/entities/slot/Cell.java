package game.modules.slot.entities.slot;

public class Cell {
     private int row;
     private int col;
     private Object item;

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

     public Object getItem() {
          return this.item;
     }

     public void setItem(Object item) {
          this.item = item;
     }
}
