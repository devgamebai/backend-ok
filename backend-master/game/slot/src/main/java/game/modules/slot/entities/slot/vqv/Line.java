package game.modules.slot.entities.slot.vqv;

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
          CellVQV cell1 = new CellVQV(r1, c1);
          this.cells.add(cell1);
          CellVQV cell2 = new CellVQV(r2, c2);
          this.cells.add(cell2);
          CellVQV cell3 = new CellVQV(r3, c3);
          this.cells.add(cell3);
          CellVQV cell4 = new CellVQV(r4, c4);
          this.cells.add(cell4);
          CellVQV cell5 = new CellVQV(r5, c5);
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

     public CellVQV getCell(int index) {
          return (CellVQV)this.cells.get(index);
     }

     public VQVItem getItem(int index) {
          return ((CellVQV)this.cells.get(index)).getItem();
     }
}
