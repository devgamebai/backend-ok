package game.modules.slot.entities.slot.vqv;

public class CellVQV {
     private int row;
     private int col;
     private VQVItem item;

     public CellVQV(int row, int col) {
          this.item = VQVItem.NONE;
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

     public VQVItem getItem() {
          return this.item;
     }

     public void setItem(VQVItem item) {
          this.item = item;
     }
}
