/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

import game.modules.minigame.entities.pokego.Line;
import java.util.ArrayList;
import java.util.List;

public class Lines {
    private List<Line> lines = new ArrayList<Line>();

    public Lines() {
        this.initLines();
    }

    private void initLines() {
        this.lines.clear();
        Line line1 = new Line("line1", 0, 0, 0, 1, 0, 2);
        this.lines.add(line1);
        Line line2 = new Line("line2", 1, 0, 1, 1, 1, 2);
        this.lines.add(line2);
        Line line3 = new Line("line3", 2, 0, 2, 1, 2, 2);
        this.lines.add(line3);
        Line line4 = new Line("line4", 0, 0, 2, 1, 0, 2);
        this.lines.add(line4);
        Line line5 = new Line("line5", 2, 0, 0, 1, 2, 2);
        this.lines.add(line5);
        Line line6 = new Line("line6", 0, 0, 1, 1, 0, 2);
        this.lines.add(line6);
        Line line7 = new Line("line7", 0, 0, 1, 1, 2, 2);
        this.lines.add(line7);
        Line line8 = new Line("line8", 2, 0, 1, 1, 0, 2);
        this.lines.add(line8);
        Line line9 = new Line("line9", 1, 0, 2, 1, 1, 2);
        this.lines.add(line9);
        Line line10 = new Line("line10", 1, 0, 0, 1, 1, 2);
        this.lines.add(line10);
        Line line11 = new Line("line11", 2, 0, 1, 1, 2, 2);
        this.lines.add(line11);
        Line line12 = new Line("line12", 0, 0, 0, 1, 1, 2);
        this.lines.add(line12);
        Line line13 = new Line("line13", 1, 0, 1, 1, 2, 2);
        this.lines.add(line13);
        Line line14 = new Line("line14", 1, 0, 1, 1, 0, 2);
        this.lines.add(line14);
        Line line15 = new Line("line15", 2, 0, 2, 1, 1, 2);
        this.lines.add(line15);
        Line line16 = new Line("line16", 1, 0, 0, 1, 0, 2);
        this.lines.add(line16);
        Line line17 = new Line("line17", 2, 0, 1, 1, 1, 2);
        this.lines.add(line17);
        Line line18 = new Line("line18", 0, 0, 1, 1, 1, 2);
        this.lines.add(line18);
        Line line19 = new Line("line19", 1, 0, 2, 1, 2, 2);
        this.lines.add(line19);
        Line line20 = new Line("line20", 0, 0, 2, 1, 1, 2);
        this.lines.add(line20);
    }

    public List<Line> list() {
        return this.lines;
    }

    public Line get(int index) {
        return this.lines.get(index);
    }

    public void renew() {
    }
}

