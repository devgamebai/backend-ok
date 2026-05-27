package game.modules.slot.entities.slot;

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
          Cell cell1 = new Cell(r1, c1);
          this.cells.add(cell1);
          Cell cell2 = new Cell(r2, c2);
          this.cells.add(cell2);
          Cell cell3 = new Cell(r3, c3);
          this.cells.add(cell3);
          Cell cell4 = new Cell(r4, c4);
          this.cells.add(cell4);
          Cell cell5 = new Cell(r5, c5);
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

     public Cell getCell(int index) {
          return (Cell)this.cells.get(index);
     }
}
