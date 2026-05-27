package game.modules.slot.entities.slot.khobau;

public class CellKhoBau {
     private int row;
     private int col;
     private KhoBauItem item;

     public CellKhoBau(int row, int col) {
          this.item = KhoBauItem.NONE;
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

     public KhoBauItem getItem() {
          return this.item;
     }

     public void setItem(KhoBauItem item) {
          this.item = item;
     }
}
