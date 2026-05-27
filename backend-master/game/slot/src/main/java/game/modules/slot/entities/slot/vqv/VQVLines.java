package game.modules.slot.entities.slot.vqv;

import java.util.ArrayList;
import java.util.List;

public class VQVLines {
     private List lines = new ArrayList();

     public VQVLines() {
          this.initLines();
     }

     private void initLines() {
          this.lines.clear();
          Line line1 = new Line("line1", 1, 0, 1, 1, 1, 2, 1, 3, 1, 4);
          this.lines.add(line1);
          Line line2 = new Line("line2", 0, 0, 0, 1, 0, 2, 0, 3, 0, 4);
          this.lines.add(line2);
          Line line3 = new Line("line3", 2, 0, 2, 1, 2, 2, 2, 3, 2, 4);
          this.lines.add(line3);
          Line line4 = new Line("line4", 1, 0, 1, 1, 0, 2, 1, 3, 1, 4);
          this.lines.add(line4);
          Line line5 = new Line("line5", 1, 0, 1, 1, 2, 2, 1, 3, 1, 4);
          this.lines.add(line5);
          Line line6 = new Line("line6", 0, 0, 0, 1, 1, 2, 0, 3, 0, 4);
          this.lines.add(line6);
          Line line7 = new Line("line7", 2, 0, 2, 1, 1, 2, 2, 3, 2, 4);
          this.lines.add(line7);
          Line line8 = new Line("line8", 0, 0, 2, 1, 0, 2, 2, 3, 0, 4);
          this.lines.add(line8);
          Line line9 = new Line("line9", 2, 0, 0, 1, 2, 2, 0, 3, 2, 4);
          this.lines.add(line9);
          Line line10 = new Line("line10", 1, 0, 0, 1, 2, 2, 0, 3, 1, 4);
          this.lines.add(line10);
          Line line11 = new Line("line11", 2, 0, 1, 1, 0, 2, 1, 3, 2, 4);
          this.lines.add(line11);
          Line line12 = new Line("line12", 0, 0, 1, 1, 2, 2, 1, 3, 0, 4);
          this.lines.add(line12);
          Line line13 = new Line("line13", 1, 0, 2, 1, 1, 2, 0, 3, 1, 4);
          this.lines.add(line13);
          Line line14 = new Line("line14", 1, 0, 0, 1, 1, 2, 2, 3, 1, 4);
          this.lines.add(line14);
          Line line15 = new Line("line15", 2, 0, 1, 1, 1, 2, 1, 3, 2, 4);
          this.lines.add(line15);
          Line line16 = new Line("line16", 0, 0, 1, 1, 1, 2, 1, 3, 0, 4);
          this.lines.add(line16);
          Line line17 = new Line("line17", 1, 0, 0, 1, 0, 2, 0, 3, 1, 4);
          this.lines.add(line17);
          Line line18 = new Line("line18", 1, 0, 2, 1, 2, 2, 2, 3, 1, 4);
          this.lines.add(line18);
          Line line19 = new Line("line19", 2, 0, 2, 1, 1, 2, 0, 3, 0, 4);
          this.lines.add(line19);
          Line line20 = new Line("line20", 0, 0, 0, 1, 1, 2, 2, 3, 2, 4);
          this.lines.add(line20);
     }

     public List list() {
          return this.lines;
     }

     public Line get(int index) {
          return (Line)this.lines.get(index);
     }

     public void renew() {
     }
}
