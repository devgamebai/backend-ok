package game.modules.slot.entities.slot.khobau;

import java.util.ArrayList;
import java.util.List;

public class Line {
     private String name;
     private List cells = new ArrayList();

     public Line(String name) {
          this.name = name;
     }

     public Line(String name, List cells) {
          this.name = name;
          this.cells = cells;
     }

     public Line(String name, int r1, int c1, int r2, int c2, int r3, int c3, int r4, int c4, int r5, int c5) {
          this.name = name;
          CellKhoBau cell1 = new CellKhoBau(r1, c1);
          this.cells.add(cell1);
          CellKhoBau cell2 = new CellKhoBau(r2, c2);
          this.cells.add(cell2);
          CellKhoBau cell3 = new CellKhoBau(r3, c3);
          this.cells.add(cell3);
          CellKhoBau cell4 = new CellKhoBau(r4, c4);
          this.cells.add(cell4);
          CellKhoBau cell5 = new CellKhoBau(r5, c5);
          this.cells.add(cell5);
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          this.name = name;
     }

     public List getCells() {
          return this.cells;
     }

     public void setCells(List cells) {
          this.cells = cells;
     }

     public CellKhoBau getCell(int index) {
          return (CellKhoBau)this.cells.get(index);
     }

     public KhoBauItem getItem(int index) {
          return ((CellKhoBau)this.cells.get(index)).getItem();
     }
}
